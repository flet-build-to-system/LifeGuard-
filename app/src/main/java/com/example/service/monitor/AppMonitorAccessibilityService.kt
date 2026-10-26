package com.example.service.monitor

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.local.LifeGuardDatabase
import kotlinx.coroutines.*

class AppMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        serviceScope.launch {
            val dao = LifeGuardDatabase.getInstance(applicationContext).appRestrictionDao()
            val restriction = dao.getRestriction(packageName)
            if (restriction != null && (restriction.isBlocked || restriction.requiresBreathingExercise)) {
                val now = System.currentTimeMillis()
                // Prevent quick repeats
                if (lastInterceptedPackage == packageName && (now - lastInterceptionTime) < 10000L) {
                    return@launch
                }

                lastInterceptedPackage = packageName
                lastInterceptionTime = now

                withContext(Dispatchers.Main) {
                    val interceptIntent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra("NAVIGATE_TO", "INTERVENTION")
                        putExtra("TARGET_PACKAGE", packageName)
                        putExtra("APP_NAME", restriction.appName)
                        putExtra("DELAY_SECONDS", restriction.delaySeconds)
                    }
                    startActivity(interceptIntent)
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
