package com.rohaanahmed.habittracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationScheduler.ACTION_MORNING_REMINDER ->
                NotificationScheduler.triggerMorningNow(context)
            NotificationScheduler.ACTION_EVENING_SUMMARY ->
                NotificationScheduler.triggerEveningNow(context)
        }
        NotificationScheduler.scheduleDailyReminders(context)
    }
}
