package com.mms.meetingroom

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mms.meetingroom.ntp.NTPManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class NTPBackgroundService : Service() {
    companion object {
        private const val TAG = "NTPBackgroundService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ntp_sync_channel"
        private const val CHANNEL_NAME = "NTP同步服务"
        
        // 服务控制
        fun startService(context: Context, ntpServer: String = "time.windows.com", syncInterval: Long = 30) {
            val intent = Intent(context, NTPBackgroundService::class.java).apply {
                putExtra("ntp_server", ntpServer)
                putExtra("sync_interval", syncInterval)
            }
            context.startForegroundService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, NTPBackgroundService::class.java)
            context.stopService(intent)
        }
    }
    
    private lateinit var ntpManager: NTPManager
    private var syncJob: Job? = null
    private var ntpServer: String = "time.windows.com"
    private var syncInterval: Long = 30
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    
    // Binder for activity communication
    inner class NTPBinder : Binder() {
        fun getService(): NTPBackgroundService = this@NTPBackgroundService
    }
    
    private val binder = NTPBinder()
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "NTP后台服务创建")
        
        ntpManager = NTPManager(this)
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "NTP后台服务启动")
        
        // 获取配置参数
        intent?.let {
            ntpServer = it.getStringExtra("ntp_server") ?: "time.windows.com"
            syncInterval = it.getLongExtra("sync_interval", 30)
        }
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification("NTP同步服务运行中"))
        
        // 开始NTP同步
        startNTPSync()
        
        return START_STICKY // 服务被杀死后自动重启
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "NTP后台服务销毁")
        
        // 停止同步
        stopNTPSync()
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "NTP时间同步服务通知"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(content: String): Notification {
        val ntpStatus = ntpManager.getNTPStatus()
        val statusText = if (ntpStatus.isRunning) "同步中" else "已停止"
        val offsetText = ntpStatus.lastOffset?.let { "偏移: ${if (it >= 0) "+" else ""}${it}ms" } ?: "未同步"
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NTP时间同步")
            .setContentText("$content - $statusText")
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$content\n$statusText\n$offsetText\n服务器: $ntpServer"))
            .build()
    }
    
    /**
     * 启动NTP同步
     */
    private fun startNTPSync() {
        Log.d(TAG, "启动NTP后台同步，服务器: $ntpServer，间隔: ${syncInterval}分钟")
        
        // 立即执行一次同步
        serviceScope.launch {
            try {
                val result = ntpManager.syncNow(ntpServer)
                if (result.success) {
                    Log.d(TAG, "NTP初始同步成功: 偏移=${result.offset}ms")
                    updateNotification("同步成功")
                } else {
                    Log.e(TAG, "NTP初始同步失败: ${result.errorMessage}")
                    updateNotification("同步失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "NTP初始同步异常: ${e.message}", e)
                updateNotification("同步异常")
            }
        }
        
        // 开始定期同步
        ntpManager.startPeriodicSync(syncInterval, ntpServer)
        
        // 监控同步状态并更新通知
        startStatusMonitoring()
    }
    
    /**
     * 停止NTP同步
     */
    private fun stopNTPSync() {
        Log.d(TAG, "停止NTP后台同步")
        ntpManager.stopPeriodicSync()
        syncJob?.cancel()
    }
    
    /**
     * 开始状态监控
     */
    private fun startStatusMonitoring() {
        syncJob = serviceScope.launch {
            while (true) {
                try {
                    val status = ntpManager.getNTPStatus()
                    val statusText = if (status.isRunning) "同步中" else "已停止"
                    val offsetText = status.lastOffset?.let { "偏移: ${if (it >= 0) "+" else ""}${it}ms" } ?: "未同步"
                    
                    Log.d(TAG, "NTP状态: $statusText, $offsetText")
                    updateNotification("$statusText - $offsetText")
                    
                    kotlinx.coroutines.delay(30000) // 30秒更新一次
                } catch (e: Exception) {
                    Log.e(TAG, "状态监控异常: ${e.message}", e)
                    kotlinx.coroutines.delay(60000) // 异常时1分钟后再试
                }
            }
        }
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 获取NTP状态
     */
    fun getNTPStatus() = ntpManager.getNTPStatus()
    
    /**
     * 获取校正后的时间
     */
    fun getCorrectedTime() = ntpManager.getCorrectedTime()
    
    /**
     * 手动同步
     */
    fun syncNow() {
        serviceScope.launch {
            try {
                val result = ntpManager.syncNow(ntpServer)
                if (result.success) {
                    Log.d(TAG, "手动同步成功: 偏移=${result.offset}ms")
                    updateNotification("手动同步成功")
                } else {
                    Log.e(TAG, "手动同步失败: ${result.errorMessage}")
                    updateNotification("手动同步失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "手动同步异常: ${e.message}", e)
                updateNotification("手动同步异常")
            }
        }
    }
} 