package com.nirotem.simplecall.statuses

import android.Manifest.permission.CALL_PHONE
import android.Manifest.permission.READ_CALL_LOG
import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.SEND_SMS
import android.app.Activity
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity.ROLE_SERVICE
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.MutableLiveData
import com.nirotem.sharedmodules.statuses.OemDetector
import com.nirotem.simplecall.R
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog

//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber

object PermissionsStatus {
    private val REQUEST_RECORD_AUDIO_PERMISSION = 1
    var defaultDialerPermissionGranted = MutableLiveData(false) // Needed to answer calls
    var readContactsPermissionGranted =
        MutableLiveData(false) // Needed for Contacts screen and Add New Contact and Gold Number and Call Details/History
    var writeContactsPermissionGranted = MutableLiveData(false) // Needed to add new Contact
    var readCallLogPermissionGranted =
        MutableLiveData(false) // For Calls screen, Single Call history, Contact Calls history
    var canDrawOverlaysPermissionGranted =
        MutableLiveData(false) // For Loading Activity when app is not loaded (Incoming call)
    var backgroundWindowsAllowed = MutableLiveData(false)
    var callPhonePermissionGranted = MutableLiveData(false) // To make phone calls
    var permissionToShowWhenDeviceLockedAllowed = MutableLiveData(false)

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
                activity, context, context.getString(R.string.app_must_have_permission_to_make_calls),
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
        val isXiaomi = (Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true))
        if (isXiaomi) {
            return isBackgroundWindowsAllowedMIUIXiaomi(context)
        }
        else {
            return isBackgroundWindowsAllowedStandard(context)
        }
    }

    fun isBackgroundWindowsAllowedStandard(context: Context): Boolean {
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

    fun isBackgroundWindowsAllowedMIUIXiaomi(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val mgr = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            return try {
                val method = AppOpsManager::class.java.getMethod(
                    "checkOpNoThrow",
                    Int::class.java, Int::class.java, String::class.java
                )
                // 10021 is MIUI’s "background start activity" op
                val mode = method.invoke(
                    mgr,
                    10021,
                    android.os.Process.myUid(),
                    context.packageName
                ) as Int
                mode == AppOpsManager.MODE_ALLOWED
            } catch (e: Exception) {
                false
            }
        }
        return true
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

           // if (PermissionsStatus.canDrawOverlaysPermissionGranted.value == true && PermissionsStatus.backgroundWindowsAllowed.value == true) {
            if (PermissionsStatus.canDrawOverlaysPermissionGranted.value != true) {
                if (Settings.canDrawOverlays(context)) {
                    // Permission is granted
                    PermissionsStatus.canDrawOverlaysPermissionGranted.value = true
                }
            }

            if (PermissionsStatus.backgroundWindowsAllowed.value != true) {
                if (isBackgroundWindowsAllowed(context)) {
                    // Permission is granted
                    PermissionsStatus.backgroundWindowsAllowed.value = true
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
            defaultDialerPermissionGranted.value = dialerPermissionGranted
            readContactsPermissionGranted.value = readContactsPermission

            val canShowWhenDeviceLockedAllowed = isShowOnLockScreenAllowed(context)
            if (canShowWhenDeviceLockedAllowed != permissionToShowWhenDeviceLockedAllowed.value) {
                showCustomToastDialog(context, context.getString(R.string.permission_change_detected_reloading))
            }
            permissionToShowWhenDeviceLockedAllowed.value = canShowWhenDeviceLockedAllowed

            val canPaintOnBackgroundWindows = isBackgroundWindowsAllowed(context)
            val canDrawOverlays = Settings.canDrawOverlays(context)
            if (canPaintOnBackgroundWindows != backgroundWindowsAllowed.value
                || canDrawOverlays != canDrawOverlaysPermissionGranted.value) {
                showCustomToastDialog(context, context.getString(R.string.permission_change_detected_reloading))
            }
            canDrawOverlaysPermissionGranted.value = canDrawOverlays
            backgroundWindowsAllowed.value = canPaintOnBackgroundWindows
        }
        catch (e: Exception) {
            Log.e("Permission Status - checkForPermissionsChangesAndShowToastIfChanged", "Error (${e.message})")
        }
    }

    fun isShowOnLockScreenAllowed(context: Context): Boolean {
        return if (Build.MANUFACTURER.equals("Xiaomi", true)) {
            isShowOnLockScreenAllowedMIUIXiaomi(context)    // הפונקציה שבנינו
        } else {
            // ברוב המכשירים די ב‑Overlay
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                true
                //Settings.canDrawOverlays(context) // but we already check this for draw overlay
            } else true
        }
    }

    fun isShowOnLockScreenAllowedMIUIXiaomi(context: Context): Boolean {
        try {
            val MIUI_LOCKSCREEN_OP_NUM = 10020
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val uid    = Binder.getCallingUid()
            val pkg    = context.packageName

            /* ---------- 1. מנסים למצוא OPSTR_* ---------- */
            val opStr = runCatching {
                val names = listOf(
                    "OPSTR_SHOW_ON_LOCK_SCREEN",
                    "OPSTR_DISPLAY_ON_LOCK_SCREEN",
                    "OPSTR_WAKEUP_ON_LOCK_SCREEN"
                )
                val cls = AppOpsManager::class.java
                names.firstNotNullOf { n ->
                    runCatching {
                        cls.getDeclaredField(n).apply { isAccessible = true }.get(null) as String
                    }.getOrNull()
                }
            }.getOrNull()

            if (opStr != null && opStr.any { !it.isDigit() }) {
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(opStr, uid, pkg)
                } else {
                    AppOpsManager.MODE_ALLOWED // try to allow
                }
                return mode == AppOpsManager.MODE_ALLOWED
            }

            /* ---------- 2. Fallback למספר 10020 (Reflection) ---------- */
            val modeNum = runCatching {
                val m = AppOpsManager::class.java.getMethod(
                    "checkOpNoThrow",
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType,
                    String::class.java
                )
                m.invoke(appOps, MIUI_LOCKSCREEN_OP_NUM, uid, pkg) as Int
            }.getOrElse { AppOpsManager.MODE_ERRORED }

            return modeNum == AppOpsManager.MODE_ALLOWED
        }
        catch(e: Exception) {
            Log.d("lockscreen - PermissionStatus", "isSowOnLockScreenAllowedMIUIXiaomi error (${e.message})")
        }
        return true
    }

    fun askForRecordPermission(context: Context, activity: Activity) {
        try {
            if (ActivityCompat.checkSelfPermission(context, RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            }
        }
        catch (e: Exception) {
            Log.e("Permission Status - askForRecordPermission", "Error (${e.message})")
            suggestManualPermissionGrant(context)
        }
    }

    fun askForSMSPermission(context: Context, activity: Activity) {
        try {
            if (ContextCompat.checkSelfPermission(context, SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, arrayOf(SEND_SMS), 1)
            }


        }
        catch (e: Exception) {
            Log.e("Permission Status - askForSMSPermission", "Error (${e.message})")
            suggestManualPermissionGrant(context)
        }
    }

    fun loadOtherPermissionsIssueDialog(resourceText: Int, context: Context) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.lock_screen_dialog_permission, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.permission_image)
        val messageView = dialogView.findViewById<TextView>(R.id.permission_message)
        val titleView = dialogView.findViewById<TextView>(R.id.lock_permission_dialog_title)

        titleView.text = context.getString(R.string.permission_missing)

        var overlayPermissionName: String

        if (Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)) {
            overlayPermissionName = context.getString(R.string.overlay_permission_display_name_huawei)
        }
        else {
            val oem = OemDetector.current()
            overlayPermissionName = when (oem) {
                OemDetector.Oem.XIAOMI -> context.getString(R.string.allow_pop_permission_display_name_xiaomi)
                OemDetector.Oem.SAMSUNG -> context.getString(R.string.overlay_permission_display_name_samsung)
                else -> context.getString(R.string.overlay_permission_display_name) // Pixel and others (One Plus should also be like Pixel)
            }
        }

        val lockScreenPermissionName =
            context.getString(R.string.lock_screen_permission_display_name)

        val backgroundWindowsPermissionNameXiaomi =
            context.getString(R.string.overlay_permission_display_name_xiaomi)

        // בדיקת הרשאות חסרות
        val isOverlayMissing = !(PermissionsStatus.canDrawOverlaysPermissionGranted.value ?: false)
        val isLockScreenMissing =
            !(PermissionsStatus.permissionToShowWhenDeviceLockedAllowed.value ?: false)
        val isBackgroundWindowsMissing =
            !(PermissionsStatus.backgroundWindowsAllowed.value ?: false)

        val missingPermissionsList = mutableListOf<String>()

        if (isOverlayMissing) {
            missingPermissionsList.add(overlayPermissionName)
        }
        if (isLockScreenMissing) {
            missingPermissionsList.add(lockScreenPermissionName)
        }
        if (isBackgroundWindowsMissing) {
            missingPermissionsList.add(backgroundWindowsPermissionNameXiaomi)
        }

        val permissionsText = missingPermissionsList.joinToString(separator = ", ")

        // עכשיו הודעה דינמית
        val quantity = if (missingPermissionsList.size == 1) 1 else 2

        messageView.text = context.resources.getQuantityString(
            resourceText,
            quantity,
            permissionsText
        )

        var imageResId: Int
        if (Build.MANUFACTURER.equals("HUAWEI", ignoreCase = true)) {
            imageResId = R.drawable.other_permissions_huawei
        }
        else {
            val oem = OemDetector.current()
            imageResId = when (oem) {
                OemDetector.Oem.SAMSUNG, OemDetector.Oem.OTHER -> R.drawable.other_permissions_samsung
                OemDetector.Oem.GOOGLE -> R.drawable.other_permissions_pixel
                OemDetector.Oem.XIAOMI -> {
                    when {
                        (isOverlayMissing || isBackgroundWindowsMissing) && isLockScreenMissing -> R.drawable.all_other_permissions_xioami
                        (isOverlayMissing || isBackgroundWindowsMissing) -> R.drawable.other_permissions_xioami
                        isLockScreenMissing -> R.drawable.showonlockscreenpermission
                        else -> R.drawable.other_permissions_xioami
                    }
                }
                else -> R.drawable.other_permissions_samsung
            }
        }
        // בחירת תמונה מתאימה


        imageView.setImageResource(imageResId)

        val alertDialog = AlertDialog.Builder(context, R.style.LockScreenPermissionDialogBackground)
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.open_settings)) { dialog, _ ->
                openAppSettings(context)
            }
            .setNegativeButton(context.getString(R.string.ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()

        // שינוי צבע כפתורי הדיאלוג
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            ?.setTextColor(ContextCompat.getColor(context, R.color.blue_500))
        alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            ?.setTextColor(ContextCompat.getColor(context, R.color.blue_500))

        // שינוי גודל הדיאלוג — רווח 15dp מכל צד
        val marginDp = 15
        val marginPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            marginDp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
        val screenWidth = context.resources.displayMetrics.widthPixels
        val desiredWidth = screenWidth - (marginPx * 2)
        val window = alertDialog.window
        window?.setLayout(desiredWidth, WindowManager.LayoutParams.WRAP_CONTENT)
    }

}