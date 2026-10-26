package com.example.ui.social

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LifeGuardDatabase
import com.example.data.local.entity.AppRestrictionEntity
import com.example.data.repository.DeviceManagerRepository
import com.example.domain.model.InstalledAppInfo
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@Composable
fun SocialControlScreen(
    onTriggerBreathingDemo: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { LifeGuardDatabase.getInstance(context) }
    val prefs = remember { com.example.data.preferences.LifeGuardPreferences(context) }
    val deviceRepo = remember { DeviceManagerRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val isMasterEnabled by prefs.isAppRestrictionMasterEnabled.collectAsState(initial = true)
    val restrictions by db.appRestrictionDao().getAllRestrictions().collectAsState(initial = emptyList())
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var appsUsageToday by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var showAddAppDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = deviceRepo.getInstalledApps()

            val usageMap = mutableMapOf<String, Int>()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            val stats = usageStatsManager?.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY, cal.timeInMillis, System.currentTimeMillis()
            )
            stats?.forEach { stat ->
                val mins = (stat.totalTimeInForeground / (1000 * 60)).toInt()
                usageMap[stat.packageName] = (usageMap[stat.packageName] ?: 0) + mins
            }

            withContext(Dispatchers.Main) {
                installedApps = apps
                appsUsageToday = usageMap
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "مكافحة إدمان وسائل التواصل",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "حدد حدوداً يومية ذكية واستبدل التشتت التلقائي بوقفات تنفس هادئة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Master App Restriction Switch Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMasterEnabled) EmeraldHealth.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_restriction_master_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isMasterEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = if (isMasterEnabled) EmeraldHealth else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isMasterEnabled) "خاصية التقييد: مفعّلة ونشطة" else "خاصية التقييد: متوقفة ومقفلة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMasterEnabled) EmeraldHealth else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isMasterEnabled)
                                "يقوم النظام بمراقبة استخدام التطبيقات المقيدة، وفرض وقفات التنفس، والحظر الفوري عند تجاوز الحد اليومي."
                            else
                                "تم إيقاف نظام التقييد مؤقتاً — لن يتم حظر أي تطبيق أو إظهار شاشات التوقف حتى تعيد تفعيله.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }

                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = { enabled ->
                            coroutineScope.launch {
                                prefs.setAppRestrictionMasterEnabled(enabled)
                            }
                        },
                        modifier = Modifier.testTag("app_restriction_master_toggle")
                    )
                }
            }
        }

        // Accessibility Restricted Settings Helper Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("accessibility_fix_guide_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccessibilityNew, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "حل مشكلة تعذر تفعيل خدمة الوصول (أندرويد 13 فما فوق)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "إذا ظهرت لك رسالة «إعداد مقيد - لدواعي الأمان هذا الإعداد غير متاح حالياً»، فهذا قيد أمان تلقائي في أندرويد للتطبيقات المحملة يدوياً. يمكنك فكه خلال ثوانٍ:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1️⃣ اضغط على زر «1. فتح معلومات التطبيق» بالأسفل.\n2️⃣ في أعلى زاوية الشاشة، اضغط على النقاط الثلاث (⋮).\n3️⃣ اختر «السماح بالإعدادات المقيدة» (Allow restricted settings) وأدخل رمز القفل.\n4️⃣ اضغط على «2. فتح إمكانية الوصول» وستتمكن من تفعيل الخدمة فوراً!",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("1. معلومات التطبيق (⋮)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("2. إمكانية الوصول", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Gradual Reduction Plan Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = EmeraldHealth)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "خطة التخفيض التدريجي (10% أسبوعياً)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "التغيير المستدام يبدأ بالتدريج. يساعدك النظام على تقليص فترات تصفح مقاطع الفيديو القصيرة أسبوعياً دون صدمات أو انتكاسات مفاجئة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Add App Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "التطبيقات المقيدة (${restrictions.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                FilledTonalButton(
                    onClick = { showAddAppDialog = true },
                    modifier = Modifier.testTag("add_restricted_app_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("إضافة تطبيق")
                }
            }
        }

        // Restricted Apps List
        items(restrictions, key = { it.packageName }) { app ->
            val todayUsed = appsUsageToday[app.packageName] ?: 0
            val isExceeded = todayUsed >= app.dailyLimitMinutes

            val currentHours = app.dailyLimitMinutes / 60
            val currentMins = app.dailyLimitMinutes % 60

            val formattedLimit = buildString {
                if (currentHours > 0) {
                    append("$currentHours ساعة ")
                    if (currentMins > 0) append("و $currentMins دقيقة")
                } else {
                    append("$currentMins دقيقة")
                }
                append(" (إجمالي: ${app.dailyLimitMinutes} دقيقة)")
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExceeded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_card_${app.appName}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "المستهلك اليوم: $todayUsed دقيقة / الحد: ${app.dailyLimitMinutes} دقيقة",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isExceeded) MaterialTheme.colorScheme.error else GoldAccent
                                )
                                if (isExceeded) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("تجاوز الحد ⛔", color = Color.White, modifier = Modifier.padding(horizontal = 4.dp))
                                    }
                                }
                            }
                        }

                        // Demo Breathing Button
                        OutlinedButton(
                            onClick = { onTriggerBreathingDemo(app.appName) },
                            modifier = Modifier.testTag("test_breathing_${app.appName}")
                        ) {
                            Icon(Icons.Default.SelfImprovement, contentDescription = null, tint = EmeraldHealth, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تمرين التنفس", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily Limit Header with Hours & Minutes Display
                    Text(
                        text = "الحد اليومي المسموح: $formattedLimit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldHealth
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hours Selector Chips
                    Text(
                        text = "الساعات:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 1, 2, 3, 4, 5).forEach { hour ->
                            val isSelected = (currentHours == hour)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newTotal = (hour * 60) + currentMins
                                    val safeTotal = if (newTotal <= 0) 5 else newTotal
                                    coroutineScope.launch {
                                        db.appRestrictionDao().upsertRestriction(
                                            app.copy(dailyLimitMinutes = safeTotal)
                                        )
                                    }
                                },
                                label = { Text("$hour س", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minutes Selector Chips
                    Text(
                        text = "الدقائق:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0, 10, 15, 20, 30, 45).forEach { min ->
                            val isSelected = (currentMins == min)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newTotal = (currentHours * 60) + min
                                    val safeTotal = if (newTotal <= 0) 5 else newTotal
                                    coroutineScope.launch {
                                        db.appRestrictionDao().upsertRestriction(
                                            app.copy(dailyLimitMinutes = safeTotal)
                                        )
                                    }
                                },
                                label = { Text("$min د", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Fine-tune Slider
                    Slider(
                        value = app.dailyLimitMinutes.coerceIn(5, 360).toFloat(),
                        onValueChange = { newLimit ->
                            coroutineScope.launch {
                                db.appRestrictionDao().upsertRestriction(
                                    app.copy(dailyLimitMinutes = newLimit.toInt())
                                )
                            }
                        },
                        valueRange = 5f..360f,
                        steps = 70,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "وقفة تنفس واعية إجبارية قبل الدخول",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = app.requiresBreathingExercise,
                            onCheckedChange = { isEnabled ->
                                coroutineScope.launch {
                                    db.appRestrictionDao().upsertRestriction(
                                        app.copy(requiresBreathingExercise = isEnabled)
                                    )
                                }
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حظر كامل وفوري بعد استنفاد الوقت",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = app.isBlocked || isExceeded,
                            onCheckedChange = { isBlocked ->
                                coroutineScope.launch {
                                    db.appRestrictionDao().updateBlockedStatus(app.packageName, isBlocked)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add App Selection Dialog
    if (showAddAppDialog) {
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            title = { Text("اختر تطبيقاً من جهازك") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    val availableApps = installedApps.filter { app ->
                        restrictions.none { it.packageName == app.packageName }
                    }
                    if (availableApps.isEmpty()) {
                        item {
                            Text("لا توجد تطبيقات إضافية قابلة للإضافة حالياً.")
                        }
                    } else {
                        items(availableApps) { app ->
                            TextButton(
                                onClick = {
                                    coroutineScope.launch {
                                        db.appRestrictionDao().upsertRestriction(
                                            AppRestrictionEntity(
                                                packageName = app.packageName,
                                                appName = app.appName,
                                                dailyLimitMinutes = 30,
                                                requiresBreathingExercise = true
                                            )
                                        )
                                    }
                                    showAddAppDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(app.appName, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddAppDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
