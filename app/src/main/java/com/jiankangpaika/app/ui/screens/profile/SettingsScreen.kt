package com.jiankangpaika.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jiankangpaika.app.MainActivity
import com.jiankangpaika.app.ad.ConfigMonitorManager
import com.jiankangpaika.app.data.model.ProfileMenuItem
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.utils.VersionMonitorManager
import com.jiankangpaika.app.utils.VersionUpdateManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    val context = LocalContext.current
    val configMonitorManager = remember { ConfigMonitorManager.getInstance(context) }
    val versionMonitorManager = remember { VersionMonitorManager.getInstance(context) }
    val versionUpdateManager = remember { VersionUpdateManager(context) }
    var showMonitorStatusDialog by remember { mutableStateOf(false) }
    
    // 获取当前版本信息
    val currentVersionName = remember { versionUpdateManager.getCurrentVersionName() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF161823)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { 
                        // 返回到ProfileScreen（我的页面）
                        navController.navigate("profile_main") {
                            popUpTo("main") { inclusive = false }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color(0xFF161823)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // 设置菜单卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column {
                    val menuItems = listOf(
                        ProfileMenuItem("个人设置", "管理您的个人信息") { navigationManager.navigateToPersonalSettings() },
//                        ProfileMenuItem("监控状态", "查看配置和版本监控状态") { showMonitorStatusDialog = true },
                        ProfileMenuItem("隐私政策", "查看隐私政策") { navigationManager.navigateToPrivacyPolicy() },
                        ProfileMenuItem("用户协议", "查看用户协议") { navigationManager.navigateToUserAgreement() },
                        ProfileMenuItem("联系我们", "获取帮助和支持") { navigationManager.navigateToContactUs() },ProfileMenuItem("检查更新", "点击检查最新版本") {
                            // 手动检查版本更新
                            if (context is MainActivity) {
                                context.checkVersionUpdateManually()
                            } else {
                                // 如果不是MainActivity，直接调用版本更新管理器
                                versionUpdateManager.checkForUpdate(showNoUpdateDialog = true)
                            }
                        },
                        ProfileMenuItem("关于健康派卡", "版本 $currentVersionName") {
                            // TODO: 实现关于页面
                        }
                    )
                    
                    menuItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { item.onClick() }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.title,
                                    fontSize = 16.sp,
                                    color = Color(0xFF161823),
                                    fontWeight = FontWeight.Medium
                                )
                                if (item.subtitle.isNotEmpty()) {
                                    Text(
                                        text = item.subtitle,
                                        fontSize = 14.sp,
                                        color = Color(0xFF6B7280)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                        
                        if (index < menuItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = Color.Gray.copy(alpha = 0.1f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 监控状态对话框
    if (showMonitorStatusDialog) {
        MonitorStatusDialog(
            configMonitorManager = configMonitorManager,
            versionMonitorManager = versionMonitorManager,
            onDismiss = { showMonitorStatusDialog = false }
        )
    }
}

@Composable
fun MonitorStatusDialog(
    configMonitorManager: ConfigMonitorManager,
    versionMonitorManager: VersionMonitorManager,
    onDismiss: () -> Unit
) {
    val configStatus = configMonitorManager.getMonitorStatus()
    val versionStatus = versionMonitorManager.getMonitorStatus()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "监控状态",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF161823)
            )
        },
        text = {
            Column {
                Text(
                    text = "📡 配置监控状态",
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF161823),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "• 启用状态: ${if (configStatus.isEnabled) "✅ 已启用" else "❌ 已禁用"}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "• 运行状态: ${if (configStatus.isRunning) "🟢 运行中" else "🔴 已停止"}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "• 暂停状态: ${if (configStatus.isPaused) "⏸️ 已暂停" else "▶️ 正常"}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "• 监控间隔: ${configStatus.intervalMs / 1000}秒",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "🔄 版本监控状态",
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF161823),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "• 启用状态: ${if (versionStatus.isEnabled) "✅ 已启用" else "❌ 已禁用"}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "• 运行状态: ${if (versionStatus.isRunning) "🟢 运行中" else "🔴 已停止"}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "• 暂停状态: ${if (versionStatus.isPaused) "⏸️ 已暂停" else "▶️ 正常"}",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
                Text(
                    text = "• 监控间隔: ${versionStatus.intervalMs / 1000}秒",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "确定",
                    color = Color(0xFFEA3323)
                )
            }
        }
    )
}