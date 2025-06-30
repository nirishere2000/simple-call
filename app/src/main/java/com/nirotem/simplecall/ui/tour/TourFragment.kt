package com.nirotem.simplecall.ui.tour


import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_PHONE_STATE
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.TypedValue
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.telephony.emergency.EmergencyNumber
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.nirotem.sharedmodules.statuses.OemDetector
import com.nirotem.sharedmodules.statuses.OemDetector.checkIsOnePlus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.DBHelper.fetchContactsOptimized
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getPhoneNumberFromContactName
import com.nirotem.simplecall.helpers.DBHelper.saveContactsForCallWithoutPermissions
import com.nirotem.simplecall.helpers.DialogManager
import com.nirotem.simplecall.helpers.GeneralUtils.emergencyNumbersByRegion
import com.nirotem.simplecall.helpers.GeneralUtils.handleEmergencySelectContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadUpdatedTourCaption
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadUserAlreadyOpenedTermsAndConditionsOnce
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadUserApprovedTermsAndConditions
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveUserAlreadyOpenedTermsAndConditionsOnce
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveUserApprovedTermsAndConditions
import com.nirotem.simplecall.helpers.isAppBatteryOptimizationIgnored
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.OpenScreensStatus.shouldUpdateSettingsScreens
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.checkForPermissionsChangesAndShowToastIfChanged
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.statuses.SettingsStatus.isPremium
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TourFragment : Fragment() {

    interface TourListener {
        fun onTourCompleted()
    }

    private var listener: TourListener? = null
    private var viewContext: Context? = null
    private var isXiaomi: Boolean = false
    private var openedSettingsForOverlayDraw: Boolean = false
    private lateinit var roleLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    // Initialize your views here
    private lateinit var fragmentView: View
    private lateinit var tourImage: ImageView
    private lateinit var tourImageWithBorder: ImageView
    private lateinit var tourImageContainerWithBorder: FrameLayout
    private lateinit var tourTitle: TextView
    private lateinit var tourDescription: TextView
    private lateinit var stepAcceptAppTermsContainer: LinearLayout
    private lateinit var distressButtonBack: LinearLayout
    private lateinit var distressButtonIcon: ImageView
    private lateinit var distressButtonTextDisabled: TextView
    private lateinit var descriptionTextArea: LinearLayout
    private lateinit var buttonNext: Button
    private lateinit var buttonPrev: Button
    private lateinit var lastPageButtons: LinearLayout
    private var prevAndNextButtonsGap: View? = null
    private lateinit var buttonStart: Button
    private lateinit var buttonOpenBackgroundSettings: Button
    private lateinit var buttonOpenTermsAndUse: Button
    private lateinit var buttonOpenDefaultDialer: Button
    private lateinit var buttonOpenSettingsForOverlayDraw: Button
    private lateinit var stepAcceptAppTermsCheckbox: ImageView
    private lateinit var stepAcceptAppTermsText: TextView
    private lateinit var stepDoneBack: LinearLayout
    //  private lateinit var powerManager: PowerManager
    private lateinit var packageName: String
    private lateinit var tourPages: List<TourPage>
    private lateinit var scrollView: ScrollView
    private lateinit var scrollArrow: ImageView
    private lateinit var gradientView: View
    private lateinit var emergencyNumbersListSpinner: Spinner
    private lateinit var goldNumberSpinner: Spinner
    private lateinit var callsReportSpinner: Spinner
    private lateinit var spinnersContainer: LinearLayout

    // private lateinit var konfettiView: nl.dionsegijn.konfetti.KonfettiView
    private lateinit var constraintLayout: ConstraintLayout

    private var currentPageIndex = 0
    private var currentPageKey = ""
    private var movingForward = true
    private var isPortrait = true
    private val handler = Handler(Looper.getMainLooper())
    private var showA = true
    private var timerEnabled = false
    private var askingForMakingMakingCallPermission = false
    private var appTermsAndUseApproved = false
    private var termsOfUsedOpenedAtLeastOnce = false
    private var contactsList: MutableList<String>? = null
    private var contactsSpinnerAdapter: ArrayAdapter<String>? = null
    private val coroutineScope =
        CoroutineScope(Dispatchers.Main + Job()) // Main thread for UI updates
    private var lastSnackbar: Snackbar? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (parentFragment is TourListener) {
            listener = parentFragment as TourListener
        }

        roleLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    PermissionsStatus.defaultDialerPermissionGranted.value = true

                    val context = requireContext()

                    // Also check for other permissions granted because of default dialer:
                    PermissionsStatus.checkForPermissionsGranted(context)

                    // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
                    // we may not have another chance to save the contacts before the app would run from InCallService
                    saveContactsForCallWithoutPermissions(context, lifecycleScope)
                } else {
                    // עדכון סטטוס ההרשאה במידה והפעולה נכשלה או בוטלה
                    // permissionsViewModel.setDefaultDialerPermission(false)
                }

                updateContent()
            }

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted && PermissionsStatus.callPhonePermissionGranted.value != true) {
                val toastMsg = getString(R.string.phone_permission_granted)
                //Snackbar.make(fragmentView, toastMsg, 8000).show()

                lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
            }
            PermissionsStatus.callPhonePermissionGranted.value = isGranted
            askingForMakingMakingCallPermission = false
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onResume() {
        super.onResume()

        checkForPermissionsChangesAndShowToastIfChanged(requireContext(), requireActivity())

        // checkBatteryOptimization()
        updateContent()
    }

    override fun onDestroy() {
        super.onDestroy()

        try {
            coroutineScope.cancel() // Clean up the coroutine
            stopTimer()
            OpenScreensStatus.isHelpScreenOpened = false
            scrollView.setOnScrollChangeListener(null)
            //    konfettiView.stopGracefully()
        } catch (e: Exception) {
            Log.e("SimplyCall - TourFragment", "onDestroy Error (${e.message})")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        OpenScreensStatus.isHelpScreenOpened = true
        val context = requireContext()
        packageName = context.packageName
        //  powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
        isPortrait =
            context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        shouldUpdateSettingsScreens.value = false

        tourPages = listOf(
            TourPage(
                key = "welcome",
                title = getString(R.string.tour_welcome),
                description = getString(R.string.tour_welcome_text),
                imageRes = SettingsStatus.appLogoResourceSmall,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "powerfulFeatures",
                title = getString(R.string.tour_powerful_features),
                description = getString(R.string.tour_powerful_features_text),
                imageRes = if (isPremium) R.drawable.dots_menu_premium else R.drawable._dotsmenu,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "customizableSettings",
                title = getString(R.string.tour_customizable_settings),
                description = getString(R.string.tour_customizable_settings_text),
                imageRes = R.drawable.menusettingscaption,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "autoAnswer",
                title = getString(R.string.tour_auto_answer),
                description = getString(R.string.tour_auto_answer_text),
                imageRes = R.drawable.auto_answer_second_image, // auto_answer_second_image should be the first
                secondImageRes = R.drawable.autoanswerspecificphoneexample,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "appPermissions",
                title = getString(R.string.tour_app_permissions),
                description = getString(R.string.tour_app_permissions_text),
                imageRes = R.drawable.menupermissionscaption,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "permissionsScreen",
                title = getString(R.string.tour_permissions_screen),
                description = getString(R.string.tour_permissions_screen_text),
                imageRes = if (isPremium) R.drawable.permissionsscreen_premium else R.drawable.permissionsscreen,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "defaultApp",
                title = getString(R.string.tour_default_app),
                description = getString(R.string.tour_default_app_text),
                imageRes = if (isPremium) R.drawable.defaultdialerselection_premium else R.drawable.defaultdialerselection,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "goldNumber",
                title = getString(R.string.tour_gold_number),
                description = getString(R.string.tour_gold_number_text),
                imageRes = R.drawable.goldnumberincontactsexample,
                secondImageRes = R.drawable.gold_number_second_image,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "quickCall",
                title = getString(R.string.tour_quick_call_button_caption),
                description = getString(R.string.tour_quick_call_button_description),
                imageRes = null,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
/*            TourPage(
                key = "callsReport",
                title = getString(R.string.tour_call_report_title),
                description =  getString(R.string.tour_call_report_description),
                imageRes = R.drawable.defaultdialerselection,
                secondImageRes = null,
                isPremium = true,
                isXiaomiOnly = false
            ),*/
            TourPage( // special page - only for Xiaomi
                key = "overlayDrawPermission",
                title = getString(R.string.tour_overlay_draw_permission_xiaomi),
                description = getString(R.string.tour_overlay_draw_permission_text),
                imageRes = null,
                secondImageRes = null,
                isXiaomiOnly = true,
                isPremium = false
            ),
            TourPage(
                key = "lockedScreen",
                title = getString(R.string.tour_screen_lock_title),
                description = getString(R.string.tour_lock_screen_text),
                imageRes = R.drawable.showonlockscreenpermission,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "batterySaver",
                title = getString(R.string.tour_almost_done),
                description = getString(R.string.tour_almost_done_text),
                /* description = "To ensure the app runs smoothly in the background:\n" +
                         "\n" +
                         "1. Open Background Settings.\n" +
                         "2. Set Battery Saver to \"No restrictions\".\n" +
                         "This will prevent interruptions. The app is designed to use minimal battery.",*/
                imageRes = R.drawable.batteryrestrictions,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            ),
            TourPage(
                key = "allDone",
                title = getString(R.string.tour_all_done),
                description = getString(R.string.tour_all_done_text),
                imageRes = null,
                secondImageRes = null,
                isXiaomiOnly = false,
                isPremium = false
            )
        )

        // Inflate the fragment layout

        return if (isPortrait)
            return inflater.inflate(R.layout.fragment_tour, container, false)
        else
            inflater.inflate(R.layout.fragment_tour_landscape, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isXiaomi = (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true))
        fragmentView = view
        viewContext = view.context
        // Initialize components
        scrollView = view.findViewById(R.id.scrollable_tour_description)
        scrollArrow = view.findViewById(R.id.scroll_arrow)
        gradientView = view.findViewById(R.id.gradient_view)


        scrollView.viewTreeObserver.addOnGlobalLayoutListener {
            scrollView.post {
                checkScroll(scrollView.context)
            }
        }

        scrollView.postDelayed({
            checkScroll(view.context)
        }, 50)

        scrollView.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            checkScroll(view.context)
        }
        tourImage = view.findViewById(R.id.tour_image)
        tourImageContainerWithBorder = view.findViewById<FrameLayout>(R.id.tour_image_container_with_border)
        tourImageWithBorder = view.findViewById<ImageView>(R.id.tour_image_with_white_background)
        tourTitle = view.findViewById(R.id.tour_title)
        descriptionTextArea = view.findViewById(R.id.descriptionTextArea)
        stepAcceptAppTermsContainer = view.findViewById(R.id.stepAcceptAppTermsContainer)
        distressButtonBack = view.findViewById(R.id.distressButtonBack)
        distressButtonIcon = view.findViewById(R.id.emergency_button_icon)
        distressButtonTextDisabled = view.findViewById(R.id.quick_call_button_text_disabled)
        tourDescription = view.findViewById(R.id.tour_description)
        buttonNext = view.findViewById(R.id.button_next)
        buttonPrev = view.findViewById(R.id.button_prev)
        lastPageButtons = view.findViewById(R.id.last_page_buttons)
        if (!isPortrait) {
            prevAndNextButtonsGap = view.findViewById(R.id.prevAndNextButtonsGap)
        }
        // konfettiView = view.findViewById(R.id.konfettiView)
        emergencyNumbersListSpinner = view.findViewById(R.id.emergencyNumbersListSpinner)
        goldNumberSpinner = view.findViewById(R.id.goldNumberListSpinner)
        callsReportSpinner =  view.findViewById(R.id.callsReportListSpinner)
        spinnersContainer = view.findViewById(R.id.spinnersContainer)
        constraintLayout = view.findViewById(R.id.tour_fragment)
        buttonOpenDefaultDialer = view.findViewById(R.id.button_open_default_dialer)
        buttonOpenBackgroundSettings = view.findViewById(R.id.button_battery_saver)
        buttonOpenTermsAndUse = view.findViewById(R.id.buttonOpenTermsAndUse)
        buttonOpenSettingsForOverlayDraw =
            view.findViewById(R.id.button_open_settings_for_overlay_draw)
        stepAcceptAppTermsCheckbox = view.findViewById(R.id.step_accept_app_termsCheckbox)
        stepAcceptAppTermsText = view.findViewById(R.id.step_accept_app_termsText)
        stepDoneBack = view.findViewById(R.id.step_is_done_back)
        // buttonClose = view.findViewById(R.id.button_close)

        // Display the first page content
        updateContent()

        // Set listeners for buttons
        buttonNext.setOnClickListener {
            if (viewContext != null) {
                val runningInBackgroundAllowed = PermissionsStatus.canDrawOverlaysPermissionGranted.value == true && PermissionsStatus.backgroundWindowsAllowed.value == true
                if (currentPageKey == "defaultApp" && PermissionsStatus.defaultDialerPermissionGranted.value != true) {
                    // default phone app - user clicked skip
                    showDefaultAppAlertDialog(viewContext!!) // alert him before skipping
                } else if (currentPageKey == "overlayDrawPermission" && (!runningInBackgroundAllowed && !openedSettingsForOverlayDraw)) { // User selected "Skip" so we'll give him alert
                    showOverlayDrawAlertDialog(viewContext!!)
                } else if (currentPageKey == "batterySaver" && (!isAppBatteryOptimizationIgnored(
                        viewContext!!, packageName
                    ))
                ) {
                    showBatterySaverAlertDialog(viewContext!!)
                } else if (currentPageIndex < tourPages.size - 1) {
                    currentPageIndex++
                    movingForward = true
                    updateContent()
                } else { // last page -> button is named 'FINISH"
                    if (appTermsAndUseApproved) {
                        // only here we also save that app terms and use were approved
                        saveUserApprovedTermsAndConditions(
                            appTermsAndUseApproved,
                            fragmentView.context
                        )
                        listener?.onTourCompleted()
                    } else {
                        val toastMsg =
                            getString(R.string.please_approve_the_terms_of_use_before_finishi)
                        // Snackbar.make(fragmentView, toastMsg, 8000).show()
                        lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
                    }
                }
            }
        }

        /*        buttonLast.setOnClickListener {
                    currentPageIndex = tourPages.size - 1
                    updateContent()
                }*/

        buttonPrev.setOnClickListener {
            if (currentPageIndex > 0) {
                currentPageIndex--
                movingForward = false
                //  buttonNext.setTextColor(Color.BLACK)
                // buttonNext.isEnabled = true

                updateContent()
            }
        }

        buttonOpenDefaultDialer.setOnClickListener {
            requestRole(view.context)
        }

        buttonOpenBackgroundSettings.setOnClickListener {
            requestIgnoreBatteryOptimizations()
            buttonNext.visibility = VISIBLE // can finish now
            // Action to learn more, e.g., open a web page
            // Example:
            // val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yourwebsite.com"))
            // startActivity(intent)
        }

        buttonOpenSettingsForOverlayDraw.setOnClickListener {
            openedSettingsForOverlayDraw = true
            buttonNext.text = getString(R.string.tour_next_button) // it will not be skipped anymore
            requestOverlayPermission()
            // Action to learn more, e.g., open a web page
            // Example:
            // val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://yourwebsite.com"))
            // startActivity(intent)
        }

        buttonOpenTermsAndUse.setOnClickListener {
            termsOfUsedOpenedAtLeastOnce = true
            try { // it doesn't have to be opened
                lastSnackbar?.dismiss()
            }
            catch (e: Exception) {

            }
            saveUserAlreadyOpenedTermsAndConditionsOnce(true, view.context)
            //konfettiView.stopGracefully()
            showTermsOfUseDialog(view.context)
        }

        stepAcceptAppTermsCheckbox.setOnClickListener {
            toggleAcceptAppTerms()
        }

        stepAcceptAppTermsText.setOnClickListener {
            toggleAcceptAppTerms()
        }
    }

    private fun toggleAcceptAppTerms() {
        if (!termsOfUsedOpenedAtLeastOnce) {
            val toastMsg =
                getString(R.string.please_open_and_read_terms_of_use_before_proceed)
            // Snackbar.make(fragmentView, toastMsg, 8000).show()
            lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
        } else { // User already opened terms of use at least once
            if (!appTermsAndUseApproved) {
                stepAcceptAppTermsCheckbox.setImageResource(android.R.drawable.checkbox_on_background)
                //  buttonNext.setTextColor(Color.BLACK)
                appTermsAndUseApproved = true
                // buttonNext.isEnabled = true
            } else {
                //  buttonNext.isEnabled = false
                stepAcceptAppTermsCheckbox.setImageResource(android.R.drawable.checkbox_off_background)
                //    buttonNext.setTextColor(Color.parseColor("#747474"))
                appTermsAndUseApproved = false
            }
        }
    }

    fun requestIgnoreBatteryOptimizations() {
        if (viewContext != null) {
            /*            val packageName = viewContext!!.packageName
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")
                        )
                        // פעולה זו תציג מסך מערכת, בו המשתמש יכול לאשר החרגה לאפליקציה
                        viewContext!!.startActivity(intent)*/

            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", viewContext!!.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            viewContext!!.startActivity(intent)
        }
    }

    private fun updateContent(tourPage: TourPage? = null) {
        if (currentPageIndex < 0 || currentPageIndex >= tourPages.size) return
        scrollView.scrollTo(0, 0)
        val page = if (tourPage != null) tourPage else tourPages[currentPageIndex]
        tourTitle.text = page.title
        val context = requireContext()
        val updatedVersionCaption = loadUpdatedTourCaption(context)
        tourDescription.text = page.description
        if (updatedVersionCaption != null && currentPageIndex == 0) { // First page
            tourTitle.text = updatedVersionCaption
        }
        if (page.imageRes != null) {
            tourImage.setImageResource(page.imageRes)
            tourImage.visibility = VISIBLE
        } else {
            tourImage.setImageDrawable(null)
            tourImage.visibility = GONE
        }

        // border for Samsung and Pixel white pictures:
        // should be only for Samsung and Pixel
        tourImageWithBorder.visibility = if (page.key == "overlayDrawPermission") VISIBLE else GONE
        tourImageContainerWithBorder.visibility = if (page.key == "overlayDrawPermission") VISIBLE else GONE

        val layoutParams = tourTitle.layoutParams as ConstraintLayout.LayoutParams
        val marginTopDp = if (page.key == "allDone") 30f else 210f

        // המרה מ-dp לפיקסלים
        val marginTopPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginTopDp,
            resources.displayMetrics
        ).toInt()

        layoutParams.topMargin = marginTopPx
        tourTitle.layoutParams = layoutParams


        currentPageKey = page.key
        //    val descriptionTextAreaParams: ViewGroup.LayoutParams = descriptionTextArea.getLayoutParams()

        buttonPrev.visibility = if (currentPageIndex > 0) View.VISIBLE else View.GONE
        buttonOpenSettingsForOverlayDraw.visibility =  View.GONE
          /*  if (page.key == "overlayDrawPermission") View.VISIBLE else View.GONE*/
        buttonOpenDefaultDialer.visibility = GONE
        buttonOpenBackgroundSettings.visibility = GONE
        buttonOpenTermsAndUse.visibility = GONE
        stepDoneBack.visibility = GONE
        stepAcceptAppTermsContainer.visibility = if (page.key == "allDone") VISIBLE else GONE
        distressButtonBack.visibility = if (page.key == "quickCall") VISIBLE else GONE
        distressButtonIcon.setImageResource(R.drawable.ic_bell_white)
        distressButtonIcon.visibility = if (page.key == "quickCall") VISIBLE else GONE // making sure the button look is enabled for the Tour display
        distressButtonTextDisabled.visibility = GONE // making sure the button look is enabled for the Tour display
        spinnersContainer.visibility = if (page.key == "quickCall" || page.key == "goldNumber" || page.key == "callsReport") VISIBLE else GONE

        // konfettiView.stopGracefully()
        //descriptionTextAreaParams.height = convertDpToPx(255) // Convert dp to pixels
        val isXiaomi = Build.MANUFACTURER.equals("Xiaomi", true)
        val isStepLockScreenOnNotXiaomiAndNotPremium = page.key == "lockedScreen" && (isXiaomi == false) && !isPremium
        val batterSaverShouldBeSkipped = currentPageKey == "batterySaver" && (isAppBatteryOptimizationIgnored(
            viewContext!!, packageName
        ))
        var pageShouldBeSkipped = (page.isPremium && !isPremium) || isStepLockScreenOnNotXiaomiAndNotPremium || batterSaverShouldBeSkipped
        if (pageShouldBeSkipped && (movingForward || currentPageIndex > 0)) {
            if (movingForward) {
                currentPageIndex++ // move next without doing anything
            } else {
                currentPageIndex-- // move prev without doing anything
            }
            updateContent()
            return
        } else if (page.key == "overlayDrawPermission") { // xiaomi Overlay Draw
            buttonNext.text = getString(R.string.tour_next_button)
                /*if (PermissionsStatus.canDrawOverlaysPermissionGranted.value == true && PermissionsStatus.backgroundWindowsAllowed.value == true) getString(R.string.tour_next_button) else getString(
                    R.string.tour_skip_button
                )*/

            /*            val xiaomiOverlayDrawTextHeight = 200
                        val newHeightInPx = convertDpToPx(xiaomiOverlayDrawTextHeight) // Convert dp to pixels
                        descriptionTextAreaParams.height = newHeightInPx
                        descriptionTextArea.layoutParams = descriptionTextAreaParams*/

            if (PermissionsStatus.canDrawOverlaysPermissionGranted.value == true && PermissionsStatus.backgroundWindowsAllowed.value == true) {
                stepDoneBack.visibility = VISIBLE
                buttonOpenBackgroundSettings.visibility = GONE
            }
            else {
                stepDoneBack.visibility = GONE
                buttonOpenBackgroundSettings.visibility = VISIBLE
            }
            buttonOpenTermsAndUse.visibility = GONE
            buttonOpenDefaultDialer.visibility = View.GONE
            lastPageButtons.visibility = View.VISIBLE

            if (checkIsOnePlus()) { // Should be like Pixel
                tourImageWithBorder.setImageResource(R.drawable.other_permissions_pixel)
                tourDescription.text = getString(R.string.tour_overlay_draw_permission_text_pixel)
            }
            else if (OemDetector.current() == OemDetector.Oem.SAMSUNG) {
                tourImageWithBorder.setImageResource(R.drawable.other_permissions_samsung)
                tourDescription.text = getString(R.string.tour_overlay_draw_permission_text_samsung)
            }
            else if (OemDetector.current() == OemDetector.Oem.GOOGLE || OemDetector.current() == OemDetector.Oem.OTHER) { // Also default
                tourImageWithBorder.setImageResource(R.drawable.other_permissions_pixel)
                tourDescription.text = getString(R.string.tour_overlay_draw_permission_text_pixel)
            }
            else { // Xioami
                tourImage.setImageResource(R.drawable.other_permissions_xioami)
                tourImage.visibility = VISIBLE
                tourImageContainerWithBorder.visibility = GONE
                tourDescription.text = getString(R.string.tour_overlay_draw_permission_text)
            }

            if (!isPortrait) {
                prevAndNextButtonsGap?.visibility = GONE
            }
        } else if (page.key == "lockedScreen") { // xiaomi Overlay Draw
            buttonNext.text = getString(R.string.tour_next_button)
/*            if (PermissionsStatus.permissionToShowWhenDeviceLockedAllowed.value == true) getString(R.string.tour_next_button) else getString(
                R.string.tour_skip_button
            )*/

        /*            val xiaomiOverlayDrawTextHeight = 200
                    val newHeightInPx = convertDpToPx(xiaomiOverlayDrawTextHeight) // Convert dp to pixels
                    descriptionTextAreaParams.height = newHeightInPx
                    descriptionTextArea.layoutParams = descriptionTextAreaParams*/

        if (PermissionsStatus.permissionToShowWhenDeviceLockedAllowed.value == true) {
            buttonOpenBackgroundSettings.visibility = GONE

            if (Build.MANUFACTURER.equals("Xiaomi", true)) {
                if (isPremium) { //
                    tourDescription.text = getString(R.string.premium_screen_lock_text)
                    tourImage.setImageResource(R.drawable.lock_screen_all_languages)
                    stepDoneBack.visibility = GONE
                    lastPageButtons.visibility = GONE
                }
                else { // showing the same text about the permission
                    tourImage.setImageResource(R.drawable.showonlockscreenpermission)
                    tourDescription.text = getString(R.string.tour_lock_screen_text)
                    stepDoneBack.visibility = VISIBLE
                    lastPageButtons.visibility = View.VISIBLE
                }
            }
            else { // other device types are not aware of this permission
                // This should be shown only on premium. IF not premium this step should not be shown at all if device is not Xiaomi
                stepDoneBack.visibility = GONE
                lastPageButtons.visibility = View.GONE
                tourImage.setImageResource(R.drawable.lock_screen_all_languages)
                tourDescription.text = getString(R.string.premium_screen_lock_text)
            }
            buttonOpenTermsAndUse.visibility = GONE
            buttonOpenDefaultDialer.visibility = View.GONE
        }
        else {
            stepDoneBack.visibility = GONE
            buttonOpenBackgroundSettings.visibility = VISIBLE
            tourImage.setImageResource(R.drawable.showonlockscreenpermission)
            tourDescription.text = getString(R.string.tour_lock_screen_text)
        }

        if (!isPortrait) {
            prevAndNextButtonsGap?.visibility = GONE
        }


    } else if (page.key == "defaultApp") { // default phone app
            if (PermissionsStatus.defaultDialerPermissionGranted.value == true) { // already default dialer
                lastPageButtons.visibility = View.VISIBLE
                if (!isPortrait) {
                    prevAndNextButtonsGap?.visibility = GONE
                }
                buttonNext.text = getString(R.string.tour_next_button)
                buttonNext.visibility = VISIBLE // can finish now
                stepDoneBack.visibility = VISIBLE
                buttonOpenDefaultDialer.visibility = View.GONE

            } else { // load default dialer request
                buttonOpenDefaultDialer.visibility = View.VISIBLE
                //buttonNext.text = getString(R.string.tour_skip_button)
                buttonNext.text = getString(R.string.tour_next_button)
                stepDoneBack.visibility = GONE
                lastPageButtons.visibility = View.VISIBLE
                if (!isPortrait) {
                    prevAndNextButtonsGap?.visibility = GONE
                }

            }
            buttonOpenTermsAndUse.visibility = GONE
            buttonOpenBackgroundSettings.visibility = GONE

        }
        else if (page.key == "goldNumber") {
            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                loadContactsIntoGoldNumberSpinnerAsync(goldNumberSpinner)
                goldNumberSpinner.visibility = VISIBLE
            }
            else {
                val toastMsg =
                    getString(R.string.cannot_choose_gold_number_without_contacts_permission)
                lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
                goldNumberSpinner.visibility = GONE
                buttonNext.text = getString(R.string.tour_next_button)
            }

            emergencyNumbersListSpinner.visibility = View.GONE
            callsReportSpinner.visibility = View.GONE
            lastPageButtons.visibility = View.GONE
        }
        else if (page.key == "callsReport") {
            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                loadContactsIntoCallsReportSpinnerAsync(callsReportSpinner)
                callsReportSpinner.visibility = VISIBLE
            }
            else {
                val toastMsg =
                    "Cannot add Contacts to the Call Report selection list. Please enable Contacts access via the app's permissions screen or your device settings."
                lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
                callsReportSpinner.visibility = GONE

            }

            emergencyNumbersListSpinner.visibility = View.GONE
            goldNumberSpinner.visibility = GONE
            lastPageButtons.visibility = View.GONE
        }
        else if (page.key == "quickCall") {
            val telephonyManager =
                requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "US"
            //val emergencyNumbers = getEmergencyNumbers(requireContext(), countryCode)

            val emergencyPhoneNumber = loadQuickCallNumber(context)
            val existsQuickCallNumberForQuickCallButtonButWithoutPermission = (emergencyPhoneNumber != null) && (PermissionsStatus.callPhonePermissionGranted.value != true)

            if (existsQuickCallNumberForQuickCallButtonButWithoutPermission) {
                var toastMsg =
                    getString(R.string.phone_permission_required_for_quick_call)
                //Snackbar.make(fragmentView, toastMsg, 8000).show()
                lastSnackbar =  showLongSnackBar(context, toastMsg, anchorView = requireView())
            }
            else if (PermissionsStatus.readContactsPermissionGranted.value != true) {
                val toastMsg = getString(R.string.to_add_contacts_to_the_list_app_must_be_default)

                // Snackbar.make(fragmentView, toastMsg, 8000).show()
                lastSnackbar = showLongSnackBar(context, toastMsg, anchorView = requireView())

            }

// נניח שכל איבר ב-emergencyNumbers הוא אובייקט שמכיל את number ואת emergencyServiceCategories
// ניצור רשימת מחרוזות להצגה, למשל "מספר - קטגוריה"
/*            var emergencyNumbersList = emergencyNumbers.map { number ->
                val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // number.emergencyServiceCategories.toString()
                    number.emergencyNumberSources.toString()
                } else {
                    ""
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${number.number}"
                    //  Log.d("Emergency", "Number: ${number.number}, Service Category: ${number.emergencyServiceCategories.toString()}, Source: ${number.emergencyNumberSources.toString()}")
                } else {
                    ""
                }
            }.distinct()

            if (emergencyNumbersList.isEmpty()) { // add static emergency phone numbers by country
                val listOfStaticEmergencyNumbersPerRegion = countryCode.let { emergencyNumbersByRegion[it] }
                if (listOfStaticEmergencyNumbersPerRegion != null && listOfStaticEmergencyNumbersPerRegion.isNotEmpty()) {
                    emergencyNumbersList = listOfStaticEmergencyNumbersPerRegion
                }
            }*/

// מצא את ה-Spinner (ב-XML שלך יש לו מזהה מתאים)
            //  val spinner: Spinner = findViewById(R.id.spinnerEmergencyNumbers)

// צור ArrayAdapter והגדר אותו לספינר
            //val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, emergencyNumbersList)
            // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            loadContactsIntoEmergencySpinnerAsync(emergencyNumbersListSpinner, emptyList())

            callsReportSpinner.visibility = View.GONE
            goldNumberSpinner.visibility = View.GONE
            lastPageButtons.visibility = View.GONE
        } else if (page.key == "batterySaver") { // Battery Saver
            // One before Last page
            //      buttonNext.visibility = View.GONE


            if (viewContext != null) {
                //val packageName = viewContext!!.packageName
                //val pm = viewContext!!.getSystemService(POWER_SERVICE) as PowerManager

                /*                val usageStatsManager =
                                    viewContext!!.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                                val standbyBucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                    usageStatsManager.appStandbyBucket
                                } else {
                                    null
                                }*/
//                val toastMsg = "powerManager.isIgnoringBatteryOptimizations(packageName) = $powerManager.isIgnoringBatteryOptimizations(packageName), standbyBucket = $standbyBucket"
//                showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
                // (STANDBY_BUCKET_EXEMPTED=5, STANDBY_BUCKET_ACTIVE     = 10 ...)


                if (!isAppBatteryOptimizationIgnored(viewContext!!, packageName)) { // need to ask for to remove from battery optimization
                    lastPageButtons.visibility = VISIBLE
                    if (!isPortrait) {
                        prevAndNextButtonsGap?.visibility = GONE
                    }
                    buttonOpenBackgroundSettings.visibility = VISIBLE
                    stepDoneBack.visibility = GONE
                    buttonNext.text = getString(R.string.tour_skip_button)
                    /*                    Log.d(
                                            "SimplyCall - Tour Fragment",
                                            "1 pm.isIgnoringBatteryOptimizations(packageName) = $pm.isIgnoringBatteryOptimizations(packageName)"
                                        )*/
                } else {
                    lastPageButtons.visibility = View.VISIBLE
                    if (!isPortrait) {
                        prevAndNextButtonsGap?.visibility = GONE
                    }
                    buttonNext.text = getString(R.string.tour_next_button)
                    buttonNext.visibility = VISIBLE // can finish now
                    stepDoneBack.visibility = VISIBLE
                }
            }
            buttonNext.visibility = VISIBLE
            //lastPageButtons.visibility = View.VISIBLE
        } else if (page.key == "allDone") { // We got to the finish line
            // Last page
            //      buttonNext.visibility = View.GONE
            //val terms = getString(R.string.terms_and_conditions, getString(R.string.app_name))

            buttonNext.text = getString(R.string.tour_start_button) // which is "FINISH" now
            if (!isPortrait) {
                prevAndNextButtonsGap?.visibility = VISIBLE
            }
            if (!appTermsAndUseApproved) {
                stepAcceptAppTermsCheckbox.setImageResource(android.R.drawable.checkbox_off_background)
                // buttonNext.setTextColor(Color.parseColor("#747474"))
            }
            //  buttonNext.isEnabled = false

            val termsOfUsedApprovedOrNull = loadUserApprovedTermsAndConditions(viewContext)
            appTermsAndUseApproved = termsOfUsedApprovedOrNull == true
            val termsOfUsedOpenedAtLeastOnceOrNull =
                loadUserAlreadyOpenedTermsAndConditionsOnce(viewContext) // user should not have to open it everytime he visits the Tour
            termsOfUsedOpenedAtLeastOnce = termsOfUsedOpenedAtLeastOnceOrNull == true
            if (termsOfUsedOpenedAtLeastOnce && appTermsAndUseApproved) {
                stepAcceptAppTermsCheckbox.setImageResource(android.R.drawable.checkbox_on_background)
                stepAcceptAppTermsCheckbox.isEnabled = false
                stepAcceptAppTermsText.isEnabled = false
            } else {
                stepAcceptAppTermsCheckbox.setImageResource(android.R.drawable.checkbox_off_background)
                stepAcceptAppTermsCheckbox.isEnabled = true
                stepAcceptAppTermsText.isEnabled = true
            }
            //  false // the user will have to approve terms of use everytime ghe gets here even if he already approved and went back:
            buttonOpenTermsAndUse.visibility = VISIBLE
            lastPageButtons.visibility = View.VISIBLE
            if (viewContext != null) {
                val packageName = viewContext!!.packageName
                // val pm = viewContext!!.getSystemService(POWER_SERVICE) as PowerManager
                val emergencyPhoneNumber = loadQuickCallNumber(context)
                val existsDistressNumberForDistressButtonButWithoutPermission = (emergencyPhoneNumber != null) && (PermissionsStatus.callPhonePermissionGranted.value != true)
                if (PermissionsStatus.defaultDialerPermissionGranted.value != true) { // default dialer was not granted.
                    if (existsDistressNumberForDistressButtonButWithoutPermission) {
                        if (!isAppBatteryOptimizationIgnored(viewContext!!, packageName)) { // still not removed from battery optimization
                            // we need to inform about all 3: default + distress + battery
                            tourDescription.text =
                                getString(R.string.tour_finished_but_without_phone_permission_and_default_app_and_battery_saver)
                        }
                        else { // default + distress
                            tourDescription.text =
                                getString(R.string.tour_finished_but_without_phone_permission_and_default_app)
                        }
                    }
                    else if (!isAppBatteryOptimizationIgnored(viewContext!!, packageName)) { // still not removed from battery optimization
                        tourDescription.text =
                            getString(R.string.tour_finished_but_without_default_app_and_battery_saver)
                    } else {
                        tourDescription.text =
                            getString(R.string.tour_finished_but_without_default_app)
                    }
                } else if (existsDistressNumberForDistressButtonButWithoutPermission) { // still not removed from battery optimization
                    if (!isAppBatteryOptimizationIgnored(viewContext!!, packageName)) { // emergency + battery
                        tourDescription.text =
                            getString(R.string.tour_finished_but_without_phone_permission_and_battery_saver)
                    } else { // only emergency
                        tourDescription.text =
                            getString(R.string.tour_finished_but_without_phone_permission)
                    }
                } else if (!isAppBatteryOptimizationIgnored(viewContext!!, packageName)) { // still not removed from battery optimization
                    tourDescription.text =
                        getString(R.string.tour_finished_but_without_battery_saver)
                }
            }
            buttonNext.visibility = VISIBLE // can finish now

            /*            konfettiView.build()
                            .addColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA)
                            .setDirection(0.0, 359.0)
                            .setSpeed(1f, 5f)
                            .setFadeOutEnabled(true)
                            .setTimeToLive(1500L)
                            .addShapes(Shape.Square, Shape.Circle)
                            .addSizes(Size(12))
                            .setPosition(-50f, konfettiView.width + 50f, -50f, -50f)
                            .streamFor(300, 3700L) // מפעיל 300 קונפטי למשך 5 שניות*/

            //lastPageButtons.visibility = View.VISIBLE
        } else {
            // Pages before the last page
            // buttonNext.visibility = View.VISIBLE
            buttonNext.text = getString(R.string.tour_next_button)
            lastPageButtons.visibility = View.GONE
            if (!isPortrait) {
                prevAndNextButtonsGap?.visibility = VISIBLE
            }
            //lastPageButtons.visibility = View.GONE
        }



        scrollView.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                scrollView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (view?.context != null) {
                    checkScroll(view!!.context)
                }

            }
        })
        // Also reset scroll position
        scrollView.scrollTo(0, 0)

        stopTimer()
        timerEnabled = page.key == "goldNumber" || page.key == "autoAnswer"
        if (timerEnabled) {
            startTimer()
            showA = false
        }

        /*        if (viewContext != null) {
                    checkScroll(viewContext!!)
                }*/

        // descriptionTextArea.layoutParams = descriptionTextAreaParams
        // lastPageButtons.visibility = if (currentPageIndex >= tourPages.size - 2) View.VISIBLE else View.GONE

    }

    private fun alertAboutNoPhonePermission() {
        try {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    CALL_PHONE
                )
            ) {
                // val toastMsg =
                //   getString(R.string.make_calls_permission_was_denied_but_needed)
                val rootView: View = fragmentView.findViewById(android.R.id.content)
                //Snackbar.make(rootView, toastMsg, 8000).show()

                lastSnackbar = Snackbar.make(rootView, getString(R.string.make_calls_permission_was_denied_but_needed), 8000)
                    .setAction(getString(R.string.settings_capital)) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", rootView.context.packageName, null)
                        intent.data = uri
                        rootView.context.startActivity(intent)
                    }
                lastSnackbar?.show()
            }
            else {
                askingForMakingMakingCallPermission = true
                showPermissionsConfirmationDialog(
                    getString(R.string.permission_needed_capital_p),
                    getString(R.string.in_order_to_make_calls_the_application_must_have_the_proper_permission),
                    ::requestCallPhonePermission
                )
            }

        } catch (e: Exception) {
            Log.e("SimplyCall - TourFragment", "alertAboutNoPhonePermission Error (${e.message})")

            val toastMsg =
                getString(R.string.in_order_to_make_calls_the_application_must_have_the_proper_permission)
            val rootView: View = fragmentView.findViewById(android.R.id.content)
            //Snackbar.make(rootView, toastMsg, 8000).show()
            lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
        }
    }

    private fun requestCallPhonePermission() {
        requestPermissionLauncher.launch(CALL_PHONE)
    }

    private fun showPermissionsConfirmationDialog(
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit
    ) {
        val context = requireContext()
        val builder = androidx.appcompat.app.AlertDialog.Builder(context)

        builder.setTitle(msgTitle)
        builder.setMessage(msgText)

        builder.setPositiveButton(context.getString(R.string.ask_permission_capital_a)) { dialog, which ->
            dialog.dismiss()
            onAskPermission()

        }

        builder.setNegativeButton(context.getString(R.string.cancel_capital)) { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    data class TourPage(
        val key: String,
        val title: String,
        val description: String,
        val imageRes: Int?,
        val secondImageRes: Int?,
        val isXiaomiOnly: Boolean,
        val isPremium: Boolean
    )

    /*    fun getEmergencyNumbersAsStrings(
            telephonyManager: TelephonyManager,
            regionCode: String?
        ): List<String> {
            // נסה לקבל את המספרים מהמכשיר
            val emergencyNumbersMap = telephonyManager.getEmergencyNumberList(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED)
            val allEmergencyNumbers = emergencyNumbersMap.values.flatten()

            // אם המכשיר מחזיר מספרים — ניקח רק את המספרים, ללא מידע נוסף
            if (allEmergencyNumbers.isNotEmpty()) {
                return allEmergencyNumbers.map { it.number }.distinct()
            }

            // אם לא קיבלנו מהמכשיר — נעבור למספרים הסטטיים לפי המדינה
            return regionCode?.let { emergencyNumbersByRegion[it] } ?: emptyList()
        }*/


    private fun getEmergencyNumbers(context: Context, regionCode: String): List<EmergencyNumber> {
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_PHONE_STATE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return emptyList()
            }

            val telephonyManager =
                context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return emptyList()

            // קבלת רשימת מספרי חירום מהמערכת
            val emergencyNumbersMap =
                telephonyManager.getEmergencyNumberList(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED)
            val allEmergencyNumbers = emergencyNumbersMap.values.flatten()

            val phoneUtil = PhoneNumberUtil.getInstance()

            // מסננים את הרשימה כך שיוחזרו רק מספרי חירום שמזוהים כמקומיים
            return allEmergencyNumbers.filter { emergencyNumber ->
                try {
                    isValidEmergencyNumberForRegion(emergencyNumber.number, regionCode)
                    // val numberProto = phoneUtil.parse(emergencyNumber.number, regionCode)
                    //   val actualRegion = phoneUtil.getRegionCodeForNumber(numberProto)
                    // אם האזור בפועל תואם לאזור הנתון, המספר נחשב למקומי
                    //  actualRegion.equals(regionCode, ignoreCase = true)
                } catch (e: Exception) {
                    true
                }
            }
        }
        catch (e: Exception) {
            Log.e("SimplyCall - TourFragment", "getEmergencyNumbers Error (${e.message})")
            return emptyList()
        }
        // בדיקת הרשאת קריאת מצב הטלפון

    }

    /*    private fun isEmergencyNumber(context: Context, phone: String, regionCode: String): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10 ומעלה
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val a= telephonyManager.isEmergencyNumber(phone)
                val phoneUtil = PhoneNumberUtil.getInstance()
               // val actualRegion = phoneUtil.getRegionCodeForNumber(numberProto)
                val numberProto = phoneUtil.parse(phone, regionCode)
    val b = phoneUtil.isValidNumber(numberProto)
               return b
                return a
            } else {
                // במכשירים ישנים יותר, אין תמיכה מובנית, אפשר להחזיר false או לנסות מימוש חלופי (למשל עם libphonenumber)
                true //false
            }
        }*/

    fun isValidEmergencyNumberForRegion(number: String, regionCode: String): Boolean {
        // המרה לאותיות גדולות עבור קוד המדינה
        val validNumbers = emergencyNumbersByRegion[regionCode.uppercase()] ?: emptyList()
        return validNumbers.contains(number)
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

    private fun showDefaultAppAlertDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.set_default_phone_app))
        builder.setMessage(getString(R.string.for_handling_calls_it_is_important_to_set_the_app_as_default_text))

        builder.setNegativeButton(getString(R.string.no_set_as_default_capital)) { dialog, which ->
            requestRole(context) // try to set as default
            dialog.dismiss()
        }

        builder.setPositiveButton(getString(R.string.yes_skip_capital)) { dialog, which ->
            currentPageIndex++ // we continue the "next/skip" button click
            movingForward = true
            updateContent()
            dialog.dismiss()
        }

        // Make the dialog non-cancelable (optional)
        builder.setCancelable(false)

        // Show the dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun loadContactsIntoCallsReportSpinnerAsync(spinner: Spinner) {
        /*   val contactsWithEmptyItemOnTop: MutableList<String> = mutableListOf()
           val context = requireContext()
           val callsReportPhoneNumber = loadCallsReportNumber(context)
           *//* goldNumberEnabledToggle.isChecked =
             (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != context.getString(
                 R.string.unknown_capital
             ))*//*


        coroutineScope.launch {
            if (contactsList == null) {
                contactsList = withContext(Dispatchers.IO) {
                    // Load contacts in a background thread
                    if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                        fetchContactsOptimized(context).toMutableList()
                    } else {
                        fetchContacts().toMutableList()
                    }
                }
            }

            if (contactsList != null) {

                contactsList?.let { list ->
                    contactsWithEmptyItemOnTop.addAll(list)
                }

                // add empty item in beginning of Contacts list:
                contactsWithEmptyItemOnTop.add(
                    0,
                    getString(R.string.not_now_click_for_selection)
                )

                // Add an empty string at the top (index 0)

                *//*            if (PermissionsStatus.defaultDialerPermissionGranted.value != true) {
                                val toastMsg =
                                    getString(R.string.to_filter_blocked_contacts_please_set_the_app_as_default)
                                Snackbar.make(requireActivity(), toastMsg, 8000).show()
                            }*//*

                // Update the Spinner on the UI thread
                *//*  contactsSpinnerAdapter =
                      ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactList)*//*
                //           // contactsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                contactsSpinnerAdapter = object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    contactsWithEmptyItemOnTop
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setTextColor(Color.BLACK) // Change color of selected item
                        textView.gravity = Gravity.START // align left or right according to locale
                        return view
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setBackgroundColor(Color.WHITE)
                        textView.setTextColor(Color.BLACK) // Change color of dropdown items
                        if (position == 0) {
                            // we remove the " (Click for selection)" because it's not clear when the spinner is opened
                            textView.text = getString(R.string.not_now_capital)
                        }
                        return view
                    }
                }
                spinner.adapter = contactsSpinnerAdapter

                val contactListIsNotEmpty = !contactsList.isNullOrEmpty()
                spinner.isEnabled = contactListIsNotEmpty // Enable the spinner after loading
                // Set default selection to the first contact if needed
                if (contactListIsNotEmpty) {
                    spinner.setSelection(0)
                }


                if (!callsReportPhoneNumber.isNullOrEmpty() && callsReportPhoneNumber != "Unknown" && callsReportPhoneNumber != getString(
                        R.string.unknown_capital
                    )
                ) {
                    val callsReportContactName = getContactNameFromPhoneNumber(
                        context,
                        callsReportPhoneNumber
                    ) // we don't check for blocked numbers here

                    val spinnerPosition =
                        if (contactsSpinnerAdapter != null) contactsSpinnerAdapter!!.getPosition(
                            callsReportContactName
                        ) else -1

                    if (spinnerPosition >= 0) {
                        callsReportSpinner.setSelection(spinnerPosition)
                        buttonNext.text = getString(R.string.tour_next_button)
                    } else {
                        // Handle the case where the string does not match any Spinner item
                        // For example, set to a default position or show a message
                        callsReportSpinner.setSelection(0) // Setting to the first item
                        buttonNext.text = getString(R.string.tour_skip_button)
                        //Toast.makeText(this, "Item not found. Default selected.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    buttonNext.text = getString(R.string.tour_skip_button)
                }

                callsReportSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedItem = parent.getItemAtPosition(position).toString()
                            // Handle the selected item
                            // Toast.makeText(this@MainActivity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                            if (position > 0) {
                                handleCallsReportSelectContact(context, selectedItem)
                                buttonNext.text = getString(R.string.tour_next_button)
                            } else { // user chose empty item - no gold number
                                handleCallsReportSelectContact(context, null)
                                buttonNext.text = getString(R.string.tour_skip_button)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("Not yet implemented")
                        }
                    }
            }
        }*/
    }

    private fun loadContactsIntoGoldNumberSpinnerAsync(spinner: Spinner) {
        val contactsWithEmptyItemOnTop: MutableList<String> = mutableListOf()
        val context = requireContext()
        val goldPhoneNumber = loadGoldNumber(context)
        /* goldNumberEnabledToggle.isChecked =
             (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != context.getString(
                 R.string.unknown_capital
             ))*/


        coroutineScope.launch {
            if (contactsList == null) {
                contactsList = withContext(Dispatchers.IO) {
                    // Load contacts in a background thread
                    if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                        fetchContactsOptimized(context).toMutableList()
                    } else {
                        fetchContacts().toMutableList()
                    }
                }
            }

            if (contactsList != null) {

                contactsList?.let { list ->
                    contactsWithEmptyItemOnTop.addAll(list)
                }

                // add empty item in beginning of Contacts list:
                contactsWithEmptyItemOnTop.add(
                    0,
                    getString(R.string.not_now_click_for_selection)
                )

                // Add an empty string at the top (index 0)

                /*            if (PermissionsStatus.defaultDialerPermissionGranted.value != true) {
                                val toastMsg =
                                    getString(R.string.to_filter_blocked_contacts_please_set_the_app_as_default)
                                Snackbar.make(requireActivity(), toastMsg, 8000).show()
                            }*/

                // Update the Spinner on the UI thread
                /*  contactsSpinnerAdapter =
                      ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactList)*/
                //           // contactsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                contactsSpinnerAdapter = object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    contactsWithEmptyItemOnTop
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setTextColor(Color.BLACK) // Change color of selected item
                        textView.gravity = Gravity.START // align left or right according to locale
                        return view
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setBackgroundColor(Color.WHITE)
                        textView.setTextColor(Color.BLACK) // Change color of dropdown items
                        if (position == 0) {
                            // we remove the " (Click for selection)" because it's not clear when the spinner is opened
                            textView.text = getString(R.string.not_now_capital)
                        }
                        return view
                    }
                }
                spinner.adapter = contactsSpinnerAdapter

                val contactListIsNotEmpty = !contactsList.isNullOrEmpty()
                spinner.isEnabled = contactListIsNotEmpty // Enable the spinner after loading
                // Set default selection to the first contact if needed
                if (contactListIsNotEmpty) {
                    spinner.setSelection(0)
                }


                if (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != getString(
                        R.string.unknown_capital
                    )
                ) {
                    val goldNumberContactName = getContactNameFromPhoneNumber(
                        context,
                        goldPhoneNumber
                    ) // we don't check for blocked numbers here

                    val spinnerPosition =
                        if (contactsSpinnerAdapter != null) contactsSpinnerAdapter!!.getPosition(
                            goldNumberContactName
                        ) else -1

                    if (spinnerPosition >= 0) {
                        goldNumberSpinner.setSelection(spinnerPosition)
                        buttonNext.text = getString(R.string.tour_next_button)
                    } else {
                        // Handle the case where the string does not match any Spinner item
                        // For example, set to a default position or show a message
                        goldNumberSpinner.setSelection(0) // Setting to the first item
                        buttonNext.text = getString(R.string.tour_skip_button)
                        //Toast.makeText(this, "Item not found. Default selected.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    if (contactListIsNotEmpty) {
                        buttonNext.text = getString(R.string.tour_skip_button)
                    }
                    else {
                        buttonNext.text = getString(R.string.tour_next_button)
                    }
                }

                goldNumberSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedItem = parent.getItemAtPosition(position).toString()
                            // Handle the selected item
                            // Toast.makeText(this@MainActivity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                            if (position > 0) {
                                handleGoldNumberSelectContact(context, selectedItem)
                                buttonNext.text = getString(R.string.tour_next_button)
                            } else { // user chose empty item - no gold number
                                handleGoldNumberSelectContact(context, null)
                                buttonNext.text = getString(R.string.tour_skip_button)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("Not yet implemented")
                        }
                    }
            }
        }
    }

    private fun loadContactsIntoEmergencySpinnerAsync(
        spinner: Spinner,
        emergencyNumbersList: List<String>
    ) {
        val contactsWithEmergencyNumbers: MutableList<String> = mutableListOf()
        val context = requireContext()
        val quickCallPhoneNumber = loadQuickCallNumber(context)
        val quickCallPhoneNumberContact = loadQuickCallNumberContact(context)
        /* goldNumberEnabledToggle.isChecked =
             (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != context.getString(
                 R.string.unknown_capital
             ))*/


        coroutineScope.launch {
            if (contactsList == null && PermissionsStatus.readContactsPermissionGranted.value == true) {
                contactsList = withContext(Dispatchers.IO) {
                    // Load contacts in a background thread
                    if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                        fetchContactsOptimized(context).toMutableList()
                    } else {
                        fetchContacts().toMutableList()
                    }
                }
            }

            // add Contacts to the Emergency list:
            contactsList?.let { list ->
                contactsWithEmergencyNumbers.addAll(list)
            }

            // add emergency list in beginning of Contacts list:
            contactsWithEmergencyNumbers.addAll(0, emergencyNumbersList)
            contactsWithEmergencyNumbers.add(
                0,
                getString(R.string.not_now_click_for_selection)
            ) // User doesn't want distress button now

            if (contactsWithEmergencyNumbers.isNotEmpty()) {
                // Add an empty string at the top (index 0)


                /*            if (PermissionsStatus.defaultDialerPermissionGranted.value != true) {
                                val toastMsg =
                                    getString(R.string.to_filter_blocked_contacts_please_set_the_app_as_default)
                                Snackbar.make(requireActivity(), toastMsg, 8000).show()
                            }*/

                // Update the Spinner on the UI thread
                /*  contactsSpinnerAdapter =
                      ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactList)*/
                //           // contactsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                contactsSpinnerAdapter = object : ArrayAdapter<String>(
                    context,
                    android.R.layout.simple_spinner_item,
                    contactsWithEmergencyNumbers
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setTextColor(Color.BLACK) // Change color of selected item
                        textView.gravity = Gravity.START // align left or right according to locale
                        return view
                    }

                    override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        val view = super.getDropDownView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setBackgroundColor(Color.WHITE)
                        textView.setTextColor(Color.BLACK) // Change color of dropdown items

                        if (position == 0) {
                            // we remove the " (Click for selection)" because it's not clear when the spinner is opened
                            textView.text = getString(R.string.not_now_capital)
                        }

                        return view
                    }
                }
                spinner.adapter = contactsSpinnerAdapter

                val contactListIsNotEmpty = contactsWithEmergencyNumbers.isNotEmpty()
                spinner.isEnabled = contactListIsNotEmpty // Enable the spinner after loading
                buttonNext.text = getString(R.string.tour_next_button)
                // Set default selection to the first contact if needed
                if (contactListIsNotEmpty) {
                    if (!quickCallPhoneNumber.isNullOrEmpty() && quickCallPhoneNumber != "Unknown" && quickCallPhoneNumber != getString(
                            R.string.unknown_capital
                        )
                    ) {
                        //if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                        /*val emergencyNumberContactName = getContactNameFromPhoneNumber(
                            context,
                            emergencyPhoneNumber
                        ) // if it's local found emergency number and not a contact then contact and phone should be the same // (also, we don't check for blocked numbers here)*/

                        var selectedItem =
                            if (quickCallPhoneNumberContact != null) quickCallPhoneNumberContact else quickCallPhoneNumber

                        val spinnerPosition =
                            if (contactsSpinnerAdapter != null) contactsSpinnerAdapter!!.getPosition(
                                selectedItem // by contact if it's a contact or by phone number if it's emergency phone number
                            ) else -1

                        if (spinnerPosition >= 0) {
                            emergencyNumbersListSpinner.setSelection(spinnerPosition)
                            buttonNext.text = getString(R.string.tour_next_button)
                        } else {
                            // Handle the case where the string does not match any Spinner item
                            // For example, set to a default position or show a message
                            var toastMsg: String
                            if (quickCallPhoneNumberContact != null) { // it's Contact that wasn't found
                                toastMsg =
                                    if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                                        getString(R.string.unable_to_display_selection_unexpected_error)
                                    } else {
                                        getString(R.string.cannot_display_selection_contacts_permission_required)
                                    }
                            } else { // since the phone number isn't null - we should have find it in the list
                                // emergencyPhoneNumberContact is null because it's emergency number
                                toastMsg =
                                    if (ContextCompat.checkSelfPermission(context, READ_PHONE_STATE)
                                        != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        getString(R.string.cannot_show_selection_app_must_be_default)
                                    } else {
                                        getString(R.string.unable_to_display_selection_unexpected_error)
                                    }
                            }
                            //Snackbar.make(fragmentView, toastMsg, 8000).show()
                            lastSnackbar = showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
                            emergencyNumbersListSpinner.setSelection(0) // Setting to the first item
                            //Toast.makeText(this, "Item not found. Default selected.", Toast.LENGTH_SHORT).show()
                            buttonNext.text = getString(R.string.tour_skip_button)

                        }
                    } else {
                        buttonNext.text = getString(R.string.tour_skip_button)
                        spinner.setSelection(0)
                    }
                } else {
                    //buttonNext.text = getString(R.string.tour_skip_button)
                    buttonNext.text = getString(R.string.tour_next_button)

                }

                emergencyNumbersListSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedItem = parent.getItemAtPosition(position).toString()
                            // Handle the selected item
                            // Toast.makeText(this@MainActivity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()
                            buttonNext.text = getString(R.string.tour_skip_button)
                            if (position >= 0) {
                                if (position == 0) { // user selected NOT NOW
                                    handleEmergencySelectContact(context, null, false) // We don't need requireView() here because we are not going to show any SnackBar
                                    buttonNext.text = getString(R.string.tour_skip_button)
                                } else if (position <= (emergencyNumbersList.size)) { // user selected Emergency number
                                    handleEmergencySelectContact(context, selectedItem, false, requireView())
                                    buttonNext.text = getString(R.string.tour_next_button)
                                } else {
                                    handleEmergencySelectContact(context, selectedItem, true, requireView())
                                    buttonNext.text = getString(R.string.tour_next_button)
                                }
                            } else { // user chose empty item - but we should never get here - first item is no selection
                                //  goldNumberEnabledToggle.isChecked = false
                                buttonNext.text = getString(R.string.tour_skip_button)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("Not yet implemented")
                        }
                    }
            }
            val itemCount = emergencyNumbersListSpinner.adapter.count;
            if (itemCount <= 1) {
                 buttonNext.text = getString(R.string.tour_next_button)
            }
            emergencyNumbersListSpinner.visibility = if (itemCount > 1) VISIBLE else GONE
        }
    }

/*    private fun handleCallsReportSelectContact(context: Context, selectedContactName: Any?) {
        // We should not have blocked number and we could create an error is we'll return unknown here
        if (selectedContactName != null) {
            val selectedCallsReportContact = selectedContactName.toString()
            val selectedCallsReportPhoneNumber =
                getPhoneNumberFromContactName(context, selectedCallsReportContact)

            saveCallsReportContact(selectedCallsReportContact, context)
            saveCallsReportNumber(selectedCallsReportPhoneNumber, context)
            saveCallsReportIsGoldNumber(false, context)
        }
        else { // Delete Gold Number
            saveCallsReportContact(null, context)
            saveCallsReportNumber(null, context)
            saveCallsReportIsGoldNumber(false, context)
        }
    }*/

    private fun handleGoldNumberSelectContact(context: Context, selectedContactName: Any?) {
        /*        val selectedGoldPhoneNumber = if (PermissionsStatus.defaultDialerPermissionGranted.value == true)
                    getPhoneNumberFromContactNameAndFilterBlocked(context, selectedContactName.toString()) else
                getPhoneNumberFromContactName(context, selectedContactName.toString())*/

        // We should not have blocked number and we could create an error is we'll return unknown here
        if (selectedContactName != null) {
            val selectedGoldNumberContact = selectedContactName.toString()
            val selectedGoldPhoneNumber =
                getPhoneNumberFromContactName(context, selectedGoldNumberContact)

            saveGoldNumberContact(selectedGoldNumberContact, context)
            saveGoldNumber(selectedGoldPhoneNumber, context)
        }
        else { // Delete Gold Number
            saveGoldNumberContact(null, context)
            saveGoldNumber(null, context)
        }
    }

    private fun showBatterySaverAlertDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.battery_saver))
        builder.setMessage(getString(R.string.tour_battery_saver_skipped_but_needed_alert))

        builder.setNegativeButton(getString(R.string.no_open_app_info)) { dialog, which ->
            requestIgnoreBatteryOptimizations()
        }

        builder.setPositiveButton(getString(R.string.yes_skip_capital)) { dialog, which ->
            currentPageIndex++ // we continue the "next/skip" button click
            movingForward = true
            updateContent()
            dialog.dismiss()
        }

        // Make the dialog non-cancelable (optional)
        builder.setCancelable(false)

        // Show the dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun showOverlayDrawAlertDialog(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(getString(R.string.tour_overlay_draw_permission_skipped_alert_title))
        builder.setMessage(getString(R.string.tour_overlay_draw_permission_skipped_alert_text))

        builder.setNegativeButton(getString(R.string.no_open_settings)) { dialog, which ->
            openedSettingsForOverlayDraw = true
            requestOverlayPermission() // try to set as default
        }

        builder.setPositiveButton(getString(R.string.yes_skip_capital)) { dialog, which ->
            currentPageIndex++ // we continue the "next/skip" button click
            movingForward = true
            updateContent()
        }

        // Make the dialog non-cancelable (optional)
        builder.setCancelable(false)

        // Show the dialog
        val dialog = builder.create()
        dialog.show()
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //    if (!Settings.canDrawOverlays(this)) {
            if ("xiaomi" == Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                intent.putExtra("extra_pkgname", requireContext().packageName)
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.request_overlay_permission_alert_title))
                    .setMessage(getString(R.string.request_overlay_permission_alert_text))
                    .setPositiveButton(getString(R.string.go_to_settings)) { _, _ ->
                        startActivity(intent)
                    }
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setCancelable(false)
                    .show()
            } else {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${requireContext().packageName}")
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)

                /*                    val overlaySettings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                    startActivityForResult(overlaySettings, OVERLAY_REQUEST_CODE)*/
            }
        }
        //}
    }

    private fun checkScroll(context: Context) {
        if (scrollView.canScrollVertically(1)) {
            if (scrollArrow.animation == null) {
                val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
                scrollArrow.startAnimation(blinkAnimation)
            }
            scrollArrow.visibility = View.VISIBLE
            gradientView.visibility = View.VISIBLE
        } else {
            scrollArrow.clearAnimation()
            scrollArrow.visibility = View.GONE
            gradientView.visibility = View.GONE
        }
    }

    // הגדרת ה-Runnable במשתנה
    private val imageSwitcherRunnable = object : Runnable {
        override fun run() {
            if (currentPageIndex < 0 || !timerEnabled) return

            val page = tourPages[currentPageIndex]

            if (page.imageRes != null && page.secondImageRes != null && (page.key == "goldNumber" || page.key == "autoAnswer")) {
                if (showA) {
                    tourImage.setImageResource(page.imageRes)
                } else {
                    tourImage.setImageResource(page.secondImageRes)
                }
            }

            showA = !showA
            // הפעלה חוזרת של ה-Runnable לאחר 5000 מילישניות
            handler.postDelayed(this, 5000)
        }
    }

    // התחלת ההפעלה החוזרת
    fun startTimer() {
        timerEnabled = true  // או כל הגדרה אחרת שתוודא שהטיימר פעיל
        handler.postDelayed(imageSwitcherRunnable, 5000)
    }

    // עצירת ה-Runnable
    fun stopTimer() {
        timerEnabled = false
        handler.removeCallbacks(imageSwitcherRunnable)
    }


    private fun canScrollVertically(view: ScrollView, direction: Int): Boolean {
        return view.canScrollVertically(direction)
    }

    private fun showTermsOfUseDialog(context: Context) {
        val title = context.getString(R.string.accept_term_caption)
        val appName = getString(R.string.app_name)
        val acceptButtonText = getString(R.string.accept_terms_button)
        val supportEmail = this.resources.getString(R.string.supportEmail)
        val termsText = getString(R.string.terms_and_conditions, appName, acceptButtonText, supportEmail)
        val okButtonCaption = context.getString(R.string.accept_terms_button_got_it)

        val overlayFragment = DialogManager()
        val args = Bundle().apply {
            putString("title", title)
            putString("text", termsText)
            putString("okButtonCaption", okButtonCaption)
        }
        overlayFragment.arguments = args
        overlayFragment.show(parentFragmentManager, "DialogManagerTag")
    }

    private fun fetchContacts(): List<String> {
        val contactList = mutableListOf<String>()
        val cursor = requireContext().contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ), // Add DISPLAY_NAME to projection
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val displayNameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (displayNameIndex == -1) { // Issue that should not happen
                //Toast.makeText(this, "Column DISPLAY_NAME not found!", Toast.LENGTH_SHORT).show()
                return contactList // Probably will return empty list
            }

            while (it.moveToNext()) {
                val name = it.getString(displayNameIndex) // Safely fetch column value
                if (!name.isNullOrEmpty()) {
                    contactList.add(name)
                }
            }
        }
        return contactList
    }
}
