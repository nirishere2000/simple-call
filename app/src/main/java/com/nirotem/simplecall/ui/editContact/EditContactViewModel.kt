package com.nirotem.simplecall.ui.editContact

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class EditContactViewModel(private val phoneNumber: String): ViewModel() {

    private val _texts = MutableLiveData<List<String>>().apply {
        value = (1..16).mapIndexed { _, i ->
            "This is item # $i"
        }
    }

    val texts: LiveData<List<String>> = _texts
}