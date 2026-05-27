package com.orghub.app

import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat

class ReminderService : Service() {

    companion object {
        const val CHANNEL_ID = "orghub_channel"
        const val NOTIF_ID = 101
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra("ID", -1) ?: -1
        val subject = intent?.getStringExtra("SUBJECT") ?: "Reminder"
        val gender = intent?.getStringExtra("GENDER") ?: "female"
        val speed = intent?.getFloatExtra("SPEED", 1.0f) ?: 1.0f

        createChannel()
        startForeground(NOTIF_ID, buildNotification(subject))
        startVibration()

        val callIntent = Intent(this, CallScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("ID", id)
            putExtra("SUBJECT", subject)
            putExtra("GENDER", gender)
            putExtra("SPEED", speed)
        }
        startActivity(callIntent)

        return START_NOT_STICKY
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            val v = getSystemService(VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        }
    }

    private fun buildNotification(subject: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, CallScreenActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("OrgHub Reminder!")
            .setContentText(subject)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true)
            .setOngoing(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "OrgHub Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).cancel()
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(VIBRATOR_SERVICE) as Vibrator).cancel()
            }
        } catch (e: Exception) { }
    }
}
