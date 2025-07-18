package com.mms.meetingroom.ntp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.TimeoutCancellationException

class SystemTimeManager(private val context: Context) {
    companion object {
        private const val TAG = "SystemTimeManager"
    }
    
    // 缓存权限状态，避免重复检查
    private val rootPermissionCache = AtomicReference<Boolean?>(null)
    private val settingsPermissionCache = AtomicReference<Boolean?>(null)
    
    /**
     * 修改系统时间
     * @param newTime 新的系统时间
     * @return 是否成功
     */
    fun setSystemTime(newTime: Date): Boolean {
        return try {
            Log.d(TAG, "尝试修改系统时间: $newTime")
            
            // 方法1: 使用Shell命令（需要root权限）
            val success = setSystemTimeViaShell(newTime)
            if (success) {
                Log.d(TAG, "通过Shell命令修改系统时间成功")
                return true
            }
            
            // 方法2: 使用Settings API（需要系统权限）
            val success2 = setSystemTimeViaSettings(newTime)
            if (success2) {
                Log.d(TAG, "通过Settings API修改系统时间成功")
                return true
            }
            
            Log.w(TAG, "无法修改系统时间，需要root权限或系统权限")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "修改系统时间失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 通过Shell命令修改系统时间（需要root权限）
     */
    private fun setSystemTimeViaShell(newTime: Date): Boolean {
        return try {
            // 尝试多种date命令格式，适配不同设备
            val commands = listOf(
                // 格式1: date "YYYY-MM-DD HH:MM:SS" (适用于较新的busybox)
                "date \"${formatDateForCommand1(newTime)}\"",
                // 格式2: date MMDDhhmm (适用于较老的busybox)
                "date ${formatDateForCommand2(newTime)}",
                // 格式3: date -s @timestamp (适用于支持-s参数的date)
                "date -s @${newTime.time / 1000}"
            )
            
            // 尝试多种su命令执行方式
            val suCommands = listOf(
                arrayOf("su", "-c"),           // 标准方式
                arrayOf("su", "0", "-c"),      // 指定用户ID
                arrayOf("su", "root", "-c"),   // 指定用户名为root
                arrayOf("su", "-")             // 交互式方式（备用）
            )
            
            for (command in commands) {
                Log.d(TAG, "尝试执行命令: $command")
                
                var success = false
                for (suCommand in suCommands) {
                    try {
                        val process = if (suCommand.size == 2) {
                            // 标准方式: su -c "command"
                            Runtime.getRuntime().exec(arrayOf(suCommand[0], suCommand[1], command))
                        } else if (suCommand.size == 3) {
                            // 指定用户方式: su 0 -c "command" 或 su root -c "command"
                            Runtime.getRuntime().exec(arrayOf(suCommand[0], suCommand[1], suCommand[2], command))
                        } else {
                            // 交互式方式（备用）
                            Runtime.getRuntime().exec(arrayOf("su", "-"))
                        }
                        
                        val exitCode = process.waitFor()
                        
                        if (exitCode == 0) {
                            Log.d(TAG, "Shell命令执行成功: $command (使用: ${suCommand.joinToString(" ")})")
                            
                            // 尝试同步硬件时钟
                            try {
                                val hwclockProcess = Runtime.getRuntime().exec(arrayOf("su", "-c", "hwclock -w"))
                                val hwclockExitCode = hwclockProcess.waitFor()
                                if (hwclockExitCode == 0) {
                                    Log.d(TAG, "硬件时钟同步成功")
                                } else {
                                    Log.w(TAG, "硬件时钟同步失败，退出码: $hwclockExitCode")
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "硬件时钟同步异常: ${e.message}")
                            }
                            
                            success = true
                            break
                        } else {
                            Log.w(TAG, "命令执行失败: $command (使用: ${suCommand.joinToString(" ")})，退出码: $exitCode")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "命令执行异常: $command (使用: ${suCommand.joinToString(" ")}) - ${e.message}")
                    }
                }
                
                if (success) {
                    return true
                }
            }
            
            Log.w(TAG, "所有date命令格式都失败")
            
            // 尝试使用shell脚本方式
            return tryShellScript(newTime)
            
        } catch (e: Exception) {
            Log.e(TAG, "Shell命令执行异常: ${e.message}", e)
            false
        }
    }
    
    /**
     * 使用shell脚本方式修改系统时间（备用方法）
     */
    private fun tryShellScript(newTime: Date): Boolean {
        return try {
            val calendar = Calendar.getInstance().apply { time = newTime }
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
            val hour = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
            val minute = String.format("%02d", calendar.get(Calendar.MINUTE))
            
            // 创建临时shell脚本
            val script = """
                #!/system/bin/sh
                date $month$day$hour$minute
                hwclock -w
            """.trimIndent()
            
            Log.d(TAG, "尝试使用shell脚本方式修改时间")
            
            // 方法1: 直接执行shell脚本
            val process1 = Runtime.getRuntime().exec(arrayOf("su", "-c", script))
            val exitCode1 = process1.waitFor()
            
            if (exitCode1 == 0) {
                Log.d(TAG, "Shell脚本执行成功")
                return true
            }
            
            // 方法2: 使用echo管道到su
            val echoCommand = "echo 'date $month$day$hour$minute' | su"
            val process2 = Runtime.getRuntime().exec(arrayOf("sh", "-c", echoCommand))
            val exitCode2 = process2.waitFor()
            
            if (exitCode2 == 0) {
                Log.d(TAG, "Echo管道方式执行成功")
                return true
            }
            
            Log.w(TAG, "Shell脚本方式也失败")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Shell脚本执行异常: ${e.message}", e)
            false
        }
    }
    
    /**
     * 格式化日期为 "YYYY-MM-DD HH:MM:SS" 格式
     */
    private fun formatDateForCommand1(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val year = calendar.get(Calendar.YEAR)
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        val hour = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
        val minute = String.format("%02d", calendar.get(Calendar.MINUTE))
        val second = String.format("%02d", calendar.get(Calendar.SECOND))
        
        return "$year-$month-$day $hour:$minute:$second"
    }
    
    /**
     * 格式化日期为 "MMDDhhmm" 格式
     */
    private fun formatDateForCommand2(date: Date): String {
        val calendar = Calendar.getInstance().apply { time = date }
        val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
        val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
        val hour = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
        val minute = String.format("%02d", calendar.get(Calendar.MINUTE))
        
        return "$month$day$hour$minute"
    }
    
    /**
     * 通过Settings API修改系统时间（需要系统权限）
     */
    private fun setSystemTimeViaSettings(newTime: Date): Boolean {
        return try {
            // 检查是否有修改系统时间的权限
            if (!hasSystemTimePermission()) {
                Log.w(TAG, "没有修改系统时间的权限")
                return false
            }
            
            val timeInMillis = newTime.time
            val settings = context.contentResolver
            
            // 使用正确的系统时间设置项
            // 注意：大多数Android设备不允许通过Settings API修改系统时间
            // 这里只是尝试，实际需要root权限
            try {
                // 尝试使用系统时间设置（如果存在）
                android.provider.Settings.System.putLong(settings, "system_time", timeInMillis)
                Log.d(TAG, "通过Settings API修改系统时间成功")
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Settings API不支持修改系统时间: ${e.message}")
                return false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Settings API修改系统时间失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 检查是否有修改系统时间的权限（缓存结果）
     */
    private fun hasSystemTimePermission(): Boolean {
        return settingsPermissionCache.get() ?: run {
            val result = try {
                // 检查是否有WRITE_SETTINGS权限
                context.checkSelfPermission(android.Manifest.permission.WRITE_SETTINGS) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                Log.e(TAG, "检查权限失败: ${e.message}", e)
                false
            }
            settingsPermissionCache.set(result)
            result
        }
    }
    
    /**
     * 获取当前系统时间
     */
    fun getCurrentSystemTime(): Date {
        return Date()
    }
    
    /**
     * 检查是否有root权限（异步执行，避免ANR）
     */
    suspend fun hasRootPermissionAsync(): Boolean {
        return withContext(Dispatchers.IO) {
            rootPermissionCache.get() ?: run {
                val result = try {
                    // 使用超时机制避免阻塞
                    withTimeout(3000) { // 3秒超时
                        val process = Runtime.getRuntime().exec("su")
                        val exitCode = process.waitFor()
                        exitCode == 0
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "检查root权限失败: ${e.message}", e)
                    false
                }
                rootPermissionCache.set(result)
                result
            }
        }
    }
    
    /**
     * 快速检查root权限（同步版本，不阻塞UI）
     */
    fun hasRootPermissionQuick(): Boolean {
        return rootPermissionCache.get() ?: run {
            val result = try {
                // 检查多个可能的su文件路径，适配不同设备
                val rootFiles = listOf(
                    "/system/bin/su",      // RK3566等设备
                    "/system/xbin/su",     // RK3568、RK3288等设备
                    "/sbin/su",            // 部分设备
                    "/bin/su",             // 部分设备
                    "/usr/bin/su"          // 部分设备
                )
                
                for (file in rootFiles) {
                    try {
                        val process = Runtime.getRuntime().exec("ls $file")
                        val exitCode = process.waitFor()
                        if (exitCode == 0) {
                            Log.d(TAG, "发现root文件: $file")
                            return@run true
                        }
                    } catch (e: Exception) {
                        // 忽略单个文件检查失败
                        Log.d(TAG, "检查文件失败: $file - ${e.message}")
                    }
                }
                
                Log.w(TAG, "未找到root权限文件")
                false
            } catch (e: Exception) {
                Log.e(TAG, "检查root权限失败: ${e.message}", e)
                false
            }
            rootPermissionCache.set(result)
            result
        }
    }
    
    /**
     * 检查是否有root权限（通过多种方式）
     */
    fun hasRootPermission(): Boolean {
        return hasRootPermissionQuick()
    }
    
    /**
     * 检查是否有root权限（通过文件系统）
     */
    fun hasRootPermissionByFiles(): Boolean {
        return hasRootPermissionQuick()
    }
    
    /**
     * 实际测试系统时间修改权限
     */
    fun testSystemTimePermission(): Boolean {
        return try {
            // 获取当前时间
            val currentTime = Date()
            val testTime = Date(currentTime.time + 1000) // 设置为1秒后
            
            // 使用与快速检查相同的逻辑
            val calendar = Calendar.getInstance().apply { time = testTime }
            val month = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
            val day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
            val hour = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
            val minute = String.format("%02d", calendar.get(Calendar.MINUTE))
            
            val command = "date ${month}${day}${hour}${minute}"
            Log.d(TAG, "测试系统时间修改权限: $command")
            
            // 使用su -c执行命令
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exitCode = process.waitFor()
            
            val success = exitCode == 0
            Log.d(TAG, "系统时间修改权限测试结果: $success (退出码: $exitCode)")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "测试系统时间修改权限失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 获取权限状态信息（异步版本，带超时）
     */
    suspend fun getPermissionStatusAsync(): String {
        return try {
            withTimeout(5000) { // 5秒超时
                val hasRoot = hasRootPermissionAsync()
                
                if (hasRoot) {
                    "✅ 已获得root权限"
                } else {
                    "❌ 无root权限"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "权限状态检查超时或失败: ${e.message}", e)
            "❌ 权限检查超时"
        }
    }
    
    /**
     * 获取权限状态信息（快速同步版本，包含实际测试）
     */
    fun getPermissionStatusQuick(): String {
        val hasRoot = hasRootPermissionQuick()
        
        Log.d(TAG, "权限检查结果: $hasRoot")
        
        return if (hasRoot) {
            "✅ 已获得root权限"
        } else {
            "❌ 无root权限"
        }
    }
    
    /**
     * 获取权限状态信息（同步版本，使用缓存）
     */
    fun getPermissionStatus(): String {
        val hasRoot = hasRootPermission()
        val hasSettings = hasSystemTimePermission()
        
        return when {
            hasRoot -> "✅ 已获得root权限"
            hasSettings -> "⚠️ 仅有Settings权限（可能无法修改系统时间）"
            else -> "❌ 无修改系统时间权限"
        }
    }
    
    /**
     * 调试权限检查（详细日志）
     */
    fun debugPermissionCheck(): String {
        val result = StringBuilder()
        result.append("=== 权限检查调试信息 ===\n")
        
        try {
            // 检查root相关文件
            result.append("检查root相关文件:\n")
            val rootFiles = listOf(
                "/system/bin/su",      // RK3566等设备
                "/system/xbin/su",     // RK3568、RK3288等设备
                "/sbin/su",            // 部分设备
                "/bin/su",             // 部分设备
                "/usr/bin/su"          // 部分设备
            )
            
            var foundFiles = 0
            for (file in rootFiles) {
                try {
                    val process = Runtime.getRuntime().exec("ls $file")
                    val exitCode = process.waitFor()
                    if (exitCode == 0) {
                        result.append("✅ 发现root文件: $file\n")
                        foundFiles++
                    } else {
                        result.append("❌ 文件不存在: $file\n")
                    }
                } catch (e: Exception) {
                    result.append("❌ 检查失败: $file - ${e.message}\n")
                }
            }
            
            // 综合判断
            result.append("\n综合判断:\n")
            result.append("发现root文件数量: $foundFiles\n")
            val hasRoot = foundFiles > 0
            result.append("最终结果: ${if (hasRoot) "✅ 有root权限" else "❌ 无root权限"}\n")
            
        } catch (e: Exception) {
            result.append("权限检查异常: ${e.message}\n")
        }
        
        result.append("=== 调试信息结束 ===\n")
        return result.toString()
    }
    

    

    
    /**
     * 获取当前系统时区
     * @return 当前时区标识符
     */
    fun getCurrentTimezone(): String {
        return try {
            val timezone = TimeZone.getDefault()
            val timezoneId = timezone.id
            Log.d(TAG, "当前系统时区: $timezoneId")
            timezoneId
        } catch (e: Exception) {
            Log.e(TAG, "获取当前时区失败: ${e.message}", e)
            "UTC"
        }
    }
    

    
    /**
     * 清除权限缓存，强制重新检查
     */
    fun clearPermissionCache() {
        rootPermissionCache.set(null)
        settingsPermissionCache.set(null)
    }
}

 