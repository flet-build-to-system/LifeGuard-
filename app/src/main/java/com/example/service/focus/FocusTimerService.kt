package com.example.service.focus

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.data.local.LifeGuardDatabase
import com.example.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.*

class FocusTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var totalSeconds = 25 * 60
    private var remainingSeconds = 25 * 60
    private var sessionTag = "DEEP_WORK"

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_DURATION_MINUTES = "EXTRA_DURATION_MINUTES"
        const val EXTRA_TAG = "EXTRA_TAG"
        const val NOTIFICATION_ID = 3001
        const val CHANNEL_ID = "lifeguard_focus_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                sessionTag = intent.getStringExtra(EXTRA_TAG) ?: "DEEP_WORK"
                totalSeconds = minutes * 60
                remainingSeconds = totalSeconds
                createNotificationChannel()
                startForeground(NOTIFICATION_ID, buildNotification(remainingSeconds))
                startTimer()
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_STOP -> stopSession(completed = false)
        }
        return START_NOT_STICKY
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
                updateNotification(remainingSeconds)
            }
            stopSession(completed = true)
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
    }

    private fun stopSession(completed: Boolean) {
        timerJob?.cancel()
        val durationMins = ((totalSeconds - remainingSeconds) / 60).coerceAtLeast(1)
        val startTime = System.currentTimeMillis() - ((totalSeconds - remainingSeconds) * 1000L)
        val tag = sessionTag

        CoroutineScope(Dispatchers.IO).launch {
            LifeGuardDatabase.getInstance(applicationContext).focusSessionDao().insertSession(
                FocusSessionEntity(
                    startTime = startTime,
                    durationMinutes = durationMins,
                    completed = completed,
                    tag = tag
                )
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updateNotification(secondsLeft: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(secondsLeft))
    }

    private fun buildNotification(secondsLeft: Int): Notification {
        val mins = secondsLeft / 60
        val secs = secondsLeft % 60
        val timeFormatted = String.format("%02d:%02d", mins, secs)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("جلسة تركيز: $sessionTag")
            .setContentText("الوقت المتبقي: $timeFormatted")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Session Timer",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
