package com.rohaanahmed.habittracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.rohaanahmed.habittracker.R
import com.rohaanahmed.habittracker.MainActivity
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.notifications.NotificationScheduler
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class HabitWidgetReceiver : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                updateAll(context)
                NotificationScheduler.scheduleDailyReminders(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HabitWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            appWidgetIds.forEach { appWidgetId ->
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        }

        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, HabitWidgetReceiver::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(component)
            val footerText = buildFooterText(context)
            val footerViews = RemoteViews(context.packageName, R.layout.widget_root).apply {
                setTextViewText(R.id.widget_footer, footerText)
            }
            appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, footerViews)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list)
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_root)
            views.setTextViewText(R.id.widget_title, "Rohaan's Habit Tracker")
            views.setTextViewText(R.id.widget_footer, buildFooterText(context))

            val serviceIntent = Intent(context, HabitWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, serviceIntent)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            val toggleIntent = Intent(context, ToggleHabitReceiver::class.java).apply {
                action = ToggleHabitReceiver.ACTION_TOGGLE_HABIT
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list, pendingIntent)

            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, launchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        }

        private fun buildFooterText(context: Context): String = runBlocking {
            withContext(Dispatchers.IO) {
                val dao = HabitDatabase.getDatabase(context).habitDao()
                val habits = dao.getAllHabitsOnce()
                val today = LocalDate.now()
                val todayStr = today.toString()
                val completedToday = dao.getCompletionsForDateOnce(todayStr)
                val completedCount = completedToday.map { it.habitId }.toSet().size
                val startDate = today.minusDays(6).toString()
                val completions = dao.getCompletionsInRange(startDate, todayStr)
                val countsByHabit = completions.groupingBy { it.habitId }.eachCount()
                val targetsMet = habits.count { habit ->
                    (countsByHabit[habit.id] ?: 0) >= habit.targetPerWeek
                }
                "$completedCount activities completed. $targetsMet targets met."
            }
        }
    }
}
