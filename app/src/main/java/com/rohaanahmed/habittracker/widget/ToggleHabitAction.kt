package com.rohaanahmed.habittracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ToggleHabitAction : ActionCallback {
    companion object {
        val HabitIdKey = ActionParameters.Key<Long>("habitId")
        private const val TAG = "ToggleHabitAction"
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        Log.d(TAG, "onAction called with glanceId: $glanceId")
        val habitId = parameters[HabitIdKey]
        if (habitId == null) {
            Log.e(TAG, "habitId is null")
            return
        }
        Log.d(TAG, "Toggling habit $habitId")

        // Toggle in database
        withContext(Dispatchers.IO) {
            val database = HabitDatabase.getDatabase(context)
            val repository = HabitRepository(database.habitDao())
            val today = LocalDate.now().toString()
            repository.toggleHabitCompletion(habitId, today)
        }
        Log.d(TAG, "Toggle complete")

        // Get all widget instances and update each one
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(HabitTrackerWidget::class.java)
        Log.d(TAG, "Found ${glanceIds.toList().size} glance IDs")

        glanceIds.forEach { id ->
            Log.d(TAG, "Updating glanceId: $id")
            try {
                HabitTrackerWidget().update(context, id)
                Log.d(TAG, "Update called for $id")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating widget $id", e)
            }
        }

        Log.d(TAG, "All updates complete")
    }
}
