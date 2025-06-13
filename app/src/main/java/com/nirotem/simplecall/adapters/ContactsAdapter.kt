package com.nirotem.simplecall.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
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
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.OngoingCall
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
        //private val dialButtonBorder: ImageView = itemView.findViewById(R.id.dialButtonBorder)
        private val goldNumberDialButton: ImageView = itemView.findViewById(R.id.goldNumberDialButton)
        private val contactsCallButton: FrameLayout = itemView.findViewById(R.id.contactsCallButton)
        private val contactsCallButtonContainer: LinearLayout = itemView.findViewById(R.id.contactsCallButtonContainer)
        private val infoButton: ImageView = itemView.findViewById(R.id.contactsInfoButton)

        //private val videoCallButton: ImageView = itemView.findViewById(R.id.videoCallButton)
        private val openWhatsUpButton: ImageView = itemView.findViewById(R.id.openWhatsUpButton)
        private val fragmentViewLifecycleOwner = paramFragmentViewLifecycleOwner
        private val activity = activityUI
        private val requestPermissionLauncher = requestPermissionLauncherUI

        // private val msgContactButton: ImageView = itemView.findViewById(R.id.msgContactButton)
        private val fragmentContext = fragContext
        private var contactId: String? = null
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
        private val phoneNumber: TextView = itemView.findViewById(R.id.phoneNumber)

        // private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)

        private val contactExistingPhotoBackContainer: LinearLayout =
            itemView.findViewById(R.id.contactExistingPhotoBackContainer)
        private val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        private val phoneNumberBack: LinearLayout = itemView.findViewById(R.id.phoneNumberBack)
        private val buttonsBack: LinearLayout = itemView.findViewById(R.id.buttonsBack)
        private val autoAnswerCheckBox: CheckBox = itemView.findViewById(R.id.autoAnswerCheckBox)
        private var isRowOpened = false
        private val languageEnum = LanguagesEnum.fromCode(Locale.getDefault().language)

        fun bind(contact: ContactsInLetterListItem.Contact) {
            nameText.text = contact.contactOrPhoneNumber
            val phoneNumberToCall = contact.phoneNumber
            phoneNumber.text =
                OngoingCall.formatPhoneNumberWithLib(
                    contact.phoneNumber,
                    languageEnum.region
                )

            // Try to get photo:
            contactId = if (fragmentContext != null) getContactIdFromPhoneNumber(
                fragmentContext,
                phoneNumberToCall
            ) else null

            contactExistingPhotoBackContainer.visibility = GONE
            if (contactId != null) {
                val userProfilePicture = getContactPhoto(
                    fragmentContext!!,
                    contactId!!
                ) // if contactId is not null then we know fragmentContext isn't null
                if (userProfilePicture != null) {
                    photoImageView.setImageBitmap(userProfilePicture)
                    contactExistingPhotoBackContainer.visibility = VISIBLE
                    //contactExistingPhotoBack.visibility = VISIBLE
                } else {
                    contactExistingPhotoBackContainer.visibility = GONE
                    // Handle case where no profile picture exists
                    //contactExistingPhotoBack.visibility = GONE
                }
            }

            val isUserGoldNumber =
                (SettingsStatus.goldNumber.value != null && contact.phoneNumber == SettingsStatus.goldNumber.value)
                        && (SettingsStatus.goldNumberContact.value != null && contact.contactOrPhoneNumber == SettingsStatus.goldNumberContact.value)
            val userAllowsCalls = selectedOutgoingCallsEnum != AllowOutgoingCallsEnum.NO_ONE
/*            contactsCallButton.visibility =
                if (contact.phoneNumber === "" || (!userAllowsCalls && !isUserGoldNumber)) INVISIBLE else VISIBLE*/
            if (contact.phoneNumber !== "" && (userAllowsCalls || isUserGoldNumber)) {
                contactsCallButtonContainer.visibility = VISIBLE
                if (isUserGoldNumber) {
                    goldNumberDialButton.setOnClickListener({
                        handlePhoneDialClick(contact.phoneNumber)
                    })
                }
                else {
/*                    contactsCallButton.setOnClickListener({
                        handlePhoneDialClick(contact.phoneNumber)
                    })*/
                    dialButton.setOnClickListener({
                        handlePhoneDialClick(contact.phoneNumber)
                    })
/*                    dialButtonBorder.setOnClickListener({
                        handlePhoneDialClick(contact.phoneNumber)
                    })*/
                }
            }
            else {
                contactsCallButtonContainer.visibility = GONE
            }

            if (isUserGoldNumber) {
                //callButton.setImageResource(R.drawable.goldappiconphoneblack)
                contactsCallButton.visibility = GONE
                if (SettingsStatus.isPremium) {
                    goldNumberDialButton.setImageResource(SettingsStatus.appLogoResourceSmall) // otherwise it has the default already from design-time
                }
                goldNumberDialButton.visibility = VISIBLE
/*                val paddingInDp = 10
                val density = callButton.context.resources.displayMetrics.density
                val paddingInPx = (paddingInDp * density + 0.5f).toInt()
                callButton.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)*/
            }
            else {
/*                val paddingInDp = 0
                val density = callButton.context.resources.displayMetrics.density
                val paddingInPx = (paddingInDp * density + 0.5f).toInt()
                callButton.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)*/
               // callButton.setImageResource(R.drawable.goldappiconphoneblack)
                contactsCallButton.visibility = VISIBLE
                goldNumberDialButton.visibility = GONE
            }

            infoButton.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("phone_number", contact.phoneNumber)
                    putString("contact", contact.contactOrPhoneNumber)
                }
                singleCallViewModel.setSharedData(
                    SingleCallHistoryViewModel.ContactData(
                        contact.phoneNumber,
                        contact.contactOrPhoneNumber
                    )
                )
                navController.navigate(R.id.contacts_to_single_call_info, bundle)
            }

            openWhatsUpButton.setOnClickListener {
                if (fragmentContext != null) {
                    openWhatsAppContact(contact.phoneNumber, fragmentContext)
                }
            }
            val allowOpeningWhatsApp = SettingsStatus.allowOpeningWhatsApp.value
            openWhatsUpButton.visibility = if (allowOpeningWhatsApp == true) VISIBLE else GONE
            SettingsStatus.allowOpeningWhatsApp.observe(fragmentViewLifecycleOwner) { allowOpening ->
                val allowOpeningWhatsApp = SettingsStatus.allowOpeningWhatsApp.value
                openWhatsUpButton.visibility =
                    if (allowOpeningWhatsApp == true) VISIBLE else GONE
            }

            if (fragmentContext != null) {
                loadAndShowSettings(
                    contact.phoneNumber,
                    fragmentContext
                ) // better be before listener register
            }

            autoAnswerCheckBox.setOnCheckedChangeListener { _, isChecked ->
/*                if (isChecked) {
                    // Handle the case when the user just checked the box
                } else {
                    // Handle the case when the user just unchecked the box
                }*/
                if (fragmentContext != null) {
                    saveAutoAnswer(contact.phoneNumber, isChecked, fragmentContext)
                }
            }

            /*            msgContactButton.setOnClickListener {
                            if (fragmentContext != null) {
                                openSMSApp(contact.phoneNumber, fragmentContext)
                            }
                        }*/


            /*            videoCallButton.setOnClickListener {
                            if (fragmentContext != null) {
                                OutgoingCall.makeCall(phoneNumberToCall, true, fragmentContext, currFragmentManager)
                            }
                        }*/

            // phoneText.text = contact.phoneNumber
            // favoriteIcon.visibility = if (contact.isFavourite) View.VISIBLE else View.GONE

            // טעינת התמונה מ-URI (ניתן להשתמש בספרייה כמו Glide או Picasso)
            /*            if (contact.photoUri.isNotEmpty()) {
                            Glide.with(itemView.context)
                                .load(contact.photoUri)
                                .placeholder(R.drawable.default_contact)
                                .into(photoImageView)
                        } else {
                            photoImageView.setImageResource(R.drawable.default_contact)
                        }*/

            // ניתן להוסיף OnClickListener לפי הצורך
            // פעולה בעת לחיצה על איש קשר
            itemView.setOnClickListener {
                isRowOpened = !isRowOpened
                if (isRowOpened) {
                    phoneNumberBack.visibility = VISIBLE
                    buttonsBack.visibility = VISIBLE
                } else {
                    phoneNumberBack.visibility = GONE
                    buttonsBack.visibility = GONE
                }
            }

            phoneNumberBack.visibility = GONE
            buttonsBack.visibility = GONE
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
    }
}
