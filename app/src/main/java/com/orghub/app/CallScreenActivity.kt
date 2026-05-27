package com.orghub.app

import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CallScreenActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var subject = "Reminder"
    private var gender = "female"
    private var speed = 1.0f
    private var reminderId = -1
    private var accepted = false
    private var muted = false
    private var paused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        setContentView(R.layout.activity_call)

        reminderId = intent.getIntExtra("ID", -1)
        subject = intent.getStringExtra("SUBJECT") ?: "Reminder"
        gender = intent.getStringExtra("GENDER") ?: "female"
        speed = intent.getFloatExtra("SPEED", 1.0f)

        // Set volume to max for alarm
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        audio.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )

        setupUI()
        tts = TextToSpeech(this, this)
    }

    private fun setupUI() {
        val fmt = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.tvDateTime).text = fmt.format(Date()).uppercase()
        findViewById<TextView>(R.id.tvSubject).text = subject

        // Show incoming screen
        findViewById<View>(R.id.layoutIncoming).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutActive).visibility = View.GONE

        // Accept
        findViewById<View>(R.id.btnAccept).setOnClickListener {
            onAccepted()
        }

        // Mute
        val btnMute = findViewById<ImageButton>(R.id.btnMute)
        btnMute.setOnClickListener {
            muted = !muted
            btnMute.alpha = if (muted) 0.5f else 1.0f
            if (muted) tts?.stop()
            else if (!paused) speak()
        }

        // Pause
        val btnPause = findViewById<ImageButton>(R.id.btnPause)
        btnPause.setOnClickListener {
            paused = !paused
            btnPause.alpha = if (paused) 0.5f else 1.0f
            if (paused) tts?.stop()
            else if (!muted) speak()
        }

        // Hang up
        findViewById<View>(R.id.btnHangUp).setOnClickListener {
            hangUp()
        }
    }

    private fun onAccepted() {
        accepted = true

        // Stop ringtone immediately
        stopService(Intent(this, ReminderService::class.java))

        // Switch UI
        findViewById<View>(R.id.layoutIncoming).visibility = View.GONE
        findViewById<View>(R.id.layoutActive).visibility = View.VISIBLE

        // Start TTS after delay
        Handler(Looper.getMainLooper()).postDelayed({
            speak()
        }, 800)
    }

    private fun speak() {
        if (!accepted || muted || paused) return

        val msg = "Hey! Important reminder! " +
                "You asked me to remind you about $subject! " +
                "Please don't forget! " +
                "Again! $subject!"

        tts?.setSpeechRate(speed)
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (accepted && !muted && !paused) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        speak()
                    }, 1000)
                }
            }
            override fun onError(id: String?) {}
        })
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "orghub_tts")
    }

    private fun hangUp() {
        accepted = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        try {
            stopService(Intent(this, ReminderService::class.java))
        } catch (e: Exception) { }
        if (!accepted) {
            AlarmScheduler.snooze(this, reminderId, subject, gender, speed)
        }
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(speed)
            // Auto speak if already accepted
            if (accepted) speak()
        }
    }

    override fun onBackPressed() {
        hangUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) { }
    }
}
