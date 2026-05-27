package com.orghub.app

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat

class WatchdogService : Service() {

    companion object {
        const val CHANNEL_ID = "orghub_watchdog"
        const val NOTIF_ID = 999
    }

    private val handler = Handler(Looper.getMainLooper())
    private val checkRunnable = object : Runnable {
        override fun run() {
            checkReminders()
            handler.postDelayed(this, 30000) // Check every 30 seconds
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.post(checkRunnable)
        return START_STICKY // Restart if killed!
    }

    private fun checkReminders() {
        Thread {
            val db = ReminderDatabase.get(applicationContext)
            val reminders = db.dao().getAllActive()
            val now = System.currentTimeMillis()
            reminders.forEach { reminder ->
                if (reminder.timeInMillis <= now &&
                    reminder.timeInMillis >= now - 60000) {
                    // Reminder is due! Fire it!
                    val serviceIntent = Intent(applicationContext, ReminderService::class.java).apply {
                        putExtra("ID", reminder.id)
                        putExtra("SUBJECT", reminder.subject)
                        putExtra("GENDER", reminder.voiceGender)
                        putExtra("SPEED", reminder.voiceSpeed)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            }
        }.start()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("OrgHub")
            .setContentText("Reminder service active")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "OrgHub Background",
                NotificationManager.IMPORTANCE_MIN
            )
            ch.setShowBadge(false)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        // Restart self when killed by MIUI!
        val restartIntent = Intent(applicationContext, WatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(restartIntent)
        } else {
            startService(restartIntent)
        }
    }
}
