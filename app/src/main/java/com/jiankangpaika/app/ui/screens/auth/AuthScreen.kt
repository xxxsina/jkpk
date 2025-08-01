package com.jiankangpaika.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * 认证页面 - 统一管理登录和注册页面的导航
 */
@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit = {},
    onNavigateToRegister: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(AuthScreenType.PASSWORD_LOGIN) }
    
    when (currentScreen) {
        AuthScreenType.PASSWORD_LOGIN -> {
            LoginScreen(
                onLoginSuccess = onLoginSuccess,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToMobileLogin = { currentScreen = AuthScreenType.MOBILE_LOGIN }
            )
        }
        AuthScreenType.MOBILE_LOGIN -> {
            MobileLoginScreen(
                onLoginSuccess = onLoginSuccess,
                onNavigateToRegister = onNavigateToRegister,
                onNavigateToPasswordLogin = { currentScreen = AuthScreenType.PASSWORD_LOGIN }
            )
        }
    }
}

/**
 * 认证页面类型枚举
 */
enum class AuthScreenType {
    PASSWORD_LOGIN,  // 密码登录
    MOBILE_LOGIN     // 手机短信登录
}