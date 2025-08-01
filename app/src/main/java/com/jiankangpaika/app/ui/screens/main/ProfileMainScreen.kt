package com.jiankangpaika.app.ui.screens.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jiankangpaika.app.ui.components.common.BottomNavigationBar
import com.jiankangpaika.app.ui.screens.profile.ProfileScreen
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.utils.UserManager

/**
 * 我的页面主屏幕
 * 独立的我的页面路由，包含个人信息内容和底部导航栏
 */
@Composable
fun ProfileMainScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White) // 我的页面 - 白色
    ) {
        // 主要内容区域
        Box(
            modifier = Modifier.weight(1f)
        ) {
            ProfileScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        
        // 底部导航栏
        BottomNavigationBar(
            selectedIndex = 3,
            onTabSelected = { newIndex ->
                when (newIndex) {
                    0 -> {
                        Log.d("ProfileMainScreen", "🏠 [首页] 导航到首页")
                        navController.navigate("main") {
                            popUpTo("profile_main") { inclusive = false }
                        }
                    }
                    1 -> {
                        Log.d("ProfileMainScreen", "🏃 [运动] 导航到运动页面")
                        navController.navigate("sport_main") {
                            popUpTo("profile_main") { inclusive = false }
                        }
                    }
                    2 -> {
                        Log.d("ProfileMainScreen", "📅 [签到] 导航到签到页面")
                        navController.navigate("checkin_main") {
                            popUpTo("profile_main") { inclusive = false }
                        }
                    }
                    3 -> {
                        // 已经在我的页面，不需要导航
                        Log.d("ProfileMainScreen", "👤 [我的] 已在我的页面")
                    }
                }
            }
        )
    }
}