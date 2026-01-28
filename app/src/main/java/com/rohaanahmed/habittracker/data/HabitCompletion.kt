package com.rohaanahmed.habittracker.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitCompletion(
    val habitId: Long,
    val date: String  // Format: "2026-01-22" (LocalDate.toString())
)
