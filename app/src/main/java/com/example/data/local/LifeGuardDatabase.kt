package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.*
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BlockedDomainEntity::class,
        AppRestrictionEntity::class,
        UrgeLogEntity::class,
        FocusSessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LifeGuardDatabase : RoomDatabase() {
    abstract fun blockedDomainDao(): BlockedDomainDao
    abstract fun appRestrictionDao(): AppRestrictionDao
    abstract fun urgeLogDao(): UrgeLogDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var INSTANCE: LifeGuardDatabase? = null

        fun getInstance(context: Context): LifeGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeGuardDatabase::class.java,
                    "lifeguard_secure.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val preSeedDomains = listOf(
                                "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com",
                                "onlyfans.com", "chaturbate.com", "redtube.com", "youporn.com",
                                "stripchat.com", "cam4.com", "bongaCams.com", "livejasmin.com",
                                "tube8.com", "spankbang.com", "daftsex.com", "beeg.com"
                            )
                            preSeedDomains.forEach { domain ->
                                db.execSQL("INSERT OR REPLACE INTO blocked_domains (domain, category, isCustom, dateAdded, blockedHits) VALUES ('$domain', 'ADULT', 0, ${System.currentTimeMillis()}, 0)")
                            }

                            val defaultSocialApps = listOf(
                                Triple("com.zhiliaoapp.musically", "TikTok", 30),
                                Triple("com.instagram.android", "Instagram", 30),
                                Triple("com.facebook.katana", "Facebook", 45),
                                Triple("com.snapchat.android", "Snapchat", 30),
                                Triple("com.twitter.android", "X (Twitter)", 30),
                                Triple("com.google.android.youtube", "YouTube", 60),
                                Triple("com.reddit.frontpage", "Reddit", 30)
                            )
                            defaultSocialApps.forEach { (pkg, name, limit) ->
                                db.execSQL("INSERT OR REPLACE INTO app_restrictions (packageName, appName, dailyLimitMinutes, isBlocked, requiresBreathingExercise, delaySeconds, category) VALUES ('$pkg', '$name', $limit, 0, 1, 10, 'SOCIAL')")
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
