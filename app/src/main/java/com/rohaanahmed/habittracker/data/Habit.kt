package com.rohaanahmed.habittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val targetPerWeek: Int = 4,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = Int.MAX_VALUE
)
