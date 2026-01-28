package com.rohaanahmed.habittracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rohaanahmed.habittracker.data.Habit
import com.rohaanahmed.habittracker.data.HabitWithHistory
import com.rohaanahmed.habittracker.ui.HabitViewModel
import com.rohaanahmed.habittracker.ui.HabitWith30DayHistory
import com.rohaanahmed.habittracker.ui.MonthCount
import com.rohaanahmed.habittracker.ui.theme.ExtendedTheme
import com.rohaanahmed.habittracker.ui.theme.GlassBorder
import com.rohaanahmed.habittracker.ui.theme.GlassWhite
import com.rohaanahmed.habittracker.ui.theme.SuccessGreen
import com.rohaanahmed.habittracker.ui.theme.SuccessGreenLight
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitListScreen(
    viewModel: HabitViewModel,
    onAddHabit: () -> Unit
) {
    val habits by viewModel.habitsWithFullHistory.collectAsState()
    val thirtyDayHistory by viewModel.thirtyDayHistory.collectAsState()
    val twelveMonthHistory by viewModel.twelveMonthHistory.collectAsState()
    val last7Dates = remember { (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) } }
    val todayDate = remember { LocalDate.now() }
    val dayLabels = remember {
        last7Dates.map { date ->
            if (date == todayDate) {
                "TODAY"
            } else {
                date.dayOfWeek
                    .getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    .take(2)
                    .uppercase()
            }
        }
    }
    var editingHabit by remember { mutableStateOf<HabitWithHistory?>(null) }
    var show30DayHistory by remember { mutableStateOf(false) }
    var show12MonthHistoryHabit by remember { mutableStateOf<HabitWithHistory?>(null) }
    var reorderingHabitId by remember { mutableStateOf<Long?>(null) }
    val hapticFeedback = LocalHapticFeedback.current
    val nameWidth = 140.dp
    val todayCellSize = 36.dp
    val previousCellSize = 24.dp
    val context = LocalContext.current
    var showCsvDialog by remember { mutableStateOf(false) }
    val completedTodayCount = habits.count { it.isCompletedToday }
    val targetsMetCount = habits.count { habit ->
        habit.last7DaysCompletion.count { it } >= habit.habit.targetPerWeek
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val csv = habits.joinToString(",") { it.habit.name }
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csv.toByteArray())
                }
            }.onFailure {
                Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
            }.onSuccess {
                Toast.makeText(context, "Exported habits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val text = input.readBytes().toString(Charsets.UTF_8)
                    val names = text
                        .split(',', '\n', '\r')
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    if (names.isEmpty()) {
                        error("invalid file")
                    }
                    viewModel.addHabitsBulk(names)
                }
            }.onFailure {
                Toast.makeText(context, "invalid file", Toast.LENGTH_SHORT).show()
            }.onSuccess {
                Toast.makeText(context, "Imported habits", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Rohaan's Habit Tracker",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = { showCsvDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.ImportExport,
                            contentDescription = "Import or export habits",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.load30DayHistory()
                            show30DayHistory = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "30-day history",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onAddHabit) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add habit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = { }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            val visibleDays = remember(maxWidth) {
                val reservedWidth = nameWidth + 40.dp
                val available = (maxWidth - reservedWidth - todayCellSize).coerceAtLeast(0.dp)
                val previousCount = (available / previousCellSize).toInt().coerceIn(0, 6)
                previousCount + 1
            }
            val visibleDayLabels = remember(dayLabels, visibleDays) {
                dayLabels.takeLast(visibleDays)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                HabitTableHeader(
                    dayLabels = visibleDayLabels,
                    nameWidth = nameWidth,
                    todayCellSize = todayCellSize,
                    previousCellSize = previousCellSize
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (habits.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No habits yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap + to add your first habit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(habits, key = { _, item -> item.habit.id }) { index, habitWithHistory ->
                            val isReordering = reorderingHabitId == habitWithHistory.habit.id
                            val isFirst = index == 0
                            val isLast = index == habits.lastIndex

                            HabitRow(
                                habitWithHistory = habitWithHistory,
                                dayLabels = visibleDayLabels,
                                nameWidth = nameWidth,
                                todayCellSize = todayCellSize,
                                previousCellSize = previousCellSize,
                                isReordering = isReordering,
                                canMoveUp = !isFirst,
                                canMoveDown = !isLast,
                                onToggleToday = {
                                    viewModel.toggleHabitCompletion(habitWithHistory.habit.id)
                                },
                                onClick = { editingHabit = habitWithHistory },
                                onLongPress = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    reorderingHabitId = if (isReordering) null else habitWithHistory.habit.id
                                },
                                onMoveUp = {
                                    viewModel.moveHabitUp(habitWithHistory.habit.id)
                                },
                                onMoveDown = {
                                    viewModel.moveHabitDown(habitWithHistory.habit.id)
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "$completedTodayCount activities completed. $targetsMetCount targets met.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }
    }

    if (editingHabit != null) {
        HabitHistoryDialog(
            habitWithHistory = editingHabit!!,
            last7Dates = last7Dates,
            onDismiss = { editingHabit = null },
            onSave = { updates ->
                viewModel.updateHabitHistory(editingHabit!!.habit.id, updates)
                editingHabit = null
            },
            onEditHabit = { name, target ->
                viewModel.updateHabit(editingHabit!!.habit.id, name, target)
            },
            onDeleteHabit = {
                viewModel.deleteHabit(editingHabit!!.habit)
                editingHabit = null
            },
            onShow12MonthHistory = {
                viewModel.load12MonthHistory(editingHabit!!.habit.id)
                show12MonthHistoryHabit = editingHabit
                editingHabit = null
            }
        )
    }

    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("Save or upload Habits list") },
            text = {
                Text("Import an existing list, or save your current list of habits, in CSV format.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCsvDialog = false
                        exportLauncher.launch("habits.csv")
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCsvDialog = false
                        importLauncher.launch(
                            arrayOf("text/csv", "text/plain", "text/comma-separated-values")
                        )
                    }
                ) {
                    Text("Import")
                }
            }
        )
    }

    if (show30DayHistory) {
        ThirtyDayHistorySheet(
            historyList = thirtyDayHistory,
            onDismiss = { show30DayHistory = false }
        )
    }

    if (show12MonthHistoryHabit != null) {
        TwelveMonthHistoryScreen(
            habitName = show12MonthHistoryHabit!!.habit.name,
            monthCounts = twelveMonthHistory,
            onClose = { show12MonthHistoryHabit = null }
        )
    }
}

@Composable
private fun HabitTableHeader(
    dayLabels: List<String>,
    nameWidth: Dp,
    todayCellSize: Dp,
    previousCellSize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        GlassWhite,
                        GlassWhite.copy(alpha = 0.05f)
                    )
                )
            )
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(nameWidth + 6.dp))
        dayLabels.forEachIndexed { index, label ->
            val isToday = index == dayLabels.lastIndex
            val cellSize = if (isToday) todayCellSize else previousCellSize
            Box(
                modifier = Modifier
                    .width(cellSize)
                    .height(cellSize - 4.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isToday) 9.sp else 12.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitRow(
    habitWithHistory: HabitWithHistory,
    dayLabels: List<String>,
    nameWidth: Dp,
    todayCellSize: Dp,
    previousCellSize: Dp,
    isReordering: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggleToday: () -> Unit,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val completions = habitWithHistory.last7DaysCompletion
    val goalMet = completions.count { it } >= habitWithHistory.habit.targetPerWeek

    val backgroundColor by animateColorAsState(
        targetValue = if (goalMet)
            SuccessGreen.copy(alpha = 0.15f)
        else
            GlassWhite,
        animationSpec = tween(durationMillis = 350),
        label = "rowBackground"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isReordering -> MaterialTheme.colorScheme.primary
            goalMet -> SuccessGreenLight.copy(alpha = 0.5f)
            else -> GlassBorder
        },
        animationSpec = tween(durationMillis = 350),
        label = "rowBorder"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .border(if (isReordering) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reorder buttons (shown when long-pressed)
                    AnimatedVisibility(
                        visible = isReordering,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier.padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onMoveUp,
                                enabled = canMoveUp,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Move up",
                                    tint = if (canMoveUp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                            IconButton(
                                onClick = onMoveDown,
                                enabled = canMoveDown,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Move down",
                                    tint = if (canMoveDown) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }

                    Text(
                        text = habitWithHistory.habit.name,
                        color = if (goalMet) SuccessGreenLight else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = true,
                        modifier = Modifier
                            .width(if (isReordering) nameWidth - 72.dp else nameWidth)
                            .padding(end = 8.dp)
                    )

                    dayLabels.forEachIndexed { index, _ ->
                        val isToday = index == dayLabels.lastIndex
                        val completionIndex = completions.size - dayLabels.size + index
                        DayCell(
                            completed = completions.getOrElse(completionIndex) { false },
                            isToday = isToday,
                            goalMet = goalMet,
                            size = if (isToday) todayCellSize else previousCellSize,
                            onToggle = if (isToday) onToggleToday else null
                        )
                    }
                }

                // Target text under the check marks
                Text(
                    text = "Target is ${habitWithHistory.habit.targetPerWeek} days/week",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    completed: Boolean,
    isToday: Boolean,
    goalMet: Boolean,
    size: Dp,
    onToggle: (() -> Unit)?
) {
    val accent = if (goalMet) SuccessGreen else MaterialTheme.colorScheme.primary
    val background by animateColorAsState(
        targetValue = if (completed)
            accent.copy(alpha = 0.25f)
        else
            Color.Transparent,
        animationSpec = tween(250),
        label = "cellBackground"
    )
    val scale by animateFloatAsState(
        targetValue = if (completed) 1f else 0.85f,
        animationSpec = tween(200),
        label = "checkScale"
    )
    val borderColor = if (isToday) accent else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .width(size)
            .height(size - 4.dp)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .border(if (isToday) 2.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            .then(
                if (onToggle != null) Modifier.clickable(onClick = onToggle) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        if (completed) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.height((14 * scale).dp)
            )
        }
    }
}

@Composable
private fun HabitHistoryDialog(
    habitWithHistory: HabitWithHistory,
    last7Dates: List<LocalDate>,
    onDismiss: () -> Unit,
    onSave: (Map<String, Boolean>) -> Unit,
    onEditHabit: (String, Int) -> Unit,
    onDeleteHabit: () -> Unit,
    onShow12MonthHistory: () -> Unit
) {
    val initial = habitWithHistory.last7DaysCompletion
    val state = remember(habitWithHistory) {
        mutableStateListOf<Boolean>().apply {
            addAll(initial.ifEmpty { List(7) { false } })
        }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habitWithHistory.habit.name,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit habit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                last7Dates.forEachIndexed { index, date ->
                    val isToday = date == LocalDate.now()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isToday) "TODAY  ${date.format(dateFormatter)}"
                            else date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .uppercase() + "  " + date.format(dateFormatter),
                            modifier = Modifier.weight(1f),
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Checkbox(
                            checked = state.getOrElse(index) { false },
                            onCheckedChange = { checked ->
                                if (index in state.indices) {
                                    state[index] = checked
                                }
                            }
                        )
                    }
                }

                TextButton(
                    onClick = onShow12MonthHistory,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("12-Month History")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val updates = last7Dates.mapIndexed { index, date ->
                    date.toString() to state.getOrElse(index) { false }
                }.toMap()
                onSave(updates)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Edit Habit Dialog
    if (showEditDialog) {
        EditHabitDialog(
            habit = habitWithHistory.habit,
            onDismiss = { showEditDialog = false },
            onSave = { name, target ->
                onEditHabit(name, target)
                showEditDialog = false
            },
            onDelete = { showDeleteConfirm = true }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Habit") },
            text = { Text("Are you sure you want to delete \"${habitWithHistory.habit.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        showEditDialog = false
                        onDeleteHabit()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EditHabitDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: () -> Unit
) {
    var habitName by remember { mutableStateOf(habit.name) }
    var targetPerWeek by remember { mutableStateOf(habit.targetPerWeek.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Habit", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = habitName,
                    onValueChange = { habitName = it },
                    label = { Text("Habit Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Target per Week",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = targetPerWeek.toInt().toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = targetPerWeek,
                        onValueChange = { targetPerWeek = it },
                        valueRange = 1f..7f,
                        steps = 5
                    )
                }

                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Habit")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(habitName.trim(), targetPerWeek.toInt()) },
                enabled = habitName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThirtyDayHistorySheet(
    historyList: List<HabitWith30DayHistory>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "30-Day History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No habits to show",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(400.dp)
                ) {
                    items(historyList) { habitHistory ->
                        HabitHistoryCard(habitHistory)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TwelveMonthHistoryScreen(
    habitName: String,
    monthCounts: List<MonthCount>,
    onClose: () -> Unit
) {
    val maxCount = 31
    val barMaxHeight = 180.dp
    val axisColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "12-Month History for $habitName",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onClose) {
                        Text("Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barMaxHeight + 36.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(axisColor)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    monthCounts.forEach { entry ->
                        val height = if (maxCount == 0) 0.dp else {
                            barMaxHeight * (entry.count.toFloat() / maxCount.toFloat())
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.width(22.dp)
                        ) {
                            Text(
                                text = entry.count.toString(),
                                color = barColor,
                                fontSize = 10.sp
                            )
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(height)
                                    .background(barColor, RoundedCornerShape(6.dp))
                            )
                            Text(
                                text = entry.month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                color = labelColor,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitHistoryCard(habitHistory: HabitWith30DayHistory) {
    val completedCount = habitHistory.last30DaysCompletion.count { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = GlassWhite
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = habitHistory.habit.name,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$completedCount/30 days",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 30-day dots
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(habitHistory.last30DaysCompletion) { (_, isComplete) ->
                    val dotColor = if (isComplete) SuccessGreen else Color.Transparent
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
