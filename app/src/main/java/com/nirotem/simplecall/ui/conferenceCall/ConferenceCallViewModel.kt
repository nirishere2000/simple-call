package com.nirotem.simplecall.ui.conferenceCall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ConferenceCallViewModel(private val phoneNumber: String) : ViewModel() {
    private val _callAccepted = MutableLiveData<Boolean>()
    val callAccepted: LiveData<Boolean> get() = _callAccepted
    private val _callRejected = MutableLiveData<Boolean>()
    val callRejected: LiveData<Boolean> get() = _callRejected


    fun acceptIncomingCall() {
        _callAccepted.value = true
    }

    fun rejectIncomingCall() {
        _callRejected.value = true
    }

    fun incomingCallRinging() { // ממתינה
        TODO("Not yet implemented")
    }

    val text = MutableLiveData<String>().apply {
        value = phoneNumber
    }

    class ConferenceCallViewModelFactory(private val phoneNumber: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConferenceCallViewModel(phoneNumber) as T
        }
    }

}