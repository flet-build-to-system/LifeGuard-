package com.example.ui.devicemanager

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.LifeGuardPreferences
import com.example.data.repository.DeviceManagerRepository
import com.example.domain.model.BatterySpecs
import com.example.domain.model.DeviceSpecs
import com.example.domain.model.ProtectionProfile
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DeviceManagerScreen() {
    val context = LocalContext.current
    val deviceRepo = remember { DeviceManagerRepository(context) }
    val prefs = remember { LifeGuardPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    var specs by remember { mutableStateOf<DeviceSpecs?>(null) }
    var battery by remember { mutableStateOf<BatterySpecs?>(null) }
    var networkStatus by remember { mutableStateOf(deviceRepo.getNetworkStatus()) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    val currentProfileName by prefs.protectionProfile.collectAsState(initial = "BALANCED")

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val s = deviceRepo.getDeviceSpecs()
            val b = deviceRepo.getBatterySpecs()
            val n = deviceRepo.getNetworkStatus()
            withContext(Dispatchers.Main) {
                specs = s
                battery = b
                networkStatus = n
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
                    text = "مركز إدارة ومواصفات الهاتف",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "بيانات تشخيصية حقيقية، إدارة الذاكرة، وفحص الصلاحيات والأمان",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Protection Profiles Selection
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("protection_profiles_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "مستوى الحماية والانضباط الذاتي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProtectionProfile.values().forEach { profile ->
                        val isSelected = currentProfileName == profile.name
                        Card(
                            onClick = {
                                coroutineScope.launch {
                                    prefs.setProtectionProfile(profile.name)
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) EmeraldHealth.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        coroutineScope.launch {
                                            prefs.setProtectionProfile(profile.name)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = profile.titleAr,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) EmeraldHealth else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = profile.descriptionAr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Hardware Specifications Card
        specs?.let { d ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = EmeraldHealth)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "مواصفات العتاد والنظام الحقيقية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        DetailRow("الشركة والموديل", "${d.manufacturer} ${d.model}")
                        DetailRow("إصدار أندرويد", "${d.androidVersion} (API ${d.sdkInt})")
                        DetailRow("تحديث الأمان", d.securityPatch)

                        val totalRamGb = String.format("%.1f", d.totalRamBytes / (1024.0 * 1024 * 1024))
                        val availRamGb = String.format("%.1f", d.availableRamBytes / (1024.0 * 1024 * 1024))
                        DetailRow("الذاكرة العشوائية (RAM)", "$availRamGb جيجابايت متاحة من أصل $totalRamGb")

                        val freeGb = d.freeInternalStorageBytes / (1024 * 1024 * 1024)
                        val totalGb = d.totalInternalStorageBytes / (1024 * 1024 * 1024)
                        DetailRow("التخزين الداخلي", "$freeGb جيجابايت حرة من أصل $totalGb")
                        DetailRow("حالة الشبكة", networkStatus)
                    }
                }
            }
        }

        // Battery Diagnostics Card
        battery?.let { b ->
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BatteryChargingFull, contentDescription = null, tint = GoldAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "صحة وحالة البطارية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))

                        DetailRow("نسبة الشحن الحالية", "${b.percentage}%")
                        DetailRow("وضع الشحن", if (b.isCharging) "متصل بالشاحن ⚡" else "يعمل على البطارية")
                        DetailRow("درجة حرارة البطارية", "${b.temperatureCelsius} °مئوية")
                        DetailRow("الجهد الكهربائي", "${b.voltageMv} mV")
                        DetailRow("صحة البطارية", b.healthState)
                    }
                }
            }
        }

        // Storage & Cache Management Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "إدارة التخزين والتنظيف الآمن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "التطبيق يحترم خصوصيتك بالكامل ولا يقوم بحذف ملفاتك الشخصية دون إذن. يمكنك تنظيف الملفات المؤقتة لتطبيق LifeGuard أو فتح إعدادات النظام الرسمية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val cleared = deviceRepo.clearAppCache()
                                val kb = cleared / 1024
                                cacheClearedMessage = "تم تنظيف $kb كيلوبايت من الملفات المؤقتة للتطبيق بأمان."
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_cache_button")
                        ) {
                            Text("تنظيف كاش التطبيق")
                        }

                        OutlinedButton(
                            onClick = { deviceRepo.openStorageSettings() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إعدادات تخزين الهاتف")
                        }
                    }

                    cacheClearedMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = msg, style = MaterialTheme.typography.bodySmall, color = EmeraldHealth)
                    }
                }
            }
        }

        // Permissions and Privacy Center Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldHealth)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مركز الصلاحيات والخصوصية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "شفافية مطلقة: كل صلاحية تستخدم حصرياً لغرضها المعلن ولا تُشارك البيانات مع أي طرف ثالث.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PermissionItem(
                        title = "بيانات الاستخدام (Usage Stats)",
                        desc = "لقياس وقت الشاشة وتطبيق الحدود اليومية",
                        onClick = {
                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )

                    PermissionItem(
                        title = "خدمة الوصول (Accessibility)",
                        desc = "لاكتشاف تطبيقات التواصل في الواجهة وإظهار وقفة التنفس",
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )

                    PermissionItem(
                        title = "درع الويب وفلترة DNS",
                        desc = "حجب المواقع الإباحية والضارة محلياً عبر نفق VpnService",
                        onClick = {
                            val intent = Intent(Settings.ACTION_VPN_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    )

                    PermissionItem(
                        title = "حماية Google Play Protect",
                        desc = "فحص التطبيقات والتحقق من الأمان عبر المتجر الرسمي",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://play.google.com/store/apps")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PermissionItem(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        OutlinedButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("فتح الإعدادات", fontSize = 12.sp)
        }
    }
}
