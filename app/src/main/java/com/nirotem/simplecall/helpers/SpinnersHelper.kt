package com.nirotem.simplecall.helpers

import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context

import android.view.View

import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.DBHelper.fetchContacts
import com.nirotem.simplecall.helpers.DBHelper.fetchContactsOptimized

import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.PermissionsStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object SpinnersHelper {
    // משתנה פנימי לשמירת רשימת אנשי הקשר (אם נטענו בעבר)
    var contactsList: MutableList<String>? = null


    suspend fun loadContactsAndEmergencyIntoList(

        emergencyNumbersList: List<String>,
        context: Context,
        anchorView: View,
        shouldAddEmergencyNumbersToList: Boolean
    ): MutableList<String> {
        // אם רשימת אנשי הקשר טרם נטענה ויש הרשאה לקריאה – טוענים ברקע
        if (contactsList == null && PermissionsStatus.readContactsPermissionGranted.value == true) {
            contactsList = withContext(Dispatchers.IO) {
                if (PermissionsStatus.defaultDialerPermissionGranted.value == true) {
                    fetchContactsOptimized(context).toMutableList()
                } else {
                    fetchContacts(context).toMutableList()
                }
            }
        }

        // אם אין הרשאה לקריאת אנשי קשר – מציגים הודעת Snackbar
        if (PermissionsStatus.readContactsPermissionGranted.value != true) {
            val toastMsg =
                context.getString(R.string.to_add_contacts_to_the_list_app_must_be_default)
            showLongSnackBar(context, toastMsg, null, anchorView)
        }

        val contactsWithEmergencyNumbers = mutableListOf<String>()

        // מוסיפים את אנשי הקשר (אם קיימים) לרשימה
        contactsList?.let { list ->
            contactsWithEmergencyNumbers.addAll(list)
        }

        // מוסיפים את רשימת המספרים של החירום בתחילת הרשימה
        if (shouldAddEmergencyNumbersToList) {
            contactsWithEmergencyNumbers.addAll(0, emergencyNumbersList)
        }

        // Add not now item at position 0:
        contactsWithEmergencyNumbers.add(0, "") // context.getString(R.string.not_now_capital))


        return contactsWithEmergencyNumbers
    }
}
