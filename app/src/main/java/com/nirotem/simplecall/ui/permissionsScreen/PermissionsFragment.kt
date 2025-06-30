package com.nirotem.simplecall.ui.permissionsScreen

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.ROLE_SERVICE
import androidx.appcompat.app.AppCompatActivity.TELECOM_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentPermissionsBinding
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.isBackgroundWindowsAllowed
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.nirotem.sharedmodules.statuses.OemDetector
import com.nirotem.simplecall.helpers.DBHelper.saveContactsForCallWithoutPermissions
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.LanguagesEnum
import com.nirotem.simplecall.statuses.PermissionsStatus.suggestManualPermissionGrant
import com.nirotem.simplecall.statuses.SettingsStatus

class PermissionsFragment : Fragment() {

    private var _binding: FragmentPermissionsBinding? = null
    private var askingReadContactsPermission = false
    private val REQUEST_CODE_ROLE_DIALER = 1001

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
                if (askingForMakingMakingCallPermission) {
                    askingForMakingMakingCallPermission = false

                    PermissionsStatus.callPhonePermissionGranted.value = true
                    displayPermissionsGrantedViews(fragmentRoot)
                }
                if (askingReadContactsPermission) { // from now on we have making call permission
                    askingReadContactsPermission = false
                    PermissionsStatus.readContactsPermissionGranted.value = true
                    displayPermissionsGrantedViews(fragmentRoot)

                    // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
                    // we may not have another chance to save the contacts before the app would run from InCallService
                    saveContactsForCallWithoutPermissions(binding.root.context, lifecycleScope)
                }
                if (askingViewCallsPermission) {
                    askingViewCallsPermission = false
                    PermissionsStatus.readCallLogPermissionGranted.value = true
                    displayPermissionsGrantedViews(fragmentRoot)
                }
/*                Toast.makeText(
                    context,
                    getString(R.string.permission_was_granted),
                    Toast.LENGTH_LONG
                ).show()*/
                showCustomToastDialog(context, getString(R.string.permission_was_granted))
            } else {
                if (askingForMakingMakingCallPermission) {
                    askingForMakingMakingCallPermission = false
                    //showMakePhoneCallPermissionsExplanationDialog(context)
                    suggestManualPermissionGrant(context)
                }
                if (askingReadContactsPermission) {
                    askingReadContactsPermission = false
                    //showReadContactsPermissionsExplanationDialog(context)
                    suggestManualPermissionGrant(context)
                }
                if (askingViewCallsPermission) {
                    askingViewCallsPermission = false
                    //showCallLogPermissionsExplanationDialog(context)
                    suggestManualPermissionGrant(context)
                }
/*                Toast.makeText(
                    this.requireContext(),
                    "Permission was not approved but needed",
                    Toast.LENGTH_SHORT
                ).show()*/
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        OpenScreensStatus.isPermissionsScreenOpened = true
        if (OpenScreensStatus.shouldCloseSettingsScreens.value != null) {
            OpenScreensStatus.shouldCloseSettingsScreens.value = OpenScreensStatus.shouldCloseSettingsScreens.value!! + 1
        }
        if (OpenScreensStatus.shouldClosePremiumTourScreens.value != null) {
            OpenScreensStatus.shouldClosePremiumTourScreens.value = OpenScreensStatus.shouldClosePremiumTourScreens.value!! + 1
        }

        _binding = FragmentPermissionsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        fragmentRoot = root
        val context = requireContext()
        val activity = requireActivity()

        // הרשמת ה-Launcher
        roleLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    // בדיקה אם ההרשאה הושגה
                    //updatePermissionStatus()

                    // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
                    // we may not have another chance to save the contacts before the app would run from InCallService
                    saveContactsForCallWithoutPermissions(binding.root.context, lifecycleScope)

                } else {
                    // עדכון סטטוס ההרשאה במידה והפעולה נכשלה או בוטלה
                    //permissionsViewModel.setDefaultDialerPermission(false)
                }
            }

        displayPermissionsGrantedViews(root)

        val approveDefaultDialerPermission =
            root.findViewById<Button>(R.id.approveDefaultDialerPermission)
        approveDefaultDialerPermission.setOnClickListener {
            //askingDefaultDialerPermission = true // user goes outside app anyway
            requestRole(context) // Default Dialer is a bit different
        }

        val approveOverlayDrawPermission =
            root.findViewById<Button>(R.id.approveOverlayDrawPermission)
        approveOverlayDrawPermission.setOnClickListener {
            //  onDisplayPopupPermission()
            requestOverlayPermission()
            /*            AlertDialog.Builder(context)
                            .setMessage("The application needs this permission to pop up the screen for Incoming call.")
                            .setPositiveButton("Grant Permission") { _, _ ->
                                requestOverlayPermission()
                            }
                            .setNegativeButton(context.getString(R.string.cancel_capital), null)
                            .show()*/
        }

        val approveCallPhonePermission = root.findViewById<Button>(R.id.approveCallPhonePermission)
        approveCallPhonePermission.setOnClickListener {
            askForCallPhonePermission(context)
        }

        val approveReadContactsPermissionButton =
            root.findViewById<Button>(R.id.approveReadContactsPermission)
        approveReadContactsPermissionButton.setOnClickListener {
            try {
                askingReadContactsPermission = true
                showPermissionsConfirmationDialog(
                    context.getString(R.string.permission_needed_capital_p),
                    context.getString(R.string.in_order_to_read_contacts_details_app_must_have_the_proper_permission),
                    ::requestReadContactPermission
                )
            }
            catch (e: Exception) {
                //showReadContactsPermissionsExplanationDialog(context)
                suggestManualPermissionGrant(context)
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

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val context = requireContext()
            //    if (!Settings.canDrawOverlays(this)) {
            if ("xiaomi" == Build.MANUFACTURER.toLowerCase(Locale.ROOT)) {
                val intent = Intent("miui.intent.action.APP_PERM_EDITOR")
                intent.setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                intent.putExtra("extra_pkgname", context.packageName)
                AlertDialog.Builder(context)
                    .setTitle(getString(R.string.enable_application_to_show_call_screen_from_background))
                    .setMessage(getString(R.string.enable_application_to_show_call_screen_from_background_text))
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

                /*                    val overlaySettings = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                                    startActivityForResult(overlaySettings, OVERLAY_REQUEST_CODE)*/
            }
        }
        //}
    }

    private fun displayPermissionsGrantedViews(root: View) {
        val defaultDialerPermissionGrantedImage =
            root.findViewById<ImageView>(R.id.defaultDialerPermissionGrantedImage)
        val defaultDialerPermissionGrantedText =
            root.findViewById<TextView>(R.id.defaultDialerPermissionGrantedText)
        val approveDefaultDialerPermissionButton =
            root.findViewById<Button>(R.id.approveDefaultDialerPermission)

        val overlayDrawPermissionGrantedImage =
            root.findViewById<ImageView>(R.id.overlayDrawPermissionGrantedImage)
        val overlayDrawPermissionGrantedText =
            root.findViewById<TextView>(R.id.overlayDrawPermissionGrantedText)
        val approveOverlayDrawPermissionButton =
            root.findViewById<Button>(R.id.approveOverlayDrawPermission)

        val callPhonePermissionGrantedImage =
            root.findViewById<ImageView>(R.id.callPhonePermissionGrantedImage)
        val callPhonePermissionGrantedText =
            root.findViewById<TextView>(R.id.callPhonePermissionGrantedText)
        val approveCallPhonePermissionButton =
            root.findViewById<Button>(R.id.approveCallPhonePermission)

        val contactsPermissionGrantedImage =
            root.findViewById<ImageView>(R.id.contactsPermissionGrantedImage)
        val contactsPermissionGrantedText =
            root.findViewById<TextView>(R.id.contactsPermissionGrantedText)
        val approveReadContactsPermissionButton =
            root.findViewById<Button>(R.id.approveReadContactsPermission)

        val callsLogPermissionGrantedTextImage =
            root.findViewById<ImageView>(R.id.callsLogPermissionGrantedImage)
        val callsLogPermissionGrantedText =
            root.findViewById<TextView>(R.id.callsLogPermissionGrantedText)
        val approvePhoneLogPermissionButton =
            root.findViewById<Button>(R.id.approvePhoneLogPermission)


        displayPermissionGrantedViewsByBoolValue(
            PermissionsStatus.defaultDialerPermissionGranted.value == true, approveDefaultDialerPermissionButton,
            defaultDialerPermissionGrantedImage, defaultDialerPermissionGrantedText
        )

        //val isXiaomi = (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true))
        //if (!isXiaomi) { // trying to detect permission as usual
        displayPermissionGrantedViewsByBoolValue(
                PermissionsStatus.canDrawOverlaysPermissionGranted.value == true && PermissionsStatus.backgroundWindowsAllowed.value == true,
                approveOverlayDrawPermissionButton,
                overlayDrawPermissionGrantedImage,
                overlayDrawPermissionGrantedText
            )
/*        } else { // We cannot detect this permission in Xiaomi, so we'll give user special text
            approveOverlayDrawPermissionButton.visibility = VISIBLE
            overlayDrawPermissionGrantedImage.visibility = GONE
            overlayDrawPermissionGrantedText.visibility = GONE
            val textOverlayDrawPermissionsExplain =
                root.findViewById<TextView>(R.id.textOverlayDrawPermissionsExplain)
            textOverlayDrawPermissionsExplain.text =
                getString(R.string.xiaomi_custom_permission_settings_explain)
        }*/

        displayPermissionGrantedViewsByBoolValue(
            PermissionsStatus.callPhonePermissionGranted.value == true, approveCallPhonePermissionButton,
            callPhonePermissionGrantedImage, callPhonePermissionGrantedText
        )

        displayPermissionGrantedViewsByBoolValue(
            PermissionsStatus.readContactsPermissionGranted.value == true, approveReadContactsPermissionButton,
            contactsPermissionGrantedImage, contactsPermissionGrantedText
        )

        displayPermissionGrantedViewsByBoolValue(
            PermissionsStatus.readCallLogPermissionGranted.value == true, approvePhoneLogPermissionButton,
            callsLogPermissionGrantedTextImage, callsLogPermissionGrantedText
        )
    }

   /* private fun displayPermissionGrantedViews(
        permissionGrantedObject: MutableLiveData<Boolean>,
        approvePermissionButton: Button,
        permissionGrantedImage: ImageView,
        permissionGrantedTextImage: TextView
    ) {
        if (permissionGrantedObject.value != true) {
            // val explainContactsPermissionText = "The application needs Phone Call permission in order to make phone calls as the Application Default Dialer."
            // contactsPermissionExplainText.text = explainContactsPermissionText
            approvePermissionButton.visibility = VISIBLE
            permissionGrantedImage.visibility = GONE
            permissionGrantedTextImage.visibility = GONE
        } else {
            approvePermissionButton.visibility = GONE
            permissionGrantedImage.visibility = VISIBLE
            permissionGrantedTextImage.visibility = VISIBLE
        }
    }*/

    private fun displayPermissionGrantedViewsByBoolValue(
        booleanPermissionGranted: Boolean,
        approvePermissionButton: Button,
        permissionGrantedImage: ImageView,
        permissionGrantedTextImage: TextView
    ) {
        if (!booleanPermissionGranted) {
            // val explainContactsPermissionText = "The application needs Phone Call permission in order to make phone calls as the Application Default Dialer."
            // contactsPermissionExplainText.text = explainContactsPermissionText
            approvePermissionButton.visibility = VISIBLE
            permissionGrantedImage.visibility = GONE
            permissionGrantedTextImage.visibility = GONE
        } else {
            approvePermissionButton.visibility = GONE
            permissionGrantedImage.visibility = VISIBLE
            permissionGrantedTextImage.visibility = VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        OpenScreensStatus.isPermissionsScreenOpened = false
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

                displayPermissionsGrantedViews(fragmentRoot)
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
                displayPermissionsGrantedViews(fragmentRoot)
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

                displayPermissionsGrantedViews(fragmentRoot)
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

        //val context = requireContext()
        if (PermissionsStatus.readCallLogPermissionGranted.value === null || (!(PermissionsStatus.readCallLogPermissionGranted.value!!))) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    READ_CALL_LOG
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Permission is granted
                PermissionsStatus.readCallLogPermissionGranted.value = true
                displayPermissionsGrantedViews(fragmentRoot)
                if (!permissionsGrantedBecauseOfDefaultDialer) {
/*                    Toast.makeText(
                        context,
                        getString(R.string.calls_log_permission_was_granted),
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context,
                        getString(R.string.calls_log_permission_was_granted))
                }
            } else if (askingViewCallsPermission) {
                val toastMsg =
                    getString(R.string.calls_log_permission_was_denied_but_needed)
               // Snackbar.make(fragmentRoot, toastMsg, 8000).show()
                showLongSnackBar(requireActivity(), toastMsg, 8000)
                // Permission is still denied
                //Toast.makeText(context, "Permission is not granted.", Toast.LENGTH_SHORT).show()
            }
            askingViewCallsPermission = false
        }

        if (PermissionsStatus.backgroundWindowsAllowed.value != true) {
            if (isBackgroundWindowsAllowed(fragmentRoot.context)) {
                PermissionsStatus.backgroundWindowsAllowed.value = true
                displayPermissionsGrantedViews(fragmentRoot)
                if (!permissionsGrantedBecauseOfDefaultDialer) { // && !isXiaomi) { // For isXiaomi we are not sure if permission was really granted
                    showCustomToastDialog(requireContext(),
                        getString(R.string.overlay_draw_permission_was_granted)) // for user we write same msg as for canDrawOverlaysPermissionGranted
                }
            }
        }

        if (PermissionsStatus.canDrawOverlaysPermissionGranted.value != true) {
            if (Settings.canDrawOverlays(fragmentRoot.context)) {
                // Permission is granted
                PermissionsStatus.canDrawOverlaysPermissionGranted.value = true
                displayPermissionsGrantedViews(fragmentRoot)
                if (!permissionsGrantedBecauseOfDefaultDialer) { // && !isXiaomi) { // For isXiaomi we are not sure if permission was really granted
                    showCustomToastDialog(requireContext(),
                        getString(R.string.overlay_draw_permission_was_granted))
                }
            }
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