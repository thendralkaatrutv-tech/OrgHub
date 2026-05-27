package com.orghub.app

import androidx.room.*

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY timeInMillis ASC")
    fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY timeInMillis DESC")
    fun getCompletedReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    fun getById(id: Int): ReminderEntity?

    @Insert
    fun insert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    fun deleteById(id: Int)

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    fun markDone(id: Int)

    @Query("SELECT * FROM reminders WHERE isCompleted = 0")
    fun getAllActive(): List<ReminderEntity>
}
