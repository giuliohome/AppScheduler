package com.giuliohome.appscheduler

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.widget.ScrollView

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
}