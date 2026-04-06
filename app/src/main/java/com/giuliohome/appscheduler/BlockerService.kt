package com.giuliohome.appscheduler

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

class BlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            // App da bloccare SEMPRE (es. Patreon)
            val alwaysBlocked = listOf("com.patreon.android")
            
            // App da bloccare solo in fascia oraria (YouTube e Chrome)
            val scheduledApps = listOf("com.google.android.youtube", "com.android.chrome", "com.instagram.android", "com.zhiliaoapp.musically")
            
            if (alwaysBlocked.contains(packageName)) {
                // Blocco immediato e silenzioso
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            if (scheduledApps.contains(packageName)) {
                if (isBlockActive()) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }
    }

    private fun isBlockActive(): Boolean {
        val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)
        
        val startMin = prefs.getInt("start_time", 1320) // 22:00
        val endMin = prefs.getInt("end_time", 420)     // 07:00

        val now = Calendar.getInstance()
        val currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (startMin < endMin) {
            currentMin in startMin..endMin
        } else {
            currentMin >= startMin || currentMin <= endMin
        }
    }

    override fun onInterrupt() {}
}