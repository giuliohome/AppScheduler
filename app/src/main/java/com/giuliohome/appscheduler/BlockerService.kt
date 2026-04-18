package com.giuliohome.appscheduler

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import java.util.Calendar

class BlockerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            val prefs = getSharedPreferences("BlockerPrefs", Context.MODE_PRIVATE)

            val alwaysBlocked = prefs.getStringSet("always_blocked_pkgs", DEFAULT_ALWAYS) ?: DEFAULT_ALWAYS
            val scheduledApps = prefs.getStringSet("scheduled_pkgs", DEFAULT_SCHEDULED) ?: DEFAULT_SCHEDULED

            if (alwaysBlocked.contains(packageName)) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                return
            }

            if (scheduledApps.contains(packageName)) {
                if (isBlockActive(prefs)) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }
    }

    private fun isBlockActive(prefs: android.content.SharedPreferences): Boolean {
        val startMin = prefs.getInt("start_time", 1320) // 22:00
        val endMin = prefs.getInt("end_time", 420)      // 07:00

        val now = Calendar.getInstance()
        val currentMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        return if (startMin < endMin) {
            currentMin in startMin..endMin
        } else {
            currentMin >= startMin || currentMin <= endMin
        }
    }

    override fun onInterrupt() {}

    companion object {
        val DEFAULT_ALWAYS: Set<String> = setOf("com.patreon.android")
        val DEFAULT_SCHEDULED: Set<String> = setOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.android.chrome",
            "com.instagram.android",
            "com.zhiliaoapp.musically"
        )
    }
}
