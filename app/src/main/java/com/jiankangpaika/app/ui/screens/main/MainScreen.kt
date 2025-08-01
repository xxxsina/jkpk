package com.jiankangpaika.app.ui.screens.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jiankangpaika.app.data.model.SportItem
import com.jiankangpaika.app.ui.components.common.BottomNavigationBar
import com.jiankangpaika.app.ui.screens.home.MainHomeContent
import com.jiankangpaika.app.ui.screens.home.FloatingBottomLayer
import com.jiankangpaika.app.ui.screens.sport.ArticleDetailScreen
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.utils.UserManager

/**
 * 首页主屏幕
 * 独立的首页路由，包含首页内容和底部导航栏
 */
@Composable
fun MainScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    var selectedSport by remember { mutableStateOf<SportItem?>(null) }
    val context = LocalContext.current
    
    // 如果选择了运动项目，显示文章详细页面
    if (selectedSport != null) {
        ArticleDetailScreen(
            title = selectedSport!!.name,
            category = selectedSport!!.category,
            imageRes = selectedSport!!.imageRes,
            content = selectedSport!!.description,
            onBackClick = { selectedSport = null }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)) // 首页 - 浅灰色
    ) {
        // 主要内容区域
        Box(
            modifier = Modifier.weight(1f)
        ) {
            MainHomeContent(
                onSportClick = { sport -> selectedSport = sport }
            )
            
            // 悬浮层 - 位于底部导航栏上方
            FloatingBottomLayer(selectedTabIndex = 0)
        }
        
        // 底部导航栏
        BottomNavigationBar(
            selectedIndex = 0,
            onTabSelected = { newIndex ->
                when (newIndex) {
                    0 -> {
                        // 已经在首页，不需要导航
                        Log.d("MainScreen", "🏠 [首页] 已在首页")
                    }
                    1 -> {
                        Log.d("MainScreen", "🏃 [运动] 导航到运动页面")
                        navController.navigate("sport_main") {
                            popUpTo("main") { inclusive = false }
                        }
                    }
                    2 -> {
                        Log.d("MainScreen", "📅 [签到] 导航到签到页面")
                        navController.navigate("checkin_main") {
                            popUpTo("main") { inclusive = false }
                        }
                    }
                    3 -> {
                        // 检查用户登录状态
                        if (UserManager.isLoggedIn(context)) {
                            Log.d("MainScreen", "👤 [我的] 用户已登录，导航到我的页面")
                            navController.navigate("profile_main") {
                                popUpTo("main") { inclusive = false }
                            }
                        } else {
                            Log.d("MainScreen", "🔐 [登录] 用户未登录，跳转到登录页面")
                            navigationManager.navigateToLogin("profile_main")
                        }
                    }
                }
            }
        )
    }
}