package com.nirotem.simplecall

import androidx.lifecycle.MutableLiveData

object CallActivity {
    var originalPhoneNumber: String? = null
    var callEndedShouldCloseActivityEvent = MutableLiveData(false)
    var criticalErrorEvent = MutableLiveData(false)
    var criticalErrorEventMessage = ""
}
