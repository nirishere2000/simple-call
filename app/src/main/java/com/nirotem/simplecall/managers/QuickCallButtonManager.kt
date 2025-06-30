package com.nirotem.simplecall.managers

import android.Manifest.permission.CALL_PHONE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadDistressButtonShouldAlsoSendSmsToGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadDistressNumberOfSecsToCancel
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadDistressNumberShouldAlsoTalk
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumber
import com.nirotem.simplecall.helpers.sendSms
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.statuses.SettingsStatus.distressNumberOfSecsToCancel
import com.nirotem.simplecall.statuses.SettingsStatus.isPremium
import kotlinx.coroutines.*
import java.util.*

object QuickCallButtonManager {
    var quickCallIsOn = false
    var quickCallWasAnswered = false
    private var quickCallDelayTimeAnimationIsRunning = false
    private var quickCallDelayTimeAnimationSecondsPassed: Long = 0
   // private const val EMERGENCY_DELAY_TIME_ANIMATION_SECONDS: Long = 6 // actually it's 1 second less than that (if this equals 6 then it's 5 secs)
    private var countDownTimer: CountDownTimer? = null
    private var askingForMakingMakingCallPermission = false
    private var displayedQuickCallMsg: Snackbar? = null
    private var shouldAlsoCallForHelp = true
    private var quickCallJob: Job? = null
    private var quickCallNumberOfTries = 0
    private var quickCallShouldAlsoTalk = false
    private var shouldAlsoSendSms = false
    private var numOfTimesSpeakAndSentSms = 0
    private var quickCallRunnable: Runnable? = null
    private var quickCallIsOnWaitingDelay = false // between one call and sms to another we should wait
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var checkJob: Job? = null // for gold-white animation of quick call icon
    //var numberToCheck: Long = 0L // for gold-white animation of quick call icon
    private var quickCallAnimationCounter: Int = 0
    private fun stopRepeatingQuickCallMessage() {
        quickCallJob?.cancel()
        quickCallJob = null
    }

    fun checkForDistressButton(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity,
        lifecycleOwner: LifecycleOwner
    ) {
        val quickCallButtonSmall = activity.findViewById<FrameLayout>(R.id.emergencyButtonSmall)
        val toolbar: Toolbar = activity.findViewById(R.id.toolbar)
        if (SettingsStatus.quickCallNumber.value != null) {
            quickCallButtonSmall?.visibility = VISIBLE
            handleDistressButton(view, context, supportFragmentManager, requestPermissionLauncher, activity, lifecycleOwner)
        } else {
            quickCallButtonSmall?.visibility = GONE
            val params = toolbar.layoutParams as? ViewGroup.MarginLayoutParams
            params?.let {
                it.topMargin = 0
                it.bottomMargin = 0
                toolbar.layoutParams = it
            }
        }
    }

    fun cancelQuickCall(activity: Activity, context: Context) {
        quickCallIsOn = false
        quickCallIsOnWaitingDelay = false
        displayedQuickCallMsg?.dismiss()
        stopSwitchQuickCallIconColorAnimation(activity)
        resetQuickCallButton(activity, context)
    }

    private fun handleDistressButton(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity,
        lifecycleOwner: LifecycleOwner
    ) {
        val cancelQuickCallButtonCancel = activity.findViewById<ImageView>(R.id.emergency_button_cancel)
        cancelQuickCallButtonCancel?.setOnClickListener {
            if (quickCallDelayTimeAnimationIsRunning || quickCallIsOnWaitingDelay) {
                cancelQuickCall(activity, context)
            }
        }

        quickCallShouldAlsoTalk = if (isPremium) loadDistressNumberShouldAlsoTalk(view.context) else false
        shouldAlsoSendSms = loadDistressButtonShouldAlsoSendSmsToGoldNumber(context)
        quickCallNumberOfTries = 0 // this should be done only once in the beginning
        numOfTimesSpeakAndSentSms = 0 // this should be done only once in the beginning

        val quickCallButton = activity.findViewById<ImageView>(R.id.quick_call_button_small)

        val isAppNotDefault = (PermissionsStatus.defaultDialerPermissionGranted.value != true)
        enableDistressButton(view, context, activity, requestPermissionLauncher, (PermissionsStatus.readContactsPermissionGranted.value == true && PermissionsStatus.callPhonePermissionGranted.value == true && (!isAppNotDefault)))
        PermissionsStatus.callPhonePermissionGranted.observe(lifecycleOwner) { isGranted ->
            if (!OpenScreensStatus.isHelpScreenOpened) {
                val appIsDefault = PermissionsStatus.defaultDialerPermissionGranted.value == true
                enableDistressButton(view, context, activity, requestPermissionLauncher, isGranted && PermissionsStatus.readContactsPermissionGranted.value == true && appIsDefault, false)
            }
        }

        quickCallButton?.setOnClickListener {
            val appIsDefault = (PermissionsStatus.defaultDialerPermissionGranted.value == true)
            if (PermissionsStatus.callPhonePermissionGranted.value == true && PermissionsStatus.readContactsPermissionGranted.value == true && appIsDefault) {
                handleDistressButtonClick(view, context, supportFragmentManager, requestPermissionLauncher, activity)
            }
        }

        val quickCallIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)
        quickCallIcon.setOnClickListener {
            val appIsDefault = (PermissionsStatus.defaultDialerPermissionGranted.value == true)
            if (PermissionsStatus.callPhonePermissionGranted.value == true && appIsDefault) {
                handleDistressButtonClick(view, context, supportFragmentManager, requestPermissionLauncher, activity)
            }
        }
    }

    private fun initQuickCallParameters(context: Context, activity: Activity) {
        quickCallDelayTimeAnimationIsRunning = true

        val quickCallButtonCancelBack = activity.findViewById<FrameLayout>(R.id.quickCallMessage)
        quickCallButtonCancelBack?.visibility = VISIBLE
        val distressCircle = activity.findViewById<FrameLayout>(R.id.distress_circle)
        distressCircle?.visibility = VISIBLE
        val toastMsg = context.getString(R.string.calling_quick_call_number_tap_cancel_to_stop)
        displayedQuickCallMsg = showLongSnackBar(context, toastMsg, (((distressNumberOfSecsToCancel + 1) * 1000).toInt()))

        val quickCallButtonText = activity.findViewById<TextView>(R.id.quick_call_button_text)
        quickCallButtonText.text = "(${(distressNumberOfSecsToCancel + 1)})"

        val quickCallButton = activity.findViewById<ImageView>(R.id.quick_call_button)
        val pulsateAnim = AnimationUtils.loadAnimation(context, R.anim.pulsate)
        quickCallButton.startAnimation(pulsateAnim)
        quickCallDelayTimeAnimationSecondsPassed = 0
    }

    private fun handleDistressButtonClick(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity
    ) {
        if (!quickCallDelayTimeAnimationIsRunning) {
            quickCallIsOn = true
            SettingsStatus.quickCallNumber.value = loadQuickCallNumber(context)
            distressNumberOfSecsToCancel = loadDistressNumberOfSecsToCancel(context)
            shouldAlsoCallForHelp = if (isPremium) loadDistressNumberShouldAlsoTalk(context) else false

            if (PermissionsStatus.callPhonePermissionGranted.value == true) {
                initQuickCallParameters(context, activity)
                startQuickCallTimer(view, context, supportFragmentManager, requestPermissionLauncher, activity)
            } else { // handle permissions as fast as can
                // Also try speak and send SMS
                sendSms(context)
                if (quickCallShouldAlsoTalk) {
                    TextToSpeechManager.addToQueueAndSpeak(context.getString(R.string.quick_call_activated_voice_message_speech))
                }
                callQuickCallNumber(context, view, supportFragmentManager, requestPermissionLauncher, activity)
            }
        }
    }

    private fun sendSms(context: Context) {
        if (numOfTimesSpeakAndSentSms <= 3) {
            numOfTimesSpeakAndSentSms++
            // Speak if user chose option:
/*            if (quickCallShouldAlsoTalk) {
                Handler(Looper.getMainLooper()).postDelayed({
                    TextToSpeechManager.speak(context.getString(R.string.quick_call_activated_voice_message_speech))
                }, 500) // חצי שניה השהיה
            }*/

            if (shouldAlsoSendSms) {
                val goldPhoneNumber = loadGoldNumber(context)
                if (goldPhoneNumber != null) { // Sending first SMS
                    sendSms(goldPhoneNumber, context.getString(R.string.quick_call_activated_voice_message_speech))
                }
                else {
                    val goldNumberName = context.getString(R.string.gold_number)
                    val quickCallName = context.getString(R.string.quick_call_button_caption)
                    val toastMsg = context.getString(R.string.gold_number_contact_could_not_be_found_to_send_sms_during_a_quick_call, goldNumberName, quickCallName)

                    showCustomToastDialog(context, toastMsg)
                }
            }
        }
    }

    private fun resetQuickCallButton(activity: Activity, context: Context) {
        quickCallWasAnswered = false
        stopQuickCallTimer()
        quickCallRunnable?.let { handler.removeCallbacks(it) }
        displayedQuickCallMsg?.dismiss()
        quickCallDelayTimeAnimationIsRunning = false
        quickCallDelayTimeAnimationSecondsPassed = 0
        val distressButtonIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)
        distressButtonIcon.setImageResource(R.drawable.ic_bell_white)
        val quickCallButton = activity.findViewById<ImageView>(R.id.quick_call_button)
        quickCallButton.clearAnimation()
        val quickCallButtonText = activity.findViewById<TextView>(R.id.quick_call_button_text)
        quickCallButtonText.text = context.getString(R.string.quick_call_button_caption)
        val quickCallButtonCancelBack = activity.findViewById<FrameLayout>(R.id.quickCallMessage)
        quickCallButtonCancelBack?.visibility = GONE
        val distressCircle = activity.findViewById<FrameLayout>(R.id.distress_circle)
        distressCircle?.visibility = GONE
    }

    private fun callQuickCallNumber(
        context: Context,
        view: View,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity
    ) {
        val rootView: View = activity.findViewById(android.R.id.content)
        if (SettingsStatus.quickCallNumber.value != null) {
            OutgoingCall.makeCall(
                SettingsStatus.quickCallNumber.value.toString(),
                false,
                rootView.context,
                supportFragmentManager,
                activity,
                requestPermissionLauncher,
                true
            )
        } else {
            val toastMsg = context.getString(R.string.quick_call_unavailable_try_gold_number)
            showLongSnackBar(context, toastMsg, 8000)
        }
    }

    fun enableDistressButton(
        view: View,
        context: Context,
        activity: Activity,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        shouldEnable: Boolean?,
        shouldShowDialog: Boolean = false
    ) {
        val quickCallButton = activity.findViewById<ImageView>(R.id.quick_call_button_small)
        val quickCallIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)
        val quickCallIconDisabledText: TextView = activity.findViewById(R.id.quick_call_button_text_disabled)

        if (shouldEnable == true) {
            quickCallIcon.visibility = VISIBLE
            quickCallButton.alpha = 1f
            quickCallIconDisabledText.visibility = GONE
        } else {
            quickCallIcon.visibility = GONE
            quickCallButton.alpha = 0.4f
            quickCallIconDisabledText.visibility = VISIBLE

            val newTextSizeSp = when (Locale.getDefault().language) {
                "ar" -> 20f
                "he", "iw" -> 16f
                "de" -> 11f
                "fr" -> 9f
                "es" -> 13f
                "nl" -> 8f
                "ru" -> 9f
                else -> 10f
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                quickCallIconDisabledText.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
            }
            quickCallIconDisabledText.setTextSize(TypedValue.COMPLEX_UNIT_SP, newTextSizeSp)

            if (shouldShowDialog) {
                if (!askingForMakingMakingCallPermission) {
                    alertAboutNoPhonePermission(view, context, activity, requestPermissionLauncher)
                }
            }
        }
    }

    private fun alertAboutNoPhonePermission(
        view: View,
        context: Context,
        activity: Activity,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, CALL_PHONE)) {
            val rootView: View = activity.findViewById(android.R.id.content)
            val snackBar = Snackbar.make(
                rootView,
                context.getString(R.string.make_calls_permission_was_denied_but_needed),
                12000
            ).setAction(context.getString(R.string.settings_capital)) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", rootView.context.packageName, null)
                intent.data = uri
                rootView.context.startActivity(intent)
            }
            val snackBarView = snackBar.view
            val textView = snackBarView.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
            textView.maxLines = 5
            snackBar.show()
        } else {
            askingForMakingMakingCallPermission = true
            showPermissionsConfirmationDialog(
                context.getString(R.string.permission_needed_capital_p),
                context.getString(R.string.in_order_to_make_calls_the_application_must_have_the_proper_permission),
                { requestCallPhonePermission(requestPermissionLauncher) },
                context
            )
        }
    }

    private fun requestCallPhonePermission(requestPermissionLauncher: ActivityResultLauncher<String>) {
        requestPermissionLauncher.launch(CALL_PHONE)
    }

    private fun showPermissionsConfirmationDialog(
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit,
        context: Context
    ) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)
        builder.setTitle(msgTitle)
        builder.setMessage(msgText)
        builder.setPositiveButton(context.getString(R.string.ask_permission_capital_a)) { dialog, _ ->
            dialog.dismiss()
            onAskPermission()
        }
        builder.setNegativeButton(context.getString(R.string.cancel_capital)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    fun startQuickCallTimer(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity
    ) {
        quickCallAnimationCounter = 0
        val quickCallButtonText = activity.findViewById<TextView>(R.id.quick_call_button_text)
        startSwitchQuickCallIconColorAnimation(activity, context)

        if (quickCallShouldAlsoTalk) {
            TextToSpeechManager.addToQueueAndSpeak(context.getString(R.string.quick_call_activated_voice_message_speech))
            //TextToSpeechManager.speak(context.getString(R.string.quick_call_activated_voice_message_speech))
        }

        countDownTimer = object : CountDownTimer(((distressNumberOfSecsToCancel + 1) * 1000), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (quickCallDelayTimeAnimationIsRunning) {
                    quickCallDelayTimeAnimationSecondsPassed++
                    val secondsDiff = (distressNumberOfSecsToCancel + 1) - quickCallDelayTimeAnimationSecondsPassed
                    quickCallButtonText.text = "($secondsDiff)"
                }
            }

            override fun onFinish() {
                if (quickCallDelayTimeAnimationIsRunning &&
                    quickCallDelayTimeAnimationSecondsPassed >= (distressNumberOfSecsToCancel + 1)
                ) {
                    resetQuickCallButton(activity, context)
                    sendSms(context) // first speak and send sms
                    callQuickCallNumber(context, view, supportFragmentManager, requestPermissionLauncher, activity)
                    waitAndThenStartTimerAgain(view, context, supportFragmentManager, requestPermissionLauncher, activity) // Wait and start all over again (if call was not answered
                }
            }
        }.start()
    }

    // Show Quick call is on - without timer,but with the cancel button
    // After a while - if call was not answer and cancel button was not pressed restart time
    private fun waitAndThenStartTimerAgain(view: View,
                                           context: Context,
                                           supportFragmentManager: FragmentManager,
                                           requestPermissionLauncher: ActivityResultLauncher<String>,
                                           activity: Activity) {

        if (quickCallNumberOfTries < 2) { // 0, 1, 2
            val quickCallName = context.getString(R.string.quick_call_button_caption)
            val toastMsg = context.getString(R.string.quick_call_in_progress, quickCallName)
            displayedQuickCallMsg = showLongSnackBar(context, toastMsg, 40000) // Until we try again
            val quickCallButtonCancelBack = activity.findViewById<FrameLayout>(R.id.quickCallMessage)
            quickCallButtonCancelBack?.visibility = View.VISIBLE
            quickCallIsOnWaitingDelay = true

            quickCallRunnable = Runnable {
                displayedQuickCallMsg?.dismiss()
                if (quickCallIsOnWaitingDelay) {
                    quickCallIsOnWaitingDelay = false
                    initQuickCallParameters(context, activity)
                    startQuickCallAgain(view, context, supportFragmentManager, requestPermissionLauncher, activity)
                }
            }

            handler.postDelayed(quickCallRunnable!!, 45_000)
        }
        else {
            finishQuickCall(context, activity, false)
        }
    }

    private fun finishQuickCall(context: Context, activity: Activity, success: Boolean) {
        quickCallIsOn = false
        quickCallIsOnWaitingDelay = false
        checkJob?.cancel()
        checkJob = null
        resetQuickCallButton(activity, context)
        val cancelQuickCallButtonCancel = activity.findViewById<ImageView>(R.id.emergency_button_cancel)
        cancelQuickCallButtonCancel.visibility = GONE
        stopSwitchQuickCallIconColorAnimation(activity) // just in case
        val quickCallName = context.getString(R.string.quick_call_button_caption)
        val toastMsg = if (success)
            context.getString(R.string.quick_call_was_answered_and_completed_successfully)
        else
            context.getString(R.string.tried_to_activate_quick_call_for_3_times_but_no_success, quickCallName)
        displayedQuickCallMsg = showLongSnackBar(context, toastMsg)
    }

    private fun startQuickCallAgain(view: View,
                                    context: Context,
                                    supportFragmentManager: FragmentManager,
                                    requestPermissionLauncher: ActivityResultLauncher<String>,
                                    activity: Activity) {
        if (!quickCallWasAnswered && quickCallNumberOfTries < 2) {
            quickCallNumberOfTries++
            startQuickCallTimer(view, context, supportFragmentManager, requestPermissionLauncher, activity)
        } else { // Finished
            resetQuickCallButton(activity, context)
        }
    }

    fun startSwitchQuickCallIconColorAnimation(activity: Activity, context: Context, intervalMillis: Long = 700) {
        val quickCallButtonIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)

        // ודא שאין טיימר פעיל
        if (checkJob?.isActive == true) return

        checkJob = scope.launch {
            while (isActive) {
                if (quickCallAnimationCounter % 2 == 0) {
                    quickCallButtonIcon.setImageResource(R.drawable.ic_bell_gold)
                } else {
                    quickCallButtonIcon.setImageResource(R.drawable.ic_bell_white)
                }
                quickCallAnimationCounter++
                if (quickCallWasAnswered) {
                    finishQuickCall(context, activity, true)
                }
                else if (quickCallDelayTimeAnimationIsRunning || quickCallIsOnWaitingDelay) {
                    delay(intervalMillis)
                }
            }
        }
    }

    fun stopSwitchQuickCallIconColorAnimation(activity: Activity) {
        checkJob?.cancel()
        checkJob = null
        val quickCallButtonIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)
        quickCallButtonIcon.setImageResource(R.drawable.ic_bell_white)
    }

    private fun stopQuickCallTimer() {
        displayedQuickCallMsg?.dismiss()
        stopRepeatingQuickCallMessage()
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
