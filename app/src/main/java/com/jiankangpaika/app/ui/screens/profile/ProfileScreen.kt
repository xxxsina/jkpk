package com.jiankangpaika.app.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import kotlin.system.exitProcess
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jiankangpaika.app.R
import com.jiankangpaika.app.data.preferences.CheckInPreferences
import com.jiankangpaika.app.ui.components.CheckInCalendar
import com.jiankangpaika.app.utils.CalendarResult
import com.jiankangpaika.app.utils.CalendarSyncResult
import com.jiankangpaika.app.utils.CheckInManager
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.ui.navigation.NavigationManager
import kotlinx.coroutines.launch
import java.util.Calendar


@Composable
fun ProfileScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    val context = LocalContext.current
    val checkInPreferences = remember { CheckInPreferences(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var displayName by remember { mutableStateOf("健康派卡") }
    var currentAvatarResource by remember { mutableStateOf(R.drawable.logo) }
    var customAvatarBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var hasCustomAvatar by remember { mutableStateOf(false) }
    var avatarUrl by remember { mutableStateOf<String?>(null) }
    var hasNetworkAvatar by remember { mutableStateOf(false) }
    
    // 签到日历相关状态
    val calendar = Calendar.getInstance()
    var currentYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }
    var checkInData by remember { mutableStateOf(checkInPreferences.getMonthCheckInData(currentYear, currentMonth)) }
    var maxCheckInPerDay by remember { mutableStateOf(CheckInManager.getMaxCheckInPerDay()) }
    
    // 积分状态
    var totalScore by remember { mutableStateOf(CheckInManager.getTotalScore(context)) }
    
    // 从UserManager获取显示名称
    fun getDisplayName(): String {
        return UserManager.getNickname(context) ?: "健康派卡"
    }
    
    // 更新日历数据的函数
    fun updateCalendarData(year: Int, month: Int) {
        val currentCalendar = Calendar.getInstance()
        val currentYearNow = currentCalendar.get(Calendar.YEAR)
        val currentMonthNow = currentCalendar.get(Calendar.MONTH) + 1
        
        // 检查是否超过当前月份
        val isCurrentOrPastMonth = year < currentYearNow || (year == currentYearNow && month <= currentMonthNow)
        
        currentYear = year
        currentMonth = month
        
        if (isCurrentOrPastMonth) {
            // 当前月份或过去月份，请求服务器数据
            coroutineScope.launch {
                try {
                    val calendarResult = CheckInManager.refreshCalendarData(context, year, month)
                    when (calendarResult) {
                        is CalendarResult.Success -> {
                            // 更新本地状态
                            maxCheckInPerDay = calendarResult.maxCheckinPerDay
                            // 更新日历数据
                            checkInData = checkInPreferences.getMonthCheckInData(year, month)
                        }
                        is CalendarResult.Error -> {
                            // 即使请求失败，也显示本地数据
                            checkInData = checkInPreferences.getMonthCheckInData(year, month)
                        }
                    }
                } catch (e: Exception) {
                    // 异常时显示本地数据
                    checkInData = checkInPreferences.getMonthCheckInData(year, month)
                }
            }
        } else {
            // 未来月份，不请求接口，直接显示本地数据
            checkInData = checkInPreferences.getMonthCheckInData(year, month)
        }
    }
    
    // 更新头像状态的函数
    fun updateAvatarState() {
        // 首先检查是否有自定义头像
        val hasCustomAvatarValue = com.jiankangpaika.app.ui.screens.profile.hasCustomAvatar(context)
        hasCustomAvatar = hasCustomAvatarValue
        
        if (hasCustomAvatarValue) {
            customAvatarBitmap = com.jiankangpaika.app.ui.screens.profile.getCustomAvatar(context)
            hasNetworkAvatar = false
            avatarUrl = null
        } else {
            // 检查UserManager中是否有头像URL
            val userAvatarUrl = UserManager.getAvatarUrl(context)
            if (!userAvatarUrl.isNullOrEmpty()) {
                if (userAvatarUrl.startsWith("http")) {
                    // 真正的网络头像
                    avatarUrl = userAvatarUrl
                    hasNetworkAvatar = true
                    customAvatarBitmap = null
                } else if (userAvatarUrl.startsWith("android.resource://")) {
                    // 解析android.resource://格式的URL，提取资源名称
                    val resourceName = userAvatarUrl.substringAfterLast("/")
                    val resourceId = when (resourceName) {
                        "avatar_1" -> R.drawable.avatar_1
                        "avatar_2" -> R.drawable.avatar_2
                        "avatar_3" -> R.drawable.avatar_3
                        "avatar_4" -> R.drawable.avatar_4
                        "avatar_5" -> R.drawable.avatar_5
                        "avatar_6" -> R.drawable.avatar_6
                        "avatar_7" -> R.drawable.avatar_7
                        "avatar_8" -> R.drawable.avatar_8
                        else -> R.drawable.logo // 默认使用logo
                    }
                    currentAvatarResource = resourceId
                    customAvatarBitmap = null
                    hasNetworkAvatar = false
                    avatarUrl = null
                } else if (userAvatarUrl.startsWith("avatar_")) {
                    // 处理服务器返回的avatar_{n}格式
                    val resourceId = when (userAvatarUrl) {
                        "avatar_1" -> R.drawable.avatar_1
                        "avatar_2" -> R.drawable.avatar_2
                        "avatar_3" -> R.drawable.avatar_3
                        "avatar_4" -> R.drawable.avatar_4
                        "avatar_5" -> R.drawable.avatar_5
                        "avatar_6" -> R.drawable.avatar_6
                        "avatar_7" -> R.drawable.avatar_7
                        "avatar_8" -> R.drawable.avatar_8
                        else -> R.drawable.logo
                    }
                    currentAvatarResource = resourceId
                    customAvatarBitmap = null
                    hasNetworkAvatar = false
                    avatarUrl = null
                } else {
                    // 其他格式，使用本地预设头像
                    currentAvatarResource = R.drawable.logo
                    customAvatarBitmap = null
                    hasNetworkAvatar = false
                    avatarUrl = null
                }
            } else {
                // 使用本地预设头像
                currentAvatarResource = R.drawable.logo
                customAvatarBitmap = null
                hasNetworkAvatar = false
                avatarUrl = null
            }
        }
    }
    
    // 从本地数据更新所有状态
    suspend fun refreshUserData() {
        // 更新显示名称
        displayName = getDisplayName()
        
        // 更新头像状态
        updateAvatarState()
        
        // 初始化签到日历数据
        try {
            val syncResult = CheckInManager.updateTodayStats(context)
            when (syncResult) {
                is CalendarSyncResult.Success -> {
                    maxCheckInPerDay = syncResult.maxCheckinPerDay
                    totalScore = syncResult.currentScore
                    checkInData = checkInPreferences.getMonthCheckInData(currentYear, currentMonth)
                }
                is CalendarSyncResult.Error -> {
                    totalScore = CheckInManager.getTotalScore(context)
                    checkInData = checkInPreferences.getMonthCheckInData(currentYear, currentMonth)
                }
            }
        } catch (e: Exception) {
            totalScore = CheckInManager.getTotalScore(context)
            checkInData = checkInPreferences.getMonthCheckInData(currentYear, currentMonth)
        }
    }
    
    // 初始化时加载用户数据
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            refreshUserData()
        }
    }
    
    // 监听导航变化，每次进入profile页面时刷新数据
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "profile") {
                coroutineScope.launch {
                    refreshUserData()
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // 浅灰色背景
    ) {
        // 红色背景的头部区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFFEA3323) // 红色背景 EA3323
                )
                .padding(top = 8.dp, bottom = 10.dp)
        ) {
            Column {
                // 顶部区域：用户信息和设置图标
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // 用户信息卡片
                    Box(modifier = Modifier.weight(1f)) {
                        UserProfileHeader(
                            nickname = displayName, 
                            avatarResource = currentAvatarResource,
                            customAvatarBitmap = customAvatarBitmap,
                            hasCustomAvatar = hasCustomAvatar,
                            avatarUrl = avatarUrl,
                            hasNetworkAvatar = hasNetworkAvatar,
                            isRedBackground = true,
                            onAvatarClick = { navigationManager.navigateToEditAvatar("profile") }, // 修改头像
                            onNicknameClick = { navigationManager.navigateToEditNickname(displayName, "profile") } // 编辑昵称
                        )
                    }
                    
                    // 设置图标
                    IconButton(
                        onClick = { navigationManager.navigateToSettings() },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                
//                Spacer(modifier = Modifier.height(10.dp))
                
                // 余额、优惠券、积分统计区域
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    // 余额
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-14).dp)
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "余额",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    
                    // 分隔线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .offset(y = 16.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // 优惠券
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-14).dp)
                    ) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "优惠券",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                    
                    // 分隔线
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .offset(y = 16.dp)
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // 积分
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-14).dp)
                    ) {
                        Text(
                            text = totalScore.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "积分",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // 可滚动的内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 10.dp)
        ) {
            // 签到日历标题
            Text(
                text = "签到日历",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF161823),
                fontSize = 18.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 签到日历
            CheckInCalendar(
                checkInData = checkInData,
                currentYear = currentYear,
                currentMonth = currentMonth,
                maxCheckInPerDay = maxCheckInPerDay,
                onMonthChange = { year: Int, month: Int -> updateCalendarData(year, month) },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 分享APP和退出APP按钮
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // 分享APP按钮
                Button(
                    onClick = {
                        val downloadUrl = ApiConfig.Version.DOWNLOAD_BASE
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("APP下载地址", downloadUrl)
                        clipboardManager.setPrimaryClip(clipData)
                        ToastUtils.showSuccessToast(context, "已复制下载地址，分享给好友吧！")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00BFFF)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "分享APP",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // 退出APP按钮
                Button(
                    onClick = {
                        ToastUtils.showInfoToast(context, "正在退出应用...")
                        // 延迟退出，让用户看到提示
                        coroutineScope.launch {
                            kotlinx.coroutines.delay(800)
                            exitProcess(0)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "退出APP",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // 底部导航栏间距
        }
    }
}

@Composable
fun UserProfileHeader(
    nickname: String = "健康派卡",
    avatarResource: Int = R.drawable.logo,
    customAvatarBitmap: ImageBitmap? = null,
    hasCustomAvatar: Boolean = false,
    avatarUrl: String? = null,
    hasNetworkAvatar: Boolean = false,
    isRedBackground: Boolean = false,
    onAvatarClick: () -> Unit = {},
    onNicknameClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 用户头像
        if (hasCustomAvatar && customAvatarBitmap != null) {
            // 自定义头像（本地图片）
            Image(
                bitmap = customAvatarBitmap,
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() },
                contentScale = ContentScale.Crop
            )
        } else if (hasNetworkAvatar && !avatarUrl.isNullOrEmpty()) {
            // 网络头像（包括默认头像URL）
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() },
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.logo),
                error = painterResource(id = avatarResource)
            )
        } else {
            // 本地预设头像
            Image(
                painter = painterResource(id = avatarResource),
                contentDescription = "用户头像",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .clickable { onAvatarClick() },
                contentScale = ContentScale.Crop
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))

        // 用户信息
        Column(
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // 用户名
            Text(
                text = nickname,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isRedBackground) Color.White else Color(0xFF161823),
                fontSize = 16.sp,
                modifier = Modifier.clickable { onNicknameClick() }
            )
            
            // username显示
            UserManager.getUsername(LocalContext.current)?.let { username ->
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isRedBackground) Color.White.copy(alpha = 0.9f) else Color(0xFF374151),
                    fontSize = 14.sp
                )
            }
        }
    }
}