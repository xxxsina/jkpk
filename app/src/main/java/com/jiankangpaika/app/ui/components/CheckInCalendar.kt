package com.jiankangpaika.app.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankangpaika.app.data.preferences.CheckInDayData
import com.jiankangpaika.app.data.preferences.CheckInStatus
import java.util.*

@Composable
fun CheckInCalendar(
    checkInData: List<CheckInDayData>,
    currentYear: Int,
    currentMonth: Int,
    maxCheckInPerDay: Int = 10,
    onMonthChange: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthNames = listOf(
        "一月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "十一月", "十二月"
    )
    
    val weekDays = listOf("日", "一", "二", "三", "四", "五", "六")
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 月份导航栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (currentMonth == 1) {
                            onMonthChange(currentYear - 1, 12)
                        } else {
                            onMonthChange(currentYear, currentMonth - 1)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上个月",
                        tint = Color(0xFF6366F1)
                    )
                }
                
                Text(
                    text = "${currentYear}年 ${monthNames[currentMonth - 1]}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF161823)
                )
                
                IconButton(
                    onClick = {
                        if (currentMonth == 12) {
                            onMonthChange(currentYear + 1, 1)
                        } else {
                            onMonthChange(currentYear, currentMonth + 1)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "下个月",
                        tint = Color(0xFF6366F1)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 星期标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(2f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // 日历网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.height(240.dp),
            ) {
                // 计算月份第一天是星期几
                val calendar = Calendar.getInstance()
                calendar.set(currentYear, currentMonth - 1, 1)
                val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1

                // 添加空白占位符
                items(firstDayOfWeek) {
                    Box(modifier = Modifier.size(32.dp))
                }

                // 添加日期
                items(checkInData) { dayData ->
                    CheckInDayItem(
                        dayData = dayData,
                        maxCheckInPerDay = maxCheckInPerDay,
                        onDayClick = { /* 处理日期点击 */ },
                        modifier = Modifier.size(38.dp)
                    )
                }
            }
            
//            Spacer(modifier = Modifier.height(8.dp))
            
            // 图例
//            CheckInLegend(maxCheckInPerDay = maxCheckInPerDay)
        }
    }
}

@Composable
fun CheckInDayItem(
    dayData: CheckInDayData,
    maxCheckInPerDay: Int = 10,
    onDayClick: (CheckInDayData) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (dayData.status) {
        CheckInStatus.TODAY -> Color.Transparent //(0xFF6366F1)
        CheckInStatus.CHECKED_IN -> Color.Transparent  // 过去日期取消背景色
        CheckInStatus.AVAILABLE -> Color.Transparent   // 过去日期取消背景色
        CheckInStatus.MISSED -> Color.Transparent      // 过去日期取消背景色
        CheckInStatus.FUTURE -> Color.Transparent
    }

    val contentColor = when (dayData.status) {
        CheckInStatus.TODAY -> Color(0xFF6366F1)
        CheckInStatus.CHECKED_IN -> Color(0xF89CA3AF)
        CheckInStatus.AVAILABLE -> Color(0xFF374151)
        CheckInStatus.MISSED -> Color(0xFF9CA3AF)
        CheckInStatus.FUTURE -> Color(0xF89CA3AF)
    }
    
    // 外层Box用于容纳可能超出边界的圆圈
    Box(
        modifier = modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        // 日期主体
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(backgroundColor)
                .clickable(
                    enabled = dayData.status == CheckInStatus.TODAY || dayData.status == CheckInStatus.AVAILABLE
                ) {
                    onDayClick(dayData)
                }
                .then(
                    when (dayData.status) {
                        CheckInStatus.TODAY -> Modifier.border(1.dp, Color(0xFF6366F1), CircleShape)
                        CheckInStatus.CHECKED_IN -> Modifier.border(1.dp, Color(0xFFDAD4D6), CircleShape)  // 已签到过去日期用灰色圆圈
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            // 显示日期
            Text(
                text = dayData.day.toString(),
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (dayData.status == CheckInStatus.CHECKED_IN || dayData.status == CheckInStatus.TODAY) FontWeight.Bold else FontWeight.Normal
            )
        }
        
        // 签到状态圆圈，浮动在右上角
        if ((dayData.status == CheckInStatus.CHECKED_IN || dayData.status == CheckInStatus.TODAY) && dayData.checkInCount > 0) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .offset(x = 5.dp)
                    .clip(CircleShape)
                    .background(
                        if (dayData.isComplete) Color(0xFF10B981) // 绿色 - 已完成
                        else Color(0xFF3B82F6) // 蓝色 - 未完成
                    )
                    .align(Alignment.TopEnd)
                    .zIndex(2f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (dayData.isComplete) "√" else dayData.checkInCount.toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.offset(y = (-3).dp)
                )
            }
        }
    }
}

@Composable
fun CheckInLegend(maxCheckInPerDay: Int = 10) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(
                color = Color(0xFF3B82F6),
                text = "未满${maxCheckInPerDay}次",
                showNumber = true
            )
            LegendItem(
                color = Color(0xFF10B981),
                text = "满${maxCheckInPerDay}次",
                showFull = true
            )
            LegendItem(
                color = Color(0xFF6366F1),
                text = "今天",
                showBorder = true
            )
        }
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String,
    showCheck: Boolean = false,
    showBorder: Boolean = false,
    showNumber: Boolean = false,
    showFull: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (showBorder) Color.Transparent else color)
                .then(
                    if (showBorder) {
                        Modifier.border(1.dp, color, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                showCheck -> {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(8.dp)
                    )
                }
                showNumber -> {
                    Text(
                        text = "5",
                        color = Color.White,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                showFull -> {
                    Text(
                        text = "√",
                        color = Color.White,
                        fontSize = 6.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF6B7280),
            fontSize = 10.sp
        )
    }
}