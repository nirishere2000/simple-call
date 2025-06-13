package com.nirotem.simplecall

import android.telecom.Call
import android.telecom.VideoProfile
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat

//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber

object OngoingCall {
    //val state: BehaviorSubject<Int> = BehaviorSubject.create()

/*    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, newState: Int) {
           // Timber.d(call.toString())
           // state.onNext(newState)
            Log.d("SimplyCall - OngoingCall", "OngoingCall - onStateChanged: ${call.state}")
        }
    }*/

    var wasAnswered: Boolean = false
    var onHold: Boolean = false // was answered (not ringing) but on hold for other call
    var phoneNumberOrContact: String? = "Unknown Caller"

    var callWasDisconnectedManually = false
    var call: Call? = null
    var conference = false
    var isSpeakerOn = false // if active call fragment was unloaded and reloaded because of call waiting we need to reset it
    var isKeypadOpened = false
    var autoAnwered = MutableLiveData(false)

    // These are to convey message to InCallService, because sometimes it doesn't take
    // the Broadcast event so it doesn't toggle the speaker
    // This is "static" and always through OngoingCall only, even if it's out going or waiting call
    var speakerWasAlreadyOnWhenStarted = false
    var shouldToggleSpeaker = MutableLiveData(false)
    var shouldToggleSpeakerOnOff = false
    var shouldUpdateSpeakerState = MutableLiveData(false)
    var callWasEndedMustClose = MutableLiveData(false)
    // End SpeakerPhone Toggle events

/*    var call: Call? = null
        set(value) {
            field?.unregisterCallback(callback)
            value?.let {
                it.registerCallback(callback)
               // state.onNext(it.state)
            }
            field = value
        }*/

    fun answer(videoState: Int) {
        wasAnswered = true
        call?.answer(videoState)
    }

    fun hangup() {
        callWasDisconnectedManually = true
        Log.d("SimplyCall - OngoingCall", "OngoingCall - hangup: ${call?.details}")
        call?.disconnect()
        call = null
    }

    /* This is a static function only in Ongoing Call: */
    fun formatPhoneNumberWithLib(phone: String, regionCode: String): String {
        if (phone.isEmpty()) {
            return phone
        }
        // Detect if the phone is a special dial code starting with '*' or '#'
        // and return it unformatted (or apply custom formatting).
        if (phone.startsWith("*") || phone.startsWith("#")) {
            return phone
        }

        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(phone, regionCode)

            if (!phoneUtil.isValidNumber(numberProto)) {
                return phone
            }

            val actualRegion = phoneUtil.getRegionCodeForNumber(numberProto)
            return if (actualRegion.equals(regionCode, ignoreCase = true)) {
                phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            } else {
                phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            }
        }
        catch (e: Exception) {
            return phone
        }
    }

    private fun getPhoneNumberLocale(phone: String, regionCode: String): String {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(phone, regionCode)
            val actualRegion = phoneUtil.getRegionCodeForNumber(numberProto)
            return if (actualRegion.equals(regionCode, ignoreCase = true)) {
                phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.NATIONAL)
            } else {
                phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
            }
        } catch (e: Exception) {
            return phone
        }
    }

    private fun formatPhoneInternationally(phone: String, regionCode: String): String {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(phone, regionCode)
            return phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        } catch (e: Exception) {
            return phone
        }

    }
}