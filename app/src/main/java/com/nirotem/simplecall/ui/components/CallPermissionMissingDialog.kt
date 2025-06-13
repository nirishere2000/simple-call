package com.nirotem.simplecall.ui.components

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

import androidx.fragment.app.DialogFragment
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.OutgoingCall
import com.nirotem.simplecall.R
import com.nirotem.simplecall.statuses.OpenScreensStatus
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.requestRole
import com.nirotem.simplecall.statuses.SettingsStatus


class CallPermissionMissingDialog : DialogFragment() {
    private var callingFragment: String? = null
    private var phoneNumberToCall: String? = null
    private var isVideoCall: Boolean = false
    private lateinit var roleLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_call_permision_missing_dialog, container, false)
        phoneNumberToCall = arguments?.getString("PHONE_NUMBER", "")
        isVideoCall = arguments?.getBoolean("IS_VIDEO", false) == true
        callingFragment = arguments?.getString("CALLING_FRAGMENT", "info")

        // we init the dialog app logo here since it's not common dialog
        val missingCallPermissionAppIcon = view.findViewById<ImageView>(R.id.missingCallPermissionAppIcon)
        if (SettingsStatus.isPremium) {
            missingCallPermissionAppIcon.setImageResource(SettingsStatus.appLogoResourceSmall)
        }

        roleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (PermissionsStatus.askingForDefaultDialerPermission) {
                    PermissionsStatus.defaultDialerPermissionGranted.value = true
                    // Also check for other permissions granted because of default dialer:
                    PermissionsStatus.checkForPermissionsGranted(requireContext())
                    PermissionsStatus.askingForDefaultDialerPermission = false
                }
            } else {
                // עדכון סטטוס ההרשאה במידה והפעולה נכשלה או בוטלה
                //permissionsViewModel.setDefaultDialerPermission(false)
            }
            closeForm()
        }

        Log.d("SimplyCall - CallPermissionMissingDialog", "Missing call permission (phoneNumberToCall = $phoneNumberToCall, isVideo = $isVideoCall)")
        val openDefaultDialerButton = view.findViewById<TextView>(R.id.openDefaultDialerButton)
        openDefaultDialerButton.setOnClickListener {
            requestRole(roleLauncher, view.context) // Ask for Default Dialer
/*            if (OpenScreensStatus.isPermissionsScreenOpened) {
                Toast.makeText(requireContext(), "Permissions screen is already opened", Toast.LENGTH_SHORT).show()
            } else {
               val navController = requireActivity().findNavController(R.id.nav_host_fragment_content_main)
              //  val navController = findNavController(R.id.nav_permissions)
                navController.navigate(R.id.nav_permissions)
              //  parentFragment.navigate(R.id.nav_permissions)
                closeForm()
            }*/
        }

        val callButton = view.findViewById<TextView>(R.id.callButton)
        callButton.setOnClickListener {
            makeCall()
            closeForm()
        }

        val closeButton = view.findViewById<ImageView>(R.id.closeWindow)
        closeButton.setOnClickListener {
            closeForm()
        }

        return view
    }

    private fun closeForm() {
        val overlayDialog = parentFragmentManager.findFragmentByTag("CallPermissionMissingDialogTag") as? CallPermissionMissingDialog
        overlayDialog?.dismiss()
    }

    private fun makeCall() {
        //   if (checkSelfPermission(this, CALL_PHONE) == PERMISSION_GRANTED) {
        //val uri = "tel:${+97237537900}".toUri()

        val uri = "tel:${phoneNumberToCall}".toUri()

        // We have permissions - make the call:
        OutgoingCall.isCalling = true
        var callIntent = Intent(Intent.ACTION_CALL, uri)
        if (isVideoCall) {
            /*                val callIntent = Intent("com.android.phone.videocall", uri)
                                if (callIntent.resolveActivity(requireContext().packageManager) != null) {
                                    ContextCompat.startActivity(requireContext(), callIntent, null)
                                } else {
                                    Toast.makeText(requireContext(), "Video call not supported on this device", Toast.LENGTH_SHORT).show()
                                }*/

            val callIntent = Intent(Intent.ACTION_CALL, uri)
            callIntent.putExtra(
                "android.telecom.extra.START_CALL_WITH_VIDEO_STATE",
                3
            ) // Video call
            ContextCompat.startActivity(requireContext(), callIntent, null)

            // callIntent = Intent("com.android.phone.videocall", uri)
        }
        ContextCompat.startActivity(requireContext(), callIntent, null)

    }

    override fun onStart() {
        super.onStart()

        // Set the dialog to occupy only 50% of the screen's width and height
        val dialog = dialog ?: return
        val window = dialog.window ?: return

        window.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(), // 80% of the screen width
            WindowManager.LayoutParams.WRAP_CONTENT
           // (resources.displayMetrics.heightPixels * 0.5).toInt() // 60% of the screen height
        )

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Optional: Transparent background
        window.setGravity(Gravity.CENTER) // Center the dialog
    }
}