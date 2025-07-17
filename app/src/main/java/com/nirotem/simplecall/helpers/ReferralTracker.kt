package com.nirotem.simplecall.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAlreadyPurchasedInStore
import com.nirotem.simplecall.helpers.SharedPreferencesCache.loadAlreadyRegisteredInStore
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAlreadyPurchasedInStore
import com.nirotem.simplecall.helpers.SharedPreferencesCache.saveAlreadyRegisteredInStore
import java.util.UUID

/*מנגנון מעקב הפניות (Referral Tracker), כלומר:

מזהה אם מישהו התקין את האפליקציה דרך לינק עם פרמטרים.

שומר את זה ב־Firestore.

מסמן אם אותו יוזר ביצע רכישה, גם כן מעדכן ב־Firestore.

איך זה עובד בפועל:
פונקציה עיקרית:
handleDeepLinkAndTrack(context, intent)

בודקת אם המשתמש כבר רשום (alreadyRegisteredInStore).

אם לא:

קוראת את הפרמטרים מה-URL (referrer, app_id).

בודקת ב־Firestore אם referrer_id קיים.

בודקת אם app_id קיים.

אם שניהם קיימים, מוסיפה רשומה חדשה ל־install_referrals עם מזהה ייחודי (install_id).

אם לא, שומרת את ההפניה כלא תקינה ב־invalid_referrals.*/

object ReferralTracker {
    private const val PREFS_NAME = "referral"
    private const val KEY_INSTALL_ID = "install_id"
    private const val KEY_REFERRER_ID = "referrer_id"

    fun handleDeepLinkAndTrack(context: Context, intent: Intent?) {
        val alreadyRegisteredInStore = loadAlreadyRegisteredInStore(context)

        if (!alreadyRegisteredInStore) {
            saveAlreadyRegisteredInStore(context, true)

            val data = intent?.data
            val referrerId = data?.getQueryParameter("referrer") ?: return
            val appId = data.getQueryParameter("app_id") ?: return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_INSTALL_ID)) return

            val installId = UUID.randomUUID().toString()
            val db = FirebaseFirestore.getInstance()

            // בדיקת קיום referrer_id
            db.collection("referrers").document(referrerId).get()
                .addOnSuccessListener { referrerDoc ->
                    if (!referrerDoc.exists()) {
                        saveInvalidReferral(db, appId, referrerId, installId, "referrer_not_found")
                        return@addOnSuccessListener
                    }

                    // בדיקת קיום app_id
                    db.collection("apps").document(appId).get()
                        .addOnSuccessListener { appDoc ->
                            if (!appDoc.exists()) {
                                saveInvalidReferral(db, appId, referrerId, installId, "app_id_not_found")
                                return@addOnSuccessListener
                            }

                            // שניהם קיימים – שמור בהצלחה
                            prefs.edit()
                                .putString(KEY_INSTALL_ID, installId)
                                .putString(KEY_REFERRER_ID, referrerId)
                                .apply()

                            val referralData = hashMapOf(
                                "referrer_id" to referrerId,
                                "referred_user_id" to installId,
                                "app_id" to appId,
                                "install_date" to FieldValue.serverTimestamp(),
                                "status" to "pending"
                            )

                            db.collection("install_referrals")
                                .add(referralData)
                                .addOnSuccessListener {
                                    Log.d("Referral", "Saved referral: $referrerId → $installId")
                                }
                                .addOnFailureListener {
                                    Log.e("Referral", "Failed to save referral", it)
                                }
                        }
                        .addOnFailureListener {
                            Log.e("Referral", "Error checking app_id", it)
                        }
                }
                .addOnFailureListener {
                    Log.e("Referral", "Error checking referrer_id", it)
                }
        }
    }

    private fun saveInvalidReferral(
        db: FirebaseFirestore,
        appId: String,
        referrerId: String,
        referredUserId: String,
        reason: String
    ) {
        val invalidReferralData = hashMapOf(
            "referrer_id" to referrerId,
            "referred_user_id" to referredUserId,
            "app_id" to appId,
            "referral_date" to FieldValue.serverTimestamp(),
            "reason" to reason
        )

        db.collection("invalid_referrals")
            .add(invalidReferralData)
            .addOnSuccessListener {
                Log.w("Referral", "Saved invalid referral ($reason): $referrerId")
            }
            .addOnFailureListener {
                Log.e("Referral", "Failed to save invalid referral", it)
            }
    }


    fun markPurchase(context: Context) {
        val alreadyPurchasedInStore = loadAlreadyPurchasedInStore(context)

        if (!alreadyPurchasedInStore) {
            saveAlreadyPurchasedInStore(context, true)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val installId = prefs.getString(KEY_INSTALL_ID, null)

            if (installId == null) {
                Log.e("Purchase", "No install ID found in SharedPreferences")

                // שמירה לאוסף שגיאות
                saveInvalidReferralOnPurchase(
                    reason = "missing_install_id",
                    referredUserId = "unknown",
                    referrerId = "unknown",
                    appId = "unknown"
                )
                return
            }

            val db = FirebaseFirestore.getInstance()
            db.collection("install_referrals")
                .whereEqualTo("referred_user_id", installId)
                .get()
                .addOnSuccessListener { result ->
                    if (result.isEmpty) {
                        Log.w("Purchase", "No referral found for install ID: $installId")

                        saveInvalidReferralOnPurchase(
                            reason = "referral_not_found",
                            referredUserId = installId,
                            referrerId = "unknown",
                            appId = "unknown"
                        )
                        return@addOnSuccessListener
                    }

                    for (doc in result) {
                        val referrerId = doc.getString("referrer_id") ?: "unknown"
                        val appId = doc.getString("app_id") ?: "unknown"

                        db.collection("install_referrals").document(doc.id)
                            .update(
                                mapOf(
                                    "status" to "paid",
                                    "payment_timestamp" to FieldValue.serverTimestamp()
                                )
                            )
                            .addOnSuccessListener {
                                Log.d("Purchase", "Marked purchase for $installId")
                            }
                            .addOnFailureListener {
                                Log.e("Purchase", "Failed to update to paid", it)
                                saveInvalidReferralOnPurchase(
                                    reason = "update_failed",
                                    referredUserId = installId,
                                    referrerId = referrerId,
                                    appId = appId
                                )
                            }
                    }
                }
                .addOnFailureListener {
                    Log.e("Purchase", "Error querying install_referrals", it)
                    saveInvalidReferralOnPurchase(
                        reason = "query_failed",
                        referredUserId = installId,
                        referrerId = "unknown",
                        appId = "unknown"
                    )
                }
        }
    }

    private fun saveInvalidReferralOnPurchase(
        reason: String,
        referredUserId: String,
        referrerId: String,
        appId: String
    ) {
        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf(
            "reason" to reason,
            "referrer_id" to referrerId,
            "referred_user_id" to referredUserId,
            "app_id" to appId,
            "referral_date" to FieldValue.serverTimestamp()
        )

        db.collection("invalid_referrals")
            .add(data)
            .addOnSuccessListener {
                Log.w("Purchase", "Logged invalid referral during purchase ($reason)")
            }
            .addOnFailureListener {
                Log.e("Purchase", "Failed to log invalid referral", it)
            }
    }

}