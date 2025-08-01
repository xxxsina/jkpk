package com.jiankangpaika.app.ui.screens.profile

import android.content.Context
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.LogoutResult
import com.jiankangpaika.app.ui.navigation.NavigationManager
import androidx.navigation.NavController
import java.io.File
import kotlin.math.pow

// 获取文件夹大小
fun getFolderSize(folder: File): Long {
    var size = 0L
    if (folder.exists() && folder.isDirectory) {
        folder.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                getFolderSize(file)
            } else {
                file.length()
            }
        }
    }
    return size
}

// 格式化文件大小
fun formatFileSize(size: Long): String {
    if (size <= 0) return "0B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f%s", size / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

// 删除文件夹及其内容
fun deleteFolder(folder: File): Boolean {
    return try {
        if (folder.exists()) {
            folder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteFolder(file)
                } else {
                    file.delete()
                }
            }
            // 重新创建缓存目录，保持目录结构
            folder.mkdirs()
        }
        true
    } catch (e: Exception) {
        false
    }
}

// 计算缓存大小的函数
fun calculateCacheSize(context: Context): String {
    return try {
        var totalSize = 0L
        
        // 计算应用缓存目录大小
        val cacheDir = context.cacheDir
        totalSize += getFolderSize(cacheDir)
        
        // 计算外部缓存目录大小
        context.externalCacheDir?.let { externalCache ->
            totalSize += getFolderSize(externalCache)
        }
        
        // 计算数据库缓存大小
        val databaseDir = File(context.applicationInfo.dataDir, "databases")
        if (databaseDir.exists()) {
            totalSize += getFolderSize(databaseDir)
        }
        
        formatFileSize(totalSize)
    } catch (e: Exception) {
        "未知"
    }
}

// 清除缓存的函数
suspend fun clearCache(context: Context): Boolean {
    return try {
        var success = true
        
        // 清除应用缓存目录
        val cacheDir = context.cacheDir
        if (cacheDir.exists()) {
            success = success && deleteFolder(cacheDir)
        }
        
        // 清除外部缓存目录
        context.externalCacheDir?.let { externalCache ->
            if (externalCache.exists()) {
                success = success && deleteFolder(externalCache)
            }
        }
        
        // 清除广告SDK缓存
        try {
            val adClearResults = UnifiedAdManager.getInstance().clearAllCache(context)
            // 记录广告缓存清理结果，但不影响整体成功状态
        } catch (e: Exception) {
            // 广告缓存清理失败不影响整体结果
        }
        
        success
    } catch (e: Exception) {
        false
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalSettingsScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentNickname by remember { mutableStateOf("健康派卡") }
    var currentPhone by remember { mutableStateOf("138****8527") }
    var currentEmail by remember { mutableStateOf("未绑定") }
    var cacheSize by remember { mutableStateOf("计算中...") }
    var isClearing by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // 通知设置状态
    var pushNotificationEnabled by remember { mutableStateOf(true) }
    var systemMessageEnabled by remember { mutableStateOf(true) }
    
    // 通知设置确认对话框状态
    var showNotificationDialog by remember { mutableStateOf(false) }
    var notificationDialogType by remember { mutableStateOf("") }
    var notificationDialogAction by remember { mutableStateOf("") }
    
    // 获取显示名称的函数
    fun getDisplayName(): String {
        val nickname = UserManager.getNickname(context)
        val username = UserManager.getUsername(context)
        return when {
            !nickname.isNullOrBlank() -> nickname
            !username.isNullOrBlank() -> username
            else -> "健康派卡"
        }
    }
    
    // 获取绑定状态的函数
    fun getPhoneBindingStatus(): String {
        // 优先从UserManager获取登录后返回的手机号
        val userPhone = UserManager.getPhone(context)
        if (!userPhone.isNullOrEmpty()) {
            return formatPhoneNumber(userPhone)
        }
        // 如果UserManager中没有手机号，则显示未绑定
        return "未绑定"
    }
    
    fun getEmailBindingStatus(): String {
        // 优先从UserManager获取登录后返回的邮箱
        val userEmail = UserManager.getEmail(context)
        if (!userEmail.isNullOrEmpty()) {
            return maskEmail(userEmail)
        }
        // 如果UserManager中没有，则从本地绑定记录获取
        val bindedEmail = getBindedEmail(context)
        return if (bindedEmail.isNotEmpty()) maskEmail(bindedEmail) else "未绑定"
    }
    
    // 邮箱掩码函数
    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        
        val username = parts[0]
        val domain = parts[1]
        
        return if (username.length > 3) {
            "${username.substring(0, 2)}***@$domain"
        } else {
            "***@$domain"
        }
    }

    
    // 在组件初始化时获取数据
    LaunchedEffect(Unit) {
        currentNickname = getDisplayName()
        currentPhone = getPhoneBindingStatus()
        currentEmail = getEmailBindingStatus()
        cacheSize = calculateCacheSize(context)
    }
    
    // 使用DisposableEffect来监听页面重新获得焦点
    DisposableEffect(Unit) {
        onDispose {
            // 页面销毁时刷新所有数据，确保下次进入时显示最新数据
            currentNickname = getDisplayName()
            currentPhone = getPhoneBindingStatus()
            currentEmail = getEmailBindingStatus()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // 顶部导航栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = "个人设置",
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )
        
        // 设置内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 账户信息设置
            SettingsSection(
                title = "账户信息",
                items = listOf(
                    SettingsItem("修改昵称", currentNickname),
                    SettingsItem("修改头像", "点击更换头像"), // 修改头像已隐藏
                    SettingsItem("绑定手机", currentPhone),
                    SettingsItem("绑定邮箱", currentEmail)
                ),
                onItemClick = { item ->
                    when (item.title) {
                        "修改昵称" -> {
                            navigationManager.navigateToEditNickname(currentNickname)
                        }
                        "修改头像" -> {
                            navigationManager.navigateToEditAvatar("personal_settings")
                        } // 修改头像功能已隐藏
                        "绑定手机" -> {
                            // 获取当前手机号（未格式化的原始数据），如果没有则传递空字符串
                            val currentPhoneData = UserManager.getPhone(context) ?: ""
                            navigationManager.navigateToBindPhone(currentPhoneData)
                        }
                        "绑定邮箱" -> {
                            // 获取当前邮箱（未格式化的原始数据），如果没有则传递空字符串
                            val currentEmailData = UserManager.getEmail(context) ?: getBindedEmail(context)
                            navigationManager.navigateToBindEmail(currentEmailData)
                        }
                    }
                }
            )
            
            // 通知设置
            SettingsSection(
                title = "通知设置",
                items = listOf(
                    SettingsItem("推送通知", if (pushNotificationEnabled) "已开启" else "已关闭"),
                    SettingsItem("系统消息", if (systemMessageEnabled) "已开启" else "已关闭")
                ),
                onItemClick = { item ->
                    when (item.title) {
                        "推送通知" -> {
                            notificationDialogType = "推送通知"
                            notificationDialogAction = if (pushNotificationEnabled) "关闭" else "开启"
                            showNotificationDialog = true
                        }
                        "系统消息" -> {
                            notificationDialogType = "系统消息"
                            notificationDialogAction = if (systemMessageEnabled) "关闭" else "开启"
                            showNotificationDialog = true
                        }
                    }
                }
            )
            
            // 隐私设置
            // SettingsSection(
            //     title = "隐私设置",
            //     items = listOf(
            //         SettingsItem("运动数据公开", "仅好友可见"),
            //         SettingsItem("在线状态", "显示"),
            //         SettingsItem("允许陌生人添加", "关闭")
            //     ),
            //     onItemClick = null
            // )
            
            // 其他设置
            SettingsSection(
                title = "其他设置",
                items = listOf(
                    SettingsItem("清除缓存", if (isClearing) "清理中..." else cacheSize)
//                    SettingsItem("Draw广告测试", "测试Draw广告功能")
                ),
                onItemClick = { item ->
                    when (item.title) {
                        "清除缓存" -> {
                            if (!isClearing) {
                                isClearing = true
                                scope.launch {
                                    try {
                                        val success = clearCache(context)
                                        if (success) {
                                            ToastUtils.showSuccessToast(context, "缓存清理成功")
                                            // 重新计算缓存大小
                                            cacheSize = calculateCacheSize(context)
                                        } else {
                                            ToastUtils.showErrorToast(context, "缓存清理失败")
                                        }
                                    } catch (e: Exception) {
                                        ToastUtils.showErrorToast(context, "缓存清理出错: ${e.message}")
                                    } finally {
                                        isClearing = false
                                    }
                                }
                            }
                        }
                        "Draw广告测试" -> {
                            navigationManager.navigateToDrawAdTest()
                        }
                    }
                }
            )
            
            // 退出登录区域
            SettingsSection(
                title = "账户管理",
                items = listOf(
                    SettingsItem("退出登录", "退出当前账户")
                ),
                onItemClick = { item ->
                    when (item.title) {
                        "退出登录" -> {
                            showLogoutDialog = true
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
    
    // 退出登录确认对话框
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "退出登录",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("确定要退出当前账户吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
        scope.launch {
                            navigationManager.logout()
                        }
                    }
                ) {
                    Text(
                        text = "确定",
                        color = Color(0xFFE53E3E)
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutDialog = false }
                ) {
                    Text(
                        text = "取消",
                        color = Color(0xFF6B7280)
                    )
                }
            }
        )
     }
     
     // 通知设置确认对话框
     if (showNotificationDialog) {
         AlertDialog(
             onDismissRequest = { showNotificationDialog = false },
             title = {
                 Text(
                     text = "${notificationDialogAction}${notificationDialogType}",
                     fontWeight = FontWeight.Bold
                 )
             },
             text = {
                 Text("确定要${notificationDialogAction}${notificationDialogType}吗？")
             },
             confirmButton = {
                 TextButton(
                     onClick = {
                         showNotificationDialog = false
                         // 执行相应的开启/关闭操作
                         when (notificationDialogType) {
                             "推送通知" -> {
                                 pushNotificationEnabled = notificationDialogAction == "开启"
                                 ToastUtils.showSuccessToast(context, "推送通知已${notificationDialogAction}")
                             }
                             "系统消息" -> {
                                 systemMessageEnabled = notificationDialogAction == "开启"
                                 ToastUtils.showSuccessToast(context, "系统消息已${notificationDialogAction}")
                             }
                         }
                     }
                 ) {
                     Text(
                         text = "确定",
                         color = if (notificationDialogAction == "关闭") Color(0xFFE53E3E) else Color(0xFF10B981)
                     )
                 }
             },
             dismissButton = {
                 TextButton(
                     onClick = { showNotificationDialog = false }
                 ) {
                     Text(
                         text = "取消",
                         color = Color(0xFF6B7280)
                     )
                 }
             }
         )
     }

}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>,
    onItemClick: ((SettingsItem) -> Unit)? = null
) {
    Column {
        // 分组标题
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF6B7280),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        
        // 设置卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 1.dp
            )
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    SettingsItemRow(
                        title = item.title,
                        subtitle = item.subtitle,
                        onClick = { onItemClick?.invoke(item) }
                    )
                    
                    if (index < items.size - 1) {
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

@Composable
fun SettingsItemRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF161823)
            )
            
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "进入$title",
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )
    }
}

data class SettingsItem(
    val title: String,
    val subtitle: String,
    val onClick: (() -> Unit)? = null
)