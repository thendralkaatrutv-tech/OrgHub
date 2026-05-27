package com.orghub.app

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: ReminderDatabase
    private val scope = CoroutineScope(Dispatchers.Main)
    private var selectedTime = Calendar.getInstance()
    private var selectedGender = "female"
    private var selectedSpeed = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        db = ReminderDatabase.get(this)

        checkExactAlarmPermission()
        setupUI()
        loadReminders()
    }

    private fun setupUI() {
        // Tabs
        val tabActive = findViewById<TextView>(R.id.tabActive)
        val tabHistory = findViewById<TextView>(R.id.tabHistory)
        tabActive.setOnClickListener { loadReminders(); highlightTab(true) }
        tabHistory.setOnClickListener { loadHistory(); highlightTab(false) }

        // FAB
        findViewById<View>(R.id.fabAdd).setOnClickListener {
            showAddDialog()
        }
    }

    private fun highlightTab(activeSelected: Boolean) {
        val tabActive = findViewById<TextView>(R.id.tabActive)
        val tabHistory = findViewById<TextView>(R.id.tabHistory)
        tabActive.setBackgroundColor(if (activeSelected)
            getColor(R.color.olive) else getColor(R.color.dark_surface))
        tabHistory.setBackgroundColor(if (!activeSelected)
            getColor(R.color.olive) else getColor(R.color.dark_surface))
    }

    private fun showAddDialog() {
        val dialog = android.app.AlertDialog.Builder(this, R.style.DialogTheme)
        val view = layoutInflater.inflate(R.layout.dialog_add, null)
        dialog.setView(view)

        val etSubject = view.findViewById<EditText>(R.id.etSubject)
        val tvTime = view.findViewById<TextView>(R.id.tvSelectedTime)
        val rgGender = view.findViewById<RadioGroup>(R.id.rgGender)
        val rgSpeed = view.findViewById<RadioGroup>(R.id.rgSpeed)
        val btnPickTime = view.findViewById<Button>(R.id.btnPickTime)

        selectedTime = Calendar.getInstance()
        tvTime.text = "No time selected"

        btnPickTime.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selectedTime.set(y, m, d)
                TimePickerDialog(this, { _, h, min ->
                    selectedTime.set(Calendar.HOUR_OF_DAY, h)
                    selectedTime.set(Calendar.MINUTE, min)
                    selectedTime.set(Calendar.SECOND, 0)
                    val fmt = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
                    tvTime.text = fmt.format(selectedTime.time)
                }, selectedTime.get(Calendar.HOUR_OF_DAY), selectedTime.get(Calendar.MINUTE), false).show()
            }, selectedTime.get(Calendar.YEAR), selectedTime.get(Calendar.MONTH),
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

        dialog.setPositiveButton("Save") { _, _ ->
            val subject = etSubject.text.toString().trim()
            if (subject.isEmpty()) {
                Toast.makeText(this, "Enter reminder subject!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            if (selectedTime.timeInMillis <= System.currentTimeMillis()) {
                Toast.makeText(this, "Pick a future time!", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            saveReminder(subject)
        }
        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    private fun saveReminder(subject: String) {
        scope.launch {
            val reminder = ReminderEntity(
                subject = subject,
                timeInMillis = selectedTime.timeInMillis,
                voiceGender = selectedGender,
                voiceSpeed = selectedSpeed
            )
            val id = withContext(Dispatchers.IO) { db.dao().insert(reminder) }
            val saved = reminder.copy(id = id.toInt())
            AlarmScheduler.schedule(this@MainActivity, saved)
            Toast.makeText(this@MainActivity, "Reminder saved! ✅", Toast.LENGTH_SHORT).show()
            loadReminders()
        }
    }

    private fun loadReminders() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { db.dao().getActiveReminders() }
            showList(list)
        }
    }

    private fun loadHistory() {
        scope.launch {
            val list = withContext(Dispatchers.IO) { db.dao().getCompletedReminders() }
            showList(list)
        }
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
            row.findViewById<TextView>(R.id.tvReminderTime).text = fmt.format(Date(reminder.timeInMillis))

            val diff = reminder.timeInMillis - System.currentTimeMillis()
            val remaining = when {
                diff < 0 -> "Completed"
                diff < 3600000 -> "${diff / 60000} mins remaining"
                diff < 86400000 -> "${diff / 3600000} hrs remaining"
                else -> "${diff / 86400000} days remaining"
            }
            row.findViewById<TextView>(R.id.tvRemaining).text = remaining

            row.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
                scope.launch {
                    withContext(Dispatchers.IO) {
                        AlarmScheduler.cancel(this@MainActivity, reminder.id)
                        db.dao().deleteById(reminder.id)
                    }
                    loadReminders()
                }
            }
            container.addView(row)
        }
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:$packageName")))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadReminders()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
