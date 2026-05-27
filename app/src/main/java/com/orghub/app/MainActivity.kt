package com.orghub.app

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: ReminderDatabase
    private var selectedTime = Calendar.getInstance()
    private var selectedGender = "female"
    private var selectedSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = ReminderDatabase.get(this)
        startWatchdog()
        requestPermissions()
        setupUI()
        loadReminders()
    }

    private fun startWatchdog() {
        // Start background watchdog service
        val intent = Intent(this, WatchdogService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun requestPermissions() {
        // Battery optimization
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Important Permission")
                .setMessage("OrgHub needs to run in background to remind you on time.\n\nPlease tap 'Allow' on next screen to enable this.")
                .setPositiveButton("Allow") { _, _ ->
                    try {
                        startActivity(Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:$packageName")))
                    } catch (e: Exception) {
                        startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                    }
                }
                .setCancelable(false)
                .show()
        }

        // Exact alarm permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                android.app.AlertDialog.Builder(this)
                    .setTitle("Alarm Permission")
                    .setMessage("OrgHub needs Alarm permission to remind you at exact time.")
                    .setPositiveButton("Allow") { _, _ ->
                        startActivity(Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:$packageName")))
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        // Notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.tabActive).setOnClickListener {
            loadReminders(); highlightTab(true)
        }
        findViewById<TextView>(R.id.tabHistory).setOnClickListener {
            loadHistory(); highlightTab(false)
        }
        findViewById<View>(R.id.fabAdd).setOnClickListener { showAddDialog() }
    }

    private fun highlightTab(activeSelected: Boolean) {
        findViewById<TextView>(R.id.tabActive).setBackgroundColor(
            if (activeSelected) getColor(R.color.olive) else getColor(R.color.dark_surface))
        findViewById<TextView>(R.id.tabHistory).setBackgroundColor(
            if (!activeSelected) getColor(R.color.olive) else getColor(R.color.dark_surface))
    }

    private fun showAddDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add, null)
        val etSubject = view.findViewById<EditText>(R.id.etSubject)
        val tvTime = view.findViewById<TextView>(R.id.tvSelectedTime)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val rgSpeed = view.findViewById<RadioGroup>(R.id.rgSpeed)

        selectedTime = Calendar.getInstance()
        selectedTime.add(Calendar.MINUTE, 5)
        val fmt = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        tvTime.text = fmt.format(selectedTime.time)

        view.findViewById<Button>(R.id.btnPickTime).setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedTime.set(y, m, d)
                TimePickerDialog(this, { _, h, min ->
                    selectedTime.set(Calendar.HOUR_OF_DAY, h)
                    selectedTime.set(Calendar.MINUTE, min)
                    selectedTime.set(Calendar.SECOND, 0)
                    tvTime.text = fmt.format(selectedTime.time)
                }, selectedTime.get(Calendar.HOUR_OF_DAY),
                    selectedTime.get(Calendar.MINUTE), false).show()
            }, selectedTime.get(Calendar.YEAR),
                selectedTime.get(Calendar.MONTH),
                selectedTime.get(Calendar.DAY_OF_MONTH)).show()
        }

        rgGender.setOnCheckedChangeListener { _, id ->
            selectedGender = if (id == R.id.rbFemale) "female" else "male"
        }
        rgSpeed.setOnCheckedChangeListener { _, id ->
            selectedSpeed = when (id) {
                R.id.rbSlow -> 0.7f
                R.id.rbFast -> 1.4f
                else -> 1.0f
            }
        }

        android.app.AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val subject = etSubject.text.toString().trim()
                if (subject.isEmpty()) {
                    Toast.makeText(this, "Enter reminder subject!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (selectedTime.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Please pick a future time!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                saveReminder(subject)
            }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

    private fun saveReminder(subject: String) {
        Thread {
            val reminder = ReminderEntity(
                subject = subject,
                timeInMillis = selectedTime.timeInMillis,
                voiceGender = selectedGender,
                voiceSpeed = selectedSpeed
            )
            val id = db.dao().insert(reminder)
            val saved = reminder.copy(id = id.toInt())
            AlarmScheduler.schedule(this, saved)
            runOnUiThread {
                val fmt = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
                Toast.makeText(this,
                    "✅ Reminder set for ${fmt.format(Date(selectedTime.timeInMillis))}",
                    Toast.LENGTH_LONG).show()
                loadReminders()
            }
        }.start()
    }

    private fun loadReminders() {
        Thread {
            val list = db.dao().getActiveReminders()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun loadHistory() {
        Thread {
            val list = db.dao().getCompletedReminders()
            runOnUiThread { showList(list) }
        }.start()
    }

    private fun showList(list: List<ReminderEntity>) {
        val container = findViewById<LinearLayout>(R.id.reminderContainer)
        val emptyView = findViewById<TextView>(R.id.tvEmpty)
        container.removeAllViews()

        if (list.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            container.visibility = View.GONE
            return
        }

        emptyView.visibility = View.GONE
        container.visibility = View.VISIBLE
        val fmt = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())

        list.forEach { reminder ->
            val row = layoutInflater.inflate(R.layout.item_reminder, container, false)
            row.findViewById<TextView>(R.id.tvReminderSubject).text = reminder.subject
            row.findViewById<TextView>(R.id.tvReminderTime).text =
                fmt.format(Date(reminder.timeInMillis))
            val diff = reminder.timeInMillis - System.currentTimeMillis()
            row.findViewById<TextView>(R.id.tvRemaining).text = when {
                diff < 0 -> "Time passed"
                diff < 3600000 -> "${diff / 60000} mins remaining"
                diff < 86400000 -> "${diff / 3600000} hrs remaining"
                else -> "${diff / 86400000} days remaining"
            }
            row.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
                Thread {
                    // Cancel alarm AND mark cancelled in DB
                    AlarmScheduler.cancel(this, reminder.id)
                    db.dao().markCancelled(reminder.id)
                    // Stop service if currently ringing this reminder
                    if (ReminderService.currentReminderId == reminder.id) {
                        stopService(Intent(this, ReminderService::class.java))
                    }
                    runOnUiThread { loadReminders() }
                }.start()
            }
            container.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        startWatchdog()
        loadReminders()
    }
}
