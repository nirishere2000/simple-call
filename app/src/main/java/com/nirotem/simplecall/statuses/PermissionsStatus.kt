package com.nirotem.simplecall.statuses

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.app.Activity
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.ROLE_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.MutableLiveData
import com.nirotem.simplecall.R
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog

//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber

object PermissionsStatus {
    var defaultDialerPermissionGranted = MutableLiveData(false) // Needed to answer calls
    var readContactsPermissionGranted =
        MutableLiveData(false) // Needed for Contacts screen and Add New Contact and Gold Number and Call Details/History
    var writeContactsPermissionGranted = MutableLiveData(false) // Needed to add new Contact
    var readCallLogPermissionGranted =
        MutableLiveData(false) // For Calls screen, Single Call history, Contact Calls history
    var canDrawOverlaysPermissionGranted =
        MutableLiveData(false) // For Loading Activity when app is not loaded (Incoming call)
    var callPhonePermissionGranted = MutableLiveData(false) // To make phone calls

    // asking for permissions
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private fun askForPermission(
        activity: Activity,
        context: Context,
        permissionRegularExplain: String,
        permissionRejectedManualExplain: String,
        onAskPermission: () -> Unit
    ): Boolean {
/*        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permissionName
            )
        ) {
            showPermissionManualExplanationDialog(context, permissionRejectedManualExplain)
            return false // User already rejected the permission. We just show him/her how to grant manually
        } else {

            showPermissionsConfirmationDialog(
                context,
                context.getString(R.string.permission_needed_capital_p),,
                permissionRegularExplain,
                onAskPermission
            )
            return true // asking for permission started
        }*/

        try {
            showPermissionsConfirmationDialog(
                context,
                context.getString(R.string.permission_needed_capital_p),
                permissionRegularExplain,
                onAskPermission
            )
            return true // asking for permission started
        }
        catch (e: Exception) {
            Log.e("Permission Status - askForPermission", "Error (${e.message})")
           // showPermissionManualExplanationDialog(context, permissionRejectedManualExplain)
            suggestManualPermissionGrant(context)
            return false // asking for permission started
        }
    }

    private fun showPermissionsConfirmationDialog(
        context: Context,
        msgTitle: String,
        msgText: String,
        onAskPermission: () -> Unit
    ) {
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

    /*fun showReadContactsPermissionsExplanationDialog(context: Context) {
        val title = context.getString(R.string.permissions_enable_contacts_permission_manually_title1)
        val permissionName = context.getString(R.string.permissions_enable_contacts_permission_manually_title2)
        val message = context.getString(R.string.how_to_enable_standard_permission_manually_text, title, permissionName).trimIndent()

       // showPermissionManualExplanationDialog(context, message)
        suggestManualPermissionGrant(context)
    }

    fun showCallLogPermissionsExplanationDialog(context: Context) {
        val title = context.getString(R.string.permissions_enable_calls_log_permission_manually_title1)
        val permissionName = context.getString(R.string.permissions_enable_calls_log_permission_manually_title2)
        val message = context.getString(R.string.how_to_enable_standard_permission_manually_text, title, permissionName).trimIndent()

        //showPermissionManualExplanationDialog(context, message)
        suggestManualPermissionGrant(context)
    }

    fun showMakePhoneCallPermissionsExplanationDialog(context: Context) {
        val title = context.getString(R.string.permissions_enable_call_phone_permission_manually_title1)
        val permissionName = context.getString(R.string.permissions_enable_call_phone_permission_manually_title2)
        val message = context.getString(R.string.how_to_enable_standard_permission_manually_text, title, permissionName).trimIndent()

       // showPermissionManualExplanationDialog(context, message)
        suggestManualPermissionGrant(context)
    }

    fun showPermissionManualExplanationDialog(context: Context, message: String) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.enable_permission_manually_caption))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.enable_permission_manually_ok_button)) { dialog, _ ->
                dialog.dismiss() // Close the dialog
            }
            .setCancelable(false) // Make the dialog not cancellable by touching outside
            .show()
    }*/

    // Default Dialer permission:
     var askingForDefaultDialerPermission = false
    fun requestRole(roleLauncher: ActivityResultLauncher<Intent>, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // עבור Android 10 ומעלה
            val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (roleManager.isRoleAvailable(RoleManager.ROLE_DIALER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_DIALER)
            ) {
                val roleRequestIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                askingForDefaultDialerPermission = true
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
                askingForDefaultDialerPermission = true
                roleLauncher.launch(intent)
            }
        }
    }

    // Call phone permission:
    var askingForMakingMakingCallPermission = false

    fun askForCallPhonePermission(
        activity: Activity, context: Context,
        permissionLauncher: ActivityResultLauncher<String>
    ) {
        requestPermissionLauncher = permissionLauncher
        askingForMakingMakingCallPermission =
            askForPermission(
                activity, context, CALL_PHONE,
                context.getString(R.string.app_must_have_permission_to_make_calls),
                ::requestCallPhonePermission
            )
    }

    fun suggestManualPermissionGrant(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.permission_denied_capital))
            .setMessage(context.getString(R.string.permission_could_not_be_granted_automatically))
            .setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ -> 
                openAppSettings(context)
            }
            .setNegativeButton(context.getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun isBackgroundWindowsAllowed(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.unsafeCheckOpNoThrow(
                "android:system_alert_window",
                android.os.Process.myUid(),
                context.packageName
            )
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun requestCallPhonePermission() {
        requestPermissionLauncher.launch(CALL_PHONE)
    }

    // We want the permissions to be updated - so checking after default dialer chosen
    fun checkForPermissionsGranted(context: Context) {
        try {
            if (PermissionsStatus.callPhonePermissionGranted.value === null || (!(PermissionsStatus.callPhonePermissionGranted.value!!))) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is granted
                    PermissionsStatus.callPhonePermissionGranted.value = true
                }
            }

            if (PermissionsStatus.readContactsPermissionGranted.value === null || (!(PermissionsStatus.readContactsPermissionGranted.value!!))) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is granted
                    PermissionsStatus.readContactsPermissionGranted.value = true
                }
            }

            if (PermissionsStatus.readCallLogPermissionGranted.value === null || (!(PermissionsStatus.readCallLogPermissionGranted.value!!))) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        READ_CALL_LOG
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission is granted
                    PermissionsStatus.readCallLogPermissionGranted.value = true
                }
            }

            if (PermissionsStatus.canDrawOverlaysPermissionGranted.value === null || (!(PermissionsStatus.canDrawOverlaysPermissionGranted.value!!))) {
                if (isBackgroundWindowsAllowed(context)) {
                    // Permission is granted
                    PermissionsStatus.canDrawOverlaysPermissionGranted.value = true
                }
            }
        }
        catch (e: Exception) {
            Log.e("Permission Status - checkForPermissionsGranted", "Error (${e.message})")
        }
    }

    // For now only default and Read Contacts - for Tour and Settings !!!
    fun checkForPermissionsChangesAndShowToastIfChanged(context: Context, activity: Activity) {
        try {
            val telecomManager = context.getSystemService(TELECOM_SERVICE) as TelecomManager
            val dialerPermissionGranted = telecomManager.defaultDialerPackage == context.packageName
            val readContactsPermission = ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED

            if (PermissionsStatus.defaultDialerPermissionGranted.value != dialerPermissionGranted
                || PermissionsStatus.readContactsPermissionGranted.value != readContactsPermission) {
/*                Toast.makeText(context,
                    context.getString(R.string.permission_change_detected_reloading), Toast.LENGTH_LONG).show()*/
                showCustomToastDialog(context, context.getString(R.string.permission_change_detected_reloading))
            }
            PermissionsStatus.defaultDialerPermissionGranted.value = dialerPermissionGranted
            PermissionsStatus.readContactsPermissionGranted.value = readContactsPermission
        }
        catch (e: Exception) {
            Log.e("Permission Status - checkForPermissionsChangesAndShowToastIfChanged", "Error (${e.message})")
        }
    }
}