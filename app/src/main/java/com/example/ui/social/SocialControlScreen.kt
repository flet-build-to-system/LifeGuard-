package com.example.ui.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.AppRestrictionEntity
import com.example.data.repository.DeviceManagerRepository
import com.example.domain.model.InstalledAppInfo
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SocialControlScreen(
    onTriggerBreathingDemo: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { LifeGuardDatabase.getInstance(context) }
    val deviceRepo = remember { DeviceManagerRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    val restrictions by db.appRestrictionDao().getAllRestrictions().collectAsState(initial = emptyList())
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var showAddAppDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = deviceRepo.getInstalledApps()
            withContext(Dispatchers.Main) {
                installedApps = apps
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
                    text = "حدد أوقات استخدام واعية واستبدل التشتت التلقائي بوقفات تنفس هادئة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
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
                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = EmeraldHealth)
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                            Text(
                                text = "الحد اليومي: ${app.dailyLimitMinutes} دقيقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent
                            )
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

                    Spacer(modifier = Modifier.height(12.dp))

                    // Daily Limit Slider
                    Text(
                        text = "تعديل الحد اليومي: ${app.dailyLimitMinutes} دقيقة",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Slider(
                        value = app.dailyLimitMinutes.toFloat(),
                        onValueChange = { newLimit ->
                            coroutineScope.launch {
                                db.appRestrictionDao().upsertRestriction(
                                    app.copy(dailyLimitMinutes = newLimit.toInt())
                                )
                            }
                        },
                        valueRange = 10f..180f,
                        steps = 16,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "وقفة تنفس واعية إجبارية",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
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
                            text = "حظر كامل بعد استنفاد الوقت",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = app.isBlocked,
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
