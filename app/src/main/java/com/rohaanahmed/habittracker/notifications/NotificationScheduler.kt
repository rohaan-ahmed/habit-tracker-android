package com.rohaanahmed.habittracker.notifications

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

object NotificationScheduler {
    const val ACTION_MORNING_REMINDER = "com.rohaanahmed.habittracker.MORNING_REMINDER"
    const val ACTION_EVENING_SUMMARY = "com.rohaanahmed.habittracker.EVENING_SUMMARY"
    private const val MORNING_TEST_WORK = "morning_reminder_test"
    private const val EVENING_TEST_WORK = "evening_summary_test"

    fun scheduleDailyReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        scheduleAlarm(
            context = context,
            alarmManager = alarmManager,
            hour = 6,
            requestCode = 2001,
            action = ACTION_MORNING_REMINDER
        )
        scheduleAlarm(
            context = context,
            alarmManager = alarmManager,
            hour = 21,
            requestCode = 2002,
            action = ACTION_EVENING_SUMMARY
        )
    }

    fun triggerTestNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            MORNING_TEST_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MorningReminderWorker>().build()
        )
        workManager.enqueueUniqueWork(
            EVENING_TEST_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<EveningSummaryWorker>().build()
        )
    }

    fun triggerMorningNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            MORNING_TEST_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<MorningReminderWorker>().build()
        )
    }

    fun triggerEveningNow(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            EVENING_TEST_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<EveningSummaryWorker>().build()
        )
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        requestCode: Int,
        action: String
    ) {
        val triggerAtMillis = nextTriggerMillis(hour)
        val intent = Intent(context, NotificationAlarmReceiver::class.java).apply {
            this.action = action
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun nextTriggerMillis(targetHour: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(targetHour).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
