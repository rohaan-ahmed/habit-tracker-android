package com.rohaanahmed.habittracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    // Habit CRUD
    @Insert
    suspend fun insertHabit(habit: Habit): Long

    @Insert
    suspend fun insertHabits(habits: List<Habit>)

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllHabitsOnce(): List<Habit>

    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: Long): Habit?

    @Query("SELECT MAX(sortOrder) FROM habits")
    suspend fun getMaxSortOrder(): Int?

    @Query("UPDATE habits SET sortOrder = :sortOrder WHERE id = :habitId")
    suspend fun updateSortOrder(habitId: Long, sortOrder: Int)

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAllCompletions()

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    // Completions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markComplete(completion: HabitCompletion)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun markIncomplete(habitId: Long, date: String)

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<HabitCompletion>>

    @Query("SELECT * FROM habit_completions WHERE date = :date")
    suspend fun getCompletionsForDateOnce(date: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE date >= :startDate AND date <= :endDate")
    suspend fun getCompletionsInRange(startDate: String, endDate: String): List<HabitCompletion>

    @Query("SELECT * FROM habit_completions WHERE habitId = :habitId AND date >= :startDate AND date <= :endDate")
    suspend fun getCompletionsForHabitInRange(
        habitId: Long,
        startDate: String,
        endDate: String
    ): List<HabitCompletion>

    @Query("SELECT EXISTS(SELECT 1 FROM habit_completions WHERE habitId = :habitId AND date = :date)")
    suspend fun isHabitComplete(habitId: Long, date: String): Boolean

    @Query("SELECT habitId, MAX(date) AS date FROM habit_completions GROUP BY habitId")
    suspend fun getLastCompletionDates(): List<HabitLastCompletion>

    @Transaction
    suspend fun replaceAllHabits(habits: List<Habit>) {
        deleteAllCompletions()
        deleteAllHabits()
        insertHabits(habits)
    }
}
