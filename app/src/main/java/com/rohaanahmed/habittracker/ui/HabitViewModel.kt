package com.rohaanahmed.habittracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rohaanahmed.habittracker.data.Habit
import com.rohaanahmed.habittracker.data.HabitDatabase
import com.rohaanahmed.habittracker.data.HabitRepository
import com.rohaanahmed.habittracker.data.HabitWithHistory
import com.rohaanahmed.habittracker.data.HabitCompletion
import com.rohaanahmed.habittracker.widget.HabitWidgetReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

data class HabitWith30DayHistory(
    val habit: Habit,
    val last30DaysCompletion: List<Pair<LocalDate, Boolean>>
)

data class MonthCount(
    val month: YearMonth,
    val count: Int
)

class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HabitRepository

    private val today: String
        get() = LocalDate.now().toString()

    private val _completedIds = MutableStateFlow<Set<Long>>(emptySet())

    val habitsWithHistory: StateFlow<List<HabitWithHistory>>

    private val _thirtyDayHistory = MutableStateFlow<List<HabitWith30DayHistory>>(emptyList())
    val thirtyDayHistory: StateFlow<List<HabitWith30DayHistory>> = _thirtyDayHistory

    private val _twelveMonthHistory = MutableStateFlow<List<MonthCount>>(emptyList())
    val twelveMonthHistory: StateFlow<List<MonthCount>> = _twelveMonthHistory

    init {
        val dao = HabitDatabase.getDatabase(application).habitDao()
        repository = HabitRepository(dao)

        habitsWithHistory = combine(
            repository.allHabits,
            repository.getCompletionsForDate(today)
        ) { habits, completions ->
            val completedIds = completions.map { it.habitId }.toSet()
            _completedIds.value = completedIds

            habits.map { habit ->
                HabitWithHistory(
                    habit = habit,
                    isCompletedToday = habit.id in completedIds,
                    last7DaysCompletion = emptyList() // Will be loaded separately
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Load history data
        viewModelScope.launch {
            loadHistory()
        }
    }

    private val _historyMap = MutableStateFlow<Map<Long, List<Boolean>>>(emptyMap())

    val habitsWithFullHistory: StateFlow<List<HabitWithHistory>> = combine(
        habitsWithHistory,
        _historyMap
    ) { habits, history ->
        habits.map { hwh ->
            val last6Days = history[hwh.habit.id] ?: List(6) { false }
            hwh.copy(last7DaysCompletion = last6Days + hwh.isCompletedToday)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private suspend fun loadHistory() {
        val sixDaysAgo = LocalDate.now().minusDays(6).toString()
        val yesterday = LocalDate.now().minusDays(1).toString()
        val completions = repository.getCompletionsInRange(sixDaysAgo, yesterday)

        val habits = repository.getAllHabitsOnce()
        val historyMap = habits.associate { habit ->
            habit.id to (6 downTo 1).map { daysAgo ->
                val date = LocalDate.now().minusDays(daysAgo.toLong()).toString()
                completions.any { it.habitId == habit.id && it.date == date }
            }
        }
        _historyMap.value = historyMap
    }

    fun load30DayHistory() {
        viewModelScope.launch {
            val thirtyDaysAgo = LocalDate.now().minusDays(29).toString()
            val todayStr = LocalDate.now().toString()
            val completions = repository.getCompletionsInRange(thirtyDaysAgo, todayStr)
            val habits = repository.getAllHabitsOnce()

            val historyList = habits.map { habit ->
                val daysList = (29 downTo 0).map { daysAgo ->
                    val date = LocalDate.now().minusDays(daysAgo.toLong())
                    val isComplete = completions.any { it.habitId == habit.id && it.date == date.toString() }
                    date to isComplete
                }
                HabitWith30DayHistory(habit, daysList)
            }
            _thirtyDayHistory.value = historyList
        }
    }

    fun load12MonthHistory(habitId: Long) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val start = today.withDayOfMonth(1).minusMonths(11)
            val completions = repository.getCompletionsForHabitInRange(
                habitId,
                start.toString(),
                today.toString()
            )
            val countsByMonth = completions.groupingBy {
                YearMonth.from(LocalDate.parse(it.date))
            }.eachCount()
            val months = (11 downTo 0).map { offset ->
                YearMonth.from(today.minusMonths(offset.toLong()))
            }
            _twelveMonthHistory.value = months.map { month ->
                MonthCount(month, countsByMonth[month] ?: 0)
            }
        }
    }

    fun addHabit(name: String, targetPerWeek: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addHabit(name.trim(), targetPerWeek.coerceIn(1, 7))
            loadHistory()
            refreshWidget()
        }
    }

    fun addHabitsBulk(names: List<String>) {
        val cleaned = names.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return
        viewModelScope.launch {
            repository.replaceHabits(cleaned)
            loadHistory()
            refreshWidget()
        }
    }

    fun updateHabit(habitId: Long, name: String, targetPerWeek: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val habit = repository.getHabitById(habitId)
            if (habit != null) {
                repository.updateHabit(habit.copy(name = name.trim(), targetPerWeek = targetPerWeek.coerceIn(1, 7)))
                refreshWidget()
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
            loadHistory()
            refreshWidget()
        }
    }

    fun moveHabitUp(habitId: Long) {
        viewModelScope.launch {
            repository.moveHabitUp(habitId)
            refreshWidget()
        }
    }

    fun moveHabitDown(habitId: Long) {
        viewModelScope.launch {
            repository.moveHabitDown(habitId)
            refreshWidget()
        }
    }

    fun reorderHabits(orderedIds: List<Long>) {
        viewModelScope.launch {
            val habits = repository.getAllHabitsOnce().associateBy { it.id }
            val ordered = orderedIds.mapNotNull { habits[it] }
            if (ordered.isNotEmpty()) {
                repository.reorderHabits(ordered)
                refreshWidget()
            }
        }
    }

    fun toggleHabitCompletion(habitId: Long) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, today)
            refreshWidget()
        }
    }

    fun updateHabitHistory(habitId: Long, updates: Map<String, Boolean>) {
        viewModelScope.launch {
            updates.forEach { (date, isComplete) ->
                repository.setHabitCompletion(habitId, date, isComplete)
            }
            loadHistory()
            refreshWidget()
        }
    }

    private suspend fun refreshWidget() {
        val context = getApplication<Application>()
        HabitWidgetReceiver.notifyDataChanged(context)
    }
}
