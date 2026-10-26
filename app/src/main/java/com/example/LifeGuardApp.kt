package com.example

import android.app.Application
import com.example.data.local.LifeGuardDatabase

class LifeGuardApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize database and trigger pre-seed
        LifeGuardDatabase.getInstance(this)
    }
}
