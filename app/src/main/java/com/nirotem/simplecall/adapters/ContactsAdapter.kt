package com.nirotem.simplecall.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OngoingCall.formatPhoneNumberWithLib
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.DBHelper.getContactIdFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getContactPhoto
import com.nirotem.simplecall.helpers.ExternalAppsHelper.openSMSApp
import com.nirotem.simplecall.helpers.ExternalAppsHelper.openWhatsAppContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadVariableFromMemory
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowOpeningWhatsApp
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.components.CallPermissionMissingDialog
import com.nirotem.simplecall.ui.contacts.ContactsInLetterListItem
import com.nirotem.simplecall.ui.permissionsScreen.PermissionsAlertFragment
import com.nirotem.simplecall.ui.singleCallHistory.SingleCallHistoryViewModel
import java.util.Locale

class ContactsAdapter(
    private val items: List<ContactsInLetterListItem>,
    context: Context?,
    navigationController: NavController,
    sharedSingleCallViewModel: SingleCallHistoryViewModel,
    hostFragmentManager: FragmentManager,
    activityUI: Activity,
    requestPermissionLauncherUI: ActivityResultLauncher<String>,
    paramFragmentViewLifecycleOwner: LifecycleOwner
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val fragmentContext = context
    private val navController = navigationController
    private val singleCallViewModel = sharedSingleCallViewModel
    private val fragmentManager = hostFragmentManager
    private val activity = activityUI
    private val requestPermissionLauncher = requestPermissionLauncherUI
    private val fragmentViewLifecycleOwner = paramFragmentViewLifecycleOwner

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_FAVOURITS_HEADER = 1
        private const val VIEW_TYPE_CONTACT = 2
        private const val VIEW_TYPE_GOLD_NUMBER_HEADER = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ContactsInLetterListItem.Header -> VIEW_TYPE_HEADER
            is ContactsInLetterListItem.FavouritesHeader -> VIEW_TYPE_FAVOURITS_HEADER
            is ContactsInLetterListItem.GoldNumberHeader -> VIEW_TYPE_GOLD_NUMBER_HEADER
            is ContactsInLetterListItem.Contact -> VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(view)
        } else if (viewType == VIEW_TYPE_FAVOURITS_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_favourites_caption, parent, false)
            FavouritesHeaderViewHolder(view)
        } else if (viewType == VIEW_TYPE_GOLD_NUMBER_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.contacts_gold_number_caption, parent, false)
            GoldNumberHeaderViewHolder(view)
        } else {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.contact_row_big, parent, false)
            ContactViewHolder(
                view,
                singleCallViewModel,
                navController,
                fragmentContext,
                fragmentManager,
                activity,
                requestPermissionLauncher,
                fragmentViewLifecycleOwner
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ContactsInLetterListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactsInLetterListItem.FavouritesHeader -> (holder as FavouritesHeaderViewHolder).bind(
                item
            )

            is ContactsInLetterListItem.GoldNumberHeader -> (holder as GoldNumberHeaderViewHolder).bind(
                item
            )

            is ContactsInLetterListItem.Contact -> (holder as ContactViewHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerText: TextView = itemView.findViewById(R.id.headerText)

        fun bind(header: ContactsInLetterListItem.Header) {
            headerText.text = header.letter
        }
    }

    class FavouritesHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //  private val headerText: TextView = itemView.findViewById(R.id.contacts_f)

        fun bind(header: ContactsInLetterListItem.FavouritesHeader) {
            // headerText.text = header.letter
        }
    }

    class GoldNumberHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //  private val headerText: TextView = itemView.findViewById(R.id.contacts_f)

        fun bind(header: ContactsInLetterListItem.GoldNumberHeader) {
            // headerText.text = header.letter
        }
    }

    class ContactViewHolder(
        itemView: View,
        singleCallVM: SingleCallHistoryViewModel,
        navigationController: NavController,
        fragContext: Context?,
        holderFragmentManager: FragmentManager,
        activityUI: Activity,
        requestPermissionLauncherUI: ActivityResultLauncher<String>,
        paramFragmentViewLifecycleOwner: LifecycleOwner
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameText: TextView = itemView.findViewById(R.id.contactsContactName)
        private val dialButton: ImageView = itemView.findViewById(R.id.dialButton)
        private val goldNumberDialButton: ImageView = itemView.findViewById(R.id.goldNumberDialButton)
        private val contactsCallButton: FrameLayout = itemView.findViewById(R.id.contactsCallButton)
        private val contactsCallButtonContainer: LinearLayout = itemView.findViewById(R.id.contactsCallButtonContainer)
        private val infoButton: ImageView = itemView.findViewById(R.id.contactsInfoButton)

        private val openWhatsUpButton: ImageView = itemView.findViewById(R.id.openWhatsUpButton)
        private val fragmentViewLifecycleOwner = paramFragmentViewLifecycleOwner
        private val activity = activityUI
        private val requestPermissionLauncher = requestPermissionLauncherUI

        private val fragmentContext = fragContext
        private val currFragmentManager = holderFragmentManager
        private val navController = navigationController
        private val singleCallViewModel = singleCallVM
        private var resourceAllowOutgoingCallsModeOrNull = fragContext?.resources?.getString(R.string.allowOutgoingCallsMode)
        private var resourceAllowOutgoingCallsMode = if (resourceAllowOutgoingCallsModeOrNull != null) resourceAllowOutgoingCallsModeOrNull else AllowOutgoingCallsEnum.NO_ONE.toString()

        private val userAllowsMakingCallsMode =
            if (fragmentContext != null) loadAllowMakingCallsEnum(fragmentContext) else null
        private var selectedOutgoingCallsEnum =
            if (userAllowsMakingCallsMode != null) AllowOutgoingCallsEnum.valueOf(
                userAllowsMakingCallsMode
            ) else AllowOutgoingCallsEnum.valueOf(resourceAllowOutgoingCallsMode.toString())

        // private val noPermissionToCall =
        //   PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))
       //    private val phoneNumber: TextView = itemView.findViewById(R.id.phoneNumber)

        // private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)

        private val contactExistingPhotoBackContainer: LinearLayout =
            itemView.findViewById(R.id.contactExistingPhotoBackContainer)
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val phoneNumberBack: LinearLayout = itemView.findViewById(R.id.phoneNumberBack)
        private val buttonsBack: LinearLayout = itemView.findViewById(R.id.buttonsBack)
        private val autoAnswerCheckBox: CheckBox = itemView.findViewById(R.id.autoAnswerCheckBox)
        private var isRowOpened = false
        private val languageEnum = LanguagesEnum.fromCode(Locale.getDefault().language)
        private val phoneNumbersContainer = itemView.findViewById<LinearLayout>(R.id.phoneNumbersContainer)
        private val simpleLineBack: View = itemView.findViewById(R.id.simpleLineBack)


        fun bind(contact: ContactsInLetterListItem.Contact) {
            simpleLineBack.setOnClickListener {
                isRowOpened = !isRowOpened
                phoneNumberBack.visibility = if (isRowOpened) View.VISIBLE else View.GONE
                buttonsBack.visibility = if (isRowOpened) View.VISIBLE else View.GONE
            }

            nameText.text = contact.contactOrPhoneNumber

            contactExistingPhotoBackContainer.visibility = GONE
            if (contact.photoUri.isNotEmpty() && contact.photoUri != "null") {
                val uri = contact.photoUri.toUri()
                photoImageView.setImageURI(uri)
                contactExistingPhotoBackContainer.visibility = VISIBLE
            }

            val isUserGoldNumber =
                (SettingsStatus.goldNumber.value != null && contact.phoneNumbers.contains(SettingsStatus.goldNumber.value)) &&
                        (SettingsStatus.goldNumberContact.value != null && contact.contactOrPhoneNumber == SettingsStatus.goldNumberContact.value)

            val userAllowsCalls = selectedOutgoingCallsEnum != AllowOutgoingCallsEnum.NO_ONE

            if (contact.phoneNumbers.isNotEmpty() && (userAllowsCalls || isUserGoldNumber)) {
                contactsCallButtonContainer.visibility = VISIBLE
                if (isUserGoldNumber) {
                    goldNumberDialButton.setOnClickListener {
                        handlePhoneDialClick(contact.phoneNumbers.first())
                    }
                } else {
                    dialButton.setOnClickListener {
                        handlePhoneDialClick(contact.phoneNumbers.first())
                    }
                }
            } else {
                contactsCallButtonContainer.visibility = GONE
            }

            if (isUserGoldNumber) {
                contactsCallButton.visibility = GONE
                goldNumberDialButton.visibility = VISIBLE
            } else {
                contactsCallButton.visibility = VISIBLE
                goldNumberDialButton.visibility = GONE
            }

            infoButton.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("phone_number", contact.phoneNumbers.firstOrNull())
                    putString("contact", contact.contactOrPhoneNumber)
                }
                singleCallViewModel.setSharedData(
                    SingleCallHistoryViewModel.ContactData(
                        contact.phoneNumbers.firstOrNull() ?: "",
                        contact.contactOrPhoneNumber
                    )
                )
                navController.navigate(R.id.contacts_to_single_call_info, bundle)
            }

            openWhatsUpButton.setOnClickListener {
                if (fragmentContext != null && contact.phoneNumbers.isNotEmpty()) {
                    openWhatsAppContact(contact.phoneNumbers.first(), fragmentContext)
                }
            }

            val allowOpeningWhatsApp = SettingsStatus.allowOpeningWhatsApp.value
            openWhatsUpButton.visibility = if (allowOpeningWhatsApp == true) VISIBLE else GONE
            SettingsStatus.allowOpeningWhatsApp.observe(fragmentViewLifecycleOwner) { allowOpening ->
                openWhatsUpButton.visibility = if (allowOpening == true) VISIBLE else GONE
            }

            phoneNumberBack.visibility = GONE
            buttonsBack.visibility = GONE

/*
            phoneNumbersContainer.removeAllViews() // לניקוי קודמים אם יש
            for (number in contact.phoneNumbers) {
                val textView = TextView(itemView.context).apply {
                    text = number
                    textSize = 28f
                    textDirection = View.TEXT_DIRECTION_LTR
                    textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                    gravity = Gravity.START
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    typeface = ResourcesCompat.getFont(context, R.font.inter_bold)
                    setPadding(0, 10, 0, 10)
                }
                if (userAllowsCalls) {
                    textView.setOnClickListener {
                        handlePhoneDialClick(number)
                    }
                }
                textView.isClickable = userAllowsCalls
                textView.isFocusable = userAllowsCalls
                phoneNumbersContainer.addView(textView)
            }
*/

            autoAnswerCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (fragmentContext != null && contact.phoneNumbers.isNotEmpty()) {
                    saveAutoAnswer(contact.phoneNumbers.first(), isChecked, fragmentContext)
                }
            }

            if (fragmentContext != null && contact.phoneNumbers.isNotEmpty()) {
                loadAndShowSettings(contact.phoneNumbers.first(), fragmentContext)
            }

            phoneNumbersContainer.removeAllViews()

            val showIcon = contact.phoneNumbers.size > 1
            contact.phoneNumbers.forEach { number ->
                val row = ConstraintLayout(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dpToPx(6)
                    }
                    layoutDirection = View.LAYOUT_DIRECTION_LTR
                }

                val tvId = View.generateViewId()
                val tv = TextView(itemView.context).apply {
                    id = tvId
                    text = formatPhoneNumberWithLib(number, languageEnum.region)
                    setTextColor(ContextCompat.getColor(context, R.color.white))
                    textSize = 28f
                    typeface = ResourcesCompat.getFont(context, R.font.inter_bold)
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    textDirection = View.TEXT_DIRECTION_LTR
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    gravity = Gravity.CENTER

                    layoutParams = ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                        bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                        startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                        endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    }

                    if (userAllowsCalls) {
                        setOnClickListener { handlePhoneDialClick(number) }
                        isClickable = true
                        isFocusable = true
                    }
                }
                row.addView(tv)

                if (showIcon) {
                    val ivId = View.generateViewId()
                    val iv = ImageView(itemView.context).apply {
                        id = ivId
                        setImageResource(R.drawable.call_phone_green_small)
                        layoutParams = ConstraintLayout.LayoutParams(
                            dpToPx(56),
                            dpToPx(56)
                        ).apply {
                            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                            marginStart = dpToPx(12)
                        }
                        scaleType = ImageView.ScaleType.FIT_CENTER

                        if (userAllowsCalls) {
                            setOnClickListener { handlePhoneDialClick(number) }
                            isClickable = true
                            isFocusable = true
                        }
                    }
                    row.addView(iv)
                }

                phoneNumbersContainer.addView(row)
            }
        }

        private fun loadAndShowSettings(phoneNumber: String?, context: Context) {
            if (phoneNumber != null) {
                val autoAnswerKey = "${phoneNumber}_aa" // aa = auto answer
                val autoAnswer = loadVariableFromMemory(autoAnswerKey, context)
                autoAnswerCheckBox.isChecked = autoAnswer == "true"
            } else {
                autoAnswerCheckBox.isChecked = false
            }
        }

        private fun handlePhoneDialClick(phoneNumberToCall: String) {
            if (fragmentContext != null) {
                OutgoingCall.makeCall(
                    phoneNumberToCall,
                    false,
                    fragmentContext,
                    currFragmentManager,
                    activity,
                    requestPermissionLauncher
                )
            }
        }

        private fun dpToPx(dp: Int): Int {
            return (dp * itemView.context.resources.displayMetrics.density).toInt()
        }

    }
}
