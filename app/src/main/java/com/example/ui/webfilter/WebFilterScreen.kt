package com.example.ui.webfilter

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.data.local.entity.BlockedDomainEntity
import com.example.data.preferences.LifeGuardPreferences
import com.example.service.vpn.LifeGuardDnsVpnService
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch

@Composable
fun WebFilterScreen() {
    val context = LocalContext.current
    val db = remember { LifeGuardDatabase.getInstance(context) }
    val prefs = remember { LifeGuardPreferences(context) }
    val coroutineScope = rememberCoroutineScope()

    val isShieldActive by prefs.isWebShieldEnabled.collectAsState(initial = false)
    val domains by db.blockedDomainDao().getAllBlockedDomains().collectAsState(initial = emptyList())
    val domainCount by db.blockedDomainDao().getDomainCount().collectAsState(initial = 0)
    val totalBlockedHits by db.blockedDomainDao().getTotalBlockedHits().collectAsState(initial = 0)

    var customDomainInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val startIntent = Intent(context, LifeGuardDnsVpnService::class.java).apply {
                action = LifeGuardDnsVpnService.ACTION_START
            }
            context.startForegroundService(startIntent)
            coroutineScope.launch { prefs.setWebShieldEnabled(true) }
        }
    }

    fun toggleShield(targetActive: Boolean) {
        if (!targetActive) {
            val stopIntent = Intent(context, LifeGuardDnsVpnService::class.java).apply {
                action = LifeGuardDnsVpnService.ACTION_STOP
            }
            context.startService(stopIntent)
            coroutineScope.launch { prefs.setWebShieldEnabled(false) }
        } else {
            val prepareIntent = VpnService.prepare(context)
            if (prepareIntent != null) {
                vpnPrepareLauncher.launch(prepareIntent)
            } else {
                val startIntent = Intent(context, LifeGuardDnsVpnService::class.java).apply {
                    action = LifeGuardDnsVpnService.ACTION_START
                }
                context.startForegroundService(startIntent)
                coroutineScope.launch { prefs.setWebShieldEnabled(true) }
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
                    text = "درع الويب ومكافحة الإباحية",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "فلترة DNS محلية للمواقع الإباحية والضارة بنسبة 100% دون خوادم وسيطة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Master Shield Toggle Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isShieldActive) EmeraldHealth.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("web_shield_status_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isShieldActive) EmeraldHealth else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isShieldActive) "درع الويب نشط ومفعل" else "درع الويب متوقف",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isShieldActive)
                                "يتم حجب المواقع فورياً عبر استجابة NXDOMAIN محلية وخادم أمان عائلي مشفر."
                            else
                                "اضغط لتفعيل الفلترة المحلية وحماية متصفحك من المواقع الإباحية والضارة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }

                    Switch(
                        checked = isShieldActive,
                        onCheckedChange = { toggleShield(it) },
                        modifier = Modifier.testTag("web_shield_toggle")
                    )
                }
            }
        }

        // Stats Banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "النطاقات المحظورة", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$domainCount نطاق",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldHealth
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "المحاولات المحجوبة", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${totalBlockedHits ?: 0} محاولة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }

        // Privacy Guarantee Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldHealth)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ضمان الخصوصية وعدم جمع البيانات (Zero-Log)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "لا نقوم بقراءة محتوى HTTPS، ولا تفقد الرسائل، ولا إرسال سجل تصفحك لأي خادم إطلاقاً. تتم مقارنة أسماء النطاقات مباشرة على جهازك داخل نفق DNS محلي.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        lineHeight = 19.sp
                    )
                }
            }
        }

        // Add Custom Domain Input
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "إضافة نطاق مخصص للحظر",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customDomainInput,
                            onValueChange = { customDomainInput = it },
                            placeholder = { Text("مثال: badsite.com") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("custom_domain_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val clean = customDomainInput.trim().lowercase()
                                    .removePrefix("https://").removePrefix("http://").removePrefix("www.")
                                    .substringBefore("/").substringBefore(":")
                                if (clean.isNotEmpty()) {
                                    coroutineScope.launch {
                                        db.blockedDomainDao().insertDomains(
                                            listOf(
                                                BlockedDomainEntity(
                                                    domain = clean,
                                                    category = "CUSTOM",
                                                    isCustom = true
                                                )
                                            )
                                        )
                                        customDomainInput = ""
                                    }
                                }
                            },
                            modifier = Modifier.testTag("add_custom_domain_button")
                        ) {
                            Text("حظر")
                        }
                    }
                }
            }
        }

        // Chrome DoH Notice Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تلميح مهم لمتصفح Google Chrome",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "إذا فتح متصفح كروم موقعاً محجوباً، فهذا بسبب تفعيل ميزة التشفير الخاصة بكروم (Secure DNS) التي تتجاوز حماية النظام. للتأكد من الحظر الكامل:\nافتح كروم -> الإعدادات -> الخصوصية والأمان -> «استخدام نظام أسماء نطاقات آمن» -> اضبطه على «مع موفر الخدمة الحالي» أو عطّله ليمر عبر درع الحماية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Blocked Domains List Header & Search
        item {
            Column {
                Text(
                    text = "قائمة النطاقات المحمية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث في النطاقات المحجوبة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        val filteredDomains = domains.filter {
            it.domain.contains(searchQuery.trim().lowercase())
        }

        items(filteredDomains, key = { it.domain }) { domainItem ->
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = domainItem.domain, fontWeight = FontWeight.SemiBold)
                        val catLabel = if (domainItem.isCustom) "مخصص من المستخدم" else "محتوى إباحي / ضار"
                        Text(
                            text = "$catLabel • محجوب ${domainItem.blockedHits} مرة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    if (domainItem.isCustom) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    db.blockedDomainDao().deleteDomain(domainItem.domain)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    } else {
                        Badge(containerColor = EmeraldHealth.copy(alpha = 0.2f)) {
                            Text("محمي", color = EmeraldHealth, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}
