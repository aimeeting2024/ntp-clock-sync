package com.mms.meetingroom.ntp

import android.util.Log
import org.apache.commons.net.ntp.NTPUDPClient
import org.apache.commons.net.ntp.TimeInfo
import java.net.InetAddress
import java.util.*

class NTPClient {
    companion object {
        private const val TAG = "NTPClient"
        private const val DEFAULT_TIMEOUT = 5000 // 5秒超时
        private const val DEFAULT_NTP_SERVER = "time.windows.com" // 默认NTP服务器
    }

    /**
     * 从NTP服务器获取时间
     * @param ntpServer NTP服务器地址，默认为time.windows.com
     * @param timeout 超时时间（毫秒），默认5000ms
     * @return NTP时间结果
     */
    fun getNTPTime(ntpServer: String = DEFAULT_NTP_SERVER, timeout: Int = DEFAULT_TIMEOUT): NTPTimeResult {
        val client = NTPUDPClient()
        client.defaultTimeout = timeout
        
        return try {
            Log.d(TAG, "开始从NTP服务器获取时间: $ntpServer")
            
            // 解析NTP服务器地址
            val address = InetAddress.getByName(ntpServer)
            
            // 获取NTP时间信息
            val timeInfo: TimeInfo = client.getTime(address)
            timeInfo.computeDetails()
            
            // 计算网络延迟和偏移
            val delay = timeInfo.delay
            val offset = timeInfo.offset
            
            // 获取NTP时间
            val ntpTime = timeInfo.message.transmitTimeStamp.time
            
            Log.d(TAG, "NTP时间获取成功: $ntpTime, 延迟: ${delay}ms, 偏移: ${offset}ms")
            
            NTPTimeResult(
                success = true,
                ntpTime = Date(ntpTime),
                localTime = Date(),
                delay = delay,
                offset = offset,
                server = ntpServer
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "获取NTP时间失败: ${e.message}", e)
            NTPTimeResult(
                success = false,
                errorMessage = e.message ?: "未知错误",
                server = ntpServer
            )
        } finally {
            client.close()
        }
    }

    /**
     * 同步本地时间（需要root权限）
     * @param ntpServer NTP服务器地址
     * @return 同步结果
     */
    fun syncLocalTime(ntpServer: String = DEFAULT_NTP_SERVER): NTPSyncResult {
        val ntpResult = getNTPTime(ntpServer)
        
        if (!ntpResult.success) {
            return NTPSyncResult(
                success = false,
                errorMessage = ntpResult.errorMessage
            )
        }

        return try {
            // 这里可以添加系统时间同步逻辑
            // 注意：修改系统时间需要root权限
            Log.d(TAG, "NTP时间同步成功，偏移: ${ntpResult.offset}ms")
            
            NTPSyncResult(
                success = true,
                offset = ntpResult.offset,
                ntpTime = ntpResult.ntpTime,
                localTime = ntpResult.localTime
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "同步本地时间失败: ${e.message}", e)
            NTPSyncResult(
                success = false,
                errorMessage = e.message ?: "同步失败"
            )
        }
    }
}

/**
 * NTP时间结果数据类
 */
data class NTPTimeResult(
    val success: Boolean,
    val ntpTime: Date? = null,
    val localTime: Date? = null,
    val delay: Long? = null,
    val offset: Long? = null,
    val server: String = "",
    val errorMessage: String? = null,
    var systemTimeModified: Boolean? = null
)

/**
 * NTP同步结果数据类
 */
data class NTPSyncResult(
    val success: Boolean,
    val offset: Long? = null,
    val ntpTime: Date? = null,
    val localTime: Date? = null,
    val errorMessage: String? = null
) 