package com.rohaanahmed.habittracker.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun addHabit(name: String) {
        val maxOrder = habitDao.getMaxSortOrder() ?: 0
        habitDao.insertHabit(Habit(name = name, sortOrder = maxOrder + 1))
    }

    suspend fun addHabit(name: String, targetPerWeek: Int) {
        val maxOrder = habitDao.getMaxSortOrder() ?: 0
        habitDao.insertHabit(Habit(name = name, targetPerWeek = targetPerWeek, sortOrder = maxOrder + 1))
    }

    suspend fun replaceHabits(names: List<String>) {
        val habits = names.mapIndexed { index, name ->
            Habit(name = name, sortOrder = index)
        }
        habitDao.replaceAllHabits(habits)
    }

    suspend fun updateHabit(habit: Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun getHabitById(habitId: Long): Habit? {
        return habitDao.getHabitById(habitId)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun reorderHabits(habits: List<Habit>) {
        habits.forEachIndexed { index, habit ->
            habitDao.updateSortOrder(habit.id, index)
        }
    }

    suspend fun moveHabitUp(habitId: Long) {
        val habits = habitDao.getAllHabitsOnce().toMutableList()
        val currentIndex = habits.indexOfFirst { it.id == habitId }
        if (currentIndex > 0) {
            val habit = habits.removeAt(currentIndex)
            habits.add(currentIndex - 1, habit)
            reorderHabits(habits)
        }
    }

    suspend fun moveHabitDown(habitId: Long) {
        val habits = habitDao.getAllHabitsOnce().toMutableList()
        val currentIndex = habits.indexOfFirst { it.id == habitId }
        if (currentIndex < habits.size - 1) {
            val habit = habits.removeAt(currentIndex)
            habits.add(currentIndex + 1, habit)
            reorderHabits(habits)
        }
    }

    suspend fun toggleHabitCompletion(habitId: Long, date: String) {
        val isComplete = habitDao.isHabitComplete(habitId, date)
        setHabitCompletion(habitId, date, !isComplete)
    }

    suspend fun setHabitCompletion(habitId: Long, date: String, isComplete: Boolean) {
        if (isComplete) {
            habitDao.markComplete(HabitCompletion(habitId, date))
        } else {
            habitDao.markIncomplete(habitId, date)
        }
    }

    fun getCompletionsForDate(date: String): Flow<List<HabitCompletion>> {
        return habitDao.getCompletionsForDate(date)
    }

    // For widget - suspend functions (not Flow)
    suspend fun getAllHabitsOnce(): List<Habit> = habitDao.getAllHabitsOnce()

    suspend fun getCompletionsForDateOnce(date: String): List<HabitCompletion> =
        habitDao.getCompletionsForDateOnce(date)

    suspend fun getCompletionsInRange(startDate: String, endDate: String): List<HabitCompletion> =
        habitDao.getCompletionsInRange(startDate, endDate)

    suspend fun getCompletionsForHabitInRange(
        habitId: Long,
        startDate: String,
        endDate: String
    ): List<HabitCompletion> = habitDao.getCompletionsForHabitInRange(habitId, startDate, endDate)

    suspend fun getLastCompletionDates(): List<HabitLastCompletion> =
        habitDao.getLastCompletionDates()
}
