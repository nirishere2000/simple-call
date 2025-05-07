package com.nirotem.simplecall.helpers

import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

fun isAppBatteryOptimizationIgnored(context: Context, appPackageName: String): Boolean {
    val tag = "SimplyCall - DeviceHelper"

    try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val standbyBucket = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            usageStatsManager.appStandbyBucket
        } else {
            null
        }

        return powerManager.isIgnoringBatteryOptimizations(appPackageName) || standbyBucket == 5 || standbyBucket == 10
    }
    catch (e: Exception) {
        Log.e(tag, "isAppBatteryOptimizationIgnored Error (${e.message})")
    }

    return true // we prefer not to trouble the user if we don't know
}