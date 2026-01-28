package com.rohaanahmed.habittracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rohaanahmed.habittracker.ui.HabitViewModel
import com.rohaanahmed.habittracker.ui.screens.HabitListScreen
import com.rohaanahmed.habittracker.ui.screens.NewHabitScreen
import com.rohaanahmed.habittracker.ui.theme.HabitTrackerTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rohaanahmed.habittracker.notifications.NotificationScheduler
import android.app.AlarmManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.LaunchedEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 1001)
            }
        }
        NotificationScheduler.scheduleDailyReminders(this)
        setContent {
            HabitTrackerTheme {
                val viewModel: HabitViewModel = viewModel()
                var isAddingHabit by remember { mutableStateOf(false) }
                val context = LocalContext.current
                var showExactAlarmDialog by remember { mutableStateOf(false) }
                val lifecycleOwner = LocalLifecycleOwner.current
                val skipExactAlarmPrompt = remember {
                    context.getSharedPreferences("habit_prefs", MODE_PRIVATE)
                        .getBoolean("skip_exact_alarm_prompt", false)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !skipExactAlarmPrompt) {
                    val alarmManager = context.getSystemService(AlarmManager::class.java)
                    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                showExactAlarmDialog = !alarmManager.canScheduleExactAlarms()
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                    }
                }

                if (showExactAlarmDialog) {
                    AlertDialog(
                        onDismissRequest = { showExactAlarmDialog = false },
                        title = { Text("Enable Alarms & Reminders") },
                        text = {
                            Text("For daily reminders, please enable Alarms & Reminders for this app.")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = "package:${context.packageName}".toUri()
                                    }
                                    context.startActivity(intent)
                                    showExactAlarmDialog = false
                                }
                            ) {
                                Text("Enable")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    context.getSharedPreferences("habit_prefs", MODE_PRIVATE)
                                        .edit()
                                        .putBoolean("skip_exact_alarm_prompt", true)
                                        .apply()
                                    showExactAlarmDialog = false
                                }
                            ) {
                                Text("Leave Disabled")
                            }
                        }
                    )
                }

                if (isAddingHabit) {
                    NewHabitScreen(
                        onSave = { name, target ->
                            viewModel.addHabit(name, target)
                            isAddingHabit = false
                        },
                        onCancel = { isAddingHabit = false }
                    )
                } else {
                    HabitListScreen(
                        viewModel = viewModel,
                        onAddHabit = { isAddingHabit = true }
                    )
                }
            }
        }
    }
}
