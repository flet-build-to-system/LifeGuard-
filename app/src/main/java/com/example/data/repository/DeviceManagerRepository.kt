package com.example.data.repository

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import com.example.domain.model.BatterySpecs
import com.example.domain.model.DeviceSpecs
import com.example.domain.model.InstalledAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeviceManagerRepository(private val context: Context) {

    suspend fun getDeviceSpecs(): DeviceSpecs = withContext(Dispatchers.IO) {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memoryInfo)

        val stat = StatFs(Environment.getDataDirectory().path)
        val totalStorage = stat.totalBytes
        val freeStorage = stat.availableBytes

        val patch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH
        } else {
            "N/A"
        }

        DeviceSpecs(
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkInt = Build.VERSION.SDK_INT,
            securityPatch = patch,
            totalRamBytes = memoryInfo.totalMem,
            availableRamBytes = memoryInfo.availMem,
            totalInternalStorageBytes = totalStorage,
            freeInternalStorageBytes = freeStorage
        )
    }

    fun getBatterySpecs(): BatterySpecs {
        val iFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val bStatus: Intent? = context.registerReceiver(null, iFilter)

        val level = bStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = bStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val temp = (bStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        val voltage = bStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

        val healthCode = bStatus?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val health = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "سليمة (Good)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "حرارة مرتفعة (Overheat)"
            BatteryManager.BATTERY_HEALTH_DEAD -> "تالفة (Dead)"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "جهد زائد (Over Voltage)"
            else -> "طبيعية (Normal)"
        }

        return BatterySpecs(pct, isCharging, temp, voltage, health)
    }

    fun getNetworkStatus(): String {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connMgr.activeNetwork ?: return "غير متصل (Offline)"
        val caps = connMgr.getNetworkCapabilities(activeNetwork) ?: return "غير معروف"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "متصل عبر Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "متصل عبر بيانات الجوال (Cellular)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "متصل سلكياً (Ethernet)"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "نفق مشفر (VPN)"
            else -> "شبكة أخرى متصلة"
        }
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        resolveInfos.mapNotNull { resolveInfo ->
            val pkg = resolveInfo.activityInfo.packageName
            val name = resolveInfo.loadLabel(pm).toString()
            val isSystem = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            } catch (_: Exception) {
                false
            }
            InstalledAppInfo(
                packageName = pkg,
                appName = name,
                isSystemApp = isSystem
            )
        }.distinctBy { it.packageName }
         .sortedBy { it.appName.lowercase() }
    }

    fun clearAppCache(): Long {
        val cacheDir = context.cacheDir
        var deletedBytes = 0L
        cacheDir.listFiles()?.forEach { file ->
            deletedBytes += file.length()
            file.deleteRecursively()
        }
        return deletedBytes
    }

    fun openAppSystemSettings(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openStorageSettings() {
        val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(fallback)
        }
    }
}
