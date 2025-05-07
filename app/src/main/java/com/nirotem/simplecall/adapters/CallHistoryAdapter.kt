package com.nirotem.simplecall.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.startActivity
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController

import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.DBHelper.getContactIdFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getContactPhoto
import com.nirotem.simplecall.helpers.DBHelper.isNumberInContacts
import com.nirotem.simplecall.helpers.DBHelper.isNumberInFavorites
import com.nirotem.simplecall.helpers.ExternalAppsHelper.openSMSApp
import com.nirotem.simplecall.helpers.ExternalAppsHelper.openWhatsAppContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowMakingCallsEnum
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.callsHistory.PhoneCall2
import com.nirotem.simplecall.ui.singleCallHistory.SingleCallHistoryViewModel
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Locale

class CallHistoryAdapter(context: Context?, navigationController: NavController,
                         sharedSingleCallViewModel: SingleCallHistoryViewModel,
                         paramFragmentManager: FragmentManager,
    paramFragmentViewLifecycleOwner: LifecycleOwner, uiActivity: Activity,
                         uiRequestPermissionLauncher: ActivityResultLauncher<String>) :
    RecyclerView.Adapter<CallHistoryAdapter.CallViewHolder>() {
   // private val loadedRecords = callHistory
    private val callHistoryList = mutableListOf<PhoneCall2>()
    private val fragmentContext = context
    private val navController = navigationController
    private val singleCallViewModel = sharedSingleCallViewModel
    private val fragmentManager = paramFragmentManager
    private val fragmentViewLifecycleOwner = paramFragmentViewLifecycleOwner
    private val activity = uiActivity
    private val requestPermissionLauncher = uiRequestPermissionLauncher
    private var callIsWaitingForPermission: String? = null
    private var videoCallIsWaitingForPermission: String? = null
    private val languageEnum = LanguagesEnum.fromCode(Locale.getDefault().language)

/*    init {
        if (loadedRecords.isNotEmpty()) {
            addItems(loadedRecords)
        }
    }*/

    class CallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val contactName: TextView = itemView.findViewById(R.id.contactName)
        //val contactExistingPhotoBack: FrameLayout = itemView.findViewById(R.id.contactExistingPhotoBack)
//        val photoImageView: ImageView = itemView.findViewById(R.id.photoImageView)
        //    val callTypeIcon: ImageView = itemView.findViewById(R.id.callTypeIcon)
        val callDate: TextView = itemView.findViewById(R.id.callDate)
        val missedIndicator: ImageView =
            itemView.findViewById(R.id.missedIndicator) // A red dot or similar
        val contactsCallButton: FrameLayout = itemView.findViewById(R.id.contactsCallButton)
        val contactsCallButtonContainer: LinearLayout = itemView.findViewById(R.id.contactsCallButtonContainer)
       // val videoCallButton: ImageView = itemView.findViewById(R.id.videoCallButton)
        val infoButton: ImageView = itemView.findViewById(R.id.infoButton)
        val openWhatsUpButton: ImageView = itemView.findViewById(R.id.openWhatsUpButton)
        val msgContactButton: ImageView = itemView.findViewById(R.id.msgContactButton)

        val buttonsBack: LinearLayout = itemView.findViewById(R.id.buttonsBack)
        val callDateBack: LinearLayout = itemView.findViewById(R.id.callDateBack)
        var isRowOpened = false
    }

    fun historyListSize(): Int {
        return callHistoryList.size
    }

    fun addItems(newItems: List<PhoneCall2>) {
        val startPosition = callHistoryList.size
        callHistoryList.addAll(newItems)
        notifyItemRangeInserted(startPosition, newItems.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
       // callHistoryList.addAll(callHistoryList)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_call_big, parent, false)
        return CallViewHolder(view)
    }

    fun waveAnimation(textView: TextView) {
// In the layout, use a shader for the TextView
// In the layout, use a shader for the TextView
        // val textView = this.findViewById<TextView>(R.id.textView)

        val textShader = LinearGradient(
            0f, 0f, textView.paint.measureText(textView.text.toString()), textView.textSize,
            intArrayOf(Color.RED, Color.BLUE, Color.GREEN),
            null,
            Shader.TileMode.CLAMP
        )
        textView.paint.shader = textShader

        // val textView = findViewById<TextView>(R.id.textView)
        /*    val colorAnimator = ObjectAnimator.ofArgb(textView, "textColor", Color.RED, Color.BLUE)
            colorAnimator.duration = 1000
            colorAnimator.repeatMode = ValueAnimator.REVERSE
            colorAnimator.repeatCount = ValueAnimator.INFINITE
            colorAnimator.start()*/
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        var allowCalls = false

        val call = callHistoryList[position]
        holder.contactName.text = call.contactOrphoneNumber
        if (call.isPhoneInContacts) { // Contact name is text
            holder.contactName.textDirection = View.TEXT_DIRECTION_LOCALE // should be according to language
        }
        else { // Contact name is Phone number
            holder.contactName.text = OngoingCall.formatPhoneNumberWithLib(call.contactOrphoneNumber, languageEnum.region)
            holder.contactName.textDirection = View.TEXT_DIRECTION_LTR // Phone number must be left to right
        }


        /*if (call.phoneNumber.isNotEmpty()) {
            // Try to get photo:
            val contactId = if (fragmentContext != null) getContactIdFromPhoneNumber(
                fragmentContext,
                call.phoneNumber
            ) else null*/

/*            holder.contactExistingPhotoBack.visibility = GONE
            if (contactId != null) {
                val userProfilePicture = getContactPhoto(
                    fragmentContext!!,
                    contactId
                ) // if contactId is not null then we know fragmentContext isn't null
                if (userProfilePicture != null) {
                    holder.photoImageView.setImageBitmap(userProfilePicture)
                    holder.contactExistingPhotoBack.visibility = VISIBLE
                    //contactExistingPhotoBack.visibility = VISIBLE
                } else {
                    holder.contactExistingPhotoBack.visibility = GONE
                    // Handle case where no profile picture exists
                    //contactExistingPhotoBack.visibility = GONE
                }
            }*/
    //    }

        //val noPermissionToCall =
        //    PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))

        if (call.phoneNumber.isNotEmpty() && call.contactOrphoneNumber != "Unknown Caller" && fragmentContext != null && call.contactOrphoneNumber != fragmentContext.getString(R.string.unknown_caller)) {
            if (SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.TO_EVERYONE) {
                allowCalls = true
            }
            else if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                if (SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.CONTACTS_ONLY) {
                    allowCalls = isNumberInContacts(call.phoneNumber, fragmentContext)
                }
                else if (SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.FAVOURITES_ONLY) {
                    allowCalls = isNumberInFavorites(call.phoneNumber, fragmentContext)
                }
            }
        }

        holder.contactsCallButtonContainer.visibility = if (allowCalls) VISIBLE else GONE

        /*holder.infoButton.visibility =
            if (call.contactOrphoneNumber === "Unknown Caller") INVISIBLE else VISIBLE*/


        // ניתן להוסיף OnClickListener לפי הצורך
        // פעולה בעת לחיצה על איש קשר
        holder.itemView.setOnClickListener {
            holder.isRowOpened = !holder.isRowOpened
            if (holder.isRowOpened) {
                //holder.callDateBack.visibility = VISIBLE
                if (call.contactOrphoneNumber !== "Unknown Caller" && call.contactOrphoneNumber !== fragmentContext?.getString(R.string.unknown_caller)) {
                    holder.buttonsBack.visibility = VISIBLE
                }
            }
            else {
               // holder.callDateBack.visibility = GONE
                holder.buttonsBack.visibility = GONE
            }
        }

       // holder.callDateBack.visibility = GONE
        holder.buttonsBack.visibility = GONE

        if (call.phoneNumber.isNotEmpty()) {
            holder.infoButton.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("phone_number", call.phoneNumber)
                    putString("contact", call.contactOrphoneNumber)
                }
                singleCallViewModel.setSharedData(
                    SingleCallHistoryViewModel.ContactData(
                        call.phoneNumber,
                        call.contactOrphoneNumber
                    )
                )
                navController.navigate(R.id.recent_calls_to_single_call_info, bundle)
            }

            if (allowCalls && fragmentContext != null) { // here we also need permission to call
                holder.contactsCallButton.setOnClickListener {
                    /*callIsWaitingForPermission = if (PermissionsStatus.defaultDialerPermissionGranted.value != true || PermissionsStatus.callPhonePermissionGranted.value != true) { // no permission
                        call.phoneNumber
                    } else {
                        null
                    }*/ // we still go with the call, even if no permissions, and the user can grant it

                    callIsWaitingForPermission = null // for now not automatically sending call
                    OutgoingCall.makeCall(call.phoneNumber, false, fragmentContext, fragmentManager,
                        activity, requestPermissionLauncher)
                }

/*                holder.videoCallButton.setOnClickListener {
                    videoCallIsWaitingForPermission = if (PermissionsStatus.defaultDialerPermissionGranted.value != true || PermissionsStatus.callPhonePermissionGranted.value != true) { // no permission
                        call.phoneNumber
                    } else {
                        null
                    } // we still go with the call, even if no permissions, and the user can grant it
                    OutgoingCall.makeCall(call.phoneNumber, true, fragmentContext, fragmentManager)
                }*/

                PermissionsStatus.defaultDialerPermissionGranted.observe(fragmentViewLifecycleOwner) { permissionGranted ->
                    if (permissionGranted) {
                        if (callIsWaitingForPermission != null) {
                            OutgoingCall.makeCall(callIsWaitingForPermission!!, false, fragmentContext, fragmentManager ,
                                activity, requestPermissionLauncher)
                            callIsWaitingForPermission = null
                        }
                        else if (videoCallIsWaitingForPermission != null) {
                            OutgoingCall.makeCall(videoCallIsWaitingForPermission!!, true, fragmentContext,
                                fragmentManager, activity, requestPermissionLauncher)
                            videoCallIsWaitingForPermission = null
                        }
                    }
                }

                PermissionsStatus.callPhonePermissionGranted.observe(fragmentViewLifecycleOwner) { permissionGranted ->
                    if (permissionGranted) {
                        if (callIsWaitingForPermission != null) {
                            OutgoingCall.makeCall(callIsWaitingForPermission!!, false, fragmentContext, fragmentManager,
                                activity, requestPermissionLauncher)
                            callIsWaitingForPermission = null
                        }
                        else if (videoCallIsWaitingForPermission != null) {
                            OutgoingCall.makeCall(videoCallIsWaitingForPermission!!, true, fragmentContext, fragmentManager,
                                activity, requestPermissionLauncher)
                            videoCallIsWaitingForPermission = null
                        }
                    }
                }
            }

            holder.openWhatsUpButton.visibility = GONE // for now only from Contacts
/*            holder.openWhatsUpButton.setOnClickListener {
                if (fragmentContext != null) {
                    openWhatsAppContact(call.phoneNumber, fragmentContext)
                }
            }
            holder.openWhatsUpButton.visibility = if (SettingsStatus.allowOpeningWhatsApp.value == true && call.isPhoneInContacts) VISIBLE else GONE
            SettingsStatus.allowOpeningWhatsApp.observe(fragmentViewLifecycleOwner) { allowOpening ->
                holder.openWhatsUpButton.visibility =
                    if (SettingsStatus.allowOpeningWhatsApp.value == true && call.isPhoneInContacts) VISIBLE else GONE
            }*/


            /*            holder.msgContactButton.setOnClickListener {
                            if (fragmentContext != null) {
                                openSMSApp(call.phoneNumber, fragmentContext)
                            }
                        }*/
        }

        var dateFormat = "yyyy-dd-MM HH:mm"
        if (SettingsStatus.currLanguage.value == LanguagesEnum.ENGLISH) {
            dateFormat = "yyyy-MM-dd HH:mm"
        }

        if (SettingsStatus.currLanguage.value == LanguagesEnum.ARABIC) {
            holder.callDate.text = call.callDate
        }
        else {
            val dateTime: LocalDateTime = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // For API 26 and above, use DateTimeFormatter
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                    .withDecimalStyle(DecimalStyle.of(Locale.ENGLISH))
                    .withLocale(Locale.ENGLISH)
                LocalDateTime.parse(call.callDate, formatter)
            } else {
                // For API < 26, use SimpleDateFormat and Calendar for conversion
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
                val date = sdf.parse(call.callDate) ?: throw IllegalArgumentException("Invalid date format")
                val calendar = Calendar.getInstance().apply { time = date }
                LocalDateTime.of(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE)
                )
            }

            // עכשיו נגדיר Formatter "מקומי" לפי SHORT או MEDIUM (תלוי כמה פירוט רוצים)
            // - SHORT:    בפועל יציג מאוד מקוצר, לדוגמה "29.1.25, 20:27"
            // - MEDIUM:   קצת יותר מלא, למשל "29 בינו׳ 2025, 20:27"
            // - LONG/FULL יכולים לכלול פרטים נוספים כמו שם יום בשבוע או אזור זמן.
            val localizedFormatter = DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM)
                // כדי להשתמש בשפת המכשיר, עושים withLocale(Locale.getDefault()),
                // אם רוצים לכפות עברית: withLocale(Locale("he", "IL"))
                .withLocale(Locale.getDefault())

            // המרת התאריך שפורסר לתצוגה מקומית
            val localizedDateString = dateTime.format(localizedFormatter)

            holder.callDate.text = localizedDateString

        }



        //    holder.callType.text = "Incoming" // call.type

        //holder.callDate.text = call.callDate


        // Set call type icon
        when (call.type) {
            "Incoming" -> holder.missedIndicator.setImageResource(android.R.drawable.sym_call_incoming)
            "Outgoing" -> holder.missedIndicator.setImageResource(android.R.drawable.sym_call_outgoing)
            "Missed" -> holder.missedIndicator.setImageResource(android.R.drawable.sym_call_missed)
            "Rejected" -> holder.missedIndicator.setImageResource(android.R.drawable.ic_delete)
            "Blocked" -> holder.missedIndicator.setImageResource(android.R.drawable.ic_dialog_alert)
        }
        // holder.callTypeIcon.setImageResource(R.drawable.speaker_on)

        // Show or hide missed call indicator
        // holder.missedIndicator.visibility = if (call.missed) View.VISIBLE else View.GONE
        // holder.missedIndicator.visibility = View.VISIBLE
    }

    override fun getItemCount(): Int = callHistoryList.size
}
