package com.jiankangpaika.app.ui.screens.checkin

// 移除不需要的导入
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import com.jiankangpaika.app.utils.HtmlUtils

import com.jiankangpaika.app.R
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.data.model.CheckInTask
// 移除不再使用的日历相关导入
// import com.jiankangpaika.app.data.preferences.CheckInPreferences
// import com.jiankangpaika.app.ui.components.CheckInCalendar
import com.jiankangpaika.app.utils.AddScoreResult
import com.jiankangpaika.app.utils.CalendarResult
import com.jiankangpaika.app.utils.CalendarSyncResult
import com.jiankangpaika.app.utils.CheckInManager
import com.jiankangpaika.app.utils.CheckInResult
import com.jiankangpaika.app.utils.ScreenshotUtils
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.data.model.CheckInInitResult
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.data.model.CustomerMessage
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CheckInScreen(
    navigationManager: NavigationManager,
    onNavigateToQuestionList: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    // 根据屏幕宽度和系统字体缩放比例动态计算字体大小
    val screenWidthDp = configuration.screenWidthDp
    val fontScale = configuration.fontScale // 获取系统字体缩放比例
    
    // 基础字体大小（根据屏幕宽度）
    val baseFontSize = when {
        screenWidthDp < 360 -> 18.sp // 小屏幕
        screenWidthDp < 400 -> 20.sp // 中等屏幕
        else -> 22.sp // 大屏幕
    }
    
    // 根据系统字体缩放比例调整字体大小
    val adjustedFontSize = when {
        fontScale <= 1.0f -> baseFontSize // 正常或更小字体
        fontScale <= 1.3f -> (baseFontSize.value * 0.9f).sp // 稍大字体，缩小10%
        fontScale <= 1.45f -> (baseFontSize.value * 0.8f).sp // 大字体，缩小20%
        else -> (baseFontSize.value * 0.7f).sp // 超大字体（老年模式），缩小30%
    }
    
    // 设置最小字体大小限制，确保文字仍然可读
    val minFontSize = 10.sp
    val dynamicFontSize = maxOf(adjustedFontSize.value, minFontSize.value).sp
    // 状态管理
    var totalScore by remember { mutableStateOf(CheckInManager.getTotalScore(context)) }
    var todayCheckInCount by remember { mutableStateOf(CheckInManager.getTodayCheckInCount(context)) }
    var maxCheckInPerDay by remember { mutableStateOf(CheckInManager.getMaxCheckInPerDay()) }
    var isCheckingIn by remember { mutableStateOf(false) } // 防抖状态
    var noticeMessage by remember { mutableStateOf<String?>(null) } // 通知说明消息
    var lastInitRequestTime by remember { mutableStateOf(0L) } // 上次初始化请求时间
    
    // 赚取更多积分相关状态
    var scoreAgainMore by remember { mutableStateOf(0) } // 今日已获得的赚取更多积分∂
    var maxScoreAgainMore by remember { mutableStateOf(0) } // 每日最大赚取更多积分次数
    var todayScoreAgainMore by remember { mutableStateOf(0) } // 今日已赚取更多积分的次数
    var isEarningMorePoints by remember { mutableStateOf(false) } // 防止重复点击赚取更多积分按钮
    
    // 延时显示提示文字的状态
    var showTipText by remember { mutableStateOf(false) }
    
    // 延时1秒显示提示文字
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        showTipText = true
    }
    
    // 最近问题相关状态
    var recentMessage by remember { mutableStateOf<CustomerMessage?>(null) }
    var isLoadingRecentMessage by remember { mutableStateOf(false) }
    var hasUnconfirmedMessage by remember { mutableStateOf(false) }
    
    // 获取最近问题数据的函数
    val loadRecentMessage = {
        if (UserManager.isLoggedIn(context)) {
            coroutineScope.launch {
                try {
                    isLoadingRecentMessage = true
                    val userId = UserManager.getUserId(context)
                    if (!userId.isNullOrEmpty()) {
                        val formData = mapOf("user_id" to userId)
                        val result = NetworkUtils.postJsonWithAuth(
                            context = context,
                            url = ApiConfig.CustomerService.CHECK_IN_MESSAGE,
                            data = formData
                        )
                        
                        when (result) {
                            is NetworkResult.Success -> {
                                val jsonResponse = JSONObject(result.data)
                                val code = jsonResponse.optInt("code", 0)
                                val message = jsonResponse.optString("message", "")

                                Log.d("CheckInScreen", "API完整响应: ${result.data}")
                                Log.d("CheckInScreen", "API code字段: $code, message字段: $message")

                                if (code == 200) {
                                    val dataObject = jsonResponse.optJSONObject("data")
                                    if (dataObject != null) {
                                        hasUnconfirmedMessage = dataObject.optBoolean("has_unconfirmed", false)
                                        val messageData = dataObject.optJSONObject("message")
                                        
                                        Log.d("CheckInScreen", "API响应解析: hasUnconfirmedMessage=$hasUnconfirmedMessage, messageData=${messageData != null}")
                                        
                                        if (hasUnconfirmedMessage && messageData != null) {
                                            recentMessage = CustomerMessage(
                                                id = messageData.optInt("id", 0),
                                                userId = messageData.optInt("user_id", 0),
                                                status = messageData.optString("status", ""),
                                                looked = messageData.optInt("looked", 0),
                                                realname = messageData.optString("realname", ""),
                                                mobile = messageData.optString("mobile", ""),
                                                problem = messageData.optString("problem", ""),
                                                answer = messageData.optString("answer", ""),
                                                image = messageData.optString("image", ""),
                                                video = messageData.optString("video", ""),
                                                answerImage = messageData.optString("answer_image", ""),
                                                answerVideo = messageData.optString("answer_video", ""),
                                                isOvercome = messageData.optInt("is_overcome", 0),
                                                createTime = messageData.optLong("createtime", 0),
                                                updateTime = messageData.optLong("updatetime", 0),
                                                createTimeFormatted = messageData.optString("createtime_formatted", ""),
                                                updateTimeFormatted = messageData.optString("updatetime_formatted", "")
                                            )
                                            Log.d("CheckInScreen", "成功创建recentMessage: ${recentMessage?.problem}")
                                        } else {
                                            recentMessage = null
                                            Log.d("CheckInScreen", "未创建recentMessage: hasUnconfirmedMessage=$hasUnconfirmedMessage, messageData存在=${messageData != null}")
                                        }
                                    } else {
                                        Log.e("CheckInScreen", "API响应中缺少data字段")
                                        hasUnconfirmedMessage = false
                                        recentMessage = null
                                    }
                                } else {
                                    Log.e("CheckInScreen", "获取最近问题失败: $message")
                                    hasUnconfirmedMessage = false
                                    recentMessage = null
                                }
                            }
                            is NetworkResult.Error -> {
                                Log.e("CheckInScreen", "网络请求失败: ${result.message}")
                            }
                            is NetworkResult.Exception -> {
                                Log.e("CheckInScreen", "请求异常: ${result.exception.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CheckInScreen", "获取最近问题异常: ${e.message}")
                } finally {
                    isLoadingRecentMessage = false
                }
            }
        } else {
            // 用户未登录，清空数据
            recentMessage = null
            hasUnconfirmedMessage = false
        }
    }
    
    // 页面加载时获取最近问题数据
    LaunchedEffect(UserManager.isLoggedIn(context)) {
        loadRecentMessage()
    }
    
    // 更新签到初始化数据的函数（带防抖）
    val updateCheckInInitData = {
        val currentTime = System.currentTimeMillis()
        val debounceInterval = 2000L // 2秒防抖间隔
        
        if (currentTime - lastInitRequestTime >= debounceInterval) {
            lastInitRequestTime = currentTime
            coroutineScope.launch {
                try {
                    // 根据用户登录状态传递不同的user_id
                    val userIdOverride = if (UserManager.isLoggedIn(context)) null else "0"
                    val initResult = CheckInManager.refreshCheckInInitData(context, userIdOverride)
                    when (initResult) {
                        is CheckInInitResult.Success -> {
                            // 更新本地状态
                            todayCheckInCount = initResult.todayCheckinCount
                            maxCheckInPerDay = initResult.maxCheckinPerDay
                            scoreAgainMore = initResult.scoreAgainMore
                            maxScoreAgainMore = initResult.maxScoreAgainMore
                            todayScoreAgainMore = initResult.todayScoreAgainMore
                            noticeMessage = initResult.noticeMessage
                            Log.d("CheckInScreen", "[签到初始化] 获取数据成功: 今日签到${initResult.todayCheckinCount}次, 上限${initResult.maxCheckinPerDay}次, 赚取更多积分${initResult.scoreAgainMore}, 上限${initResult.maxScoreAgainMore}次, 今日已赚取${initResult.todayScoreAgainMore}次")
                        }
                        is CheckInInitResult.Error -> {
                            ToastUtils.showErrorToast(context, "获取签到数据失败: ${initResult.message}")
                            Log.e("CheckInScreen", "[签到初始化] 获取数据失败: ${initResult.message}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CheckInScreen", "请求签到初始化数据异常: ${e.message}")
                }
            }
        } else {
            Log.d("CheckInScreen", "[签到初始化] 防抖拦截，距离上次请求时间过短")
        }
    }
    // 签到处理函数
    fun handleCheckIn() {
        // 防抖检查
        if (isCheckingIn) {
            return
        }
        
        coroutineScope.launch {
            try {
                isCheckingIn = true // 开始签到，设置防抖状态
                
                // 检查用户是否已登录
                if (!UserManager.isLoggedIn(context)) {
                    isCheckingIn = false
                    navigationManager.navigateToLogin("checkin")
                    return@launch
                }
                
                // 检查是否可以签到
                if (!CheckInManager.canCheckInToday(context)) {
                    isCheckingIn = false
                    ToastUtils.showErrorToast(context, "今日签到次数已达上限(${maxCheckInPerDay}次)")
                    return@launch
                }

                val activity = context as? Activity
                if (activity != null) {
                    val adManager = UnifiedAdManager.getInstance()

                    // 先加载激励视频广告
                    adManager.loadRewardVideoAd(context) { loadSuccess, loadMessage ->
                        if (loadSuccess) {
                            // 加载成功后展示激励视频广告
                            adManager.showRewardVideoAd(activity) { showSuccess, hasReward, showMessage ->
                                if (showSuccess && hasReward) {
                                    // 用户观看完激励视频并获得奖励后才进行签到
                                    coroutineScope.launch {
                                        val result = CheckInManager.performCheckIn(context)
                                        when (result) {
                                            is CheckInResult.Success -> {
                                                // 更新本地状态 - 修复：使用API返回的new_score
                                                todayCheckInCount = result.todayCount
                                                totalScore = result.newScore
                                                maxCheckInPerDay = result.maxCount
                                                // 更新签到初始化数据
                                                updateCheckInInitData()
                                                ToastUtils.showSuccessToast(context, "签到成功！获得${result.scoreEarned}积分，今日已签到${result.todayCount}次")
                                            }
                                            is CheckInResult.Error -> {
                                                ToastUtils.showErrorToast(context, result.message)
                                            }
                                        }
                                        isCheckingIn = false // 签到完成，重置状态
                                    }
                                } else if (showSuccess && !hasReward) {
                                    // 用户观看了广告但没有获得奖励（可能是跳过或其他原因）
                                    isCheckingIn = false
                                    ToastUtils.showTopToast(context, "请观看完整广告以获得签到奖励")
                                } else {
                                    // 广告展示失败
                                    isCheckingIn = false
                                    ToastUtils.showErrorToast(context, "签到失败: $showMessage")
                                }
                            }
                        } else {
                            isCheckingIn = false
                            ToastUtils.showErrorToast(context, "加载广告失败，请稍后再试！")
                        }
                    }
                } else {
                    isCheckingIn = false
                    ToastUtils.showErrorToast(context, "系统加载失败，请重新启动！")
                }
            } catch (e: Exception) {
                isCheckingIn = false
                ToastUtils.showErrorToast(context, "签到失败：${e.message}")
            }
        }
    }
    
    // 赚取更多积分处理函数
    val handleEarnMorePoints = handleEarnMorePoints@{
        // 防重复点击检查
        if (isEarningMorePoints) {
            return@handleEarnMorePoints
        }
        
        coroutineScope.launch {
            try {
                isEarningMorePoints = true // 开始处理，设置防抖状态
                
                // 1. 验证是否登录，没有登录跳转到登录
                if (!UserManager.isLoggedIn(context)) {
                    isEarningMorePoints = false
                    navigationManager.navigateToLogin("checkin")
                    return@launch
                }
                
                // 2. 检查今日签到次数是否已达上限
                if (todayCheckInCount < maxCheckInPerDay) {
                    isEarningMorePoints = false
                    ToastUtils.showErrorToast(context, "请先完成今日签到任务")
                    return@launch
                }
                
                // 3. 检查赚取更多积分次数是否已达上限
                if (todayScoreAgainMore >= maxScoreAgainMore) {
                    isEarningMorePoints = false
                    ToastUtils.showErrorToast(context, "今日积分赚取已达上限，明日再来")
                    return@launch
                }

                val activity = context as? Activity
                if (activity != null) {
                    val adManager = UnifiedAdManager.getInstance()

                    // 2. 载入激励广告，全屏展示
                    adManager.loadRewardVideoAd(context) { loadSuccess, loadMessage ->
                        if (loadSuccess) {
                            // 加载成功后展示激励视频广告
                            adManager.showRewardVideoAd(activity) { showSuccess, hasReward, showMessage ->
                                if (showSuccess && hasReward) {
                                    // 3. 视频播放完成后，调用ADD_SCORE接口增加积分
                                    coroutineScope.launch {
                                        val addScoreResult = CheckInManager.addScore(context, "score_again_more")
                                        when (addScoreResult) {
                                            is AddScoreResult.Success -> {
                                                // 更新本地总分和赚取更多积分次数
                                                totalScore = addScoreResult.newTotalScore
                                                todayScoreAgainMore += 1
                                                // 更新签到初始化数据以同步最新状态
                                                updateCheckInInitData()
                                                ToastUtils.showSuccessToast(context, "观看广告成功！获得${addScoreResult.scoreAdded}积分")
                                            }
                                            is AddScoreResult.Error -> {
                                                ToastUtils.showErrorToast(context, "积分增加失败: ${addScoreResult.message}")
                                            }
                                        }
                                        isEarningMorePoints = false // 处理完成，重置防抖状态
                                    }
                                } else if (showSuccess && !hasReward) {
                                    // 用户观看了广告但没有获得奖励（可能是跳过或其他原因）
                                    isEarningMorePoints = false
                                    ToastUtils.showTopToast(context, "请观看完整广告以获得积分奖励")
                                } else {
                                    // 广告展示失败
                                    isEarningMorePoints = false
                                    ToastUtils.showErrorToast(context, "广告展示失败: $showMessage")
                                }
                            }
                        } else {
                            isEarningMorePoints = false
                            ToastUtils.showErrorToast(context, "加载广告失败，请稍后再试！")
                        }
                    }
                } else {
                    isEarningMorePoints = false
                    ToastUtils.showErrorToast(context, "系统加载失败，请重新启动！")
                }
            } catch (e: Exception) {
                isEarningMorePoints = false
                ToastUtils.showErrorToast(context, "赚取积分失败：${e.message}")
            }
        }
    }
    
    // 初始化签到数据
    LaunchedEffect(Unit) {
        // 无论用户是否登录，都获取签到初始化数据
        updateCheckInInitData()
    }
    
    // 监听用户登录状态变化，登录后同步数据
    LaunchedEffect(UserManager.isLoggedIn(context)) {
        // 无论用户是否登录，都获取签到初始化数据
        updateCheckInInitData()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // 浅灰色背景
            .verticalScroll(rememberScrollState())
    ) {
        // 主要内容区域
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题行：每日签到活动和签到领积分
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
            ) {
                // 背景图片
                androidx.compose.foundation.Image(
                    painter = painterResource(id = R.drawable.qiandao_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.TopStart
                )
            }
            
            // 其他内容区域添加水平内边距
            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                // 当前日期
                val currentDate = remember {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2A2A2C),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // 签到统计按钮
                Button(
                    onClick = { handleCheckIn() },
                    enabled = !isCheckingIn, // 防抖控制
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isCheckingIn -> Color(0xFFED5144) // 签到中显示灰色
                            todayCheckInCount >= maxCheckInPerDay -> Color(0xFFCACACE) // 已完成显示浅灰色
                            else -> Color(0xFFED5144) // 正常状态显示红色
                        },
                        disabledContainerColor = Color(0xFFED5144) // 禁用状态颜色
                    ),
                    contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp)
                ) {
                    // 签到信息
                    Text(
                        text = when {
                            isCheckingIn -> "签到中..."
                            todayCheckInCount >= maxCheckInPerDay -> "今日签到已完成，明日再来吧！"
                            else -> "签到+10积分(需完成:$maxCheckInPerDay,已完成:$todayCheckInCount)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontSize = dynamicFontSize, // 使用动态字体大小
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible, // 允许文字可见，配合动态字体大小
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp) // 添加水平内边距，给文字更多空间
                    )
                }

                // 添加绿色提示文字（延时1秒显示，带渐进效果）
                AnimatedVisibility(
                    visible = showTipText && !(todayCheckInCount >= maxCheckInPerDay && todayScoreAgainMore >= maxScoreAgainMore),
                    enter = fadeIn(animationSpec = tween(1000)) + slideInVertically(
                        animationSpec = tween(1000),
                        initialOffsetY = { it / 2 }
                    )
                ) {
                    Text(
                        text = if (todayCheckInCount >= maxCheckInPerDay) {
                            "已签到完成，继续赚取更多积分吧"
                        } else {
                            "已加载完成，赶紧签到领取积分吧"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (todayCheckInCount >= maxCheckInPerDay) {
                            Color(0xFFED5144) // 红色
                        } else {
                            Color(0xFF10B981) // 绿色
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold, // 加粗
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 每日任务卡片
//                Card(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .clickable { navigationManager.navigateToDailyTasks() },
//                    shape = RoundedCornerShape(12.dp),
//                    colors = CardDefaults.cardColors(containerColor = Color.White),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp)
//                    ) {
//                        // 居中显示的每日任务内容
//                        Row(
//                            modifier = Modifier.align(Alignment.Center),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Icon(
//                                imageVector = Icons.Filled.CalendarToday,
//                                contentDescription = "每日任务",
//                                tint = Color(0xFFFF6B6B),
//                                modifier = Modifier.size(24.dp)
//                            )
//                            Spacer(modifier = Modifier.width(12.dp))
//                            Text(
//                                text = "每日任务",
//                                style = MaterialTheme.typography.bodyLarge,
//                                fontWeight = FontWeight.Medium,
//                                color = Color(0xFF2A2A2C),
//                                fontSize = 16.sp
//                            )
//                        }
//                        // 右侧箭头
//                        Icon(
//                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
//                            contentDescription = "前往",
//                            tint = Color(0xFF9CA3AF),
//                            modifier = Modifier
//                                .size(20.dp)
//                                .align(Alignment.CenterEnd)
//                        )
//                    }
//                }

//                Spacer(modifier = Modifier.height(16.dp))

                // 赚取更多积分卡片组
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 左侧卡片
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isEarningMorePoints) { handleEarnMorePoints() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors( 
                            containerColor = if (isEarningMorePoints) Color(0xFFF0F0F0) else Color.White 
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                 imageVector = Icons.Filled.EmojiEvents,
                                 contentDescription = "赚取积分",
                                 tint = if (isEarningMorePoints) Color(0xFFCCCCCC) else Color(0xFF10B981),
                                 modifier = Modifier.size(30.dp)
                             )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (isEarningMorePoints) "加载中..." else "赚取更多积分",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isEarningMorePoints) Color(0xFF999999) else Color(0xFF2A2A2C),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 右侧卡片
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = !isEarningMorePoints) { handleEarnMorePoints() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors( 
                            containerColor = if (isEarningMorePoints) Color(0xFFF0F0F0) else Color.White 
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                 imageVector = Icons.Filled.EmojiEvents,
                                 contentDescription = "赚取积分",
                                 tint = if (isEarningMorePoints) Color(0xFFCCCCCC) else Color(0xFFFF6B6B),
                                 modifier = Modifier.size(30.dp)
                             )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = if (isEarningMorePoints) "加载中..." else "赚取更多积分",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (isEarningMorePoints) Color(0xFF999999) else Color(0xFF2A2A2C),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 签到有问题点这里卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToQuestionList() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF6B6B),
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "签到有问题点这里",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2A2A2C),
                                fontSize = 16.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "前往",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 联系客服卡片
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 检查用户是否已登录
                            if (UserManager.isLoggedIn(context)) {
                                navigationManager.navigateToCustomerService()
                            } else {
                                // 未登录则跳转到登录页面，传递表单页面标识
                                navigationManager.navigateToLogin("customer_service_form")
                            }
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                 imageVector = Icons.Default.SupportAgent,
                                 contentDescription = "联系客服",
                                 tint = Color(0xFF10B981),
                                 modifier = Modifier.size(24.dp)
                             )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "联系客服",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2A2A2C),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "遇到问题？点击联系我们",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "前往",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 您最近的问题卡片
        val isLoggedIn = UserManager.isLoggedIn(context)
        Log.d("CheckInScreen", "UI显示条件检查: isLoggedIn=$isLoggedIn, hasUnconfirmedMessage=$hasUnconfirmedMessage, recentMessage=${recentMessage != null}")
        if (isLoggedIn && hasUnconfirmedMessage && recentMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // 顶部标题行
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigationManager.navigateToCustomerMessageList()
                                    }
                            ) {
                                Text(
                                    text = "您最近的问题",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2A2A2C),
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.Center)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "查看更多",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.CenterEnd)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 问题内容
                            Column {
                                Text(
                                    text = "您的问题：",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = recentMessage!!.problem,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2A2A2C),
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "客服回复：",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280),
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (recentMessage!!.answer.isNotEmpty()) {
                                        recentMessage!!.answer
                                    } else {
                                        "客服会尽快回复，请耐心等待..."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (recentMessage!!.answer.isNotEmpty()) {
                                        Color(0xFF2A2A2C)
                                    } else {
                                        Color(0xFF9CA3AF)
                                    },
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 底部查看详情按钮
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navigationManager.navigateToCustomerMessageDetail(recentMessage!!)
                                    },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "查看详细回复",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF10B981),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "查看详情",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 通知说明 - 单独展示在最下面
                if (!noticeMessage.isNullOrEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 图标部分 - 占1份
                             Box(
                                 modifier = Modifier.weight(0.1f),
                                 contentAlignment = Alignment.CenterStart
                             ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "喇叭",
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
//                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // 文本部分 - 占9份
                            Column(
                                modifier = Modifier.weight(0.9f)
                            ) {
                                Text(
                                    text = HtmlUtils.htmlToAnnotatedString(noticeMessage ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFD97706),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp)) // 底部导航栏间距
            }
        }
    }
}