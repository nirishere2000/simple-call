package com.nirotem.simplecall.managers

import android.Manifest.permission.CALL_PHONE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
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
import com.nirotem.simplecall.OngoingCall
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

object DistressButtonManager {
    var emergencyIsOn = false
    var emergencyCallWasAnswered = false
    private var timer: CountDownTimer? = null
    private var emergencyDelayTimeAnimationIsRunning = false
    private var emergencyDelayTimeAnimationSecondsPassed: Long = 0
   // private const val EMERGENCY_DELAY_TIME_ANIMATION_SECONDS: Long = 6 // actually it's 1 second less than that (if this equals 6 then it's 5 secs)
    private var countDownTimer: CountDownTimer? = null
    private var askingForMakingMakingCallPermission = false
    private var displayedEmergencyMsg: Snackbar? = null
    private var shouldAlsoCallForHelp = true
    private var emergencyJob: Job? = null
    private var alreadySentSms = false

    private fun startRepeatingEmergencyMessage(context: Context) {
        emergencyJob = CoroutineScope(Dispatchers.Main).launch {
            while (shouldAlsoCallForHelp) {
                if (!emergencyCallWasAnswered && OngoingCall.call == null && OutgoingCall.call == null) {
                    TextToSpeechManager.speak(context.getString(R.string.quick_call_activated_voice_message_speech))
                }
                if (emergencyIsOn) {
                    delay(20_000) // 20 שניות
                }
            }
        }
    }

    private fun stopRepeatingEmergencyMessage() {
        emergencyJob?.cancel()
        emergencyJob = null
    }

    fun checkForDistressButton(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity,
        lifecycleOwner: LifecycleOwner
    ) {
        val emergencyButtonSmall = activity.findViewById<FrameLayout>(R.id.emergencyButtonSmall)
        val toolbar: Toolbar = activity.findViewById(R.id.toolbar)
        if (SettingsStatus.quickCallNumber.value != null) {
            emergencyButtonSmall?.visibility = VISIBLE
            handleDistressButton(view, context, supportFragmentManager, requestPermissionLauncher, activity, lifecycleOwner)
        } else {
            emergencyButtonSmall?.visibility = GONE
            val params = toolbar.layoutParams as? ViewGroup.MarginLayoutParams
            params?.let {
                it.topMargin = 0
                it.bottomMargin = 0
                toolbar.layoutParams = it
            }
        }
    }

    private fun handleDistressButton(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity,
        lifecycleOwner: LifecycleOwner
    ) {
        val emergencyButtonCancel = activity.findViewById<ImageView>(R.id.emergency_button_cancel)
        emergencyButtonCancel?.setOnClickListener {
            if (emergencyDelayTimeAnimationIsRunning) {
                resetEmergencyButton(activity, context)
            }
        }

        val emergencyButton = activity.findViewById<ImageView>(R.id.emergency_button_small)

        val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
        enableDistressButton(view, context, activity, requestPermissionLauncher, (PermissionsStatus.callPhonePermissionGranted.value == true && (!isPremiumAndAppNotDefault)))
        PermissionsStatus.callPhonePermissionGranted.observe(lifecycleOwner) { isGranted ->
            if (!OpenScreensStatus.isHelpScreenOpened) {
                val premiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
                enableDistressButton(view, context, activity, requestPermissionLauncher, isGranted, premiumAndAppNotDefault)
            }
        }

        emergencyButton?.setOnClickListener {
            val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
            if (PermissionsStatus.callPhonePermissionGranted.value == true && (!isPremiumAndAppNotDefault)) {
                handleDistressButtonClick(view, context, supportFragmentManager, requestPermissionLauncher, activity)
            }
        }

        val emergencyIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)
        emergencyIcon.setOnClickListener {
            val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
            if (PermissionsStatus.callPhonePermissionGranted.value == true && (!isPremiumAndAppNotDefault)) {
                handleDistressButtonClick(view, context, supportFragmentManager, requestPermissionLauncher, activity)
            }
        }
    }

    private fun handleDistressButtonClick(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity
    ) {
        if (!emergencyDelayTimeAnimationIsRunning) {
            emergencyIsOn = true
            SettingsStatus.quickCallNumber.value = loadQuickCallNumber(context)
            distressNumberOfSecsToCancel = loadDistressNumberOfSecsToCancel(context)
            shouldAlsoCallForHelp = if (isPremium) loadDistressNumberShouldAlsoTalk(context) else false

            // One time speak on the beginning even if user did not select the option:
            if (isPremium) {
                TextToSpeechManager.speak(context.getString(R.string.quick_call_activated_voice_message_speech))
            }

            val shouldAlsoSendSms = loadDistressButtonShouldAlsoSendSmsToGoldNumber(context)
            if (shouldAlsoSendSms && !alreadySentSms) {
                alreadySentSms = true
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

            if (PermissionsStatus.callPhonePermissionGranted.value == true) {
                emergencyDelayTimeAnimationIsRunning = true

                val emergencyButtonCancelBack = activity.findViewById<FrameLayout>(R.id.emergencyMessage)
                emergencyButtonCancelBack?.visibility = VISIBLE
                val distressCircle = activity.findViewById<FrameLayout>(R.id.distress_circle)
                distressCircle?.visibility = VISIBLE
                val toastMsg = context.getString(R.string.calling_quick_call_number_tap_cancel_to_stop)
                displayedEmergencyMsg = showLongSnackBar(context, toastMsg, (((distressNumberOfSecsToCancel + 1) * 1000).toInt()))

                val emergencyButtonText = activity.findViewById<TextView>(R.id.emergency_button_text)
                emergencyButtonText.text = "(${(distressNumberOfSecsToCancel + 1)})"

                val emergencyButton = activity.findViewById<ImageView>(R.id.emergency_button)
                val pulsateAnim = AnimationUtils.loadAnimation(context, R.anim.pulsate)
                emergencyButton.startAnimation(pulsateAnim)
                emergencyDelayTimeAnimationSecondsPassed = 0
                startEmergencyTimer(view, context, supportFragmentManager, requestPermissionLauncher, activity)
            } else { // handle permissions as fast as can
                callEmergencyNumber(context, view, supportFragmentManager, requestPermissionLauncher, activity)
            }
        }
    }

    private fun resetEmergencyButton(activity: Activity, context: Context) {
        emergencyIsOn = false
        emergencyCallWasAnswered = false
        stopEmergencyTimer()
        displayedEmergencyMsg?.dismiss()
        emergencyDelayTimeAnimationIsRunning = false
        alreadySentSms = false
        emergencyDelayTimeAnimationSecondsPassed = 0
        val emergencyButton = activity.findViewById<ImageView>(R.id.emergency_button)
        emergencyButton.clearAnimation()
        val emergencyButtonText = activity.findViewById<TextView>(R.id.emergency_button_text)
        emergencyButtonText.text = context.getString(R.string.quick_call_button_caption)
        val emergencyButtonCancelBack = activity.findViewById<FrameLayout>(R.id.emergencyMessage)
        emergencyButtonCancelBack?.visibility = GONE
        val distressCircle = activity.findViewById<FrameLayout>(R.id.distress_circle)
        distressCircle?.visibility = GONE
    }

    private fun callEmergencyNumber(
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
        val emergencyButton = activity.findViewById<ImageView>(R.id.emergency_button_small)
        val emergencyIcon: ImageView = activity.findViewById(R.id.emergency_button_icon)
        val emergencyIconDisabledText: TextView = activity.findViewById(R.id.emergency_button_text_disabled)

        if (shouldEnable == true) {
            emergencyIcon.visibility = VISIBLE
            emergencyButton.alpha = 1f
            emergencyIconDisabledText.visibility = GONE
        } else {
            emergencyIcon.visibility = GONE
            emergencyButton.alpha = 0.4f
            emergencyIconDisabledText.visibility = VISIBLE

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
                emergencyIconDisabledText.setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_NONE)
            }
            emergencyIconDisabledText.setTextSize(TypedValue.COMPLEX_UNIT_SP, newTextSizeSp)

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

    fun startEmergencyTimer(
        view: View,
        context: Context,
        supportFragmentManager: FragmentManager,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        activity: Activity
    ) {
        val emergencyButtonText = activity.findViewById<TextView>(R.id.emergency_button_text)
        countDownTimer = object : CountDownTimer(((distressNumberOfSecsToCancel + 1) * 1000), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                if (emergencyDelayTimeAnimationIsRunning) {
                    emergencyDelayTimeAnimationSecondsPassed++
                    val secondsDiff = (distressNumberOfSecsToCancel + 1) - emergencyDelayTimeAnimationSecondsPassed
                    emergencyButtonText.text = "($secondsDiff)"
                }
            }

            override fun onFinish() {
                if (emergencyDelayTimeAnimationIsRunning &&
                    emergencyDelayTimeAnimationSecondsPassed >= (distressNumberOfSecsToCancel + 1)
                ) {
                    resetEmergencyButton(activity, context)
                    callEmergencyNumber(context, view, supportFragmentManager, requestPermissionLauncher, activity)
                }
            }
        }.start()
    }

    private fun stopEmergencyTimer() {
        stopRepeatingEmergencyMessage()
        countDownTimer?.cancel()
        countDownTimer = null
    }
}
