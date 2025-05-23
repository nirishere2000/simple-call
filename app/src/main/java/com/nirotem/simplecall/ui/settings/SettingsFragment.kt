package com.nirotem.simplecall.ui.settings

import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.telephony.emergency.EmergencyNumber
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.nirotem.simplecall.InCallServiceManager
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.WaitingCall
import com.nirotem.simplecall.adapters.DescriptiveEnum
import com.nirotem.simplecall.adapters.DescriptiveEnumAdapter
import com.nirotem.simplecall.helpers.DBHelper.fetchContacts
import com.nirotem.simplecall.helpers.DBHelper.fetchContactsOptimized
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getPhoneNumberFromContactName
import com.nirotem.simplecall.helpers.DBHelper.getPhoneNumberFromContactNameAndFilterBlocked
import com.nirotem.simplecall.helpers.DBHelper.isNumberInContacts
import com.nirotem.simplecall.helpers.DBHelper.isNumberInFavorites
import com.nirotem.simplecall.helpers.GeneralUtils.distressButtonSpinnerClickEvent
import com.nirotem.simplecall.helpers.GeneralUtils.emergencyNumbersByRegion
import com.nirotem.simplecall.helpers.GeneralUtils.loadContactsIntoEmergencySpinnerAsync
import com.nirotem.simplecall.helpers.SharedPreferencesCache.isGlobalAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadEmergencyNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowCallWaiting
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowMakingCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAllowOpeningWhatsApp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveEmergencyNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveEmergencyNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveGoldNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveIsGlobalAutoAnswer
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveShouldCallsStartWithSpeakerOn
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveShouldShowKeypadInActiveCall
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowCallWaiting
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldAllowOpeningWhatsApp
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldCallsStartWithSpeakerOn
import com.nirotem.simplecall.helpers.SharedPreferencesCache.shouldShowKeypadInActiveCall
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.AllowAnswerCallsEnum
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.OpenScreensStatus.shouldUpdateSettingsScreens
import com.nirotem.simplecall.statuses.PermissionsStatus.checkForPermissionsChangesAndShowToastIfChanged
import com.nirotem.simplecall.statuses.SettingsStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.toString

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
                val tabCalls = view.findViewById<LinearLayout>(R.id.tabCalls)
              //  val tabCallsText = view.findViewById<TextView>(R.id.tabCallsText)
                val tabDistressButton = view.findViewById<LinearLayout>(R.id.tabDistressButton)
                val groupDistressButton = view.findViewById<LinearLayout>(R.id.groupDistressButton)
                val groupCalls = view.findViewById<LinearLayout>(R.id.groupCalls)
                val tabLock = view.findViewById<LinearLayout>(R.id.tabLock)
                val groupLock = view.findViewById<LinearLayout>(R.id.groupLock)
                val tabReports = view.findViewById<LinearLayout>(R.id.tabReports)
                val groupReports = view.findViewById<LinearLayout>(R.id.groupReports)
                val tabOthers = view.findViewById<LinearLayout>(R.id.tabOthers)
                val groupOthers = view.findViewById<LinearLayout>(R.id.groupOthers)



                groupCalls.visibility = VISIBLE
                groupDistressButton.visibility = GONE

                groupLock.visibility = GONE
                groupReports.visibility = GONE
                groupOthers.visibility = GONE

                tabCalls.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = VISIBLE
                    groupLock.visibility = GONE
                    groupReports.visibility = GONE
                    groupOthers.visibility = GONE
                }

                tabDistressButton.setOnClickListener {
                    groupDistressButton.visibility = VISIBLE
                    groupCalls.visibility = GONE
                    groupLock.visibility = GONE
                    groupReports.visibility = GONE
                    groupOthers.visibility = GONE
                }

                tabLock.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = GONE
                    groupReports.visibility = GONE
                    groupLock.visibility = VISIBLE
                    groupOthers.visibility = GONE
                }

                tabReports.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = GONE
                    groupLock.visibility = GONE
                    groupReports.visibility = VISIBLE
                    groupOthers.visibility = GONE
                }

                tabOthers.setOnClickListener {
                    groupDistressButton.visibility = GONE
                    groupCalls.visibility = GONE
                    groupLock.visibility = GONE
                    groupReports.visibility = GONE
                    groupOthers.visibility = VISIBLE
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
            val emergencyPhoneNumber = loadEmergencyNumber(context)
            val existsDistressNumberForDistressButtonButWithoutPermission = (emergencyPhoneNumber != null) && (PermissionsStatus.callPhonePermissionGranted.value != true)
            if (existsDistressNumberForDistressButtonButWithoutPermission) {
                var toastMsg =
                    getString(R.string.phone_permission_required_for_distress_button)
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
        val view = if (SettingsStatus.isPremium) inflater.inflate(R.layout.fragment_premium_settings, container, false) else inflater.inflate(R.layout.fragment_settings, container, false)
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
            OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
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
        try {
            //adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            goldNumberEnabledToggle.isEnabled = PermissionsStatus.readContactsPermissionGranted.value == true
            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                loadContactsIntoSpinnerAsync(goldNumberSpinner)
            } else {
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

                showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
            }

            val disabledColorString = "#4F4F4F"
            val disabledColor = Color.parseColor(disabledColorString)
            val newColor = if (goldNumberSpinner.isEnabled) ContextCompat.getColor(
                requireContext(),
                R.color.white
            ) else disabledColor
            changeSpinnerBackgroundColor(newColor, goldNumberSpinner)
            goldNumberSpinner.dropDownVerticalOffset = 155
        } catch (ex: Exception) {

        }

        goldNumberEnabledToggle.visibility = VISIBLE
        goldNumberSpinner.visibility = VISIBLE

        initDistressButton(view)

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
        val intervalSpinner: Spinner = view.findViewById(R.id.report_interval_spinner)
        val intervals = arrayOf("1", "3", "7", "14", "30")
        val intervalAdapter = ArrayAdapter(
            currContext,
            android.R.layout.simple_spinner_item,
            intervals
        )
        intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        intervalSpinner.adapter = intervalAdapter

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

    private fun initDistressButton(view: View) {
        val context = requireContext()
        val distressButtonSpinner: Spinner =
            view.findViewById(R.id.distress_button_number_contacts_spinner)
        distressButtonNumberToggle = view.findViewById(R.id.distress_button_number_enabled_toggle)
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryCode = telephonyManager.simCountryIso?.uppercase() ?: "US"
        val emergencyNumbers = getEmergencyNumbers(context, countryCode)

        // val emergencyNumbersList = getEmergencyNumbersAsStrings(telephonyManager, countryCode)

        /*        if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                    var toastMsg =
                        getString(R.string.phone_permission_required_for_distress_button)
                    //Snackbar.make(fragmentView, toastMsg, 8000).show()
                    showLongSnackBar(context, toastMsg, anchorView = requireView())
                }*/

// נניח שכל איבר ב-emergencyNumbers הוא אובייקט שמכיל את number ואת emergencyServiceCategories
// ניצור רשימת מחרוזות להצגה, למשל "מספר - קטגוריה"
        var emergencyNumbersList = emergencyNumbers.map { number ->
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
            val listOfStaticEmergencyNumbersPerRegion =
                countryCode.let { emergencyNumbersByRegion[it] }
            if (listOfStaticEmergencyNumbersPerRegion != null && listOfStaticEmergencyNumbersPerRegion.isNotEmpty()) {
                emergencyNumbersList = listOfStaticEmergencyNumbersPerRegion
            }
        }

// מצא את ה-Spinner (ב-XML שלך יש לו מזהה מתאים)
        //  val spinner: Spinner = findViewById(R.id.spinnerEmergencyNumbers)
        coroutineScope.launch {
            try {
                loadContactsIntoEmergencySpinnerAsync(
                    spinner = distressButtonSpinner,
                    emergencyNumbersList = emergencyNumbersList,
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
                            saveEmergencyNumberContact(null, context)
                            saveEmergencyNumber(null, context)
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

    fun isValidEmergencyNumberForRegion(number: String, regionCode: String): Boolean {
        // המרה לאותיות גדולות עבור קוד המדינה
        val validNumbers = emergencyNumbersByRegion[regionCode.uppercase()] ?: emptyList()
        return validNumbers.contains(number)
    }

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
        } catch (e: Exception) {
            Log.e("SimplyCall - TourFragment", "getEmergencyNumbers Error (${e.message})")
            return emptyList()
        }
        // בדיקת הרשאת קריאת מצב הטלפון

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

    override fun onDestroy() {
        super.onDestroy()
        try {
            scrollView.setOnScrollChangeListener(null)
            OpenScreensStatus.isSettingsScreenOpened = false
            coroutineScope.cancel() // Clean up the coroutine
        }
        catch (e: Exception) {

        }
    }
}
