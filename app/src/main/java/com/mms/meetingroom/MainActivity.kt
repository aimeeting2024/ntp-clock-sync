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
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                    
                    // 异步显示权限状态
                    var permissionStatus by remember { mutableStateOf("检查中...") }
                    
                    LaunchedEffect(Unit) {
                        try {
                            val status = ntpManager.getSystemTimeManager().getPermissionStatusAsync()
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
                            permissionStatus.contains("⚠️") -> MaterialTheme.colorScheme.tertiary
                            permissionStatus.contains("检查中") -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.error
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
                                    
                                    val systemTimeInfo = "\n系统时间修改: ${if (result.ntpTime != null) "已尝试修改" else "未修改"}"
                                    
                                    manualSyncResult = "✅ 手动同步成功！\n" +
                                            "NTP时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.ntpTime)}\n" +
                                            "本地时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(result.localTime)}\n" +
                                            "时间偏移: ${if (result.offset!! >= 0) "+" else ""}${result.offset}ms\n" +
                                            "网络延迟: ${result.delay}ms$systemTimeInfo"
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
            
            // 当前时间显示
            var currentTime by remember { mutableStateOf(Date()) }
            
            LaunchedEffect(Unit) {
                while (true) {
                    currentTime = Date() // 直接使用系统当前时间
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