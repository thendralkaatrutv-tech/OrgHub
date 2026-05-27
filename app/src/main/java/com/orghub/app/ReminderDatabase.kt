package com.orghub.app

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReminderEntity::class], version = 2, exportSchema = false)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun dao(): ReminderDao

    companion object {
        @Volatile private var INSTANCE: ReminderDatabase? = null

        fun get(context: Context): ReminderDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ReminderDatabase::class.java,
                    "orghub_db"
                ).fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
        }
    }
}
