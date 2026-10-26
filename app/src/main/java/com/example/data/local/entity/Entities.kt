package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_domains")
data class BlockedDomainEntity(
    @PrimaryKey val domain: String,
    val category: String = "ADULT",
    val isCustom: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis(),
    val blockedHits: Int = 0
)

@Entity(tableName = "app_restrictions")
data class AppRestrictionEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val dailyLimitMinutes: Int = 30,
    val isBlocked: Boolean = false,
    val requiresBreathingExercise: Boolean = true,
    val delaySeconds: Int = 10,
    val category: String = "SOCIAL"
)

@Entity(tableName = "urge_logs")
data class UrgeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val triggerBehavior: String,
    val triggerContext: String,
    val delayCompletedSeconds: Int = 0,
    val wasResisted: Boolean = true,
    val notes: String = ""
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val durationMinutes: Int = 25,
    val completed: Boolean = true,
    val tag: String = "DEEP_WORK"
)
