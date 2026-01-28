package com.rohaanahmed.habittracker.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.glance.unit.ColorProvider
import com.rohaanahmed.habittracker.R
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import com.rohaanahmed.habittracker.data.HabitWithHistory
import java.time.LocalDate

// Colors
private val WidgetBackground = ColorProvider(R.color.widget_background)
private val WidgetSurface = ColorProvider(R.color.widget_surface)
private val WidgetTextPrimary = ColorProvider(R.color.widget_text_primary)
private val WidgetTextMuted = ColorProvider(R.color.widget_text_muted)
private val WidgetCompletedBg = ColorProvider(R.color.widget_completed_bg)

class HabitTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        Log.d("HabitTrackerWidget", "provideGlance called at ${System.currentTimeMillis()}")
        val habitsWithHistory = try {
            val database = HabitDatabase.getDatabase(context)
            val repository = HabitRepository(database.habitDao())

            val today = LocalDate.now().toString()
            val habits = repository.getAllHabitsOnce()
            val todayCompletions = repository.getCompletionsForDateOnce(today)
                .map { it.habitId }.toSet()

            val sixDaysAgo = LocalDate.now().minusDays(6).toString()
            val yesterday = LocalDate.now().minusDays(1).toString()
            val historyCompletions = repository.getCompletionsInRange(sixDaysAgo, yesterday)

            habits.map { habit ->
                val last7Days = (6 downTo 0).map { daysAgo ->
                    val date = LocalDate.now().minusDays(daysAgo.toLong()).toString()
                    if (daysAgo == 0) {
                        habit.id in todayCompletions
                    } else {
                        historyCompletions.any { it.habitId == habit.id && it.date == date }
                    }
                }
                val isCompleted = habit.id in todayCompletions
                Log.d("HabitTrackerWidget", "Habit ${habit.id} (${habit.name}): completedToday=$isCompleted")
                HabitWithHistory(
                    habit = habit,
                    isCompletedToday = isCompleted,
                    last7DaysCompletion = last7Days
                )
            }
        } catch (e: Exception) {
            Log.e("HabitTrackerWidget", "Widget data load failed", e)
            emptyList()
        }

        Log.d("HabitTrackerWidget", "Providing content with ${habitsWithHistory.size} habits")
        provideContent {
            WidgetContent(habitsWithHistory)
        }
    }
}

@Composable
private fun WidgetContent(habits: List<HabitWithHistory>) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBackground)
            .cornerRadius(16.dp)
            .padding(8.dp)
    ) {
        // Title
        Text(
            text = "Rohaan's Habit Tracker",
            style = TextStyle(
                color = WidgetTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 8.dp)
        )

        if (habits.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No habits yet",
                    style = TextStyle(
                        color = WidgetTextMuted,
                        fontSize = 14.sp
                    )
                )
            }
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(habits) { _, habit ->
                    HabitCell(
                        habitWithHistory = habit,
                        modifier = GlanceModifier.fillMaxWidth()
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun HabitCell(habitWithHistory: HabitWithHistory, modifier: GlanceModifier) {
    val completions = habitWithHistory.last7DaysCompletion
    val weeklyCompleted = completions.count { it }
    val targetMet = weeklyCompleted >= habitWithHistory.habit.targetPerWeek
    val completedToday = habitWithHistory.isCompletedToday
    val habitId = habitWithHistory.habit.id

    Box(
        modifier = modifier
            .cornerRadius(8.dp)
            .background(if (completedToday) WidgetCompletedBg else WidgetSurface)
            .clickable(
                actionRunCallback<ToggleHabitAction>(
                    parameters = actionParametersOf(
                        ToggleHabitAction.HabitIdKey to habitId
                    )
                )
            )
    ) {
        Column(
            modifier = GlanceModifier.padding(10.dp)
        ) {
        // Habit name
        Text(
            text = habitWithHistory.habit.name,
            style = TextStyle(
                color = WidgetTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1
        )

        Spacer(modifier = GlanceModifier.height(6.dp))

        // 6 day dots (previous 6 days, not today)
        Row {
            completions.dropLast(1).forEach { completed ->
                Box(
                    modifier = GlanceModifier
                        .size(14.dp)
                        .padding(1.dp)
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(12.dp)
                            .cornerRadius(2.dp)
                            .background(if (completed) WidgetTextPrimary else WidgetTextMuted)
                    ) {
                        if (!completed) {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = GlanceModifier
                                        .fillMaxSize()
                                        .cornerRadius(1.dp)
                                        .background(if (completedToday) WidgetCompletedBg else WidgetSurface)
                                ) {}
                            }
                        }
                    }
                }
            }
        }

            // Target Met text (only show if target is met)
            if (targetMet) {
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Target Met",
                    style = TextStyle(
                        color = WidgetTextPrimary,
                        fontSize = 10.sp
                    )
                )
            }
        }
    }
}
