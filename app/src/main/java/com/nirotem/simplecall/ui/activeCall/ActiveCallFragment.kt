package com.nirotem.simplecall.ui.activeCall

import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import android.Manifest
import android.Manifest.permission.READ_CONTACTS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nirotem.simplecall.CallActivity
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentActiveCallBinding
import com.nirotem.simplecall.helpers.isSpeakerphoneOn
import com.nirotem.simplecall.helpers.toggleSpeakerphone
import com.nirotem.simplecall.ui.components.StandaloneDialerDialogFragment
import com.nirotem.simplecall.InCallServiceManager.Companion.TOGGLE_SPEAKER_ACTION
import com.nirotem.simplecall.InCallServiceManager.Companion.EXTRA_ENABLE_SPEAKER
import com.nirotem.simplecall.InCallServiceManager.Companion.SPEAKER_STATUS_UPDATED_ACTION
import com.nirotem.simplecall.WaitingCall
import com.nirotem.simplecall.activities.EventScreenActivity
import com.nirotem.simplecall.helpers.DBHelper.getContactIdFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getContactPhoto
import com.nirotem.simplecall.helpers.SharedPreferencesCache.isInMiddleOfCallIsCalling
import com.nirotem.simplecall.helpers.SharedPreferencesCache.isInMiddleOfCallIsOutgoing
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsInMiddleOfCallIsCalling
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsInMiddleOfCallIsOutgoing
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldCallsStartWithSpeakerOn
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldShowKeypadInActiveCall
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.ui.waitingCall.WaitingCallFragment

class ActiveCallFragment : Fragment() {
    private lateinit var textViewPhoneNumber: TextView
    private var activeCallUIContext: Context? = null
    private var speakerWasAlreadyOnWhenStarted = false
    private var isSpeakerOn: Boolean = false
    private var _binding: FragmentActiveCallBinding? = null
    private var isOutgoingCall = false
    private var userReacted = false
    private var firstSpeakerTry = true
    private var inMiddleOfTryingToChangeSpeaker = false
    private var waitingCallIsRinging = false
    private var callCallback: Call.Callback? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    // private var contactNameExistsForPhoneNum = false
    private val speakerStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == SPEAKER_STATUS_UPDATED_ACTION) {
                    if (context != null) {
                        updateSpeakerState(context)
                    }
                }
            } catch (e: Exception) {
                inMiddleOfTryingToChangeSpeaker = false
                Log.e(
                    TAG,
                    "speakerStatusReceiver onReceiver error (isSpeakerOn = $isSpeakerOn, error: ${e.message})"
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(SPEAKER_STATUS_UPDATED_ACTION)
        ContextCompat.registerReceiver(
            requireContext(), // Context
            speakerStatusReceiver, // The BroadcastReceiver
            filter, // The IntentFilter
            ContextCompat.RECEIVER_NOT_EXPORTED // Export state
        )
    }

    override fun onStop() {
        super.onStop()
        try {
            requireContext().unregisterReceiver(speakerStatusReceiver)
        } catch (_: Exception) {

        }

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
/*                Toast.makeText(
                    context,
                    getString(R.string.permission_was_granted),
                    Toast.LENGTH_LONG
                ).show()*/
                showCustomToastDialog(context, getString(R.string.permission_was_granted))
            } else {
/*                Toast.makeText(
                    this.requireContext(),
                    getString(R.string.read_contacts_permission_was_denied_but_needed),
                    Toast.LENGTH_LONG
                ).show()*/
                showCustomToastDialog(context, getString(R.string.read_contacts_permission_was_denied_but_needed))
            }
        }
    }

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentActiveCallBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Get the phone number from the arguments (passed from the activity)
        val phoneNumber =
            arguments?.getString("CALLER_NUMBER") ?: root.context.getString(R.string.unknown_caller)
        var isCalling = arguments?.getBoolean("IS_CALLING", false) ?: false
        if (isCalling) { // animate and show Calling text
            isOutgoingCall = true
        }
        val startingFromMiddleOfCall =
            arguments?.getBoolean("STARTING_FROM_MIDDLE_OF_CALL", false) ?: false


        try {
           /* SpeakCommandsManager.lastCommand.value = ""
            SpeakCommandsManager.lastCommand.observe(viewLifecycleOwner) {
                val spokeCommand = SpeakCommandsManager.lastCommand.value

                if (spokeCommand != null) {
                    val indexSpeakerLocal = spokeCommand.lastIndexOf(getString(R.string.speaker_capital_for_voice))
                    val indexSpeakerEnglish = spokeCommand.lastIndexOf("Speaker") // also allow English
                    val indexSpeaker = maxOf(indexSpeakerLocal, indexSpeakerEnglish)

                    val indexEndLocal = spokeCommand.lastIndexOf(getString(R.string.end_capital_for_voice))
                    val indexEndEnglish = spokeCommand.lastIndexOf("End") // also allow English
                    val indexEnd = maxOf(indexEndLocal, indexEndEnglish)

                    val indexKeypadLocal = spokeCommand.lastIndexOf(getString(R.string.keypad_capital_for_voice))
                    val indexKeypadEnglish = spokeCommand.lastIndexOf("Keypad") // also allow English
                    val indexKeypad = maxOf(indexKeypadLocal, indexKeypadEnglish)

                    if (indexEnd >= 0) {
                        clickEndButton()
                    } else {
                        if (indexSpeaker >= 0) {
                            clickSpeakerButton(root.context)
                        }
                        if (indexKeypad >= 0) {
                            clickKeypadButton()
                        }
                    }

                }*/
           // }
            //SpeakCommandsManager.startListen(root.context)
        }
        catch (e: Exception) {
            Log.e(
                "SimplyCall - ActiveCallFragment",
                "SpeakCommandsManager startListen Error (${e.message})"
            )
        }

        //  contactNameExistsForPhoneNum = arguments?.getBoolean("CONTACT_NANE_EXISTS_FOR_PHONE_NUMBER", false) ?: false

        // Use ViewModelFactory to create the ViewModel
        /*        val activeCallViewModel = ViewModelProvider(
                    this,
                    ActiveCallViewModel.ActiveCallViewModelFactory(phoneNumber)
                 ).get(ActiveCallViewModel::class.java)*/


        activeCallUIContext = root.context

        if (startingFromMiddleOfCall) {
            isCalling = isInMiddleOfCallIsCalling(root.context)
            if (isCalling) { // animate and show Calling text
                isOutgoingCall = true
            } else {
                isOutgoingCall = isInMiddleOfCallIsOutgoing(root.context)
            }
        } else {
            saveIsInMiddleOfCallIsOutgoing(root.context, isOutgoingCall)
            saveIsInMiddleOfCallIsCalling(root.context, isCalling)
        }

        val outgoingCallCallingText =
            binding.root.findViewById<AppCompatTextView>(R.id.text_calling)



      //  val activeCallAppImage = binding.root.findViewById<android.widget.LinearLayout>(R.id.activeCallAppImage)


        val openKeyPadButton =
            binding.root.findViewById<LinearLayout>(R.id.openKeyPadButton)
        val openKeyPadButtonImage =
            binding.root.findViewById<ImageView>(R.id.openKeyPadButtonImage)
        val openKeyPadButtonText =
            binding.root.findViewById<TextView>(R.id.openKeyPadButtonText)
        if (isOutgoingCall) {
            val text = outgoingCallCallingText.text
            val handler = Handler(Looper.getMainLooper())
            outgoingCallCallingText.text = ""
            for (i in text.indices) {
                handler.postDelayed({
                    outgoingCallCallingText.text = outgoingCallCallingText.text.toString() + text[i]
                }, (50 * i).toLong())
            }
            // waveAnimation(outgoingCallCallingText)

            val textShaderIvoryLightBlue = LinearGradient(
                0f, 0f,
                outgoingCallCallingText.paint.measureText(outgoingCallCallingText.text.toString()),
                outgoingCallCallingText.textSize,
                intArrayOf(
                    Color.parseColor("#FFFFF0"),
                    Color.parseColor("#ADD8E6")
                ), // Ivory and Light Blue
                null,
                Shader.TileMode.CLAMP
            )
            outgoingCallCallingText.paint.shader = textShaderIvoryLightBlue
            //text_callingBack.visibility = View.VISIBLE - for now not showing
            // openKeyPadButtonBack.visibility = View.GONE
        } else {
            //text_callingBack.visibility = View.GONE
            //  openKeyPadButtonBack.visibility = View.VISIBLE
        }

        try { // try to set Contact's image, if exists. Otherwise, put App logo.
            setContactPhoto(CallActivity.originalPhoneNumber)
        }
        catch (e: Exception) {
            Log.e("SimplyCall - ActiveCallFragment", "try to set Contact's image - but got error (${e.message})")
        }

        val shouldShowKeypad = shouldShowKeypadInActiveCall(root.context)
        openKeyPadButton.visibility = if (shouldShowKeypad) VISIBLE else GONE

        textViewPhoneNumber = binding.textActiveCallContact
        updateText(phoneNumber)
        // textViewContactName = binding.textIncomingCallLabel

        /*        val textView: TextView = binding.textIncomingCallContact
                incomingCallViewModel.text.observe(viewLifecycleOwner) {
                    textView.text = phoneNumber
                }*/
        val declineButtonImage = binding.root.findViewById<ImageView>(R.id.declineButtonImage)
        val declineButtonText = binding.root.findViewById<TextView>(R.id.declineButtonText)
        // waveAnimation(textViewPhoneNumber)

        declineButtonImage.setOnClickListener { // decline call and hide screen
            clickEndButton()
        }
        declineButtonText.setOnClickListener { // decline call and hide screen
            clickEndButton()
        }
        val speakerButtonImage = binding.root.findViewById<ImageView>(R.id.speakerButtonImage)
        val speakerButtonText = binding.root.findViewById<TextView>(R.id.speakerButtonText)

        // שימוש בפונקציה:
        if (hasModifyAudioSettingsPermission(this.requireContext())) {
            Log.d("SimplyCall - toggleSpeakerphone", "הרשאה MODIFY_AUDIO_SETTINGS קיימת.")
        } else {
            Log.d("SimplyCall - toggleSpeakerphone", "הרשאה MODIFY_AUDIO_SETTINGS אינה קיימת.")
        }

        isSpeakerOn = isSpeakerphoneOn(this.requireContext()) // first get the real is speaker on
        speakerWasAlreadyOnWhenStarted =
            isSpeakerOn // we did not start it ourself yet even if shouldStartWithSpeakerOn=true
        if (isSpeakerOn) {
            speakerButtonImage.setImageResource(R.drawable.speakeron)
        } else {
            speakerButtonImage.setImageResource(R.drawable.speakeroff)
        }

        if (!startingFromMiddleOfCall) {
            var shouldStartWithSpeakerOn = shouldCallsStartWithSpeakerOn(root.context)

            Log.d(
                "SimplyCall - ActiveCallFragment",
                "TOGGLE_SPEAKER_ACTION - shouldStartWithSpeakerOn: $shouldStartWithSpeakerOn, isSpeakerOn: $isSpeakerOn"
            )

            // This is not good because sometimes isSpeakerOn get stuck
            // So if shouldStartWithSpeakerOn or isSpeakerOn we'll try to turn on just in case
            if (shouldStartWithSpeakerOn) {
                isSpeakerOn = false
                handleSpeakerOnOff(root.context, true)
            }
            //} else if (isSpeakerOn) { // if shouldStartWithSpeakerOn = isSpeakerOn or isSpeakerOn = false then we'll try to turn off
            //  handleSpeakerOnOff(root.context, false)
            //}
        } else { // since we were in the middle of a call and got interrupted by call waiting we want to restore
            // speaker on and opened keypad
            val speakerWasOriginallyOn =
                (!isOutgoingCall && OngoingCall.isSpeakerOn) || (isOutgoingCall && OutgoingCall.isSpeakerOn)
            if (speakerWasOriginallyOn) {
                isSpeakerOn = false
                handleSpeakerOnOff(root.context, true)
            }
            if (shouldShowKeypad) {
                val keypadWasOriginallyOpened =
                    (!isOutgoingCall && OngoingCall.isKeypadOpened) || (isOutgoingCall && OutgoingCall.isKeypadOpened)
                if (keypadWasOriginallyOpened) {
                    clickKeypadButton()
                }
            }
        }

        speakerButtonImage.setOnClickListener {
            clickSpeakerButton(root.context)
        }

        speakerButtonText.setOnClickListener {
            clickSpeakerButton(root.context)
        }

        openKeyPadButtonImage.setOnClickListener {
            clickKeypadButton()
        }

        openKeyPadButtonText.setOnClickListener {
            clickKeypadButton()
        }

        // Observe the wasAnswered property
        if (isOutgoingCall) {
            openKeyPadButton.visibility = GONE
            OutgoingCall.otherCallerAnswered.observe(viewLifecycleOwner) { callWasAnswered ->
                if (callWasAnswered) {
                    isCalling = false
                    OutgoingCall.isCalling = false
                    openKeyPadButton.visibility = if (shouldShowKeypad) VISIBLE else GONE
                    registerCallForDisconnect(OutgoingCall.call)
                    /*  val outgoingCallCallingText =
                          binding.root.findViewById<AppCompatTextView>(R.id.text_calling)*/

                    //  openKeyPadButtonBack.visibility = View.VISIBLE
                    //text_callingBack.visibility = INVISIBLE

                    // Handle the case when the call was answered
                    // Toast.makeText(requireContext(), "Call was answered!", Toast.LENGTH_SHORT).show()
                } else {
                    // Handle the case when the call was not answered
                    // Toast.makeText(requireContext(), "Waiting for call to be answered.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            openKeyPadButton.visibility = if (shouldShowKeypad) VISIBLE else GONE
            registerCallForDisconnect(OngoingCall.call)
        }

        WaitingCall.startedRinging.observe(viewLifecycleOwner) { callWaitingStartedRinging ->
            if (callWaitingStartedRinging) { // We have a new Call Waiting
                waitingCallIsRinging = true
                openWaitingCallWindow()
            } else {
                if (waitingCallIsRinging) {
                    waitingCallIsRinging = false
                    val overlayDialog =
                        parentFragmentManager.findFragmentByTag("WaitingCallWindowDialogFragmentTag") as? WaitingCallFragment
                    overlayDialog?.dismiss()

                    if (WaitingCall.wasAnswered) { // Update UI
                        // need to change the number
                        var originalCall = OutgoingCall.call

                        if (isOutgoingCall) {
                            OutgoingCall.phoneNumberOrContact = WaitingCall.phoneNumberOrContact
                            originalCall = OutgoingCall.call
                            OutgoingCall.call = WaitingCall.call
                        } else {
                            OngoingCall.phoneNumberOrContact = WaitingCall.phoneNumberOrContact
                            originalCall = OngoingCall.call
                            OngoingCall.call = WaitingCall.call
                        }

                        originalCall?.unregisterCallback(callCallback)
                        registerCallForDisconnect(WaitingCall.call)

                        // Update UI:
                        updateText(WaitingCall.phoneNumberOrContact.toString())
                        setContactPhoto(WaitingCall.originalPhoneNumber)

                        WaitingCall.replacedWithWaitingCall =
                            true // so we'll know not to close anything from InCallService manager, because another call is now active
                        WaitingCall.call = originalCall

                        WaitingCall.call?.disconnect() // and replacedWithWaitingCall should make InCallService manager to not close the active call screenn
                    }
                }
            }
        }

        if (ContextCompat.checkSelfPermission( // this could be not reset yet -> if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                root.context,
                READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) { // try to request permission on run-time, at least for next time (and this time we should use the inner contacts db)
            requestReadContactPermission()
        }

        OngoingCall.shouldUpdateSpeakerState.observe(viewLifecycleOwner) { shouldUpdateSpeakerStatus ->
            if (shouldUpdateSpeakerStatus) {
                OngoingCall.shouldUpdateSpeakerState.value = false
                updateSpeakerState(root.context)
            }
        }

        CallActivity.criticalErrorEvent.value = false // no reason not to reset this event here
        CallActivity.criticalErrorEventMessage = ""
        CallActivity.criticalErrorEvent.observe(viewLifecycleOwner) { isCriticalError ->
            if (isCriticalError) { // try to unload
                CallActivity.criticalErrorEventMessage = ""
                CallActivity.criticalErrorEvent.value = false
                unLoadScreen()
               //showCallErrorAlert(binding.root.context, message)
            }
        }

        return root
    }

    private fun setContactPhoto(phoneNumber: String?) {
        val context = binding.root.context
        val contactId = if (phoneNumber != null) getContactIdFromPhoneNumber(
            context,
            CallActivity.originalPhoneNumber!!
        ) else null

        val contactOrAppIcon = binding.activeCallAppImage
        val contactExistingPhotoBack = binding.contactExistingPhotoBack
        if (contactId != null) {
            val photoImageView = binding.photoImageView
            val userProfilePicture = getContactPhoto(
                context,
                contactId
            ) // if contactId is not null then we know fragmentContext isn't null
            if (userProfilePicture != null) {
                photoImageView.setImageBitmap(userProfilePicture)
                contactExistingPhotoBack.visibility = VISIBLE
                contactOrAppIcon.visibility = GONE
            } else {
                //contactOrAppIcon.set
                contactExistingPhotoBack.visibility = GONE
                contactOrAppIcon.visibility = VISIBLE
            }
        }
        else {
            contactExistingPhotoBack.visibility = GONE
            contactOrAppIcon.visibility = VISIBLE
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
                    //finish() // This will close the current activity
                }
                .setCancelable(false) // Make the dialog not cancellable by touching outside
                .show()
        }
    }

    private fun updateSpeakerState(context: Context) {
        try {
            Log.d(
                "SimplyCall - ActiveCallFragment",
                "Speaker status updated by InCallService: $isSpeakerOn"
            )
            val speakerWasTurnedOn =
                context?.let { isSpeakerphoneOn(it) } // first get the real is speaker on
            val speakerButton =
                binding.root.findViewById<ImageView>(R.id.speakerButtonImage)
            if (speakerWasTurnedOn == true) {
                if (!isSpeakerOn) { // Good - Speaker was turned off and we turned it on
                    isSpeakerOn = true // adjust UI for Speaker ON
                    speakerButton.setImageResource(R.drawable.speakeron)
                } else if (firstSpeakerTry) { // Speaker was on and we wanted to turn off
                    firstSpeakerTry = false // Only once
                    toggleSpeakerphone(false, context) // try again
                    updateSpeakerUI(context)
                } else { // Speaker was already on and we wanted to turn off but failed - 2nd failure only logging
                    Log.e(
                        TAG,
                        "Speaker is on and we wanted to turn off - but 2nd failure only logging"
                    )
                }
            } else {
                if (isSpeakerOn) { // Good - Speaker was turned on and we turned it off
                    isSpeakerOn = false // adjust UI for Speaker OFF
                    speakerButton.setImageResource(R.drawable.speakeroff)
                } else if (firstSpeakerTry) { // Speaker was already off and we wanted to turn on
                    firstSpeakerTry = false // Only once
                    toggleSpeakerphone(true, context) // try to turn it on again
                    updateSpeakerUI(context)
                } else { // Speaker was off and we wanted to turn on but failed - 2nd failure only logging
                    Log.e(
                        TAG,
                        "Speaker is off and we wanted to turn on - but 2nd failure only logging"
                    )
                }
            }
            inMiddleOfTryingToChangeSpeaker = false
        } catch (e: Exception) {
            inMiddleOfTryingToChangeSpeaker = false
            Log.e(
                TAG,
                "speakerStatusReceiver onReceiver error (isSpeakerOn = $isSpeakerOn, error: ${e.message})"
            )
        }
    }

    private fun requestReadContactPermission() {
        try {
            requestPermissionLauncher.launch(READ_CONTACTS)
        } catch (e: Exception) {

        }
    }

    // So we can come back
    private fun createNotificationForActiveCall(context: Context) {
        // In your Fragment:

        val intent = Intent(context, EventScreenActivity::class.java).apply {
            // Use these flags so that if MainActivity is already running, it is reused.
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("fragmentToShow", "MyFragmentIdentifier")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

// Create a notification channel for Android O and above.
        val channelId = "my_channel_id"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "My Channel"
            val descriptionText = "Channel for my notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

// Build and issue the notification.
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.goldappiconphoneblack)
            .setContentTitle(getString(R.string.active_call_capital))
            .setContentText(getString(R.string.click_to_go_back_to_call))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())

    }

    private fun clickSpeakerButton(context: Context?) {
        if (context !== null) {
            handleSpeakerOnOff(context, (!isSpeakerOn))
        }
    }

    // Open Waiting Call screen from within
    private fun openWaitingCallWindow() {
        val overlayFragment = WaitingCallFragment()
        overlayFragment.show(parentFragmentManager, "WaitingCallWindowDialogFragmentTag")
        // waitingCallOverlayFragment = overlayFragment
        /*        val args = Bundle().apply {
                    putBoolean("IS_OUT_GOING", isOutgoingCall)
                }
                overlayFragment.arguments = args*/

    }

    private fun clickKeypadButton() {
        val overlayFragment = StandaloneDialerDialogFragment()
        val args = Bundle().apply {
            putBoolean("IS_OUT_GOING", isOutgoingCall)
        }
        overlayFragment.arguments = args
        overlayFragment.show(parentFragmentManager, "StandaloneDialerDialogFragmentTag")
    }

    private fun clickEndButton() {
        Log.d(
            "SimplyCall - ActiveCallFragment",
            "declineButton clicked - Fragment (isOutgoingCall = $isOutgoingCall)"
        )
        try {
            userReacted = true
            //SpeakCommandsManager.stopListen()
            if (isOutgoingCall) {
                OutgoingCall.hangup()
            } else {
                OngoingCall.hangup()
            }
        } catch (e: Exception) {

        }

        unLoadScreen()
    }

    private fun unLoadScreen() {
        try {
            //exitTransition = android.transition.Fade() // Add fade effect
            /*parentFragmentManager.beginTransaction()
                .remove(this)
                .commit()*/

            requireActivity().finish()
        } catch (e: Exception) {
            Log.e(
                TAG,
                "unLoadScreen error (error: ${e.message})"
            )
        }
    }

    private fun registerCallForDisconnect(call: Call?) {
        callCallback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                try {
                    super.onStateChanged(call, newState)
                    if (newState == Call.STATE_DISCONNECTED) {
                        // The call was disconnected
                        call.unregisterCallback(this)
                        unLoadScreen()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "registerCallForDisconnect error (error: ${e.message})")
                }
            }
        }

// רישום ה־callback
        call?.registerCallback(callCallback)
    }

    private fun handleSpeakerOnOff(context: Context, shouldEnable: Boolean) {
        try {
            inMiddleOfTryingToChangeSpeaker = true
            firstSpeakerTry = true
            val toggleSucceeded = toggleSpeakerphone(shouldEnable, context)
            // Check if speaker really changed:
            if (toggleSucceeded) {
                updateSpeakerUI(context)
                inMiddleOfTryingToChangeSpeaker = false
            } // else - we'll handle results in event from InCallService

        } catch (e: Exception) {
            inMiddleOfTryingToChangeSpeaker = false
            Log.e(
                TAG,
                "set Speaker error (isSpeakerOn = $isSpeakerOn, error: ${e.message})"
            )
        }
    }

    private fun updateSpeakerUI(context: Context) {
        val speakerButton = binding.root.findViewById<ImageView>(R.id.speakerButtonImage)
        isSpeakerOn = isSpeakerphoneOn(context)
        if (isSpeakerOn) {
            speakerButton.setImageResource(R.drawable.speakeron)
        } else {
            speakerButton.setImageResource(R.drawable.speakeroff)
        }
    }

    private fun hasModifyAudioSettingsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()

       // SpeakCommandsManager.stopListen()

        OngoingCall.shouldToggleSpeaker.value = false
        OngoingCall.shouldUpdateSpeakerState.value = false

        // val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // notificationManager.cancel(1) // 1 הוא ה-ID ששימש בעת קריאה ל-notify

        if (activeCallUIContext != null && !speakerWasAlreadyOnWhenStarted) {
            handleSpeakerOnOff(activeCallUIContext!!, false) // Turn off speaker when finished
        }

        /*if (!userReacted) { // if we are closed and user did not click any button
            // Not disconnecting for now - but leaving a notification
            //  then we must manually disconnect the call
            if (isOutgoingCall) {
                OutgoingCall.hangup()
            } else {
                OngoingCall.hangup()
            }
        } */

        try {
            OngoingCall.call?.unregisterCallback(callCallback)
            OutgoingCall.call?.unregisterCallback(callCallback)
        } catch (e: Exception) {

        }

        _binding = null
    }

    private fun updateText(newPhoneOrContact: String) {
        val contactLabel =
            binding.root.findViewById<AppCompatTextView>(R.id.text_active_call_contact)
        contactLabel.text = newPhoneOrContact

        if (!OutgoingCall.contactExistsForPhoneNum) {
            //val numericNumber = PhoneNumberUtils.convertKeypadLettersToDigits(newPhoneOrContact)
            contactLabel.layoutDirection = View.LAYOUT_DIRECTION_LTR
            contactLabel.textDirection = View.TEXT_DIRECTION_LTR
        }

/*        contactLabel.post {
            val contactTextRowsNum = contactLabel.lineCount
            val contactOrAppIcon = binding.activeCallAppImage
            val contactExistingPhotoBack = binding.contactExistingPhotoBack
            if (contactTextRowsNum > 2) {
                contactExistingPhotoBack.height =

            }
        }*/

    }

    /*    private fun setSpeakerphone(context: Context, enable: Boolean) {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.requestAudioFocus(
                null, // ניתן להוסיף Listener לשינויים אם צריך
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN
            )
            audioManager.setMode(AudioManager.MODE_IN_CALL)
            //audioManager.isSpeakerphoneOn = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val speakerphone = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                    .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
                if (enable && speakerphone != null) {
                    audioManager.setCommunicationDevice(speakerphone)
                } else {
                    audioManager.clearCommunicationDevice()
                }
            } else { // Older versions
                audioManager.isSpeakerphoneOn = enable
            }
            isSpeakerOn = enable
        }

        fun isSpeakerphoneOn(context: Context): Boolean {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager


            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                //getCommunicationDevice(
                val currentDevice = audioManager.communicationDevice
                currentDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                // בגרסאות ישנות יותר, משתמשים במאפיין הישן
                audioManager.isSpeakerphoneOn
            }
        }*/

    /**
     * פונקציה להדלקה או כיבוי של הרמקול (Speaker).
     * פועלת על גרסאות Android 12+ בעזרת setCommunicationDevice,
     * ובגרסאות ישנות יותר בעזרת isSpeakerphoneOn.
     *
     * @param context קונטקסט
     * @param enable  true אם רוצים להדליק את הרמקול, false אם לכבות
     */
    private fun sendSpeakerphoneEvent(context: Context, enable: Boolean) {
        //toggleSpeakerphone(enable, context)

        val intent = Intent(TOGGLE_SPEAKER_ACTION).apply {

        }
        intent.putExtra(EXTRA_ENABLE_SPEAKER, enable)
        context.sendBroadcast(intent)
    }

    /*
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // נבקש AudioFocus, כדי שהמערכת תדע שאנחנו רוצים לנהל כרגע את ערוץ האודיו
    val focusResult = audioManager.requestAudioFocus(
        *//* listener = *//* null,
            *//* streamType = *//* AudioManager.STREAM_VOICE_CALL,
            *//* durationHint = *//* AudioManager.AUDIOFOCUS_GAIN
        )
        if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.w(TAG, "Audio focus not granted. Can't toggle speakerphone.")
            return
        }

        // נגדיר את מצב האודיו כדי לנהל שיחה.
        // כברירת מחדל באפליקציות Dialer רבות נהוג להשתמש ב-MODE_IN_CALL,
        // אבל אם אתה מוצא בעיות, ניתן לנסות MODE_IN_COMMUNICATION.
        audioManager.mode = AudioManager.MODE_IN_CALL

        // כדי לעקוב אחר מה שקורה בלוג:
        Log.d(TAG, "setSpeakerphone(enable=$enable) >> Before: " +
                "mode=${audioManager.mode}, " +
                "isSpeakerphoneOn=${audioManager.isSpeakerphoneOn}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 ומעלה:
            val speakerDevice = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

            if (enable) {
                if (speakerDevice != null) {
                    val success = audioManager.setCommunicationDevice(speakerDevice)
                    if (!success) {
                        Log.e(TAG, "setCommunicationDevice(speaker) failed!")
                    } else {
                        Log.d(TAG, "setCommunicationDevice(speaker) succeeded.")
                    }
                } else {
                    // אם לא נמצאה יציאה לרמקול, אולי יש בעיה במכשיר
                    Log.e(TAG, "No built-in speaker device found!")
                }
            } else {
                // החזרת המצב להתקן ברירת המחדל (אוזניה/אפרכסת וכו')
                audioManager.clearCommunicationDevice()
                Log.d(TAG, "clearCommunicationDevice called.")
            }
        } else {
            // גרסאות ישנות יותר:
            audioManager.isSpeakerphoneOn = enable
            Log.d(TAG, "isSpeakerphoneOn set to $enable (legacy API).")
        }*/

    // לוג אחרי שהגדרנו
    /*  Log.d(TAG, "setSpeakerphone(enable=$enable) >> After: " +
              "mode=${audioManager.mode}, " +
              "isSpeakerphoneOn=${audioManager.isSpeakerphoneOn}")*/
    //  }

    fun waveAnimation(textView: TextView) {
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
    }


}