package com.orghub.app

import androidx.room.*

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY timeInMillis ASC")
    suspend fun getActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY timeInMillis DESC")
    suspend fun getCompletedReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Int): ReminderEntity?

    @Insert
    suspend fun insert(reminder: ReminderEntity): Long

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE reminders SET isCompleted = 1 WHERE id = :id")
    suspend fun markDone(id: Int)

    @Query("SELECT * FROM reminders WHERE isCompleted = 0")
    suspend fun getAllActive(): List<ReminderEntity>
}
