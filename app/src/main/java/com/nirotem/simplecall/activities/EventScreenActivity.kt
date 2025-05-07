package com.nirotem.simplecall.activities

import android.Manifest
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nirotem.simplecall.CallActivity
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.OutgoingCall.isCalling
import com.nirotem.simplecall.OutgoingCall.wasAnswered
import com.nirotem.simplecall.R
import com.nirotem.simplecall.WaitingCall
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumberAndReturnNullIfNotFound
import com.nirotem.simplecall.helpers.SharedPreferencesCache.getContactNameFromPhoneNumberInJson
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveCallActivityLoadedTimeStamp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveLastExternalCallDate
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.SpeakCommandsManager
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.ui.activeCall.ActiveCallFragment
import com.nirotem.simplecall.ui.conferenceCall.ConferenceCallFragment
import com.nirotem.simplecall.ui.goldNumber.GoldNumberFragment
import com.nirotem.simplecall.ui.incomingCall.IncomingCallFragment
import com.nirotem.simplecall.ui.waitingCall.WaitingCallFragment
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class EventScreenActivity : AppCompatActivity() {

    private var currFragmentView: View? = null
    private var isSpeakerOn = false
    private var activeCallFragment: ActiveCallFragment? = null

    //  private lateinit var mediaPlayer: MediaPlayer
    private lateinit var callerNumberTextView: TextView
    private lateinit var activeFragment: androidx.fragment.app.Fragment

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("SimplyCall - EventScreenActivity", "Received event: ${intent?.action}")
            if (intent?.action == "com.nirotem.simplecall.REMOVE_ACTIVE_CALL") {
                finish()
            } else if (intent?.action == "com.nirotem.simplecall.CHANGE_ACTIVE_CALL") {
                val phoneContactOrNumber = intent.getStringExtra("phoneContactOrNumber")
                // Update your UI or data with the new call information
                val fragment: ConferenceCallFragment? =
                    supportFragmentManager.findFragmentById(R.id.fragment_container) as ConferenceCallFragment?


                // שינוי הטקסט ב-TextView שבפרגמנט
                if (fragment != null && phoneContactOrNumber !== null) {
                    fragment.updateText(phoneContactOrNumber)
                }
            } else if (intent?.action == "ADD_CALLER") {
                val phoneContactOrNumber = intent.getStringExtra("phoneContactOrNumber")
                val fragmentTransaction = supportFragmentManager.beginTransaction()

                val fragment =
                    // For now only 2 callers (+user)
                    ConferenceCallFragment().apply {
                        arguments = Bundle().apply {
                            putString("CALLER_NUMBER", OngoingCall.phoneNumberOrContact)
                            putString("CALLER_NUMBER2", WaitingCall.phoneNumberOrContact)
                        }
                    }


                fragmentTransaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_down)
                fragmentTransaction.replace(R.id.fragment_container, fragment)
                fragmentTransaction.commit()
            } else if (intent?.action == "com.nirotem.simplecall.ACTION_ERROR_MESSAGE_EVENT") {
                val message = intent.getStringExtra("event_message")
                showCallErrorAlert(context, message)
            }
        }
    }

    private fun showCallErrorAlert(context: Context?, message: String?) {
        if (context != null && message != null) {
            AlertDialog.Builder(context)
                .setTitle(getString(R.string.serious_error_capital))
                .setMessage(message)
                .setIcon(R.drawable.goldappiconphoneblack)
                .setPositiveButton(getString(R.string.close_capital)) { dialog, _ ->
                    dialog.dismiss() // Close the dialog
                    finish() // This will close the current activity
                }
                .setCancelable(false) // Make the dialog not cancellable by touching outside
                .show()
        }
    }

    // @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*        window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )*/

        OpenScreensStatus.eventScreenActivityIsOpen.value = true
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val data: Uri? = intent.data
        val externalAppOutgoingCallPhoneNumber = data?.schemeSpecificPart
        Log.d(
            "SimplyCall - EventScreenActivity",
            "Loading...phoneNumber (from external Intent)=$externalAppOutgoingCallPhoneNumber"
        )

        try {
            // Close the activity when call (all calls, including waiting-calls) is ended)
            CallActivity.callEndedShouldCloseActivityEvent.value =
                false // no reason not to reset this event here
            CallActivity.callEndedShouldCloseActivityEvent.observe(this) { shouldCloseActivity ->
                if (shouldCloseActivity) {
                    CallActivity.callEndedShouldCloseActivityEvent.value = false
                    finish()
                }
            }
        } catch (e: Exception) {
            Log.e(
                "SimplyCall - EventScreenActivity",
                "CallActivity.callEndedShouldCloseActivityEvent Error (${e.message})"
            )
        }

        try {
            if (SpeakCommandsManager.speechCommandsEnabled) {
                SpeakCommandsManager.init(this, this)
            }
        } catch (e: Exception) {
            Log.e(
                "SimplyCall - EventScreenActivity",
                "SpeakCommandsManager Error (${e.message})"
            )
        }

        try { // Call Critical Error event:
            // CallActivity.criticalErrorEvent.value = false // no reason not to reset this event here
            //CallActivity.criticalErrorEventMessage = ""
            /*            CallActivity.criticalErrorEvent.observe(this) { isCriticalError ->
                            if (isCriticalError) {
                                val message = CallActivity.criticalErrorEventMessage
                                CallActivity.criticalErrorEventMessage = ""
                                CallActivity.criticalErrorEvent.value = false
                               // showCallErrorAlert(this, message)
                            }
                        }*/
        } catch (e: Exception) {
            Log.e(
                "SimplyCall - EventScreenActivity",
                "CallActivity.criticalErrorEventMessage Error (${e.message})"
            )
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)

                val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                km.requestDismissKeyguard(this, null)
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                            WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                )
            }

            //Toast.makeText(this, "Please unlock screen", Toast.LENGTH_SHORT).show()

            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, null)
            }
        } catch (e: Exception) {
            Log.e(
                "SimplyCall - EventScreenActivity",
                "keyguardManager and lock screen error (${e.message})"
            )
        }

        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } catch (e: Exception) {
            Log.e(
                "SimplyCall - EventScreenActivity",
                "requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT error (${e.message})"
            )
        }

        // If we got here then the Activity could be created.
        // so we must save that there is no issue with 'Display pop-up windows while running in the background'
        // that can happen in Xiaomi
        // SAVE TIMESTAMP:
        saveCallActivityLoadedTimeStamp(this)

        // Set this activity to show when locked
        // setShowWhenLocked(true)

        // Turn the screen on when this activity is displayed
        // setTurnScreenOn(true)


        // Set the content view for the activity
        setContentView(R.layout.activity_event_screen)

        try {
            // בקשת ביטול מסך נעילה (זמין החל מ־API 26)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                keyguardManager.requestDismissKeyguard(this, null)
            }
        } catch (err: Error) {
            Log.d(
                "SimplyCall - EventScreenActivity",
                "EventScreenActivity OnCreate keyguardManager ERROR (error = $err)"
            )
        }

        try {
// if we don't have the permission we must show notification at least
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
                window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) // Optional
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
                window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
            }
        } catch (err: Error) {
            Log.d(
                "SimplyCall - EventScreenActivity",
                "EventScreenActivity OnCreate setShowWhenLocked ERROR (error = $err)"
            )
        }

        // Retrieve intent data
        val isCallWaiting = intent.getBooleanExtra("IS_CALL_WAITING", false)
        val callerNumber =
            intent.getStringExtra("CALLER_NUMBER") ?: this.getString(R.string.unknown_caller)

        val callerNumber2 = if (OngoingCall.conference) {
            intent.getStringExtra("CALLER_NUMBER1") ?: this.getString(R.string.unknown_caller)
        } else ""
        val isAutoAnswer = intent.getBooleanExtra("AUTO_ANSWER", false)
        val callAnswered = intent.getBooleanExtra("CALL_ANSWERED", false)
        val isCallOutgoing = intent.getBooleanExtra("IS_CALLING", false)
        val isInMiddleOfCall = intent.getBooleanExtra("IN_MIDDLE_OF_CALL", false)
        val isGoldNumberAddingOrEditing = intent.getBooleanExtra("IS_GOLD_NUMBER", false)
        val showActivityOnlyForCallErrorMessage =
            intent.getStringExtra("ONLY_SHOW_ERROR_MESSAGE") ?: ""
        val showActivityOnlyForCallErrorTitle =
            intent.getStringExtra("ONLY_SHOW_ERROR_MESSAGE_TITLE") ?: ""
        var contactNameForPhoneNumber: String? = null


        var isFragmentNewActiveCall = false

        // If this is the first creation, dynamically load the appropriate fragment
        if (savedInstanceState == null) {
            if (isGoldNumberAddingOrEditing) {
                val fragment =
                    GoldNumberFragment.newInstance("") // for now only adding new Gold Number from here


                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)  // Replace with the second fragment
                    .addToBackStack(null)  // Add this transaction to the back stack
                    .commit()
            } else if (OngoingCall.conference) {
                val fragment = ConferenceCallFragment().apply {
                    arguments = Bundle().apply {
                        putString("CALLER_NUMBER", callerNumber)
                        putString("CALLER_NUMBER2", callerNumber2)
                    }
                }
                activeFragment = fragment

                // Replace the fragment container with the selected fragment
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit()
            } else if (showActivityOnlyForCallErrorMessage.isNotEmpty()) { // Only show alert dialog
                showCallErrorAlert(this, showActivityOnlyForCallErrorMessage)
            } else {
                if (externalAppOutgoingCallPhoneNumber != null) { // External app made outgoing call and we need to handle it since we are default dialer app
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val currentDateTime = LocalDateTime.now()
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        val formattedDateTime = currentDateTime.format(formatter)
                        saveLastExternalCallDate(this, formattedDateTime)
                    }
                    val normalizedPhoneCall =
                        externalAppOutgoingCallPhoneNumber.filter { it.isDigit() }

                    if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                        contactNameForPhoneNumber =
                            getContactNameFromPhoneNumberAndReturnNullIfNotFound(
                                this,
                                externalAppOutgoingCallPhoneNumber
                            )
                    }
                    if (contactNameForPhoneNumber == null) {
                        contactNameForPhoneNumber = getContactNameFromPhoneNumberInJson(
                            this,
                            externalAppOutgoingCallPhoneNumber
                        )
                    }

                    OutgoingCall.contactExistsForPhoneNum = contactNameForPhoneNumber != null
                    makeCallFromExternalApp(normalizedPhoneCall, this)
                } else if ((callAnswered || isCallOutgoing) && ((!isInMiddleOfCall) || activeCallFragment == null)) { // Load ActiveCallFragment if the call was answered
                    // We save the instance, because in case waiting call was answered or rejected
                    activeCallFragment = ActiveCallFragment().apply {
                        arguments = Bundle().apply {
                            putString("CALLER_NUMBER", callerNumber)
                            putBoolean("IS_CALLING", isCallOutgoing)
                            putBoolean("STARTING_FROM_MIDDLE_OF_CALL", isInMiddleOfCall)
                        }
                    }
                } else {
                    if (!isCallWaiting && OngoingCall.call == null) { // missed call
                        //callWasMissed = true
                        val toastMsg = getString(R.string.missed_call_from, callerNumber)

                        showCustomToastDialog(this, toastMsg)
                    }
                }
                val fragment =
                    if (externalAppOutgoingCallPhoneNumber != null) { // External app made outgoing call and we need to handle it since we are default dialer app


                        val contactName =
                            if (contactNameForPhoneNumber != null) contactNameForPhoneNumber else externalAppOutgoingCallPhoneNumber

                        ActiveCallFragment().apply {
                            arguments = Bundle().apply {
                                putString("CALLER_NUMBER", contactName)
                                putBoolean("IS_CALLING", true)
                                putBoolean("STARTING_FROM_MIDDLE_OF_CALL", false)
                                putString(
                                    "CONTACT_NANE_FOR_PHONE_NUMBER",
                                    contactNameForPhoneNumber
                                )
                            }
                        }
                    } else if (isInMiddleOfCall || callAnswered || isCallOutgoing) { // if call answered we will show Active call screen even if it was call waiting
                        // Load ActiveCallFragment if the call was answered
                        // We save the instance, because in case waiting call was answered or rejected
                        // We want to come back to the same active call fragment and not to a new one
                        activeCallFragment
                        /*                        ActiveCallFragment().apply {
                                                    arguments = Bundle().apply {
                                                        putString("CALLER_NUMBER", callerNumber)
                                                        putBoolean("IS_CALLING", isCallOutgoing)
                                                        putBoolean("STARTING_FROM_MIDDLE_OF_CALL", isInMiddleOfCall)
                                                    }
                                                }*/
                    } else if (OngoingCall.call != null) {
                        IncomingCallFragment.newInstance(callerNumber, isAutoAnswer)
                    } else {
                        null


                        //if (isCallWaiting)  // WE SHOULD NEVER GET HERE - ALWAYS FROM ACTIVECALLFRAGMENT LISTENING
                        /*                    // Load ActiveCallFragment if the call was answered
                    WaitingCallFragment().apply {
                        arguments = Bundle().apply {
                            putString("CALLER_NUMBER", callerNumber)
                        }
                    }*/

                        // WaitingCallFragment.newInstance(callerNumber)
                        //   else {
                        // Load IncomingCallFragment if the call was not answered

                        //   }

                    }

                if (fragment != null) {
                    activeFragment = fragment

                    // Replace the fragment container with the selected fragment
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment, "ACTIVE_CALL_TAG")
                        .addToBackStack("ACTIVE_CALL_TAG")
                        .commit()
                }
                else {
                    finish()
                }
            }
        }

        Log.d(
            "SimplyCall - EventScreenActivity",
            "EventScreenActivity OnCreate (callAnswered = $callAnswered, callerNumber = $callerNumber)"
        )


        val intentFilter = IntentFilter("com.nirotem.simplecall.CHANGE_ACTIVE_CALL").apply {
            addAction("com.nirotem.simplecall.REMOVE_ACTIVE_CALL") // If you need multiple actions
            addAction("com.nirotem.simplecall.SWITCH_TO_ACTIVE_CALL")
            addAction("com.nirotem.simplecall.SHOW_WAITING_CALL")
            addAction("com.nirotem.simplecall.BACK_TO_ACTIVE_CALL")
            addAction("com.nirotem.simplecall.ACTION_ERROR_MESSAGE_EVENT")
        }

        ContextCompat.registerReceiver(
            this, // Context
            receiver, // The BroadcastReceiver
            intentFilter, // The IntentFilter
            ContextCompat.RECEIVER_NOT_EXPORTED // Export state
        )
    }

    /*    override fun onNewIntent(intent: Intent?) {
            super.onNewIntent(intent)
            setIntent(intent)
            handleIntent(intent)
        }*/

    fun makeCallFromExternalApp(
        callPhoneNumber: String,
        context: Context,
    ): Boolean {
        if (PermissionsStatus.callPhonePermissionGranted.value == true) {
            if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                // We have permissions - make the call:
                wasAnswered = false
                isCalling = true

                val extras = Bundle()
                val uri = "tel:${callPhoneNumber}".toUri()
                //val callIntent = Intent(Intent.ACTION_CALL, uri)
                try {
                    val telecomManager =
                        context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        /*                        val toastMsg = context.getString(R.string.missing_call_phone_permission)
                                                Toast.makeText(
                                                    context,
                                                    toastMsg,
                                                    Toast.LENGTH_LONG
                                                ).show()*/
                        // show ui
                        return false
                    }
                    telecomManager.placeCall(uri, extras)
                    return true
                    //ContextCompat.startActivity(context, callIntent, null)
                } catch (e: Exception) {
                    val seriousErrorTitle = context.getString(R.string.serious_error_capital)
                    val toastMsg = "$seriousErrorTitle (${e.message})"
                    /*                    Toast.makeText(
                                            context,
                                            toastMsg,
                                            Toast.LENGTH_LONG
                                        ).show()*/
                    showCustomToastDialog(context, toastMsg)
                    return false
                }

            } else { // We can make a call - but it will get out through the default dialer app, which is not us in this case:

                // load ui to say that
                return false
            }
        } else { // We cannot make a call - ask for permissions:
            /*            val toastMsg = "No call phone permission"
                        Toast.makeText(
                            context,
                            toastMsg,
                            Toast.LENGTH_LONG
                        ).show()*/
            // show in ui
            //askForCallPhonePermission(activity, context, requestPermissionLauncher)
            return false

            /*            val overlayFragment = PermissionsAlertFragment()
                        val args = Bundle().apply {
                            putBoolean("IS_MAKE_CALL_PERMISSION", true)
                        }
                        overlayFragment.arguments = args
                        overlayFragment.show(hostFragment, "PermissionMissingAlertDialogTag")*/
        }
        return false
    }


    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("fragmentToShow")?.let { fragmentToShow ->
            if (fragmentToShow == "MyFragmentIdentifier") {
                val fragmentManager = supportFragmentManager
                // Check if the fragment already exists by its tag.
                val existingFragment = fragmentManager.findFragmentByTag("ACTIVE_CALL_TAG")
                if (existingFragment != null) {
                    // Bring the existing fragment to the front.
                    fragmentManager.popBackStack("ACTIVE_CALL_TAG", 0)
                } else {
                    // Create a new instance if it's not found.
                    /*                    val newFragment = MyFragment()
                                        fragmentManager.beginTransaction()
                                            .replace(R.id.fragment_container, newFragment, "ACTIVE_CALL_TAG")
                                            .addToBackStack("ACTIVE_CALL_TAG")
                                            .commit()*/
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()

        // stopRingtone()  // Stop the ringtone when the activity stops
    }

    override fun onDestroy() {
        super.onDestroy()
        //  mediaPlayer.release()
        OpenScreensStatus.eventScreenActivityIsOpen.value = false
        CallActivity.callEndedShouldCloseActivityEvent.value = false
        // Ensure the view is removed when the Activity is destroyed
        if (currFragmentView !== null && currFragmentView!!.isAttachedToWindow) {
            windowManager.removeView(currFragmentView)
            currFragmentView = null
        }
    }
}
