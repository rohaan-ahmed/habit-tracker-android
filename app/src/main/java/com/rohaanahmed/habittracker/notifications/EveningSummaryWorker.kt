package com.rohaanahmed.habittracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import java.time.LocalDate

class EveningSummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = HabitRepository(HabitDatabase.getDatabase(context).habitDao())

        val today = LocalDate.now()
        val habits = repository.getAllHabitsOnce()
        if (habits.isEmpty()) return Result.success()

        val todayCompletions = repository.getCompletionsForDateOnce(today.toString())
        val completedIds = todayCompletions.map { it.habitId }.toSet()

        val weekStart = today.minusDays(6)
        val weeklyCompletions = repository.getCompletionsInRange(
            weekStart.toString(),
            today.toString()
        )
        val weeklyCounts = weeklyCompletions.groupingBy { it.habitId }.eachCount()

        val completedHabits = habits.filter { it.id in completedIds }
        if (completedHabits.isEmpty()) return Result.success()

        val lines = completedHabits.map { habit ->
            val weeklyCount = weeklyCounts[habit.id] ?: 0
            val targetMet = weeklyCount >= habit.targetPerWeek
            if (targetMet) "${habit.name} (weekly target met)" else habit.name
        }

        val body = buildString {
            append("Completed today:\n")
            lines.forEach { append(it).append('\n') }
        }.trimEnd()

        NotificationUtils.showNotification(
            context = context,
            notificationId = 1002,
            title = "Today's habit summary",
            body = body
        )
        return Result.success()
    }
}
