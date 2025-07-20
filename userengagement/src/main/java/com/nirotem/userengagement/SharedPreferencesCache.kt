package com.nirotem.userengagement

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object SharedPreferencesCache {
    const val EASY_CALL_AND_ANSWER_SHARED_FILE = "SimpleCallUserEngagementSharePreferences"

    // Function to save phone number in SharedPreferences
    fun saveVariableInMemory(context: Context, varToSave: String, valToSave: String?) {
        // Obtain SharedPreferences instance
        val sharedPrefApp = EASY_CALL_AND_ANSWER_SHARED_FILE // if (sharedPrefApp != null) sharedPrefApp else "SimpleCallPreferences"
        val sharedPreferences: SharedPreferences = context.getSharedPreferences(sharedPrefApp, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Save the phone number with a key
        editor.putString(varToSave, valToSave)
        editor.apply() // Apply changes asynchronously
    }

    // Function to save in SharedPreferences
    fun loadVariableFromMemory(variableToLoad: String, context: Context?): String? {
        try {
            if (context != null) {
                val sharedPrefApp = EASY_CALL_AND_ANSWER_SHARED_FILE // if (sharedPrefApp != null) sharedPrefApp else "SimpleCallPreferences"
                val sharedPreferences: SharedPreferences = context.getSharedPreferences(
                    sharedPrefApp,
                    Context.MODE_PRIVATE
                )
                return sharedPreferences.getString(variableToLoad, null)
            }
        }
        catch (e: Exception) {
            Log.e("Simple Call - SharedPreferencesCache - Subscription", "Error on loadVariableFromMemory ($e)")
        }
        return null
    }

    fun loadNumOfAskingUserToRateApp(context: Context): Int {
        val numOfAskingUserToRateApp =
            loadVariableFromMemory(
                "num_of_asking_user_to_rate_app",
                context
            )
        if (numOfAskingUserToRateApp.isNullOrEmpty()) {
            return 0
        }
        return numOfAskingUserToRateApp.toInt()
    }

    fun saveNumOfAskingUserToRateApp(context: Context, numOfAskingUserToRateApp: Int) {
        saveVariableInMemory(
            context,
            "num_of_asking_user_to_rate_app",
            numOfAskingUserToRateApp.toString()
        )
    }

    fun loadLastUserAnswerToRateApp(context: Context): Int {
        val lastUserAnswerToRateApp =
            loadVariableFromMemory(
                "last_user_answer_to_rate_app",
                context
            )
        if (lastUserAnswerToRateApp.isNullOrEmpty()) {
            return 0
        }
        return lastUserAnswerToRateApp.toInt()
    }

    fun saveLastUserAnswerToRateApp(context: Context, lastUserAnswerToRateApp: Int) {
        saveVariableInMemory(
            context,
            "last_user_answer_to_rate_app",
            lastUserAnswerToRateApp.toString()
        )
    }

    fun loadLastDateAskedToRateApp(context: Context): Long {
        val dateStr = SharedPreferencesCache.loadVariableFromMemory(
            "last_date_asked_to_rate_app",
            context
        )

        if (!dateStr.isNullOrEmpty()) {
            // נניח שהתאריך נשמר כ: "2024-06-28" או "2024-06-28T14:30:00"
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
            )

            for (format in formats) {
                try {
                    val sdf = SimpleDateFormat(format, Locale.getDefault())
                    sdf.timeZone = TimeZone.getDefault()
                    val date = sdf.parse(dateStr)
                    if (date != null) return date.time
                } catch (e: Exception) {
                    // Ignore and try next format
                }
            }

        }
        return 0L // תאריך לא תקין או ריק → נחזיר 0 כדי לאפשר הצגה ראשונה
    }

    fun saveCurrentDateAskedToRateApp(context: Context) {
        val now = Date()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        val dateStr = sdf.format(now)

        saveVariableInMemory(
            context,
            "last_date_asked_to_rate_app",
            dateStr
        )
    }
}
