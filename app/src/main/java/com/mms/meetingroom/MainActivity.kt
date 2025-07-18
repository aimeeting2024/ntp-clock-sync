package com.mms.meetingroom

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.mms.meetingroom.ntp.NTPManager
import com.mms.meetingroom.ntp.NTPStatus
import com.mms.meetingroom.ui.component.NTPStatusComponent
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var ntpManager: NTPManager
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "MainActivity onCreate 开始")
        
        try {
            // 初始化NTP管理器
            ntpManager = NTPManager(this)
            
            setContent {
                MaterialTheme {
                    NTPConfigScreen(
                        ntpManager = ntpManager,
                        onStartService = { ntpServer, syncInterval ->
                            startBackgroundService(ntpServer, syncInterval)
                        }
                    )
                }
            }
            
            Log.d(TAG, "MainActivity onCreate 完成")
        } catch (e: Exception) {
            Log.e(TAG, "MainActivity onCreate 失败: ${e.message}", e)
            // 显示错误界面
            setContent {
                MaterialTheme {
                    ErrorScreen(error = e.message ?: "未知错误")
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "MainActivity onDestroy")
    }
    
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "MainActivity onPause")
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "MainActivity onResume")
    }
    
    private fun startBackgroundService(ntpServer: String, syncInterval: Long) {
        try {
            Log.d("MainActivity", "启动NTP后台服务: 服务器=$ntpServer, 间隔=${syncInterval}分钟")
            
            // 启动后台服务
            NTPBackgroundService.startService(this, ntpServer, syncInterval)
            
            // 显示启动成功提示
            showStartSuccessDialog()
            
        } catch (e: Exception) {
            Log.e("MainActivity", "启动后台服务失败: ${e.message}", e)
        }
    }
    
    private fun showStartSuccessDialog() {
        Log.d("MainActivity", "NTP后台服务启动成功，应用将退出")
        
        // 显示成功提示
        setContent {
            MaterialTheme {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "✅ 启动成功",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "NTP后台服务已启动\n应用将在2秒后退出",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            Text(
                                text = "可通过通知栏查看同步状态",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
        
        // 延迟2秒后退出应用
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            finish()
        }, 2000)
    }
}

@Composable
fun NTPConfigScreen(
    ntpManager: NTPManager,
    onStartService: (String, Long) -> Unit
) {
    var ntpServer by remember { mutableStateOf("time.windows.com") }
    var syncInterval by remember { mutableStateOf(30L) }
    var isStarting by remember { mutableStateOf(false) }
    var isManualSyncing by remember { mutableStateOf(false) }
    var manualSyncResult by remember { mutableStateOf<String?>(null) }
    var ntpStatus by remember { mutableStateOf(ntpManager.getNTPStatus()) }

    val scope = rememberCoroutineScope()
    
    // 自动更新状态
    LaunchedEffect(Unit) {
        while (true) {
            ntpStatus = ntpManager.getNTPStatus()
            kotlinx.coroutines.delay(1000) // 每秒更新状态，更及时
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 左侧配置区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 标题
            Text(
                text = "NTP后台同步配置",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // 说明文字
            Text(
                text = "配置完成后，NTP同步服务将在后台运行，不会干扰其他程序。",
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // NTP服务器配置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "NTP服务器配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = ntpServer,
                        onValueChange = { ntpServer = it },
                        label = { Text("NTP服务器地址") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 同步间隔配置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "同步间隔配置",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "当前间隔: ${syncInterval}分钟",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { syncInterval = 5L },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("5分钟", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = { syncInterval = 15L },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("15分钟", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = { syncInterval = 30L },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("30分钟", fontSize = 11.sp)
                        }
                        
                        Button(
                            onClick = { syncInterval = 60L },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("60分钟", fontSize = 11.sp)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            

            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 权限状态显示
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp)
                ) {
                    Text(
                        text = "系统权限状态",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 手动权限检查
                    var permissionStatus by remember { mutableStateOf("未检查") }
                    var isCheckingPermission by remember { mutableStateOf(false) }
                    
                    // 手动检查权限按钮
                    Button(
                        onClick = {
                            if (!isCheckingPermission) {
                                isCheckingPermission = true
                                scope.launch {
                                    try {
                                        val status = ntpManager.getSystemTimeManager().getPermissionStatusAsync()
                                        permissionStatus = status
                                    } catch (e: Exception) {
                                        Log.e("MainActivity", "权限检查失败: ${e.message}", e)
                                        permissionStatus = "❌ 权限检查失败"
                                    } finally {
                                        isCheckingPermission = false
                                    }
                                }
                            }
                        },
                        enabled = !isCheckingPermission,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text(
                            text = if (isCheckingPermission) "检查中..." else "检查Root权限",
                            fontSize = 12.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 调试按钮
                    var debugInfo by remember { mutableStateOf<String?>(null) }
                    var showDebug by remember { mutableStateOf(false) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                showDebug = !showDebug
                                if (showDebug) {
                                    scope.launch {
                                        debugInfo = ntpManager.getSystemTimeManager().debugPermissionCheck()
                                    }
                                } else {
                                    debugInfo = null
                                }
                            }
                        ) {
                            Text(
                                text = if (showDebug) "隐藏调试信息" else "显示调试信息",
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                    // 权限状态显示
                    LaunchedEffect(Unit) {
                        try {
                            val status = ntpManager.getSystemTimeManager().getPermissionStatusQuick()
                            permissionStatus = status
                        } catch (e: Exception) {
                            permissionStatus = "❌ 权限检查失败"
                        }
                    }
                    
                    Text(
                        text = "权限状态: $permissionStatus",
                        fontSize = 13.sp,
                        color = when {
                            permissionStatus.contains("✅") -> MaterialTheme.colorScheme.primary
                            permissionStatus.contains("❌") -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    
                    if (!permissionStatus.contains("✅")) {
                        Text(
                            text = "⚠️ 需要root权限才能修改系统时间",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    // 显示调试信息
                    debugInfo?.let { info ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Text(
                                text = info,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 手动同步按钮
            Button(
                onClick = {
                    if (!isManualSyncing && ntpServer.isNotBlank()) {
                        scope.launch {
                            isManualSyncing = true
                            manualSyncResult = null
                            
                            try {
                                val result = ntpManager.syncNow(ntpServer, true) // 默认修改系统时间
                                if (result.success) {
                                    // 立即更新NTP状态
                                    ntpStatus = ntpManager.getNTPStatus()
                                    
                                    // 根据实际修改结果显示状态
                                    val systemTimeInfo = when {
                                        result.systemTimeModified == true -> "✅ 修改成功"
                                        result.systemTimeModified == false -> "❌ 修改失败"
                                        else -> "⚠️ 未尝试修改"
                                    }
                                    
                                    // 获取当前时区
                                    val currentTz = ntpManager.getSystemTimeManager().getCurrentTimezone()
                                    
                                    manualSyncResult = "✅ 手动同步成功！\n" +
                                            "NTP时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.ntpTime)}\n" +
                                            "本地时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.localTime)}\n" +
                                            "时间偏移: ${if (result.offset!! >= 0) "+" else ""}${result.offset}ms\n" +
                                            "网络延迟: ${result.delay}ms\n" +
                                            "系统时间修改: $systemTimeInfo\n" +
                                            "当前时区: $currentTz"
                                } else {
                                    manualSyncResult = "❌ 手动同步失败！\n错误信息: ${result.errorMessage}"
                                }
                            } catch (e: Exception) {
                                manualSyncResult = "❌ 手动同步异常！\n异常信息: ${e.message}"
                            } finally {
                                isManualSyncing = false
                            }
                        }
                    }
                },
                enabled = !isManualSyncing && ntpServer.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Text(
                    text = if (isManualSyncing) "同步中..." else "手动同步测试",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 启动服务按钮
            Button(
                onClick = {
                    if (!isStarting && ntpServer.isNotBlank()) {
                        isStarting = true
                        onStartService(ntpServer, syncInterval)
                    }
                },
                enabled = !isStarting && ntpServer.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isStarting) "启动中..." else "启动后台服务",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 说明文字
            Text(
                text = "启动后应用将退出，NTP同步服务在后台运行。\n可通过通知栏查看同步状态。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // 右侧状态和结果显示区域
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            // 手动同步结果
            manualSyncResult?.let { result ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (result.startsWith("✅")) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // NTP状态显示
            NTPStatusComponent(
                ntpStatus = ntpStatus,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 当前时间和时区显示
            var currentTime by remember { mutableStateOf(Date()) }
            var currentTimezone by remember { mutableStateOf("") }
            
            LaunchedEffect(Unit) {
                while (true) {
                    currentTime = Date() // 直接使用系统当前时间
                    currentTimezone = ntpManager.getSystemTimeManager().getCurrentTimezone()
                    kotlinx.coroutines.delay(1000) // 每秒更新
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "当前时间",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
                    Text(
                        text = dateFormat.format(currentTime),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = "时区: $currentTimezone",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (ntpStatus.lastOffset != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "NTP偏移: ${if (ntpStatus.lastOffset!! >= 0) "+" else ""}${ntpStatus.lastOffset}ms",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 

@Composable
fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "❌ 应用启动失败",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "错误信息: $error",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "请重启应用或检查设备状态",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
} 