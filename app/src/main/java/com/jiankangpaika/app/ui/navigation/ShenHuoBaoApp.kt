package com.jiankangpaika.app.ui.navigation

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.net.URLDecoder
import java.net.URLEncoder
import android.net.Uri
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ui.screens.home.HomeScreen

import com.jiankangpaika.app.ui.screens.profile.ProfileScreen
import com.jiankangpaika.app.ui.screens.main.MainScreen
import com.jiankangpaika.app.ui.screens.main.SportMainScreen
import com.jiankangpaika.app.ui.screens.main.CheckInMainScreen
import com.jiankangpaika.app.ui.screens.main.ProfileMainScreen
import com.jiankangpaika.app.ui.screens.profile.PersonalSettingsScreen
import com.jiankangpaika.app.ui.screens.profile.SettingsScreen
import com.jiankangpaika.app.ui.screens.checkin.CheckInScreen
import com.jiankangpaika.app.ui.screens.auth.LoginScreen
import com.jiankangpaika.app.ui.screens.auth.MobileLoginScreen
import com.jiankangpaika.app.ui.screens.auth.RegisterScreen
import com.jiankangpaika.app.ui.screens.info.ContactUsScreen
import com.jiankangpaika.app.ui.screens.info.PrivacyPolicyScreen
import com.jiankangpaika.app.ui.screens.info.UserAgreementScreen
import com.jiankangpaika.app.ui.screens.customer.CustomerServiceFormScreen
import com.jiankangpaika.app.ui.screens.customer.CustomerMessageListScreen
import com.jiankangpaika.app.ui.screens.customer.CustomerMessageDetailScreen
import com.jiankangpaika.app.ui.screens.question.QuestionListScreen
import com.jiankangpaika.app.ui.screens.profile.EditNicknameScreen
import com.jiankangpaika.app.ui.screens.profile.EditAvatarScreen
import com.jiankangpaika.app.ui.screens.profile.BindPhoneScreen
import com.jiankangpaika.app.ui.screens.profile.BindEmailScreen
import com.jiankangpaika.app.ui.screens.ad.DrawAdScreen
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.ui.components.WebViewScreen

@Composable
fun ShenHuoBaoApp() {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    
    // 创建统一的导航管理器
    val navigationManager = rememberNavigationManager(
        navController = navController,
        coroutineScope = coroutineScope
    )
    
    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        // 新的独立路由架构
        composable("main") {
            MainScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        
        composable("sport_main") {
            SportMainScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        
        composable("checkin_main") {
            CheckInMainScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        
        composable("profile_main") {
            ProfileMainScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        

        
        composable("profile") {
            ProfileScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        
        composable("settings") {
            SettingsScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }
        
        composable("personal_settings") {
            PersonalSettingsScreen(
                navController = navController,
                navigationManager = navigationManager
            )
        }

        composable("sport") {
            com.jiankangpaika.app.ui.screens.task.DailyTaskListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                navigationManager = navigationManager
            )
        }
        
        composable("daily_task_list") {
            com.jiankangpaika.app.ui.screens.task.DailyTaskListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                navigationManager = navigationManager
            )
        }
        
        composable("checkin") {
            CheckInScreen(
                navigationManager = navigationManager,
                onNavigateToQuestionList = {
                    navigationManager.navigateToQuestionList()
                }
            )
        }
        
        composable(
            "login?returnTo={returnTo}",
            arguments = listOf(
                navArgument("returnTo") { 
                    type = NavType.StringType
                    defaultValue = "profile_main"
                }
            )
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: "profile_main"
            LoginScreen(
                onLoginSuccess = {
                    if (returnTo == "checkin") {
                        navController.navigate("checkin_main") {
                            popUpTo(0) { inclusive = false }
                        }
                    } else if (returnTo == "profile_main") {
                        navController.navigate("profile_main") {
                            popUpTo("main") { inclusive = false }
                        }
                    } else {
                        navController.navigate(returnTo) {
                            popUpTo(0) { inclusive = false }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register?username=&password=&confirmPassword=&agreeToTerms=false")
                },
                onNavigateToMobileLogin = {
                    navController.navigate("mobile_login?returnTo=$returnTo")
                }
            )
        }
        
        // 兼容不带参数的login路由调用，重定向到mobile_login
        composable("login") {
            LaunchedEffect(Unit) {
                navController.navigate("mobile_login?returnTo=profile_main") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
        
        composable(
            "mobile_login?returnTo={returnTo}",
            arguments = listOf(
                navArgument("returnTo") { 
                    type = NavType.StringType
                    defaultValue = "profile_main"
                }
            )
        ) { backStackEntry ->
            val returnTo = backStackEntry.arguments?.getString("returnTo") ?: "profile_main"
            MobileLoginScreen(
                onLoginSuccess = {
                    if (returnTo == "checkin") {
                        navController.navigate("checkin_main") {
                            popUpTo(0) { inclusive = false }
                        }
                    } else if (returnTo == "profile_main") {
                        navController.navigate("profile_main") {
                            popUpTo("main") { inclusive = false }
                        }
                    } else {
                        navController.navigate(returnTo) {
                            popUpTo(0) { inclusive = false }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register?username=&password=&confirmPassword=&agreeToTerms=false")
                },
                onNavigateToPasswordLogin = {
                    navController.navigate("login?returnTo=$returnTo") {
                        popUpTo("mobile_login") { inclusive = true }
                    }
                }
            )
        }
        
        // 兼容不带参数的mobile_login路由调用，重定向到带参数的路由
        composable("mobile_login") {
            LaunchedEffect(Unit) {
                navController.navigate("mobile_login?returnTo=profile_main") {
                    popUpTo("mobile_login") { inclusive = true }
                }
            }
        }
        
        composable(
            "register?username={username}&password={password}&confirmPassword={confirmPassword}&agreeToTerms={agreeToTerms}",
            arguments = listOf(
                navArgument("username") { defaultValue = "" },
                navArgument("password") { defaultValue = "" },
                navArgument("confirmPassword") { defaultValue = "" },
                navArgument("agreeToTerms") { defaultValue = false }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val confirmPassword = backStackEntry.arguments?.getString("confirmPassword") ?: ""
            val agreeToTerms = backStackEntry.arguments?.getBoolean("agreeToTerms") ?: false
            
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("profile_main") {
                        popUpTo("main") { inclusive = false }
                    }
                },
                onNavigateToUserAgreement = { u, p, cp, agree ->
                    navController.navigate("user_agreement_from_register?username=${Uri.encode(u)}&password=${Uri.encode(p)}&confirmPassword=${Uri.encode(cp)}&agreeToTerms=$agree")
                },
                onBackToLogin = {
                    navController.popBackStack()
                },
                initialUsername = username,
                initialPassword = password,
                initialConfirmPassword = confirmPassword,
                initialAgreeToTerms = agreeToTerms
            )
        }
        
        composable("privacy_policy") {
            PrivacyPolicyScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("user_agreement") {
            UserAgreementScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            "user_agreement_from_register?username={username}&password={password}&confirmPassword={confirmPassword}&agreeToTerms={agreeToTerms}",
            arguments = listOf(
                navArgument("username") { defaultValue = "" },
                navArgument("password") { defaultValue = "" },
                navArgument("confirmPassword") { defaultValue = "" },
                navArgument("agreeToTerms") { defaultValue = false }
            )
        ) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: ""
            val password = backStackEntry.arguments?.getString("password") ?: ""
            val confirmPassword = backStackEntry.arguments?.getString("confirmPassword") ?: ""
            val agreeToTerms = backStackEntry.arguments?.getBoolean("agreeToTerms") ?: false
            
            UserAgreementScreen(
                 onBackClick = {
                     navController.navigate("register?username=${URLEncoder.encode(username, "UTF-8")}&password=${URLEncoder.encode(password, "UTF-8")}&confirmPassword=${URLEncoder.encode(confirmPassword, "UTF-8")}&agreeToTerms=true") {
                         popUpTo("register") { inclusive = true }
                     }
                 }
             )
        }
        
        composable("contact_us") {
            ContactUsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onNavigateToCustomerForm = {
                    navigationManager.navigateToCustomerService()
                },
                onNavigateToLogin = {
                    navController.navigate("mobile_login?returnTo=customer_service_form")
                }
            )
        }
        
        composable("customer_service_form") {
            CustomerServiceFormScreen(
                onNavigateBack = {
                    // 检查上一个页面是否是main相关路由
                    val previousEntry = navController.previousBackStackEntry
                    val previousRoute = previousEntry?.destination?.route
                    Log.i("ShenHuoBaoApp", "previousRoute = $previousRoute")
                    if (previousRoute != null && previousRoute.contains("main")) {
                        // 如果上一个页面是main页面（比如从登录直接跳转过来），导航到签到页面
                        navController.navigate("checkin_main")
                    } else if(previousRoute != null && previousRoute == "contact_us") {
                        navController.popBackStack()
                    } else {
                        // 如果没有main页面（比如从登录直接跳转过来），导航到签到页面
                        navController.navigate("checkin_main") {
                            popUpTo(0) { inclusive = false }
                        }
                    }
                },
                onNavigateToMessageList = {
                    navigationManager.navigateToCustomerMessageList()
                }
            )
        }
        
        composable("customer_message_list") {
            CustomerMessageListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { message ->
                    // 将消息数据序列化为JSON字符串传递
                    try {
                        val messageJson = Json.encodeToString(
                            com.jiankangpaika.app.data.model.CustomerMessage.serializer(),
                            message
                        )
                        val encodedMessage = java.net.URLEncoder.encode(messageJson, "UTF-8")
                        navController.navigate("customer_message_detail/$encodedMessage")
                    } catch (e: Exception) {
                        Log.e("ShenHuoBaoApp", "序列化消息数据失败: ${e.message}", e)
                    }
                }
            )
        }
        
        composable(
            "customer_message_detail/{messageData}",
            arguments = listOf(navArgument("messageData") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedMessageData = backStackEntry.arguments?.getString("messageData") ?: ""
            
            // 解析消息数据
            val message = remember(encodedMessageData) {
                try {
                    val decodedMessageData = java.net.URLDecoder.decode(encodedMessageData, "UTF-8")
                    Json.decodeFromString(
                        com.jiankangpaika.app.data.model.CustomerMessage.serializer(),
                        decodedMessageData
                    )
                } catch (e: Exception) {
                    Log.e("ShenHuoBaoApp", "解析消息数据失败: ${e.message}", e)
                    null
                }
            }
            
            if (message != null) {
                CustomerMessageDetailScreen(
                    message = message,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                // 如果解析失败，返回列表页面
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
        
        // 兼容旧版本的edit_nickname路由（无source参数）
        composable("edit_nickname") {
            val context = LocalContext.current
            // 获取当前昵称，优先从UserManager获取，如果没有则使用默认值
            val currentNickname = UserManager.getNickname(context) ?: UserManager.getUsername(context) ?: "健康派卡"
            Log.i("ShenHuoBaoApp", "currentNickname = $currentNickname")
            EditNicknameScreen(
                currentNickname = currentNickname,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNicknameSaved = { _ ->
                    // 昵称保存成功，不执行导航，由onNavigateBack处理
                }
            )
        }
        
        // 新版本的edit_nickname路由（带source参数）
        composable("edit_nickname/{source}") { backStackEntry ->
            val context = LocalContext.current
            val source = backStackEntry.arguments?.getString("source") ?: "personal_settings"
            // 获取当前昵称，优先从UserManager获取，如果没有则使用默认值
            val currentNickname = UserManager.getNickname(context) ?: UserManager.getUsername(context) ?: "健康派卡"
            EditNicknameScreen(
                currentNickname = currentNickname,
                onNavigateBack = {
                    // 根据来源页面进行正确的返回导航
                    when (source) {
                        "profile" -> {
                            // 从ProfileScreen进入编辑昵称，返回到profile_main页面
                            navController.navigate("profile_main") {
                                popUpTo("profile_main") { inclusive = true }
                            }
                        }
                        "personal_settings" -> {
                            navController.navigate("personal_settings") {
                                popUpTo("personal_settings") { inclusive = false }
                            }
                        }
                        else -> {
                            navController.popBackStack()
                        }
                    }
                },
                onNicknameSaved = { _ ->
                    // 昵称保存成功，不执行导航，由onNavigateBack处理
                }
            )
        }
        
        composable("edit_avatar/{source}") { backStackEntry ->
            val source = backStackEntry.arguments?.getString("source") ?: "home"
            EditAvatarScreen(
                onNavigateBack = {
                    when (source) {
                        "profile" -> {
                            navController.navigate("profile_main") {
                                popUpTo("profile_main") { inclusive = true }
                            }
                        }
                        else -> {
                            navController.popBackStack()
                        }
                    }
                }
            )
        }
        
        composable(
            "bind_phone/{phoneData}",
            arguments = listOf(navArgument("phoneData") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedPhoneData = backStackEntry.arguments?.getString("phoneData") ?: ""
            // 对URL编码的参数进行解码
            val decodedPhoneData = try {
                URLDecoder.decode(encodedPhoneData, "UTF-8")
            } catch (e: Exception) {
                encodedPhoneData
            }
            // 如果是占位符，则转换为空字符串
            val phoneData = if (decodedPhoneData == "empty") "" else decodedPhoneData
            BindPhoneScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                initialPhone = phoneData
            )
        }
        
        composable(
            "bind_email/{emailData}",
            arguments = listOf(navArgument("emailData") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedEmailData = backStackEntry.arguments?.getString("emailData") ?: ""
            // 对URL编码的参数进行解码
            val decodedEmailData = try {
                URLDecoder.decode(encodedEmailData, "UTF-8")
            } catch (e: Exception) {
                encodedEmailData
            }
            // 如果是占位符，则转换为空字符串
            val emailData = if (decodedEmailData == "empty") "" else decodedEmailData
            BindEmailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                initialEmail = emailData
            )
        }
        
        composable("draw_ad_test") {
            DrawAdScreen(
                adManager = UnifiedAdManager.getInstance(),
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("question_list") {
            QuestionListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            "webview/{url}/{title}",
            arguments = listOf(
                navArgument("url") { type = NavType.StringType },
                navArgument("title") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            val encodedTitle = backStackEntry.arguments?.getString("title") ?: "网页"
            
            // 对URL编码的参数进行解码
            val url = try {
                URLDecoder.decode(encodedUrl, "UTF-8")
            } catch (e: Exception) {
                encodedUrl
            }
            
            val title = try {
                URLDecoder.decode(encodedTitle, "UTF-8")
            } catch (e: Exception) {
                encodedTitle
            }
            
            WebViewScreen(
                url = url,
                title = title,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}