package com.orghub.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun schedule(context: Context, reminder: ReminderEntity) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = getPendingIntent(context, reminder.id, reminder.subject, reminder.voiceGender, reminder.voiceSpeed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.timeInMillis, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, reminder.timeInMillis, pi)
        }
    }

    fun cancel(context: Context, id: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(getPendingIntent(context, id, "", "", 1f))
    }

    fun snooze(context: Context, id: Int, subject: String, gender: String, speed: Float) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val time = System.currentTimeMillis() + 2 * 60 * 1000
        val pi = getSnoozePendingIntent(context, id, subject, gender, speed)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pi)
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, time, pi)
        }
    }

    private fun getPendingIntent(context: Context, id: Int, subject: String, gender: String, speed: Float): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.orghub.ALARM_TRIGGER"
            putExtra("ID", id)
            putExtra("SUBJECT", subject)
            putExtra("GENDER", gender)
            putExtra("SPEED", speed)
        }
        return PendingIntent.getBroadcast(context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getSnoozePendingIntent(context: Context, id: Int, subject: String, gender: String, speed: Float): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.orghub.ALARM_TRIGGER"
            putExtra("ID", id)
            putExtra("SUBJECT", subject)
            putExtra("GENDER", gender)
            putExtra("SPEED", speed)
        }
        return PendingIntent.getBroadcast(context, id + 9000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
