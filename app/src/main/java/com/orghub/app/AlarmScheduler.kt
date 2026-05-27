package com.orghub.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun schedule(context: Context, reminder: ReminderEntity) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = buildIntent(context, reminder.id, reminder.subject,
            reminder.voiceGender, reminder.voiceSpeed)
        val pi = PendingIntent.getBroadcast(context, reminder.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeInMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, reminder.timeInMillis, pi)
            }
        } catch (e: Exception) {
            am.set(AlarmManager.RTC_WAKEUP, reminder.timeInMillis, pi)
        }
    }

    fun cancel(context: Context, reminderId: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, reminderId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(pi)
        // Also cancel snooze
        val snoozeIntent = Intent(context, AlarmReceiver::class.java)
        val snoozePi = PendingIntent.getBroadcast(context, reminderId + 9000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        am.cancel(snoozePi)
    }

    fun snooze(context: Context, reminderId: Int, subject: String,
               gender: String, speed: Float) {
        // Check if reminder is cancelled before snoozing
        Thread {
            val db = ReminderDatabase.get(context)
            val reminder = db.dao().getById(reminderId)
            if (reminder == null || reminder.isCancelled || reminder.isCompleted) {
                return@Thread // Don't snooze cancelled reminders!
            }
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val snoozeTime = System.currentTimeMillis() + 2 * 60 * 1000
            val intent = buildIntent(context, reminderId, subject, gender, speed)
            val pi = PendingIntent.getBroadcast(context, reminderId + 9000, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
                }
            } catch (e: Exception) {
                am.set(AlarmManager.RTC_WAKEUP, snoozeTime, pi)
            }
        }.start()
    }

    private fun buildIntent(context: Context, id: Int, subject: String,
                            gender: String, speed: Float): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            action = "com.orghub.ALARM_TRIGGER"
            putExtra("ID", id)
            putExtra("SUBJECT", subject)
            putExtra("GENDER", gender)
            putExtra("SPEED", speed)
        }
    }
}
