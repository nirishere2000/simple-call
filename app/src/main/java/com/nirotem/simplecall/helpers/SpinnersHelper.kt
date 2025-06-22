package com.nirotem.simplecall.helpers

import android.Manifest.permission.READ_PHONE_STATE
import android.content.Context
import android.util.Log
import android.view.LayoutInflater

import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nirotem.lockscreen.managers.SharedPreferencesCache.CustomAppInfo

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

        Log.d("SimplyCall - SpinnerHelper", "loadContactsAndEmergencyIntoList - ${contactsList?.count()}")

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

        Log.d("SimplyCall - SpinnerHelper", "loadContactsAndEmergencyIntoList - ${contactsWithEmergencyNumbers.count()}")


        return contactsWithEmergencyNumbers
    }

    fun setupSpinnerWithIcons(spinner: Spinner, appList: List<CustomAppInfo>) {
        val adapter = object : ArrayAdapter<CustomAppInfo>(
            spinner.context,
            R.layout.spinner_item_with_icon,
            appList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                return createCustomView(position, convertView, parent, isDropDown = false)
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                return createCustomView(position, convertView, parent, isDropDown = true)
            }

            private fun createCustomView(position: Int, convertView: View?, parent: ViewGroup, isDropDown: Boolean): View {
                val inflater = LayoutInflater.from(context)
                val view = convertView ?: inflater.inflate(R.layout.spinner_item_with_icon, parent, false)

                val icon = view.findViewById<ImageView>(R.id.app_icon)
                val name = view.findViewById<TextView>(R.id.app_name)

                val appInfo = getItem(position)

                icon.setImageDrawable(appInfo?.icon)
                name.text = appInfo?.appName

                if (isDropDown) {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.white))
                }

                return view
            }
        }

        spinner.adapter = adapter
    }
}
