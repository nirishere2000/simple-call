package com.nirotem.simplecall.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.DBHelper.getPhoneNumberFromContactName
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadQuickCallNumberContact
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallNumber
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveQuickCallNumberContact
import com.nirotem.simplecall.helpers.SpinnersHelper.loadContactsAndEmergencyIntoList
import com.nirotem.simplecall.managers.MessageBoxManager.showLongSnackBar
import com.nirotem.simplecall.statuses.PermissionsStatus
import kotlinx.coroutines.coroutineScope

object GeneralUtils {
    var distressButtonSpinnerClickEvent = MutableLiveData(false)

    // רשימת מספרי חירום לפי אזור
    val emergencyNumbersByRegion = mapOf(
        "IL" to listOf("100", "101", "102"),
        "US" to listOf("911"),
        "CA" to listOf("911"),
        "GB" to listOf("999", "112"),
        "AU" to listOf("000", "112"),
        "NZ" to listOf("111"),
        "IN" to listOf("112"),
        "SG" to listOf("995", "994", "999", "112"),
        "CN" to listOf("110", "120", "119"),
        "JP" to listOf("110", "119"),
        "DE" to listOf("110", "112"),
        "FR" to listOf("112", "15", "17", "18"),
        "ES" to listOf("112"),
        "IT" to listOf("112", "113", "118"),
        "RU" to listOf("112"),
        "BR" to listOf("190", "192", "193"),
        "MX" to listOf("911"),
        "ZA" to listOf("10111", "10177"),
        "KE" to listOf("112"),
        "NG" to listOf("112"),
        "AR" to listOf("911"),
        "CL" to listOf("131", "133"),
        "SE" to listOf("112"),
        "NO" to listOf("112"),
        "FI" to listOf("112"),
        "DK" to listOf("112"),
        "NL" to listOf("112"),
        "BE" to listOf("112"),
        "AT" to listOf("112"),
        "CH" to listOf("112", "117"),
        "PT" to listOf("112"),
        "PL" to listOf("112"),
        "GR" to listOf("112"),
        "TR" to listOf("112")
    )

    // משתנה פנימי לשמירת רשימת אנשי הקשר (אם נטענו בעבר)
    private var contactsList: MutableList<String>? = null

    /**
     * טוען באופן אסינכרוני את אנשי הקשר לתוך Spinner של מספרי חירום.
     *
     * @param spinner Spinner שבו יוצגו אנשי הקשר ומספרי החירום.
     * @param emergencyNumbersList רשימה של מספרי חירום (לדוגמה, ממפה לפי אזור).
     * @param context הקונטקסט בו אנו פועלים.
     * @param anchorView View שישמש לעיגון ההודעה (Snackbar), למשל ה-View העיקרי של הפרגמנט.
     * @param buttonNext כפתור שעליו מתעדכנים טקסטים בהתאם לבחירה.
     * @param coroutineScope Scope בו תתבצע ההרצה האסינכרונית.
     */
    suspend fun loadContactsIntoEmergencySpinnerAsync(
        spinner: Spinner,
        emergencyNumbersList: List<String>,
        context: Context,
        anchorView: View
    ) {
        val contactsWithEmergencyNumbers =
            loadContactsAndEmergencyIntoList(emergencyNumbersList, context, anchorView, true)

        // טוענים מספרי חירום שמורים (פונקציות אלו מניחות שקיימות במערכת)
        val emergencyPhoneNumber = loadQuickCallNumber(context)
        val emergencyPhoneNumberContact = loadQuickCallNumberContact(context)

        if (contactsWithEmergencyNumbers.isNotEmpty()) {
            // יוצרים ArrayAdapter מותאם אישית להצגת הנתונים
            val adapter = object : ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_item,
                contactsWithEmergencyNumbers
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setTextColor(Color.BLACK)
                    textView.gravity = Gravity.START
                    return view
                }

                override fun getDropDownView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {
                    val view = super.getDropDownView(position, convertView, parent)
                    val textView = view.findViewById<TextView>(android.R.id.text1)
                    textView.setBackgroundColor(Color.WHITE)
                    textView.setTextColor(Color.BLACK)
                    if (position == 0) {
                        // כאשר הרשימה נפתחת, במקום להציג את " (Click for selection)" מציגים טקסט ברור יותר
                        textView.text = "" // context.getString(R.string.not_now_capital)
                    }
                    return view
                }
            }
            spinner.adapter = adapter

            val contactsListIsNotEmpty = contactsWithEmergencyNumbers.isNotEmpty()
            spinner.isEnabled = contactsListIsNotEmpty

            // קביעת בחירה ברירת מחדל בהתאם למספר החירום אם קיים
            if (contactsListIsNotEmpty) {
                if (!emergencyPhoneNumber.isNullOrEmpty() &&
                    emergencyPhoneNumber != "Unknown" &&
                    emergencyPhoneNumber != context.getString(R.string.unknown_capital)
                ) {
                    val selectedItem = emergencyPhoneNumberContact ?: emergencyPhoneNumber
                    val spinnerPosition = adapter.getPosition(selectedItem)
                    if (spinnerPosition >= 0) {
                        spinner.setSelection(spinnerPosition)
                    } else {
                        val toastMsg = if (emergencyPhoneNumberContact != null) {
                            if (PermissionsStatus.readContactsPermissionGranted.value == true)
                                context.getString(R.string.unable_to_display_selection_unexpected_error)
                            else
                                context.getString(R.string.cannot_display_selection_contacts_permission_required)
                        } else {
                            if (ContextCompat.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.READ_PHONE_STATE
                                )
                                != PackageManager.PERMISSION_GRANTED
                            )
                                context.getString(R.string.cannot_show_selection_app_must_be_default)
                            else
                                context.getString(R.string.unable_to_display_selection_unexpected_error)
                        }
                        showLongSnackBar(context, toastMsg, null, anchorView)
                        spinner.setSelection(0)
                    }
                } else {

                    spinner.setSelection(0)
                }
            } else {

            }

            // מאזין לבחירות ב-Spinner ומעדכן את הכפתור בהתאם
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedItem = parent.getItemAtPosition(position).toString()

                    if (position >= 0) {
                        when {
                            position == 0 -> {
                                // המשתמש בחר "לא עכשיו"
                                handleEmergencySelectContact(context, null, false)

                            }

                            position <= emergencyNumbersList.size -> {
                                handleEmergencySelectContact(context, selectedItem, false)

                            }

                            else -> {
                                handleEmergencySelectContact(context, selectedItem, true)

                            }
                        }
                    } else {

                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // אין צורך בטיפול כאן
                }
            }


        }

    }

    fun handleEmergencySelectContact(
        context: Context,
        selectedPhoneItem: Any?,
        phoneHasContact: Boolean,
        anchorView: View? = null
    ) {
        /*        val selectedGoldPhoneNumber = if (PermissionsStatus.defaultDialerPermissionGranted.value == true)
                    getPhoneNumberFromContactNameAndFilterBlocked(context, selectedContactName.toString()) else
                getPhoneNumberFromContactName(context, selectedContactName.toString())*/

        // We should not have blocked number and we could create an error is we'll return unknown here
        if (selectedPhoneItem == null) { // user chose NOT NOW
            saveQuickCallNumberContact(null, context)
            saveQuickCallNumber(null, context)
        } else if (phoneHasContact) {
            val selectedEmergencyNumberContact = selectedPhoneItem.toString()
            var selectedEmergencyPhoneNumber =
                getPhoneNumberFromContactName(context, selectedEmergencyNumberContact)

            if (selectedEmergencyPhoneNumber == "Unknown" || selectedEmergencyPhoneNumber == context.getString(
                    R.string.unknown_capital
                )
            ) {
                selectedEmergencyPhoneNumber =
                    selectedPhoneItem.toString() // Not a Contact, but local emergency number
            }
            saveQuickCallNumberContact(selectedEmergencyNumberContact, context)
            saveQuickCallNumber(selectedEmergencyPhoneNumber, context)
        } else { // Emergency number - save Contact as null
            saveQuickCallNumberContact(null, context)
            saveQuickCallNumber(selectedPhoneItem.toString(), context)
        }

        val existsDistressNumberForDistressButtonButWithoutPermission = (selectedPhoneItem != null) && (PermissionsStatus.callPhonePermissionGranted.value != true)
        if (existsDistressNumberForDistressButtonButWithoutPermission) {
            var toastMsg =
                context.getString(R.string.phone_permission_required_for_quick_call)
            //Snackbar.make(fragmentView, toastMsg, 8000).show()
            showLongSnackBar(context, toastMsg, null, anchorView)
        }
        distressButtonSpinnerClickEvent.value = true
    }
}
