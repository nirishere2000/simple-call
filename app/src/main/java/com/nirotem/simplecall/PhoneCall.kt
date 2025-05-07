package com.nirotem.simplecall

data class PhoneCall(
    val contactName: String,
    val phoneNumber: String,
    val type: String, // e.g., "Incoming", "Outgoing", "Missed"
    val missed: Boolean = false // True if it is a missed call
)