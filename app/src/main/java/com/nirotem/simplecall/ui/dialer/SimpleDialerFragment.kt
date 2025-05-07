package com.nirotem.simplecall.ui.dialer

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_CONTACTS
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.TelecomManager
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumberAndReturnNullIfNotFound
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowAnswerCallsEnum
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAllowMakingCallsEnum
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.SoundPoolManager
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.permissionsScreen.PermissionsAlertFragment
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SimpleDialerFragment : Fragment(R.layout.fragment_simple_dialer) {
    private var textEnteredNumber: TextView? = null
    private var makingMakingCallPermission = false
    private val buttonPressedImaged = R.drawable.digitkeybuttonpressedtransparent
    private val originalButtonImage = R.drawable.digitkeybuttontransparent
    private val keypadPressedImaged = R.drawable.digitkeyclickedbluefulltransparent
    private val originalkeypadImage = R.drawable.digitkeytransparent
    private var callIsWaitingForPermission: String? = null
    private var callCallback: Call.Callback? = null
    private var contactNameTextView: TextView? = null
    private var contactNameContainer: LinearLayout? = null
    private val languageEnum = LanguagesEnum.fromCode(Locale.getDefault().language)
    private var unformattedPhoneNum = ""

    // Declare the permission request launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
/*                if (askingReadContactsPermission) { // Read Contacts permission was granted
                    askingReadContactsPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
                    loadFragment(fragmentRoot)
                    Toast.makeText(
                        this.requireContext(),
                        "Permission was granted! Reloading..",
                        Toast.LENGTH_LONG
                    ).show()
                }*/
            }
            else {
                suggestManualPermissionGrant(context)
            }

            /*   else {
                   val toastMsg = "Cannot continue since permission was not approved"
                   Toast.makeText(
                       this.requireContext(),
                       toastMsg,
                       Toast.LENGTH_LONG
                   ).show()
               }*/
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
            OpenScreensStatus.shouldCloseSettingsScreens.value = OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
        }
        if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
            OpenScreensStatus.shouldClosePermissionsScreens.value = OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
        }
        if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
            OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
        }
        val currContext = requireContext()
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            // Hide the FAB in this fragment
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            fab?.visibility = View.GONE
        }

        val keypadLayout = view.findViewById<LinearLayout>(R.id.numbersKeypadBack)

        val key0 = keypadLayout.findViewById<ConstraintLayout>(R.id.key0Button)
      //  val key0ButtonImage = view.findViewById<ImageView>(R.id.key0ButtonImage)
        val key1 = keypadLayout.findViewById<ConstraintLayout>(R.id.key1Button)
    //    val key1ButtonImage = view.findViewById<ImageView>(R.id.key1ButtonImage)
        val key2 = keypadLayout.findViewById<ConstraintLayout>(R.id.key2Button)
     //   val key2ButtonImage = view.findViewById<ImageView>(R.id.key2ButtonImage)
        val key3 = keypadLayout.findViewById<ConstraintLayout>(R.id.key3Button)
     //   val key3ButtonImage = view.findViewById<ImageView>(R.id.key3ButtonImage)
        val key4 = keypadLayout.findViewById<ConstraintLayout>(R.id.key4Button)
      //  val key4ButtonImage = view.findViewById<ImageView>(R.id.key4ButtonImage)
        val key5 = keypadLayout.findViewById<ConstraintLayout>(R.id.key5Button)
    //    val key5ButtonImage = view.findViewById<ImageView>(R.id.key5ButtonImage)
        val key6 = keypadLayout.findViewById<ConstraintLayout>(R.id.key6Button)
      //  val key6ButtonImage = view.findViewById<ImageView>(R.id.key6ButtonImage)
        val key7 = keypadLayout.findViewById<ConstraintLayout>(R.id.key7Button)
     //   val key7ButtonImage = view.findViewById<ImageView>(R.id.key7ButtonImage)
        val key8 = keypadLayout.findViewById<ConstraintLayout>(R.id.key8Button)
      //  val key8ButtonImage = view.findViewById<ImageView>(R.id.key8ButtonImage)
        val key9 = keypadLayout.findViewById<ConstraintLayout>(R.id.key9Button)
     //   val key9ButtonImage = view.findViewById<ImageView>(R.id.key9ButtonImage)
    //    val addNewContactButton = view.findViewById<ImageView>(R.id.addNewContactButton)
        val poundKey = keypadLayout.findViewById<ConstraintLayout>(R.id.keyPoundButton)
    //    val keyPoundButtonImage = view.findViewById<ImageView>(R.id.keyPoundButtonImage)
        val asteriskKey = keypadLayout.findViewById<ConstraintLayout>(R.id.asteriskKeyButton)
     //   val asteriskKeyButtonImage = view.findViewById<ImageView>(R.id.asteriskKeyButtonImage)
        val backDeleteButtonImage = view.findViewById<ImageView>(R.id.deleteOneCharButton)
        //val backDeleteButtonImage = view.findViewById<ImageView>(R.id.backDeleteButtonImage)
        val dialerCallButton = view.findViewById<ImageView>(R.id.dialerCallButton)
        val keypadBack = view.findViewById<LinearLayout>(R.id.keypadBack)
        val textEnteredNumberBack = view.findViewById<LinearLayout>(R.id.textEnteredNumberBack)
        val controlButtonsBack = view.findViewById<LinearLayout>(R.id.controlButtonsBack)
       // val contactsRecyclerViewBack = view.findViewById<LinearLayout>(R.id.contactsRecyclerViewBack)
        val noOutGoingCallsCaptionBack = view.findViewById<LinearLayout>(R.id.noOutGoingCallsCaptionBack)
        val appWasConfiguredWithoutOutgoingCaption = view.findViewById<TextView>(R.id.appWasConfiguredWithoutOutgoingCaption)
        //val plusKeyPadButton = view.findViewById<ConstraintLayout>(R.id.plusKeyPadButton)
        //val plusKeyPadButtonImage = view.findViewById<ImageView>(R.id.plusKeyPadButtonImage)
        //val deleteAllButton = view.findViewById<ConstraintLayout>(R.id.deleteAllKeyPadButton)
        //val deleteAllKeyPadImage = view.findViewById<ImageView>(R.id.deleteAllKeyPadImage)
        val goldNumberButton = view.findViewById<ImageView>(R.id.goldNumberButton)
        val goldNumberButtonCircle = view.findViewById<ImageView>(R.id.goldNumberButtonCircle)
        val goldNumberBar = view.findViewById<LinearLayout>(R.id.goldNumberBar)
        val goldNumberBarGoldButtonBack = view.findViewById<FrameLayout>(R.id.goldNumberBarGoldButtonBack)
        val goldNumberBarContactName = view.findViewById<TextView>(R.id.goldNumberBarContactName)
        val goldenButtonBack = view.findViewById<FrameLayout>(R.id.goldenButtonBack)
        contactNameTextView = view.findViewById(R.id.textContactName)
        contactNameContainer = view.findViewById(R.id.contactNameContainer)

        textEnteredNumber = view.findViewById<TextView>(R.id.textEnteredNumber)
      //  backDeleteButtonImage.visibility = VISIBLE // to avoid א-סימטריה with left side // if (textEnteredNumber?.text.isNullOrEmpty()) GONE else VISIBLE

        // Gold Number
        // For now we go by Gold Number Contact.
        goldNumberBarContactName.text = if (SettingsStatus.goldNumberContact.value.isNullOrEmpty()) "" else SettingsStatus.goldNumberContact.value
        goldenButtonBack.visibility = if (SettingsStatus.goldNumberContact.value.isNullOrEmpty()) GONE else VISIBLE
       // goldNumberButtonCircle.visibility = if (SettingsStatus.goldNumberContact.value.isNullOrEmpty()) GONE else VISIBLE
        goldNumberBar.visibility = if (SettingsStatus.goldNumberContact.value.isNullOrEmpty()) GONE else VISIBLE
        SettingsStatus.goldNumberContact.observe(viewLifecycleOwner) { newGoldNumber ->
            goldNumberButton.visibility = if (SettingsStatus.goldNumberContact.value.isNullOrEmpty()) GONE else VISIBLE
            goldNumberButtonCircle.visibility = if (SettingsStatus.goldNumberContact.value.isNullOrEmpty()) GONE else VISIBLE
        }

        goldNumberButton.setOnClickListener {
            goldNumberClicked(currContext, view)
        }

        goldNumberButtonCircle.setOnClickListener {
            goldNumberClicked(currContext, view)
        }

        goldNumberBarGoldButtonBack.setOnClickListener {
            goldNumberClicked(currContext, view)
        }

        // Keypad
        backDeleteButtonImage.setOnClickListener {
            deleteLastChar()
        }

        key0.setOnClickListener {
            clickKey("0", view)
        }

        key1.setOnClickListener {
            clickKey("1", view)
        }

        key2.setOnClickListener {
            clickKey("2", view)
        }

        key3.setOnClickListener {
            clickKey("3", view)
        }

        key4.setOnClickListener {
            clickKey("4", view)

        }
        key5.setOnClickListener {
            clickKey("5", view)
        }

        key6.setOnClickListener {
            clickKey("6", view)
        }

        key7.setOnClickListener {
            clickKey("7", view)
        }

        key8.setOnClickListener {
            clickKey("8", view)
        }

        key9.setOnClickListener {
            clickKey("9", view)
        }

        asteriskKey.setOnClickListener {
            clickKey("*", view)
        }

        poundKey.setOnClickListener {
            clickKey("#", view)
        }

        dialerCallButton.setOnClickListener {
            // Handle button click
            //  val digit = button.text.toString()
            makePhoneCallFromKeyPadText()
        }

        /*       var dateDiff = calculateDaysDifference(view.context, "premium_dialer_speak_digits_last_shown_date")

             var premiumVersionText = ""
              if (dateDiff > 2) {
                  //premiumAdvancedDialerSpeakWasShowed = true
                  saveInMemory(view.context,"premium_dialer_speak_digits_last_shown_date")
                  premiumVersionText = "Get the Premium version for an advanced Dialer with option to speak digits!"
              }
              else {
                  dateDiff = calculateDaysDifference(view.context,"premium_dialer_gold_number_last_shown_date")
                  if (dateDiff > 3) {
                      saveInMemory(view.context,"premium_dialer_gold_number_last_shown_date")
                      premiumVersionText = "Get the Premium version for an advanced Dialer with Gold Number!"
                  }
              }
              if (premiumVersionText.isNotEmpty()) {
                 // Toast.makeText(context, premiumVersionText, Toast.LENGTH_LONG).show()
                  // Inside your Activity or Fragment
                  Snackbar.make(view, premiumVersionText, Snackbar.LENGTH_LONG)
                      .show()
              }

              PermissionsStatus.defaultDialerPermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
                  if (permissionGranted) {
                      if (callIsWaitingForPermission != null) {
                          OutgoingCall.makeCall(callIsWaitingForPermission!!, false, requireContext(), parentFragmentManager)
                          callIsWaitingForPermission = null
                      }
                  }
              }*/

        PermissionsStatus.callPhonePermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
            if (permissionGranted) {
                if (callIsWaitingForPermission != null) {
                    val activity = requireActivity()
                    OutgoingCall.makeCall(callIsWaitingForPermission!!, false, currContext, parentFragmentManager, activity, requestPermissionLauncher)
                    callIsWaitingForPermission = null
                }
            }
        }

        val allowMakingCallsMode = loadAllowMakingCallsEnum(currContext)
        if (AllowOutgoingCallsEnum.valueOf(allowMakingCallsMode.toString()) == AllowOutgoingCallsEnum.TO_EVERYONE) {
            keypadBack.visibility = VISIBLE
            textEnteredNumberBack.visibility = VISIBLE
            controlButtonsBack.visibility = VISIBLE
            noOutGoingCallsCaptionBack.visibility = GONE
        }
        else if (AllowOutgoingCallsEnum.valueOf(allowMakingCallsMode.toString()) == AllowOutgoingCallsEnum.FAVOURITES_ONLY) {
            // Show only Favourites to call to
            keypadBack.visibility = GONE
            controlButtonsBack.visibility = GONE
            textEnteredNumberBack.visibility = GONE
            appWasConfiguredWithoutOutgoingCaption.text =
                getString(R.string.outgoing_calls_are_currently_restricted_to_favorites)
            noOutGoingCallsCaptionBack.visibility = VISIBLE

        }
        else if (AllowOutgoingCallsEnum.valueOf(allowMakingCallsMode.toString()) == AllowOutgoingCallsEnum.CONTACTS_ONLY) {
           // We should never get here
            // Show caption - tou c
            keypadBack.visibility = GONE
            controlButtonsBack.visibility = GONE
            textEnteredNumberBack.visibility = GONE
            appWasConfiguredWithoutOutgoingCaption.text =
                getString(R.string.outgoing_calls_are_currently_restricted_to_contacts)
            noOutGoingCallsCaptionBack.visibility = VISIBLE
        }
        else if (AllowOutgoingCallsEnum.valueOf(allowMakingCallsMode.toString()) == AllowOutgoingCallsEnum.NO_ONE) {
            // Show call is not possible
           // We should never get here
            keypadBack.visibility = GONE
            textEnteredNumberBack.visibility = GONE
            controlButtonsBack.visibility = GONE
            appWasConfiguredWithoutOutgoingCaption.text =
                getString(R.string.outgoing_calls_are_currently_turned_off)
            noOutGoingCallsCaptionBack.visibility = VISIBLE
        }
    }

    /**
     * פונקציה להשוואת שני תאריכים והחזרת ההבדל בימים.
     *
     * @param firstDate התאריך הראשון (למשל היום).
     * @param otherDate התאריך השני להשוואה.
     * @return ההבדל במספר הימים בין today ל-otherDate.
     */
/*    private fun getDaysDifference(firstDate: LocalDate, otherDate: LocalDate): Long {
        return ChronoUnit.DAYS.between(firstDate, otherDate)
    }

    private fun getDaysDifference2(firstDate: Calendar, otherDate: Calendar): Long {
        val millisBetween = otherDate.timeInMillis - firstDate.timeInMillis
        return millisBetween / (24 * 60 * 60 * 1000)
    }*/

    private fun goldNumberClicked(currContext: Context, view: View) {
        if (SettingsStatus.goldNumber.value != null) {
            OutgoingCall.callGoldNumber(currContext, parentFragmentManager, view, requireActivity(), requestPermissionLauncher)
        }
    }

    private fun getDaysDifference(firstDate: Any, otherDate: Any): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Safe to use java.time APIs
            val firstLocalDate = firstDate as LocalDate
            val otherLocalDate = otherDate as LocalDate
            ChronoUnit.DAYS.between(firstLocalDate, otherLocalDate)
        } else {
            // Fallback to older APIs
            val firstCalendar = firstDate as Calendar
            val otherCalendar = otherDate as Calendar

            val millisBetween = otherCalendar.timeInMillis - firstCalendar.timeInMillis
            millisBetween / (24 * 60 * 60 * 1000)
        }
    }

    private fun saveInMemory(context: Context, variableToSave: String) {
        val dateString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // For API ≥26: Use LocalDate and format it
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) // "yyyy-MM-dd"
        } else {
            // For API <26: Use Calendar and SimpleDateFormat
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            dateFormat.format(calendar.time) // "yyyy-MM-dd"
        }

        // Save the date string to SharedPreferences
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(variableToSave, dateString)
            apply()
        }
    }

    // Function to save in SharedPreferences
    private fun loadFromMemory(context: Context, variableToLoad: String): String? {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString(variableToLoad, null)
    }

    fun calculateDaysDifference(context: Context, variableToLoad: String): Long {
        val dateString = loadFromMemory(context, variableToLoad)
        var dateDiff: Long = 0

        if (dateString != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // **For API Level 26 and Above: Using java.time APIs**
                try {
                    val otherLocalDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                    val firstLocalDate = LocalDate.now()
                    dateDiff = ChronoUnit.DAYS.between(otherLocalDate, firstLocalDate)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Handle parsing error if needed
                }
            } else {
                // **For Below API Level 26: Using java.util.Calendar and SimpleDateFormat**
                try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val otherDate = dateFormat.parse(dateString)
                    val firstCalendar = Calendar.getInstance()
                    val otherCalendar = Calendar.getInstance().apply {
                        time = otherDate!!
                    }
                    val millisBetween = firstCalendar.timeInMillis - otherCalendar.timeInMillis
                    dateDiff = millisBetween / (24 * 60 * 60 * 1000)
                } catch (e: ParseException) {
                    e.printStackTrace()
                    // Handle parsing error if needed
                }
            }
        }

        return dateDiff
    }

    private fun deleteLastChar() {
        // Removing the last digit

        if (unformattedPhoneNum.isNotEmpty()) {
            unformattedPhoneNum = unformattedPhoneNum.dropLast(1)
            if (unformattedPhoneNum.length > 3) {
                textEnteredNumber!!.text = OngoingCall.formatPhoneNumberWithLib(unformattedPhoneNum, languageEnum.region)
            }
            else {
                textEnteredNumber!!.text = unformattedPhoneNum
            }
            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                setContactName(unformattedPhoneNum)
            }
        }
        // show/hide back delete button
       // val backDeleteButtonImage = view.findViewById<ImageView>(R.id.deleteOneCharButton)
       // backDeleteButtonImage.visibility = if (textEnteredNumber?.text.isNullOrEmpty()) GONE else VISIBLE
    }

    private fun clickKey(keyChar: String, view: View) {
       // val backDeleteButtonImage = view.findViewById<ImageView>(R.id.deleteOneCharButton)
       /* buttonImage?.setImageResource(keypadPressedImaged)
        Handler(Looper.getMainLooper()).postDelayed({
            buttonImage?.setImageResource(originalkeypadImage)
        }, 200) // Revert after 200ms*/
        if (textEnteredNumber !== null) {
            unformattedPhoneNum += keyChar
            if (unformattedPhoneNum.length > 3) {
                textEnteredNumber!!.text = OngoingCall.formatPhoneNumberWithLib(unformattedPhoneNum, languageEnum.region)
            }
            else {
                textEnteredNumber!!.text = unformattedPhoneNum
            }
            if (PermissionsStatus.readContactsPermissionGranted.value == true) {
                setContactName(unformattedPhoneNum)
            }
        }
       // backDeleteButtonImage.visibility = if (textEnteredNumber?.text.isNullOrEmpty()) GONE else VISIBLE
    }

    private fun setContactName(phoneNumber: String) {
        if (phoneNumber.isNotEmpty()) {
            val currContext = requireContext()
            val contactName = getContactNameFromPhoneNumberAndReturnNullIfNotFound(currContext, phoneNumber)
            if (contactName != null) {
                contactNameContainer?.visibility = VISIBLE
                contactNameTextView?.text = contactName
            }
            else {
                contactNameContainer?.visibility = INVISIBLE
                contactNameTextView?.text = ""
            }
        }
    }

    private fun makePhoneCallFromKeyPadText() {
        if (unformattedPhoneNum.isNotEmpty()) {
            makeCall(unformattedPhoneNum)
        }
    }

    private val callDisconnectedObserver = Observer<Boolean> { wasDisconnected ->
        if (wasDisconnected) {
            unformattedPhoneNum = ""
            textEnteredNumber?.text = ""
        }
    }

    private fun makeCall(callPhoneNumber: String) {
        callIsWaitingForPermission = if (PermissionsStatus.defaultDialerPermissionGranted.value != true || PermissionsStatus.callPhonePermissionGranted.value != true) { // no permission
            callPhoneNumber
        } else {
            null
        } // we still go with the call, even if no permissions, and the user can grant it
        val activity = requireActivity()
        val callWasSuccessfullyPlace = OutgoingCall.makeCall(callPhoneNumber, false, requireContext(), parentFragmentManager,
            activity = activity, requestPermissionLauncher)

        OutgoingCall.callWasDisconnected.removeObserver(callDisconnectedObserver)
        OutgoingCall.callWasDisconnected.value = false
        OutgoingCall.callWasDisconnected.observe(viewLifecycleOwner, callDisconnectedObserver)


        /*        if (callWasSuccessfullyPlace) { // if call was placed delete clicked digits
                    textEnteredNumber?.text = ""
                }*/
        //   if (checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED) {
        //val uri = "tel:${+97237537900}".toUri()

       // val uri = "tel:${callPhoneNumber}".toUri()

/*        if (PermissionsStatus.callPhonePermissionGranted.value !== null && PermissionsStatus.callPhonePermissionGranted.value!!) {
            // We have permissions - make the call:
            if (PermissionsStatus.defaultDialerPermissionGranted == true) {
*//*                OutgoingCall.isCalling = true
                ContextCompat.startActivity(requireContext(), Intent(Intent.ACTION_CALL, uri), null)*//*

            }
            else {

            }
        } else { // ask for permissions:
            loadPermissionAlert()
        }*/

        //     } else {
        //        requestPermissions(this, arrayOf(CALL_PHONE), REQUEST_PERMISSION)
        //      }
    }

    private fun registerCallForDisconnect(call: Call?) {
        callCallback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
                try {
                    super.onStateChanged(call, newState)
                    if (newState == Call.STATE_DISCONNECTED) {
                        // The call was disconnected
                        textEnteredNumber?.text = ""
                        unformattedPhoneNum = ""
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Simple Dialer - registerCallForDisconnect error (error: ${e.message})")
                }
            }
        }

// רישום ה־callback
        call?.registerCallback(callCallback)
    }

    private fun loadPermissionAlert() {
        val overlayFragment = PermissionsAlertFragment()
        val args = Bundle().apply {
            putBoolean("IS_CALL_PERMISSION", true)
        }
        overlayFragment.arguments = args
        overlayFragment.show(parentFragmentManager, "PermissionMissingAlertDialogTag")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            // Hide the FAB in this fragment
            fab?.visibility = View.VISIBLE
        }
        OutgoingCall.callWasDisconnected.removeObserver(callDisconnectedObserver)
    }

    override fun onResume() {
        super.onResume()
        if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
            OpenScreensStatus.shouldCloseSettingsScreens.value = OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
        }
        if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
            OpenScreensStatus.shouldClosePermissionsScreens.value = OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
        }
        if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
            OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
        }
        if (PermissionsStatus.defaultDialerPermissionGranted.value === null || (!(PermissionsStatus.defaultDialerPermissionGranted.value!!))) {
            val context = requireContext()
            val telecomManager = context.getSystemService(TELECOM_SERVICE) as TelecomManager
            val permissionWasGranted = telecomManager.defaultDialerPackage == context.packageName

            if (permissionWasGranted) {
                // Permission is granted
                PermissionsStatus.defaultDialerPermissionGranted.value = true
                showCustomToastDialog(context, getString(R.string.default_dialer_permission_granted_try_again))

               /* Toast.makeText(context,
                    getString(R.string.default_dialer_permission_granted_try_again), Toast.LENGTH_LONG).show()*/
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_LONG).show()
            //  }
        }

 /*       if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readContactsPermissionGranted.value = true
                Toast.makeText(context,
                    getString(R.string.read_contacts_permission_granted_try_again), Toast.LENGTH_LONG).show()
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_LONG).show()
            //  }
        }*/
        val context = requireContext()
        if (PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.callPhonePermissionGranted.value = true
                makingMakingCallPermission = false
                /*Toast.makeText(context,
                    getString(R.string.make_call_permission_granted_try_again), Toast.LENGTH_LONG).show()*/
                showCustomToastDialog(context, getString(R.string.make_call_permission_granted_try_again))
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_LONG).show()
            //  }
        }
    }
}

// Define a sealed class to represent different parsed date types
sealed class ParsedDate {
    data class ModernDate(val localDate: LocalDate) : ParsedDate()
    data class LegacyDate(val date: Date) : ParsedDate()
    object InvalidDate : ParsedDate()
}