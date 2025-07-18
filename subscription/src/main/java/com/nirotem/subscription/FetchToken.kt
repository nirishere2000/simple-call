package com.nirotem.subscription

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

object FetchToken {
    fun fetchAndValidateToken(appIdBasic: String, appIdPremium: String, context: Context, code: String, alreadySaved: Boolean, onResult: (AccessToken?, String?) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("access_tokens")
            .whereEqualTo("token", code)
            .whereIn("app_id", listOf(appIdBasic, appIdPremium))
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onResult(null, context.getString(R.string.promo_dialog_token_does_not_exist))
                    return@addOnSuccessListener
                }

                val doc = result.documents.first()

                val active = doc.getBoolean("is_active") ?: false
                val maxUses = doc.getLong("max_uses_per_token") ?: 0
                val tokensAlreadyUsed = doc.getLong("num_of_tokens_already_used") ?: 0
                val expiresAt = doc.getDate("expires_at")
                val now = Date()

                if (!active) {
                    onResult(null, context.getString(R.string.promo_dialog_token_not_active))
                    return@addOnSuccessListener
                }

                // If Token already saved once no need to check how much tokens left, it was already ok
                if (!alreadySaved && tokensAlreadyUsed >= maxUses) {
                    onResult(null, context.getString(R.string.promo_dialog_code_already_used_too_many_times))
                    return@addOnSuccessListener
                }

                if (expiresAt != null && now.after(expiresAt)) {
                    onResult(null, context.getString(R.string.promo_dialog_expired))
                    return@addOnSuccessListener
                }

                val usedByRaw: List<*> = doc.get("used_by") as? List<*> ?: emptyList<Any?>()
                val usedBy = usedByRaw.filterIsInstance<String>()
                val accessType = doc.getString("access_type") ?: "basic"

                val accessToken = AccessToken(
                    token = doc.getString("token") ?: "",
                    maxUses = maxUses,
                    tokensAlreadyUsed = tokensAlreadyUsed,
                    createdAt = doc.getDate("created_at") ?: now,
                    expiresAt = expiresAt ?: now,
                    active = active,
                    usedBy = usedBy,
                    accessType = accessType,
                    appId = doc.getString("app_id") ?: appIdBasic
                )

                // עדכון המספר בפיירסטור
                if (!alreadySaved) {
                    db.collection("access_tokens").document(doc.id)
                        .update("num_of_tokens_already_used", FieldValue.increment(1))
                        .addOnSuccessListener {
                            Log.d("SimplyCall - fetchAndValidateToken", "tokens_already_used updated")
                        }
                        .addOnFailureListener {
                            Log.d("SimplyCall - fetchAndValidateToken", "tokens_already_used update FAILED!")
                        }
                }

                onResult(accessToken, null)
            }
            .addOnFailureListener { e ->
                onResult(null, e.localizedMessage)
            }
    }
}
