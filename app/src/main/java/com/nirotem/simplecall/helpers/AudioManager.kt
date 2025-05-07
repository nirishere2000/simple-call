package com.nirotem.simplecall.helpers

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioManager.ROUTE_SPEAKER
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallException
import android.telecom.InCallService.AUDIO_SERVICE
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.nirotem.simplecall.InCallServiceManager.Companion.EXTRA_ENABLE_SPEAKER
import com.nirotem.simplecall.InCallServiceManager.Companion.TOGGLE_SPEAKER_ACTION
import com.nirotem.simplecall.OngoingCall
import com.nirotem.simplecall.R

/*fun toggleSpeakerphone(enable: Boolean, context: Context) {
    val tag = "SimplyCall - toggleSpeakerphone"
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        audioManager.mode = AudioManager.MODE_NORMAL
        Thread.sleep(50)
        audioManager.mode = AudioManager.MODE_IN_CALL

        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val speakerDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

        if (enable && speakerDevice != null) {
            val success = audioManager.setCommunicationDevice(speakerDevice)
            Log.d(tag, "setCommunicationDevice(speaker): success=$success")
            retrySetSpeakerphone(audioManager, speakerDevice)
        } else if (!enable) {
            audioManager.clearCommunicationDevice()
            Log.d(tag, "clearCommunicationDevice called")
        } else {
            Log.e(tag, "Speaker device not found")
        }
    } else {
        audioManager.isSpeakerphoneOn = enable
        Log.d(tag, "Speakerphone toggled: $enable (legacy API)")
    }
}*/

fun toggleSpeakerphone(enable: Boolean, context: Context): Boolean {
    val tag = "SimplyCall - toggleSpeakerphone"
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        //audioManager.mode = AudioManager.MODE_NORMAL

        if (enable) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val speakerDevice = devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
            if (speakerDevice != null) {
                //  audioManager.clearCommunicationDevice()
                // Thread.sleep(50) // Wait for state to reset
                val success = audioManager.setCommunicationDevice(speakerDevice)
                //  Thread.sleep(50)
                //audioManager.mode = AudioManager.MODE_IN_CALL
                //Thread.sleep(50)
                Log.d(tag, "setCommunicationDevice(speaker): success=$success")
                val shouldRetry = if (success) {
                    // Check that speaker status was really changed to On
                    !isSpeakerphoneOn(context)
                } else {
                    true // failure - then retry = true
                }
                if (shouldRetry) {
                    Log.d(tag, "Trying to turn On Speaker through IncallService")
                    sendUpdateSpeakerphoneEvent(context, true) // try through IncallService
                    return false // we did not succeed yet
                }
            } else {
                Log.e(tag, "Speaker device not found")
                Log.d(tag, "Trying to turn Speaker On through IncallService")
                sendUpdateSpeakerphoneEvent(context, true) // try through IncallService
                return false // we did not succeed yet
            }
            //retrySetSpeakerphone(audioManager, speakerDevice)
        } else { // turn Speaker Off:
            audioManager.clearCommunicationDevice()
            if ((isSpeakerphoneOn(context))) { // if Speaker is still On
                Log.d(tag, "Trying to turn Off Speaker through IncallService")
                sendUpdateSpeakerphoneEvent(context, false) // try through IncallService
                return false // we did not succeed yet
            }
            else {
                Log.d(tag, "Speaker was turned off")
            }
        }
    } else {
        audioManager.isSpeakerphoneOn = enable
        Log.d(tag, "Speakerphone toggled: $enable (legacy API)")
    }
    return true
}

private fun sendUpdateSpeakerphoneEvent(context: Context, enable: Boolean) {
    val tag = "SimplyCall - sendUpdateSpeakerphoneEvent"

    Log.d(
        tag,
        "Sending TOGGLE_SPEAKER_ACTION (enable = $enable)"
    )

/*    val intent = Intent(TOGGLE_SPEAKER_ACTION)
    intent.putExtra(EXTRA_ENABLE_SPEAKER, enable)
    context.sendBroadcast(intent)*/

    OngoingCall.shouldToggleSpeakerOnOff = enable
    OngoingCall.shouldToggleSpeaker.value = true
}

/*
@RequiresApi(Build.VERSION_CODES.S)
fun retrySetSpeakerphone(audioManager: AudioManager, speakerDevice: AudioDeviceInfo, retries: Int = 3) {
    for (i in 1..retries) {
        val success = audioManager.setCommunicationDevice(speakerDevice)
        Log.d("SimplyCall - toggleSpeakerphone", "Attempt $i to set speaker: success=$success")
        Thread.sleep(100)
        if (audioManager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
            Log.d("SimplyCall - toggleSpeakerphone", "Speakerphone successfully enabled on attempt $i")
            return
        }
    }
    Log.e("SimplyCall", "Failed to enable speakerphone after $retries attempts")
}
*/

/**
 * פונקציה לבדיקה אם הרמקול דולק כרגע.
 * עבור אנדרואיד 12 ומעלה, נבדוק אם ה-CommunicationDevice הנוכחי הוא הרמקול המובנה.
 * בגרסאות ישנות יותר, נתבסס על AudioManager.isSpeakerphoneOn.
 *
 * @param context קונטקסט
 * @return true אם הרמקול דולק, אחרת false
 */
fun isSpeakerphoneOn(context: Context): Boolean {
    val tag = "SimplyCall - toggleSpeakerphone"

    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    //val isSpeakerOn = audioManager.isSpeakerphoneOn()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val currentDevice = audioManager.communicationDevice
        Log.d(
            tag,
            "isSpeakerphoneOn = ${currentDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER}"
        )
        // אם ה-CommunicationDevice הוא רמקול
        return currentDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    } else {
        // גרסאות ישנות: פשוט בודקים את המתג של speakerphone
        return audioManager.isSpeakerphoneOn
    }
}