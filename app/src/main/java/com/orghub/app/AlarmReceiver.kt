package com.orghub.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule after reboot handled by service
            return
        }
        val id = intent.getIntExtra("ID", -1)
        val subject = intent.getStringExtra("SUBJECT") ?: "Reminder"
        val gender = intent.getStringExtra("GENDER") ?: "female"
        val speed = intent.getFloatExtra("SPEED", 1.0f)

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
}
