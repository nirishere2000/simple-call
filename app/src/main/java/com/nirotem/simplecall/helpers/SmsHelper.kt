package com.nirotem.simplecall.helpers

import android.telephony.SmsManager

fun sendSms(phoneNumber: String, smsMessage: String) {
    val smsManager = SmsManager.getDefault()
    smsManager.sendTextMessage(phoneNumber, null, smsMessage, null, null)
}
