package com.mms.meetingroom

import android.app.Application
import android.util.Log

class NTPSyncApplication : Application() {
    
    companion object {
        private const val TAG = "NTPSyncApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NTP同步应用启动")
        
        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "未捕获异常: ${throwable.message}", throwable)
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "NTP同步应用终止")
    }
} 