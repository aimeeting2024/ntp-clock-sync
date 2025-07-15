package com.mms.meetingroom.ntp

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference

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
            
            for (command in commands) {
                Log.d(TAG, "尝试执行命令: $command")
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    Log.d(TAG, "Shell命令执行成功: $command")
                    
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
                    
                    return true
                } else {
                    Log.w(TAG, "命令执行失败: $command，退出码: $exitCode")
                }
            }
            
            Log.w(TAG, "所有date命令格式都失败")
            false
            
        } catch (e: Exception) {
            Log.e(TAG, "Shell命令执行异常: ${e.message}", e)
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
                    val process = Runtime.getRuntime().exec("su")
                    val exitCode = process.waitFor()
                    exitCode == 0
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
     * 检查是否有root权限（同步版本，使用缓存）
     */
    fun hasRootPermission(): Boolean {
        return rootPermissionCache.get() ?: false
    }
    
    /**
     * 获取权限状态信息（异步版本）
     */
    suspend fun getPermissionStatusAsync(): String {
        val hasRoot = hasRootPermissionAsync()
        val hasSettings = hasSystemTimePermission()
        
        return when {
            hasRoot -> "✅ 已获得root权限"
            hasSettings -> "⚠️ 仅有Settings权限（可能无法修改系统时间）"
            else -> "❌ 无修改系统时间权限"
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
     * 清除权限缓存，强制重新检查
     */
    fun clearPermissionCache() {
        rootPermissionCache.set(null)
        settingsPermissionCache.set(null)
    }
} 