package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDomainDao {
    @Query("SELECT * FROM blocked_domains ORDER BY dateAdded DESC")
    fun getAllBlockedDomains(): Flow<List<BlockedDomainEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_domains WHERE domain = :domain)")
    suspend fun isDomainBlocked(domain: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDomains(domains: List<BlockedDomainEntity>)

    @Query("DELETE FROM blocked_domains WHERE domain = :domain")
    suspend fun deleteDomain(domain: String)

    @Query("SELECT COUNT(*) FROM blocked_domains")
    fun getDomainCount(): Flow<Int>

    @Query("UPDATE blocked_domains SET blockedHits = blockedHits + 1 WHERE domain = :domain")
    suspend fun incrementBlockedHits(domain: String)

    @Query("SELECT SUM(blockedHits) FROM blocked_domains")
    fun getTotalBlockedHits(): Flow<Int?>
}

@Dao
interface AppRestrictionDao {
    @Query("SELECT * FROM app_restrictions ORDER BY appName ASC")
    fun getAllRestrictions(): Flow<List<AppRestrictionEntity>>

    @Query("SELECT * FROM app_restrictions WHERE packageName = :packageName LIMIT 1")
    suspend fun getRestriction(packageName: String): AppRestrictionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRestriction(restriction: AppRestrictionEntity)

    @Delete
    suspend fun deleteRestriction(restriction: AppRestrictionEntity)

    @Query("UPDATE app_restrictions SET isBlocked = :isBlocked WHERE packageName = :packageName")
    suspend fun updateBlockedStatus(packageName: String, isBlocked: Boolean)
}

@Dao
interface UrgeLogDao {
    @Query("SELECT * FROM urge_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<UrgeLogEntity>>

    @Insert
    suspend fun insertUrgeLog(log: UrgeLogEntity)

    @Query("SELECT COUNT(*) FROM urge_logs WHERE wasResisted = 1")
    fun getResistedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM urge_logs")
    fun getTotalCount(): Flow<Int>
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<FocusSessionEntity>>

    @Insert
    suspend fun insertSession(session: FocusSessionEntity)

    @Query("SELECT SUM(durationMinutes) FROM focus_sessions WHERE completed = 1")
    fun getTotalCompletedMinutes(): Flow<Int?>
}
