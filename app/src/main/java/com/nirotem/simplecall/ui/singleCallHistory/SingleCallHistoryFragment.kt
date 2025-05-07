package com.nirotem.simplecall.ui.singleCallHistory

import android.Manifest
import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.adapters.SingleCallHistoryAdapter
import com.nirotem.simplecall.helpers.DBHelper.getContactPhoto
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.*

class SingleCallHistoryFragment : Fragment(R.layout.fragment_single_call_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var singleCallHistoryAdapter: SingleCallHistoryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerViewBack: LinearLayout

    // private lateinit var bottomBar: LinearLayout
    private lateinit var topBar: LinearLayout
    private lateinit var noPermissionsContainer: LinearLayout
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var askingViewCallsPermission = false
    private var askingReadContactPermission = false
    private var makingMakingCallPermission = false
    private lateinit var fragmentRoot: View
    private val loadedCallHistoryList = mutableListOf<SinglePhoneCall>()
    private var alreadyShownContactsExplaination = false
    private var isLoading = false
    private var currentOffset = 0
    private val limit = 100
    private var phoneNumberToCall = ""
    private var callsContact: String? = null
    private val languageEnum = LanguagesEnum.fromCode(Locale.getDefault().language)


    override fun onAttach(context: Context) {
        super.onAttach(context)
        val currContext = requireContext()

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (askingViewCallsPermission) { // from now on we have making call permission
                    askingViewCallsPermission = false
                    PermissionsStatus.readCallLogPermissionGranted.value = true
                    loadFragment(fragmentRoot)
                } else if (askingReadContactPermission) { // Read Contacts permission was granted
                    askingReadContactPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
                    val editContactsButton = fragmentRoot.findViewById<TextView>(R.id.addEditContact)
                    editContactsButton.visibility = VISIBLE
                    loadFragment(fragmentRoot)
                    loadContactDetails(true, currContext)
                } else if (makingMakingCallPermission) {
                    makingMakingCallPermission = false
                    PermissionsStatus.callPhonePermissionGranted.value = true
                }
/*                Toast.makeText(
                    currContext,
                    getString(R.string.permission_was_granted_reloading),
                    Toast.LENGTH_LONG
                ).show()*/
                showCustomToastDialog(context, context.getString(R.string.permission_was_granted_reloading))
            } else {
                var toastMsg = getString(R.string.cannot_continue_since_permission_was_not_approved)
                askingViewCallsPermission = false
                if (askingReadContactPermission) { // Read Contacts permission was granted
                    askingReadContactPermission = false
                    toastMsg = getString(R.string.contacts_permission_was_not_granted_cannot_show_contact_details)
                }
                //Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
/*                Toast.makeText(
                    currContext,
                    toastMsg,
                    Toast.LENGTH_SHORT
                ).show()*/
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currContext = requireContext()
        if (PermissionsStatus.readCallLogPermissionGranted.value === null || (!(PermissionsStatus.readCallLogPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    currContext,
                    READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readCallLogPermissionGranted.value = true
                askingViewCallsPermission = false
                loadScreen()
/*                Toast.makeText(
                    currContext,
                    getString(R.string.view_calls_permission_was_granted_reloading),
                    Toast.LENGTH_LONG
                ).show()*/
                showCustomToastDialog(currContext,
                    getString(R.string.view_calls_permission_was_granted_reloading))
                // loadFragment(fragmentRoot)
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
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
                val editContactsButton = fragmentRoot.findViewById<TextView>(R.id.addEditContact)
                editContactsButton.visibility = VISIBLE
                loadContactDetails(true, currContext)
                askingReadContactPermission = false
/*                Toast.makeText(
                    context,
                    getString(R.string.read_contacts_permission_was_granted_reloading),
                    Toast.LENGTH_LONG
                ).show()*/
                showCustomToastDialog(context, getString(R.string.read_contacts_permission_was_granted_reloading))
                //  loadFragment(fragmentRoot)
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }
    }

    private fun loadScreen() {
        try {
            val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
            if (isPremium) {
                val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
                fab?.visibility = GONE
            }
            val screenContext = requireContext()

            //val navController = findNavController()
            // val closeButton = view.findViewById<ImageView>(R.id.closeButton)
            /* closeButton.setOnClickListener {

                 navController.navigateUp()  // This will navigate back to the previous fragment        }

                 //exitTransition = android.transition.Fade() // Add fade effect
                // parentFragmentManager.popBackStack()  // This will remove the current fragment and return to the previous one
             }*/

            val singleCallContactNameOrPhone =
                fragmentRoot.findViewById<TextView>(R.id.singleCallContactNameOrPhone)

            if (callsContact != null) {
                singleCallContactNameOrPhone.text = callsContact
            } else {
                singleCallContactNameOrPhone.text = getString(R.string.unknown_capital)
            }

            val callButton = fragmentRoot.findViewById<TextView>(R.id.callButton)
            callButton.setOnClickListener {
                val activity = requireActivity()
                OutgoingCall.makeCall(phoneNumberToCall, false, screenContext, parentFragmentManager, activity, requestPermissionLauncher)
            }

            //var capabilities: Int? = null
            if (ContextCompat.checkSelfPermission(
                    screenContext,
                    Manifest.permission.READ_PHONE_NUMBERS
                )
                == PackageManager.PERMISSION_GRANTED
            ) {
                val telecomManager =
                    screenContext.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                val phoneAccounts =
                    telecomManager.callCapablePhoneAccounts // Get all call-capable phone accounts

                var videoSupportedAccount: PhoneAccountHandle? = null
                for (phoneAccountHandle in phoneAccounts) {
                    val phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle)
                    if (phoneAccount?.hasCapabilities(PhoneAccount.CAPABILITY_VIDEO_CALLING) == true) {
                        videoSupportedAccount = phoneAccount.accountHandle
                        Log.d(
                            "VideoCallCheck",
                            "Video calling is supported by account: ${phoneAccount.label}"
                        )
                    } else {
                        Log.d(
                            "VideoCallCheck",
                            "No video calling support on account: ${phoneAccount?.label}"
                        )
                    }
                }
                /*                capabilities =
                                    telecomManager.getPhoneAccount(videoSupportedAccount)?.capabilities*/
            }

/*            val isVideoCapable =
                true // for now true. capabilities?.and(PhoneAccount.CAPABILITY_VIDEO_CALLING) != 0

            if (isVideoCapable) {
                try {
                    val videoCallButton = fragmentRoot.findViewById<TextView>(R.id.videoCallButton)
                    videoCallButton.setOnClickListener {
                        val activity = requireActivity()
                        OutgoingCall.makeCall(phoneNumberToCall, true, screenContext, parentFragmentManager, activity, requestPermissionLauncher)
                    }
                } catch (error: Exception) {
                    Log.e(
                        "SimplyCall - SingleCallHistoryFragment",
                        "SingleCallHistoryFragment Video Error ($error)"
                    )
                }
            }*/

            val editContactsButton = fragmentRoot.findViewById<TextView>(R.id.addEditContact)
            //val addContactButton = view.findViewById<ImageView>(R.id.addContactButton)
            loadContactDetails(false, screenContext)

            //if (!missingCriticalPermissions) { // otherwise we will load the Permissions screen anyway
            if (PermissionsStatus.readCallLogPermissionGranted.value !== null && PermissionsStatus.readCallLogPermissionGranted.value!!) {
                noPermissionsContainer = fragmentRoot.findViewById(R.id.noPermissionsContainer)
                noPermissionsContainer.visibility = View.GONE
                loadFragment(fragmentRoot)
                if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
                    // if no Contact Read permission - we still show the screen with phone number and also show a Request Permission dialog
                    editContactsButton.visibility = GONE // we don't know if the number belongs to Contact and we cannot edit or add.
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            READ_CONTACTS
                        )
                    ) {
                        val missingCriticalPermissions =
                            PermissionsStatus.defaultDialerPermissionGranted.value === null || (!(PermissionsStatus.defaultDialerPermissionGranted.value!!))
                                    || PermissionsStatus.canDrawOverlaysPermissionGranted.value === null || (!(PermissionsStatus.canDrawOverlaysPermissionGranted.value!!))


                        /**
                         * showing Toast once and not if critical permissions are missing which should trigger the main permissions form
                         */
                        //if (!missingCriticalPermissions && !alreadyShownContactsExplaination) {
                        if (!alreadyShownContactsExplaination) {
                            alreadyShownContactsExplaination = true
                            val toastMsg =
                                getString(R.string.contacts_permission_was_not_granted_cannot_show_contact_details)
                            //Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                            showLongSnackBar(requireContext(), toastMsg, anchorView = requireView())
                        }

                        //showReadContactsPermissionsExplanationDialog()
                    } else {
                        askingReadContactPermission = true
                        showPermissionsConfirmationDialog(
                            fragmentRoot.context.getString(R.string.permission_needed_capital_p),
                            fragmentRoot.context.getString(R.string.to_display_the_calls_contacts_application_must_have_permission),
                            ::requestReadContactsPermission
                        )
                    }
                }
            } else {
                progressBar = fragmentRoot.findViewById(R.id.progressBar)
                progressBar.visibility = View.GONE
                noPermissionsContainer = fragmentRoot.findViewById(R.id.noPermissionsContainer)
                noPermissionsContainer.visibility = View.VISIBLE
                recyclerViewBack = fragmentRoot.findViewById(R.id.recyclerViewBack)
                recyclerViewBack.visibility = View.GONE
                topBar = fragmentRoot.findViewById(R.id.topBar)
                topBar.visibility = View.GONE
                // bottomBar = view.findViewById(R.id.bottomBar)


                val callsHistoryApproveViewCallsPermissionButton =
                    fragmentRoot.findViewById<Button>(R.id.callsHistoryApproveViewCallsPermission)
                callsHistoryApproveViewCallsPermissionButton.setOnClickListener {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(),
                            READ_CALL_LOG
                        )
                    ) {
                        //showCallLogPermissionsExplanationDialog(fragmentRoot.context)
                        suggestManualPermissionGrant(fragmentRoot.context)
                        //showPermissionsExplanationDialog()
                    } else {
                        askingViewCallsPermission = true
                        showPermissionsConfirmationDialog(
                            fragmentRoot.context.getString(R.string.permission_needed_capital_p),
                            getString(R.string.to_view_calls_history_application_must_have_permission),
                            ::requestCallsViewPermission
                        )
                    }
                }
            }
        } catch (error: Error) {
            Log.e(
                "SimplyCall - SingleCallHistoryFragment",
                "SingleCallHistoryFragment loadScreen Error ($error)"
            )
        }
    }

    //@SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentRoot = view
        phoneNumberToCall = arguments?.getString("phone_number").toString()
        callsContact = arguments?.getString("contact")
        loadScreen()

        val sharedViewModel: SingleCallHistoryViewModel by activityViewModels()
        sharedViewModel.contactData.observe(viewLifecycleOwner) { contactData ->
            phoneNumberToCall = contactData.phoneNumber
            callsContact = contactData.contactName
            loadScreen()
        }
    }

    fun isContactInFavorites(context: Context, contactId: String): Boolean {
        val uri =
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
        val projection = arrayOf(ContactsContract.Contacts.STARRED)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1
            }
        }
        return false
    }


    private fun getContactIdFromPhoneNumber(context: Context, phoneNumber: String): String? {
        // יצירת ה-URI עם PhoneLookup
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))

        // הגדרת הפרויקט - אנחנו צריכים רק את מזהה אנשי הקשר
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        // ביצוע השאילתה
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // מציאת אינדקס העמודה של מזהה אנשי הקשר
                val contactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)
                // החזרת מזהה אנשי הקשר
                return cursor.getString(contactIdIndex)
            }
        }
        // אם לא נמצא, החזרת null
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val isPremium = resources.getBoolean(R.bool.isAdvancedDialer)
        if (isPremium) {
            val fab = requireActivity().findViewById<FloatingActionButton>(R.id.fab)
            // Hide the FAB in this fragment
            fab?.visibility = View.VISIBLE
        }
    }

    private fun loadContactDetails(shouldReSearchContact: Boolean, context: Context) {
        val editContactsButton = fragmentRoot.findViewById<TextView>(R.id.addEditContact)
        val singleCallPhone = fragmentRoot.findViewById<TextView>(R.id.singleCallPhone)

        if (shouldReSearchContact && phoneNumberToCall == callsContact) {
            callsContact = getContactName(context, phoneNumberToCall)
            val singleCallContactNameOrPhone =
                fragmentRoot.findViewById<TextView>(R.id.singleCallContactNameOrPhone)
            if (callsContact != null) {
                singleCallContactNameOrPhone.text = callsContact
                singleCallContactNameOrPhone.textDirection = View.TEXT_DIRECTION_LOCALE // should be according to language
            } else {
                singleCallContactNameOrPhone.textDirection = View.TEXT_DIRECTION_LTR // phone number should be left to right
               // singleCallContactNameOrPhone.text = "Unknown"
            }
        }

        val contactExists = phoneNumberToCall !== callsContact
        val contactExistingPhotoBack =
            fragmentRoot.findViewById<FrameLayout>(R.id.contactExistingPhotoBack)
        contactExistingPhotoBack.visibility = GONE
        val addToFavImage = fragmentRoot.findViewById<ImageView>(R.id.addToFavImage)

        val singleCallContactNameOrPhone =
            fragmentRoot.findViewById<TextView>(R.id.singleCallContactNameOrPhone)

        if (contactExists) {
            singleCallContactNameOrPhone.textDirection = View.TEXT_DIRECTION_LOCALE // should be according to language
            editContactsButton.text = "Edit"
            editContactsButton.setOnClickListener {
                addEditContact(false)
            }
            singleCallPhone.text = OngoingCall.formatPhoneNumberWithLib(phoneNumberToCall, languageEnum.region)
            singleCallPhone.visibility = VISIBLE

            // Try to get photo:
            val contactId = getContactIdFromPhoneNumber(fragmentRoot.context, phoneNumberToCall)
            if (contactId != null) {
                val userProfilePicture = getContactPhoto(fragmentRoot.context, contactId)
                val contactExistingPhoto =
                    fragmentRoot.findViewById<ImageView>(R.id.contactExistingPhoto)
                if (userProfilePicture != null) {
                    contactExistingPhoto.setImageBitmap(userProfilePicture)
                    contactExistingPhotoBack.visibility = VISIBLE
                } else {
                    // Handle case where no profile picture exists
                    contactExistingPhotoBack.visibility = GONE
                }

                val isFavorite = isContactInFavorites(fragmentRoot.context, contactId)
                if (isFavorite) {
                    addToFavImage.visibility = VISIBLE
                } else {
                    addToFavImage.visibility = GONE
                }
            }
            else {
                addToFavImage.visibility = GONE
            }
        } else {
            singleCallContactNameOrPhone.text = OngoingCall.formatPhoneNumberWithLib(phoneNumberToCall, languageEnum.region)
            singleCallContactNameOrPhone.textDirection = View.TEXT_DIRECTION_LTR // phone number should be left to right
            editContactsButton.text = "Add"
            editContactsButton.setOnClickListener {
                addEditContact(true)
            }
            singleCallPhone.visibility = GONE
            addToFavImage.visibility = GONE
        }
    }

    private fun loadFragment(view: View) {
        recyclerView = view.findViewById(R.id.singleCallHistoryRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // progressBar = view.findViewById(R.id.progressBar)
        /*        if (loadedCallHistoryList === null) {
                    progressBar = view.findViewById(R.id.progressBar)
                    progressBar.visibility = View.VISIBLE
                    recyclerView = view.findViewById(R.id.recyclerView)

                    recyclerView.layoutManager = LinearLayoutManager(requireContext())

                    // Load data asynchronously
                 *//*   lifecycleScope.launch { // fetch first 100
                showLoading() // Show ProgressBar
                loadedCallHistoryList = loadCallHistoryAsync()
                setupRecyclerView(loadedCallHistoryList!!)
                hideLoading() // Hide ProgressBar
            }*//*


*//*            lifecycleScope.launch {
                loadedCallHistoryList = loadCallHistoryAsync(0)
                setupRecyclerView(loadedCallHistoryList!!)
            }*//*
        }
        else { // we already have at least the first chunk/page of the calls list
            progressBar = view.findViewById(R.id.progressBar)
            progressBar.visibility = View.GONE
            recyclerView = view.findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            setupRecyclerView(loadedCallHistoryList!!)
            hideLoading() // Hide ProgressBar
        }*/
        topBar = view.findViewById(R.id.topBar)
        //  bottomBar = view.findViewById(R.id.bottomBar)
        recyclerViewBack = view.findViewById(R.id.recyclerViewBack)
        progressBar = fragmentRoot.findViewById(R.id.progressBar)
        singleCallHistoryAdapter = SingleCallHistoryAdapter(requireContext())
        recyclerView.adapter = singleCallHistoryAdapter
        setupPagedRecyclerView(recyclerView, singleCallHistoryAdapter)
    }

    /*
        private fun setupRecyclerView(callHistoryList: List<SinglePhoneCall2>) {
            callHistoryAdapter = CallHistoryAdapter(this.context)
            recyclerView.adapter = callHistoryAdapter
            setupPagedRecyclerView(recyclerView, callHistoryAdapter)  // no limit bring the rest async
        }
    */

    private fun setupPagedRecyclerView(
        recyclerView: RecyclerView,
        adapter: SingleCallHistoryAdapter
    ) {
        //val existingRecordsSize = if (loadedCallHistoryList.size > 0) loadedCallHistoryList?.size else 0
        //if (loadedCallHistoryList.size <= 0) { // first time - init
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                if (!isLoading && totalItemCount >= limit && totalItemCount <= (lastVisibleItem + limit / 2)) {
                    loadMoreCallHistory(adapter)
                }
            }
        })

        loadMoreCallHistory(adapter) // Load initial data
        //}
        //   else {
        //currentOffset = existingRecordsSize
        // isLoading = false
        // }
    }

    private fun loadMoreCallHistory(adapter: SingleCallHistoryAdapter) {
        isLoading = true
        // progressBar = fragmentRoot.findViewById(R.id.progressBar)
        showLoading()
        try {
            lifecycleScope.launch {
                val newRecords = loadCallHistoryAsync(offset = currentOffset, limit = limit)
                // loadedCallHistoryList = listOf(loadedCallHistoryList, newRecords) // Immutable List
                //loadedCallHistoryList.addAll
                val noCallsFoundLabel = fragmentRoot.findViewById<TextView>(R.id.noCallsFoundLabel)

                if (newRecords.isEmpty()) {
                    noCallsFoundLabel.visibility = VISIBLE
                } else {
                    noCallsFoundLabel.visibility = GONE
                }
                adapter.addItems(newRecords)
                currentOffset += newRecords.size
                isLoading = false
                // progressBar = fragmentRoot.findViewById(R.id.progressBar)
                hideLoading()
            }
        } catch (err: Exception) {
            isLoading = false
            // progressBar = fragmentRoot.findViewById(R.id.progressBar)
            hideLoading()
            Log.e("Simply Call - Calls History Fragment", "Error trying to load Calls History", err)
        }

    }

    private suspend fun loadCallHistoryAsync(
        offset: Int = 0,
        limit: Int = 100
    ): List<SinglePhoneCall> {
        return withContext(Dispatchers.IO) {
            // Simulate data loading (e.g., from Call Log)
            loadCallHistoryAsyncPaged(offset, limit)
        }
    }

    private fun loadCallHistoryAsyncPaged(
        offset: Int = 0,
        limit: Int = 100
    ): List<SinglePhoneCall> {
        val context = this.context ?: return emptyList()
        val callHistoryList = mutableListOf<SinglePhoneCall>()
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumberToCall)

        try {
            val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                    putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(CallLog.Calls.DATE)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                    // Add the WHERE clause
                    putString(
                        ContentResolver.QUERY_ARG_SQL_SELECTION,
                        "${CallLog.Calls.NUMBER} = ?"
                    )
                    putStringArray(
                        ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                        arrayOf(normalizedNumber)
                    )
                }
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.DATE, CallLog.Calls.TYPE), // Only fetching DATE and TYPE
                    queryArgs,
                    null
                )
            } else {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.DATE, CallLog.Calls.TYPE), // Only fetching DATE and TYPE
                    "${CallLog.Calls.NUMBER} = ?", // WHERE clause
                    arrayOf(normalizedNumber),
                    "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
                )
            }

            cursor?.use {
                // val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                if (dateIndex != -1 && typeIndex != -1) {
                    var count = 0
                    while (it.moveToNext() && count < limit) {
                        // val originalNumber = it.getString(numberIndex)
                        // val number = getContactName(context, originalNumber) ?: originalNumber ?: ""
                        val date = formatTimestamp(it.getLong(dateIndex))
                        val callType = when (it.getInt(typeIndex)) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            CallLog.Calls.REJECTED_TYPE -> "Rejected"
                            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                            else -> "Unknown"
                        }

                        val call = SinglePhoneCall(date, callType)
                        callHistoryList.add(call)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SingleCallHistory", "Error loading call history", e)
        }

        return callHistoryList
    }


    /*    private suspend fun loadRestOfHistoryAsync(): List<SinglePhoneCall2> {
            return withContext(Dispatchers.IO) {
                // Simulate data loading (e.g., from Call Log)
                loadedCallHistoryList = loadCallHistory(0) // bring the rest
                setupRecyclerView(loadedCallHistoryList!!)
                return loadedCallHistoryList
            }
        }*/


    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerViewBack.visibility = View.GONE
        //bottomBar.visibility = View.GONE
        topBar.visibility = View.GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerViewBack.visibility = View.VISIBLE
        //bottomBar.visibility = View.VISIBLE
        topBar.visibility = View.VISIBLE
    }

    private fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber === null || phoneNumber == "") return context.getString(R.string.unknown_caller)

        if (PermissionsStatus.readContactsPermissionGranted.value !== null && PermissionsStatus.readContactsPermissionGranted.value == true) {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME))
                }
            }
        }

        return phoneNumber
    }

  /*  private fun makeCall(callPhoneNumber: String, isVideoCall: Boolean) {
        //   if (checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED) {
        //val uri = "tel:${+97237537900}".toUri()

        val uri = "tel:${callPhoneNumber}".toUri()

        if (PermissionsStatus.callPhonePermissionGranted.value !== null && PermissionsStatus.callPhonePermissionGranted.value!!) {
            if (PermissionsStatus.defaultDialerPermissionGranted.value !== null && PermissionsStatus.defaultDialerPermissionGranted.value!!) {
                // We have permissions - make the call:
                OutgoingCall.isCalling = true
                var callIntent = Intent(Intent.ACTION_CALL, uri)
                if (isVideoCall) {
                    *//*                val callIntent = Intent("com.android.phone.videocall", uri)
                                    if (callIntent.resolveActivity(requireContext().packageManager) != null) {
                                        ContextCompat.startActivity(requireContext(), callIntent, null)
                                    } else {
                                        Toast.makeText(requireContext(), "Video call not supported on this device", Toast.LENGTH_SHORT).show()
                                    }*//*

                    val callIntent = Intent(Intent.ACTION_CALL, uri)
                    callIntent.putExtra(
                        "android.telecom.extra.START_CALL_WITH_VIDEO_STATE",
                        3
                    ) // Video call
                    ContextCompat.startActivity(requireContext(), callIntent, null)

                    // callIntent = Intent("com.android.phone.videocall", uri)
                }
                ContextCompat.startActivity(requireContext(), callIntent, null)
            } else { // We can make a call - but it will get out through the default dialer app, which is not us in this case:
                val overlayFragment = CallPermissionMissingDialog()
                val args = Bundle().apply {
                    putString("PHONE_NUMBER", callPhoneNumber)
                    putBoolean("IS_VIDEO", isVideoCall)
                }
                overlayFragment.arguments = args
                overlayFragment.show(parentFragmentManager, "CallPermissionMissingDialogTag")
            }
        } else { // We cannot make a call - ask for permissions:
            val overlayFragment = PermissionsAlertFragment()
            val args = Bundle().apply {
                putBoolean("IS_MAKE_CALL_PERMISSION", true)
            }
            overlayFragment.arguments = args
            overlayFragment.show(parentFragmentManager, "PermissionMissingAlertDialogTag")
        }


        //     } else {
        //        requestPermissions(this, arrayOf(CALL_PHONE), REQUEST_PERMISSION)
        //      }
    }*/

    private fun addEditContact(isNew: Boolean) {
        val bundle = Bundle().apply {
            putString("phone_number", phoneNumberToCall)
            putString("contact_name", if (isNew) "" else callsContact)
        }
        val navController = findNavController()
        navController.navigate(R.id.action_recent_calls_to_editContact, bundle)
    }

    private fun showPermissionsConfirmationDialog(
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit
    ) {
        val builder = AlertDialog.Builder(this.requireContext())

        builder.setTitle(msgTitle)
        builder.setMessage(msgText)

        // Set buttons without listeners initially
        builder.setPositiveButton(fragmentRoot.context.getString(R.string.ask_permission_capital_a), null)
        builder.setNegativeButton(fragmentRoot.context.getString(R.string.cancel_capital)) { dialogInterface, which ->
            dialogInterface.dismiss()
        }

// Create the dialog
        val dialog = builder.create()

// Set a custom OnShowListener to handle button clicks
        dialog.setOnShowListener {
            // Handle Positive Button Click
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                // Dismiss the original dialog first
                dialog.dismiss()

                // Then, execute the permission request or show another dialog
                onAskPermission()
            }

            // Handle Negative Button Click (Optional: Already handled above)
            val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            negativeButton.setOnClickListener {
                dialog.dismiss()
            }
        }

// Show the dialog
        dialog.show()
    }

    private fun requestCallsViewPermission() {
        requestPermissionLauncher.launch(READ_CALL_LOG)
    }

    private fun requestReadContactsPermission() {
        requestPermissionLauncher.launch(READ_CONTACTS)
    }

    private fun formatTimestamp(timestamp: Long): String {
        // יצירת אובייקט של Calendar שמייצג את התאריך הנוכחי
        val now = Calendar.getInstance()

        // יצירת אובייקט Calendar מה-timestamp שהתקבל
        val callTime = Calendar.getInstance()
        callTime.timeInMillis = timestamp

        // חישוב הזמן שעבר מאז התאריך
        val diffInMillis = now.timeInMillis - callTime.timeInMillis

        // We always return date and time (and not strings for short periods) to avoid language confusion:
        val dateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // פורמט לדוגמה
        return dateFormat.format(callTime.time) // החזרת התאריך והשעה בפורמט לוקאלי

        // אם עברו פחות מ-24 שעות
/*        return if (diffInMillis < DateUtils.DAY_IN_MILLIS) {
            when {
                diffInMillis < DateUtils.HOUR_IN_MILLIS -> {
                    val minutes = (diffInMillis / DateUtils.MINUTE_IN_MILLIS).toInt()
                    val singularPluralFormat = if (minutes == 1) "" else "s"
                    "$minutes minute$singularPluralFormat ago" // החזרת הזמן בצורת "x minutes ago"
                }

                else -> {
                    val hours = (diffInMillis / DateUtils.HOUR_IN_MILLIS).toInt()
                    val isSingular = hours == 1
                    val singularPluralFormat = if (isSingular) "" else "s"
                    "$hours hour$singularPluralFormat ago" // החזרת הזמן בצורת "x hours ago"
                }
            }
        } else {
            // אם עברו יותר מ-24 שעות, הצגת תאריך ושעה בפורמט לוקאלי
            val dateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // פורמט לדוגמה
            dateFormat.format(callTime.time) // החזרת התאריך והשעה בפורמט לוקאלי
        }*/
    }
}

data class SinglePhoneCall(
    val callDate: String,
    val type: String
)