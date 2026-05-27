package com.orghub.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule all active reminders after reboot
                Thread {
                    val db = ReminderDatabase.get(context)
                    val reminders = db.dao().getAllActive()
                    reminders.forEach { reminder ->
                        if (reminder.timeInMillis > System.currentTimeMillis()) {
                            AlarmScheduler.schedule(context, reminder)
                        }
                    }
                    // Also restart watchdog
                    startWatchdog(context)
                }.start()
            }
            "com.orghub.ALARM_TRIGGER" -> {
                val id = intent.getIntExtra("ID", -1)
                val subject = intent.getStringExtra("SUBJECT") ?: "Reminder"
                val gender = intent.getStringExtra("GENDER") ?: "female"
                val speed = intent.getFloatExtra("SPEED", 1.0f)

                // Check if reminder is still active
                Thread {
                    val db = ReminderDatabase.get(context)
                    val reminder = db.dao().getById(id)
                    if (reminder != null && !reminder.isCancelled && !reminder.isCompleted) {
                        startReminderService(context, id, subject, gender, speed)
                    }
                }.start()
            }
        }
    }

    private fun startReminderService(context: Context, id: Int, subject: String,
                                     gender: String, speed: Float) {
        val serviceIntent = Intent(context, ReminderService::class.java).apply {
            putExtra("ID", id)
            putExtra("SUBJECT", subject)
            putExtra("GENDER", gender)
            putExtra("SPEED", speed)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }

    private fun startWatchdog(context: Context) {
        val intent = Intent(context, WatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
