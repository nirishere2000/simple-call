package com.nirotem.simplecall.ui.settings

import android.Manifest.permission.READ_PHONE_STATE
import android.Manifest.permission.SEND_SMS
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.telephony.emergency.EmergencyNumber
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.nirotem.lockscreen.IdleMonitorService
import com.nirotem.lockscreen.managers.SharedPreferencesCache.CustomAppInfo
import com.nirotem.lockscreen.managers.SharedPreferencesCache.WhenScreenUnlockedBehaviourEasyAppEnum
import com.nirotem.lockscreen.managers.SharedPreferencesCache.loadSelectedAppInfo
import com.nirotem.lockscreen.managers.SharedPreferencesCache.loadWhenScreenUnlockedBehaviourEnum
import com.nirotem.lockscreen.managers.SharedPreferencesCache.saveSelectedAppPackage
import com.nirotem.lockscreen.managers.SharedPreferencesCache.saveShouldServiceRun
import com.nirotem.lockscreen.managers.SharedPreferencesCache.saveWhenScreenUnlockedBehaviourEnum
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.VoiceApiImpl
import com.nirotem.simplecall.adapters.DescriptiveEnumAdapter
import com.nirotem.simplecall.helpers.DBHelper.fetchContacts
import com.nirotem.simplecall.helpers.DBHelper.fetchContactsOptimized
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getPhoneNumberFromContactName
import com.nirotem.simplecall.helpers.GeneralUtils.distressButtonSpinnerClickEvent
import com.nirotem.simplecall.helpers.GeneralUtils.emergencyNumbersByRegion
import com.nirotem.simplecall.helpers.GeneralUtils.loadContactsIntoEmergencySpinnerAsync
import com.nirotem.simplecall.helpers.SharedPreferencesCache.isGlobalAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadDistressButtonShouldAlsoSendSmsToGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadDistressNumberOfSecsToCancel
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadDistressNumberShouldAlsoTalk
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowCallWaiting
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowOpeningWhatsApp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallShouldAlsoSendSmsToGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveDistressNumberOfSecsToCancel
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveDistressNumberShouldAlsoTalk
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsGlobalAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveShouldCallsStartWithSpeakerOn
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveShouldShowKeypadInActiveCall
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowCallWaiting
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowOpeningWhatsApp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldCallsStartWithSpeakerOn
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldShowKeypadInActiveCall
import com.nirotem.simplecall.helpers.SpinnersHelper.setupSpinnerWithIcons
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.managers.VoiceApi
import com.nirotem.simplecall.managers.VoiceManager.initVoiceCommandsSettings
import com.nirotem.simplecall.statuses.AllowAnswerCallsEnum
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.OpenScreensStatus.shouldUpdateSettingsScreens
import com.nirotem.simplecall.statuses.PermissionsStatus.askForRecordPermission
import com.nirotem.simplecall.statuses.PermissionsStatus.checkForPermissionsChangesAndShowToastIfChanged
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import com.nirotem.simplecall.statuses.SettingsStatus
import interfaces.DescriptiveEnum
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {
    private val coroutineScope =
        CoroutineScope(Dispatchers.Main + Job()) // Main thread for UI updates
    private lateinit var contactsSpinnerAdapter: ArrayAdapter<String>
    private lateinit var contactsList: MutableList<String>
    private lateinit var goldNumberSpinner: Spinner
    private lateinit var goldNumberEnabledToggle: SwitchMaterial
    private lateinit var distressButtonNumberToggle: SwitchMaterial
    private lateinit var fragmentRoot: View
    private lateinit var scrollView: ScrollView
    private lateinit var scrollArrow: ImageView
    private lateinit var settingsScrollArrowContainer: LinearLayout
    private var serviceIsRunning: Boolean = false
    private lateinit var requestSmsPermissionLauncher: ActivityResultLauncher<String>
    private val voiceApi: VoiceApi = VoiceApiImpl()
    private var appsToLaunch: List<CustomAppInfo>? = null
    private var selectedWhenScreenUnlockedBehaviourEnum: WhenScreenUnlockedBehaviourEasyAppEnum =
        WhenScreenUnlockedBehaviourEasyAppEnum.EASY_CALL_AND_ANSWER_APP
    private var shouldInitQuickCall = true
    private var shouldInitGoldNumber = true

    /*    private var distressNumberSelectedButNoCallPermissionMsgDisplayedCount = 0
        private var toFilterBlockedContactsMsgDisplayedCount = 0
        private var cannotAddContactsPermissionIssueMsgDisplayedCount = 0*/

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            scrollView = view.findViewById(R.id.scrollable_settings_options)
            scrollArrow = view.findViewById(R.id.settings_scroll_arrow)
            settingsScrollArrowContainer = view.findViewById(R.id.settings_scroll_arrow_container)

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

            // PREMIUM ONLY!
            if (SettingsStatus.isPremium) {
                requestSmsPermissionLauncher =
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                        if (isGranted) {
                            saveQuickCallShouldAlsoSendSmsToGoldNumber(view.context, true)
                            val toastMsg =
                                getString(R.string.quick_call_send_sms_to_gold_number_settings_msg)
                            showLongSnackBar(
                                requireContext(),
                                toastMsg,
                                10000,
                                anchorView = requireView()
                            )
                        } else {
                            val distressAlsoSendSmsToGoldToggle =
                                view.findViewById<SwitchMaterial>(R.id.distress_also_send_sms_to_gold_toggle)
                            distressAlsoSendSmsToGoldToggle.isChecked = false
                            suggestManualPermissionGrant(view.context)
                        }
                    }


                val context = requireContext()
                val tabCalls = view.findViewById<LinearLayout>(R.id.tabCalls)
                //  val tabCallsText = view.findViewById<TextView>(R.id.tabCallsText)
                val tabDistressButton = view.findViewById<LinearLayout>(R.id.tabDistressButton)
                val groupDistressButton = view.findViewById<LinearLayout>(R.id.groupDistressButton)
                val groupCalls = view.findViewById<LinearLayout>(R.id.groupCalls)
                val tabLock = view.findViewById<LinearLayout>(R.id.tabLock)
                val groupLock = view.findViewById<LinearLayout>(R.id.groupLock)
                val tabReports = view.findViewById<LinearLayout>(R.id.tabReports)
                tabReports.visibility = GONE
                val tabVoice = view.findViewById<LinearLayout>(R.id.tabVoice)
                val groupVoice = view.findViewById<LinearLayout>(R.id.groupVoice)
                val groupReports = view.findViewById<LinearLayout>(R.id.groupReports)
                val tabOthers = view.findViewById<LinearLayout>(R.id.tabOthers)
                val groupOthers = view.findViewById<LinearLayout>(R.id.groupOthers)

                //val commandsContainer = view.findViewById<LinearLayout>(R.id.settingsCommandsContainerLayout)
                //commandsContainer.visibility = GONE // if there is voice tab then it doesn't need to be in Others too

                serviceIsRunning =
                    isServiceRunning(requireContext(), IdleMonitorService::class.java)
                val lockScreenToggle =
                    view.findViewById<SwitchMaterial>(R.id.show_custom_lock_screen_toggle)
                lockScreenToggle.isChecked = serviceIsRunning
                lockScreenToggle.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        startUnlockService()
                    } else {
                        stopUnlockServiceClick()
                    }
                }

                if (voiceApi.isEnabled()) {
                    initVoiceCommandsSettings(view, requireActivity())
                }

                val intervals = arrayOf("1", "2", "3", "5", "10", "20")
                val quickCallNumOfSecsToCancelAdapter = ArrayAdapter(
                    view.context,
                    android.R.layout.simple_spinner_item,
                    intervals
                )
                quickCallNumOfSecsToCancelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                val distressButtonNumOfSecsToCancelSpinner =
                    view.findViewById<Spinner>(R.id.distress_button_num_of_secs_to_cancel_spinner)
                distressButtonNumOfSecsToCancelSpinner.adapter =
                    quickCallNumOfSecsToCancelAdapter

                var savedValue = loadDistressNumberOfSecsToCancel(context)
                val index = intervals.indexOf(savedValue.toString())
                if (index >= 0) {
                    distressButtonNumOfSecsToCancelSpinner.setSelection(index)
                } else {
                    distressButtonNumOfSecsToCancelSpinner.setSelection(0)
                }

                distressButtonNumOfSecsToCancelSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedValue = intervals[position]
                            saveDistressNumberOfSecsToCancel(selectedValue, context)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {
                            // כלום לא קורה כאן, וזה בסדר
                        }
                    }
                distressButtonNumOfSecsToCancelSpinner.dropDownVerticalOffset = 25
                distressButtonNumOfSecsToCancelSpinner.visibility = VISIBLE

                val distressNumberShouldAlsoTalkToggle =
                    view.findViewById<SwitchMaterial>(R.id.distress_number_should_also_talk_toggle)
                distressNumberShouldAlsoTalkToggle.isChecked =
                    loadDistressNumberShouldAlsoTalk(context)
                distressNumberShouldAlsoTalkToggle.setOnCheckedChangeListener { buttonView, isChecked ->
                    saveDistressNumberShouldAlsoTalk(context, isChecked)
                }

                groupCalls.visibility = VISIBLE

                groupDistressButton.visibility = GONE
                groupLock.visibility = GONE
                groupReports.visibility = GONE
                groupVoice.visibility = GONE
                groupOthers.visibility = GONE

                tabCalls.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = VISIBLE
                    groupLock.visibility = GONE
                    groupReports.visibility = GONE
                    groupVoice.visibility = GONE
                    groupOthers.visibility = GONE
                }

                tabDistressButton.setOnClickListener {
                    groupDistressButton.visibility = VISIBLE
                    groupCalls.visibility = GONE
                    groupLock.visibility = GONE
                    groupReports.visibility = GONE
                    groupVoice.visibility = GONE
                    groupOthers.visibility = GONE

                    if (shouldInitQuickCall) { // for premium we'll upload only when this tab is selected
                        initQuickCallButton(view)
                        shouldInitQuickCall = false
                    }

                    // Send SMS:
                    val quickCallAlsoSendSmsToGoldToggle =
                        view.findViewById<SwitchMaterial>(R.id.distress_also_send_sms_to_gold_toggle)
                    quickCallAlsoSendSmsToGoldToggle.isChecked =
                        loadDistressButtonShouldAlsoSendSmsToGoldNumber(context)
                    // If already checked and no permission:
                    if (quickCallAlsoSendSmsToGoldToggle.isChecked && ContextCompat.checkSelfPermission(
                            context,
                            SEND_SMS
                        )
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestSmsPermissionLauncher.launch(SEND_SMS)
                    }
                    // when checked:
                    quickCallAlsoSendSmsToGoldToggle.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            if (ContextCompat.checkSelfPermission(context, SEND_SMS)
                                != PackageManager.PERMISSION_GRANTED
                            ) { // no permission - ask
                                requestSmsPermissionLauncher.launch(SEND_SMS)
                            } else { // already have permission:
                                saveQuickCallShouldAlsoSendSmsToGoldNumber(context, true)
                                val toastMsg =
                                    getString(R.string.quick_call_send_sms_to_gold_number_settings_msg)
                                showLongSnackBar(
                                    context,
                                    toastMsg,
                                    10000,
                                    anchorView = requireView()
                                )
                            }
                        } else {
                            saveQuickCallShouldAlsoSendSmsToGoldNumber(context, false)
                        }
                    }
                }

                tabLock.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = GONE
                    groupReports.visibility = GONE
                    groupLock.visibility = VISIBLE
                    groupVoice.visibility = GONE
                    groupOthers.visibility = GONE
                }

                setWhenScreenUnlockedBehaviourSpinner(view)
                handleWhenScreenUnlocked(selectedWhenScreenUnlockedBehaviourEnum, view)
                setAppToLaunch(view, context)

                if (voiceApi.isEnabled()) {
                    tabVoice.setOnClickListener {
                        groupDistressButton.visibility = GONE
                        groupCalls.visibility = GONE
                        groupLock.visibility = GONE
                        groupReports.visibility = GONE
                        groupVoice.visibility = VISIBLE
                        groupOthers.visibility = GONE

                        val commandToggleAnswerCalls =
                            view.findViewById<SwitchMaterial>(R.id.command_toggle_answer_calls)
                        val commandToggleGoldNumber =
                            view.findViewById<SwitchMaterial>(R.id.command_toggle_gold_number)
                        val commandToggleUnlockScreen =
                            view.findViewById<SwitchMaterial>(R.id.command_toggle_unlock_screen)
                        val commandToggleDistressButton =
                            view.findViewById<SwitchMaterial>(R.id.command_toggle_distress_button)

                        val atLeastOnceCommandEnabled =
                            commandToggleAnswerCalls.isChecked || commandToggleGoldNumber.isChecked
                                    || commandToggleDistressButton.isChecked || commandToggleUnlockScreen.isChecked

                        if (atLeastOnceCommandEnabled) {
                            askForRecordPermission(view.context, requireActivity())
                        }
                    }
                    tabVoice.visibility = VISIBLE
                } else {
                    tabVoice.visibility = GONE
                }

                /*                tabReports.setOnClickListener {
                                    groupDistressButton.visibility = GONE
                                    groupCalls.visibility = GONE
                                    groupLock.visibility = GONE
                                    groupReports.visibility = VISIBLE
                                    groupOthers.visibility = GONE
                                }*/

                tabOthers.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = GONE
                    groupLock.visibility = GONE
                    groupReports.visibility = GONE
                    groupVoice.visibility = GONE
                    groupOthers.visibility = VISIBLE

                    if (shouldInitGoldNumber) {
                        initGoldNumber(view)
                        shouldInitGoldNumber = false
                    }
                }

                // Insert data into controls:


                /* tabCallsText.setOnClickListener {
                     groupCalls.visibility = VISIBLE
                     Toast.makeText(this.requireContext(), "aerfsdf", Toast.LENGTH_SHORT).show()
                 }*/
            }
            // END PREMIUM ONLY

            loadView(view)

            OpenScreensStatus.shouldCloseSettingsScreens.observe(viewLifecycleOwner) { currInstance ->
                if (currInstance != null && currInstance > OpenScreensStatus.registerSettingsInstanceValue) {
                    parentFragmentManager.popBackStack()
                }
            }

            shouldUpdateSettingsScreens.observe(viewLifecycleOwner) { shouldUpdate ->
                if (shouldUpdate) {
                    loadView(view)
                }
            }

            val context = requireContext()
            val quickCallPhoneNumber = loadQuickCallNumber(context)
            val existsQuickCallNumberForQuickCallButWithoutPermission =
                (quickCallPhoneNumber != null) && (PermissionsStatus.callPhonePermissionGranted.value != true)
            if (existsQuickCallNumberForQuickCallButWithoutPermission) {
                var toastMsg =
                    getString(R.string.phone_permission_required_for_quick_call)
                showLongSnackBar(context, toastMsg, anchorView = requireView())
            } else if (PermissionsStatus.defaultDialerPermissionGranted.value != true) {
                //toFilterBlockedContactsMsgDisplayedCount++
                val toastMsg =
                    getString(R.string.to_filter_blocked_contacts_please_set_the_app_as_default)
                showLongSnackBar(context, toastMsg, anchorView = requireView())
            } else if (PermissionsStatus.readContactsPermissionGranted.value != true) {
                //cannotAddContactsPermissionIssueMsgDisplayedCount++
                val toastMsg =
                    getString(R.string.the_app_cannot_add_contacts_to_the_gold_number_list_since_contacts_permission)

                showLongSnackBar(context, toastMsg, anchorView = requireView())
            }


        } catch (e: Exception) {
            Log.e("SimplyCall - Settings", "Settings Error (${e.message})")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = if (SettingsStatus.isPremium) inflater.inflate(
            R.layout.fragment_premium_settings,
            container,
            false
        ) else inflater.inflate(R.layout.fragment_settings, container, false)
        fragmentRoot = view
        return view
    }

    private fun loadView(view: View): View? {
        OpenScreensStatus.isSettingsScreenOpened = true
        if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
            OpenScreensStatus.shouldClosePermissionsScreens.value =
                OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
        }
        if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
            OpenScreensStatus.shouldClosePremiumTourScreens.value =
                OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
        }

        val currContext = view.context
        var allowReceiveCallsEnabledToggle: SwitchMaterial =
            view.findViewById(R.id.allow_receive_calls_enabled_toggle)
        var allowOutgoingCallsEnabledToggle: SwitchMaterial =
            view.findViewById(R.id.allow_making_calls_enabled_toggle)
        goldNumberEnabledToggle = view.findViewById(R.id.gold_number_enabled_toggle)
        // Setup gold number spinner
        goldNumberSpinner = view.findViewById(R.id.gold_number_contacts_spinner)

        // Access the boolean resource
        // val isHebrew = resources.getBoolean(R.bool.startingLanguageIsHebrew)

        // Receive Calls:

        /*        val receiveCallsDataList = mutableListOf(
                    AllowAnswerCallsEnum.FROM_EVERYONE.description,
                    AllowAnswerCallsEnum.IDENTIFIED_ONLY.description,
                    AllowAnswerCallsEnum.CONTACTS_ONLY.description,
                    AllowAnswerCallsEnum.FAVOURITES_ONLY.description,
                    AllowAnswerCallsEnum.NO_ONE.description
                )*/

        /*        val receiveCallsDataList2 = AllowAnswerCallsEnum.entries.map {
                    currContext.getString(it.descriptionRes)
                }.toMutableList()*/

        val receiveCallsDataList: MutableList<AllowAnswerCallsEnum> =
            AllowAnswerCallsEnum.entries.toMutableList()


// Initialize the adapter with AllowAnswerCallsEnum
        val allowAnswerCallsAdapter = DescriptiveEnumAdapter(
            requireContext(),
            receiveCallsDataList
        )

        // Context here refers to the activity/fragment context
        /*        val adapter = object : ArrayAdapter<String>(
                    currContext,
                    android.R.layout.simple_spinner_item,
                    receiveCallsDataList
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent)
                        val textView = view.findViewById<TextView>(android.R.id.text1)
                        textView.setTextColor(Color.BLACK) // Change color of selected item
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
                        return view
                    }
                }

                val receiveCallsAdapter = ArrayAdapter(
                    currContext,
                    android.R.layout.simple_spinner_item,
                    receiveCallsDataList
                )*/
        val allowReceiveCallsSpinner: Spinner = view.findViewById(R.id.allow_receive_calls_spinner)
        allowAnswerCallsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        allowReceiveCallsSpinner.adapter = allowAnswerCallsAdapter // adapter
        allowReceiveCallsSpinner.dropDownVerticalOffset = 25

        /*        allowReceiveCallsSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            // val selectedItem = parent.getItemAtPosition(position).toString()
                            val selectedItem = parent.getItemAtPosition(position) as AllowAnswerCallsEnum
                            // Handle the selected item
                            // Toast.makeText(this@MainActivity, "Selected: $selectedItem", Toast.LENGTH_SHORT).show()

                            // Example: Convert selected string to enum
                            if (selectedItem == AllowAnswerCallsEnum.NO_ONE) {
                                allowReceiveCallsEnabledToggle.isChecked = false // this should save NO ONE
                            } else {
                                var allowAnswerCallsEnum: AllowAnswerCallsEnum =
                                    AllowAnswerCallsEnum.FROM_EVERYONE
                                if (selectedItem == AllowAnswerCallsEnum.CONTACTS_ONLY) {
                                    allowAnswerCallsEnum = AllowAnswerCallsEnum.CONTACTS_ONLY
                                } else if (selectedItem == AllowAnswerCallsEnum.FAVOURITES_ONLY) {
                                    allowAnswerCallsEnum = AllowAnswerCallsEnum.FAVOURITES_ONLY
                                } else if (selectedItem == AllowAnswerCallsEnum.IDENTIFIED_ONLY) {
                                    allowAnswerCallsEnum = AllowAnswerCallsEnum.IDENTIFIED_ONLY
                                }
                                saveAllowAnswerCallsEnum(currContext, allowAnswerCallsEnum.name)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            TODO("Not yet implemented")
                        }
                    }*/

        /*        allowReceiveCallsSpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            //val itemPosition = parent.getItemAtPosition(position)
                            val selectedDescription = parent.getItemAtPosition(position) as String
                            val selectedEnum = AllowAnswerCallsEnum.entries.find {
                                requireContext().getString(it.descriptionRes) == selectedDescription
                            } ?: AllowAnswerCallsEnum.FROM_EVERYONE

                           // val selectedEnum2 = parent.getItemAtPosition(position) as AllowAnswerCallsEnum

                            if (selectedEnum == AllowAnswerCallsEnum.NO_ONE) {
                                allowReceiveCallsEnabledToggle.isChecked = false
                            } else {
                                allowReceiveCallsEnabledToggle.isChecked = true
                            }

                            saveAllowAnswerCallsEnum(currContext, selectedEnum.name)
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                            // Optional: Handle case when nothing is selected
                        }
                    }*/

        allowReceiveCallsSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Directly cast to AllowAnswerCallsEnum
                    val selectedEnum = parent.getItemAtPosition(position) as AllowAnswerCallsEnum

                    if (selectedEnum == AllowAnswerCallsEnum.NO_ONE) {
                        allowReceiveCallsEnabledToggle.isChecked = false
                    } else {
                        allowReceiveCallsEnabledToggle.isChecked = true
                    }

                    saveAllowAnswerCallsEnum(requireContext(), selectedEnum.name)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optionally handle this case
                }
            }

        val resourceAllowAnswerCallsMode = this.resources.getString(R.string.allowAnswerCallsMode)
        val answerCallsMode = loadAllowAnswerCallsEnum(currContext)
        var selectedEnum =
            if (answerCallsMode != null) AllowAnswerCallsEnum.valueOf(answerCallsMode) else
                (if (resourceAllowAnswerCallsMode.isNotEmpty()) AllowAnswerCallsEnum.valueOf(
                    resourceAllowAnswerCallsMode
                ) else AllowAnswerCallsEnum.NO_ONE)
        allowReceiveCallsEnabledToggle = view.findViewById(R.id.allow_receive_calls_enabled_toggle)
        allowReceiveCallsEnabledToggle.isChecked =
            selectedEnum != AllowAnswerCallsEnum.NO_ONE  // Turns the switch ON
        allowReceiveCallsEnabledToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                selectedEnum = AllowAnswerCallsEnum.NO_ONE
                saveAllowAnswerCallsEnum(currContext, AllowAnswerCallsEnum.NO_ONE.name)
            } else { // user chose to receive calls - change dropdown to initial value
                val resourceAllowAnswerCallsMode =
                    currContext.resources?.getString(R.string.allowAnswerCallsMode)
                if (resourceAllowAnswerCallsMode != null) {
                    selectedEnum = AllowAnswerCallsEnum.valueOf(resourceAllowAnswerCallsMode)
                    saveAllowAnswerCallsEnum(currContext, AllowAnswerCallsEnum.NO_ONE.name)
                }
            }

            //  val selectedIndex = receiveCallsDataList.indexOf(selectedEnum)
            // Retrieve the localized description from the selected enum
            //    val selectedDescription = currContext.getString(selectedEnum.descriptionRes)

// Find the index of the description in the list
            //   val selectedIndex = receiveCallsDataList.indexOf(selectedDescription)

// Optionally handle the case where the description is not found
            // if (selectedIndex != -1) {
            // toggleSpinner.setSelection(selectedIndex)
            //   } else {
            // toggleSpinner.setSelection(0) // Default to first item
            //   }

            handleSwitchToggleByEnum(
                allowReceiveCallsSpinner,
                selectedEnum,
                allowReceiveCallsEnabledToggle,
                receiveCallsDataList
            )

            /*            handleSwitchToggle(
                            allowReceiveCallsSpinner, currContext.getString(selectedEnum.descriptionRes),
                            allowReceiveCallsEnabledToggle, receiveCallsDataList, selectedIndex
                        )*/
        }
        handleSwitchToggleByEnum(
            allowReceiveCallsSpinner,
            selectedEnum,
            allowReceiveCallsEnabledToggle,
            receiveCallsDataList
        )
        /*        handleSwitchToggle(
                    allowReceiveCallsSpinner, currContext.getString(selectedEnum.descriptionRes),
                    allowReceiveCallsEnabledToggle, receiveCallsDataList, null
                )*/

        // Outgoing Calls:

        val allowOutgoingCallsDataList: MutableList<AllowOutgoingCallsEnum> =
            AllowOutgoingCallsEnum.entries.toMutableList()


// Initialize the adapter with AllowAnswerCallsEnum
        val outgoingListAdapter = DescriptiveEnumAdapter(
            requireContext(),
            allowOutgoingCallsDataList
        )

        /*        val allowOutgoingCallsDataList = mutableListOf(
                    AllowOutgoingCallsEnum.TO_EVERYONE.description,
                    AllowOutgoingCallsEnum.CONTACTS_ONLY.description,
                    AllowOutgoingCallsEnum.FAVOURITES_ONLY.description,
                    AllowOutgoingCallsEnum.NO_ONE.description
                )*/

        /*  val outgoingListAdapter = object : ArrayAdapter<String>(
              currContext,
              android.R.layout.simple_spinner_item,
              allowOutgoingCallsDataList
          ) {
              override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                  val view = super.getView(position, convertView, parent)
                  val textView = view.findViewById<TextView>(android.R.id.text1)
                  textView.setTextColor(Color.BLACK) // Change color of selected item
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
                  return view
              }
          }*/

        /*        val allowOutgoingCallsAdapter = ArrayAdapter(
                    currContext,
                    android.R.layout.simple_spinner_item,
                    allowOutgoingCallsDataList
                )*/

        val allowOutgoingCallsSpinner: Spinner = view.findViewById(R.id.allow_making_calls_spinner)
        // allowOutgoingCallsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        outgoingListAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        allowOutgoingCallsSpinner.adapter = outgoingListAdapter
        allowOutgoingCallsSpinner.dropDownVerticalOffset = 25

        allowOutgoingCallsSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Directly cast to AllowAnswerCallsEnum
                    val selectedEnum = parent.getItemAtPosition(position) as AllowOutgoingCallsEnum

                    if (selectedEnum == AllowOutgoingCallsEnum.NO_ONE) {
                        allowOutgoingCallsEnabledToggle.isChecked = false
                    } else {
                        allowOutgoingCallsEnabledToggle.isChecked = true
                    }

                    saveAllowMakingCallsEnum(requireContext(), selectedEnum.name)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optionally handle this case
                }
            }

        /*  allowOutgoingCallsSpinner.onItemSelectedListener =
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

                      // Example: Convert selected string to enum
                      //לעשות בחירה על פי מפתח על מנת שאפשר יהיה לשים ברשימה תרגומים
                      if (selectedItem == AllowOutgoingCallsEnum.NO_ONE.description) {
                          allowOutgoingCallsEnabledToggle.isChecked = false // this should save NO ONE
                      } else {
                          var allowOutgoingCallsEnum: AllowOutgoingCallsEnum =
                              AllowOutgoingCallsEnum.TO_EVERYONE
                          if (selectedItem == AllowOutgoingCallsEnum.CONTACTS_ONLY.description) {
                              allowOutgoingCallsEnum = AllowOutgoingCallsEnum.CONTACTS_ONLY
                              *//*                        if (view != null) {
                                                        var toastMsg = "Contacts tab will contain all Contacts now."
                                                        Snackbar.make(view, toastMsg, 8000).show()
                                                    }*//*
                        } else if (selectedItem == AllowOutgoingCallsEnum.FAVOURITES_ONLY.description) {
                            allowOutgoingCallsEnum = AllowOutgoingCallsEnum.FAVOURITES_ONLY
                            *//*                        if (view != null) {
                                                        var toastMsg = "Contacts tab will contain Favorites only now."
                                                        Snackbar.make(view, toastMsg, 8000).show()
                                                    }*//*
                        }
                        saveAllowMakingCallsEnum(currContext, allowOutgoingCallsEnum.name)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }*/


        val resourceAllowOutgoingCallsMode =
            this.resources.getString(R.string.allowOutgoingCallsMode)
        val allowMakingCallsMode = loadAllowMakingCallsEnum(currContext)
        var selectedOutgoingCallsEnum =
            if (allowMakingCallsMode != null) AllowOutgoingCallsEnum.valueOf(allowMakingCallsMode) else
                (if (resourceAllowOutgoingCallsMode.isNotEmpty()) AllowOutgoingCallsEnum.valueOf(
                    resourceAllowOutgoingCallsMode
                ) else AllowOutgoingCallsEnum.NO_ONE)
        allowOutgoingCallsEnabledToggle = view.findViewById(R.id.allow_making_calls_enabled_toggle)
        allowOutgoingCallsEnabledToggle.isChecked =
            selectedOutgoingCallsEnum != AllowOutgoingCallsEnum.NO_ONE  // Turns the switch ON
        allowOutgoingCallsEnabledToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                selectedOutgoingCallsEnum = AllowOutgoingCallsEnum.NO_ONE
                saveAllowMakingCallsEnum(currContext, AllowOutgoingCallsEnum.NO_ONE.name)
            } else { // user chose to receive calls - change dropdown to initial value
                val resourceAllowMakingCallsMode =
                    currContext.resources?.getString(R.string.allowOutgoingCallsMode)
                if (resourceAllowMakingCallsMode != null) {
                    selectedOutgoingCallsEnum =
                        AllowOutgoingCallsEnum.valueOf(resourceAllowMakingCallsMode)
                    saveAllowMakingCallsEnum(currContext, selectedOutgoingCallsEnum.name)
                }
            }

            handleSwitchToggleByEnum(
                allowOutgoingCallsSpinner,
                selectedOutgoingCallsEnum,
                allowOutgoingCallsEnabledToggle,
                allowOutgoingCallsDataList
            )
        }
        /*        handleSwitchToggle(
                    allowOutgoingCallsSpinner, selectedOutgoingCallsEnum.description,
                    allowOutgoingCallsEnabledToggle, allowOutgoingCallsDataList
                )*/
        handleSwitchToggleByEnum(
            allowOutgoingCallsSpinner,
            selectedOutgoingCallsEnum,
            allowOutgoingCallsEnabledToggle,
            allowOutgoingCallsDataList
        )

        /** Gold Number: */
        if (!SettingsStatus.isPremium) {
            initGoldNumber(view)
        }
        else {
            shouldInitGoldNumber = true
        }

        if (!SettingsStatus.isPremium) { // for premium we'll upload only when tab is selected
            initQuickCallButton(view)
        }
        else {
            shouldInitQuickCall = true
        }

        val startWithSpeakerOnToggle =
            view.findViewById<SwitchMaterial>(R.id.starts_with_speaker_on_toggle)
        startWithSpeakerOnToggle.isChecked = shouldCallsStartWithSpeakerOn(currContext)
        startWithSpeakerOnToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            saveShouldCallsStartWithSpeakerOn(isChecked, currContext)
        }

        val answerCallsAutomaticallyToggle =
            view.findViewById<SwitchMaterial>(R.id.should_answer_all_calls_auto_toggle)
        answerCallsAutomaticallyToggle.isChecked = isGlobalAutoAnswer(currContext)
        answerCallsAutomaticallyToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            saveIsGlobalAutoAnswer(isChecked, currContext)
        }

        val allowCallWaitingToggle =
            view.findViewById<SwitchMaterial>(R.id.allow_call_waiting_toggle)
        allowCallWaitingToggle.isChecked = shouldAllowCallWaiting(currContext)
        allowCallWaitingToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            saveAllowCallWaiting(isChecked, currContext)

        }

        val shouldShowKeypadInActiveCallToggle =
            view.findViewById<SwitchMaterial>(R.id.show_keypad_inside_calls_toggle)
        shouldShowKeypadInActiveCallToggle.isChecked = shouldShowKeypadInActiveCall(currContext)
        shouldShowKeypadInActiveCallToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            saveShouldShowKeypadInActiveCall(isChecked, currContext)
        }

        val allowOpeningWhatsAppToggle =
            view.findViewById<SwitchMaterial>(R.id.allow_opening_whatsapp_toggle)
        allowOpeningWhatsAppToggle.isChecked = shouldAllowOpeningWhatsApp(currContext)
        allowOpeningWhatsAppToggle.setOnCheckedChangeListener { buttonView, isChecked ->
            saveAllowOpeningWhatsApp(isChecked, currContext)
        }


        // Sound guidance toggle
        /*        val soundToggle: SwitchMaterial = view.findViewById(R.id.sound_guidance_toggle)
                soundToggle.setOnCheckedChangeListener { _, isChecked ->
                    Toast.makeText(
                        requireContext(),
                        if (isChecked) "Sound Guidance Enabled" else "Sound Guidance Disabled",
                        Toast.LENGTH_SHORT
                    ).show()
                }*/

        // Call report contact button
        /*        val contactButton: Button = view.findViewById(R.id.select_contact_button)
                contactButton.setOnClickListener {
                    // Handle contact selection logic
                   // Toast.makeText(requireContext(), "Select a contact", Toast.LENGTH_SHORT).show()
                   // val REQUEST_CODE_PICK_CONTACT = 1
                    val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
                    //startActivityForResult(pickContactIntent, REQUEST_CODE_PICK_CONTACT)
                    ContextCompat.startActivity(requireContext(), pickContactIntent, null)

                }*/

        // Report interval spinner
        /*        val intervalSpinner: Spinner = view.findViewById(R.id.report_interval_spinner)
                val intervals = arrayOf("1", "3", "7", "14", "30")
                val intervalAdapter = ArrayAdapter(
                    currContext,
                    android.R.layout.simple_spinner_item,
                    intervals
                )
                intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                intervalSpinner.adapter = intervalAdapter*/

        // Contacts Spinner:
        // val spinner = view.findViewById<Spinner>(R.id.contacts_spinner)
        //   spinner.isEnabled = false // Disable the spinner initially

        // Check for permission and load contacts
        //   if (PermissionsStatus.readContactsPermissionGranted.value !== null && PermissionsStatus.readContactsPermissionGranted.value!!) {
        //      loadContactsIntoSpinnerAsync(spinner)

        //   } else {
        // Show Phone Number field
        //   }


        return view
    }

    private fun initGoldNumber(view: View) {
        try {
            val context = requireContext()
            //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            goldNumberEnabledToggle.isEnabled =
                PermissionsStatus.readContactsPermissionGranted.value == true
            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                loadContactsIntoSpinnerAsync(goldNumberSpinner)
            } else {
                val allowOutgoingCallsSpinner: Spinner = view.findViewById(R.id.allow_making_calls_spinner)

                goldNumberSpinner.isEnabled = false
                goldNumberSpinner.post { // make empty gold number spinner disabled but with the height of allowOutgoingCallsSpinner
                    val measuredHeight = allowOutgoingCallsSpinner.height
                    val layoutParams = goldNumberSpinner.layoutParams
                    layoutParams.height = measuredHeight
                    goldNumberSpinner.layoutParams = layoutParams
                }
                //goldNumberSpinner.visibility = GONE

                val toastMsg =
                    getString(R.string.the_app_cannot_add_contacts_to_the_gold_number_list_since_contacts_permission)

                showLongSnackBar(context, toastMsg, anchorView = requireView())
            }

            val disabledColorString = "#4F4F4F"
            val disabledColor = Color.parseColor(disabledColorString)
            val newColor = if (goldNumberSpinner.isEnabled) ContextCompat.getColor(
                context,
                R.color.white
            ) else disabledColor
            changeSpinnerBackgroundColor(newColor, goldNumberSpinner)
            goldNumberSpinner.dropDownVerticalOffset = 155
        } catch (ex: Exception) {

        }

        goldNumberEnabledToggle.visibility = VISIBLE
        goldNumberSpinner.visibility = VISIBLE
    }

    private fun setWhenScreenUnlockedBehaviourSpinner(view: View) {
        val resourceWhenScreenUnlockedBehaviourMode =
            view.context.resources.getString(R.string.whenScreenUnlockedBehaviour)

        val whenScreenUnlockedBehaviourMode = loadWhenScreenUnlockedBehaviourEnum(view.context)
        selectedWhenScreenUnlockedBehaviourEnum =
            if (whenScreenUnlockedBehaviourMode != null) WhenScreenUnlockedBehaviourEasyAppEnum.valueOf(
                whenScreenUnlockedBehaviourMode
            ) else
                (if (resourceWhenScreenUnlockedBehaviourMode.isNotEmpty()) WhenScreenUnlockedBehaviourEasyAppEnum.valueOf(
                    resourceWhenScreenUnlockedBehaviourMode
                ) else WhenScreenUnlockedBehaviourEasyAppEnum.EASY_CALL_AND_ANSWER_APP)

        val whenScreenUnlockedBehaviourSpinner =
            view.findViewById<Spinner>(R.id.when_screen_is_unlocked_behaviour_spinner)

        val whenScreenUnlockedBehaviourDataList = mutableListOf(
            WhenScreenUnlockedBehaviourEasyAppEnum.EASY_CALL_AND_ANSWER_APP,
            WhenScreenUnlockedBehaviourEasyAppEnum.EXTERNAL_APP,
            WhenScreenUnlockedBehaviourEasyAppEnum.HOME_SCREEN
        )

        /*        val whenScreenUnlockedBehaviourDataList: MutableList<WhenScreenUnlockedBehaviourEasyAppEnum> =
                    WhenScreenUnlockedBehaviourEasyAppEnum.entries.toMutableList()*/
        val whenScreenUnlockedBehaviourEnumAdapter = DescriptiveEnumAdapter(
            requireContext(),
            whenScreenUnlockedBehaviourDataList
        )

        whenScreenUnlockedBehaviourEnumAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        whenScreenUnlockedBehaviourSpinner.adapter = whenScreenUnlockedBehaviourEnumAdapter
        whenScreenUnlockedBehaviourSpinner.dropDownVerticalOffset = 25

        setSelectedEnumInSpinner(
            whenScreenUnlockedBehaviourSpinner,
            selectedWhenScreenUnlockedBehaviourEnum,
            whenScreenUnlockedBehaviourDataList
        )

        val fragmentView = view

        whenScreenUnlockedBehaviourSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Directly cast to AllowAnswerCallsEnum
                    selectedWhenScreenUnlockedBehaviourEnum =
                        parent.getItemAtPosition(position) as WhenScreenUnlockedBehaviourEasyAppEnum
                    handleWhenScreenUnlocked(selectedWhenScreenUnlockedBehaviourEnum, fragmentView)

                    saveWhenScreenUnlockedBehaviourEnum(
                        requireContext(),
                        selectedWhenScreenUnlockedBehaviourEnum.name
                    )

                    // loadForm(requireView())
                    //handleMissingPermissions()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optionally handle this case
                }
            }
    }

    fun findAppIndexByPackage(appList: List<CustomAppInfo>, targetPackage: String): Int {
        return appList.indexOfFirst { it.packageName == targetPackage }
    }

    private fun setAppToLaunch(currView: View, context: Context) {
        // Select App to Launch
        val selectAppToLaunchSpinner =
            currView.findViewById<Spinner>(R.id.select_app_to_launch_spinner)

        appsToLaunch = loadAppsToLaunch(context).sortedBy { it.appName.lowercase() }
        if (appsToLaunch != null) {
            setupSpinnerWithIcons(selectAppToLaunchSpinner, appsToLaunch!!)
        }
        val selectedPackageName = loadSelectedAppInfo(context)

        try {
            if (selectedPackageName != null && appsToLaunch != null) {
                val index = findAppIndexByPackage(appsToLaunch!!, selectedPackageName)
                if (index > -1) { // found the item in the list
                    selectAppToLaunchSpinner.setSelection(index)
                } else {
                    selectAppToLaunchSpinner.setSelection(0)
                }
            } else if (appsToLaunch != null) {
                selectAppToLaunchSpinner.setSelection(0)
            }

        } catch (ex: Exception) {
            null // אם האפליקציה הוסרה או לא קיימת

            Log.e(
                "Unlock By Click - SettingsDashboardFragment",
                "Unable to find package $selectedPackageName"
            )
        }
        selectAppToLaunchSpinner.dropDownVerticalOffset = 135
        selectAppToLaunchSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Directly cast to AllowAnswerCallsEnum
                    val selectedAppInfo = parent.getItemAtPosition(position) as CustomAppInfo
                    saveSelectedAppPackage(requireContext(), selectedAppInfo.packageName)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Optionally handle this case
                }
            }
    }

    private fun loadAppsToLaunch(context: Context): List<CustomAppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val apps = pm.queryIntentActivities(intent, 0)
        val currentPackage = context.packageName

        val appList = apps.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName

            if (packageName == currentPackage) return@mapNotNull null // don't add the app here

            val appName = resolveInfo.loadLabel(pm).toString()
            val icon = resolveInfo.loadIcon(pm)

            CustomAppInfo(appName, packageName, icon)
        }

        return appList
    }


    private fun <T> setSelectedEnumInSpinner(
        spinner: Spinner,
        selectedEnumValue: T,
        dataList: MutableList<T>
    ) where T : Enum<T>, T : DescriptiveEnum {
        val index = dataList.indexOf(selectedEnumValue)
        if (index != -1) { // found the item in the list
            spinner.setSelection(index)
        } else {
            try {  // Handle the case where the enum is not found
                //toggleSpinner.setSelection(0)
                spinner.isEnabled = false // switchToggle.isChecked
            } catch (e: Exception) {
                Log.e(
                    "SimplyCall - SettingsFragment",
                    "setSelectedEnumInSpinner Error (${e.message})"
                )
            }
        }
    }

    private fun handleWhenScreenUnlocked(
        selectedBehaviourMode: WhenScreenUnlockedBehaviourEasyAppEnum,
        view: View
    ) {
        // External App Only:
        val selectAppToLaunchField =
            view.findViewById<LinearLayout>(R.id.select_app_to_launch_container)

        if (selectAppToLaunchField != null) {
            val selectAppToLaunchFieldShouldBeVisible =
                selectedBehaviourMode == WhenScreenUnlockedBehaviourEasyAppEnum.EXTERNAL_APP
            selectAppToLaunchField.visibility =
                if (selectAppToLaunchFieldShouldBeVisible) VISIBLE else GONE
        }
    }

    private fun initQuickCallButton(view: View) {
        val context = requireContext()
        val distressButtonSpinner: Spinner =
            view.findViewById(R.id.distress_button_number_contacts_spinner)
        distressButtonNumberToggle = view.findViewById(R.id.distress_button_number_enabled_toggle)
        /*  val telephonyManager =
              context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "US"
            val aidNumbers = getAidNumbers(context, countryCode)

            var aidNumbersList = aidNumbers.map { number ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${number.number}"
                } else {
                    ""
                }
            }.distinct()

            if (aidNumbersList.isEmpty()) { // add static emergency phone numbers by country
                val listOfStaticEmergencyNumbersPerRegion =
                    countryCode.let { emergencyNumbersByRegion[it] }
                if (listOfStaticEmergencyNumbersPerRegion != null && listOfStaticEmergencyNumbersPerRegion.isNotEmpty()) {
                    aidNumbersList = listOfStaticEmergencyNumbersPerRegion
                }
            }*/

// מצא את ה-Spinner (ב-XML שלך יש לו מזהה מתאים)
        //  val spinner: Spinner = findViewById(R.id.spinnerEmergencyNumbers)
        coroutineScope.launch {
            try {
                loadContactsIntoEmergencySpinnerAsync(
                    spinner = distressButtonSpinner,
                    emergencyNumbersList = emptyList(), // not emergency. not aidNumbersList,
                    context = requireContext(), // seems better to require the context here for this
                    anchorView = requireView()
                )
                distressButtonNumberToggle.isChecked =
                    distressButtonSpinner.selectedItemPosition > 0
                distressButtonSpinner.isEnabled = distressButtonSpinner.selectedItemPosition > 0
                handleSpinnerEnabledDisabled(distressButtonSpinner)

                val adapter = distressButtonSpinner.adapter
                if (adapter == null || adapter.count <= 1) {
                    distressButtonNumberToggle.isEnabled = false
                } else {
                    distressButtonNumberToggle.isEnabled = true
                }

                distressButtonNumberToggle.setOnCheckedChangeListener { buttonView, isChecked ->
                    try {
                        if (isChecked) {
                            /*  saveEmergencyNumberContact(null, context)
                              saveEmergencyNumber(null, context)*/
                        } else {
                            distressButtonSpinner.setSelection(0)
                            saveQuickCallNumberContact(null, context)
                            saveQuickCallNumber(null, context)
                        }
                        distressButtonSpinner.isEnabled = isChecked
                        handleSpinnerEnabledDisabled(distressButtonSpinner)
                    } catch (e: Exception) {

                    }
                }

                // Observe Spinner click:
                distressButtonSpinnerClickEvent.observe(viewLifecycleOwner) { isEvent ->
                    if (isEvent) {
                        distressButtonNumberToggle.isChecked =
                            distressButtonSpinner.selectedItemPosition > 0
                        distressButtonSpinner.isEnabled =
                            distressButtonSpinner.selectedItemPosition > 0
                        handleSpinnerEnabledDisabled(distressButtonSpinner)
                        distressButtonSpinnerClickEvent.value = false
                    }
                }
                distressButtonSpinnerClickEvent.value = false
            } catch (ex: Exception) {

            }

            distressButtonNumberToggle.visibility = VISIBLE
            distressButtonSpinner.visibility = VISIBLE
            // After the function completes, show a Toast
            //  Toast.makeText(requireContext(), "Operation completed", Toast.LENGTH_SHORT).show()
        }

// צור ArrayAdapter והגדר אותו לספינר
        //val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, emergencyNumbersList)
        // adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //loadContactsIntoEmergencySpinnerAsync(distressButtonSpinner, emergencyNumbersList, requireContext(), requireView())
        // emergencyNumbersListSpinner.adapter = adapter

    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    fun isValidEmergencyNumberForRegion(number: String, regionCode: String): Boolean {
        // המרה לאותיות גדולות עבור קוד המדינה
        val validNumbers = emergencyNumbersByRegion[regionCode.uppercase()] ?: emptyList()
        return validNumbers.contains(number)
    }

    private fun getAidNumbers(context: Context, regionCode: String): List<EmergencyNumber> {
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

            // קבלת רשימת מספרי עזר מהמערכת
            val aidNumbersMap =
                telephonyManager.getEmergencyNumberList(EmergencyNumber.EMERGENCY_SERVICE_CATEGORY_UNSPECIFIED)
            val allAidNumbers = aidNumbersMap.values.flatten()

            // מסננים את הרשימה כך שיוחזרו רק מספרי חירום שמזוהים כמקומיים
            return allAidNumbers.filter { aidNumber ->
                try {
                    isValidEmergencyNumberForRegion(aidNumber.number, regionCode)
                    // val numberProto = phoneUtil.parse(emergencyNumber.number, regionCode)
                    //   val actualRegion = phoneUtil.getRegionCodeForNumber(numberProto)
                    // אם האזור בפועל תואם לאזור הנתון, המספר נחשב למקומי
                    //  actualRegion.equals(regionCode, ignoreCase = true)
                } catch (e: Exception) {
                    true
                }
            }
        } catch (e: Exception) {
            Log.e("SimplyCall - SettingsFragment", "getAidNumbers Error (${e.message})")
            return emptyList()
        }
        // בדיקת הרשאת קריאת מצב הטלפון

    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun handleGoldNumberSelectContact(context: Context, selectedContactName: Any) {
        /*        val selectedGoldPhoneNumber = if (PermissionsStatus.defaultDialerPermissionGranted.value == true)
                    getPhoneNumberFromContactNameAndFilterBlocked(context, selectedContactName.toString()) else
                getPhoneNumberFromContactName(context, selectedContactName.toString())*/

        // We should not have blocked number and we could create an error is we'll return unknown here
        val selectedGoldNumberContact = selectedContactName.toString()
        val selectedGoldPhoneNumber =
            getPhoneNumberFromContactName(context, selectedGoldNumberContact)

        saveGoldNumberContact(selectedGoldNumberContact, context)
        saveGoldNumber(selectedGoldPhoneNumber, context)
        handleSwitchToggle(
            goldNumberSpinner, selectedContactName.toString(),
            goldNumberEnabledToggle, contactsList
        )
    }

    private fun changeSpinnerBackgroundColor(color: Int, spinner: Spinner) {
        // Access the Spinner's background drawable
        val backgroundDrawable = spinner.background

        if (backgroundDrawable is GradientDrawable) {
            // Mutate to avoid affecting other views
            backgroundDrawable.mutate()
            // Set the new color while preserving shape properties
            backgroundDrawable.setColor(color)
        } else {
            // Optionally, handle other drawable types or set a new GradientDrawable
            // For example:
            /*
            val newDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = resources.getDimension(R.dimen.corner_radius)
                setColor(color)
                setStroke(2, ContextCompat.getColor(this@MainActivity, R.color.stroke_color))
            }
            spinner.background = newDrawable
            */
        }
    }

    private fun <T> handleSwitchToggleByEnum(
        toggleSpinner: Spinner,
        selectedEnumValue: T,
        switchToggle: SwitchMaterial,
        dataList: MutableList<T>
    ) where T : Enum<T>, T : DescriptiveEnum {
        toggleSpinner.isEnabled = false
        val index = dataList.indexOf(selectedEnumValue)
        if (index != -1) { // found the item in the list
            toggleSpinner.setSelection(index)
            toggleSpinner.isEnabled = switchToggle.isChecked
        } else {
            try {  // Handle the case where the enum is not found
                //toggleSpinner.setSelection(0)
                toggleSpinner.isEnabled = false // switchToggle.isChecked
            } catch (e: Exception) {
                Log.e("SimplyCall - Settings", "handleSwitchToggle Error (${e.message})")
            }
        }

        handleSpinnerEnabledDisabled(toggleSpinner)
    }

    private fun handleSpinnerEnabledDisabled(toggleSpinner: Spinner) {
        // Change spinner background color based on enabled state
        val disabledColorString = "#4F4F4F"
        val disabledColor = Color.parseColor(disabledColorString)
        val newColor = if (toggleSpinner.isEnabled) ContextCompat.getColor(
            requireContext(),
            R.color.white
        ) else disabledColor

        changeSpinnerBackgroundColor(newColor, toggleSpinner)
    }

    override fun onResume() {
        super.onResume()

        checkForPermissionsChangesAndShowToastIfChanged(requireContext(), requireActivity())

        if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
            OpenScreensStatus.shouldClosePermissionsScreens.value =
                OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
        }

        try {
            loadView(fragmentRoot)
        } catch (e: Exception) {
            Log.e("SimplyCall - Settings", "loadView Error (${e.message}")
        }
    }

    private fun handleSwitchToggle(
        toggleSpinner: Spinner,
        selectedEnumValueDescription: String,
        switchToggle: SwitchMaterial,
        dataList: MutableList<String>,
        selectedIndex: Int? = null
    ) {

        val selectedDescription = selectedEnumValueDescription // e.g. "Contacts Only"
        toggleSpinner.isEnabled = false
        val index =
            if (selectedIndex != null) selectedIndex else dataList.indexOf(selectedDescription)
        if (index != -1) { // found the item in the list
            toggleSpinner.setSelection(index)
            toggleSpinner.isEnabled = switchToggle.isChecked
        } else {
            try {  // Handle the case where the description is not found
                toggleSpinner.setSelection(0)
                toggleSpinner.isEnabled = switchToggle.isChecked
            } catch (e: Exception) {
                Log.e("SimplyCall - Settings", "handleSwitchToggle Error (${e.message}")
            }
        }
        //#4F4F4F
        val disabledColorString = "#4F4F4F"
        val disabledColor = Color.parseColor(disabledColorString)
        val newColor = if (toggleSpinner.isEnabled) ContextCompat.getColor(
            requireContext(),
            R.color.white
        ) else disabledColor

        changeSpinnerBackgroundColor(newColor, toggleSpinner)

    }

    private fun loadContactsIntoSpinnerAsync(spinner: Spinner) {
        val context = requireContext()
        val goldPhoneNumber = loadGoldNumber(context)
        goldNumberEnabledToggle.isChecked =
            (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != context.getString(
                R.string.unknown_capital
            ))
        coroutineScope.launch {
            contactsList = withContext(Dispatchers.IO) {
                // Load contacts in a background thread
                if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                    fetchContactsOptimized(context).toMutableList()
                } else {
                    fetchContacts(context).toMutableList()
                }
            }

            // Add an empty string at the top (index 0)
            contactsList.add(0, "") // empty item at the beginning of the list = no selection
            // Update the Spinner on the UI thread
            /*  contactsSpinnerAdapter =
                  ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, contactList)*/
            //           // contactsSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            contactsSpinnerAdapter = object : ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_item,
                contactsList
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
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
                    return view
                }
            }
            spinner.adapter = contactsSpinnerAdapter

            val contactListIsNotEmpty = contactsList.isNotEmpty()
            spinner.isEnabled = contactListIsNotEmpty // Enable the spinner after loading
            // Set default selection to the first contact if needed
            if (contactListIsNotEmpty) {
                spinner.setSelection(0)
            }

            goldNumberEnabledToggle.isChecked =
                (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != context.getString(
                    R.string.unknown_capital
                ))
            if (!goldPhoneNumber.isNullOrEmpty() && goldPhoneNumber != "Unknown" && goldPhoneNumber != context.getString(
                    R.string.unknown_capital
                )
            ) {
                val goldNumberContactName = getContactNameFromPhoneNumber(
                    context,
                    goldPhoneNumber
                ) // we don't check for blocked numbers here

                val spinnerPosition = contactsSpinnerAdapter.getPosition(goldNumberContactName)

                if (spinnerPosition >= 0) {
                    goldNumberSpinner.setSelection(spinnerPosition)
                } else {
                    // Handle the case where the string does not match any Spinner item
                    // For example, set to a default position or show a message
                    goldNumberSpinner.setSelection(0) // Setting to the first item
                    //Toast.makeText(this, "Item not found. Default selected.", Toast.LENGTH_SHORT).show()
                }
                handleSwitchToggle(
                    goldNumberSpinner, goldNumberContactName,
                    goldNumberEnabledToggle, contactsList
                )
            } else { // disable Spinner
                handleSwitchToggle(
                    goldNumberSpinner, "",
                    goldNumberEnabledToggle, contactsList
                )
            }

            goldNumberEnabledToggle.setOnCheckedChangeListener { buttonView, isChecked ->
                if (!isChecked) {
                    handleSwitchToggle(goldNumberSpinner, "", goldNumberEnabledToggle, contactsList)
                    saveGoldNumber(null, context)
                    saveGoldNumberContact(null, context)
                } else { // user chose to receive calls - change dropdown to initial value
                    try {
                        val firstContact = goldNumberSpinner.getItemAtPosition(0)
                        handleGoldNumberSelectContact(context, firstContact)
                    } catch (e: Exception) {

                    }
                }
            }

            goldNumberSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
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
                    } else { // user chose empty item - no gold number
                        goldNumberEnabledToggle.isChecked = false
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    TODO("Not yet implemented")
                }
            }
//            goldNumberSpinner.visibility = VISIBLE

            // Handle item selection
            /*            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(
                                parent: AdapterView<*>,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                val selectedContact = parent.getItemAtPosition(position).toString()
                                // Select the given contact, if specified
                                selectContactInSpinner(spinner, selectedContact)
            *//*                    spinner.invalidate()
                    spinner.requestLayout()*//*

                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Optional: handle when no item is selected
                }
            }*/

        }
    }


    private fun selectContactInSpinner(spinner: Spinner, contactName: String) {
        val adapter = spinner.adapter
        if (adapter != null) {
            for (i in 0 until adapter.count) {
                if (adapter.getItem(i) == contactName) {
                    spinner.setSelection(i) // Select the matching contact
                    Log.d("SimplyCall - Settings", "Spinner - select ${spinner.selectedItem}")
                    contactsSpinnerAdapter = adapter as ArrayAdapter<String>
                    contactsSpinnerAdapter.notifyDataSetChanged()

                    spinner.invalidate()
                    spinner.requestLayout()
                    return
                }
            }
        }
    }

    private fun checkScroll(context: Context) {
        if (scrollView.canScrollVertically(1)) {
            if (scrollArrow.animation == null) {
                val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
                scrollArrow.startAnimation(blinkAnimation)
            }
            settingsScrollArrowContainer.visibility = View.VISIBLE
        } else {
            scrollArrow.clearAnimation()
            settingsScrollArrowContainer.visibility = View.GONE
        }
    }

    private fun startUnlockService() {
        /* parentFragmentManager.beginTransaction()
             .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
             .replace(R.id.main_fragment, SeniorDashboardFragment())
             .commitNow()
 */
        val context = requireContext()

        if (PermissionsStatus.permissionToShowWhenDeviceLockedAllowed.value == true &&
            PermissionsStatus.canDrawOverlaysPermissionGranted.value == true &&
            PermissionsStatus.backgroundWindowsAllowed.value == true) {

            serviceIsRunning = true
            // startServiceButton.text = getString(R.string.stop_service)
            // startServiceButton.background.setTint(Color.parseColor("#E91E63"))
            // startServiceButton.setTextColor(Color.parseColor("#FFFFFF"))


            saveShouldServiceRun(true, context)

            val intent = Intent(context, IdleMonitorService::class.java)
            context.startService(intent)

            Log.d("SimplyCall - Settings", "Unlock service started!")

            val toastMsg = getString(R.string.lock_service_was_activated_msg)
            showLongSnackBar(context, toastMsg, 10000, anchorView = requireView())
        }
        else {
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.permission_denied_capital))
                .setMessage(context.getString(R.string.permission_could_not_be_granted_automatically))
                .setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ ->
                    openAppSettings(context)
                }
                .setNegativeButton(context.getString(R.string.ok)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        }

    }

    private fun stopUnlockServiceClick() {
        val context = requireContext()

        //startServiceButton.background.setTint(Color.parseColor("#4CAF50")) // green
        // startServiceButton.text = context.getString(R.string.activate_service)

        stopUnlockService()
    }

    private fun stopUnlockService() {
        val context = requireContext()

        //saveShouldServiceRun(false, context)

        serviceIsRunning = false
        val intent = Intent(context, IdleMonitorService::class.java)
        context.stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            scrollView.setOnScrollChangeListener(null)
            OpenScreensStatus.isSettingsScreenOpened = false
            coroutineScope.cancel() // Clean up the coroutine
        } catch (e: Exception) {

        }
    }
}
