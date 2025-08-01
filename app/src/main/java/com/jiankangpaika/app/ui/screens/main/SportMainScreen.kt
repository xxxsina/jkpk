package com.jiankangpaika.app.ui.screens.main

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jiankangpaika.app.ui.components.common.BottomNavigationBar
import com.jiankangpaika.app.ui.screens.sport.SportScreen
import com.jiankangpaika.app.ui.screens.sport.ArticleDetailScreen
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.data.model.SportItem

/**
 * 运动页面主屏幕
 * 独立的运动路由，包含运动内容和底部导航栏
 */
@Composable
fun SportMainScreen(
    navController: NavController,
    navigationManager: NavigationManager
) {
    val context = LocalContext.current
    var selectedSport by remember { mutableStateOf<SportItem?>(null) }
    
    // 如果选择了文章，显示文章详细页面
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
            .background(Color.White) // 运动页面 - 白色
    ) {
        // 主要内容区域
        Box(
            modifier = Modifier.weight(1f)
        ) {
            SportScreen(
                onArticleClick = { article -> 
                    // 将ArticleItem转换为SportItem以兼容现有的详情页面
                    val sportItem = SportItem(
                        id = article.id.toString(),
                        name = article.title,
                        category = article.type,
                        imageRes = article.cover_image,
                        description = article.content
                    )
                    selectedSport = sportItem
                }
            )
        }
        
        // 底部导航栏
        BottomNavigationBar(
            selectedIndex = 1,
            onTabSelected = { newIndex ->
                when (newIndex) {
                    0 -> {
                        Log.d("SportMainScreen", "🏠 [首页] 导航到首页")
                        navController.navigate("main") {
                            popUpTo("sport_main") { inclusive = false }
                        }
                    }
                    1 -> {
                        // 已经在运动页面，不需要导航
                        Log.d("SportMainScreen", "🏃 [运动] 已在运动页面")
                    }
                    2 -> {
                        Log.d("SportMainScreen", "📅 [签到] 导航到签到页面")
                        navController.navigate("checkin_main") {
                            popUpTo("sport_main") { inclusive = false }
                        }
                    }
                    3 -> {
                        // 检查用户登录状态
                        if (UserManager.isLoggedIn(context)) {
                            Log.d("SportMainScreen", "👤 [我的] 用户已登录，导航到我的页面")
                            navController.navigate("profile_main") {
                                popUpTo("sport_main") { inclusive = false }
                            }
                        } else {
                            Log.d("SportMainScreen", "🔐 [登录] 用户未登录，跳转到登录页面")
                            navigationManager.navigateToLogin("profile_main")
                        }
                    }
                }
            }
        )
    }
}