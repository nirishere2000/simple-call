package com.nirotem.simplecall

import android.telecom.Call
import android.telecom.VideoProfile
import android.util.Log
import androidx.lifecycle.MutableLiveData

//import io.reactivex.subjects.BehaviorSubject
//import timber.log.Timber

object WaitingCall {
    //val state: BehaviorSubject<Int> = BehaviorSubject.create()
    var wasAnswered: Boolean = false
    var onHold: Boolean = false // was answered (not ringing) but on hold for other call
    var callWasDisconnectedManually = false
    var originalPhoneNumber: String? = null
    var phoneNumberOrContact: String? = "Unknown Caller"
    var conference = false
    var startedRinging = MutableLiveData(false)
    var replacedWithWaitingCall = false

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, newState: Int) {
           // Timber.d(call.toString())
           // state.onNext(newState)
            Log.d("SimplyCall - WaitingCall", "WaitingCall - onStateChanged: ${call.state}")
        }
    }

    var call: Call? = null
        set(value) {
            field?.unregisterCallback(callback)
            value?.let {
                it.registerCallback(callback)
               // state.onNext(it.state)
            }
            field = value
        }

    fun answer(videoState: Int) {
        wasAnswered = true
        call?.answer(videoState)
    }

    fun hangup() { // by user!
        callWasDisconnectedManually = true
        call?.disconnect()
    }
}