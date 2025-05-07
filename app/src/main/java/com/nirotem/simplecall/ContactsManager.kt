package com.nirotem.simplecall

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.TextUtils
import com.nirotem.simplecall.helpers.DBHelper.getContactNameFromPhoneNumber

/*fun getContactNameOrPhoneNumber(contentResolver: ContentResolver, phoneNumber: String): String {
    // First, check if the number is blocked by querying the CallLog
    if (isBlockedNumber(contentResolver, phoneNumber)) {
        return "Unknown Caller"
    }

    // Query the ContactsContentProvider to check if a contact exists for the phone number
    val contactName = getContactNameFromPhoneNumber(contentResolver, phoneNumber)

    // If a contact name is found, return it, otherwise return the phone number
    return if (!TextUtils.isEmpty(contactName)) {
        contactName
    } else {
        phoneNumber
    }
}*/

private fun isBlockedNumber(contentResolver: ContentResolver, phoneNumber: String): Boolean {
    // Check if the phone number is present in the blocked numbers list
    val blockedNumbersUri = android.provider.BlockedNumberContract.BlockedNumbers.CONTENT_URI
    val cursor: Cursor? = contentResolver.query(
        blockedNumbersUri,
        arrayOf(android.provider.BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
        "${android.provider.BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
        arrayOf(phoneNumber),
        null
    )
    val isBlocked = cursor?.count ?: 0 > 0
    cursor?.close()
    return isBlocked
}