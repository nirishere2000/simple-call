package com.nirotem.simplecall.ui.callsHistory

import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.content.ContentResolver
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Button
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
import com.nirotem.simplecall.R
import com.nirotem.simplecall.adapters.CallHistoryAdapter
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.singleCallHistory.SingleCallHistoryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class CallsHistoryFragment : Fragment(R.layout.fragment_calls_history) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var callHistoryAdapter: CallHistoryAdapter
    private lateinit var progressBar: ProgressBar
    private lateinit var noCallsAvailableMsg: TextView
    private lateinit var noPermissionsContainer: LinearLayout
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private var askingViewCallsPermission = false
    private var askingReadContactPermission = false
    private lateinit var fragmentRoot: View
    private var totalItemCount: Int = 0
    private var alreadyShownContactsExplaination = false
    private var isLoading = false
    private var currentOffset = 0
    private val currentLimit = 100
    private lateinit var navController: NavController
    private var currentShownAlertDialog: AlertDialog? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (currentShownAlertDialog != null) {
                currentShownAlertDialog!!.dismiss()
                currentShownAlertDialog = null
            }
            if (isGranted) {
                if (askingViewCallsPermission) { // from now on we have making call permission
                    askingViewCallsPermission = false
                    PermissionsStatus.readCallLogPermissionGranted.value = true
                    noPermissionsContainer.visibility = GONE
/*                    Toast.makeText(
                        context,
                        getString(R.string.permission_was_granted_reloading),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.permission_was_granted_reloading))
                    initView()
                } else if (askingReadContactPermission) { // Read Contacts permission was granted
                    askingReadContactPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
/*                    Toast.makeText(
                        context,
                        getString(R.string.permission_was_granted_reloading),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.permission_was_granted_reloading))
                    initView() // loadFragment(fragmentRoot)
                }
            } else {
                if (askingViewCallsPermission) {
                    askingViewCallsPermission = false
                    //showCallLogPermissionsExplanationDialog(context)
                    suggestManualPermissionGrant(context)
                } else if (askingReadContactPermission) {
                    askingReadContactPermission = false
                    alreadyShownContactsExplaination = true // don't allow anymore requests for this intstance
                    suggestManualPermissionGrant(context)
                    //showReadContactsPermissionsExplanationDialog(context)
                }
            }
              /*  var toastMsg = "Cannot continue since permission was not approved"
                askingViewCallsPermission = false
                if (askingReadContactPermission) { // Read Contacts permission was granted
                    askingReadContactPermission = false
                    toastMsg =
                        "Contacts permission was not granted. Showing calls with phone numbers only."
                }
                Snackbar.make(fragmentRoot, toastMsg, 8000).show()*/
                /*                Toast.makeText(
                                    this.requireContext(),
                                    toastMsg,
                                    Toast.LENGTH_SHORT
                                ).show()*/
           // }
        }
    }

    override fun onStop() {
        super.onStop()
        //OpenScreensStatus.shouldCloseTopMenuScreens.value = false
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

        if (PermissionsStatus.readCallLogPermissionGranted.value === null || (!(PermissionsStatus.readCallLogPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readCallLogPermissionGranted.value = true
                noPermissionsContainer.visibility = GONE
                if (askingViewCallsPermission) {
/*                    Toast.makeText(
                        context,
                        getString(R.string.view_calls_permission_was_granted_reloading),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(requireContext(), getString(R.string.view_calls_permission_was_granted_reloading))
                }
                askingViewCallsPermission = false
                //initView() - we do initView anyway
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }
        if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readContactsPermissionGranted.value = true
                if (askingReadContactPermission) {
/*                    Toast.makeText(
                        context,
                        getString(R.string.read_contacts_permission_was_granted_reloading),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(requireContext(), getString(R.string.read_contacts_permission_was_granted_reloading))
                }
                askingReadContactPermission = false
               // initView()
            } //else {
            // Permission is still denied
            //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            //  }
        }


        if (PermissionsStatus.callPhonePermissionGranted.value != true) {
            PermissionsStatus.callPhonePermissionGranted.value = ContextCompat.checkSelfPermission(
                fragmentRoot.context,
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (PermissionsStatus.defaultDialerPermissionGranted.value != true) {
            val telecomManager = fragmentRoot.context.getSystemService(TELECOM_SERVICE) as TelecomManager
            PermissionsStatus.defaultDialerPermissionGranted.value =
                telecomManager.defaultDialerPackage == fragmentRoot.context.packageName
        }

        // We do initView any way because calls list could have changed
        initView()
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

        fragmentRoot = view
        try {
            navController = findNavController()


            PermissionsStatus.defaultDialerPermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
                if (permissionGranted) {
                    initView()
                }
            }

            // Response to Allow outgoing mode changes:
            SettingsStatus.userAllowOutgoingCallsEnum.observe(viewLifecycleOwner) { newUserAllowOutgoingCallsEnum ->
                initView()
            }


            //if (!missingCriticalPermissions) { // otherwise we will load the Permissions screen anyway

            //}

            /*            if (missingCriticalPermissions) {
                            val navController = findNavController()
                            navController.navigate(R.id.action_load_permissions_form)
                        }*/
        } catch (error: Error) {
            Log.e(
                "SimplyCall - CallsHistoryFragment",
                "CallsHistoryFragment onViewCreated Error ($error)"
            )
        }


        // Set adapter for RecyclerView
        // callHistoryAdapter = CallHistoryAdapter(loadCallHistory())
        // recyclerView.adapter = callHistoryAdapter


    }

    private fun initView() {
        if (PermissionsStatus.readCallLogPermissionGranted.value !== null && PermissionsStatus.readCallLogPermissionGranted.value!!) {
            noPermissionsContainer = fragmentRoot.findViewById(R.id.noPermissionsContainer)
            noPermissionsContainer.visibility = View.GONE
            loadFragment(fragmentRoot)
            if (PermissionsStatus.callPhonePermissionGranted.value != true) {
                PermissionsStatus.callPhonePermissionGranted.observe(viewLifecycleOwner) { permissionGranted ->
                    if (permissionGranted) {
                        loadFragment(fragmentRoot)
                    }
                }
            }
            if ((PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) && (!alreadyShownContactsExplaination) && (!askingReadContactPermission)) {
                // if no Contact Read permission - we still show the screen with phone number and also show a Request Permission dialog
                try {
                    askingReadContactPermission = true
                    showPermissionsConfirmationDialog(
                        fragmentRoot.context.getString(R.string.permission_needed_capital_p),
                        getString(R.string.to_display_the_calls_contacts_application_must_have_permission),
                        ::requestReadContactsPermission
                    )
                }
                catch (e: Exception) {
                    if (!alreadyShownContactsExplaination) {
                        alreadyShownContactsExplaination = true
                        val toastMsg =
                            getString(R.string.contacts_permission_was_not_granted_showing_calls_list_with_phone_numbers_only)
                        /*                            Toast.makeText(
                                                        this.requireContext(),
                                                        toastMsg,
                                                        Toast.LENGTH_LONG
                                                    ).show()*/
                        //Snackbar.make(fragmentRoot, toastMsg, 8000).show()

                        showLongSnackBar(requireActivity(), toastMsg, 8000)
                    }
                }
            }
        } else {
            progressBar = fragmentRoot.findViewById(R.id.progressBar)
            progressBar.visibility = View.GONE
            noPermissionsContainer = fragmentRoot.findViewById(R.id.noPermissionsContainer)
            noPermissionsContainer.visibility = View.VISIBLE
            val callsHistoryApproveViewCallsPermissionButton =
                fragmentRoot.findViewById<Button>(R.id.callsHistoryApproveViewCallsPermission)
            callsHistoryApproveViewCallsPermissionButton.setOnClickListener {
                try {
                    askingViewCallsPermission = true
                    showPermissionsConfirmationDialog(
                        fragmentRoot.context.getString(R.string.permission_needed_capital_p),
                        getString(R.string.to_view_calls_history_application_must_have_permission),
                        ::requestCallsViewPermission
                    )
                }
                catch (e: Exception) {
                    //showPermissionsExplanationDialog()
                   // showCallLogPermissionsExplanationDialog(fragmentRoot.context)

                    suggestManualPermissionGrant(fragmentRoot.context)
                }

            }
        }
    }


    private fun loadFragment(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        progressBar = view.findViewById(R.id.progressBar)
        noCallsAvailableMsg = view.findViewById(R.id.noCallsAvailableMsg)
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
       // progressBar = fragmentRoot.findViewById(R.id.progressBar)
        val sharedViewModel: SingleCallHistoryViewModel by activityViewModels()
        callHistoryAdapter = CallHistoryAdapter(
            requireContext(), navController, sharedViewModel, parentFragmentManager,
            viewLifecycleOwner, requireActivity(), requestPermissionLauncher
        )
        recyclerView.adapter = callHistoryAdapter
        setupPagedRecyclerView(recyclerView, callHistoryAdapter)
    }

    /*
        private fun setupRecyclerView(callHistoryList: List<PhoneCall2>) {
            callHistoryAdapter = CallHistoryAdapter(this.context)
            recyclerView.adapter = callHistoryAdapter
            setupPagedRecyclerView(recyclerView, callHistoryAdapter)  // no limit bring the rest async
        }
    */

    private fun setupPagedRecyclerView(recyclerView: RecyclerView, adapter: CallHistoryAdapter) {
        //val existingRecordsSize = if (loadedCallHistoryList.size > 0) loadedCallHistoryList?.size else 0
        //if (loadedCallHistoryList.size <= 0) { // first time - init
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                try {
                    var newRecords: List<PhoneCall2>? = null
                    lifecycleScope.launch {
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        val visibleItemCount = layoutManager.itemCount
                        val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                        if (dy > 0 && !isLoading && (currentOffset + currentLimit <= totalItemCount) && ((lastVisibleItem + 1) >= visibleItemCount)) {
                            //   loadMoreCallHistory(adapter)

                            // User scrolled down
                            //  Log.d("ScrollDirection", "Scrolled down")
                            isLoading = true
                            // progressBar = fragmentRoot.findViewById(R.id.progressBar)
                            showLoading()

                            newRecords =
                                loadMoreHistoryFromCursor(currentOffset, limit = currentLimit)

                            val safeRecords = newRecords // copy the current reference
                            if (safeRecords != null && safeRecords.isNotEmpty()) {
                                adapter.addItems(safeRecords)
                            }
/*
                            if (newRecords.isNotEmpty()) {
                                adapter.addItems(newRecords)
                            }*/
                            isLoading = false
                            // progressBar = fragmentRoot.findViewById(R.id.progressBar)
                            hideLoading()
                            //loadMoreHistoryFromCursor(currentOffset)
                        } else if (dy < 0) {
                            // User scrolled "back" up
                            //  newRecords = loadMoreHistoryFromCursor(currentOffset - (currentLimit * 2), limit = currentLimit)

                            //Log.d("ScrollDirection", "Scrolled up")
                        }

                        // loadedCallHistoryList = listOf(loadedCallHistoryList, newRecords) // Immutable List
                        //loadedCallHistoryList.addAll(newRecords)


                    }

                } catch (err: Exception) {
                    isLoading = false
                    // progressBar = fragmentRoot.findViewById(R.id.progressBar)
                    hideLoading()
                    Log.e("Simply Call - Calls History Fragment", "Error onScrolled", err)
                }


                // Update the lastScrollY if needed for further logic
                //    currentOffset += dy


                /*                 val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                                 val totalItemCount = layoutManager.itemCount
                                 val lastVisibleItem = layoutManager.findLastVisibleItemPosition()

                                 if (!isLoading && totalItemCount >= limit && totalItemCount <= (lastVisibleItem + limit / 2)) {
                                  //   loadMoreCallHistory(adapter)
                                 }*/
            }
        })

        loadMoreCallHistory(adapter) // Load initial data
        //}
        //   else {
        //currentOffset = existingRecordsSize
        // isLoading = false
        // }
    }

    private fun loadMoreCallHistory(adapter: CallHistoryAdapter) {
        isLoading = true
        val context = this.requireContext()
        // progressBar = fragmentRoot.findViewById(R.id.progressBar)
        showLoading()
        try {
            lifecycleScope.launch {
                val newRecords = loadCallHistoryAsync(offset = currentOffset, limit = currentLimit)
                // loadedCallHistoryList = listOf(loadedCallHistoryList, newRecords) // Immutable List
                //loadedCallHistoryList.addAll(newRecords)

                adapter.addItems(newRecords)
                noCallsAvailableMsg.visibility = if (adapter.itemCount > 0) GONE else VISIBLE
                if (adapter.itemCount > 0 && !OpenScreensStatus.alreadyShownCallsClickOnRowForMore && !OpenScreensStatus.isHelpScreenOpened) {
                    OpenScreensStatus.alreadyShownCallsClickOnRowForMore = true
/*                    Toast.makeText(
                        context,
                        getString(R.string.tap_on_call_item_for_more),
                        Toast.LENGTH_LONG
                    ).show()*/

                    showCustomToastDialog(context, getString(R.string.tap_on_call_item_for_more))
                }
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

    private suspend fun loadCallHistoryAsync(offset: Int = 0, limit: Int = 100): List<PhoneCall2> {
        return withContext(Dispatchers.IO) {
            // Simulate data loading (e.g., from Call Log)
            loadCallHistoryAsyncPaged(offset, limit)
            //  loadCallHistory()
        }
    }

    private suspend fun loadMoreHistoryFromCursor(
        newPosition: Int = 0,
        limit: Int = 100
    ): List<PhoneCall2> {
        return withContext(Dispatchers.IO) {
            // Simulate data loading (e.g., from Call Log)
            loadMoreHistoryFromCursorAsync(newPosition, limit)
            //  loadCallHistory()
        }
    }

    private fun loadMoreHistoryFromCursorAsync(
        newPosition: Int = 0,
        limit: Int = 100
    ): List<PhoneCall2> {
        val context = this.context ?: return emptyList()
        val callHistoryList = mutableListOf<PhoneCall2>()

        val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val queryArgs = Bundle().apply {
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                //putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                putStringArray(
                    ContentResolver.QUERY_ARG_SORT_COLUMNS,
                    arrayOf(CallLog.Calls.DATE)
                )
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
            }
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                queryArgs,
                null
            )

            /*  val cr: ContentResolver = context.contentResolver
              cr.query(CallLog.Calls.CONTENT_URI,
                  arrayOf(CallLog.Calls.NUMBER,CallLog.Calls.DATE, CallLog.Calls.TYPE),
                  null, null, CallLog.Calls.DATE + " DESC LIMIT 100");*/

            /*                cr.query(
                                CallLog.Calls.CONTENT_URI,
                                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                                null,
                                null,
                                "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
                            )*/

            /*context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
            )*/
        } else {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT $limit" // OFFSET $offset
            )
        }

        cursor?.use {
            // Move to the offset position
            if (it.moveToPosition(newPosition)) { // Index is zero-based, so 100th position = 99
                currentOffset = newPosition
                var count = 0
                while (it.moveToNext() && count < limit) {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                    if (numberIndex != -1 && dateIndex != -1 && typeIndex != -1) {
                        var originalNumber = it.getString(numberIndex)
                        val contactName = getContactName(context, originalNumber)
                        val isPhoneInContacts = contactName != null
                        var number = contactName ?: originalNumber ?: context.getString(R.string.unknown_caller)
                        if (number.isEmpty()) {
                            number = context.getString(R.string.unknown_caller)
                        }
                        val date = formatTimestamp(it.getLong(dateIndex))
                        val callType = when (it.getInt(typeIndex)) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            CallLog.Calls.REJECTED_TYPE -> "Rejected"
                            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                            else -> "Unknown"
                        }

                        val call = PhoneCall2(originalNumber ?: "", number, date, callType, isPhoneInContacts)
                        callHistoryList.add(call)
                        count++
                        currentOffset++
                    }
                }
                //   Log.d("CursorData", "Number: $number, Date: $date, Type: $type")
            } else {
                // Log.e("CursorError", "Could not move to position 100. Cursor count: ${it.count}")
            }
        }

        return callHistoryList
    }

    private fun loadCallHistoryAsyncPaged(offset: Int = 0, limit: Int = 100): List<PhoneCall2> {
        val context = this.context ?: return emptyList()
        val callHistoryList = mutableListOf<PhoneCall2>()

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
                }
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                    queryArgs,
                    null
                )

                /*  val cr: ContentResolver = context.contentResolver
                  cr.query(CallLog.Calls.CONTENT_URI,
                      arrayOf(CallLog.Calls.NUMBER,CallLog.Calls.DATE, CallLog.Calls.TYPE),
                      null, null, CallLog.Calls.DATE + " DESC LIMIT 100");*/

                /*                cr.query(
                                    CallLog.Calls.CONTENT_URI,
                                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                                    null,
                                    null,
                                    "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
                                )*/

                /*context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
                )*/
            } else {
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC LIMIT $limit OFFSET $offset"
                )
            }

            totalItemCount = cursor?.count ?: 0
            Log.d("SimplyCall - CallHistoryFragment", "Calls records count = ${cursor?.count}")

            cursor?.use {
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                if (numberIndex != -1 && dateIndex != -1 && typeIndex != -1) {
                    var count = 0
                    while (it.moveToNext() && count < limit) {
                        val originalNumber = it.getString(numberIndex)
                        val contactName = getContactName(context, originalNumber)
                        val isPhoneInContacts = contactName != null
                        var number = contactName ?: originalNumber ?: ""
                        if (number.isEmpty()) {
                            number = context.getString(R.string.unknown_caller)
                        }
                        val date = formatTimestamp(it.getLong(dateIndex))
                        val intTypeIndex = it.getInt(typeIndex)
                        val callType = when (intTypeIndex) {
                            CallLog.Calls.INCOMING_TYPE -> "Incoming"
                            CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                            CallLog.Calls.MISSED_TYPE -> "Missed"
                            CallLog.Calls.REJECTED_TYPE -> "Rejected"
                            CallLog.Calls.BLOCKED_TYPE -> "Blocked"
                            else -> "Unknown"
                        }

                        if (callType == "Unknown") {
                            Log.d("SingleCallHistory", "Unknown - intTypeIndex = $intTypeIndex")
                            intTypeIndex
                        }

                        val call = PhoneCall2(originalNumber ?: "", number, date, callType, isPhoneInContacts)
                        callHistoryList.add(call)
                        count++
                    }


                    // copy to All Records list - after UI already loaded
                    /* cursor.moveToFirst()
                     while (it.moveToNext()) {
                         val originalNumber = it.getString(numberIndex)
                         val number = getContactName(context, originalNumber) ?: originalNumber ?: ""
                         val date = formatTimestamp(it.getLong(dateIndex))
                         val callType = when (it.getInt(typeIndex)) {
                             CallLog.Calls.INCOMING_TYPE -> "Incoming"
                             CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                             CallLog.Calls.MISSED_TYPE -> "Missed"
                             else -> "Unknown"
                         }

                         val call = PhoneCall2(originalNumber ?: "", number, date, callType)
                         allRecordsCallHistoryList.add(call)
                     }*/
                }
            }
        } catch (e: Exception) {
            Log.e("SimplyCall - CallHistoryFragment", "Error loading call history", e)
        }

        return callHistoryList
    }

    /*    private suspend fun loadRestOfHistoryAsync(): List<PhoneCall2> {
            return withContext(Dispatchers.IO) {
                // Simulate data loading (e.g., from Call Log)
                loadedCallHistoryList = loadCallHistory(0) // bring the rest
                setupRecyclerView(loadedCallHistoryList!!)
                return loadedCallHistoryList
            }
        }*/


    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
       // noCallsAvailableMsg.visibility = GONE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
     //   noCallsAvailableMsg.visibility = if (recyclerView.adapter != null && recyclerView.adapter!!.itemCount > 0) GONE else VISIBLE
    }

    private fun loadAllHistory(limit: Int = 100): Cursor? {
        val context = this.context ?: return null
        val callHistoryList = mutableListOf<PhoneCall2>()

        try {
            val cursor: Cursor? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // גרסאות 11 ומעלה: שימוש ב-Bundle
                val queryArgs = Bundle().apply {
                    // if (limit > 0) {
                    //  putInt(ContentResolver.QUERY_ARG_LIMIT, limit) // מגבלה של X רשומות
                    //   }

                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(CallLog.Calls.DATE)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }

                val bundle = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 100)
                }


                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                    bundle,
                    null
                )
            } else {
                // גרסאות מוקדמות יותר: מיון ומגבלה ידנית בלולאה
                context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
            }

            Log.d("SimplyCall - CallHistoryFragment", "Calls records count = ${cursor?.count}")

            return cursor

            /*            cursor?.use {
                            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                            if (numberIndex != -1 && dateIndex != -1 && typeIndex != -1) {
                                var count = 0
                                while (it.moveToNext()) {
                                    val originalNumber = it.getString(numberIndex)
                                    val number = getContactName(context, originalNumber) ?: originalNumber ?: ""
                                    val date = formatTimestamp(it.getLong(dateIndex))
                                    val callType = when (it.getInt(typeIndex)) {
                                        CallLog.Calls.INCOMING_TYPE -> "Incoming"
                                        CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                                        CallLog.Calls.MISSED_TYPE -> "Missed"
                                        else -> "Unknown"
                                    }

                                    val call = PhoneCall2(originalNumber ?: "", number, date, callType)
                                    callHistoryList.add(call)
                                    count++
                                }
                            } else {
                                Log.e("SimplyCall - CallHistoryFragment", "Columns not found")
                            }
                        }*/
        } catch (e: Exception) {
            Log.e("SimplyCall - CallHistoryFragment", "Error loading call history", e)
        }
        return null

        //return callHistoryList
    }

    /*    private fun loadCallHistory(): List<PhoneCall2> {
            var that = this.context
            val callHistoryList = mutableListOf<PhoneCall2>()

            try {
                val cursor: Cursor? = requireContext().contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE, CallLog.Calls.TYPE),
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )

                cursor?.let {
                    val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                    val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                    if (numberIndex != -1 && dateIndex != -1) {
                        while (it.moveToNext()) {
                            val originalNumber = it.getString(numberIndex)
                            val number = if (that !== null) getContactName(that, originalNumber) else originalNumber
                            val phoneOrContact = if (number !== null) number else ""
                            val phoneNumber = if (originalNumber !== null) originalNumber else ""

                            //  Log.e("SimplyCall - CallHistory", "phoneOrContact ($phoneOrContact)")

                            val longTypeDate = it.getLongOrNull(dateIndex)
                            val date = if (longTypeDate !== null) formatTimestamp(longTypeDate) else ""
                            val callType = it.getIntOrNull(typeIndex)

                            // Map the integer type to a human-readable string
                            val callTypeStr = when (callType) {
                                null -> ""
                                CallLog.Calls.INCOMING_TYPE -> "Incoming"
                                CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                                CallLog.Calls.MISSED_TYPE -> "Missed"
                                else -> "Unknown"
                            }
                            val call = PhoneCall2(phoneNumber, phoneOrContact, date, callTypeStr)
                            callHistoryList.add(call)
                        }
                    } else {
                        Log.e("CallHistory", "Column not found")
                    }
                    it.close()
                }
            }
            catch (error: Error) {
                Log.e("SimplyCall - CallsHistoryFragment", "CallsHistoryFragment loadCallHistory Error ($error)")
            }


            return callHistoryList
        }*/

    private fun getContactName(context: Context, phoneNumber: String?): String? {
        if (phoneNumber === null || phoneNumber == "") return null // context.getString(R.string.unknown_caller)

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

        return null
    }

    private fun showPermissionsConfirmationDialog(
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit
    ) {
        val context = this.requireContext()
        val builder = AlertDialog.Builder(context)

        builder.setTitle(msgTitle)
        builder.setMessage(msgText)

        builder.setPositiveButton(context.getString(R.string.ask_permission_capital_a)) { dialog, which ->
            onAskPermission()
            dialog.dismiss()
        }

        builder.setNegativeButton(fragmentRoot.context.getString(R.string.cancel_capital)) { dialog, which ->
            dialog.dismiss()
        }

        currentShownAlertDialog = builder.create()
        currentShownAlertDialog!!.show()
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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // פורמט לדוגמה
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
                    // "$hours hours ago" // החזרת הזמן בצורת "x hours ago"
                }
            }
        } else {
            // אם עברו יותר מ-24 שעות, הצגת תאריך ושעה בפורמט לוקאלי
            val dateFormat =
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // פורמט לדוגמה
            dateFormat.format(callTime.time) // החזרת התאריך והשעה בפורמט לוקאלי
        }*/
    }

/*    override fun onDestroy() {
        super.onDestroy()
        OpenScreensStatus.shouldCloseTopMenuScreens.value = false
    }*/
}

data class PhoneCall2(
    val phoneNumber: String,
    val contactOrphoneNumber: String,
    val callDate: String,
    val type: String,
    val isPhoneInContacts: Boolean
)
