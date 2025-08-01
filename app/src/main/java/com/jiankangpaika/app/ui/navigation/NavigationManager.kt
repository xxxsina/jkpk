package com.jiankangpaika.app.ui.navigation

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jiankangpaika.app.utils.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.net.URLEncoder

/**
 * 统一导航管理器
 * 集中管理应用内所有导航逻辑，包括插屏广告、登录检查等
 */
class NavigationManager(
    private val navController: NavController,
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    companion object {
        private const val TAG = "NavigationManager"
    }

    /**
     * 导航到客服表单页面
     */
    fun navigateToCustomerService() {
        Log.d(TAG, "导航到客服表单页面")
        executeNavigation("customer_service_form")
    }

    /**
     * 导航到登录页面
     * @param returnTo 登录成功后返回的页面
     */
    fun navigateToLogin(returnTo: String = "main") {
        Log.d(TAG, "导航到手机登录页面，返回目标: $returnTo")
        executeNavigation("mobile_login?returnTo=$returnTo")
    }

    /**
     * 导航到头像编辑页面
     * @param source 来源页面标识
     */
    fun navigateToEditAvatar(source: String = "home") {
        Log.d(TAG, "导航到头像编辑页面，来源: $source")
        checkLoginAndNavigate("edit_avatar/$source", requireLogin = true)
    }

    /**
     * 导航到个人设置页面
     */
    fun navigateToPersonalSettings() {
        Log.d(TAG, "导航到个人设置页面")
        checkLoginAndNavigate("personal_settings", requireLogin = true)
    }

    /**
     * 导航到隐私政策页面
     */
    fun navigateToPrivacyPolicy() {
        Log.d(TAG, "导航到隐私政策页面")
        executeNavigation("privacy_policy")
    }

    /**
     * 导航到用户协议页面
     */
    fun navigateToUserAgreement() {
        Log.d(TAG, "导航到用户协议页面")
        executeNavigation("user_agreement")
    }

    /**
     * 导航到联系我们页面
     */
    fun navigateToContactUs() {
        Log.d(TAG, "导航到联系我们页面")
        executeNavigation("contact_us")
    }

    /**
     * 导航到设置页面
     */
    fun navigateToSettings() {
        Log.d(TAG, "导航到设置页面")
        executeNavigation("settings")
    }

    /**
     * 导航到昵称编辑页面
     * @param currentNickname 当前昵称
     * @param source 来源页面，用于返回时的导航
     */
    fun navigateToEditNickname(currentNickname: String, source: String = "personal_settings") {
        Log.d(TAG, "导航到昵称编辑页面，当前昵称: $currentNickname, 来源: $source")
        executeNavigation("edit_nickname/$source")
    }

    /**
     * 导航到手机绑定页面
     * @param phoneData 手机号数据
     */
    fun navigateToBindPhone(phoneData: String) {
        Log.d(TAG, "导航到手机绑定页面")
        val safePhoneData = phoneData.ifEmpty { "empty" }
        val encodedPhoneData = URLEncoder.encode(safePhoneData, "UTF-8")
        executeNavigation("bind_phone/$encodedPhoneData")
    }

    /**
     * 导航到邮箱绑定页面
     * @param emailData 邮箱数据
     */
    fun navigateToBindEmail(emailData: String) {
        Log.d(TAG, "导航到邮箱绑定页面")
        val safeEmailData = emailData.ifEmpty { "empty" }
        val encodedEmailData = URLEncoder.encode(safeEmailData, "UTF-8")
        executeNavigation("bind_email/$encodedEmailData")
    }

    /**
     * 导航到抽奖广告测试页面
     */
    fun navigateToDrawAdTest() {
        Log.d(TAG, "导航到抽奖广告测试页面")
        executeNavigation("draw_ad_test")
    }

    /**
     * 导航到客服消息列表页面
     */
    fun navigateToCustomerMessageList() {
        Log.d(TAG, "导航到客服消息列表页面")
        executeNavigation("customer_message_list")
    }
    
    /**
     * 导航到常见问题列表页面
     */
    fun navigateToQuestionList() {
        Log.d(TAG, "导航到常见问题列表页面")
        executeNavigation("question_list")
    }
    
    /**
     * 导航到精选栏目
     */
    fun navigateToFeatured() {
        Log.d(TAG, "导航到精选栏目")
        executeNavigation("sport_main")
    }
    
    /**
     * 导航到每日任务栏目（保持向后兼容）
     */
    @Deprecated("请使用 navigateToFeatured() 方法")
    fun navigateToDailyTasks() {
        Log.d(TAG, "导航到每日任务栏目（已废弃，请使用精选）")
        executeNavigation("sport_main")
    }
    
    /**
     * 导航到WebView页面
     * @param url 要加载的URL
     * @param title 页面标题
     */
    fun navigateToWebView(url: String, title: String = "网页") {
        Log.d(TAG, "导航到WebView页面: $url")
        val encodedUrl = URLEncoder.encode(url, "UTF-8")
        val encodedTitle = URLEncoder.encode(title, "UTF-8")
        executeNavigation("webview/$encodedUrl/$encodedTitle")
    }
    
    /**
     * 导航到客服消息详情页面
     * @param message 客服消息对象
     */
    fun navigateToCustomerMessageDetail(message: com.jiankangpaika.app.data.model.CustomerMessage) {
        Log.d(TAG, "导航到客服消息详情页面")
        try {
            val messageJson = Json.encodeToString(
                com.jiankangpaika.app.data.model.CustomerMessage.serializer(), 
                message
            )
            val encodedMessage = URLEncoder.encode(messageJson, "UTF-8")
            executeNavigation("customer_message_detail/$encodedMessage")
        } catch (e: Exception) {
            Log.e(TAG, "序列化消息数据失败: ${e.message}", e)
        }
    }

    /**
     * 执行退出登录
     */
    fun logout() {
        Log.d(TAG, "执行退出登录")
        coroutineScope.launch {
            UserManager.logout(context)
            // 导航到首页并清除所有导航栈
            navController.navigate("main") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    /**
     * 返回上一页
     */
    fun navigateBack() {
        Log.d(TAG, "返回上一页")
        navController.popBackStack()
    }

    /**
     * 执行页面导航的核心函数
     */
    private fun executeNavigation(targetRoute: String, popUpTo: String? = null, inclusive: Boolean = false) {
        Log.i(TAG, "🚀 [页面导航] 开始执行导航 - 目标页面: $targetRoute, popUpTo: $popUpTo, inclusive: $inclusive")
        
        try {
            val startTime = System.currentTimeMillis()
            
            if (popUpTo != null) {
                Log.d(TAG, "📋 [页面导航] 执行带popUpTo的导航: $targetRoute -> popUpTo($popUpTo, inclusive=$inclusive)")
                navController.navigate(targetRoute) {
                    popUpTo(popUpTo) { this.inclusive = inclusive }
                }
                Log.i(TAG, "✅ [页面导航] 带popUpTo导航成功，耗时: ${System.currentTimeMillis() - startTime}ms")
            } else {
                Log.d(TAG, "📋 [页面导航] 执行普通导航: $targetRoute")
                navController.navigate(targetRoute)
                Log.i(TAG, "✅ [页面导航] 普通导航成功，耗时: ${System.currentTimeMillis() - startTime}ms")
            }
            
            Log.d(TAG, "📊 [页面导航] 当前导航栈状态: ${navController.currentBackStackEntry?.destination?.route}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [页面导航] 导航失败 - 目标: $targetRoute, 错误: ${e.message}", e)
        }
    }

    /**
     * 页面切换时展示插屏广告的辅助函数
     */
//    private fun showInterstitialAdOnNavigation(targetRoute: String, popUpTo: String? = null, inclusive: Boolean = false) {
//        Log.i(TAG, "🧭 [插屏广告辅助函数] 被调用 - 目标页面: $targetRoute, popUpTo: $popUpTo, inclusive: $inclusive")
//        Log.d(TAG, "🔍 [插屏广告策略] 开始检查是否满足插屏广告展示条件")
//
//        if (AdUtils.shouldShowInterstitialAd(context)) {
//            Log.d(TAG, "✅ [插屏广告策略] 满足展示条件，开始加载插屏广告")
//
//            UnifiedAdManager.getInstance().loadInterstitialAd(context) { success: Boolean, message: String? ->
//                Log.d(TAG, "📥 [插屏广告加载] 结果: success=$success, message=$message")
//
//                if (success) {
//                    Log.d(TAG, "🎬 [插屏广告] 加载成功，开始展示广告")
//
//                    UnifiedAdManager.getInstance().showInterstitialAd(context as androidx.activity.ComponentActivity) { showSuccess: Boolean, showMessage: String? ->
//                        Log.d(TAG, "🎭 [插屏广告展示] 结果: success=$showSuccess, message=$showMessage")
//
//                        if (showSuccess) {
//                            Log.i(TAG, "🎉 [插屏广告] 展示成功，记录展示时间")
//                            AdUtils.recordInterstitialAdShown(context)
//                        } else {
//                            Log.w(TAG, "⚠️ [插屏广告] 展示失败: $showMessage")
//                        }
//
//                        // 无论展示成功与否，都执行页面导航
//                        executeNavigation(targetRoute, popUpTo, inclusive)
//                    }
//                } else {
//                    Log.w(TAG, "⚠️ [插屏广告] 加载失败: $message，直接执行页面导航")
//                    // 加载失败时直接导航
//                    executeNavigation(targetRoute, popUpTo, inclusive)
//                }
//            }
//        } else {
//            Log.i(TAG, "⏭️ [插屏广告策略] 不满足展示条件（间隔时间未到），跳过插屏广告")
//            Log.d(TAG, "🚀 [页面导航] 直接执行页面导航，无需等待广告")
//            // 不满足展示条件时直接导航
//            executeNavigation(targetRoute, popUpTo, inclusive)
//        }
//    }

    /**
     * 检查登录状态并导航的辅助函数
     */
    private fun checkLoginAndNavigate(targetRoute: String, requireLogin: Boolean = false, popUpTo: String? = null, inclusive: Boolean = false) {
        Log.i(TAG, "🔐 [登录检查] 检查登录状态 - 目标页面: $targetRoute, 需要登录: $requireLogin")
        
        if (requireLogin && !UserManager.isLoggedIn(context)) {
            Log.d(TAG, "❌ [登录检查] 用户未登录，跳转到手机登录页面")
            // 将目标路由作为returnTo参数传递，登录成功后直接导航到目标页面
            val encodedTargetRoute = java.net.URLEncoder.encode(targetRoute, "UTF-8")
            executeNavigation("mobile_login?returnTo=$encodedTargetRoute")
        } else {
            Log.d(TAG, "✅ [登录检查] 用户已登录或无需登录，继续导航")
            executeNavigation(targetRoute, popUpTo, inclusive)
        }
    }
}

/**
 * 创建 NavigationManager 的 Composable 函数
 */
@Composable
fun rememberNavigationManager(
    navController: NavController,
    coroutineScope: CoroutineScope
): NavigationManager {
    val context = LocalContext.current
    return remember(navController, context, coroutineScope) {
        NavigationManager(navController, context, coroutineScope)
    }
}