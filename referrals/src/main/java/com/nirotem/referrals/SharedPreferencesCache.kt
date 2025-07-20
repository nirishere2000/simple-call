package com.nirotem.referrals

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * SharedPreferencesCache
 *
 * ניהול כל המשתנים המקומיים הקשורים להפניות, רכישות ומזהים.
 * משמש כ־API אחיד עבור ReferralTracker.
 */
object SharedPreferencesCache {

    private const val EASY_CALL_AND_ANSWER_SHARED_FILE = "SimpleCallAppReferralsSharePreferences"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(EASY_CALL_AND_ANSWER_SHARED_FILE, Context.MODE_PRIVATE)
    }

    private fun saveVariableInMemory(context: Context, key: String, value: String?) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().putString(key, value).apply()
        } catch (e: Exception) {
            Log.e("SharedPreferencesCache", "Error saving key $key: $e")
        }
    }

    private fun loadVariableFromMemory(context: Context, key: String): String? {
        return try {
            getPrefs(context).getString(key, null)
        } catch (e: Exception) {
            Log.e("SharedPreferencesCache", "Error loading key $key: $e")
            null
        }
    }

    fun saveInstallId(context: Context, installId: String) {
        saveVariableInMemory(context, "install_id", installId)
    }

    fun loadInstallId(context: Context): String? {
        return loadVariableFromMemory(context, "install_id")
    }

    fun saveReferrerId(context: Context, referrerId: String) {
        saveVariableInMemory(context, "referrer_id", referrerId)
    }

    fun loadReferrerId(context: Context): String? {
        return loadVariableFromMemory(context, "referrer_id")
    }

    fun saveAlreadyRegisteredInStore(context: Context, alreadyRegistered: Boolean) {
        saveVariableInMemory(context, "already_registered_in_store", alreadyRegistered.toString())
    }

    fun loadAlreadyRegisteredInStore(context: Context): Boolean {
        return loadVariableFromMemory(context, "already_registered_in_store") == "true"
    }

    fun saveAlreadyPurchasedInStore(context: Context, alreadyPurchased: Boolean) {
        saveVariableInMemory(context, "already_purchased_in_store", alreadyPurchased.toString())
    }

    fun loadAlreadyPurchasedInStore(context: Context): Boolean {
        return loadVariableFromMemory(context, "already_purchased_in_store") == "true"
    }
}
