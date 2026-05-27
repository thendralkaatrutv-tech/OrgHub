package com.orghub.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val subject: String,
    val timeInMillis: Long,
    val voiceGender: String = "female",
    val voiceSpeed: Float = 1.0f,
    val isCompleted: Boolean = false
)
