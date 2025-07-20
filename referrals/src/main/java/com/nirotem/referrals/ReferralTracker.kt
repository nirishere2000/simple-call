package com.nirotem.referrals

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nirotem.referrals.SharedPreferencesCache
import java.util.UUID

object ReferralTracker {

    /**
     * Initialize referral tracking:
     * 1. Checks if user already registered locally.
     * 2. If not, tries deep link first.
     * 3. If no deep link, tries Install Referrer API.
     */
    fun initialize(context: Context, intent: Intent?) {
        if (SharedPreferencesCache.loadAlreadyRegisteredInStore(context)) {
            Log.d("Referral", "Already registered. Skipping initialization.")
            return
        }

        handleDeepLink(context, intent) { handled ->
            if (!handled) {
                handleInstallReferrer(context)
            }
        }
    }

    private fun handleDeepLink(context: Context, intent: Intent?, callback: (Boolean) -> Unit) {
        val data = intent?.data
        val referrerId = data?.getQueryParameter("referrer")
        val appId = data?.getQueryParameter("app_id")

        if (!referrerId.isNullOrBlank() && !appId.isNullOrBlank()) {
            processReferral(context, referrerId, appId, "deep_link")
            callback(true)
        } else {
            callback(false)
        }
    }

    private fun handleInstallReferrer(context: Context) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    val referrerUrl = referrerClient.installReferrer.installReferrer
                    val uri = Uri.parse("https://dummy?$referrerUrl")
                    val referrerId = uri.getQueryParameter("referrer")
                    val appId = uri.getQueryParameter("app_id")

                    if (!referrerId.isNullOrBlank() && !appId.isNullOrBlank()) {
                        processReferral(context, referrerId, appId, "install_referrer")
                    }

                    referrerClient.endConnection()
                } else {
                    Log.e("InstallReferrer", "Failed with code: $responseCode")
                }
            }

            override fun onInstallReferrerServiceDisconnected() {}
        })
    }

    /**
     * Main referral processing function:
     * - Checks if already registered.
     * - Validates referrer_id and app_id exist in Firestore.
     * - Saves referral or logs as invalid.
     */
    private fun processReferral(context: Context, referrerId: String, appId: String, source: String) {
        val existingInstallId = SharedPreferencesCache.loadInstallId(context)
        if (existingInstallId != null) {
            Log.d("Referral", "Install ID already exists ($existingInstallId). Skipping save.")
            return
        }

        val installId = UUID.randomUUID().toString()
        val db = FirebaseFirestore.getInstance()

        db.collection("referrers").document(referrerId).get()
            .addOnSuccessListener { referrerDoc ->
                if (!referrerDoc.exists()) {
                    saveInvalidReferral(context, appId, referrerId, installId, "referrer_not_found")
                    return@addOnSuccessListener
                }

                db.collection("apps").document(appId).get()
                    .addOnSuccessListener { appDoc ->
                        if (!appDoc.exists()) {
                            saveInvalidReferral(context, appId, referrerId, installId, "app_id_not_found")
                            return@addOnSuccessListener
                        }

                        SharedPreferencesCache.saveInstallId(context, installId)
                        SharedPreferencesCache.saveReferrerId(context, referrerId)
                        SharedPreferencesCache.saveAlreadyRegisteredInStore(context, true)

                        val data = hashMapOf(
                            "referrer_id" to referrerId,
                            "referred_user_id" to installId,
                            "app_id" to appId,
                            "install_date" to FieldValue.serverTimestamp(),
                            "install_source" to source,
                            "status" to "pending"
                        )

                        db.collection("install_referrals")
                            .document(installId)
                            .set(data)
                            .addOnSuccessListener {
                                Log.d("Referral", "Saved referral with installId: $installId")
                            }
                            .addOnFailureListener {
                                saveInvalidReferral(context, appId, referrerId, installId, "save_failed")
                            }
                    }
                    .addOnFailureListener {
                        saveInvalidReferral(context, appId, referrerId, installId, "app_check_failed")
                    }
            }
            .addOnFailureListener {
                saveInvalidReferral(context, appId, referrerId, installId, "referrer_check_failed")
            }
    }

    private fun saveInvalidReferral(
        context: Context,
        appId: String,
        referrerId: String,
        referredUserId: String,
        reason: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf(
            "referrer_id" to referrerId,
            "referred_user_id" to referredUserId,
            "app_id" to appId,
            "referral_date" to FieldValue.serverTimestamp(),
            "reason" to reason
        )

        db.collection("invalid_referrals")
            .add(data)
            .addOnSuccessListener {
                Log.w("Referral", "Saved invalid referral ($reason): $referrerId")
            }
            .addOnFailureListener {
                Log.e("Referral", "Failed to save invalid referral", it)
            }
    }

    /**
     * Mark purchase for the current user:
     * Updates the Firestore document status to 'paid'.
     */
    fun markPurchase(context: Context) {
        if (SharedPreferencesCache.loadAlreadyPurchasedInStore(context)) {
            Log.d("Purchase", "Already marked as purchased. Skipping.")
            return
        }

        val installId = SharedPreferencesCache.loadInstallId(context)
        if (installId == null) {
            Log.e("Purchase", "Missing install ID. Cannot mark purchase.")
            return
        }

        SharedPreferencesCache.saveAlreadyPurchasedInStore(context, true)

        val db = FirebaseFirestore.getInstance()
        db.collection("install_referrals")
            .document(installId)
            .update(
                "status", "paid",
                "payment_timestamp", FieldValue.serverTimestamp()
            )
            .addOnSuccessListener {
                Log.d("Purchase", "Marked purchase for $installId")
            }
            .addOnFailureListener {
                saveInvalidReferral(context, "unknown", "unknown", installId, "purchase_update_failed")
            }
    }
}
