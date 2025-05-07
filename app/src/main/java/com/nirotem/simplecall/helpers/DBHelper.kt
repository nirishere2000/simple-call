package com.nirotem.simplecall.helpers

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.BlockedNumberContract
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import android.Manifest
import android.Manifest.permission.READ_CONTACTS
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveContactsMapping
import com.nirotem.simplecall.statuses.PermissionsStatus
import com.nirotem.simplecall.ui.contacts.ContactsInLetterListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DBHelper {

     fun fetchContacts(context: Context): List<String> {
        val contactList = mutableListOf<String>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ), // Add DISPLAY_NAME to projection
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            val displayNameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            if (displayNameIndex == -1) { // Issue that should not happen
                //Toast.makeText(this, "Column DISPLAY_NAME not found!", Toast.LENGTH_SHORT).show()
                return contactList // Probably will return empty list
            }

            while (it.moveToNext()) {
                val name = it.getString(displayNameIndex) // Safely fetch column value
                if (!name.isNullOrEmpty()) {
                    contactList.add(name)
                }
            }
        }
        return contactList
    }

    fun fetchContactsOptimized(context: Context): List<String> {
        val contactList = mutableListOf<String>()

        // Step 1: Fetch all blocked numbers into a Set for efficient lookup
        val blockedNumbers = mutableSetOf<String>()
        context.contentResolver.query(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            null,
            null,
            null
        )?.use { cursor ->
            val numberIndex = cursor.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
            if (numberIndex != -1) {
                while (cursor.moveToNext()) {
                    val blockedNumber = cursor.getString(numberIndex)
                    if (!blockedNumber.isNullOrEmpty()) {
                        blockedNumbers.add(normalizePhoneNumber(blockedNumber))
                    }
                }
            }
        }

        // Step 2: Fetch all phone numbers and map them to contact IDs
        val phoneNumberMap = mutableMapOf<String, MutableList<String>>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            if (contactIdIndex != -1 && numberIndex != -1) {
                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(contactIdIndex)
                    val phoneNumber = cursor.getString(numberIndex)
                    if (!contactId.isNullOrEmpty() && !phoneNumber.isNullOrEmpty()) {
                        val normalizedNumber = normalizePhoneNumber(phoneNumber)
                        phoneNumberMap.getOrPut(contactId) { mutableListOf() }.add(normalizedNumber)
                    }
                }
            }
        }

        // Step 3: Query all contacts
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
            val contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)

            if (displayNameIndex == -1 || contactIdIndex == -1) {
                // Columns not found; return empty list or handle as needed
                return contactList
            }

            while (cursor.moveToNext()) {
                val contactName = cursor.getString(displayNameIndex)
                val contactId = cursor.getString(contactIdIndex)

                if (!contactName.isNullOrEmpty() && contactId != null) {
                    val contactNumbers = phoneNumberMap[contactId] ?: emptyList()
                    val isBlocked = contactNumbers.any { blockedNumbers.contains(it) }

                    if (!isBlocked) {
                        contactList.add(contactName)
                    }
                }
            }
        }

        return contactList
    }

    /**
     * Normalizes phone numbers by removing spaces, dashes, and other non-digit characters.
     * This helps in accurate comparison between different phone number formats.
     *
     * @param phoneNumber The phone number string to normalize.
     * @return A normalized phone number string containing only digits.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return phoneNumber.filter { it.isDigit() }
    }

    fun getContactNameFromPhoneNumber(context: Context, phoneNumber: String): String {
        // Query contact name
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val contactCursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        var contactName = phoneNumber // Default to phone number if no contact found
        if (contactCursor != null && contactCursor.moveToFirst()) {
            val nameIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            if (nameIndex >= 0) {
                contactName = contactCursor.getString(nameIndex)
            }
        }
        contactCursor?.close()

        return contactName
    }

    fun getContactNameFromPhoneNumberAndReturnNullIfNotFound(context: Context, phoneNumber: String): String? {
        // Query contact name
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val contactCursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        var contactName: String? = null // Default to phone number if no contact found
        if (contactCursor != null && contactCursor.moveToFirst()) {
            val nameIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            if (nameIndex >= 0) {
                contactName = contactCursor.getString(nameIndex)
            }
        }
        contactCursor?.close()

        return contactName
    }

    fun getContactNameFromPhoneNumberAndFilterBlocked(context: Context, phoneNumber: String): String {
        // Check if the number is blocked
        val blockedCursor = context.contentResolver.query(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
            "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
            arrayOf(phoneNumber),
            null
        )

        if (blockedCursor != null && blockedCursor.moveToFirst()) {
            blockedCursor.close()
            return "Unknown (Blocked)"
        }
        blockedCursor?.close()

        // Query contact name
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
        val contactCursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        var contactName = phoneNumber // Default to phone number if no contact found
        if (contactCursor != null && contactCursor.moveToFirst()) {
            val nameIndex = contactCursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            if (nameIndex >= 0) {
                contactName = contactCursor.getString(nameIndex)
            }
        }
        contactCursor?.close()

        return contactName
    }

    fun getPhoneNumberFromContactNameAndFilterBlocked(context: Context, contactName: String): String {
        // Define the URI and projection for querying the contact's phone number
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(contactName)

        try {
            // Query the Contacts database for the phone number(s) associated with the contact name
            val phoneNumber = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        cursor.getString(numberIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            // If no phone number found, return "Unknown"
            if (phoneNumber == null) {
                return context.getString(R.string.unknown_capital)
            }

            // Check if the phone number is blocked
            val isBlocked = context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?",
                arrayOf(phoneNumber),
                null
            )?.use { blockedCursor ->
                blockedCursor.moveToFirst()
            } ?: false

            return if (isBlocked) context.getString(R.string.unknown_blocked) else phoneNumber
        } catch (e: Exception) {
                e.printStackTrace()
            return context.getString(R.string.unknown_because_of_error)
        }
    }

    fun getPhoneNumberFromContactName(context: Context, contactName: String): String {
        // Define the URI and projection for querying the contact's phone number
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(contactName)

        try {
            // Query the Contacts database for the phone number(s) associated with the contact name
            val phoneNumber = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numberIndex != -1) {
                        cursor.getString(numberIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }

            // If no phone number found, return "Unknown"
            if (phoneNumber == null) {
                return context.getString(R.string.unknown_capital)
            }

          return phoneNumber

        } catch (e: Exception) {
            e.printStackTrace()
            return context.getString(R.string.unknown_because_of_error)
        }
    }

    fun getContactIdFromPhoneNumber(context: Context, phoneNumber: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactIdIndex =
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    return cursor.getString(contactIdIndex)
                }
            }
        return null
    }

    fun getContactPhoto(context: Context, contactId: String): Bitmap? {
        val uri =
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.PHOTO_URI),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val photoUri =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                if (photoUri != null) {
                    context.contentResolver.openInputStream(Uri.parse(photoUri))
                        ?.use { inputStream ->
                            return BitmapFactory.decodeStream(inputStream)
                        }
                }
            }
        }
        return null // Return null if no photo exists
    }

    fun getContactPhotoUri(context: Context, contactId: String): String? {
        val uri =
            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId.toLong())
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.PHOTO_URI),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val photoUri =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                return photoUri
            }
        }
        return null // Return null if no photo exists
    }

    /**
     * Checks if a contact with the given phone number is marked as a favorite (starred).
     *
     * @param phoneNumber The phone number of the contact to check.
     * @param context The context used to access the ContentResolver.
     * @return True if the contact is a favorite, False otherwise.
     */
    fun isNumberInFavorites(phoneNumber: String, context: Context): Boolean {
        // Ensure the phone number is not empty or blank
        if (phoneNumber.isBlank()) {
            return false
        }

        // Normalize the phone number to ensure consistency
        //val normalizedNumber = phoneNumber.replace("[^\\d+]".toRegex(), "")
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        // Construct the URI for phone number lookup
        val uri: Uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalizedNumber)
        )

        // Define the projection (columns to retrieve)
        val projection = arrayOf(ContactsContract.PhoneLookup.STARRED)

        var isContactInFav = false

        // Query the Contacts Provider
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // Retrieve the STARRED column index
                val starredIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                if (starredIndex != -1) {
                    // Get the value of the STARRED column (1 for starred, 0 otherwise)
                    val starred = cursor.getInt(starredIndex)
                    isContactInFav = (starred == 1)
                }
            }
        }

        return isContactInFav
    }

    /**
     * Checks if a given phone number exists in the device's contacts.
     *
     * @param phoneNumber The phone number to check.
     * @param context The context used to access the ContentResolver.
     * @return True if the phone number is found in contacts, False otherwise.
     */
    fun isNumberInContacts(phoneNumber: String, context: Context): Boolean {
        // Ensure the phone number is not empty or blank
        if (phoneNumber.isBlank()) {
            return false
        }

        // Check if the READ_CONTACTS permission is granted
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // You may choose to request the permission here or handle it in the calling function
            return false
        }

        // Normalize the phone number to ensure consistency
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        // Construct the URI for phone number lookup
        val uri: Uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(normalizedNumber)
        )

        // Define the projection (columns to retrieve)
        val projection = arrayOf(ContactsContract.PhoneLookup._ID)

        // Initialize the result
        var isInContacts = false

        // Query the Contacts Provider
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // At least one contact with the given phone number exists
                isInContacts = true
            }
        }

        return isInContacts
    }

    fun isNumberBlocked2(phoneNumber: String, context: Context): Boolean {
        // Ensure the phone number is not empty or blank
        if (phoneNumber.isBlank()) {
            return false
        }

        // Check if the READ_BLOCKED_NUMBERS permission is granted (we cannot get this permission - it's system permission)
        // Then check if the app is the default dialer
        if (PermissionsStatus.defaultDialerPermissionGranted.value != true) { // Only as default dialer we can check for blocked numbers
            // Permission is not granted
            // You may choose to request the permission here or handle it in the calling function
            return false
        }

        // Normalize the phone number for consistent comparison
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        // Construct the URI for blocked number lookup
        val uri: Uri = Uri.withAppendedPath(
            BlockedNumberContract.BlockedNumbers.CONTENT_URI,
            Uri.encode(normalizedNumber)
        )

        // Define the projection (columns to retrieve)
        val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)

        // Initialize the result
        var isBlocked = false

        // Query the Blocked Numbers Provider
        context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // The phone number is in the blocked list
                isBlocked = true
            }
        }

        return isBlocked
    }



    /**
     * Checks if a given phone number is blocked by the user.
     *
     * @param phoneNumber The phone number to check.
     * @param context The application context.
     * @return True if the number is blocked, false otherwise.
     */
    fun isNumberBlocked(phoneNumber: String, context: Context): Boolean {
        // 1. Validate the phone number
        if (phoneNumber.isBlank()) {
            return false
        }

        // 2. Ensure the device is running Android 7.0 (API level 24) or higher
/*        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) { // Android 7.0 (Nougat)
            // BlockedNumberContract is not available
            return false
        }*/

        // Check if the READ_BLOCKED_NUMBERS permission is granted (we cannot get this permission - it's system permission)
        // Then check if the app is the default dialer
        if (PermissionsStatus.defaultDialerPermissionGranted.value != true) { // Only as default dialer we can check for blocked numbers
            // Permission is not granted
            // You may choose to request the permission here or handle it in the calling function
            return false
        }


        // 5. Normalize the phone number for consistent comparison
        val normalizedNumber = PhoneNumberUtils.normalizeNumber(phoneNumber)

        // 6. Define the URI and projection for the query
        val uri: Uri = BlockedNumberContract.BlockedNumbers.CONTENT_URI
        val projection = arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
        val selection = "${BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER} = ?"
        val selectionArgs = arrayOf(normalizedNumber)

        // 7. Query the Blocked Numbers Provider
        var isBlocked = false
        context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                // The phone number is in the blocked list
                isBlocked = true
            }
        }

        return isBlocked
    }

    // we must map all contacts' names to phone numbers, so we can pull them on call loaded without the app loaded
    // we may not have another chance to save the contacts before the app would run from InCallService
    fun saveContactsForCallWithoutPermissions(context: Context, lifecycleScope: LifecycleCoroutineScope) {
        if (ContextCompat.checkSelfPermission(
                context,
                READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            lifecycleScope.launch {
                try {
                    val contactsList = loadCallHistoryAsync(context)
                    val contactsMapping: Map<String, String> =
                        contactsList.associate { it.contactOrPhoneNumber to it.phoneNumber }
                    saveContactsMapping(context, contactsMapping)
                }
                catch (e: Exception) {

                }
            }
        }
    }

    suspend fun loadCallHistoryAsync(context: Context): List<ContactsInLetterListItem.Contact> {
        return withContext(Dispatchers.IO) {
            // Simulate data loading (e.g., from Call Log)
            if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                loadContactsOptimized(context)
            } else {
                loadContacts(context)
            }
        }
    }

    private fun loadContacts(context: Context): List<ContactsInLetterListItem.Contact> {
        val contactsList = mutableListOf<ContactsInLetterListItem.Contact>()

        try {
            val uniqueContacts = mutableSetOf<String>()  // Set to store unique phone numbers
            val cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME, // Name
                    ContactsContract.CommonDataKinds.Phone.NUMBER, // Phone number
                    // ContactsContract.CommonDataKinds.Event.START_DATE, // יום ההולדת נמצא כאן
                    ContactsContract.Contacts.PHOTO_URI, // Photo URI
                    ContactsContract.Contacts.STARRED, // Starred status (1 or 0)
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER} = 1",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC" // Sort by name
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactName =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val photoUri =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                    val photoUriStr = if (photoUri !== null) photoUri else ""
                    val isStarred =
                        it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1
                    // val birthdayLong =
                    //  it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE))
                    //val birthday = formatTimestamp(birthdayLong)

                    val contact =
                        ContactsInLetterListItem.Contact(
                            phoneNumber,
                            contactName,
                            null,
                            isStarred,
                            photoUriStr
                        )

                    val shortPhoneNumber = phoneNumber.filter { it.isDigit() }

                    if (uniqueContacts.add(contactName + shortPhoneNumber)) {  // Adds only unique Contact + phone number
                        contactsList.add(contact)
                    }


                    // println("Contact: $name, Phone: $phoneNumber, Photo: $photoUri, Starred: $isStarred")
                }
            }
        } catch (error: Error) {
            Log.e("SimplyCall - MainActivity", "ContactsFragment loadContacts Error ($error)")
        }


        return contactsList
    }

    private fun loadContactsOptimized(context: Context): List<ContactsInLetterListItem.Contact> {
        val contactsList = mutableListOf<ContactsInLetterListItem.Contact>()

        try {
            // Step 1: Fetch all blocked numbers into a Set for efficient lookup
            val blockedNumbers = mutableSetOf<String>()
            context.contentResolver.query(
                BlockedNumberContract.BlockedNumbers.CONTENT_URI,
                arrayOf(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER),
                null,
                null,
                null
            )?.use { cursor ->
                val numberIndex =
                    cursor.getColumnIndex(BlockedNumberContract.BlockedNumbers.COLUMN_ORIGINAL_NUMBER)
                if (numberIndex != -1) {
                    while (cursor.moveToNext()) {
                        val blockedNumber = cursor.getString(numberIndex)
                        if (!blockedNumber.isNullOrEmpty()) {
                            blockedNumbers.add(normalizePhoneNumber(blockedNumber))
                        }
                    }
                }
            }


            val uniqueContacts = mutableSetOf<String>()  // Set to store unique phone numbers
            val cursor = context.contentResolver.query(
                ContactsContract.Data.CONTENT_URI,
                arrayOf(
                    ContactsContract.Contacts.DISPLAY_NAME, // Name
                    ContactsContract.CommonDataKinds.Phone.NUMBER, // Phone number
                    // ContactsContract.CommonDataKinds.Event.START_DATE, // יום ההולדת נמצא כאן
                    ContactsContract.Contacts.PHOTO_URI, // Photo URI
                    ContactsContract.Contacts.STARRED, // Starred status (1 or 0)
                ),
                "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER} = 1",
                arrayOf(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC" // Sort by name
            )

            cursor?.use {
                while (it.moveToNext()) {
                    val contactName =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val phoneNumber =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val photoUri =
                        it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI))
                    val photoUriStr = if (photoUri !== null) photoUri else ""
                    val isStarred =
                        it.getInt(it.getColumnIndexOrThrow(ContactsContract.Contacts.STARRED)) == 1
                    // val birthdayLong =
                    //  it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE))
                    //val birthday = formatTimestamp(birthdayLong)

                    val contact =
                        ContactsInLetterListItem.Contact(
                            phoneNumber,
                            contactName,
                            null,
                            isStarred,
                            photoUriStr
                        )

                    val shortPhoneNumber = phoneNumber.filter { it.isDigit() }

                    val isBlocked = blockedNumbers.contains(shortPhoneNumber)

                    if (!isBlocked) {
                        if (uniqueContacts.add(contactName + shortPhoneNumber)) {  // Adds only unique Contact + phone number
                            contactsList.add(contact)
                        }
                    }
                    // println("Contact: $name, Phone: $phoneNumber, Photo: $photoUri, Starred: $isStarred")
                }
            }
        } catch (error: Error) {
            Log.e("SimplyCall - MainActivity", "ContactsFragment loadContacts Error ($error)")
        }

        return contactsList
    }
}