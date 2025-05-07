package com.nirotem.simplecall

import android.Manifest
import android.Manifest.permission.READ_CONTACTS
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nirotem.simplecall.activities.EventScreenActivity
import com.nirotem.simplecall.helpers.DBHelper.isNumberInContacts

import com.nirotem.simplecall.helpers.DBHelper.isNumberInFavorites
import com.nirotem.simplecall.helpers.SharedPreferencesCache.isGlobalAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAutoAnswerPhoneNumber
import com.nirotem.simplecall.managers.SoundPoolManager
import android.os.Handler
import android.os.Looper
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import com.nirotem.simplecall.OngoingCall.formatPhoneNumberWithLib
import com.nirotem.simplecall.helpers.DBHelper.isNumberBlocked
import com.nirotem.simplecall.helpers.SharedPreferencesCache
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadLastExternalCallDate
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveCallActivityLoadedTimeStamp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveLastCallError
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowCallWaiting
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.statuses.AllowAnswerCallsEnum
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.SettingsStatus
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

class InCallServiceManager : InCallService() {
    // private val TOGGLE_SPEAKER_ACTION = "com.example.simplecall.ACTION_TOGGLE_SPEAKER"
    private lateinit var speakerToggleReceiver: BroadcastReceiver
    private lateinit var activeCallDisplayManager: ActiveCallDisplayManager
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "simplecall_active_call_channel"
    private val CHANNEL_NAME = "Incoming Call Notifications"
    private val CHANNEL_DESCRIPTION = "Notifications for ongoing calls"
    private var ringingStartTime: Long = 0
    private var ringCount: Int = 0
    private val ringDuration = 5000L // משך צלצול ממוצע במילישניות (למשל, 5 שניות)
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var onCallAddedContext: Context
    private val speakerObserver = Observer<Boolean> { shouldToggleSpeaker ->
        Log.d(TAG, "Received Speaker Toggle Request (shouldToggleSpeaker = $shouldToggleSpeaker)")
        if (shouldToggleSpeaker) {
            toggleSpeakerphoneThroughCall(OngoingCall.shouldToggleSpeakerOnOff)
            OngoingCall.shouldToggleSpeaker.value = false
            OngoingCall.shouldUpdateSpeakerState.value = true
        }
    }

    object BroadcastConstants {
        const val ACTION_ERROR_MESSAGE_EVENT = "com.nirotem.simplecall.ACTION_ERROR_MESSAGE_EVENT"
        const val EXTRA_DATA = "event_message"
    }

    companion object {
        private const val TAG = "SimplyCall - InCallServiceManager"
        const val TOGGLE_SPEAKER_ACTION = "com.example.simplecall.ACTION_TOGGLE_SPEAKER"
        const val EXTRA_ENABLE_SPEAKER = "com.example.simplecall.EXTRA_ENABLE_SPEAKER"
        const val SPEAKER_STATUS_UPDATED_ACTION =
            "com.example.simplecall.SPEAKER_STATUS_UPDATED_ACTION"

        /*   private var instance: InCallServiceManager? = null

           fun getInstance(): InCallServiceManager? {
               return instance
           }*/

        private const val ONGOING_CALL_NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        Log.d(
            "SimplyCall - InCallServiceManager",
            "onCreate, OngoingCall.call = ${OngoingCall.call})"
        )

        super.onCreate()

        /**
         *  Although this happens on the main activity as well
         *  we don't know if it would run before an incoming call
         */
        SoundPoolManager.initialize(this)


      //  createNotificationChannelForLockScreen(this)

        //registerSpeakerToggleReceiver(this)
/*        OngoingCall.shouldToggleSpeaker.observe(fragmentViewLifecycleOwner) { allowOpening ->

        }*/
        OngoingCall.shouldToggleSpeaker.observeForever(speakerObserver)


        // Initialize the late init variable
        activeCallDisplayManager = ActiveCallDisplayManager(this)
    }



    override fun onCallAdded(call: Call) {
        try {
            Log.d(
                "SimplyCall - InCallServiceManager",
                "onCallAdded (call = $call, OngoingCall.call = ${OngoingCall.call})"
            )
            //showIncomingCallNotification2(this)
            handleCallAdded(call)
        } catch (callException: Exception) {
            Log.e(
                "SimplyCall - InCallServiceManager",
                "onCallAdded error ($callException)"
            )

            try {
                saveLastCallError(onCallAddedContext, callException.message)
            }
            catch (saveToLogException: Exception) {

            }

            //  We have an error in the Call, something happened and maybe the call is going on without
            // a controlling active call screen that can end it etc.
            // This is very dangerous and un-excepted so we would disconnect all potential calls
            // and apologize to the user in a message
            try {


                val message =
                    getString(R.string.a_serious_error_has_occurred_call_cannot_continue) + " " + getString(
                        R.string.error_is_capital,
                        callException.message
                    )
                activeCallDisplayManager.handleErrorInCall(
                    this,
                    message
                ) // We need to make sure user got some details about the error
            } catch (hangupException: Exception) {

            }

/*            Toast.makeText(
                this,
                getString(R.string.a_serious_error_has_occurred_call_cannot_continue),
                Toast.LENGTH_LONG
            ).show()*/
            showCustomToastDialog(this, getString(R.string.a_serious_error_has_occurred_call_cannot_continue))

/*            Toast.makeText(
                this,
                getString(R.string.error_is_capital, callException.message),
                Toast.LENGTH_LONG
            ).show()*/

            Handler(Looper.getMainLooper()).postDelayed({
                showCustomToastDialog(this, getString(R.string.error_is_capital, callException.message))
            }, 4000)



            /*            val themedContext = ContextThemeWrapper(this, R.style.Theme_SimpleCall)
                        AlertDialog.Builder(themedContext)
                            .setTitle("Error")
                            .setMessage(getString(R.string.error_is_capital, e.message))
                            .setPositiveButton("sdf") { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setCancelable(false)
                            .show()*/

            // showPermissionManualExplanationDialog(this, getString(R.string.error_is_capital, e.message))
        }
    }

    /*
    באופן עקרוני, הערכים הללו הם קבועים ("const") שמוגדרים כחלק מהחוזה של הממשק, והם לא צפויים להשתנות. לפי התיעוד, הערכים הם בדרך כלל:
CALL_DIRECTION_INCOMING = 1
CALL_DIRECTION_OUTGOING = 2
(לעיתים קיים גם CALL_DIRECTION_UNKNOWN = 0)
     */
    private fun isCallOutgoing(call: Call): Boolean {
        val CALL_DIRECTION_INCOMING = 0 // Call.Details.CALL_DIRECTION_INCOMING
        val CALL_DIRECTION_OUTGOING = 1 // Call.Details.CALL_DIRECTION_OUTGOING
      //  val CALL_DIRECTION_UNKNOWN = 0

        // We must check like this - since an outgoing call can happen from other app, although this app is the default app,
        // Like Get app. And then the app thinks it's incoming call.

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 28 ומעלה: שימוש ישיר בכיוון השיחה
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return when (call.details.callDirection) {
                //Call.Details.CALL_DIRECTION_INCOMING -> {
                CALL_DIRECTION_INCOMING -> {
                    false
                }
                // Call.Details.CALL_DIRECTION_OUTGOING -> {
                CALL_DIRECTION_OUTGOING -> {
                    true
                }

                else -> {
                    OutgoingCall.isCalling && !OutgoingCall.wasAnswered // because it could be call waiting while OutgoingCall.isCalling=true
                }
            }
           // }
        } else {
            // גרסאות לפני API 28: ננסה לקבוע את כיוון השיחה על פי מצב השיחה
            return when (call.state) {
                Call.STATE_RINGING -> {
                    // מצב Ringing לרוב מעיד על שיחה נכנסת
                    false
                }

                Call.STATE_DIALING -> {
                    // מצב Dialing מעיד על שיחה יוצאת
                    true
                }

                else -> {
                    OutgoingCall.isCalling && !OutgoingCall.wasAnswered // because it could be call waiting while OutgoingCall.isCalling=true
                }
            }
        }
    }

    fun handleCallAdded(call: Call) {
        CallActivity.criticalErrorEvent.value = false // no reason not to reset this event here
        CallActivity.criticalErrorEventMessage = ""
        CallActivity.callEndedShouldCloseActivityEvent.value = false // no reason not to reset this event here

        val phoneNumber = call.details.handle?.schemeSpecificPart

        val isOutgoing = isCallOutgoing(call)  // OutgoingCall.isCalling && !OutgoingCall.wasAnswered // because it could be call waiting while OutgoingCall.isCalling=true

        val isCallWaiting = (OngoingCall.call != null || OutgoingCall.call != null) && !isOutgoing
        onCallAddedContext = this
        Log.d(
            "SimplyCall - InCallServiceManager",
            "handleCallAdded (isCallWaiting = $isCallWaiting, OngoingCall.call = ${OngoingCall.call})"
        )

        // Allow answering call
        var shouldAnswerCall = true
        if (!isOutgoing) {
            val answerCallsMode = loadAllowAnswerCallsEnum(onCallAddedContext)
            if (AllowAnswerCallsEnum.valueOf(answerCallsMode.toString()) == AllowAnswerCallsEnum.FROM_EVERYONE) {
                shouldAnswerCall = true
            } else if (AllowAnswerCallsEnum.valueOf(answerCallsMode.toString()) == AllowAnswerCallsEnum.IDENTIFIED_ONLY) {
                shouldAnswerCall = !phoneNumber.isNullOrEmpty() // must have a number
            } else if (AllowAnswerCallsEnum.valueOf(answerCallsMode.toString()) == AllowAnswerCallsEnum.FAVOURITES_ONLY) {
                shouldAnswerCall = isNumberInFavorites(
                    phoneNumber.toString(),
                    onCallAddedContext
                ) // must be in Favourites
            } else if (AllowAnswerCallsEnum.valueOf(answerCallsMode.toString()) == AllowAnswerCallsEnum.CONTACTS_ONLY) {
                shouldAnswerCall = isNumberInContacts(
                    phoneNumber.toString(),
                    onCallAddedContext
                ) // must be a Contact
            } else if (AllowAnswerCallsEnum.valueOf(answerCallsMode.toString()) == AllowAnswerCallsEnum.NO_ONE) {
                shouldAnswerCall = false
            }

            // Allow call waiting
            if (isCallWaiting && shouldAnswerCall) {
                shouldAnswerCall = shouldAllowCallWaiting(onCallAddedContext)
            }

            // Check if number is blocked
            // Can do it only if app is the default dialer app (otherwise it would not be able to answer the call anyway
            if (shouldAnswerCall && PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                shouldAnswerCall = (!isNumberBlocked(phoneNumber.toString(), onCallAddedContext))
                //Log.d("BLA BLA BLA", "isNumberBlocked() = $isNumberBlocked")
            }
        }

        if (shouldAnswerCall) {
            var shouldAutoAnswer: Boolean
            if (isOutgoing || isCallWaiting) {
                shouldAutoAnswer = false
            } else { // Only for Incoming call - can auto answer
                shouldAutoAnswer = isGlobalAutoAnswer(onCallAddedContext)
                if (!shouldAutoAnswer) {
                    shouldAutoAnswer =
                        shouldAutoAnswerPhoneNumber(phoneNumber.toString(), onCallAddedContext)
                }
            }

            // Register a callback to monitor state changes
            val callCallback = object : Call.Callback() {
                override fun onStateChanged(call: Call, state: Int) {
                    when (state) {
                        Call.STATE_RINGING -> {
                            Log.d(
                                "SimplyCall - InCallServiceManager",
                                "Call is ringing (onStateChanged)"
                            )

                            // Launch your custom incoming call UI
                            //loadActivity(context, number, false)
                            // You can perform actions here when the call starts ringing
                        }

                        Call.STATE_ACTIVE -> {
                            //stopRingTimer()
                            Log.d("SimplyCall - InCallServiceManager", "Call is active (answered)")
                            // EventScreenActivity().finish() // Finish the call screen activity
                            //loadActivity(context, number, true)
                            // You can perform actions here when the call is answered
                            if (isOutgoing) {
                                OutgoingCall.otherCallerAnswered.value = true
                                OutgoingCall.wasAnswered = true
                                OutgoingCall.isCalling = false
                                createNotificationChannel()
                                createCallNotificationForTray()
                            } else if (isCallWaiting) {
                                WaitingCall.replacedWithWaitingCall = false // important before WaitingCall.startedRinging.value = false
                                WaitingCall.wasAnswered =
                                    true // important before ending the started ringing

                                // This triggers ActiveCallFragment to replace the Ongoing or Outgoing call with the Waiting Call:
                                WaitingCall.startedRinging.value = false // important after wasAnswered = true

                                // This should be enough to trigger ActiveCallFragment that is listening

/*                                activeCallDisplayManager.handleAcceptCall(
                                    onCallAddedContext,
                                    isCallWaiting = false,
                                    isOutgoing = false
                                )*/
                            } else {
                                handleAnsweringCall(onCallAddedContext)
                            }

                        }

                        Call.STATE_DISCONNECTED -> {

                            //stopRingTimer()

                            // You can perform actions here when the call ends or is disconnected
                            // Unregister the callback
                            try {
                                if (isOutgoing && (!WaitingCall.replacedWithWaitingCall)) {
                                    OutgoingCall.isCalling = false
                                    OutgoingCall.callWasDisconnected.value = true
                                }
                                /*                            WaitingCall.call = call
                                                            WaitingCall.wasAnswered = false
                                                            WaitingCall.onHold = false
                                                            WaitingCall.callWasDisconnectedManually = false
                                                            WaitingCall.phoneNumberOrContact = "+972543050638"
                                                            WaitingCall.conference = false
                                                            activeCallDisplayManager.handleIncomingCall(onCallAddedContext, true)*/

                                if (isOutgoing && (!WaitingCall.replacedWithWaitingCall)) {
                                    //if  { // we don't want to close anything if the active call was just replaced to waiting call
                                        activeCallDisplayManager.handleDisconnect(
                                            onCallAddedContext,
                                            isCallWaiting,
                                            true
                                        )
                                    //}
                                    OutgoingCall.call = null
                                } else if (!isCallWaiting && (!WaitingCall.replacedWithWaitingCall)) {
                                  //  if (!OngoingCall.replacedWithWaitingCall) { // we don't want to close anything if the active call was just replaced to waiting call
                                        activeCallDisplayManager.handleDisconnect(
                                            onCallAddedContext,
                                            false,
                                            false
                                        )
                                   // }
                                    OngoingCall.call = null
                                } else { // Also for WaitingCall.replacedWithWaitingCall - meaning Ongoing or Outgoing call was replaced by the WaitingCall call and now we disconnecting WatingCall
                                    WaitingCall.replacedWithWaitingCall = false
                                    WaitingCall.startedRinging.value =
                                        false // this is enough to trigger ActiveCallFragment that is listening
                                    WaitingCall.call = null
                                }
                            } catch (error: Error) {
                                if (isOutgoing) {
                                    OutgoingCall.isCalling = false
                                    OutgoingCall.call = null
                                } else if (!isCallWaiting) {
                                    OngoingCall.call = null
                                } else {
                                    WaitingCall.replacedWithWaitingCall = false
                                    WaitingCall.startedRinging.value = false
                                    WaitingCall.call = null
                                }
                                Log.d(
                                    "SimplyCall - InCallServiceManager",
                                    "Call.STATE_DISCONNECTED -> { - Error ($error"
                                )
                            }
                        }

                        Call.STATE_HOLDING -> {
                            stopRingTimer()
                            Log.d("SimplyCall - InCallServiceManager", "Call is on hold")
                            // Actions when call is on hold (optional)
                            // TODO: flag that call is on hold
                            if (isOutgoing) {
                                if (OutgoingCall.conference) {
                                    OutgoingCall.call?.unhold() // In conference - no call should be on hold
                                } else {
                                    OutgoingCall.onHold = true
                                }
                            } else if (isCallWaiting) {
                                if (WaitingCall.conference) {
                                    WaitingCall.call?.unhold() // In conference - no call should be on hold
                                } else {
                                    WaitingCall.onHold = true
                                }

                            } else {
                                if (OngoingCall.conference) {
                                    OngoingCall.call?.unhold() // In conference - no call should be on hold
                                } else {
                                    OngoingCall.onHold = true
                                }
                            }
                        }

                        else -> {
                            Log.d("SimplyCall - InCallServiceManager", "Call state changed: $state")
                        }
                    }
                }
            }

            Log.d("SimplyCall - InCallServiceManager", "Call added4: ${call.details}")

            call.registerCallback(callCallback)



            handleAddedCall(call, isCallWaiting, onCallAddedContext, isOutgoing, shouldAutoAnswer)

            if (shouldAutoAnswer && !isCallWaiting && !isOutgoing && !OngoingCall.wasAnswered) {
                startRingTimer()
                // AutoAnswer:
                //val videoState = if (supportsVideoCall()) {
                //    VideoProfile.STATE_BIDIRECTIONAL
                //} else {
                //    VideoProfile.STATE_AUDIO_ONLY
                //}
                //stopRingtone(false)

            }
        } else { // We don't take this kind of call
            call.disconnect()
        }
    }

    override fun onCallRemoved(call: Call) {
        OngoingCall.call = null
        with(NotificationManagerCompat.from(this)) {
            cancel(NOTIFICATION_ID)
        }
    }

    private fun handleAnsweringCall(context: Context) {
        OngoingCall.wasAnswered = true
        activeCallDisplayManager.handleAcceptCall(
            context,
            isCallWaiting = false,
            isOutgoing = false
        )
        createNotificationChannel()
        createCallNotificationForTray()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Check if the channel already exists
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)

            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT // Set to DEFAULT or LOWER
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableLights(true)
                    enableVibration(true)
                    // Optionally disable sound to prevent pop-ups
                    setSound(null, null)
                }

                notificationManager.createNotificationChannel(channel)
            } else {
                // Optionally, update the channel's importance if needed
                if (existingChannel.importance != NotificationManager.IMPORTANCE_DEFAULT) {
                    existingChannel.importance = NotificationManager.IMPORTANCE_DEFAULT
                    notificationManager.createNotificationChannel(existingChannel)
                }
            }
        }
    }

    private fun createCallNotificationForTray() {
        val PERMISSION_REQUEST_CODE = 1001
        val intent = Intent(this, EventScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("IN_MIDDLE_OF_CALL", true)
            putExtra("CALL_ANSWERED", true)
        }

        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        /*        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.goldappiconphoneblack)
                    .setContentTitle("Active call")
                    .setContentText("Click to go back to call")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setContentIntent(pendingIntent) // ה-PendingIntent שמחזיר את האפליקציה
                    .setAutoCancel(true) // הנוטיפיקציה תימחק לאחר לחיצה
                    .build()*/

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.goldappiconphoneblack) // Replace with your icon
            .setContentTitle(getString(R.string.active_call_capital))
            .setContentText(getString(R.string.click_to_go_back_to_call))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent) // ה-PendingIntent שמחזיר את האפליקציה
            .setOngoing(true) // Makes the notification persistent
            .setAutoCancel(false) // Prevents the notification from being dismissed on tap

        val thisContext = this

        with(NotificationManagerCompat.from(this)) {
            // Check for POST_NOTIFICATIONS permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        thisContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is not granted; request it

                    return
                }
            }

            // Permission is granted; post the notification
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun registerSpeakerToggleReceiver(context: Context) {
        speakerToggleReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TOGGLE_SPEAKER_ACTION) {
                    val enable = intent.getBooleanExtra(EXTRA_ENABLE_SPEAKER, false)
                    Log.d(TAG, "Received TOGGLE_SPEAKER_ACTION: enable=$enable")
                    toggleSpeakerphoneThroughCall(enable)
                    if (context != null) {
                        sendSpeakerphoneUpdatedEvent(context, enable)
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(TOGGLE_SPEAKER_ACTION)
        }
        ContextCompat.registerReceiver(
            this, // Context
            speakerToggleReceiver, // The BroadcastReceiver
            filter, // The IntentFilter
            ContextCompat.RECEIVER_NOT_EXPORTED // Export state
        )

        //Log.d(TAG, "SpeakerToggleReceiver registered")
    }

    private fun sendSpeakerphoneUpdatedEvent(context: Context, enable: Boolean) {
        //toggleSpeakerphone(enable, context)

        val intent = Intent(SPEAKER_STATUS_UPDATED_ACTION)
        intent.putExtra(EXTRA_ENABLE_SPEAKER, enable)
        context.sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        OngoingCall.shouldToggleSpeaker.removeObserver(speakerObserver)
       // unregisterReceiver(speakerToggleReceiver)
    }

    fun playDefaultRingtone(context: Context) {
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        val ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
        ringtone.play()
    }

    private fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber.isNullOrEmpty()) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
            }
        }
        return null
    }

    // Main method to handle the incoming call
    private fun handleAddedCall(
        call: Call,
        isCallWaiting: Boolean,
        context: Context,
        isOutgoing: Boolean,
        isAutoAnswer: Boolean
    ) {
        val numberOrContact = call.details.handle?.schemeSpecificPart
        Log.d(
            "SimplyCall - InCallServiceManager",
            "Handle (${if (isOutgoing) "outgoing" else "incoming"} call from number ($call)"
        )
        val currentLocale = Locale.getDefault()
        val languageEnum = LanguagesEnum.fromCode(currentLocale.language)

        if (isOutgoing) {
           // setCallForegroundNotification(context, false) // false because dialing is made when the app is loaded and in focus
            OutgoingCall.call = call
            OutgoingCall.wasAnswered = false
            OutgoingCall.otherCallerAnswered.value = false
            OutgoingCall.onHold = false
            OutgoingCall.isSpeakerOn = false
            OutgoingCall.callWasDisconnectedManually = false
            OutgoingCall.contactExistsForPhoneNum = false

            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                if (numberOrContact.isNullOrEmpty()) { // this should never be the case when we are the caller
                    OutgoingCall.phoneNumberOrContact = context.getString(R.string.unknown_caller)
                    OutgoingCall.contactExistsForPhoneNum = false
                    CallActivity.originalPhoneNumber = null
                }
                else {
                    CallActivity.originalPhoneNumber = numberOrContact
                    val contactName = getContactName(context, numberOrContact)
                    OutgoingCall.phoneNumberOrContact = if (contactName != null) contactName else numberOrContact
                    if (contactName == null) {
                       // OutgoingCall.phoneNumberOrContact = numberOrContact // context.getString(R.string.unknown_caller)
                        OutgoingCall.contactExistsForPhoneNum = false
                        OutgoingCall.phoneNumberOrContact = formatPhoneNumberWithLib(numberOrContact, languageEnum.region)
                    }
                    else {
                        OutgoingCall.contactExistsForPhoneNum = OutgoingCall.phoneNumberOrContact != numberOrContact
                    }
                }
            }
            else if (!numberOrContact.isNullOrEmpty()) {
                CallActivity.originalPhoneNumber = numberOrContact
                val contactName = SharedPreferencesCache.getContactNameFromPhoneNumberInJson(context, numberOrContact)
                if (contactName != null) {
                    OutgoingCall.contactExistsForPhoneNum = true
                    OutgoingCall.phoneNumberOrContact = contactName
                }
                else {
                    OutgoingCall.contactExistsForPhoneNum = false
                    OutgoingCall.phoneNumberOrContact = formatPhoneNumberWithLib(numberOrContact, languageEnum.region)
                }
            } else {
                CallActivity.originalPhoneNumber = null
                OutgoingCall.contactExistsForPhoneNum = false
                OutgoingCall.phoneNumberOrContact = context.getString(R.string.unknown_caller) // formatPhoneNumberWithLib(numberOrContact, languageEnum.region)
            }
            OutgoingCall.conference = false
            WaitingCall.startedRinging.value = false // resting just in case

            var secondsDiff: Long = 1000 // show Active Call (if not sure)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                // קבלת התאריך העכשווי ועיצובו למחרוזת בהתאם לתבנית
                val lastExternalCallRequest = loadLastExternalCallDate(context)
                val now = LocalDateTime.now().format(formatter)
                if (!lastExternalCallRequest.isNullOrEmpty()) {
                    secondsDiff = getSecondsDifference(lastExternalCallRequest, now)
                }
            }

            if (secondsDiff > 1 || secondsDiff < -1) { // otherwise - we just already loaded the Active Call screen from external call so we should NOT load it again
                activeCallDisplayManager.handleOutgoingCall(context)
            }
        } else if (!isCallWaiting) {
            OngoingCall.call = call
            OngoingCall.wasAnswered = false
            OngoingCall.isSpeakerOn = false
            OngoingCall.autoAnwered.value = false
            OngoingCall.onHold = false
            OngoingCall.shouldToggleSpeaker.value = false
            OngoingCall.shouldToggleSpeakerOnOff = false
            OngoingCall.shouldUpdateSpeakerState.value = false
            OngoingCall.callWasDisconnectedManually = false

            if (numberOrContact.isNullOrEmpty()) {
                OngoingCall.phoneNumberOrContact = context.getString(R.string.unknown_caller)
                CallActivity.originalPhoneNumber = null
            }
            else { // see if we have read contacts permission:
                CallActivity.originalPhoneNumber = numberOrContact // save to try and fetch Contact's image
                if (ContextCompat.checkSelfPermission(
                        context,
                        READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) { // get contact through db
                    val contactName = getContactName(context, numberOrContact)
                    OngoingCall.phoneNumberOrContact = if (contactName != null) contactName else formatPhoneNumberWithLib(numberOrContact, languageEnum.region)
                } else { // no permission to get contact - we'll use our own inner contacts db
                    val contactName = SharedPreferencesCache.getContactNameFromPhoneNumberInJson(context, numberOrContact)
                    if (contactName != null) {
                        OngoingCall.phoneNumberOrContact = contactName // show contact
                    }
                    else {
                        OngoingCall.phoneNumberOrContact = formatPhoneNumberWithLib(numberOrContact, languageEnum.region)

                    }
                }
            }
            //if (PermissionsStatus.readContactsPermissionGranted.value == true) {

            OngoingCall.conference = false
            WaitingCall.startedRinging.value = false // resting just in case
            activeCallDisplayManager.handleIncomingCall(context, false, isAutoAnswer)

            // Start timer - beucase this is the only way to know if the activity was really loaded or we need to popup notification instead
            try {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (OpenScreensStatus.eventScreenActivityIsOpen.value != true) {
                        setCallForegroundNotification(context, true) // if it failed to show activity we need to pop the notification and not just show it in the tray
                    }
                }, 300)
            }
            catch (e: Exception) {

            }
        } else { // call waiting
            /*
            * then OngoingCall.hangup()
            * and then OngoingCall.call = call
            */
            WaitingCall.call = call
            WaitingCall.wasAnswered = false
            WaitingCall.originalPhoneNumber = numberOrContact
            WaitingCall.onHold = false
            WaitingCall.callWasDisconnectedManually = false
            WaitingCall.replacedWithWaitingCall = false
            if (numberOrContact.isNullOrEmpty()) {
                WaitingCall.phoneNumberOrContact = context.getString(R.string.unknown_caller)
            }
            else { // see if we have read contacts permission:
                if (ContextCompat.checkSelfPermission( // this could be not reset yet -> if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                        context,
                        READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    val contactName = getContactName(context, numberOrContact)
                    WaitingCall.phoneNumberOrContact = if (contactName != null) contactName else formatPhoneNumberWithLib(numberOrContact, languageEnum.region)
                } else { // no permission to get contact - we'll use our own inner contacts db
                    val contactName = SharedPreferencesCache.getContactNameFromPhoneNumberInJson(context, numberOrContact)
                    if (contactName != null) {
                        WaitingCall.phoneNumberOrContact = contactName // show contact
                    }
                    else {
                        WaitingCall.phoneNumberOrContact = formatPhoneNumberWithLib(numberOrContact, languageEnum.region)
                      //  WaitingCall.phoneNumberOrContact = numberOrContact // show number
                    }
                }
            }

            WaitingCall.conference = false
            WaitingCall.startedRinging.value =
                true // open the Call Waiting screen through the Active call screen
            //activeCallDisplayManager.handleIncomingCall(context, true)
        }

        saveCallActivityLoadedTimeStamp(this, true) // reset last successful call timestamp

        Log.d(
            "SimplyCall - InCallServiceManager",
            "OngoingCall.call = ${OngoingCall.call?.details}, call = ${call.details}"
        )
    }

    // פונקציה המקבלת שני תאריכים כטקסט ומחזירה את ההבדל ביניהם בשניות
    fun getSecondsDifference(date1Str: String, date2Str: String, pattern: String = "yyyy-MM-dd HH:mm:ss"): Long {
        val formatter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            DateTimeFormatter.ofPattern(pattern)
        } else {
            TODO("VERSION.SDK_INT < O")
        }
        if (formatter == null) {
            return 1000
        }

        val date1 = LocalDateTime.parse(date1Str, formatter)
        val date2 = LocalDateTime.parse(date2Str, formatter)
        return date2.toEpochSecond(ZoneOffset.UTC) - date1.toEpochSecond(ZoneOffset.UTC)
    }

    // Shows the incoming call notification
    fun showIncomingCallNotification(context: Context, call: Call) {
        val intent = Intent(context, EventScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, "CALL_CHANNEL")
            .setContentTitle("Incoming Call")
            .setContentText("Tap to answer")
            .setSmallIcon(R.drawable.avatar_12)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationManager.notify(1, notification)
    }

    // Create the notification channel for incoming call notifications (for API 26 and above)
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "CALL_CHANNEL"
            val channelName = "Call Notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notifications for incoming calls"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    //  private fun toggleSpeakerphone(enable: Boolean) {
    //   toggleSpeaker(enable)

    /*       val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
               // Android 12+ (API 31): Use setCommunicationDevice
               val speakerDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                   .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

               if (enable) {
                   if (speakerDevice != null) {
                       val success = audioManager.setCommunicationDevice(speakerDevice)
                       if (!success) {
                           Log.e(TAG, "Failed to enable speakerphone!")
                       } else {
                           Log.d(TAG, "Speakerphone enabled successfully.")
                       }
                   } else {
                       Log.e(TAG, "No built-in speaker device found.")
                   }
               } else {
                   audioManager.clearCommunicationDevice()
                   Log.d(TAG, "Speakerphone disabled, audio routed back to default.")
               }
           } else {
               // For Android 11 and below, use isSpeakerphoneOn
               audioManager.mode = AudioManager.MODE_IN_CALL
               audioManager.isSpeakerphoneOn = enable
               Log.d(TAG, "Speakerphone toggled: $enable (legacy API)")
           }*/
    //}

    fun toggleSpeakerphoneThroughCall(shouldTurnSpeakerOn: Boolean) {
        try {
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            //val isSpeakerOn: Boolean = false // audioManager.isSpeakerphoneOn()
            val earpiece = CallAudioState.ROUTE_WIRED_OR_EARPIECE
            val speaker = CallAudioState.ROUTE_SPEAKER

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                setAudioRoute(if (shouldTurnSpeakerOn) speaker else earpiece)
            } else {
                audioManager.isSpeakerphoneOn = !shouldTurnSpeakerOn
            }

            Log.d(TAG, "Speakerphone toggled: $shouldTurnSpeakerOn (legacy API)")
        }
        catch (e: Exception) {
            Log.d(TAG, "ERROR: Speakerphone toggled: $shouldTurnSpeakerOn (legacy API). (Error=${e.message})")
        }

    }

    // Checks if the call supports video
    private fun supportsVideoCall(): Boolean {
        val capabilities = OngoingCall.call?.details?.callCapabilities
        return (capabilities?.and(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX) != 0) &&
                (capabilities?.and(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX) != 0)
    }

    private val ringRunnable = object : Runnable {
        override fun run() {
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - ringingStartTime
            ringCount = (elapsed / ringDuration).toInt()
            Log.d(TAG, "Number of rings: $ringCount")
            if (ringCount > 1) { // starts from 0
                OngoingCall.autoAnwered.value = true
               // OngoingCall.answer(VideoProfile.STATE_AUDIO_ONLY)

               // handleAnsweringCall(onCallAddedContext)
            }
            else {
                handler.postDelayed(this, ringDuration)
            }
        }
    }

    private fun startRingTimer() {
        ringingStartTime = System.currentTimeMillis()

        handler.post(ringRunnable)
        Log.d(TAG, "startRingTimer()")
    }

    private fun stopRingTimer() {
        handler.removeCallbacks(ringRunnable)
        Log.d(TAG, "Final ring count: $ringCount")
    }

    private fun setCallForegroundNotification(context: Context, shouldShowNotificationWhenLoaded: Boolean) { // or show it in the tray without jumping to the user
        val channelId = "call_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationChannel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                channelId,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            )
        } else {
            null
        }
        if (notificationChannel != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val notificationIntent = Intent(context, EventScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIconBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.goldappiconphoneblack)


        val notification = Notification.Builder(context, "call_channel")
            .setSmallIcon(R.drawable.goldappiconphoneblack)
            .setLargeIcon(largeIconBitmap)
            .setContentTitle( getString(R.string.incoming_call))
            .setContentText(getString(R.string.tap_to_open_app))
            .setStyle(
                Notification.BigTextStyle()
                    .bigText(getString(R.string.the_app_requires_permission_to_run_in_the_background))
            )
            .setContentIntent(pendingIntent)
            .setCategory(Notification.CATEGORY_CALL)
            .setPriority(Notification.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            //.setFullScreenIntent(pendingIntent, shouldShowNotificationWhenLoaded)  // <-- Crucial for bringing activity to foreground
            .build()

        //val ONGOING_CALL_NOTIFICATION_ID = 1001
        startForeground(ONGOING_CALL_NOTIFICATION_ID, notification)
    }

    fun showIncomingCallNotification2(context: Context) {
        val fullScreenIntent = Intent(context, EventScreenActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "incoming_call_channel")
            .setSmallIcon(R.drawable.logotransparent)
            .setContentTitle("שיחה נכנסת")
            .setContentText("לחץ כדי לפתוח")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "NO PERMISSION", LENGTH_LONG).show()
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        notificationManager.notify(999, notification)
    }

    fun createNotificationChannelForLockScreen(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "incoming_call_channel",
                "שיחות נכנסות",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /*    @SuppressLint("NewApi")
        fun setSpeakerRoute(connection: Connection, enable: Boolean) {
            connection.setAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        }*/
}

