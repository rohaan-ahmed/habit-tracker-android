package com.rohaanahmed.habittracker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class MorningReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        val repository = HabitRepository(HabitDatabase.getDatabase(context).habitDao())

        val habits = repository.getAllHabitsOnce()
        if (habits.isEmpty()) return Result.success()

        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        val weeklyCompletions = repository.getCompletionsInRange(
            weekStart.toString(),
            today.toString()
        )
        val weeklyCounts = weeklyCompletions.groupingBy { it.habitId }.eachCount()

        val lastThreeStart = today.minusDays(2)
        val lastThreeEnd = today
        val lastThreeCompletions = repository.getCompletionsInRange(
            lastThreeStart.toString(),
            lastThreeEnd.toString()
        )
        val lastThreeIds = lastThreeCompletions.map { it.habitId }.toSet()

        val lastCompletionMap = repository.getLastCompletionDates()
            .associateBy { it.habitId }

        val lines = habits.mapNotNull { habit ->
            val weeklyCount = weeklyCounts[habit.id] ?: 0
            val targetMet = weeklyCount >= habit.targetPerWeek
            if (targetMet || habit.id in lastThreeIds) return@mapNotNull null

            val lastDate = lastCompletionMap[habit.id]?.date?.let(LocalDate::parse)
                ?: Instant.ofEpochMilli(habit.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            val daysSince = ChronoUnit.DAYS.between(lastDate, today).coerceAtLeast(0)
            "${habit.name} in $daysSince days."
        }

        if (lines.isEmpty()) return Result.success()

        val body = buildString {
            append("You have not completed the following habits:\n")
            lines.forEach { append(it).append('\n') }
        }.trimEnd()

        NotificationUtils.showNotification(
            context = context,
            notificationId = 1001,
            title = "Today's habit reminder",
            body = body
        )
        return Result.success()
    }
}
