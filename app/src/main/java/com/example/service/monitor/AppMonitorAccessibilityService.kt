package com.example.service.monitor

import android.accessibilityservice.AccessibilityService
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.data.local.LifeGuardDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.util.Calendar

class AppMonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastInterceptedPackage: String? = null
    private var lastInterceptionTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return
        if (packageName == "com.android.systemui" || packageName.contains("launcher")) return

        serviceScope.launch {
            val prefs = com.example.data.preferences.LifeGuardPreferences(applicationContext)
            val isMasterEnabled = prefs.isAppRestrictionMasterEnabled.first()
            if (!isMasterEnabled) return@launch

            val db = LifeGuardDatabase.getInstance(applicationContext)

            // 1. Browser Website Inspection (Guarantees Chrome blocks custom & adult websites even if DoH is active)
            val browserPackages = setOf(
                "com.android.chrome",
                "com.chrome.beta",
                "com.chrome.canary",
                "com.chrome.dev",
                "org.mozilla.firefox",
                "com.sec.android.app.sbrowser",
                "com.opera.browser",
                "com.microsoft.emmx"
            )
            if (packageName in browserPackages) {
                checkBrowserUrlAndBlock(packageName, db)
            }

            // 2. Only inspect App Restrictions on Window State Changed (app switch / launch)
            if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return@launch

            val dao = db.appRestrictionDao()
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

    private suspend fun checkBrowserUrlAndBlock(packageName: String, db: LifeGuardDatabase) {
        try {
            val root = rootInActiveWindow ?: return
            val urlNodes = root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
                .ifEmpty { root.findAccessibilityNodeInfosByViewId("$packageName:id/url_bar") }
            val urlText = urlNodes.firstOrNull()?.text?.toString() ?: return
            if (urlText.isBlank() || !urlText.contains(".")) return

            val cleanDomain = urlText.trim().lowercase()
                .removePrefix("https://").removePrefix("http://").removePrefix("www.")
                .substringBefore("/").substringBefore(":")

            val blockedDao = db.blockedDomainDao()
            val parts = cleanDomain.split(".")
            var matchedBlockedDomain: String? = null
            for (i in parts.indices) {
                val subDomain = parts.subList(i, parts.size).joinToString(".")
                if (blockedDao.isDomainBlocked(subDomain)) {
                    matchedBlockedDomain = subDomain
                    break
                }
            }

            if (matchedBlockedDomain != null) {
                val now = System.currentTimeMillis()
                if (lastInterceptedPackage == cleanDomain && (now - lastInterceptionTime) < 4000L) {
                    return
                }
                lastInterceptedPackage = cleanDomain
                lastInterceptionTime = now

                blockedDao.incrementBlockedHits(matchedBlockedDomain)

                withContext(Dispatchers.Main) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    val interceptIntent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("NAVIGATE_TO", "INTERVENTION")
                        putExtra("TARGET_PACKAGE", packageName)
                        putExtra("APP_NAME", "موقع محظور: $matchedBlockedDomain")
                        putExtra("IS_LIMIT_EXCEEDED", true)
                        putExtra("USED_MINUTES", 0)
                        putExtra("LIMIT_MINUTES", 0)
                        putExtra("DELAY_SECONDS", 10)
                    }
                    startActivity(interceptIntent)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
