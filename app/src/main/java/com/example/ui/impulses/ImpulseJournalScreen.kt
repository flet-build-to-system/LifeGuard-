package com.example.ui.impulses

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
import com.example.data.local.entity.UrgeLogEntity
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ImpulseJournalScreen(
    onTriggerBreathingDemo: (String) -> Unit
) {
    val context = LocalContext.current
    val db = remember { LifeGuardDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    val logs by db.urgeLogDao().getAllLogs().collectAsState(initial = emptyList())
    val resistedCount by db.urgeLogDao().getResistedCount().collectAsState(initial = 0)
    val totalCount by db.urgeLogDao().getTotalCount().collectAsState(initial = 0)

    var selectedBehavior by remember { mutableStateOf("تصفح عشوائي / مقاطع ريلز") }
    var selectedContext by remember { mutableStateOf("ملل وفراغ") }
    var notesInput by remember { mutableStateOf("") }
    var showSuccessMessage by remember { mutableStateOf(false) }

    val behaviors = listOf(
        "تصفح عشوائي / مقاطع ريلز",
        "محتوى إباحي أو ضار",
        "شراء متسرع وغير مخطط",
        "سهر وتفقد قهري للهاتف",
        "ألعاب لساعات متواصلة"
    )

    val contexts = listOf(
        "ملل وفراغ",
        "توتر وضغط نفسي",
        "تأجيل ومماطلة مهام",
        "سهر وعزلة ليلية",
        "إرهاق بعد العمل"
    )

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
                    text = "سجل إدارة الدوافع والعادات",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "تعلّم التوقف وملاحظة المحفزات النفسية قبل الاستسلام للسلوك الاندفاعي",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Implementation Intention Card (CBT Cognitive Reappraisal)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Psychology, contentDescription = null, tint = GoldAccent)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "خطة الاستجابة البديلة (إذا... سأقوم بـ...)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "«إذا شعرت برغبة قهرية لفتح التطبيقات المشتتة أو المواقع الضارة أثناء الدراسة أو قبل النوم، فسأتوقف لثلاثة أنفاس عميقة، وأشرب كوب ماء بارد، وأنتظر 5 دقائق أولاً.»",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Urge Stats Row
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
                        Text(text = "الدوافع المقاومة", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "$resistedCount نجاح",
                            style = MaterialTheme.typography.titleLarge,
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
                        Text(text = "إجمالي التسجيلات", style = MaterialTheme.typography.labelSmall)
                        val rate = if (totalCount > 0) (resistedCount * 100 / totalCount) else 100
                        Text(
                            text = "$rate% انتصار",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                    }
                }
            }
        }

        // Urge Logger Form Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("urge_logger_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "تسجيل دافع ملحّ الآن وتفريغ التوتر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("ما هو السلوك أو الدافع الذي تشعر به الآن؟", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        behaviors.forEach { behavior ->
                            FilterChip(
                                selected = selectedBehavior == behavior,
                                onClick = { selectedBehavior = behavior },
                                label = { Text(behavior) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("ما هو الشعور أو السياق المصاحب؟", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        contexts.forEach { ctx ->
                            FilterChip(
                                selected = selectedContext == ctx,
                                onClick = { selectedContext = ctx },
                                label = { Text(ctx) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        placeholder = { Text("ملاحظة سريعة أو شعور بديل (اختياري)...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    db.urgeLogDao().insertUrgeLog(
                                        UrgeLogEntity(
                                            triggerBehavior = selectedBehavior,
                                            triggerContext = selectedContext,
                                            delayCompletedSeconds = 30,
                                            wasResisted = true,
                                            notes = notesInput
                                        )
                                    )
                                    notesInput = ""
                                    showSuccessMessage = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldHealth),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("record_resisted_button")
                        ) {
                            Text("قَاومتُ الدافع ✓", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    db.urgeLogDao().insertUrgeLog(
                                        UrgeLogEntity(
                                            triggerBehavior = selectedBehavior,
                                            triggerContext = selectedContext,
                                            delayCompletedSeconds = 0,
                                            wasResisted = false,
                                            notes = notesInput.ifEmpty { "حدثت عثرة، سأتعلم منها وأستمر" }
                                        )
                                    )
                                    notesInput = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("record_relapse_button")
                        ) {
                            Text("حدث استسلام", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (showSuccessMessage) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "أحسنت! كل دافع تقاومه يضعف الرغبة القهرية التالية ويعزز قوتك الداخلية.",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldHealth,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // History of Logs
        item {
            Text(
                text = "سجل الملاحظات والمواقف (${logs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(logs, key = { it.id }) { log ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.triggerBehavior,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "السياق: ${log.triggerContext}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        if (log.notes.isNotEmpty()) {
                            Text(
                                text = "ملاحظة: ${log.notes}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    if (log.wasResisted) {
                        Badge(containerColor = EmeraldHealth.copy(alpha = 0.2f)) {
                            Text("تمت المقاومة ✓", color = EmeraldHealth, modifier = Modifier.padding(6.dp))
                        }
                    } else {
                        Badge(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.2f)) {
                            Text("استسلام", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        }
    }
}
