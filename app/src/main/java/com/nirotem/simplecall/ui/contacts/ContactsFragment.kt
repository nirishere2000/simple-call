package com.nirotem.simplecall.ui.contacts

import android.Manifest.permission.READ_CONTACTS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.adapters.ContactsAdapter
import com.nirotem.simplecall.helpers.DBHelper.getContactIdFromPhoneNumber
import com.nirotem.simplecall.helpers.DBHelper.getContactPhotoUri
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.statuses.AllowOutgoingCallsEnum
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.singleCallHistory.SingleCallHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class ContactsFragment : Fragment(R.layout.fragment_contacts) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var contactsAdapter: ContactsAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var noContactsAvailableMsg: TextView

    //  private lateinit var topBar: LinearLayout
    private lateinit var contactsNoPermissionsContainer: LinearLayout
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var askingReadContactsPermission = false
    private lateinit var fragmentRoot: View
    private lateinit var navController: NavController
    private var isSearchMode: Boolean = false
    private lateinit var searchText: EditText
    private lateinit var addNewButtonSmall: TextView
    private lateinit var addNewContactButtonContainer: FrameLayout
    var contactsList: List<ContactsInLetterListItem.Contact>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (askingReadContactsPermission) { // Read Contacts permission was granted
                    askingReadContactsPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
                    loadFragment(fragmentRoot)
/*                    Toast.makeText(
                        this.requireContext(),
                        getString(R.string.permission_was_granted_reloading),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.permission_was_granted_reloading))
                }
            } else {
                suggestManualPermissionGrant(context)
            }

            /*   else {
                   val toastMsg = "Cannot continue since permission was not approved"
                   Toast.makeText(
                       this.requireContext(),
                       toastMsg,
                       Toast.LENGTH_SHORT
                   ).show()
               }*/
        }
    }

    /*    override fun onStop() {
            super.onStop()
            //OpenScreensStatus.shouldCloseTopMenuScreens.value = false
        }*/

    override fun onResume() {
        super.onResume()

        try {
            if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
                OpenScreensStatus.shouldCloseSettingsScreens.value =
                    OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
            }
            if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
                OpenScreensStatus.shouldClosePermissionsScreens.value =
                    OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
            }
            if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
                OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
            }
            /*            val threeDotsMenuNavController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_content_main)

                        // Attempt to remove nav_settings and nav_permissions from the back stack if they exist
                        threeDotsMenuNavController.popBackStack(R.id.nav_settings, true)
                        threeDotsMenuNavController.popBackStack(R.id.nav_permissions, true)
                        threeDotsMenuNavController.popBackStack()
                        threeDotsMenuNavController.popBackStack(R.id.nav_contacts, false)
                        navController.popBackStack(R.id.nav_contacts, false)*/

        } catch (error: Error) {
            Log.e(
                "SimplyCall - ContactsFragment",
                "ContactsFragment onResume threeDotsMenuNavController Error ($error)"
            )
        }

        val context = requireContext()

        if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readContactsPermissionGranted.value = true
                if (askingReadContactsPermission) { // otherwise it could jump when granting default dialer and could confuse
/*                    Toast.makeText(
                        context,
                        getString(R.string.view_contacts_permission_was_granted_reloading),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.view_contacts_permission_was_granted_reloading))
                }
                askingReadContactsPermission = false

                // loadFragment(fragmentRoot)
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }
        if (PermissionsStatus.callPhonePermissionGranted.value != true) {
            PermissionsStatus.callPhonePermissionGranted.value = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        }

        // We always refresh on resume because maybe Contacts were added/changed/removed/favorites
        initView(fragmentRoot)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            navController = findNavController()
            if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
                OpenScreensStatus.shouldCloseSettingsScreens.value =
                    OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
            }
            if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
                OpenScreensStatus.shouldClosePermissionsScreens.value =
                    OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
            }
            if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
                OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
            }
            fragmentRoot = view
            progressBar = view.findViewById(R.id.progressBar)
            val topBar = view.findViewById<LinearLayout>(R.id.topBar)
            topBar.visibility = GONE // for now hidden - we want simple
            recyclerView = view.findViewById(R.id.recyclerView)

            // For now we go by Gold Number Contact.
            SettingsStatus.goldNumberContact.observe(viewLifecycleOwner) { newGoldNumber ->
                //initGoldNumber(view)
                initView(view)
            }

            // If no permission to make calls - listen to the permission for refresh
            if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                PermissionsStatus.callPhonePermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
                    if (permissionGranted) {
                        initView(view)
                    }
                }
            }
            // Response to Allow outgoing mode changes:
            SettingsStatus.userAllowOutgoingCallsEnum.observe(viewLifecycleOwner) { newUserAllowOutgoingCallsEnum ->
                initView(view)
            }

            /*            val callGoldNumberButton = view.findViewById<ImageView>(R.id.callGoldNumber)

                        callGoldNumberButton.setOnClickListener {
                            val activity = requireActivity()
                            callGoldNumber(context, parentFragmentManager, view, activity, requestPermissionLauncher)
                        }*/

            initView(view)

        } catch (error: Error) {
            Log.e("SimplyCall - ContactsFragment", "ContactsFragment onViewCreated Error ($error)")
        }


        // Set adapter for RecyclerView
        // callHistoryAdapter = CallHistoryAdapter(loadCallHistory())
        // recyclerView.adapter = callHistoryAdapter


    }

    private fun initView(view: View) {
        // Maybe we can just ask for Write Contacts for this also and not needing Read Contacts
        if (PermissionsStatus.readContactsPermissionGranted.value !== null && PermissionsStatus.readContactsPermissionGranted.value!!) {
            loadFragment(view)
        } else {
            progressBar.visibility = GONE
            //topBar.visibility = GONE // should start gone
            recyclerView.visibility = GONE // should start gone
            contactsNoPermissionsContainer =
                view.findViewById(R.id.contactsNoPermissionsContainer)
            contactsNoPermissionsContainer.visibility = View.VISIBLE
            val contactsApproveViewCallsPermissionButton =
                view.findViewById<Button>(R.id.contactsApproveViewCallsPermission)

            contactsApproveViewCallsPermissionButton.setOnClickListener {
                // if (!ActivityCompat.shouldShowRequestPermissionRationale(
                //        requireActivity(),
                //        READ_CONTACTS
                //    )
                // ) {
                //     showReadContactsPermissionsExplanationDialog()
                // } else {
                try {
                    askingReadContactsPermission = true
                    showPermissionsConfirmationDialog(
                        fragmentRoot.context.getString(R.string.permission_needed_capital_p),
                        getString(R.string.in_order_to_read_contacts_details_app_must_have_the_proper_permission),
                        ::requestReadContactPermission
                    )
                } catch (e: Exception) {
                   // showReadContactsPermissionsExplanationDialog(fragmentRoot.context)
                    suggestManualPermissionGrant(fragmentRoot.context)
                }

                // }
            }
        }
    }

    private fun handleDisplayMode() {
        if (isSearchMode) {
            searchText.visibility = VISIBLE
            addNewButtonSmall.visibility = VISIBLE
            addNewContactButtonContainer.visibility = GONE
        } else {
            searchText.visibility = GONE
            addNewButtonSmall.visibility = GONE
            addNewContactButtonContainer.visibility = VISIBLE
        }
    }

    private fun loadFragment(view: View) {
        val searchButton = view.findViewById<ImageView>(R.id.searchButton)
        searchText = view.findViewById(R.id.searchText)
        addNewButtonSmall = view.findViewById(R.id.addNewButtonSmall)
        addNewContactButtonContainer = view.findViewById(R.id.addNewContactButtonContainer)

        contactsNoPermissionsContainer = view.findViewById(R.id.contactsNoPermissionsContainer)
        contactsNoPermissionsContainer.visibility = View.GONE
        progressBar = view.findViewById(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        noContactsAvailableMsg = view.findViewById(R.id.noContactsAvailableMsg)
        //topBar = view.findViewById(R.id.topBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Load data asynchronously
        lifecycleScope.launch {
            showLoading() // Show ProgressBar
            contactsList = loadCallHistoryAsync()
            setupRecyclerView(contactsList!!)
            hideLoading() // Hide ProgressBar
            //val lastTimeGoldNumberAnimated = loadLastTimeAnimateGoldNumberInContacts(view.context)
            //if (lastTimeGoldNumberAnimated != null && lastTimeGoldNumberAnimated.isEmpty()) {
            //    saveLastTimeAnimateGoldNumberInContacts(view.context) // so we'll animate only once per app loaded
            //    animateGoldNumber()
            //}
        }

        searchButton.setOnClickListener {
            isSearchMode = !isSearchMode
            handleDisplayMode()
        }
        handleDisplayMode()

        addNewContactButtonContainer.setOnClickListener {
            addContact()
        }

        addNewButtonSmall.setOnClickListener {
            addContact()
        }

        searchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // val filter = s.toString()
                //  val filteredContactsList = contactsList.filter { it.contactOrPhoneNumber.contains(filter, ignoreCase = true) }
                contactsList?.let { setupRecyclerView(it) }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView(contactsList: List<ContactsInLetterListItem.Contact>) {
        var filteredContactsList: List<ContactsInLetterListItem.Contact>
        var filter = ""
        if (isSearchMode) {
            filter = searchText.text.toString().lowercase(Locale.getDefault())
            filteredContactsList = contactsList.partition {
                it.contactOrPhoneNumber.lowercase(Locale.getDefault()).startsWith(filter)
            }.first
        } else { // no filter - take as it is
            filteredContactsList = contactsList
        }

        //val filteredContactsList = contactsList.partition { it.contactOrPhoneNumber.contains(filter) }


        // הנחה שהרשימה כבר ממוין, אחרת ניתן למיין כאן
        val sortedContacts = filteredContactsList.sortedBy { it.contactOrPhoneNumber }
        val (sortedFavouritesContacts, notFavouritesSortedContacts) = sortedContacts.partition { it.isFavourite }


        val contactsByLetterListItems = mutableListOf<ContactsInLetterListItem>()
        var lastHeader = ""

        // Gold Number:
        val context = this.requireContext()
        val sharedPreferences =
            this.context?.getSharedPreferences("SimpleCallPreferences", Context.MODE_PRIVATE)

        val goldPhoneContactName = sharedPreferences?.getString("gold_phone_contactName", null)
        val goldPhoneNumber =
            if (SettingsStatus.goldNumber.value != null) SettingsStatus.goldNumber.value.toString() else ""
        val contactID = getContactIdFromPhoneNumber(context, goldPhoneNumber)
        val photoUri = if (contactID != null) getContactPhotoUri(context, contactID) else ""
        //ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactID.toLong())

        if ((!goldPhoneContactName.isNullOrEmpty())) {
            contactsByLetterListItems.add(ContactsInLetterListItem.GoldNumberHeader(true))
            contactsByLetterListItems.add(
                ContactsInLetterListItem.Contact(
                    phoneNumber = goldPhoneNumber,
                    contactOrPhoneNumber = goldPhoneContactName,
                    isFavourite = false,
                    photoUri = photoUri.toString(),
                    contactBirthday = null
                )
            )
        }
        // }


        // Favourites:
        if (sortedFavouritesContacts.isNotEmpty()) {
            contactsByLetterListItems.add(ContactsInLetterListItem.FavouritesHeader(true))
            for (contact in sortedFavouritesContacts) {
                if (goldPhoneContactName != contact.contactOrPhoneNumber && goldPhoneNumber != contact.phoneNumber) {
                    contactsByLetterListItems.add(
                        ContactsInLetterListItem.Contact(
                            phoneNumber = contact.phoneNumber,
                            contactOrPhoneNumber = contact.contactOrPhoneNumber,
                            isFavourite = contact.isFavourite,
                            photoUri = contact.photoUri,
                            contactBirthday = contact.contactBirthday
                        )
                    )
                }
            }
        }

        // Non-Favourites:
        if (SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.CONTACTS_ONLY
            || SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.TO_EVERYONE
        ) {
            val englishList = filteredContactsList.filter {
                it.contactOrPhoneNumber.firstOrNull()?.let { char -> char in 'A'..'z' } ?: false
            }.sortedBy { it.contactOrPhoneNumber }

            val numbersList = filteredContactsList.filter {
                it.contactOrPhoneNumber.firstOrNull()?.isDigit() ?: false
            }.sortedBy { it.contactOrPhoneNumber }

            val localeList = filteredContactsList.filter {
                val firstChar = it.contactOrPhoneNumber.firstOrNull() ?: return@filter false
                !firstChar.isDigit() && !isEnglishLetter(firstChar)
            }.sortedBy { it.contactOrPhoneNumber }

            for (contact in localeList) {
                if (goldPhoneContactName != contact.contactOrPhoneNumber && goldPhoneNumber != contact.phoneNumber) {
                    val firstLetter = contact.contactOrPhoneNumber.first().toUpperCase().toString()

                    if (firstLetter != lastHeader) {
                        // הוספת כותרת חדשה לרשימה
                        contactsByLetterListItems.add(ContactsInLetterListItem.Header(firstLetter))
                        lastHeader = firstLetter
                    }

                    // הוספת איש קשר לרשימה עם כל הפרמטרים
                    contactsByLetterListItems.add(
                        ContactsInLetterListItem.Contact(
                            phoneNumber = contact.phoneNumber,
                            contactOrPhoneNumber = contact.contactOrPhoneNumber,
                            isFavourite = contact.isFavourite,
                            photoUri = contact.photoUri,
                            contactBirthday = contact.contactBirthday
                        )
                    )
                }
            }

            for (contact in englishList) {
                if (goldPhoneContactName != contact.contactOrPhoneNumber && goldPhoneNumber != contact.phoneNumber) {
                    val firstLetter = contact.contactOrPhoneNumber.first().toUpperCase().toString()

                    if (firstLetter != lastHeader) {
                        // הוספת כותרת חדשה לרשימה
                        contactsByLetterListItems.add(ContactsInLetterListItem.Header(firstLetter))
                        lastHeader = firstLetter
                    }

                    // הוספת איש קשר לרשימה עם כל הפרמטרים
                    contactsByLetterListItems.add(
                        ContactsInLetterListItem.Contact(
                            phoneNumber = contact.phoneNumber,
                            contactOrPhoneNumber = contact.contactOrPhoneNumber,
                            isFavourite = contact.isFavourite,
                            photoUri = contact.photoUri,
                            contactBirthday = contact.contactBirthday
                        )
                    )
                }
            }

            for (contact in numbersList) {
                if (goldPhoneContactName != contact.contactOrPhoneNumber && goldPhoneNumber != contact.phoneNumber) {
                    val firstLetter = contact.contactOrPhoneNumber.first().toUpperCase().toString()

                    if (firstLetter != lastHeader) {
                        // הוספת כותרת חדשה לרשימה
                        contactsByLetterListItems.add(ContactsInLetterListItem.Header(firstLetter))
                        lastHeader = firstLetter
                    }

                    // הוספת איש קשר לרשימה עם כל הפרמטרים
                    contactsByLetterListItems.add(
                        ContactsInLetterListItem.Contact(
                            phoneNumber = contact.phoneNumber,
                            contactOrPhoneNumber = contact.contactOrPhoneNumber,
                            isFavourite = contact.isFavourite,
                            photoUri = contact.photoUri,
                            contactBirthday = contact.contactBirthday
                        )
                    )
                }
            }
        }

        // קבלת ה-ViewModel
        val sharedViewModel: SingleCallHistoryViewModel by activityViewModels()
        val activity = requireActivity()

        // יצירת ה-Adapter עם הרשימה המעודכנת
        contactsAdapter = ContactsAdapter(
            contactsByLetterListItems,
            this.context,
            navController,
            sharedViewModel,
            parentFragmentManager,
            activity,
            requestPermissionLauncher,
            viewLifecycleOwner
        )

        // הגדרת ה-Adapter ל-RecyclerView
        recyclerView.adapter = contactsAdapter
        if (SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.FAVOURITES_ONLY) { // We only tried to find Favorite Contacts
            noContactsAvailableMsg.text =
                fragmentRoot.context.getString(R.string.no_favorites_contacts_found_to_display) // don't let the user think there's no Contacts at all
        } else if (SettingsStatus.userAllowOutgoingCallsEnum.value == AllowOutgoingCallsEnum.NO_ONE) {
            noContactsAvailableMsg.text = getString(R.string.no_contacts_are_displayed_because_outgoing_calls_are_disabled)
        } else {
            noContactsAvailableMsg.text =
                fragmentRoot.context.getString(R.string.no_contacts_found_to_display) // Could not find Contacts
        }

        val anyContactsAvailable = (!contactsByLetterListItems.isEmpty())
        noContactsAvailableMsg.visibility =
            if (anyContactsAvailable) GONE else VISIBLE

        // Since we can get the 'readContactsPermissionGranted' during Tour opened, and this screen will refresh,
        // We don't want to show the message while Tour is opened
        if (anyContactsAvailable && !OpenScreensStatus.isHelpScreenOpened && !OpenScreensStatus.alreadyShownContactsClickOnRowForMore) {
            OpenScreensStatus.alreadyShownContactsClickOnRowForMore = true
/*            Toast.makeText(
                this.requireContext(),
                getString(R.string.tap_on_contact_row_for_more),
                Toast.LENGTH_LONG
            ).show()*/

            showCustomToastDialog(this.requireContext(), getString(R.string.tap_on_contact_row_for_more))
        }
    }

    private fun isEnglishLetter(c: Char): Boolean {
        return (c in 'A'..'Z' || c in 'a'..'z')
    }

    /*    private fun setupRecyclerView2(contactsList: List<ContactsInLetterListItem.Contact>) {
            val sortedContacts = contactsList // should already be sorted. contactsList.sortedBy { it.contactOrPhoneNumber }

            val contactsByLetterListItems = mutableListOf<LauncherActivity.ListItem>()
            var lastHeader = ""

            for (contact in sortedContacts) {
                val firstLetter = contact.contactOrPhoneNumber.first().toUpperCase().toString()
                if (firstLetter != lastHeader) {
                    contactsByLetterListItems.add(ContactsInLetterListItem.Header(firstLetter))
                    lastHeader = firstLetter
                }
                contactsByLetterListItems.add(
                    ContactsInLetterListItem.Contact(
                        contact.contactOrPhoneNumber,
                        contact.phoneNumber
                    )
                )
            }

            val sharedViewModel: SingleCallHistoryViewModel by activityViewModels()
            contactsAdapter = ContactsAdapter(contactsByLetterListItems, this.context, navController, sharedViewModel)
            recyclerView.adapter = contactsAdapter
        }*/

    private suspend fun loadCallHistoryAsync(): List<ContactsInLetterListItem.Contact> {
        val context = this.context
        return withContext(Dispatchers.IO) {
            // Simulate data loading (e.g., from Call Log)
            if (PermissionsStatus.defaultDialerPermissionGranted.value == true && context != null) {
                loadContactsOptimized(context)
            } else {
                loadContacts()
            }

        }
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noContactsAvailableMsg.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        noContactsAvailableMsg.visibility =
            if (recyclerView.adapter != null && recyclerView.adapter!!.itemCount > 0) GONE else VISIBLE
        //topBar.visibility = View.VISIBLE
    }

    private fun loadContacts(): List<ContactsInLetterListItem.Contact> {
        val contactsList = mutableListOf<ContactsInLetterListItem.Contact>()

        try {
            val uniqueContacts = mutableSetOf<String>()  // Set to store unique phone numbers
            val cursor = requireContext().contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME, // Name
                    ContactsContract.CommonDataKinds.Phone.NUMBER, // Phone number
                    // ContactsContract.CommonDataKinds.Event.START_DATE, // יום ההולדת נמצא כאן
                    ContactsContract.Contacts.PHOTO_URI, // Photo URI
                    ContactsContract.Contacts.STARRED, // Starred status (1 or 0)
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER} = 1",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC" // Sort by name
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactName =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val photoUri =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                    val photoUriStr = if (photoUri !== null) photoUri else ""
                    val isStarred =
                        it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1
                    // val birthdayLong =
                    //  it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE))
                    //val birthday = formatTimestamp(birthdayLong)

                    val contact =
                        ContactsInLetterListItem.Contact(
                            phoneNumber,
                            contactName,
                            null,
                            isStarred,
                            photoUriStr
                        )

                    val shortPhoneNumber = phoneNumber.filter { it.isDigit() }

                    if (uniqueContacts.add(contactName + shortPhoneNumber)) {  // Adds only unique Contact + phone number
                        contactsList.add(contact)
                    }


                    // println("Contact: $name, Phone: $phoneNumber, Photo: $photoUri, Starred: $isStarred")
                }
            }
        } catch (error: Error) {
            Log.e("SimplyCall - ContactsFragment", "ContactsFragment loadContacts Error ($error)")
        }


        return contactsList
    }

    private fun loadContactsOptimized(context: Context): List<ContactsInLetterListItem.Contact> {
        val contactsList = mutableListOf<ContactsInLetterListItem.Contact>()

        try {
            // Step 1: Fetch all blocked numbers into a Set for efficient lookup
            val blockedNumbers = mutableSetOf<String>()
            context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex =
                    cursor.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                if (numberIndex != -1) {
                    while (cursor.moveToNext()) {
                        val blockedNumber = cursor.getString(numberIndex)
                        if (!blockedNumber.isNullOrEmpty()) {
                            blockedNumbers.add(normalizePhoneNumber(blockedNumber))
                        }
                    }
                }
            }


            val uniqueContacts = mutableSetOf<String>()  // Set to store unique phone numbers
            val cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME, // Name
                    ContactsContract.CommonDataKinds.Phone.NUMBER, // Phone number
                    // ContactsContract.CommonDataKinds.Event.START_DATE, // יום ההולדת נמצא כאן
                    ContactsContract.Contacts.PHOTO_URI, // Photo URI
                    ContactsContract.Contacts.STARRED, // Starred status (1 or 0)
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER} = 1",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC" // Sort by name
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactName =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val photoUri =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                    val photoUriStr = if (photoUri !== null) photoUri else ""
                    val isStarred =
                        it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1
                    // val birthdayLong =
                    //  it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE))
                    //val birthday = formatTimestamp(birthdayLong)

                    val contact =
                        ContactsInLetterListItem.Contact(
                            phoneNumber,
                            contactName,
                            null,
                            isStarred,
                            photoUriStr
                        )

                    val shortPhoneNumber = phoneNumber.filter { it.isDigit() }

                    val isBlocked = blockedNumbers.contains(shortPhoneNumber)

                    if (!isBlocked) {
                        if (uniqueContacts.add(contactName + shortPhoneNumber)) {  // Adds only unique Contact + phone number
                            contactsList.add(contact)
                        }
                    }
                    // println("Contact: $name, Phone: $phoneNumber, Photo: $photoUri, Starred: $isStarred")
                }
            }
        } catch (error: Error) {
            Log.e("SimplyCall - ContactsFragment", "ContactsFragment loadContacts Error ($error)")
        }


        return contactsList
    }

    /**
     * Fetches all blocked numbers using BlockedNumberContract.
     */
    private fun fetchBlockedNumbers(context: Context): Set<String> {
        val blockedNumbers = mutableSetOf<String>()

        try {
            context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex =
                    cursor.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                if (numberIndex != -1) {
                    while (cursor.moveToNext()) {
                        val blockedNumber = cursor.getString(numberIndex)
                        if (!blockedNumber.isNullOrEmpty()) {
                            blockedNumbers.add(normalizePhoneNumber(blockedNumber))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(
                "SimplyCall - ContactsFragment",
                "fetchBlockedNumbers Error: ${e.localizedMessage}",
                e
            )
        }

        return blockedNumbers
    }

    /**
     * Normalizes phone numbers by removing non-digit characters.
     * Adjust this function based on your normalization requirements.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }

    private fun requestReadContactPermission() {
        requestPermissionLauncher.launch(READ_CONTACTS)
    }

    private fun showPermissionsConfirmationDialog(
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit
    ) {
        val builder = AlertDialog.Builder(this.requireContext())

        builder.setTitle(msgTitle)
        builder.setMessage(msgText)

        builder.setPositiveButton(fragmentRoot.context.getString(R.string.ask_permission_capital_a)) { dialog, which ->
            onAskPermission()
        }

        builder.setNegativeButton(fragmentRoot.context.getString(R.string.cancel_capital)) { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    fun makeACall(context: Context, phoneNumber: String) {
        /*        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

              //  val phoneNumber = "tel:+1234567890" // Replace with the desired phone number
                val uri = Uri.parse(phoneNumber)

                val phoneAccountHandle: PhoneAccountHandle = telecomManager.phoneAccountHandles[0] // Get the default phone account
                val intent = Intent(Intent.ACTION_CALL, uri)
                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)

                startActivity(intent) // Initiates the call*/
    }

    private fun addContact() {
        val bundle = Bundle().apply {
            putString("phone_number", "")
        }
        val navController = findNavController()
        navController.navigate(R.id.action_contacts_to_editContact, bundle)
    }

    /*    override fun onDestroy() {
            super.onDestroy()
            OpenScreensStatus.shouldCloseTopMenuScreens.value = false
        }*/
}

sealed class ContactsInLetterListItem {
    data class Header(val letter: String) : ContactsInLetterListItem()
    data class FavouritesHeader(val isShown: Boolean) : ContactsInLetterListItem()
    data class GoldNumberHeader(val isShown: Boolean) : ContactsInLetterListItem()
    data class Contact(
        val phoneNumber: String,
        val contactOrPhoneNumber: String,
        val contactBirthday: String?,
        val isFavourite: Boolean,
        val photoUri: String
    ) : ContactsInLetterListItem()
}
