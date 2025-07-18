package com.nirotem.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

object SharedPreferencesCache {
    const val EASY_CALL_AND_ANSWER_SHARED_FILE = "SimpleCallSubscriptionSharePreferences"

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

    fun saveAccessTokenId(accessTokenId: String, context: Context) {
        saveVariableInMemory(context, "access_token_id", accessTokenId)
    }

    fun loadAccessTokenId(context: Context?): String? {
        return loadVariableFromMemory("access_token_id", context)
    }
}
