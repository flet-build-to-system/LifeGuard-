package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyBackground
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay

@Composable
fun BreathingExerciseOverlay(
    targetAppName: String,
    delaySeconds: Int = 10,
    isLimitExceeded: Boolean = false,
    usedMinutes: Int = 0,
    limitMinutes: Int = 0,
    onDelayCompleted: () -> Unit,
    onCancel: () -> Unit,
    onEmergencyExtend: () -> Unit = {}
) {
    BackHandler {
        onCancel()
    }

    var countdown by remember { mutableIntStateOf(delaySeconds) }
    var phaseText by remember { mutableStateOf("شهيق عميق...") }

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    LaunchedEffect(isLimitExceeded) {
        if (!isLimitExceeded) {
            while (countdown > 0) {
                delay(1000L)
                countdown--
                phaseText = when ((delaySeconds - countdown) % 8) {
                    in 0..3 -> "شهيق عميق وهادئ..."
                    in 4..5 -> "احبس النفس بلطف..."
                    else -> "زفير بطيء وتفريغ التوتر..."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkNavyBackground.copy(alpha = 0.98f))
            .padding(24.dp)
            .testTag("breathing_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLimitExceeded) {
                // Hard Limit Exceeded View
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "تم استنفاد الحد اليومي لـ $targetAppName",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "استهلكت اليوم: $usedMinutes دقيقة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "الحد المسموح الذي حددته: $limitMinutes دقيقة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = GoldAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "لقد قمت بنفسك بوضع هذا القيد لحماية وقتك وأهدافك. تذكر أن مقاومة الرغبة الآن تصنع فارقاً حقيقياً في يومك.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("exit_to_home_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "الالتزام بالهدف والعودة للرئيسية",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onEmergencyExtend,
                    modifier = Modifier.testTag("emergency_extend_button")
                ) {
                    Text(
                        text = "استثناء طارئ ضروري (+5 دقائق إضافية)",
                        color = GoldAccent,
                        fontSize = 13.sp
                    )
                }

            } else {
                // Mindful Breathing Pause View
                Icon(
                    imageVector = Icons.Default.SelfImprovement,
                    contentDescription = null,
                    tint = EmeraldHealth,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "وقفة واعية قبل فتح $targetAppName",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "هل استخدام $targetAppName الآن قرار واعٍ يخدم أولوياتك وأهدافك، أم استجابة عفوية لملل أو تشتت؟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Animated Breathing Orb
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(170.dp)
                ) {
                    Canvas(modifier = Modifier.size(150.dp * scale)) {
                        drawCircle(color = EmeraldHealth.copy(alpha = 0.25f))
                    }
                    Canvas(modifier = Modifier.size(100.dp * scale)) {
                        drawCircle(color = EmeraldLight.copy(alpha = 0.85f))
                    }
                    Text(
                        text = if (countdown > 0) "$countdown" else "✓",
                        style = MaterialTheme.typography.headlineLarge,
                        color = DarkNavyBackground,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.titleMedium,
                    color = EmeraldHealth,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("mindful_cancel_button")
                ) {
                    Text(
                        text = "تراجع، سأستثمر وقتي في أمر أكثر نفعاً",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (countdown == 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onDelayCompleted,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("proceed_button")
                    ) {
                        Text(
                            text = "متابعة إلى التطبيق بوعي",
                            color = EmeraldHealth,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
