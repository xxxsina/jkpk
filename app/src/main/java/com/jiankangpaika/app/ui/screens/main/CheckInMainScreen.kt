package com.jiankangpaika.app.ui.screens.main

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jiankangpaika.app.ui.components.common.BottomNavigationBar
import com.jiankangpaika.app.ui.screens.checkin.CheckInScreen
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.utils.UserManager
import androidx.compose.foundation.background

/**
 * 签到页面主屏幕
 * 独立的签到路由，包含签到内容和底部导航栏
 */
@Composable
fun CheckInMainScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF6366F1),
                        Color(0xFF8B5CF6)
                    )
                )
            )
    ) {
        // 主要内容区域
        Box(
            modifier = Modifier.weight(1f)
        ) {
            CheckInScreen(
                navigationManager = navigationManager,
                onNavigateToQuestionList = {
                    navigationManager.navigateToQuestionList()
                }
            )
        }
        
        // 底部导航栏
        BottomNavigationBar(
            selectedIndex = 2,
            onTabSelected = { newIndex ->
                when (newIndex) {
                    0 -> {
                        Log.d("CheckInMainScreen", "🏠 [首页] 导航到首页")
                        navController.navigate("main") {
                            popUpTo("checkin_main") { inclusive = false }
                        }
                    }
                    1 -> {
                        Log.d("CheckInMainScreen", "🏃 [运动] 导航到运动页面")
                        navController.navigate("sport_main") {
                            popUpTo("checkin_main") { inclusive = false }
                        }
                    }
                    2 -> {
                        // 已经在签到页面，不需要导航
                        Log.d("CheckInMainScreen", "📅 [签到] 已在签到页面")
                    }
                    3 -> {
                        // 检查用户登录状态
                        if (UserManager.isLoggedIn(context)) {
                            Log.d("CheckInMainScreen", "👤 [我的] 用户已登录，导航到我的页面")
                            navController.navigate("profile_main") {
                                popUpTo("checkin_main") { inclusive = false }
                            }
                        } else {
                            Log.d("CheckInMainScreen", "🔐 [登录] 用户未登录，跳转到登录页面")
                            navigationManager.navigateToLogin("profile_main")
                        }
                    }
                }
            }
        )
    }
}