package com.nirotem.simplecall.ui.callReportScreen

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.TELECOM_SERVICE
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.statuses.OpenScreensStatus
import androidx.lifecycle.lifecycleScope
import com.example.callsreportslibrary.CallReportSettingsFragment
import com.nirotem.simplecall.databinding.FragmentCallReportBinding
import com.nirotem.simplecall.helpers.DBHelper.saveContactsForCallWithoutPermissions
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import com.nirotem.simplecall.statuses.SettingsStatus

class CallReportFragment : Fragment() {

    private var _binding: FragmentCallReportBinding? = null
    private var askingReadContactsPermission = false

    // Declare the permission request launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var fragmentRoot: View
    private lateinit var roleLauncher: ActivityResultLauncher<Intent>
    // private lateinit var roleRequestLauncher: ActivityResultLauncher<Intent>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var askingDefaultDialerPermission = false
    private var askingForMakingMakingCallPermission = false
    private var askingViewCallsPermission = false

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // Initialize the permission request launcher
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (askingReadContactsPermission) { // from now on we have making call permission
                    askingReadContactsPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
                }
                showCustomToastDialog(context, getString(R.string.permission_was_granted))
            } else {
                if (askingReadContactsPermission) {
                    askingReadContactsPermission = false
                    //showReadContactsPermissionsExplanationDialog(context)
                    suggestManualPermissionGrant(context)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        OpenScreensStatus.isCallReportScreenOpened = true
        if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
            OpenScreensStatus.shouldCloseSettingsScreens.value = OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
        }
        _binding = FragmentCallReportBinding.inflate(inflater, container, false)
        val root: View = binding.root
        fragmentRoot = root
        val context = requireContext()

       /* val lastSentDate = loadCallsReportLastSentDate(context)
        val nextSendDate = lastSentDate.add(daysDiffBetweenEachReport)
        val lastSendContact = loadCallsReportLastSentContact(context)
        val nextSendContact = loadCallsReportLastSentDate(context)
        val screenLastSentText =
            root.findViewById<Button>(R.id.screenLastSentText)
        val screenNextSendText =
            root.findViewById<Button>(R.id.screenNextSendText)

        screenLastSentText.text = "Last report was sent on $lastSentDate to $lastSendContact"
        screenNextSendText.text = "Next report will be sent on $nextSendDate to $nextSendContact"*/



        val openSettingsButton =
            root.findViewById<Button>(R.id.openSettingsButton)
        openSettingsButton.setOnClickListener {
            try {
                openCallReportSettingsWindow()
            }
            catch (e: Exception) {

            }
            /*if (!ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    READ_CONTACTS
                )
            ) {
                showReadContactsPermissionsExplanationDialog(context)
            } else {
                askingReadContactsPermission = true
                showPermissionsConfirmationDialog(
                    context.getString(R.string.permission_needed_capital_p),
                    context.getString(R.string.in_order_to_read_contacts_details_app_must_have_the_proper_permission),
                    ::requestReadContactPermission
                )
            }*/
        }

        val approvePhoneLogPermissionButton =
            root.findViewById<Button>(R.id.approvePhoneLogPermission)
        approvePhoneLogPermissionButton.setOnClickListener {
            try {
                askingViewCallsPermission = true
                showPermissionsConfirmationDialog(
                    context.getString(R.string.permission_needed_capital_p),
                    getString(R.string.in_order_to_view_calls_history_the_application_must_have_the_proper_permission),
                    ::requestCallsViewPermission
                )
            }
            catch (e: Exception) {
               // showCallLogPermissionsExplanationDialog(context)
                suggestManualPermissionGrant(context)
               // showCallsLogPermissionsExplanationDialog()
            }

           /* if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, READ_CALL_LOG)
            ) {
                showCallsLogPermissionsExplanationDialog()
            } else {
                askingViewCallsPermission = true
                showPermissionsConfirmationDialog(
                    context.getString(R.string.permission_needed_capital_p),
                    getString(R.string.in_order_to_view_calls_history_the_application_must_have_the_proper_permission),
                    ::requestCallsViewPermission
                )
            }*/
        }

        // Check if should automatically ask for specific permission
        val openCallPermissionAutomatically = arguments?.getBoolean("OPEN_CALL_PERMISSION", false)
        if (openCallPermissionAutomatically == true) {
            askForCallPhonePermission(context)
        }

        OpenScreensStatus.shouldClosePermissionsScreens.observe(viewLifecycleOwner) { currInstance ->
            if (currInstance != null && currInstance > OpenScreensStatus.registerPermissionsInstanceValue) {
                parentFragmentManager.popBackStack()
            }
        }

        return root
    }

    /*
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            val shouldOpenCallPermission = arguments?.getString("OPEN_CALL_PERMISSION").toString()
            askForCallPhonePermission()
        }
    */

    private fun askForCallPhonePermission(context: Context) {
        /*        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        CALL_PHONE
                    )
                ) {
                    showCallPhonePermissionsExplanationDialog()
                } else {
                    askingForMakingMakingCallPermission = true
                    showPermissionsConfirmationDialog(
                        requireContext().getString(R.string.permission_needed_capital_p),
                        getString(R.string.in_order_to_make_calls_the_application_must_have_the_proper_permission),
                        ::requestCallPhonePermission
                    )
                }*/

        try {
            askingForMakingMakingCallPermission = true
            showPermissionsConfirmationDialog(
                getString(R.string.permission_needed_capital_p),
                getString(R.string.in_order_to_make_calls_the_application_must_have_the_proper_permission),
                ::requestCallPhonePermission
            )
        } catch (e: Exception) {
            suggestManualPermissionGrant(context)
            //showMakePhoneCallPermissionsExplanationDialog(context)
        }

    }

    private fun openCallReportSettingsWindow() {
       val overlayFragment = CallReportSettingsFragment()
       overlayFragment.show(parentFragmentManager, "allReportSettingsWindowDialogFragmentTag")
        // waitingCallOverlayFragment = overlayFragment
        /*        val args = Bundle().apply {
                    putBoolean("IS_OUT_GOING", isOutgoingCall)
                }
                overlayFragment.arguments = args*/

    }

    override fun onDestroyView() {
        super.onDestroyView()
        OpenScreensStatus.isCallReportScreenOpened = false
        _binding = null
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
        }

        builder.setNegativeButton(context.getString(R.string.cancel_capital)) { dialog, which ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }

    private fun requestCallPhonePermission() {
        requestPermissionLauncher.launch(CALL_PHONE)
    }

    private fun requestReadContactPermission() {
        requestPermissionLauncher.launch(READ_CONTACTS)
    }

    private fun requestCallsViewPermission() {
        requestPermissionLauncher.launch(READ_CALL_LOG)
    }

    override fun onResume() {
        super.onResume()

        var permissionsGrantedBecauseOfDefaultDialer = false

        if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
            OpenScreensStatus.shouldCloseSettingsScreens.value = OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
        }

        if (OpenScreensStatus.shouldClosePermissionsScreens.value != null) {
            OpenScreensStatus.shouldClosePermissionsScreens.value = OpenScreensStatus.shouldClosePermissionsScreens.value!! + 1
        }

        if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
            OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
        }

        val context = requireContext()
        if (PermissionsStatus.defaultDialerPermissionGranted.value === null || (!(PermissionsStatus.defaultDialerPermissionGranted.value!!))) {
            val telecomManager =
                requireContext().getSystemService(TELECOM_SERVICE) as TelecomManager
            val isDefaultDialerGranted =
                telecomManager.defaultDialerPackage == requireContext().packageName
            val wasAppDefaultDialerBefore =
                PermissionsStatus.defaultDialerPermissionGranted.value !== null && PermissionsStatus.defaultDialerPermissionGranted.value == true
            if (isDefaultDialerGranted && !wasAppDefaultDialerBefore) {
                PermissionsStatus.defaultDialerPermissionGranted.value = true
                // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
                // we may not have another chance to save the contacts before the app would run from InCallService
                saveContactsForCallWithoutPermissions(binding.root.context, lifecycleScope)

                permissionsGrantedBecauseOfDefaultDialer = true

                if (SettingsStatus.currLanguage.value == LanguagesEnum.ENGLISH || SettingsStatus.currLanguage.value == LanguagesEnum.HEBREW) {
/*                    Toast.makeText(
                        context,
                        getString(R.string.app_selected_as_default_phone_app), Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.app_selected_as_default_phone_app))
                }
                else { // Longer text, does not fit well within Toast
                    val toastMsg =
                        getString(R.string.app_selected_as_default_phone_app)
                   // Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                    showLongSnackBar(requireActivity(), toastMsg, 8000)
                }
            } else if (!isDefaultDialerGranted && askingDefaultDialerPermission) {
                val toastMsg =
                    getString(R.string.default_dialer_permission_was_not_granted_but_needed)
                //Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                showLongSnackBar(requireActivity(), toastMsg, 8000)
            }
            askingDefaultDialerPermission = false
        }

        if (PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    CALL_PHONE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.callPhonePermissionGranted.value = true

                if (!permissionsGrantedBecauseOfDefaultDialer) {
/*                    Toast.makeText(
                        context,
                        getString(R.string.make_calls_permission_was_granted), Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.make_calls_permission_was_granted))
                }
            } else if (askingForMakingMakingCallPermission) {
                val toastMsg =
                    getString(R.string.make_calls_permission_was_denied_but_needed)
               // Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                showLongSnackBar(requireActivity(), toastMsg, 8000)
                // Permission is still denied
                //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            }
            askingForMakingMakingCallPermission = false
        }

        if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readContactsPermissionGranted.value = true

                // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
                // we may not have another chance to save the contacts before the app would run from InCallService
                saveContactsForCallWithoutPermissions(binding.root.context, lifecycleScope)


                if (!permissionsGrantedBecauseOfDefaultDialer) {
/*                    Toast.makeText(
                        context,
                        getString(R.string.read_contacts_permission_was_granted), Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, getString(R.string.read_contacts_permission_was_granted))
                }

            } else if (askingReadContactsPermission) {
                val toastMsg =
                    getString(R.string.read_contacts_permission_was_denied_but_needed)
             //   Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                showLongSnackBar(requireActivity(), toastMsg, 8000)
                // Permission is still denied
                //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            }
            askingReadContactsPermission = false
        }
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
}