package com.orghub.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
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

        setupUI()
        tts = TextToSpeech(this, this)
    }

    private fun setupUI() {
        val fmt = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        findViewById<TextView>(R.id.tvDateTime).text = fmt.format(Date()).uppercase()
        findViewById<TextView>(R.id.tvSubject).text = subject

        // Accept button
        findViewById<android.view.View>(R.id.btnAccept).setOnClickListener {
            onAccepted()
        }

        // Mute button
        findViewById<ImageButton>(R.id.btnMute).setOnClickListener {
            muted = !muted
            if (muted) {
                tts?.stop()
            } else {
                if (!paused) speak()
            }
        }

        // Pause button
        findViewById<ImageButton>(R.id.btnPause).setOnClickListener {
            paused = !paused
            if (paused) {
                tts?.stop()
            } else {
                if (!muted) speak()
            }
        }

        // Hang up button
        findViewById<android.view.View>(R.id.btnHangUp).setOnClickListener {
            hangUp()
        }
    }

    private fun onAccepted() {
        accepted = true

        // ✅ STOP RINGTONE FIRST!
        stopService(Intent(this, ReminderService::class.java))

        // Show active layout
        findViewById<android.view.View>(R.id.layoutIncoming).visibility = 
            android.view.View.GONE
        findViewById<android.view.View>(R.id.layoutActive).visibility = 
            android.view.View.VISIBLE

        // Start TTS after short delay
        // so ringtone fully stops first
        android.os.Handler(mainLooper).postDelayed({
            if (tts != null) speak()
        }, 500)
    }

    private fun speak() {
        if (muted || paused || !accepted) return
        val msg = "Hey! Important reminder! " +
                "You asked me to remind you about... $subject! " +
                "Please don't forget! " +
                "Again... You asked me to remind you about $subject!"
        tts?.setSpeechRate(speed)
        tts?.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "orghub")
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (accepted && !paused && !muted) {
                    runOnUiThread {
                        android.os.Handler(mainLooper).postDelayed({
                            speak()
                        }, 1000)
                    }
                }
            }
            override fun onError(id: String?) {}
        })
    }

    private fun hangUp() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        stopService(Intent(this, ReminderService::class.java))
        if (!accepted) {
            AlarmScheduler.snooze(this, reminderId, subject, gender, speed)
        }
        finish()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(speed)
        }
    }

    override fun onBackPressed() {
        AlarmScheduler.snooze(this, reminderId, subject, gender, speed)
        tts?.stop()
        tts?.shutdown()
        stopService(Intent(this, ReminderService::class.java))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.shutdown()
    }
}
