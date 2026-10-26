package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkNavyCard
import com.example.ui.theme.EmeraldHealth
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAccent

@Composable
fun WellbeingScoreCircularGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val clampedScore = score.coerceIn(0, 100)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(150.dp)
            .testTag("wellbeing_score_gauge")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

            // Background arc track (270 degrees)
            drawArc(
                color = DarkNavyCard,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            val sweep = (clampedScore / 100f) * 270f
            val arcColor = when {
                clampedScore >= 75 -> EmeraldHealth
                clampedScore >= 50 -> GoldAccent
                else -> Color(0xFFEF4444)
            }
            drawArc(
                color = arcColor,
                startAngle = 135f,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$clampedScore%",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "مؤشر العافية",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun WeeklyScreenTimeBarChart(
    dailyMinutes: List<Int>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = (dailyMinutes.maxOrNull() ?: 1).coerceAtLeast(60)
    val days = listOf("أحد", "إثن", "ثلا", "أرب", "خمي", "جمع", "سبت")

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dailyMinutes.take(7).forEachIndexed { index, minutes ->
                val fraction = (minutes.toFloat() / maxMinutes).coerceIn(0.05f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    val hours = minutes / 60
                    val mins = minutes % 60
                    val timeLabel = if (hours > 0) "${hours}س" else "${mins}د"

                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.48f)
                            .height((90 * fraction).dp.coerceAtLeast(6.dp))
                    ) {
                        val barColor = if (minutes > 240) GoldAccent else EmeraldHealth
                        drawRoundRect(
                            color = barColor,
                            size = size
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = days.getOrElse(index) { "" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun FocusProgressRing(
    fraction: Float,
    timeText: String,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(190.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 12.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

            // Background circle
            drawArc(
                color = DarkNavyCard,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokeWidth)
            )

            // Active progress
            drawArc(
                color = EmeraldLight,
                startAngle = -90f,
                sweepAngle = fraction * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "جلسة تركيز عميق",
                style = MaterialTheme.typography.labelMedium,
                color = EmeraldHealth
            )
        }
    }
}
