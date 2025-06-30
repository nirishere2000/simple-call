package com.nirotem.simplecall.ui

import android.app.Application

import android.util.Log
import android.widget.Toast
import com.google.firebase.crashlytics.FirebaseCrashlytics
//import com.example.callsreportslibrary.SharedPreferencesCache.CALLS_REPORT_LIB_SHARED_PREF_APP
import com.nirotem.simplecall.CallActivity
import com.nirotem.simplecall.R
import com.nirotem.simplecall.helpers.SharedPreferencesCache.EASY_CALL_AND_ANSWER_SHARED_FILE
import com.nirotem.simplecall.managers.MessageBoxManager.showCustomToastDialog
import java.io.File


class ApplicationManager : Application() {
    private val markerName = "restore_marker"

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Log the error, notify the user, or restart the app as needed

            try {
                // Log the thread information and the error message
                Log.e("GlobalExceptionHandler", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)

                // דיווח ל־Crashlytics
                FirebaseCrashlytics.getInstance().recordException(throwable)

                // You can also print the stack trace or handle the error further
                throwable.printStackTrace()

                /*            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(applicationContext, "An error occurred", Toast.LENGTH_SHORT).show()
                            }*/
                showCustomToastDialog(applicationContext, "${getString(R.string.oops_a_serious_error_occurred_please_restart_the_app)} (${throwable.message})")
               // Toast.makeText(applicationContext,
                //    "${getString(R.string.oops_a_serious_error_occurred_please_restart_the_app)} (${throwable.message})", Toast.LENGTH_LONG).show()
                /*            CallActivity.criticalErrorEventMessage = "Critical Error!"
                            CallActivity.criticalErrorEvent.value = true*/
            }
            catch (e: Exception) {

            }
        }

        if (isFreshInstall()) {
            // ניקוי כל SharedPreferences שהגיעו משחזור
            clearAllPreferences()
            createMarker()   // כדי שמרגע זה זה ייחשב "עדכון"
        }
    }

    /** true כשאין קובץ סימון בתיקיית noBackup → זו התקנה נקייה */
    private fun isFreshInstall(): Boolean {
        val marker = File(noBackupFilesDir, markerName)
        return !marker.exists()
    }

    private fun createMarker() {
        File(noBackupFilesDir, markerName).apply { writeText("1") }
    }

    private fun clearAllPreferences() {
        listOf(
            EASY_CALL_AND_ANSWER_SHARED_FILE,
            /*CALLS_REPORT_LIB_SHARED_PREF_APP*/
        ).forEach {
            getSharedPreferences(it, MODE_PRIVATE).edit().clear().apply()
        }
    }
}
