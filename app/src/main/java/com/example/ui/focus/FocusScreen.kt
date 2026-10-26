package com.example.ui.focus

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.LifeGuardDatabase
import com.example.data.local.entity.FocusSessionEntity
import com.example.service.focus.FocusTimerService
import com.example.ui.components.FocusProgressRing
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FocusScreen() {
    val context = LocalContext.current
    val db = remember { LifeGuardDatabase.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var selectedDurationMinutes by remember { mutableIntStateOf(25) }
    var selectedTag by remember { mutableStateOf("عمل عميق") }
    var isTimerRunning by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(25 * 60) }
    val totalSeconds = remember(selectedDurationMinutes) { selectedDurationMinutes * 60 }

    val sessions by db.focusSessionDao().getAllSessions().collectAsState(initial = emptyList())
    val totalFocusMinutes by db.focusSessionDao().getTotalCompletedMinutes().collectAsState(initial = 0)

    val tags = listOf("عمل عميق", "دراسة", "روتين عبادة وتأمل", "قراءة وبحث")

    // Local ticker countdown
    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining--
            if (secondsRemaining == 0) {
                isTimerRunning = false
                val intent = Intent(context, FocusTimerService::class.java).apply {
                    action = FocusTimerService.ACTION_STOP
                }
                context.startService(intent)

                coroutineScope.launch {
                    db.focusSessionDao().insertSession(
                        FocusSessionEntity(
                            durationMinutes = selectedDurationMinutes,
                            completed = true,
                            tag = selectedTag
                        )
                    )
                }
            }
        }
    }

    fun startFocusSession() {
        secondsRemaining = selectedDurationMinutes * 60
        isTimerRunning = true
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START
            putExtra(FocusTimerService.EXTRA_DURATION_MINUTES, selectedDurationMinutes)
            putExtra(FocusTimerService.EXTRA_TAG, selectedTag)
        }
        context.startForegroundService(intent)
    }

    fun stopFocusSession() {
        isTimerRunning = false
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_STOP
        }
        context.startService(intent)
        secondsRemaining = selectedDurationMinutes * 60
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "غرفة التركيز والإنتاجية العميقة",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "جلسات بومودورو واستغراق ذهني لحماية تركيزك من المشتتات الرقمية",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
        }

        // Circular Timer Display Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("focus_timer_card")
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    val fraction = if (totalSeconds > 0) {
                        (totalSeconds - secondsRemaining).toFloat() / totalSeconds
                    } else 0f

                    val mins = secondsRemaining / 60
                    val secs = secondsRemaining % 60
                    val timeText = String.format("%02d:%02d", mins, secs)

                    FocusProgressRing(fraction = fraction, timeText = timeText)

                    Spacer(modifier = Modifier.height(20.dp))

                    // Preset Duration Chips
                    if (!isTimerRunning) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            listOf(15, 25, 45, 60).forEach { minsPreset ->
                                FilterChip(
                                    selected = selectedDurationMinutes == minsPreset,
                                    onClick = {
                                        selectedDurationMinutes = minsPreset
                                        secondsRemaining = minsPreset * 60
                                    },
                                    label = { Text("$minsPreset د") }
                                )
                            }
                        }

                        // Tags selection
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            tags.forEach { tag ->
                                FilterChip(
                                    selected = selectedTag == tag,
                                    onClick = { selectedTag = tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }

                    // Start / Stop Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (!isTimerRunning) {
                            Button(
                                onClick = { startFocusSession() },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldHealth),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_focus_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بدء جلسة التركيز الآن", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { stopFocusSession() },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("stop_focus_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إنهاء الجلسة وحفظ التقدم", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // Total Completed Sessions Metric Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                    Column {
                        Text(text = "إجمالي دقائق التركيز المنجزة", style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = "${totalFocusMinutes ?: 0} دقيقة",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldHealth
                        )
                    }
                    Badge(containerColor = GoldAccent.copy(alpha = 0.2f)) {
                        Text("${sessions.size} جلسة", color = GoldAccent, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        }

        // Focus Sessions History
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "سجل الجلسات المكتملة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        items(sessions, key = { it.id }) { session ->
            Card(
                shape = RoundedCornerShape(12.dp),
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
                    Column {
                        Text(text = session.tag, fontWeight = FontWeight.Bold)
                        val formattedDate = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(session.startTime))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "${session.durationMinutes} دقيقة",
                        fontWeight = FontWeight.Bold,
                        color = EmeraldHealth
                    )
                }
            }
        }
    }
}
