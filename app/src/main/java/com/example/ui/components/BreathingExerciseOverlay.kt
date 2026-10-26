package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import kotlinx.coroutines.delay

@Composable
fun BreathingExerciseOverlay(
    targetAppName: String,
    delaySeconds: Int = 10,
    onDelayCompleted: () -> Unit,
    onCancel: () -> Unit
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

    LaunchedEffect(Unit) {
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
            Icon(
                imageVector = Icons.Default.SelfImprovement,
                contentDescription = null,
                tint = EmeraldHealth,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "وقفة واعية قبل الدخول",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "هل استخدام $targetAppName الآن قرار واعٍ يخدم أولوياتك وأهدافك الحقيقية، أم استجابة لملل أو توتر؟",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Animated Breathing Orb
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Canvas(modifier = Modifier.size(160.dp * scale)) {
                    drawCircle(color = EmeraldHealth.copy(alpha = 0.25f))
                }
                Canvas(modifier = Modifier.size(110.dp * scale)) {
                    drawCircle(color = EmeraldLight.copy(alpha = 0.85f))
                }
                Text(
                    text = if (countdown > 0) "$countdown" else "✓",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DarkNavyBackground,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = phaseText,
                style = MaterialTheme.typography.titleMedium,
                color = EmeraldHealth,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(40.dp))

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
