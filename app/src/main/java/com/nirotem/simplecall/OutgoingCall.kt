package com.nirotem.simplecall

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telecom.Call
import android.telecom.TelecomManager
import android.telecom.VideoProfile
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.MutableLiveData
import com.google.android.material.snackbar.Snackbar
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadGoldNumber
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.statuses.PermissionsStatus.askForCallPhonePermission
import com.nirotem.simplecall.statuses.SettingsStatus
import com.nirotem.simplecall.ui.components.CallPermissionMissingDialog
import com.nirotem.simplecall.ui.permissionsScreen.PermissionsAlertFragment

//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber

object OutgoingCall {
    //val state: BehaviorSubject<Int> = BehaviorSubject.create()

    /*    private val callback = object : Call.Callback() {
            override fun onStateChanged(call: Call, newState: Int) {
               // Timber.d(call.toString())
               // state.onNext(newState)
                Log.d("SimplyCall - OngoingCall", "OngoingCall - onStateChanged: ${call.state}")
            }
        }*/

    var wasAnswered: Boolean = false
    var otherCallerAnswered = MutableLiveData(false)
    var callWasDisconnected = MutableLiveData(false)
    var onHold: Boolean = false // was answered (not ringing) but on hold for other call
    var phoneNumberOrContact: String? =
        "Unknown Caller" // context.getString(R.string.unknown_caller)
    var callWasDisconnectedManually = false
    var call: Call? = null
    var conference = false
    var isCalling = false
    var contactExistsForPhoneNum = false
    var isSpeakerOn =
        false // if active call fragment was unloaded and reloaded because of call waiting we need to reset it
    var isKeypadOpened = false

    /*    var call: Call? = null
            set(value) {
                field?.unregisterCallback(callback)
                value?.let {
                    it.registerCallback(callback)
                   // state.onNext(it.state)
                }
                field = value
            }*/

    fun hangup() {
        callWasDisconnectedManually = true
        Log.d("SimplyCall - OutgoingCall", "OutgoingCall - hangup: ${call?.details}")
        call?.disconnect()
        call = null
    }

    fun makeCall(
        callPhoneNumber: String,
        isVideoCall: Boolean,
        context: Context,
        hostFragment: FragmentManager,
        activity: Activity,
        requestPermissionLauncher: ActivityResultLauncher<String>,
        forceCallInCaseWeHavePermissionButNotDefault: Boolean = false
    ): Boolean {
        wasAnswered = false
        isCalling = true
        val extras = Bundle()
        val uri = "tel:${callPhoneNumber}".toUri()

        // Checking for permissions first
        if (PermissionsStatus.callPhonePermissionGranted.value == true) {
            if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                // We have permissions - make the call:
                //val callIntent = Intent(Intent.ACTION_CALL, uri)
                try {

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CALL_PHONE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.

                        // Since PermissionsStatus.callPhonePermissionGranted.value == true we should never get here
                        val toastMsg = context.getString(R.string.missing_call_phone_permission)
/*                        Toast.makeText(
                            context,
                            toastMsg,
                            Toast.LENGTH_LONG
                        ).show()*/
                        showCustomToastDialog(context, toastMsg)
                        return false
                    }
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    telecomManager.placeCall(uri, extras)
                    return true
                    //ContextCompat.startActivity(context, callIntent, null)
                }
                catch (e: Exception) {
                    val seriousErrorTitle = context.getString(R.string.serious_error_capital)
                    val toastMsg = "$seriousErrorTitle (${e.message})"
/*                    Toast.makeText(
                        context,
                        toastMsg,
                        Toast.LENGTH_LONG
                    ).show()*/
                    showCustomToastDialog(context, toastMsg)
                    return false
                }

            } else { // We are not the default dialer - so we can make a call since we do have permissions for outgoing calls
                     // - but it will get out through the default dialer app, which is not us in this case:
                if (forceCallInCaseWeHavePermissionButNotDefault) { // Force a call - for example in emergency call when we don't want the user to have to interact more
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    telecomManager.placeCall(uri, extras)
                    return true
                }
                else { // Don't force calling through different app, ask the user first:
                    val overlayFragment = CallPermissionMissingDialog()
                    val args = Bundle().apply {
                        putString("PHONE_NUMBER", callPhoneNumber)
                        putBoolean("IS_VIDEO", isVideoCall)
                    }
                    overlayFragment.arguments = args
                    overlayFragment.show(hostFragment, "CallPermissionMissingDialogTag")
                    return false
                }
            }
        } else { // We cannot make a call - ask for permissions:
/*            val toastMsg = "No call phone permission"
            Toast.makeText(
                context,
                toastMsg,
                Toast.LENGTH_LONG
            ).show()*/
            askForCallPhonePermission(activity, context, requestPermissionLauncher)
            return false

            /*            val overlayFragment = PermissionsAlertFragment()
                        val args = Bundle().apply {
                            putBoolean("IS_MAKE_CALL_PERMISSION", true)
                        }
                        overlayFragment.arguments = args
                        overlayFragment.show(hostFragment, "PermissionMissingAlertDialogTag")*/
        }
        return false
    }

    fun callGoldNumber(
        context: Context?, hostFragment: FragmentManager,
        fragmentRoot: View,
        activity: Activity,
        requestPermissionLauncher: ActivityResultLauncher<String>
    ) {

        val goldPhoneNumber = SettingsStatus.goldNumber.value // loadGoldNumber(context)
        if (goldPhoneNumber !== null && goldPhoneNumber !== "" && context != null) {
            makeCall(
                goldPhoneNumber,
                false,
                context,
                hostFragment,
                activity,
                requestPermissionLauncher
            )
        } else { // load gold number dialog to insert a gold number
            var toastMsg =
                if (context != null) context.getString(R.string.to_add_a_gold_number_please_go_to_settings)
                else "To add a Gold Number, please go to Settings by tapping the three-dot menu."
            //Snackbar.make(fragmentRoot, toastMsg, 8000).show()
            showLongSnackBar(activity, toastMsg, 8000)
            //val navController = findNavController()  // Get the NavController
            //navController.navigate(R.id.action_firstFragment_to_secondFragment)
        }
    }
}