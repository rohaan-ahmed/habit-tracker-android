package com.rohaanahmed.habittracker.widget

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.rohaanahmed.habittracker.R
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import com.rohaanahmed.habittracker.data.HabitWithHistory
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

class HabitWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return HabitWidgetFactory(applicationContext)
    }
}

private class HabitWidgetFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    private var rows: List<RowData> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            rows = loadRows(context)
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = rows.size

    override fun getViewAt(position: Int): RemoteViews {
        val row = rows.getOrNull(position)
            ?: return RemoteViews(context.packageName, R.layout.widget_row)

        val views = RemoteViews(context.packageName, R.layout.widget_row)

        bindHabit(
            views = views,
            habit = row.first,
            containerId = R.id.cell_container_1,
            nameId = R.id.habit_name_1,
            todayContainerId = R.id.today_container_1,
            todayIconId = R.id.today_check_icon_1,
            historyIds = HISTORY_IDS_1
        )
        val fillInIntent1 = Intent().apply {
            putExtra(ToggleHabitReceiver.EXTRA_HABIT_ID, row.first.habit.id)
        }
        views.setOnClickFillInIntent(R.id.today_container_1, fillInIntent1)
        views.setOnClickFillInIntent(R.id.cell_container_1, fillInIntent1)

        val second = row.second
        if (second != null) {
            views.setViewVisibility(R.id.cell_container_2, View.VISIBLE)
            views.setViewVisibility(R.id.column_divider, View.VISIBLE)

            bindHabit(
                views = views,
                habit = second,
                containerId = R.id.cell_container_2,
                nameId = R.id.habit_name_2,
                todayContainerId = R.id.today_container_2,
                todayIconId = R.id.today_check_icon_2,
                historyIds = HISTORY_IDS_2
            )
            val fillInIntent2 = Intent().apply {
                putExtra(ToggleHabitReceiver.EXTRA_HABIT_ID, second.habit.id)
            }
            views.setOnClickFillInIntent(R.id.today_container_2, fillInIntent2)
            views.setOnClickFillInIntent(R.id.cell_container_2, fillInIntent2)
        } else {
            views.setViewVisibility(R.id.cell_container_2, View.GONE)
            views.setViewVisibility(R.id.column_divider, View.GONE)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        val row = rows.getOrNull(position) ?: return position.toLong()
        val firstId = row.first.habit.id
        val secondId = row.second?.habit?.id ?: 0L
        return (firstId * 31L) + secondId
    }

    override fun hasStableIds(): Boolean = true

    private fun bindHabit(
        views: RemoteViews,
        habit: HabitWithHistory,
        containerId: Int,
        nameId: Int,
        todayContainerId: Int,
        todayIconId: Int,
        historyIds: IntArray
    ) {
        val completions = habit.last7DaysCompletion
        val completedToday = habit.isCompletedToday
        val weeklyCompleted = completions.count { it }
        val targetMet = weeklyCompleted >= habit.habit.targetPerWeek

        views.setTextViewText(nameId, habit.habit.name)

        val cellBackground = if (targetMet) {
            R.drawable.widget_cell_bg_target
        } else {
            R.drawable.widget_cell_bg
        }
        views.setInt(containerId, "setBackgroundResource", cellBackground)

        val todayBackground = if (completedToday) {
            R.drawable.widget_check_bg_on
        } else {
            R.drawable.widget_check_bg_off
        }
        views.setInt(todayContainerId, "setBackgroundResource", todayBackground)
        views.setViewVisibility(todayIconId, if (completedToday) View.VISIBLE else View.INVISIBLE)

        completions.dropLast(1).asReversed().forEachIndexed { index, completed ->
            if (index >= historyIds.size) return@forEachIndexed
            val dotBackground = if (completed) {
                R.drawable.widget_dot_on
            } else {
                R.drawable.widget_dot_off
            }
            views.setInt(historyIds[index], "setBackgroundResource", dotBackground)
        }
    }

    companion object {
        private val HISTORY_IDS_1 = intArrayOf(
            R.id.day_1_1,
            R.id.day_1_2,
            R.id.day_1_3,
            R.id.day_1_4,
            R.id.day_1_5,
            R.id.day_1_6
        )

        private val HISTORY_IDS_2 = intArrayOf(
            R.id.day_2_1,
            R.id.day_2_2,
            R.id.day_2_3,
            R.id.day_2_4,
            R.id.day_2_5,
            R.id.day_2_6
        )
    }
}

private data class RowData(
    val first: HabitWithHistory,
    val second: HabitWithHistory?
)

private fun loadRows(context: Context): List<RowData> = runBlocking {
    val database = HabitDatabase.getDatabase(context)
    val repository = HabitRepository(database.habitDao())

    val today = LocalDate.now()
    val todayString = today.toString()
    val habits = repository.getAllHabitsOnce()
    val todayCompletions = repository.getCompletionsForDateOnce(todayString)
        .map { it.habitId }
        .toSet()

    val sixDaysAgo = today.minusDays(6).toString()
    val yesterday = today.minusDays(1).toString()
    val historyCompletions = repository.getCompletionsInRange(sixDaysAgo, yesterday)

    val historyByHabit = historyCompletions.groupBy { it.habitId }

    val habitsWithHistory = habits.map { habit ->
        val last7Days = (6 downTo 0).map { daysAgo ->
            val date = today.minusDays(daysAgo.toLong()).toString()
            if (daysAgo == 0) {
                habit.id in todayCompletions
            } else {
                historyByHabit[habit.id]?.any { it.date == date } ?: false
            }
        }
        HabitWithHistory(
            habit = habit,
            isCompletedToday = habit.id in todayCompletions,
            last7DaysCompletion = last7Days
        )
    }

    habitsWithHistory.chunked(2).map { chunk ->
        RowData(
            first = chunk[0],
            second = chunk.getOrNull(1)
        )
    }
}
