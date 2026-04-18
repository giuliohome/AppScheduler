package com.giuliohome.appscheduler

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var pinLayout: LinearLayout
    private lateinit var settingsLayout: ScrollView
    private lateinit var pinInput: EditText
    private lateinit var pinSubmit: Button
    private lateinit var pinTitle: TextView

    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var tvServiceStatus: TextView
    private lateinit var btnOpenAccessibility: Button
    private lateinit var btnOpenAppInfo: Button

    private lateinit var etAlwaysPkg: EditText
    private lateinit var btnAlwaysAdd: Button
    private lateinit var containerAlwaysList: LinearLayout
    private lateinit var etScheduledPkg: EditText
    private lateinit var btnScheduledAdd: Button
    private lateinit var containerScheduledList: LinearLayout

    private val prefs by lazy { getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pinLayout = findViewById(R.id.pinLayout)
        settingsLayout = findViewById(R.id.settingsLayout)
        pinInput = findViewById(R.id.pinInput)
        pinSubmit = findViewById(R.id.pinSubmit)
        pinTitle = findViewById(R.id.pinTitle)

        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        tvServiceStatus = findViewById(R.id.tvServiceStatus)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)
        btnOpenAppInfo = findViewById(R.id.btnOpenAppInfo)

        etAlwaysPkg = findViewById(R.id.etAlwaysPkg)
        btnAlwaysAdd = findViewById(R.id.btnAlwaysAdd)
        containerAlwaysList = findViewById(R.id.containerAlwaysList)
        etScheduledPkg = findViewById(R.id.etScheduledPkg)
        btnScheduledAdd = findViewById(R.id.btnScheduledAdd)
        containerScheduledList = findViewById(R.id.containerScheduledList)

        setupPinLogic()
        setupSettingsLogic()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupPinLogic() {
        val savedPin = prefs.getString("user_pin", null)
        pinTitle.text = if (savedPin == null) "Imposta PIN (4 cifre)" else "Inserisci PIN"

        pinSubmit.setOnClickListener {
            val input = pinInput.text.toString()
            if (input.length == 4) {
                if (savedPin == null) {
                    prefs.edit().putString("user_pin", input).apply()
                    showSettings()
                } else if (input == savedPin) {
                    showSettings()
                } else {
                    Toast.makeText(this, "PIN Errato", Toast.LENGTH_SHORT).show()
                    pinInput.text.clear()
                }
            }
        }
    }

    private fun showSettings() {
        pinLayout.visibility = View.GONE
        settingsLayout.visibility = View.VISIBLE
        updateTimeButtons()
        renderList(KEY_ALWAYS, BlockerService.DEFAULT_ALWAYS, containerAlwaysList)
        renderList(KEY_SCHEDULED, BlockerService.DEFAULT_SCHEDULED, containerScheduledList)
    }

    private fun setupSettingsLogic() {
        btnStartTime.setOnClickListener { showTimePicker("start_time") }
        btnEndTime.setOnClickListener { showTimePicker("end_time") }

        btnOpenAppInfo.setOnClickListener {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }

        btnOpenAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnAlwaysAdd.setOnClickListener {
            addPackage(KEY_ALWAYS, etAlwaysPkg, BlockerService.DEFAULT_ALWAYS, containerAlwaysList)
        }
        btnScheduledAdd.setOnClickListener {
            addPackage(KEY_SCHEDULED, etScheduledPkg, BlockerService.DEFAULT_SCHEDULED, containerScheduledList)
        }
    }

    private fun addPackage(key: String, input: EditText, defaults: Set<String>, container: LinearLayout) {
        val pkg = input.text.toString().trim()
        if (pkg.isEmpty()) return
        val current = prefs.getStringSet(key, defaults)?.toMutableSet() ?: defaults.toMutableSet()
        if (current.add(pkg)) {
            prefs.edit().putStringSet(key, current).apply()
            input.text.clear()
            renderList(key, defaults, container)
        } else {
            Toast.makeText(this, "Già presente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removePackage(key: String, pkg: String, defaults: Set<String>, container: LinearLayout) {
        val current = prefs.getStringSet(key, defaults)?.toMutableSet() ?: defaults.toMutableSet()
        if (current.remove(pkg)) {
            prefs.edit().putStringSet(key, current).apply()
            renderList(key, defaults, container)
        }
    }

    private fun renderList(key: String, defaults: Set<String>, container: LinearLayout) {
        container.removeAllViews()
        val current = prefs.getStringSet(key, defaults) ?: defaults
        current.sorted().forEach { pkg ->
            val row = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val label = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                text = pkg
                textSize = 14f
            }
            val btn = Button(this).apply { text = "Rimuovi" }
            btn.setOnClickListener { removePackage(key, pkg, defaults, container) }
            row.addView(label)
            row.addView(btn)
            container.addView(row)
        }
    }

    private fun showTimePicker(key: String) {
        val currentMin = prefs.getInt(key, if (key == "start_time") 1320 else 420)
        TimePickerDialog(this, { _, h, m ->
            prefs.edit().putInt(key, h * 60 + m).apply()
            updateTimeButtons()
        }, currentMin / 60, currentMin % 60, true).show()
    }

    private fun updateTimeButtons() {
        val startMin = prefs.getInt("start_time", 1320)
        val endMin = prefs.getInt("end_time", 420)
        btnStartTime.text = "Inizio Blocco: ${formatTime(startMin)}"
        btnEndTime.text = "Fine Blocco: ${formatTime(endMin)}"
    }

    private fun formatTime(min: Int) = String.format("%02d:%02d", min / 60, min % 60)

    private fun updateServiceStatus() {
        val enabled = isServiceEnabled()
        tvServiceStatus.text = "Servizio: ${if (enabled) "ATTIVO" else "NON ATTIVO"}"
        tvServiceStatus.setTextColor(if (enabled) Color.GREEN else Color.RED)
    }

    private fun isServiceEnabled(): Boolean {
        val expected = android.content.ComponentName(this, BlockerService::class.java).flattenToString()
        return Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(expected) ?: false
    }

    companion object {
        private const val KEY_ALWAYS = "always_blocked_pkgs"
        private const val KEY_SCHEDULED = "scheduled_pkgs"
    }
}
