package com.mms.meetingroom.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mms.meetingroom.ntp.NTPStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NTPStatusComponent(
    ntpStatus: NTPStatus,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NTP时间同步",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // 状态指示器
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (ntpStatus.isRunning) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 状态信息
            if (ntpStatus.lastSyncTime != null) {
                Text(
                    text = "最后同步: ${dateFormat.format(ntpStatus.lastSyncTime)}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                ntpStatus.lastOffset?.let { offset ->
                    Text(
                        text = "时间偏移: ${if (offset >= 0) "+" else ""}${offset}ms",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ntpStatus.lastDelay?.let { delay ->
                    Text(
                        text = "网络延迟: ${delay}ms",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "未同步",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 错误信息
            ntpStatus.syncError?.let { error ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "错误: $error",
                    fontSize = 12.sp,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun NTPSimpleStatus(
    ntpStatus: NTPStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 状态指示器
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = if (ntpStatus.isRunning) Color(0xFF4CAF50) else Color(0xFFFF9800),
                    shape = RoundedCornerShape(4.dp)
                )
        )
        
        Text(
            text = if (ntpStatus.isRunning) "NTP同步中" else "NTP未同步",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        ntpStatus.lastOffset?.let { offset ->
            Text(
                text = "(${if (offset >= 0) "+" else ""}${offset}ms)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
} 