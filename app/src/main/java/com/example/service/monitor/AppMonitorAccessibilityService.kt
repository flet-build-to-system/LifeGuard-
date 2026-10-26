package com.example.service.monitor

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.local.LifeGuardDatabase
import kotlinx.coroutines.*
import java.util.Calendar

class AppMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui" || packageName.contains("launcher")) return

        serviceScope.launch {
            val dao = LifeGuardDatabase.getInstance(applicationContext).appRestrictionDao()
            val restriction = dao.getRestriction(packageName) ?: return@launch

            // Calculate actual usage time today from UsageStatsManager
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis()
            )
            val todayMillis = stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
            val todayMinutes = (todayMillis / (1000 * 60)).toInt()

            val isLimitExceeded = (todayMinutes >= restriction.dailyLimitMinutes)

            if (isLimitExceeded || restriction.isBlocked || restriction.requiresBreathingExercise) {
                val now = System.currentTimeMillis()
                // Prevent duplicate triggering in quick bursts (5 seconds)
                if (lastInterceptedPackage == packageName && (now - lastInterceptionTime) < 5000L) {
                    return@launch
                }

                lastInterceptedPackage = packageName
                lastInterceptionTime = now

                withContext(Dispatchers.Main) {
                    val interceptIntent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "INTERVENTION")
                        putExtra("TARGET_PACKAGE", packageName)
                        putExtra("APP_NAME", restriction.appName)
                        putExtra("IS_LIMIT_EXCEEDED", isLimitExceeded)
                        putExtra("USED_MINUTES", todayMinutes)
                        putExtra("LIMIT_MINUTES", restriction.dailyLimitMinutes)
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
