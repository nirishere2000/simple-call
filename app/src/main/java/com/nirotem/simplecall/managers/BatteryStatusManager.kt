package com.nirotem.simplecall.managers



import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log

object BatteryStatusManager {

    // כמה זמן המכשיר נחשב "איידל" (ללא פעילות) במילישניות
    private const val DEFAULT_IDLE_THRESHOLD = 2 * 60 * 1000L // 2 דקות

    /**
     * האם יש צורך להציג את מסך הנעילה?
     * נבדק אם המכשיר איידל או שהסוללה נמוכה
     */
    fun shouldShowLockScreen(context: Context): Boolean {
        val idle = isIdleLongEnough(context)
        val battery = getBatteryPercentage(context)
        val lowBattery = battery in 1..15

        Log.d("DEVICE_STATUS", "Idle=$idle, Battery=$battery%, LowBattery=$lowBattery")

        return idle || lowBattery
    }

    /**
     * מחשב מתי בפעם האחרונה הייתה פעילות באפליקציה כלשהי
     */
    fun getLastUserInteractionTime(context: Context): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as UsageStatsManager

        val now = System.currentTimeMillis()
        val oneHourAgo = now - 1000 * 60 * 60

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            oneHourAgo,
            now
        )

        val lastUsedApp = stats.maxByOrNull { it.lastTimeUsed }
        return lastUsedApp?.lastTimeUsed ?: now
    }

    /**
     * האם עבר מספיק זמן ללא פעילות כדי להיחשב "איידל"
     */
    fun isIdleLongEnough(context: Context, thresholdMillis: Long = DEFAULT_IDLE_THRESHOLD): Boolean {
        val now = System.currentTimeMillis()
        val lastUsed = getLastUserInteractionTime(context)
        val idleTime = now - lastUsed

        Log.d("DEVICE_STATUS", "Idle for ${idleTime / 1000} שניות")

        return idleTime > thresholdMillis
    }

    /**
     * מחזיר את אחוז הסוללה הנוכחי
     */
    fun getBatteryPercentage(context: Context): Int {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        return if (level >= 0 && scale > 0) (level * 100) / scale else -1
    }

    fun isCharging(context: Context): Boolean {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }
}
