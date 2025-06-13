package com.nirotem.simplecall

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.nirotem.lockscreen.managers.SharedPreferencesCache.WhenScreenUnlockedBehaviourEasyAppEnum
import com.nirotem.lockscreen.managers.SharedPreferencesCache.saveEasyCallAndAnswerPackageName
import com.nirotem.lockscreen.managers.SharedPreferencesCache.saveWhenScreenUnlockedBehaviourEnum
import com.nirotem.simplecall.databinding.ActivityMainBinding
import com.nirotem.simplecall.helpers.DBHelper.saveContactsForCallWithoutPermissions
import com.nirotem.simplecall.helpers.SharedPreferencesCache.getAppVersionFromCache
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAlreadyPlayedWelcomeSpeech
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadCallActivityLoadedTimeStamp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadLastCallError
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadNumOfTimesWarnedForShowBattery
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllAutoAnswersAsFalse
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowCallWaiting
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowOpeningWhatsApp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAlreadyPlayedWelcomeSpeech
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAppVersionInCache
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveCallActivityLoadedTimeStamp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveContactsMapping
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsAppLoaded
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsGlobalAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveLastCallError
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveLastTimeAnimateGoldNumberInContacts
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveNumOfTimesWarnedForShowBattery
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveShouldCallsStartWithSpeakerOn
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveShouldShowKeypadInActiveCall
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveUpdatedTourCaption
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveUserAlreadyOpenedTermsAndConditionsOnce
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveUserApprovedTermsAndConditions
import com.nirotem.simplecall.helpers.SharedPreferencesCache.setTourShown
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowOpeningWhatsApp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.wasTourAlreadyShown
import com.nirotem.simplecall.helpers.isAppBatteryOptimizationIgnored
import com.nirotem.simplecall.managers.DistressButtonManager.checkForDistressButton
import com.nirotem.simplecall.managers.DistressButtonManager.enableDistressButton
import com.nirotem.simplecall.managers.MessageBoxManager.MessageBoxMessage
import com.nirotem.simplecall.managers.MessageBoxManager.MessageBoxPriority
import com.nirotem.simplecall.managers.MessageBoxManager.onNewMessageReceived
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.managers.SoundPoolManager
import com.nirotem.simplecall.managers.TextToSpeechManager
import com.nirotem.simplecall.managers.VoiceApi
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.isBackgroundWindowsAllowed
import com.nirotem.simplecall.statuses.PermissionsStatus.isShowOnLockScreenAllowed
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.statuses.SettingsStatus.isPremium
import com.nirotem.simplecall.ui.tour.TourDialogFragment
import java.io.File
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var roleLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var alreadyAskedForDefaultDialer = false
    private var alreadyAlertedAboutDefaultDialer = false
    private var emergencyCallWasCanceled = false
    private var emergencyDelayTimeAnimationIsRunning = false
    private var emergencyDelayTimeAnimationSecondsPassed = 0
    private val emergencyDelayTimeAnimationSeconds = 4
    private var countDownTimer: CountDownTimer? = null
    private var askingForMakingMakingCallPermission = false
    private var canStartCheckingForPhonePermission = false
    private var displayedEmergencyMsg: Snackbar? = null
    val voiceApi: VoiceApi = VoiceApiImpl()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        Log.d("SimplyCall - MainActivity", "Main activity loading")
        SoundPoolManager.initialize(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(binding.root)
        checkForPermissions()

        Log.d("SimpleCall - MA432 - AppLoaded", "AppLoaded")
        saveIsAppLoaded(binding.root.context, true)
        saveLastTimeAnimateGoldNumberInContacts(
            binding.root.context,
            true
        ) // so we'll animate per app loaded

        // LANGUAGE:
        val currentLocale = Locale.getDefault()

        var languageCode =
            getModernLanguageCode(currentLocale) // Returns "he" for Hebrew, "en" for English
        var languageEnum = LanguagesEnum.fromCode(languageCode)
        SettingsStatus.currLanguage.value = languageEnum
        var isCurrLanguageValid = isLanguageValid(languageEnum)
        if (isCurrLanguageValid) { // Only some of the languages for now
            /*            languageEnum = LanguagesEnum.ENGLISH
                        languageCode = getModernLanguageCode(Locale.ENGLISH)*/

            // Initialize currLanguage as a MutableList containing the mapped LanguagesEnum
            SettingsStatus.isRightToLeft.value = languageCode == "he" || languageCode == "ar"
            if (SettingsStatus.currLanguage.value != languageEnum) {
                SettingsStatus.currLanguage.value = languageEnum
                //val currLanguage: MutableList<LanguagesEnum> = mutableListOf(languageEnum)
                setLocale(binding.root.context, languageCode)
                recreate();
                return
                // setLayoutDirection(Locale.getDefault())
            }
        }


        // END LANGUAGE
        setSupportActionBar(binding.appBarMain.toolbar)

        // Distress call roleLauncher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            PermissionsStatus.callPhonePermissionGranted.value = isGranted
            askingForMakingMakingCallPermission = false
            handleIfBatterySaverIgnored()
        }

        // הרשמת ה-Launcher
        roleLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // בדיקה אם ההרשאה הושגה
                    // updatePermissionStatus()
                    PermissionsStatus.defaultDialerPermissionGranted.value = true

                    // We want to check and refrsh other permissions:
                    PermissionsStatus.checkForPermissionsGranted(this)

                    // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
                    // we may not have another chance to save the contacts before the app would run from InCallService
                    saveContactsForCallWithoutPermissions(binding.root.context, lifecycleScope)

                    canStartCheckingForPhonePermission = true

                    val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
                    if (isPremiumAndAppNotDefault) {
                        showDefaultAppDialog(getString(R.string.in_order_for_quick_call_to_work_properly_app_must_be_default))
                        enableDistressButton(
                            binding.root,
                            this,
                            this,
                            requestPermissionLauncher,
                            false
                        )
                    }
                    else if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                        enableDistressButton(
                            binding.root, this, this, requestPermissionLauncher,
                            PermissionsStatus.callPhonePermissionGranted.value,
                            true
                        ) // and show a dialog
                    } else { // user already has Phone permission
                        enableDistressButton(
                            binding.root,
                            this,
                            this,
                            requestPermissionLauncher,
                            PermissionsStatus.callPhonePermissionGranted.value
                        ) // no msg but to update button
                        handleIfBatterySaverIgnored()
                    }
                } else {
                    if (!alreadyAlertedAboutDefaultDialer) {
                        showDefaultAppDialog()
                    } else {
                        val toastMsg =
                            getString(R.string.it_is_important_to_set_the_app_as_default)
                        //val rootView: View = findViewById(android.R.id.content)
                        //Snackbar.make(rootView, toastMsg, 8000).show()
                        showLongSnackBar(this, toastMsg, 8000)
                        canStartCheckingForPhonePermission = true

                        val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)

                        if (isPremiumAndAppNotDefault) {
                            showDefaultAppDialog(getString(R.string.in_order_for_quick_call_to_work_properly_app_must_be_default))
                            enableDistressButton(
                                binding.root,
                                this,
                                this,
                                requestPermissionLauncher,
                                false
                            )
                        }
                        else if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                            enableDistressButton(
                                binding.root, this, this, requestPermissionLauncher,
                                PermissionsStatus.callPhonePermissionGranted.value,
                                true
                            ) // and show a dialog
                        } else { // user already has Phone permission
                            enableDistressButton(
                                binding.root,
                                this,
                                this,
                                requestPermissionLauncher,
                                PermissionsStatus.callPhonePermissionGranted.value
                            ) // no msg but to update button
                            handleIfBatterySaverIgnored()
                        }
                    }

                    // עדכון סטטוס ההרשאה במידה והפעולה נכשלה או בוטלה
                    // permissionsViewModel.setDefaultDialerPermission(false)
                }
            }

        loadUI()


        /*        val intent = Intent(this, CallNotificationService::class.java)
                startService(intent)*/

        // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
        // we may not have another chance to save the contacts before the app would run from InCallService
        saveContactsForCallWithoutPermissions(binding.root.context, lifecycleScope)

        if (isPremium) {
            //speechCommandsEnabled = true also should uncomment permission in manifest
            val voiceCommandsInitSuccess = voiceApi.initVoiceCommands(this, this)
            val alreadyPlayedWelcomeSpeech = loadAlreadyPlayedWelcomeSpeech(this)
            val welcomeTitle = getString(R.string.welcome_to_easy_call_and_answer_premium)
            val welcomeText =
                getString(R.string.welcome_text_speech_short) // if (alreadyPlayedWelcomeSpeech)
            //getString(R.string.welcome_text_speech_short)
            // else getString(R.string.welcome_text_speech)

            if (!alreadyPlayedWelcomeSpeech) { // for now playing voice only on first time
                saveAlreadyPlayedWelcomeSpeech(this, true)

                /* להשמיע את הטקסט הארוך רק פעם ראשונה
 ופעם השנייה להשמיע משהו קצר שיגיד ניתן האפליקציה מאזינה*/
                TextToSpeechManager.initSpeech(this, welcomeTitle, welcomeText) {
                    if (voiceApi.isEnabled() && voiceCommandsInitSuccess) {
                        voiceApi.startListenToVoiceCommands(this)
                    }
                }
            }

        }


        //    speak("Hello")

        Log.d("SimplyCall - MainActivity", "Main activity finished loading")
    }

    private fun isLanguageValid(languageEnum: LanguagesEnum): Boolean {
        return languageEnum == LanguagesEnum.ENGLISH
                || languageEnum == LanguagesEnum.SPANISH
                || languageEnum == LanguagesEnum.DUTCH
                || languageEnum == LanguagesEnum.ARABIC
                || languageEnum == LanguagesEnum.FRENCH
                || languageEnum == LanguagesEnum.RUSSIAN
                || languageEnum == LanguagesEnum.GERMAN
                || languageEnum == LanguagesEnum.HEBREW
                || languageEnum == LanguagesEnum.CHINESE
                || languageEnum == LanguagesEnum.INDONESIAN
                || languageEnum == LanguagesEnum.KOREAN
                || languageEnum == LanguagesEnum.JAPANESE
                || languageEnum == LanguagesEnum.HINDI
        /*
                        || languageEnum == LanguagesEnum.CHINEESE
                        || languageEnum == LanguagesEnum.INONEZIC
                        || languageEnum == LanguagesEnum.KOREAN
                        || languageEnum == LanguagesEnum.JAPANISE*/
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.overflow, menu)
            // menu.findItem(R.id.nav_premium_tour_item)?.isVisible = true
        } else {
            // navView.itemTextColor = resources.getColorStateList(R.color.blue_500, theme)
            // navView.setBackgroundColor(resources.getColorStateList(R.color.blue_500, theme))
            menuInflater.inflate(R.menu.navigation_drawer, menu)
            //  menu.findItem(R.id.nav_premium_tour_item)?.isVisible = true
        }
        return result
    }

    // React to menu items click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                if (OpenScreensStatus.isSettingsScreenOpened) {
                    /*                    Toast.makeText(
                                            this,
                                            getString(R.string.settings_screen_is_already_opened), Toast.LENGTH_LONG
                                        )
                                            .show()*/

                    showCustomToastDialog(
                        this,
                        getString(R.string.settings_screen_is_already_opened)
                    )
                } else { // screen is not loaded. Load it
                    OpenScreensStatus.registerSettingsInstanceValue =
                        (if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) OpenScreensStatus.shouldCloseSettingsScreens.value else 0)!!
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    navController.navigate(R.id.nav_settings)
                }
            }

            R.id.nav_permissions -> {
                if (OpenScreensStatus.isPermissionsScreenOpened) {
                    /*                    Toast.makeText(
                                            this,
                                            getString(R.string.permissions_screen_is_already_opened), Toast.LENGTH_LONG
                                        )
                                            .show()*/
                    showCustomToastDialog(
                        this,
                        getString(R.string.permissions_screen_is_already_opened)
                    )
                } else {
                    OpenScreensStatus.registerPermissionsInstanceValue =
                        (if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) OpenScreensStatus.shouldClosePermissionsScreens.value else 0)!!
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    navController.navigate(R.id.nav_permissions)
                }
            }

            R.id.nav_help -> {
                showTourDialog()
            }

            R.id.nav_call_report -> {
                if (OpenScreensStatus.isCallReportScreenOpened) {
                    showCustomToastDialog(
                        this,
                        getString(R.string.call_report_screen_is_already_opened)
                    )
                } else {
                    OpenScreensStatus.registerCallReportInstanceValue =
                        (if (OpenScreensStatus.shouldCloseCallReportScreens.value != null) OpenScreensStatus.shouldCloseCallReportScreens.value else 0)!!
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    navController.navigate(R.id.nav_call_report)
                }
            }

            R.id.nav_premium_tour_item -> {
                if (OpenScreensStatus.isPremiumTourScreenOpened) {
                    showCustomToastDialog(
                        this,
                        getString(R.string.premium_screen_is_already_opened) // premium_screen_is_already_opened
                    )
                } else {
                    OpenScreensStatus.registerPremiumTourInstanceValue =
                        (if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) OpenScreensStatus.shouldClosePremiumTourScreens.value else 0)!!
                    val navController = findNavController(R.id.nav_host_fragment_content_main)
                    navController.navigate(R.id.nav_premium_tour)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            Log.d("SimpleCall - MA432 - onDestroy", "onDestroy")
            saveIsAppLoaded(binding.root.context, false)
            if (isPremium) {
                TextToSpeechManager.shutdown()
            }
        } catch (e: Exception) {

        }
    }

    override fun onStop() {
        try {
            super.onStop()
            saveIsAppLoaded(binding.root.context, false)
        } catch (e: Exception) {

        }
    }

    override fun onResume() {
        try {
            super.onResume()
            saveIsAppLoaded(binding.root.context, true)
        } catch (e: Exception) {

        }
    }

    private fun checkForPermissions() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        PermissionsStatus.defaultDialerPermissionGranted.value =
            telecomManager.defaultDialerPackage == packageName
        PermissionsStatus.canDrawOverlaysPermissionGranted.value = Settings.canDrawOverlays(this)
        //isBackgroundWindowsAllowed(this) // Settings.canDrawOverlays(this)
        PermissionsStatus.backgroundWindowsAllowed.value = isBackgroundWindowsAllowed(this)
        PermissionsStatus.readContactsPermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        PermissionsStatus.writeContactsPermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        PermissionsStatus.readCallLogPermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_CALL_LOG
        ) == PackageManager.PERMISSION_GRANTED
        PermissionsStatus.callPhonePermissionGranted.value = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED

        PermissionsStatus.permissionToShowWhenDeviceLockedAllowed.value =
            isShowOnLockScreenAllowed(this)

        /*        val batteryOPTIMIZATIONS = ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ) == PackageManager.PERMISSION_GRANTED

                Toast.makeText(
                    this,
                    "batteryOPTIMIZATIONS Permission= $batteryOPTIMIZATIONS",
                    Toast.LENGTH_LONG
                ).show()*/


        val allowMakingCallsMode = loadAllowMakingCallsEnum(this)
        val resourceAllowOutgoingCallsMode =
            this.resources?.getString(R.string.allowOutgoingCallsMode)
        SettingsStatus.userAllowOutgoingCallsEnum.value =
            if (allowMakingCallsMode != null) AllowOutgoingCallsEnum.valueOf(allowMakingCallsMode) else
                (if (resourceAllowOutgoingCallsMode != null) AllowOutgoingCallsEnum.valueOf(
                    resourceAllowOutgoingCallsMode
                ) else AllowOutgoingCallsEnum.NO_ONE)

        SettingsStatus.goldNumber.value = loadGoldNumber(this)
        SettingsStatus.goldNumberContact.value = loadGoldNumberContact(this)
        SettingsStatus.allowOpeningWhatsApp.value = shouldAllowOpeningWhatsApp(this)

        /*  if (permissions.any {
                  ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
              }) {
              //ActivityCompat.requestPermissions(this, permissions, 1)
          }*/
    }

    fun canDrawOverlays(): Boolean {
        // Only check on devices running Android M (API 23) or higher
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true  // Permission is granted by default for pre-Marshmallow versions
        }

        try {
            // Create an invisible view to add to the WindowManager
            //val view = View(context)

            val serviceContext = ContextWrapper(applicationContext) // Wrap the application context
            val customView =
                LayoutInflater.from(serviceContext).inflate(R.layout.fragment_incoming_call, null)
            val params = WindowManager.LayoutParams(
                0, 0, // Width and height
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSPARENT
            )

            // Get the WindowManager service
            val wm = serviceContext.getSystemService(WINDOW_SERVICE) as WindowManager
            wm.addView(customView, params)
            wm.removeView(customView)

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun getModernLanguageCode(locale: Locale): String {
        return when (locale.language.lowercase()) {
            "iw" -> "he" // Hebrew
            "in" -> "id" // Indonesian
            else -> locale.language
        }
    }

    data class AppInstance(val name: String, val version: String)

    fun saveAppInstanceToJson(context: Context, appInstance: AppInstance) {
        val gson = Gson()
        val jsonString = gson.toJson(appInstance)
        val file = File(context.filesDir, "appData.json")
        file.writeText(jsonString)
    }

    private fun loadAppInstanceFromJson(context: Context): AppInstance? {
        val file = File(context.filesDir, "appData.json")
        if (!file.exists()) {
            return null  // במקרה שהקובץ לא קיים, מחזירים null או מטפלים בהתאם
        }
        val jsonString = file.readText()
        return Gson().fromJson(jsonString, AppInstance::class.java)
    }

    private fun compareAppVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".")
        val parts2 = v2.split(".")
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val part1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val part2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            if (part1 != part2) {
                return part1 - part2
            }
        }
        return 0
    }

    private fun handleVersionsAndUpdates() {
        try {            //val currentVersionCode = BuildConfig.VERSION_CODE
            val currAppVersion = resources.getString(R.string.currAppVersion)
            val appUpdatedVersionRequiresTour: Boolean = when (currAppVersion.trim()) {
                resources.getString(R.string.app_version_0020) -> resources.getBoolean(R.bool.should_show_tour_0020)
                resources.getString(R.string.app_version_0021) -> resources.getBoolean(R.bool.should_show_tour_0021)
                else -> false
            }

            var lastAppVersionFromCache =
                getAppVersionFromCache(this) // last curr app version we saved in Cache
            // lastAppVersionFromCache = null
            val appInstanceFromJson = loadAppInstanceFromJson(this)
            val currAppVersionFromJson =
                appInstanceFromJson?.version // last curr app version we saved in external JSON

            saveUpdatedTourCaption(null, this) // reset

            if ((currAppVersionFromJson == null && lastAppVersionFromCache != null)
                || (lastAppVersionFromCache == null && currAppVersionFromJson != null)
            ) {
                // This can only happen if we delete the app (not updating) so currAppVersionFromJson is deleted and null
                // But on some devices the cache isn't delete - so lastAppVersionFromCache is not null
                // So we'll need to delete things from cache so, for example, Tour will run again and terms shoudl be opened again
                saveQuickCallNumberContact(null, this)
                saveQuickCallNumber(null, this)
                saveUserApprovedTermsAndConditions(false, this)
                saveUserAlreadyOpenedTermsAndConditionsOnce(false, this)
                saveLastCallError(this, null)
                saveContactsMapping(this, emptyMap())
                setTourShown(this, true) // reset
                saveNumOfTimesWarnedForShowBattery(this, 0)
                saveAllAutoAnswersAsFalse(this)
                saveGoldNumberContact(null, this)
                saveGoldNumber(null, this)

                // Save from Default values:
                val shouldShowKeypadInActiveCall =
                    this.resources?.getBoolean(R.bool.shouldShowKeypadInActiveCall)
                saveShouldShowKeypadInActiveCall(shouldShowKeypadInActiveCall == true, this)

                val resourceAllowAnswerCallsMode =
                    this.resources?.getString(R.string.allowAnswerCallsMode)
                saveAllowAnswerCallsEnum(this, resourceAllowAnswerCallsMode.toString())

                val resourceAllowOutgoingCallsMode =
                    this.resources?.getString(R.string.allowOutgoingCallsMode)
                saveAllowMakingCallsEnum(this, resourceAllowOutgoingCallsMode.toString())

                val resourceGlobalAnswer = this.resources?.getBoolean(R.bool.autoAnswerAllCalls)
                saveIsGlobalAutoAnswer(resourceGlobalAnswer == true, this)

                val resourceAllowCallWaiting =
                    this.resources?.getBoolean(R.bool.shouldAllowCallWaiting)
                saveAllowCallWaiting(resourceAllowCallWaiting == true, this)

                val resourceShouldCallsStartWithSpeakerOn =
                    this.resources?.getBoolean(R.bool.startCallWithSpeakerOn)
                saveShouldCallsStartWithSpeakerOn(
                    resourceShouldCallsStartWithSpeakerOn == true,
                    this
                )

                val resourceAllowOpeningWhatsApp =
                    this.resources?.getBoolean(R.bool.allowOpenWhatsApp)
                saveAllowOpeningWhatsApp(resourceAllowOpeningWhatsApp == true, this)
            } else if (lastAppVersionFromCache == null) { // first time ever the app runs on this device
                // for now not doing anything - Tour should popup
            } else { // we assume Cache was not deleted for an update
                val result = compareAppVersions(lastAppVersionFromCache, currAppVersion)
                if (result < 0) {
                    //println("גרסה מעודכנת גדולה יותר")

                    if (appUpdatedVersionRequiresTour) {
                        // reset tour:
                        saveUpdatedTourCaption(
                            getString(
                                R.string.new_version_installed,
                                currAppVersion
                            ), this
                        )
                        saveUserApprovedTermsAndConditions(false, this)
                        saveUserAlreadyOpenedTermsAndConditionsOnce(false, this)
                        setTourShown(this, true) // reset tour
                    } else { // Show message telling user about new version installed and updates
                        var updatesString = ""
// Use the updates list as needed
                        val updates: Array<String>? = when (currAppVersion.trim()) {
                            resources.getString(R.string.app_version_0020) -> resources.getStringArray(
                                R.array.app_updates_0020
                            )

                            resources.getString(R.string.app_version_0021) -> resources.getStringArray(
                                R.array.app_updates_0021
                            )

                            else -> null
                        }

// אם רוצים להציג את העדכונים במחרוזת אחת, כל עדכון בשורה נפרדת:
                        updatesString = updates?.joinToString(separator = "\n") ?: ""
                        /* } else {
                            println("No updates available for version $currAppVersion")
                        }*/

                        if (updatesString.isNotEmpty()) {
                            /*                            customMessageBoxTitle.text =
                                                            getString(R.string.new_version_installed, currAppVersion)
                                                        customMessageBoxText.text = updatesString
                                                        customMessageBox.visibility = VISIBLE
                                                        customMessageBox.postDelayed({
                                                            customMessageBox.visibility = View.GONE // או View.INVISIBLE, תלוי במה שמתאים לך
                                                        }, 12000)*/

                            val customMessageBox = findViewById<FrameLayout>(R.id.customMessageBox)
                            // customMessageBox.visibility = GONE
                            val customMessageBoxTitle =
                                findViewById<TextView>(R.id.custom_msgbox_title)
                            val customMessageBoxText =
                                findViewById<TextView>(R.id.custom_msgbox_text)

                            val updateAppInstance = MessageBoxMessage(
                                MessageBoxPriority.HIGH,
                                12000,
                                0,
                                getString(R.string.new_version_installed, currAppVersion),
                                updatesString,
                                customMessageBox,
                                customMessageBoxTitle,
                                customMessageBoxText
                            )
                            onNewMessageReceived(updateAppInstance)
                        }
                    }
                    // Load the updates from the appropriate XML file (the system will pick the correct one based on the locale)
                    //val updates = loadUpdatesForCurrentVersion(this, R.xml.app_versions_updates, currAppVersion)


                }/* else if(result > 0) {
                println("גרסת המכשיר גדולה יותר")
            } else {
                println("שתי הגרסאות זהות")
            }*/
                //}
            }
            // Save app version, from default values, our source of truth in this case, to Cache
            saveAppVersionInCache(currAppVersion, this)

            // And to JSON
            val updateAppInstance = AppInstance("CurrApp", currAppVersion)
            saveAppInstanceToJson(this, updateAppInstance)
        } catch (e: Exception) {
            Log.e("SimpleCall - Handle Version", "Error: ${e.message}")
        }
    }

    private fun loadUI() {
        handleVersionsAndUpdates()

        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

//        val currentLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            // באנדרואיד 7.0 ומעלה יש תמיכה בריבוי Locales, לוקחים את הראשון ברשימה
//            binding.root.context.resources.configuration.locales[0]
//        } else {
//            // בגרסאות ישנות יותר של אנדרואיד
//            @Suppress("DEPRECATION")
//            binding.root.context.resources.configuration.locale
//        }
//
//        val languageCode = currentLocale.toLanguageTag()  // Examples: "en", "he", "fr"...
        // Convert the language code to LanguagesEnum
        // val languageEnum = LanguagesEnum.fromCode(languageCode)


        Log.d("SimpleCall - loadUI", "Current Language: ${Locale.getDefault().language}")

        // Load your UI after all permissions and roles are granted
        binding.appBarMain.fab?.setOnClickListener { view ->
            /*  Snackbar.make(view, "Messages", Snackbar.LENGTH_LONG)
                  .setAction("Action", null)
                  .setAnchorView(R.id.fab)
                  .show()*/

            // פתיחת אפליקציית ההודעות עם מספר טלפון
            /*            val phoneNumber = "1234567890"
                        val messageText = "היי, איך אתה?"*/


            if (OpenScreensStatus.isPermissionsScreenOpened) {
                /*                    Toast.makeText(
                                        this,
                                        getString(R.string.permissions_screen_is_already_opened),
                                        Toast.LENGTH_LONG
                                    )
                                        .show()*/
                showCustomToastDialog(
                    this,
                    getString(R.string.permissions_screen_is_already_opened)
                )
            } else {
                navController.navigate(R.id.nav_permissions)
            }

        }

        /* val bottomNavView = findViewById<BottomNavigationView>(R.id.bottom_nav_view)
         bottomNavView.menu.clear()

         bottomNavView.inflateMenu(R.menu.bottom_navigation)*/

        /*     val navPremiumHostFragment =
                 (supportFragmentManager.findFragmentById(R.id.nav_host_premium_fragment_content_main) as NavHostFragment?)!!

             val navStandardHostFragment =
                 (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!*/





        binding.navView?.let {
            appBarConfiguration = if (isPremium) AppBarConfiguration(
                setOf(
                    R.id.nav_settings,
                    R.id.nav_permissions,
                    R.id.nav_help,
                    R.id.nav_call_report
                ),
                binding.drawerLayout
            ) else AppBarConfiguration(
                setOf(
                    R.id.nav_settings,
                    R.id.nav_permissions,
                    R.id.nav_help
                ),
                binding.drawerLayout
            )

            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_settings,
                    R.id.nav_permissions,
                    R.id.nav_help
                ),
                binding.drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
        }

        val isAdvancedDialer = resources.getBoolean(R.bool.isAdvancedDialer)
        val dialerFragmentXmlFile =
            if (isAdvancedDialer) R.id.nav_dialer else R.id.nav_simple_dialer
        binding.appBarMain.contentMain.bottomNavView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_recent_calls, R.id.nav_contacts, dialerFragmentXmlFile
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
        }
        /* binding.appBarMain.contentMain.bottomNavView?.let { bottomNavView ->
             // Post a runnable to ensure the view hierarchy is fully laid out.
             bottomNavView.post {
                 // Get the internal menu view (the first child of BottomNavigationView)
                 val menuView = bottomNavView.getChildAt(0) as? BottomNavigationMenuView
                 menuView?.let { menu ->
                     for (i in 0 until menu.childCount) {
                         // Each child is a BottomNavigationItemView
                         val itemView = menu.getChildAt(i) as? BottomNavigationItemView
                         itemView?.let { navItemView ->
                             // Check if this item corresponds to nav_contacts.
                             if (navItemView.itemData.itemId == R.id.nav_contacts) {
                                 // For example, change the active label's font size:
                                 val activeLabel = navItemView.findViewById<TextView>(com.google.android.material.R.id.largeLabel)
                                 activeLabel?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f) // desired active size

                                 // And change the inactive label's font size:
                                 val inactiveLabel = navItemView.findViewById<TextView>(com.google.android.material.R.id.smallLabel)
                                 inactiveLabel?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f) // desired inactive size

                                 // If you only want to change one of these, adjust accordingly.
                             }
                         }
                     }
                 }
             }
         }*/


        // Toast.makeText(this, "currentLocale = $currentLocale", Toast.LENGTH_SHORT).show()

// למשל, לקבלת קוד השפה
        //   val languageCode = currentLocale.language  // דוגמה: "en", "he", "fr"...
        //     val countryCode = currentLocale.country   // דוגמה: "US", "IL", "FR"...

        /* binding.appBarMain.contentMain.bottomNavView?.let {
             it.selectedItemId =
                 R.id.nav_recent_calls  // This explicitly sets the first item as the default
         }*/
        /*
                val view = LayoutInflater.from(this).inflate(R.layout.distress_button, null)

        // 2. מדוד והנח את ה-View
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)


                val bitmap = getBitmapFromView(view)


                fab.setImageBitmap(bitmap) */

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab?.visibility = GONE


        //val toolbarLogo = findViewById<ImageView>(R.id.toolbar_logo)
        //val layoutParams = toolbarLogo.layoutParams as? FrameLayout.LayoutParams


        SettingsStatus.quickCallNumber.value = loadQuickCallNumber(this)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val appLogo = findViewById<ImageView>(R.id.toolbar_logo)

        if (isPremium) {
            // Must be first
            SettingsStatus.appLogoResourceSmall = R.drawable.gold_phone_icon_transparent_192x192

            // Must be after
            val navAppLogo = findViewById<ImageView>(R.id.navAppLogo)
            val messageBoxAppLogo = findViewById<ImageView>(R.id.msgBoxAppLogo)
            val toolBarLogo600 = findViewById<ImageView>(R.id.toolbar_logo_600dp)

            toolBarLogo600?.setImageResource(SettingsStatus.appLogoResourceSmall)
            messageBoxAppLogo?.setImageResource(SettingsStatus.appLogoResourceSmall) // otherwise it has the default already from design-time
            appLogo?.setImageResource(SettingsStatus.appLogoResourceSmall) // otherwise it has the default already from design-time
            navAppLogo?.setImageResource(SettingsStatus.appLogoResourceSmall)
        }

        appLogo?.setOnClickListener {
            openOptionsMenu()
        }
        checkForDistressButton(
            binding.root,
            this,
            supportFragmentManager,
            requestPermissionLauncher,
            this,
            this
        )
        SettingsStatus.quickCallNumber.observe(this) { emergencyNumber ->
            checkForDistressButton(
                binding.root,
                this,
                supportFragmentManager,
                requestPermissionLauncher,
                this,
                this
            )
        }
        // if (layoutParams != null) {
        //     toolbarLogo.layoutParams = layoutParams
        // }

        //if (!isPremium) {
        //  fab.setImageResource(R.drawable.lockicon)


        // Remove the 3-dots menu


        setSupportActionBar(toolbar)
        // הסרת אייקון ה-Overflow
        // toolbar.overflowIcon = null
        // }


        /*        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val standbyBucket = usageStatsManager.appStandbyBucket

                Toast.makeText(
                    this,
                    "standbyBucket = $standbyBucket", Toast.LENGTH_LONG
                ).show()

                requestIgnoreBatteryOptimizations()*/

        if (!wasTourAlreadyShown(binding.root.context)) {
            if (isPremium) { // save initial lock screen settings adjusted to easy call and answer
                // In case we won't change anything - this should be saved for lockscreen lib
                saveWhenScreenUnlockedBehaviourEnum(
                    this,
                    WhenScreenUnlockedBehaviourEasyAppEnum.EASY_CALL_AND_ANSWER_APP.toString()
                )
                saveEasyCallAndAnswerPackageName(this.packageName, this)
            }

            showTourDialog()
        } else { // Check for Default Phone app and if it's already is then for Battery Saver ignore
            /*            val usageStatsManager =
                            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

                        val APP_STANDBY_BUCKET_ACTIVE_FALLBACK = 10

                        val standbyBucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            usageStatsManager.appStandbyBucket
                        } else {
                            APP_STANDBY_BUCKET_ACTIVE_FALLBACK
                        }*/

            /* val powerManager: PowerManager =
                 binding.root.context.getSystemService(Context.POWER_SERVICE) as PowerManager*/
            val isIgnoringBatteryOptimization = isAppBatteryOptimizationIgnored(this, packageName)
            //powerManager.isIgnoringBatteryOptimizations(packageName)
            //   || standbyBucket == 5 || standbyBucket == 10 // For Samsung - isIgnoringBatteryOptimizations takes more parameters into account so it might be false
            // so if standbyBucket == 5 or 10 meaning actively used, it's enough for us for now

            val lastCallLoadedActivityTimestamp =
                loadCallActivityLoadedTimeStamp(binding.root.context)

            val appCouldNotLoadActivityLastCall =
                (lastCallLoadedActivityTimestamp != null && lastCallLoadedActivityTimestamp.isEmpty())

            val notIsDefaultDialer =
                PermissionsStatus.defaultDialerPermissionGranted.value == null || (!(PermissionsStatus.defaultDialerPermissionGranted.value!!))
            val missingCriticalPermissions =
                notIsDefaultDialer || (!isIgnoringBatteryOptimization) || appCouldNotLoadActivityLastCall
            // || PermissionsStatus.canDrawOverlaysPermissionGranted.value === null || (!(PermissionsStatus.canDrawOverlaysPermissionGranted.value!!))

            if (missingCriticalPermissions) { // These can be achived through the Tour, but if Your already shown then
                //navController.navigate(R.id.nav_permissions)


                if (notIsDefaultDialer && !alreadyAskedForDefaultDialer) { // also pop up default dialer permission ask once
                    alreadyAskedForDefaultDialer = true
                    requestRole(binding.root.context) // Default Dialer

                    // custom dialog that explains default dialer should be granted.
                    /*                alreadyAskedForDefualtDialer = true
                                    val overlayFragment = PermissionsAlertFragment()
                                    val args = Bundle().apply {
                                        putBoolean("IS_MAKE_CALL_PERMISSION", false)
                                        putBoolean("IS_DEFAULT_DIALER_PERMISSION", true)
                                    }
                                    overlayFragment.arguments = args
                                    overlayFragment.show(supportFragmentManager, "PermissionMissingAlertDialogTag")*/

                } else if (!alreadyAskedForDefaultDialer && (!isIgnoringBatteryOptimization || PermissionsStatus.callPhonePermissionGranted.value != true)) {
                    alreadyAskedForDefaultDialer = true
                    canStartCheckingForPhonePermission = true

                    val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
                    if (isPremiumAndAppNotDefault) {
                        showDefaultAppDialog(getString(R.string.in_order_for_quick_call_to_work_properly_app_must_be_default))
                        enableDistressButton(
                            binding.root,
                            this,
                            this,
                            requestPermissionLauncher,
                            false
                        )
                    }
                    else if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                        enableDistressButton(
                            binding.root,
                            this,
                            this,
                            requestPermissionLauncher,
                            PermissionsStatus.callPhonePermissionGranted.value,
                            true
                        ) // and show a dialog
                    } else if (!isIgnoringBatteryOptimization) { // user already has Phone permission
                        enableDistressButton(
                            binding.root,
                            this,
                            this,
                            requestPermissionLauncher,
                            PermissionsStatus.callPhonePermissionGranted.value
                        ) // no msg but to update button
                        handleIfBatterySaverIgnored()
                    }

                    // Check for overlay permission, which is also a must.
                    // else - check for battery optimization

                    //requestIgnoreBatteryOptimizationsIfNeeded(binding.root.context)
                } else if (appCouldNotLoadActivityLastCall && !alreadyAskedForDefaultDialer) {
                    alreadyAskedForDefaultDialer = true
                    saveCallActivityLoadedTimeStamp(binding.root.context) // Don't show again (Show this once per error)
                    requestOverlayPermission(binding.root.context)
                }
            } else {
                // Check for overlay permission, which is also a must.
                // else - check for battery optimization
                // requestIgnoreBatteryOptimizationsIfNeeded(binding.root.context)
            }
        }


        //val REQUEST_RECORD_AUDIO_PERMISSION = 1
        //ActivityCompat.requestPermissions(this, arrayOf(RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        //if (!isTourShown()) {
        // showTourDialog()
        // setTourShown()
        //  }
    }

    private fun requestRole(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // עבור Android 10 ומעלה
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                val roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                roleLauncher.launch(roleRequestIntent)
            }
        } else {
            // עבור Android 9 ומתחת
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (telecomManager.defaultDialerPackage != context.packageName) {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                    putExtra(
                        TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
                        context.packageName
                    )
                }
                roleLauncher.launch(intent)
            }
        }
    }

    fun getBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun showTourDialog() {
        val tourDialog = TourDialogFragment()
        tourDialog.show(supportFragmentManager, "TourDialog")
    }

    private fun showBatterySaveIgnoreDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.battery_saver))
        builder.setMessage(getString(R.string.for_the_app_to_run_smoothly_in_the_background_please_set_the_battery_saver))

        builder.setNegativeButton(getString(R.string.cancel_capital)) { dialog, which ->
            dialog.dismiss()
        }

        builder.setPositiveButton(getString(R.string.yes_open_app_info)) { dialog, which ->
            requestIgnoreBatteryOptimizations(context)
            dialog.dismiss()
        }

        // Make the dialog non-cancelable (optional)
        builder.setCancelable(false)

        // Show the dialog
        val dialog = builder.create()
        dialog.show()
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun showDefaultAppDialog(customMessage: String? = null) {
        if (!alreadyAlertedAboutDefaultDialer) {
            alreadyAlertedAboutDefaultDialer = true

            val builder = AlertDialog.Builder(binding.root.context)
            builder.setTitle(getString(R.string.set_default_phone_app))
            if (customMessage != null) {
                builder.setMessage(customMessage)
            }
            else {
                builder.setMessage(getString(R.string.for_handling_calls_it_is_important_to_set_the_app_as_default_text))
            }

            builder.setNegativeButton(getString(R.string.no_set_as_default_capital)) { dialog, which ->
                requestRole(binding.root.context) // try to set as default
                dialog.dismiss()
            }

            builder.setPositiveButton(getString(R.string.yes_skip_capital)) { dialog, which ->
                canStartCheckingForPhonePermission = true
                val isPremiumAndAppNotDefault = isPremium && (PermissionsStatus.defaultDialerPermissionGranted.value != true)
                if (isPremiumAndAppNotDefault) {
                    showDefaultAppDialog(getString(R.string.in_order_for_quick_call_to_work_properly_app_must_be_default))
                    enableDistressButton(
                        binding.root,
                        this,
                        this,
                        requestPermissionLauncher,
                        false
                    )
                }
                else if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                    enableDistressButton(
                        binding.root, this, this, requestPermissionLauncher,
                        PermissionsStatus.callPhonePermissionGranted.value,
                        true
                    ) // and show a dialog
                } else { // user already has Phone permission
                    enableDistressButton(
                        binding.root,
                        this,
                        this,
                        requestPermissionLauncher,
                        PermissionsStatus.callPhonePermissionGranted.value
                    ) // no msg but to update button
                    handleIfBatterySaverIgnored()
                }
                dialog.dismiss()
            }

            // Make the dialog non-cancelable (optional)
            builder.setCancelable(false)

            // Show the dialog
            val dialog = builder.create()
            dialog.show()
        }
    }

    /*    private fun requestIgnoreBatteryOptimizations() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${packageName}")
                }
                startActivity(intent)
                //startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            }
        }*/

    private fun handleIfBatterySaverIgnored() {
        //val powerManager: PowerManager =
        //   binding.root.context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // val isIgnoringBatteryOptimization = powerManager.isIgnoringBatteryOptimizations(packageName)
        if (!isAppBatteryOptimizationIgnored(this, packageName)) {
            // For Samsung isIgnoringBatteryOptimization might not be enough to detect
            // We may get only: standbyBucket = STANDBY_BUCKET_EXEMPTED
            /*            val APP_STANDBY_BUCKET_ACTIVE_FALLBACK = 10
                        val usageStatsManager =
                            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager*/
            /*            val standbyBucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            usageStatsManager.appStandbyBucket
                        } else {
                            APP_STANDBY_BUCKET_ACTIVE_FALLBACK
                        }*/
            // val standbyBucket = usageStatsManager.appStandbyBucket
            /*            Toast.makeText(
                            this,
                            "standbyBucket = $standbyBucket", Toast.LENGTH_LONG
                        )*/

            // if (standbyBucket != 5 && standbyBucket != 10) { // (STANDBY_BUCKET_EXEMPTED=5, STANDBY_BUCKET_ACTIVE     = 10 ...)
            // And also we will alert only 3 times about this:
            var numOfTimesWarnedForShowBattery =
                loadNumOfTimesWarnedForShowBattery(binding.root.context)
            if (numOfTimesWarnedForShowBattery < 3) {
                numOfTimesWarnedForShowBattery++
                saveNumOfTimesWarnedForShowBattery(
                    binding.root.context,
                    numOfTimesWarnedForShowBattery
                )
                // Show the Battery Saver dialog:
                showBatterySaveIgnoreDialog(binding.root.context)
            }
            // }
        }
    }

    /*    private fun setLayoutDirection(locale: Locale) {
            val config = resources.configuration
            //val config = Configuration()
            Locale.setDefault(locale)
            config.setLocale(locale)
            config.setLayoutDirection(locale)

            // Apply the configuration
            resources.updateConfiguration(config, resources.displayMetrics)

            // Optionally, set layout direction for the current window
            window.decorView.layoutDirection = if (isRTL(locale)) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        }*/

    private fun setLocale(newBase: Context?, languageCode: String): ContextWrapper {
        // val locale = Locale(language)
        // Locale.setDefault(locale)


        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        //val config = Configuration()
        val config = Configuration(newBase?.resources?.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        //val config = resources.configuration
        // config.setLocale(locale)
        // config.setLayoutDirection(locale)
        //  window.decorView.layoutDirection = if (SettingsStatus.isRightToLeft.value == true) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        //resources.updateConfiguration(config, resources.displayMetrics)

        // Refresh the activity to apply changes
        //    recreate()

        return ContextWrapper(newBase?.createConfigurationContext(config))
    }

    override fun attachBaseContext(newBase: Context?) {
        var localeDefault = Locale.getDefault()
        var languageCode =
            getModernLanguageCode(localeDefault) // Returns "he" for Hebrew, "en" for English
        var languageEnum = LanguagesEnum.fromCode(languageCode)
        var isCurrLanguageValid = isLanguageValid(languageEnum)
        SettingsStatus.currLanguage.value = languageEnum

        if (!isCurrLanguageValid) { // Only English and Hebrew for now
            localeDefault = Locale.ENGLISH
        }
        val locale = getModernLanguageCode(localeDefault)
        // val context = updateBaseContextLocale(newBase!!, locale)
        //  super.attachBaseContext(context)

        val context = setLocale(newBase, locale)
        super.attachBaseContext(context)

    }

    private fun requestOverlayPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // בונים את האינטנט בהתאם ליצרן
            val intent = if ("xiaomi" == Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
                Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                    setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    putExtra("extra_pkgname", context.packageName)
                }
            } else {
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            val lastCallErrorMsg = loadLastCallError(context)
            val errorMsgToUser = if (lastCallErrorMsg == null) null else getString(
                R.string.it_looks_like_the_app_has_encountered_an_error_in_last_call_error,
                lastCallErrorMsg
            )
            // טקסט ההודעה יכול להיות זהה לשני המקרים
            var message =
                getString(R.string.it_looks_like_the_app_could_not_load_call_screen_in_background)

            if (errorMsgToUser != null) {
                message = errorMsgToUser
            }
            // מציגים דיאלוג עם ההודעה ובחירת "פתח הגדרות" שיוביל לאינטנט הרלוונטי
            androidx.appcompat.app.AlertDialog.Builder(context)
                .setTitle(getString(R.string.enable_application_to_show_call_screen_from_background))
                .setMessage(message)
                .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                    startActivity(intent)
                }
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(false)
                .show()
        }
    }

    private fun updateBaseContextLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    /*    private fun requestOverlayPermission(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val context = context
                //    if (!Settings.canDrawOverlays(this)) {
                if ("xiaomi" == Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
                    val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                    intent.setClassName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity"
                    )
                    intent.putExtra("extra_pkgname", context.packageName)
                    val message =
                        getString(R.string.it_looks_like_the_app_could_not_load_call_screen_in_background)
                    androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle(getString(R.string.enable_application_to_show_call_screen_from_background))
                        .setMessage(message)
                        .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                            startActivity(intent)
                        }
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setCancelable(false)
                        .show()
                } else {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:${context.packageName}")
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)

                    *//*                    val overlaySettings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                    startActivityForResult(overlaySettings, OVERLAY_REQUEST_CODE)*//*
            }
        }
        //}
    }*/
}