package com.mms.meetingroom

import android.app.Application
import android.util.Log

class NTPSyncApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("NTPSyncApplication", "NTP同步应用启动")
    }
} 