package com.nirotem.simplecall.ui.singleCallHistory

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.nirotem.simplecall.ui.conferenceCall.ConferenceCallViewModel

class SingleCallHistoryViewModel() : ViewModel() {
    data class ContactData(
        val phoneNumber: String,
        val contactName: String,
    )

    // נתון סטטי לשיתוף
    private val _sharedData = MutableLiveData<ContactData>()
    val contactData: LiveData<ContactData> get() = _sharedData

    fun setSharedData(data: ContactData) {
        _sharedData.value = data
    }
}