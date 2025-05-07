package com.example.callsreportslibrary

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
//import com.nirotem.simplecall.statuses.SettingsStatus

object SharedPreferencesCache {
    const val CALLS_REPORT_LIB_SHARED_PREF_APP = "CallsReportLibPreferences"

    // Function to save phone number in SharedPreferences
     private fun saveVariableInMemory(context: Context, varToSave: String, valToSave: String?) {
        // Obtain SharedPreferences instance
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(CALLS_REPORT_LIB_SHARED_PREF_APP, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save the phone number with a key
        editor.putString(varToSave, valToSave)
        editor.apply() // Apply changes asynchronously
    }

    // Function to save in SharedPreferences
    private fun loadVariableFromMemory(variableToLoad: String, context: Context?): String? {
        try {
            if (context != null) {
                val sharedPreferences: SharedPreferences = context.getSharedPreferences(
                    CALLS_REPORT_LIB_SHARED_PREF_APP,
                    Context.MODE_PRIVATE
                )
                return sharedPreferences.getString(variableToLoad, null)
            }
        }
        catch (e: Exception) {
            Log.e("callsreportslibrary - loadVariableFromMemory", "Error on loadVariableFromMemory ($e)")
        }
        return null
    }

    private fun loadCallsReportLastSentDateLib(context: Context?): String? {
        return loadVariableFromMemory(
            "last_calls_report_send_date",
            context
        )
    }

    fun loadCallsReportNumberLib(context: Context?): String? {
        return loadVariableFromMemory(
            "calls_report_phone_number",
            context
        )
    }

    fun loadCallsReportContactLib(context: Context?): String? {
        return loadVariableFromMemory(
            "calls_report_contactName",
            context
        )
    }

    fun loadCallsReportIsGoldNumberLib(context: Context): String? {
        var callsReportIsGoldNumber =
            loadVariableFromMemory(
                "calls_report_is_gold_number",
                context
            )

        return callsReportIsGoldNumber // we just return the raw itself and don't make decisions here
/*        if (callsReportIsGoldNumber.isNullOrEmpty()) {
            return false // we can't start with calls report sending on (in case it's gold number it will be on)
        }
        else {
            return callsReportIsGoldNumber == "true"
        }*/
    }

    fun saveCallsReportNumberLib(callsReportPhoneNumber: String?, context: Context) {
        saveVariableInMemory(
            context,
            "calls_report_phone_number",
            callsReportPhoneNumber
        )
    }

    fun saveCallsReportContactLib(callsReportContact: String?, context: Context) {

        saveVariableInMemory(
            context,
            "calls_report_contactName",
            callsReportContact
        )
    }

    fun saveCallsReportIsGoldNumberLib(callsReportIsGoldNumber: Boolean, context: Context) {

        val valToSave = if (callsReportIsGoldNumber) "true" else "false"
        saveVariableInMemory(
            context,
            "calls_report_is_gold_number",
            valToSave
        )
    }

}
