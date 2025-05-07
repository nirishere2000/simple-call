package com.nirotem.simplecall.ui.permissionsScreen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.nirotem.simplecall.R
import com.nirotem.simplecall.databinding.FragmentPermissionsAlertBinding
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.askForCallPhonePermission
import com.nirotem.simplecall.statuses.PermissionsStatus.requestRole
import com.nirotem.simplecall.ui.components.CallPermissionMissingDialog

class PermissionsAlertFragment : DialogFragment() {

    private var _binding: FragmentPermissionsAlertBinding? = null

    // Declare the permission request launcher
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var roleLauncher: ActivityResultLauncher<Intent> // for default dialer
    private lateinit var fragmentRoot: View
   // private lateinit var roleRequestLauncher: ActivityResultLauncher<Intent>

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var askingDefaultDialerPermission = false
    private var askingForMakingMakingCallPermission = false
    private var askingViewCallsPermission = false
    private var isMakeCallPermission = false
    private var isDefaultDialerPermission = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (PermissionsStatus.askingForMakingMakingCallPermission) {
                PermissionsStatus.callPhonePermissionGranted.value = isGranted
                PermissionsStatus.askingForMakingMakingCallPermission = false

            }
            closeForm()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPermissionsAlertBinding.inflate(inflater, container, false)
        val root: View = binding.root
        fragmentRoot = root
        val activity = requireActivity()
        val context = requireContext()

        roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (PermissionsStatus.askingForDefaultDialerPermission) {
                    PermissionsStatus.askingForDefaultDialerPermission = false
                }
                PermissionsStatus.defaultDialerPermissionGranted.value = true // we do this anyway
            } else {
                // עדכון סטטוס ההרשאה במידה והפעולה נכשלה או בוטלה
                //permissionsViewModel.setDefaultDialerPermission(false)
            }
            closeForm()
        }

        val isMakeCallPermissionAutomatically = arguments?.getBoolean("IS_MAKE_CALL_PERMISSION", false)
        if (isMakeCallPermissionAutomatically == true) {
            isMakeCallPermission = true
            val formText = root.findViewById<TextView>(R.id.textPermissionsExplain)
            formText.text = getString(R.string.the_app_needs_permission_to_make_calls_please_grant)
        }
        else {
            isMakeCallPermission = false
            val isDefaultDialerPermissionAutomatically = arguments?.getBoolean("IS_DEFAULT_DIALER_PERMISSION", false)
            if (isDefaultDialerPermissionAutomatically == true) {
                isDefaultDialerPermission = true
                val formText = root.findViewById<TextView>(R.id.textPermissionsExplain)
                formText.text =
                    getString(R.string.please_grant_default_dialer_permission_so_app_can_handle_calls)
            }
            else {
                isDefaultDialerPermission = false
            }
        }

        val closePermissionsWindow = root.findViewById<ImageView>(R.id.closePermissionsWindow)
        closePermissionsWindow.setOnClickListener {
            closeForm()
        }

        val openPermissionsScreen =
            root.findViewById<TextView>(R.id.openPermissionsScreen)

        val askPermissionDirectly =
            root.findViewById<TextView>(R.id.askPermissionDirectly)

        if (isDefaultDialerPermission) {
            openPermissionsScreen.text = context.getString(R.string.cancel_capital)
        }
        openPermissionsScreen.setOnClickListener {
            if (isMakeCallPermission) {
                if (OpenScreensStatus.isPermissionsScreenOpened) {
                    //Toast.makeText(context, getString(R.string.permissions_screen_is_already_opened), Toast.LENGTH_LONG).show()
                    showCustomToastDialog(context,
                        getString(R.string.permissions_screen_is_already_opened))
                } else {
                    val navController = activity.findNavController(R.id.nav_host_fragment_content_main)
                    navController.navigate(R.id.nav_permissions)
                    closeForm()
                }
            } else if (isDefaultDialerPermission) { // Cancel button
                closeForm()
            }
        }

        askPermissionDirectly.setOnClickListener {
            if (isMakeCallPermission) {
                askForCallPhonePermission(activity, context, requestPermissionLauncher)
            } else if (isDefaultDialerPermission) { // Cancel button
                requestRole(roleLauncher, context) // Ask for Default Dialer
            }
/*            if (OpenScreensStatus.isPermissionsScreenOpened) {
                Toast.makeText(requireContext(), "Permissions screen is already opened", Toast.LENGTH_SHORT).show()
            } else {
                val navController = requireActivity().findNavController(R.id.nav_host_fragment_content_main)
                val args = Bundle().apply {
                    putBoolean("OPEN_CALL_PERMISSION", true)
                }
                navController.navigate(R.id.nav_permissions, args)
                closeForm()
            }*/
        }

        return root
    }

    private fun closeForm() {
        val overlayDialog = parentFragmentManager.findFragmentByTag("PermissionMissingAlertDialogTag") as? PermissionsAlertFragment
        overlayDialog?.dismiss()
    }

    override fun onStart() {
        super.onStart()

        // Set the dialog to occupy only 50% of the screen's width and height
        val dialog = dialog ?: return
        val window = dialog.window ?: return

        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(), // 80% of the screen width
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Optional: Transparent background
        window.setGravity(Gravity.CENTER) // Center the dialog
    }
}