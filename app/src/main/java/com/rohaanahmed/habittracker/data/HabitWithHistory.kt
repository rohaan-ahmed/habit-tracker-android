package com.rohaanahmed.habittracker.data

data class HabitWithHistory(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val last7DaysCompletion: List<Boolean>  // [6 days ago, 5 days ago, ..., today]
)
