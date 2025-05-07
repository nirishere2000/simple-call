package com.nirotem.simplecall

import android.Manifest.permission.READ_CONTACTS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.Call
import android.telecom.VideoProfile
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.nirotem.simplecall.activities.EventScreenActivity
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadIsAppLoaded
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.SoundPoolManager

class ActiveCallDisplayManager(context: Context) {
    companion object {
        // var activeCall: Call? = null
        var canDrawOverlays: Boolean = false // settingCanDrawOverlaysPermissionGranted
    }

    private val windowManager: WindowManager by lazy {
        serviceContext?.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    private var incomingCallCustomView: View? = null
    private var waitingCallCustomView: View? = null
    private var activeCallCustomView: View? = null
    private var currentCallIsCallWaiting: Boolean = false
    private var isXiaomi: Boolean = false
    private var appIsLoaded: Boolean = false
    private var serviceContext: Context? = context
    private var shouldPlayVoiceSounds = context.resources.getBoolean(R.bool.playVoiceSound)
    private var eventScreenActivityAlreadyRunning = false
    //private var incomingRingtone: Ringtone? = null

    fun handleAcceptCall(context: Context, isCallWaiting: Boolean, isOutgoing: Boolean) {
        Log.d(
            "SimplyCall - ActiveCallDisplayManager",
            "handleAcceptCall (isCallWaiting = $isCallWaiting), (isOutgoing = $isOutgoing)"
        )
        if (isOutgoing) { // we did not set this info in handleIncoming
            serviceContext = context
            canDrawOverlays = Settings.canDrawOverlays(context)
            isXiaomi = (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true))
            appIsLoaded = loadIsAppLoaded(context)
            currentCallIsCallWaiting = isCallWaiting
        }
        // canDrawOverlays = Settings.canDrawOverlays(context)

        if (canDrawOverlays || isXiaomi || appIsLoaded) {
            if (isOutgoing) {
                showCallScreenThroughActivity(
                    context,
                    isCallWaiting = false,
                    isOutgoing = true,
                    isAutoAnswer = false // we care about it only before call was answered
                ) // show Outgoing number
            } else if (isCallWaiting) {
                if (WaitingCall.conference) { // Load Conference with both contacts (or numbers)
                    showCallScreenThroughActivity(
                        context,
                        isCallWaiting = true,
                        isOutgoing = false,
                        isAutoAnswer = false // we care about it only before call was answered
                    ) // load it as regular active call
                } else { // Just replace the active caller and show active call screen again

                    // WE SHOULD NEVER GET HERE - THERE IS AN EVENT TRIGGERED FROM IN-CALL SERVICE

                    val originalWaitingCallContact = WaitingCall.phoneNumberOrContact
                    WaitingCall.phoneNumberOrContact = OngoingCall.phoneNumberOrContact
                    OngoingCall.phoneNumberOrContact = originalWaitingCallContact
                    val originalWaitingCallCall = WaitingCall.call
                    // Waiting call is never the active call:
                    WaitingCall.call = OngoingCall.call
                    OngoingCall.call = originalWaitingCallCall
                    OngoingCall.onHold = false
                    WaitingCall.onHold = true
                    showCallScreenThroughActivity(
                        context,
                        isCallWaiting = true,
                        isOutgoing = false,
                        isAutoAnswer = false // we care about it only before call was answered
                    ) // load it as regular active call
                    // But now show small blob for the original Ongoing call if it's on hold. And let it be replaced and ended
                }
                //changeActivityCall(context, WaitingCall.phoneNumberOrContact) // The activity will know how to handle WaitingCall.conference
            } else { // load Active Call screen
                removeActivity() // remove Incoming Call screen
                showCallScreenThroughActivity(context, isCallWaiting = false, isOutgoing = false, isAutoAnswer = false)
                //switchToActiveCall(context, OngoingCall.phoneNumberOrContact )
            }

        } else {
/*            Toast.makeText(
                context,
                context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
            ).show()*/
            showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))

            // Request permission if not granted and then show notification or custom UI
            //requestOverlayPermission()
           // if (isCallWaiting) {
                // replace number in active fragment
                //  showCustomCallNotification(context, isCallWaiting)
           // }
           // showActiveCallScreenCustomNotification(context)
        }
    }


    // Shows custom UI for the active call using WindowManager if permission is not granted
    fun showActiveCallScreenCustomNotification(context: Context?) {
        // Set up WindowManager and LayoutParams

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )

        val number = OngoingCall.call?.details?.handle?.schemeSpecificPart
        val customView = LayoutInflater.from(context).inflate(R.layout.fragment_incoming_call, null)
        customView.findViewById<TextView>(R.id.text_incoming_call_contact).text = number

        // Set up decline button click listener
        customView.findViewById<ImageButton>(R.id.declineButton).setOnClickListener {
            Log.d("SimplyCall - ActiveCallDisplayManager", "declineButton clicked")
            OngoingCall.call?.disconnect()
            windowManager.removeView(customView)
        }

        // Set up accept button click listener
        customView.findViewById<ImageButton>(R.id.acceptButton).setOnClickListener {
            handleAccpetCallByUser()
        }

        activeCallCustomView = customView

        // Add the custom view to WindowManager
        windowManager.addView(activeCallCustomView, layoutParams)

    }

    // Shows custom UI for the incoming call using WindowManager if permission is not granted
    fun showCustomCallNotification(context: Context, isCallWaiting: Boolean) {
        // Set up WindowManager and LayoutParams

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )

        val number = OngoingCall.call?.details?.handle?.schemeSpecificPart
        val customView = LayoutInflater.from(context).inflate(R.layout.fragment_incoming_call, null)
        customView.findViewById<TextView>(R.id.text_incoming_call_contact).text = number

        // Set up decline button click listener
        customView.findViewById<ImageButton>(R.id.declineButtonImage).setOnClickListener {
            Log.d("SimplyCall - ActiveCallDisplayManager", "declineButton clicked")
            stopRingtone(isCallWaiting)
            OngoingCall.call?.disconnect()

            windowManager.removeView(customView)
        }

        // Set up accept button click listener
        customView.findViewById<ImageButton>(R.id.acceptButton).setOnClickListener {
            handleAccpetCallByUser()
        }

        incomingCallCustomView = customView

        // Add the custom view to WindowManager
        windowManager.addView(incomingCallCustomView, layoutParams)

        Log.d("SimplyCall - ActiveCallDisplayManager", "showCustomCallNotification - added incomingCallCustomView ($incomingCallCustomView)")

    }

    fun showCustomWaitingCallNotification(context: Context) {
        // Set up WindowManager and LayoutParams

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.OPAQUE
        )

        val number = WaitingCall.call?.details?.handle?.schemeSpecificPart
        val callWaitingCustomView =
            LayoutInflater.from(context).inflate(R.layout.fragment_waiting_call, null)
        callWaitingCustomView.findViewById<TextView>(R.id.text_incoming_call_contact).text = number

        // Set up decline button click listener
        callWaitingCustomView.findViewById<ImageButton>(R.id.declineButton).setOnClickListener {
            Log.d("SimplyCall - ActiveCallDisplayManager", "Call Waiting - declineButton clicked")
            stopRingtone(true)
            WaitingCall.call?.disconnect()
            windowManager.removeView(callWaitingCustomView)
        }

        // Set up accept button click listener
        callWaitingCustomView.findViewById<ImageButton>(R.id.acceptButton).setOnClickListener {
            Log.d("SimplyCall - ActiveCallDisplayManager", "Call Waiting - acceptButton clicked")
            handleAccpetCallWaitingByUser()
        }

        waitingCallCustomView = callWaitingCustomView

        // Add the custom view to WindowManager
        windowManager.addView(waitingCallCustomView, layoutParams)
    }

    private fun getSoundToPlayName(isCallWaiting: Boolean): String {
        val soundToPlay =
            if (isCallWaiting) SoundPoolManager.incomingCallWaitingSoundName else SoundPoolManager.incomingCallSoundName
        return soundToPlay
    }

    fun handleOutgoingCall(context: Context) {
        serviceContext = context
        canDrawOverlays = Settings.canDrawOverlays(context)
        currentCallIsCallWaiting = false
        isXiaomi = (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true))
        appIsLoaded = loadIsAppLoaded(context)

        //startRingtone(getSoundToPlayName(isCallWaiting))

        if (canDrawOverlays || isXiaomi || appIsLoaded) {
            showCallScreenThroughActivity(
                context,
                isCallWaiting = false,
                isOutgoing = true,
                isAutoAnswer = false
            ) // load waiting call fragment

        } else { // We can just give toast:
/*            Toast.makeText(
                context,
                context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
            ).show()*/
            showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
            // Request permission if not granted and then show notification or custom UI
            //requestOverlayPermission()

        }
    }

    fun handleIncomingCall(context: Context, isCallWaiting: Boolean, isAutoAnswer: Boolean) {
        serviceContext = context
        canDrawOverlays = Settings.canDrawOverlays(context)
        currentCallIsCallWaiting = isCallWaiting
        isXiaomi = (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true))
        appIsLoaded = loadIsAppLoaded(context)

        //Log.d("SimplyCall - ActiveCallDisplayManager", "Before canDrawOverlays = $canDrawOverlays")

        //startRingtone(getSoundToPlayName(isCallWaiting))

       // Log.d("SimplyCall - ActiveCallDisplayManager", "After startRingtone")
        Log.d("Simplycall - handleIncomingCall", "handleIncomingCall: appIsLoaded = $appIsLoaded, isXiaomi = $isXiaomi, canDrawOverlays=$canDrawOverlays")


        if (canDrawOverlays || isXiaomi || appIsLoaded) { // for Xiaomi we cannot detect canDrawOverlays
            if (isCallWaiting) {
                //showCallWaiting(context, WaitingCall.phoneNumberOrContact)
                // The way we are working it will create a new instance of the acitivty

                showCallScreenThroughActivity(context, true, false, isAutoAnswer) // load waiting call fragment
            } else {
                showCallScreenThroughActivity(
                    context,
                    currentCallIsCallWaiting,
                    false,
                    isAutoAnswer
                )  // Show activity directly if permission is granted
            }

        } else { // We can just give toast:
/*            Toast.makeText(
                context,
                context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
            ).show()*/
            showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))


            // Request permission if not granted and then show notification or custom UI
            //requestOverlayPermission()
            /*if (isCallWaiting) {
                showCustomWaitingCallNotification(context)
            } else {
                showCustomCallNotification(context, currentCallIsCallWaiting)
            }*/
        }
    }

    fun handleDisconnect(context: Context, isCallWaiting: Boolean, isOutgoing: Boolean) {
        //stopRingtone(isCallWaiting)


        // Remove activity directly if permission is granted
        // if (call was already answered we need to remove Active Call fragment)
        // if cal is call waiting and we did not initiated the call we need to remove waiting call fragment

        // Request permission if not granted and then show notification or custom UI
        //requestOverlayPermission()
        if (isOutgoing) {
            if (OutgoingCall.wasAnswered) {
                if (OutgoingCall.onHold) {
                    // we just showed small icon for it so remove it
                } else {
                    if (canDrawOverlays || isXiaomi || appIsLoaded) {
                        if (WaitingCall.onHold) { // then we need to get it back to active call
                            // load active call screen
                            changeActivityCall(context, WaitingCall.phoneNumberOrContact)
                        } else {
                            removeActivity()
                        }
                    } else { // We can just give toast
/*                        Toast.makeText(
                            context,
                            context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
                        ).show()*/
                        showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
/*                        if (activeCallCustomView !== null) { // call was the active call
                            windowManager.removeView(activeCallCustomView)
                            activeCallCustomView = null
                        }*/

                    }
                }
            } else { // Call was not answered yet - we just remove the Incoming Call UI
                if (canDrawOverlays || isXiaomi || appIsLoaded) {
                    removeActivity()
                } else { // We can just give toast:
/*                    Toast.makeText(
                        context,
                        context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
                        //  windowManager.removeView(incomingCallCustomView)
                   // incomingCallCustomView = null
                }
            }
        } else if (isCallWaiting) {
            // *** NOT DOING ANYTHING - everything is done through listening from ActiveCallFragment
            if (WaitingCall.wasAnswered) {
                if (WaitingCall.onHold) {
                    // we just showed small icon for it so remove it
                } else {
                    if (canDrawOverlays || isXiaomi || appIsLoaded) {
                        if (OngoingCall.onHold) { // then we need to get it back to active call
                            // load active call screen
                         //   changeActivityCall(context, OngoingCall.phoneNumberOrContact)
                        } else {
                        //    removeActivity(context)
                        }
                    } else { // We can just give toast:
/*                        Toast.makeText(
                            context,
                            context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
                        ).show()*/
                        showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
/*                        if (activeCallCustomView !== null) { // waiting call was the active call (although it's still in the waiting call object)
                            windowManager.removeView(activeCallCustomView)
                            activeCallCustomView = null
                        }*/
                    }
                }
            } else { // call was not answered yet, just remove waiting call screen
                if (canDrawOverlays || isXiaomi || appIsLoaded) {
                    Log.d(
                        "SimplyCall - ActiveCallDisplayManager",
                        "Disconnect - canDrawOverlays -> removeActivity(context)"
                    )

                    // make sure activity will be unloaded
                    //removeActivity() don't remove anything besides waiting call screen

                    // reload the active call before the waiting call:
                    // We don't know if it was outgoing or incoming call but we assume it was answered
                    // so we need to go back to it.
                    //removeActivity(context) // remove Waiting call screen
                    //showCallScreenThroughActivity(context, isCallWaiting = false, isOutgoing = (OutgoingCall.call != null), isMiddleOfCall = true)
                    //goBackToActiveCall(context) // Change active fragment back from the incoming waiting call to Active Call screen
                    //removeActivity(context)
                } else { // We can just give toast:
/*                    Toast.makeText(
                        context,
                        context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
/*                    if (waitingCallCustomView !== null) {
                        windowManager.removeView(waitingCallCustomView)
                        waitingCallCustomView = null
                    }*/
                }

            }
        } else {
            if (OngoingCall.wasAnswered) {
                if (OngoingCall.onHold) {
                    // we just showed small icon for it so remove it
                } else {
                    if (canDrawOverlays || isXiaomi || appIsLoaded) {
                        if (WaitingCall.onHold) { // then we need to get it back to active call
                            // load active call screen
                            changeActivityCall(context, WaitingCall.phoneNumberOrContact)
                        } else {
                            removeActivity()
                        }
                    } else {
/*                        Toast.makeText(
                            context,
                            context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
                        ).show()*/
                        showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
                        /*                        if (activeCallCustomView !== null) { // call was the active call
                                                    windowManager.removeView(activeCallCustomView)
                                                    activeCallCustomView = null
                                                }*/

                    }
                }
            } else { // Call was not answered yet - we just remove the Incoming Call UI
                if (canDrawOverlays || isXiaomi || appIsLoaded) {
                    removeActivity()
                } else {
/*                    Toast.makeText(
                        context,
                        context.getString(R.string.app_is_missing_overlay_draw_permission), Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, context.getString(R.string.app_is_missing_overlay_draw_permission))
/*                    windowManager.removeView(incomingCallCustomView)
                    incomingCallCustomView = null*/
                }
            }
        }

        if (shouldPlayVoiceSounds) {
            if (isOutgoing) {
                if (!OutgoingCall.callWasDisconnectedManually) {
                    SoundPoolManager.playSound(SoundPoolManager.callDisconnectedSoundName, false)
                }
            } else if (!isCallWaiting) { // Ongoing call
                if (!OngoingCall.callWasDisconnectedManually) {
                    SoundPoolManager.playSound(SoundPoolManager.callDisconnectedSoundName, false)
                }
            } else { // WaitingCall
                if (WaitingCall.wasAnswered) { // it's not the active call
                    if (!OngoingCall.callWasDisconnectedManually) { // we check OngoingCall
                        SoundPoolManager.playSound(
                            SoundPoolManager.callDisconnectedSoundName,
                            false
                        ) // Call was disconnected voice
                    }
                } else {
                    if (!WaitingCall.callWasDisconnectedManually) {
                        SoundPoolManager.playSound(
                            SoundPoolManager.waitingCallDisconnectedSoundName,
                            false
                        ) // Waiting call was disconnected voice
                    }
                }
            }
        }
    }

/*    private fun switchToActiveCall(context: Context, phoneContactOrNumber: String?) {
        sendBroadcastToActivity(
            context,
            "com.nirotem.simplecall.SWITCH_TO_ACTIVE_CALL",
            "phoneContactOrNumber",
            phoneContactOrNumber
        )
    }*/




    private fun switchToConferenceScreen(context: Context) {
        sendBroadcastToActivity(
            context,
            "com.nirotem.simplecall.ADD_CALLER",
            null,
            null
        )
    }

    private fun changeActivityCall(context: Context, callContactOrNumber: String?) {
        sendBroadcastToActivity(
            context,
            "com.nirotem.simplecall.CHANGE_ACTIVE_CALL",
            "phoneContactOrNumber",
            callContactOrNumber
        )
    }

    private fun removeActivity() {
        Log.d("SimplyCall - ActiveCallDisplayManager", "removeActivity: send REMOVE_ACTIVE_CALL")
        eventScreenActivityAlreadyRunning = false
       // sendBroadcastToActivity(context, "com.nirotem.simplecall.REMOVE_ACTIVE_CALL", null, null)

        CallActivity.callEndedShouldCloseActivityEvent.value = true
    }

    private fun goBackToActiveCall(context: Context) {
        //sendBroadcastToActivity(context, "com.nirotem.simplecall.BACK_TO_ACTIVE_CALL", null, null)
    }

    private fun sendBroadcastToActivity(
        context: Context,
        event: String,
        strParameter: String?,
        strParameterValue: String?
    ) {
        try {
            val intent = Intent(event)
            if (strParameter !== null) {
                intent.putExtra(strParameter, strParameterValue)
            }
            context.sendBroadcast(intent)
        }
        catch (e: Exception) {
            Log.e("SimplyCall - ActiveCallDisplayManager", "Error: $e")
        }
    }

    // Checks if the call supports video
    private fun supportsVideoCall(): Boolean {
        val capabilities = OngoingCall.call?.details?.callCapabilities
        return (capabilities?.and(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_TX) != 0) &&
                (capabilities?.and(Call.Details.CAPABILITY_SUPPORTS_VT_LOCAL_RX) != 0)
    }

    // This function only loads the activity in case of an error
    // to show the whole error even if we don't have UI and can only show Toast (which is too short) from the Incallservice manager error
    fun handleErrorInCall(context: Context, message: String) {
        try {
            // Anyway we need to disconnect calls because user may have no control to end conversation
            try {
                OutgoingCall.hangup()
                OngoingCall.hangup()
                WaitingCall.hangup()
            }
            catch (e: Exception) {

            }

            if (eventScreenActivityAlreadyRunning) { // activity should be up - Just send Broadcast event
/*                sendBroadcastToActivity(
                    context,
                    "com.nirotem.simplecall.ACTION_ERROR_MESSAGE_EVENT",
                    "ONLY_TO_SHOW_ERROR_MESSAGE",
                    message
                )*/
                CallActivity.criticalErrorEventMessage = message
                CallActivity.criticalErrorEvent.value = true
            }
            else { // Try to load the activity with the error to show the whole error


                val intent = Intent(context, EventScreenActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                intent.putExtra("ONLY_SHOW_ERROR_MESSAGE", message)
                context.startActivity(intent) // Take over the screen with activity, and show message
            }
        }
        catch (e: Exception) {
            Log.e("SimplyCall - ActiveCallDisplayManager", "Error: $e")
        }

    }

    private fun showCallScreenThroughActivity(
        context: Context,
        isCallWaiting: Boolean,
        isOutgoing: Boolean,
        isAutoAnswer: Boolean,
        isMiddleOfCall: Boolean = false // then for example we need to not change the speaker.
    ) {
        val intent = Intent(context, EventScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        var phoneNumberOrContact = OngoingCall.phoneNumberOrContact
        var phoneNumberOrContact2 = WaitingCall.phoneNumberOrContact
        var callWasAnswered = OngoingCall.wasAnswered
        if (isOutgoing) {
            phoneNumberOrContact = OutgoingCall.phoneNumberOrContact
            callWasAnswered = OutgoingCall.wasAnswered // WaitingCall.wasAnswered
            if (!OutgoingCall.wasAnswered) {
                intent.putExtra("IS_CALLING", true)
            }
        } else if (isCallWaiting && !WaitingCall.wasAnswered) {
            phoneNumberOrContact = WaitingCall.phoneNumberOrContact
            callWasAnswered = false // WaitingCall.wasAnswered
        } // Otherwise we replaced the Ongoing and WaitingCall contacts - so now WaitingCall is on hold

        if (WaitingCall.conference) {
            intent.putExtra("CALLER_NUMBER2", phoneNumberOrContact2)
        }

        intent.putExtra("ONLY_TO_SHOW_ERROR_MESSAGE", "") // this loads the activity only to show error - so must be empty here
        intent.putExtra("CALLER_NUMBER", phoneNumberOrContact)
        intent.putExtra("AUTO_ANSWER", isAutoAnswer)
        intent.putExtra("CALL_ANSWERED", callWasAnswered)
        intent.putExtra("IS_CALL_WAITING", isCallWaiting)
        // then for example we need to change the speaker according to the original call.
        intent.putExtra("IN_MIDDLE_OF_CALL", isMiddleOfCall)

/*        if (ContextCompat.checkSelfPermission(
                context,
                READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("SimplyCall - ActiveCallDisplayManager", "No READ_CONTACTS permission")
        }*/
        // Activity is running
        eventScreenActivityAlreadyRunning = true

        try {
            Log.d("SimplyCall - ActiveCallDisplayManager", "showCallScreenThroughActivity trying to load activity")
            context.startActivity(intent) // Take over the screen with activity, and this ser
        }
        catch (e: Exception) {
            Log.d("SimplyCall - ActiveCallDisplayManager", "showCallScreenThroughActivity (Error: ${e.message})")
        }
    }


    private fun handleAccpetCallByUser() {
        Log.d("SimplyCall - ActiveCallDisplayManager", "callAccepted!")
/*        val videoState = if (supportsVideoCall()) {
            VideoProfile.STATE_BIDIRECTIONAL
        } else {
            VideoProfile.STATE_AUDIO_ONLY
        }*/
        OngoingCall.answer(VideoProfile.STATE_AUDIO_ONLY)
        stopRingtone(false)
        if (!canDrawOverlays && serviceContext !== null) { // otherwise activity already loaded and will handle this
          //  windowManager.removeView(incomingCallCustomView)


            // showActiveCallScreenCustomNotification(serviceContext)
        }
    }

    private fun handleAccpetCallWaitingByUser() {
        Log.d("SimplyCall - ActiveCallDisplayManager", "callAccepted!")
        // Sound: Stop waiting call sound
        stopRingtone(true)

/*        val videoState = if (supportsVideoCall()) {
            VideoProfile.STATE_BIDIRECTIONAL
        } else {
            VideoProfile.STATE_AUDIO_ONLY
        }*/
        WaitingCall.call?.answer(VideoProfile.STATE_AUDIO_ONLY)
        // OngoingCall should go to hold
        if (!canDrawOverlays) { // otherwise activity already loaded and will handle this
           // windowManager.removeView(waitingCallCustomView)
         //   showActiveCallScreenCustomNotification(serviceContext)
        }
    }

    private fun startRingtone(sound: String) {
        if (shouldPlayVoiceSounds) {
            SoundPoolManager.playSound(sound, true)
        } else { // try default ring sound
            // IT SEEMS THAT THE PHONE ALREADY PLAYS DEFAULT RINGTONE
            // Retrieves the actual URI of the currently set default ringtone
/*            val ringtoneUri: Uri? = RingtoneManager.getActualDefaultRingtoneUri(
                serviceContext,
                RingtoneManager.TYPE_RINGTONE
            )
            ringtoneUri?.let { uri ->
                incomingRingtone = RingtoneManager.getRingtone(serviceContext, uri)
                incomingRingtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                // Start playing the ringtone
                incomingRingtone?.play()
            }*/

        }
    }

    private fun stopRingtone(isCallWaiting: Boolean) {
        if (shouldPlayVoiceSounds) {
            val soundToStop = getSoundToPlayName(isCallWaiting)
            SoundPoolManager.stopSound(soundToStop)
        }
       /* else {
            incomingRingtone?.stop()
        }*/
    }

    protected fun finalize() {
        stopRingtone(false)
        stopRingtone(true)
    }
}