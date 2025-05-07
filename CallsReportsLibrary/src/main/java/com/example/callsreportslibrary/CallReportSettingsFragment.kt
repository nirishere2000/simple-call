package com.example.callsreportslibrary


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class CallReportSettingsFragment : DialogFragment() {
    private val coroutineScope =
        CoroutineScope(Dispatchers.Main + Job()) // Main thread for UI updates
    private lateinit var contactsSpinnerAdapter: ArrayAdapter<String>
    private lateinit var contactsList: MutableList<String>
    private lateinit var contactsSpinner: Spinner
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
         /*   scrollView = view.findViewById(R.id.scrollable_settings_options)
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

            loadView(view)*/

            val context = requireContext()

        } catch (e: Exception) {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
      //  val view = inflater.inflate(R.layout.fragment_calls_report, container, false)
       // fragmentRoot = view
        return null
    }

    private fun loadView(view: View): View? {
    //    val currContext = view.context
     //   var shouldAlwaysSendReportToGoldNumberToggle: SwitchMaterial =
       //     view.findViewById(R.id.should_always_send_report_to_gold_number_toggle)
     //   var sendOnlyExceptionalDataToggle: SwitchMaterial =
     //       view.findViewById(R.id.send_only_exceptional_data_toggle)
        // Setup gold number spinner
   //     contactsSpinner = view.findViewById(R.id.contacts_spinner)

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



// Initialize the adapter with AllowAnswerCallsEnum

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
      //  val shouldSendReportToggle: SwitchMaterial = view.findViewById(R.id.should_send_report_toggle)


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


        /*            handleSwitchToggle(
                        allowReceiveCallsSpinner, currContext.getString(selectedEnum.descriptionRes),
                        allowReceiveCallsEnabledToggle, receiveCallsDataList, selectedIndex
                    )*/

        /*        handleSwitchToggle(
                    allowReceiveCallsSpinner, currContext.getString(selectedEnum.descriptionRes),
                    allowReceiveCallsEnabledToggle, receiveCallsDataList, null
                )*/

        // Outgoing Calls:


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




        // Report interval spinner
        /* val intervalSpinner: Spinner = view.findViewById(R.id.report_interval_spinner)
         val intervals = arrayOf("1", "3", "7", "14", "30")
         val intervalAdapter = ArrayAdapter(
             currContext,
             android.R.layout.simple_spinner_item,
             intervals
         )
         intervalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
         intervalSpinner.adapter = intervalAdapter*/


        return view
    }

    private fun checkScroll(context: Context) {
        if (scrollView.canScrollVertically(1)) {
            if (scrollArrow.animation == null) {
                //val blinkAnimation = AnimationUtils.loadAnimation(context, R.anim.blink)
                //scrollArrow.startAnimation(blinkAnimation)
            }
            settingsScrollArrowContainer.visibility = VISIBLE
        } else {
            scrollArrow.clearAnimation()
            settingsScrollArrowContainer.visibility = GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            scrollView.setOnScrollChangeListener(null)

            coroutineScope.cancel() // Clean up the coroutine
        }
        catch (e: Exception) {

        }
    }
}
