package com.nirotem.simplecall.ui.waitingCall

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class WaitingCallViewModel(private val phoneNumber: String) : ViewModel() {
    private val _callAccepted = MutableLiveData<Boolean>()
    val callAccepted: LiveData<Boolean> get() = _callAccepted
    private val _callRejected = MutableLiveData<Boolean>()
    val callRejected: LiveData<Boolean> get() = _callRejected


    fun acceptIncomingCall() {
        _callAccepted.value = true
    }

    fun rejectIncomingCall() {
        _callRejected.value = true
        Log.d("SimplyCall - IncomingCallViewModel", "callRejected button clicked - viewModel")
    }

    fun incomingCallRinging() {
        TODO("Not yet implemented")
    }

    val text = MutableLiveData<String>().apply {
        value = phoneNumber
    }

    class WaitingCallViewModelFactory(private val phoneNumber: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WaitingCallViewModel(phoneNumber) as T
        }
    }

}