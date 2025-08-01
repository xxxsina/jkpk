package com.jiankangpaika.app.ui.components.common

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.data.model.BottomNavItem
import com.jiankangpaika.app.utils.UserManager

/**
 * 标签页切换时展示插屏广告的统一处理函数
 * @param context 上下文
 * @param newTabIndex 新的标签页索引
 * @param currentTabIndex 当前标签页索引
 * @param onTabChanged 标签页切换回调
 */
fun showInterstitialAdOnTabSwitch(
    context: Context,
    newTabIndex: Int,
    currentTabIndex: Int,
    onTabChanged: (Int) -> Unit
) {
    Log.d("CommonComponents", "🔄 [标签页切换] 从索引 $currentTabIndex 切换到 $newTabIndex")
    
    if (AdUtils.shouldShowInterstitialAd(context)) {
        Log.d("CommonComponents", "✅ [插屏广告策略] 满足展示条件，开始加载插屏广告")
        
        UnifiedAdManager.getInstance().loadInterstitialAd(context) { success: Boolean, message: String? ->
            Log.d("CommonComponents", "📥 [插屏广告加载] 结果: success=$success, message=$message")
            
            if (success) {
                Log.d("CommonComponents", "🎬 [插屏广告] 加载成功，开始展示广告")
                
                UnifiedAdManager.getInstance().showInterstitialAd(context as androidx.activity.ComponentActivity) { showSuccess: Boolean, showMessage: String? ->
                    Log.d("CommonComponents", "🎭 [插屏广告展示] 结果: success=$showSuccess, message=$showMessage")
                    
                    if (showSuccess) {
                        Log.i("CommonComponents", "🎉 [插屏广告] 展示成功，记录展示时间")
                        AdUtils.recordInterstitialAdShown(context)
                    } else {
                        Log.w("CommonComponents", "⚠️ [插屏广告] 展示失败: $showMessage")
                    }
                    
                    // 无论展示成功与否，都执行标签页切换
                    Log.d("CommonComponents", "🔄 [标签页切换] 执行切换到索引 $newTabIndex")
                    onTabChanged(newTabIndex)
                }
            } else {
                Log.w("CommonComponents", "⚠️ [插屏广告] 加载失败: $message，直接执行标签页切换")
                // 加载失败时直接切换标签页
                onTabChanged(newTabIndex)
            }
        }
    } else {
        Log.d("CommonComponents", "⏭️ [插屏广告策略] 不满足展示条件，直接执行标签页切换")
        // 不满足展示条件时直接切换标签页
        onTabChanged(newTabIndex)
    }
}

@Composable
fun StatusBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 状态图标区域（电池、WiFi、信号）
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 简化的状态图标
            Box(
                modifier = Modifier
                    .size(16.dp, 8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(2.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(12.dp, 8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(2.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .size(20.dp, 10.dp)
                    .background(
                        Color.Black.copy(alpha = 0.6f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val context = LocalContext.current
    val navItems = listOf(
        BottomNavItem("首页", Icons.Default.Home, selectedIndex == 0),
        BottomNavItem("精选", Icons.AutoMirrored.Filled.DirectionsRun, selectedIndex == 1),
        BottomNavItem("每日签到", Icons.Default.CheckCircle, selectedIndex == 2),
        BottomNavItem("我的", Icons.Default.Person, selectedIndex == 3)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                navItems.forEachIndexed { index, item ->
                    BottomNavItem(
                        item = item,
                        isSelected = index == selectedIndex,
                        onClick = {
                            // 检查是否需要触发插屏广告（首页=0 或 我的=3）
                            if (index == 0 || index == 3) {
                                // 对于"我的"页面，需要检查登录状态
                                if (index == 3 && !UserManager.isLoggedIn(context)) {
                                    Log.d("CommonComponents", "⚠️ [标签页切换] 用户未登录，跳过插屏广告，直接切换到我的页面")
                                    onTabSelected(index)
                                } else {
                                    Log.d("CommonComponents", "🎯 [标签页切换] 触发插屏广告逻辑，目标索引: $index")
                                    showInterstitialAdOnTabSwitch(
                                        context = context,
                                        newTabIndex = index,
                                        currentTabIndex = selectedIndex,
                                        onTabChanged = onTabSelected
                                    )
                                }
                            } else {
                                Log.d("CommonComponents", "⏭️ [标签页切换] 直接切换到索引: $index")
                                onTabSelected(index)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
//            .padding(1.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (isSelected) Color(0xFF3B82F6) else Color.Gray,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color(0xFF3B82F6) else Color.Gray,
            fontSize = 10.sp
        )
    }
}