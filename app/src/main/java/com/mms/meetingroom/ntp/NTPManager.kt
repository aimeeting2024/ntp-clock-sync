package com.mms.meetingroom.ntp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

class NTPManager(private val context: Context) {
    companion object {
        private const val TAG = "NTPManager"
        private const val DEFAULT_SYNC_INTERVAL = 30L // 默认30分钟同步一次
        private const val DEFAULT_NTP_SERVER = "time.windows.com"
    }

    private val ntpClient = NTPClient()
    private val systemTimeManager = SystemTimeManager(context)
    private var syncJob: Job? = null
    private var isRunning = false
    
    // 当前NTP状态
    private var lastSyncTime: Date? = null
    private var lastOffset: Long? = null
    private var lastDelay: Long? = null
    private var syncError: String? = null

    /**
     * 开始定期NTP同步
     * @param intervalMinutes 同步间隔（分钟）
     * @param ntpServer NTP服务器地址
     */
    fun startPeriodicSync(intervalMinutes: Long = DEFAULT_SYNC_INTERVAL, ntpServer: String = DEFAULT_NTP_SERVER) {
        if (isRunning) {
            Log.w(TAG, "NTP同步已在运行中")
            return
        }

        Log.d(TAG, "开始NTP定期同步，间隔: ${intervalMinutes}分钟，服务器: $ntpServer")
        
        syncJob = CoroutineScope(Dispatchers.IO).launch {
            isRunning = true
            
            try {
                while (isActive) {
                    // 执行NTP同步
                    val result = ntpClient.getNTPTime(ntpServer)
                    
                    if (result.success) {
                        lastSyncTime = Date()
                        lastOffset = result.offset
                        lastDelay = result.delay
                        syncError = null
                        
                        Log.d(TAG, "NTP同步成功: 偏移=${result.offset}ms, 延迟=${result.delay}ms")
                        
                        // 自动修改系统时间
                        if (result.ntpTime != null) {
                            // 修改系统时间
                            val systemTimeUpdated = systemTimeManager.setSystemTime(result.ntpTime!!)
                            if (systemTimeUpdated) {
                                Log.d(TAG, "系统时间修改成功")
                                result.systemTimeModified = true
                            } else {
                                Log.w(TAG, "系统时间修改失败，需要root权限")
                                result.systemTimeModified = false
                            }
                        }
                    } else {
                        syncError = result.errorMessage
                        Log.e(TAG, "NTP同步失败: ${result.errorMessage}")
                    }
                    
                    // 等待下次同步
                    delay(TimeUnit.MINUTES.toMillis(intervalMinutes))
                }
            } catch (e: CancellationException) {
                Log.d(TAG, "NTP同步被取消")
            } catch (e: Exception) {
                Log.e(TAG, "NTP同步异常: ${e.message}", e)
                syncError = e.message
            } finally {
                isRunning = false
            }
        }
    }

    /**
     * 停止NTP同步
     */
    fun stopPeriodicSync() {
        Log.d(TAG, "停止NTP定期同步")
        syncJob?.cancel()
        syncJob = null
        isRunning = false
    }

    /**
     * 立即执行一次NTP同步
     * @param ntpServer NTP服务器地址
     * @param updateSystemTime 是否修改系统时间
     * @return 同步结果
     */
    suspend fun syncNow(ntpServer: String = DEFAULT_NTP_SERVER, updateSystemTime: Boolean = false): NTPTimeResult {
        return withContext(Dispatchers.IO) {
            Log.d(TAG, "执行立即NTP同步")
            val result = ntpClient.getNTPTime(ntpServer)
            
            // 如果同步成功，立即更新状态
            if (result.success) {
                lastSyncTime = Date()
                lastOffset = result.offset
                lastDelay = result.delay
                syncError = null
                Log.d(TAG, "手动同步成功，状态已更新")
                
                // 如果需要修改系统时间
                if (updateSystemTime && result.ntpTime != null) {
                    // 在后台线程中执行系统时间修改
                    withContext(Dispatchers.IO) {
                        // 修改系统时间
                        val systemTimeUpdated = systemTimeManager.setSystemTime(result.ntpTime!!)
                        if (systemTimeUpdated) {
                            Log.d(TAG, "系统时间修改成功")
                            // 更新结果中的系统时间修改状态
                            result.systemTimeModified = true
                        } else {
                            Log.d(TAG, "系统时间修改失败，需要root权限")
                            result.systemTimeModified = false
                        }
                    }
                }
            } else {
                syncError = result.errorMessage
                Log.e(TAG, "手动同步失败: ${result.errorMessage}")
            }
            
            result
        }
    }

    /**
     * 获取当前NTP状态
     */
    fun getNTPStatus(): NTPStatus {
        return NTPStatus(
            isRunning = isRunning,
            lastSyncTime = lastSyncTime,
            lastOffset = lastOffset,
            lastDelay = lastDelay,
            syncError = syncError
        )
    }

    /**
     * 获取校正后的当前时间
     * @return 校正后的时间，如果没有同步过则返回本地时间
     */
    fun getCorrectedTime(): Date {
        return if (lastOffset != null) {
            val currentTime = System.currentTimeMillis()
            Date(currentTime + lastOffset!!)
        } else {
            Date()
        }
    }

    /**
     * 检查是否需要同步（如果距离上次同步超过1小时）
     */
    fun needsSync(): Boolean {
        return lastSyncTime == null || 
               (System.currentTimeMillis() - lastSyncTime!!.time) > TimeUnit.HOURS.toMillis(1)
    }
    
    /**
     * 获取SystemTimeManager实例
     */
    fun getSystemTimeManager(): SystemTimeManager {
        return systemTimeManager
    }
}

/**
 * NTP状态数据类
 */
data class NTPStatus(
    val isRunning: Boolean,
    val lastSyncTime: Date?,
    val lastOffset: Long?,
    val lastDelay: Long?,
    val syncError: String?
) 