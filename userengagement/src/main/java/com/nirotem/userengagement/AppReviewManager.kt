package com.nirotem.userengagement

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.android.play.core.review.ReviewManagerFactory
import com.nirotem.userengagement.SharedPreferencesCache.loadLastDateAskedToRateApp
import com.nirotem.userengagement.SharedPreferencesCache.loadLastUserAnswerToRateApp
import com.nirotem.userengagement.SharedPreferencesCache.loadNumOfAskingUserToRateApp
import com.nirotem.userengagement.SharedPreferencesCache.saveCurrentDateAskedToRateApp
import com.nirotem.userengagement.SharedPreferencesCache.saveLastUserAnswerToRateApp
import com.nirotem.userengagement.SharedPreferencesCache.saveNumOfAskingUserToRateApp

object AppReviewManager {

    fun shouldShowRateAppDialog(activity: Activity): Boolean {
        val context = activity.applicationContext
        val numOfAskingUserToRateApp = loadNumOfAskingUserToRateApp(context)

        if (numOfAskingUserToRateApp == 0) { // don't show dialog immediately - it's not recommended -
            saveCurrentDateAskedToRateApp(activity) // save current date - to wait a before next ask and return false
            return false // don't increase num of rating asks
        }

        if (numOfAskingUserToRateApp < 3) { // should be 3
            val daysBetweenPrompts = 7
            val lastDateDialogShown = loadLastDateAskedToRateApp(context)
            val now = System.currentTimeMillis()
            val millisBetweenPrompts = daysBetweenPrompts * 24 * 60 * 60 * 1000L

            if ((now - lastDateDialogShown) > millisBetweenPrompts) {
                val lastAnswer = loadLastUserAnswerToRateApp(context)

                var shouldShow = when {
                    numOfAskingUserToRateApp == 0 -> true
                    lastAnswer == 1 -> true // Maybe later
                    lastAnswer == 2 && numOfAskingUserToRateApp == 1 -> true
                    else -> false
                }

                return shouldShow
            }
        }

        return false
    }

    fun letUserRateApp(
        activity: Activity,
        dialogTitle: String,
        dialogText: String,
        callbackAfterwards: (() -> Unit)? = null
    ) {
        val context = activity.applicationContext
        val numOfAskingUserToRateApp = loadNumOfAskingUserToRateApp(context)

        if (shouldShowRateAppDialog(activity)) {
            showRateAppDialog(
                activity,
                numOfAskingUserToRateApp,
                dialogTitle,
                dialogText,
                callbackAfterwards
            )
        }
    }

    private fun showRateAppDialog(
        activity: Activity,
        numOfAskingUserToRateApp: Int,
        dialogTitle: String,
        dialogText: String,
        callbackAfterwards: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(activity)
            .setTitle(dialogTitle)
            .setMessage(dialogText)
            .setPositiveButton(activity.getString(R.string.rate_the_app_dialog_answer_rate_now)) { dialog, _ ->
                saveLastAction(activity, 0, numOfAskingUserToRateApp)
                launchInAppReview(activity)
                dialog.dismiss()
                callbackAfterwards?.invoke()
            }
            .setNeutralButton(activity.getString(R.string.rate_the_app_dialog_answer_maybe_later)) { dialog, _ ->
                saveLastAction(activity, 1, numOfAskingUserToRateApp)
                dialog.dismiss()
                callbackAfterwards?.invoke()
            }
            .setNegativeButton(activity.getString(R.string.rate_the_app_dialog_answer_no_thanks)) { dialog, _ ->
                saveLastAction(activity, 2, numOfAskingUserToRateApp)
                dialog.dismiss()
                callbackAfterwards?.invoke()
            }
            .setOnCancelListener {
                // לא נחשב ניסיון אם נסגר בלי לחיצה
            }
            .show()
    }

    private fun saveLastAction(activity: Activity, action: Int, numOfAskingUserToRateApp: Int) {
        saveLastUserAnswerToRateApp(activity, action)
        saveNumOfAskingUserToRateApp(activity, numOfAskingUserToRateApp + 1)
        saveCurrentDateAskedToRateApp(activity)
    }

    private fun launchInAppReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                // נכשל בבקשה – מפנים לחנות
                openPlayStoreForRating(activity)
                return@addOnCompleteListener
            }

            val reviewInfo = task.result
            // מחלצים מתוך toString() את הערך של isNoOp באמצעות Regex
            val infoString = reviewInfo.toString()
            val isNoOp = Regex("isNoOp=(true|false)")
                .find(infoString)
                ?.groups
                ?.get(1)
                ?.value
                ?.toBoolean() ?: false

            if (isNoOp) {
                // Google בחרה לא להציג את הדיאלוג – מפנים לחנות
                openPlayStoreForRating(activity)
            } else {
                // סביר שהדיאלוג הוצג
                manager.launchReviewFlow(activity, reviewInfo)
                    .addOnCompleteListener {
                        // כאן אפשר להראות "תודה שהדרגת" או כל פידבק אחר
                    }
            }
        }
    }


    private fun openPlayStoreForRating(activity: Activity) {
        val packageName = activity.packageName
        val intent = try {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        } catch (e: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
        }
        activity.startActivity(intent)

        Toast.makeText(
            activity,
            activity.getString(R.string.review_redirect_play_store),
            Toast.LENGTH_LONG
        ).show()
    }
}
