package com.orghub.app

import android.app.*
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.NotificationCompat

class ReminderService : Service() {

    companion object {
        const val CHANNEL_ID = "orghub_channel"
        const val NOTIF_ID = 101
    }

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getIntExtra("ID", -1) ?: -1
        val subject = intent?.getStringExtra("SUBJECT") ?: "Reminder"
        val gender = intent?.getStringExtra("GENDER") ?: "female"
        val speed = intent?.getFloatExtra("SPEED", 1.0f) ?: 1.0f

        createChannel()
        startForeground(NOTIF_ID, buildNotification(subject))
        startRingtone()
        startVibration()

        val callIntent = Intent(this, CallScreenActivity::class.java)
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        callIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        callIntent.putExtra("ID", id)
        callIntent.putExtra("SUBJECT", subject)
        callIntent.putExtra("GENDER", gender)
        callIntent.putExtra("SPEED", speed)
        startActivity(callIntent)

        return START_NOT_STICKY
    }

    private fun startRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_RINGTONE
            )
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_ALARM)
                        .build()
                )
                setDataSource(applicationContext, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            // Fallback to notification ringtone
            try {
                val ringtoneUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_ALARM
                )
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(applicationContext, ringtoneUri)
                    isLooping = true
                    prepare()
                    start()
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
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
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) { }
        try {
            vibrator?.cancel()
        } catch (e: Exception) { }
    }
}
