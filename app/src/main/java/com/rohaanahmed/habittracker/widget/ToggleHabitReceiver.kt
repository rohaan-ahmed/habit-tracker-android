package com.rohaanahmed.habittracker.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ToggleHabitReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TOGGLE_HABIT = "com.rohaanahmed.habittracker.TOGGLE_HABIT"
        const val EXTRA_HABIT_ID = "habit_id"
        private const val TAG = "ToggleHabitReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_HABIT) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1)
        if (habitId == -1L) {
            Log.e(TAG, "No habit ID in intent")
            return
        }

        Log.d(TAG, "Received toggle for habit $habitId")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Toggle the habit
                val database = HabitDatabase.getDatabase(context)
                val repository = HabitRepository(database.habitDao())
                val today = LocalDate.now().toString()
                repository.toggleHabitCompletion(habitId, today)
                Log.d(TAG, "Habit toggled in database")

                HabitWidgetReceiver.notifyDataChanged(context)
                Log.d(TAG, "Widget data refresh requested")

            } catch (e: Exception) {
                Log.e(TAG, "Error toggling habit", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
