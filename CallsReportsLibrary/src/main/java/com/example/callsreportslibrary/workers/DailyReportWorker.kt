package com.example.callsreportslibrary.workers

import android.content.Context
import android.provider.CallLog
import android.telephony.SmsManager
import android.util.Log
import androidx.work.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class ReportCallData(
    val contactName: String,
    val phoneNumber: String,
    val callType: Int,
    val callDate: Date,
    val duration: Long
)

class DailyReportWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val targetNumber = inputData.getString("target_number")
            ?: return Result.failure()

        val logs = getCallLogs(applicationContext)
        val report = generateCallReport(logs)

        sendSms(targetNumber, report)

        // תזמון מחדש לעוד 24 שעות
        scheduleNextRun(applicationContext, targetNumber)

        Log.d("SimplyCall - DailyReportWorker", "doWork - Worker is running and sending report!")

        return Result.success()
    }

    private fun scheduleNextRun(context: Context, phoneNumber: String) {
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply {

            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val delay = due.timeInMillis - now.timeInMillis

        val data = workDataOf("target_number" to phoneNumber)

        val request = OneTimeWorkRequestBuilder<DailyReportWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    fun startDailyReport(context: Context, phoneNumber: String) {
        scheduleNextRun(context, phoneNumber)
    }

    private fun getCallLogs(context: Context): List<ReportCallData> {
        val callLogList = mutableListOf<ReportCallData>()
        val resolver = context.contentResolver

        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)

            while (it.moveToNext()) {
                val number = it.getString(numberIndex)
                val type = it.getInt(typeIndex)
                val date = Date(it.getLong(dateIndex))
                val duration = it.getLong(durationIndex)
                val name = it.getString(nameIndex) ?: "לא ידוע"

                callLogList.add(ReportCallData(name, number, type, date, duration))
            }
        }

        return callLogList
    }

    private fun generateCallReport(callLogs: List<ReportCallData>): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val outgoing = callLogs.filter { it.callType == CallLog.Calls.OUTGOING_TYPE }
        val incoming = callLogs.filter { it.callType == CallLog.Calls.INCOMING_TYPE }

        val report = StringBuilder()
        report.append("📞 דו\"ח שיחות\n\n")

        report.append("🔹 שיחות יוצאות:\n")
        outgoing.forEach {
            report.append("${sdf.format(it.callDate)} - ${it.phoneNumber} (${it.contactName}) - ${it.duration} שניות\n")
        }

        report.append("\n🔹 שיחות נכנסות:\n")
        incoming.forEach {
            report.append("${sdf.format(it.callDate)} - ${it.phoneNumber} (${it.contactName}) - ${it.duration} שניות\n")
        }

        val anomalies = callLogs.filter {
            val cal = Calendar.getInstance().apply { time = it.callDate }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour < 6 || hour > 23 || callLogs.count { c -> c.phoneNumber == it.phoneNumber } > 5
        }

        if (anomalies.isNotEmpty()) {
            report.append("\n⚠️ מקרים חריגים:\n")
            anomalies.forEach {
                report.append("${sdf.format(it.callDate)} - ${it.phoneNumber} (${it.contactName}) - ${it.duration} שניות\n")
            }
        }

        Log.d("SimplyCall - DailyReportWorker", "generateCallReport - Report generated.")

        return report.toString()
    }

    private fun sendSms(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)

        Log.d("SimplyCall - DailyReportWorker", "sendSms - Report sent to $phoneNumber")
    }
}
