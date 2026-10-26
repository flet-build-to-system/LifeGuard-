package com.example.ui.dashboard

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.preferences.LifeGuardPreferences
import com.example.data.repository.DeviceManagerRepository
import com.example.data.repository.UsageStatsRepository
import com.example.domain.model.BatterySpecs
import com.example.domain.model.DeviceSpecs
import com.example.ui.components.WeeklyScreenTimeBarChart
import com.example.ui.components.WellbeingScoreCircularGauge
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen(
    onNavigateToSocial: () -> Unit,
    onNavigateToWebFilter: () -> Unit,
    onNavigateToFocus: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { LifeGuardDatabase.getInstance(context) }
    val deviceRepo = remember { DeviceManagerRepository(context) }
    val usageRepo = remember { UsageStatsRepository(context) }
    val prefs = remember { LifeGuardPreferences(context) }

    var screenTimeMinutes by remember { mutableLongStateOf(0L) }
    var weeklyUsage by remember { mutableStateOf(listOf(150, 190, 220, 180, 160, 210, 130)) }
    var hasUsagePermission by remember { mutableStateOf(usageRepo.hasUsageStatsPermission()) }
    var deviceSpecs by remember { mutableStateOf<DeviceSpecs?>(null) }
    var batterySpecs by remember { mutableStateOf<BatterySpecs?>(null) }

    val resistedCount by db.urgeLogDao().getResistedCount().collectAsState(initial = 0)
    val blockedHitsCount by db.blockedDomainDao().getTotalBlockedHits().collectAsState(initial = 0)
    val focusMinutesCount by db.focusSessionDao().getTotalCompletedMinutes().collectAsState(initial = 0)
    val isWebShieldActive by prefs.isWebShieldEnabled.collectAsState(initial = false)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val specs = deviceRepo.getDeviceSpecs()
            val battery = deviceRepo.getBatterySpecs()
            val st = usageRepo.getDailyScreenTimeMinutes()
            val wu = usageRepo.getWeeklyUsageHistory()
            withContext(Dispatchers.Main) {
                deviceSpecs = specs
                batterySpecs = battery
                screenTimeMinutes = st
                weeklyUsage = wu
                hasUsagePermission = usageRepo.hasUsageStatsPermission()
            }
        }
    }

    val wellbeingScore = remember(screenTimeMinutes, resistedCount, focusMinutesCount) {
        var score = 100
        if (screenTimeMinutes > 300) score -= 35
        else if (screenTimeMinutes > 180) score -= 20
        else if (screenTimeMinutes > 120) score -= 10

        score += (resistedCount * 4).coerceAtMost(15)
        score += ((focusMinutesCount ?: 0) / 10).coerceAtMost(10)
        score.coerceIn(15, 100)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Title & Tagline
        item {
            Column {
                Text(
                    text = "حارس الحياة • لوحة التحكم",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "العافية الرقمية والتحكم الذاتي وإدارة الجهاز",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Circular Wellbeing Score & Screen Time
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WellbeingScoreCircularGauge(score = wellbeingScore)

                    Column {
                        Text(
                            text = "وقت الشاشة اليوم",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        val hours = screenTimeMinutes / 60
                        val mins = screenTimeMinutes % 60
                        val timeStr = if (hours > 0) "${hours}س ${mins}د" else "${mins}دقيقة"

                        Text(
                            text = if (hasUsagePermission) timeStr else "يحتاج إذن",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hasUsagePermission) EmeraldHealth else GoldAccent
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "الدوافع المقاومة",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "$resistedCount نجاح",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }

        // Usage Permission Notice Card if not granted
        if (!hasUsagePermission) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "تفعيل صلاحية الوصول لبيانات الاستخدام",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "لقياس وقت الشاشة الحقيقي وتطبيق القيود على تطبيقات التواصل، يرجى منح إذن الاستخدام من إعدادات النظام.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            modifier = Modifier.testTag("grant_usage_permission_button")
                        ) {
                            Text("منح الإذن الآن")
                        }
                    }
                }
            }
        }

        // Quick Metrics Strip
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Focus Minutes Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_focus_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = EmeraldLight)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${focusMinutesCount ?: 0} دقيقة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تركيز منجز",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Blocked Attempts Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_blocked_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = if (isWebShieldActive) EmeraldHealth else GoldAccent)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${blockedHitsCount ?: 0} حجب",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isWebShieldActive) "الدرع نشط" else "درع الويب متوقف",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Weekly Trends Canvas Bar Chart
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "اتجاهات وقت الشاشة الأسبوعية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "آخر 7 أيام",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyScreenTimeBarChart(dailyMinutes = weeklyUsage)
                }
            }
        }

        // Intelligent Insights Card (Evidence-informed, real data based)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Insights, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تقرير ورؤى ذكية لليوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))

                    val insightText = when {
                        screenTimeMinutes > 240 -> "استهلاك الشاشة اليوم مرتفع نسبياً (${screenTimeMinutes / 60}ساعة). جرب تشغيل جلسة تركيز مدتها 25 دقيقة لتخفيف التشتت واستعادة صفائك الذهني."
                        resistedCount > 0 -> "رائع جداً! تمكنت من مقاومة $resistedCount دافع قهري اليوم. هذا يبني مسارات عصبية جديدة ويعزز انضباطك الذاتي."
                        isWebShieldActive -> "درع الويب نشط ويحجب محاولات الاتصال بالنطاقات الإباحية محلياً بدون إبطاء الشبكة أو مشاركة بياناتك."
                        else -> "خطتك اليومية متزنة. تذكر: وقفة تنفس لمدة 10 ثوانٍ قبل فتح أي تطبيق مشتت كفيلة بكسر حلقة الإدمان التلقائي."
                    }

                    Text(
                        text = insightText,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Quick Hardware Specs Strip
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    batterySpecs?.let { b ->
                        Column {
                            Text(text = "البطارية", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${b.percentage}% • ${if (b.isCharging) "شحن ⚡" else "استخدام"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldHealth
                            )
                        }
                    }

                    deviceSpecs?.let { d ->
                        val freeGb = d.freeInternalStorageBytes / (1024 * 1024 * 1024)
                        Column {
                            Text(text = "التخزين الحر", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "$freeGb جيجابايت",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldAccent
                            )
                        }
                        Column {
                            Text(text = "إصدار النظام", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "أندرويد ${d.androidVersion}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
