package com.jiankangpaika.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankangpaika.app.ad.UnifiedAdManager

/**
 * 开屏页面Activity
 * 负责展示开屏广告和应用启动画面
 */
class SplashActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "SplashActivity"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var isAdShown = false
    private var isTimeout = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "🚀 [锚点1] SplashActivity onCreate 开始")
        
        setContent {
            SplashScreen()
        }
        Log.d(TAG, "🎨 [锚点2] UI界面设置完成")
        
        // 加载并展示开屏广告
        Log.d(TAG, "📱 [锚点3] 准备加载开屏广告")
        loadAndShowSplashAd()
        
        // 设置超时跳转
        val timeoutMs = com.jiankangpaika.app.ad.AdConfig.Kuaishou.Config.SPLASH_TIMEOUT_MS
        Log.d(TAG, "⏰ [锚点4] 设置超时跳转，超时时间: ${timeoutMs}ms")
        handler.postDelayed({
            isTimeout = true
            Log.w(TAG, "⏰ [锚点超时] 开屏广告超时，isAdShown=$isAdShown")
            if (!isAdShown) {
                Log.w(TAG, "⏰ [锚点超时] 广告未显示，执行超时跳转")
                navigateToMain()
            }
        }, timeoutMs)
    }
    
    /**
     * 加载并展示开屏广告
     */
    private fun loadAndShowSplashAd() {
        Log.d(TAG, "📱 [锚点5] 开始加载并展示开屏广告")
        
        // 检查是否可以展示开屏广告
        Log.d(TAG, "🔍 [锚点6] 检查广告展示策略")
        val canShow = com.jiankangpaika.app.ad.AdUtils.canShowSplashAd(this)
        Log.d(TAG, "🔍 [锚点6结果] 广告策略检查结果: canShow=$canShow")
        
        if (!canShow) {
            Log.w(TAG, "❌ [锚点6失败] 根据策略，当前不展示开屏广告，直接跳转")
            navigateToMain()
            return
        }
        
        Log.d(TAG, "✅ [锚点7] 广告策略检查通过，获取AdManager实例")
        val adManager = UnifiedAdManager.getInstance()
        
        // 等待SDK初始化完成
        Log.d(TAG, "⏳ [锚点8] 等待广告管理器初始化完成")
        waitForAdManagerInitialization(adManager)
    }
    
    /**
     * 等待广告管理器初始化完成
     */
    private fun waitForAdManagerInitialization(adManager: UnifiedAdManager) {
        Log.d(TAG, "⏳ [锚点8] 等待广告管理器初始化完成")
        
        // 检查初始化状态，最多等待3秒
        var checkCount = 0
        val maxChecks = 30 // 30次 * 100ms = 3秒
        
        val checkRunnable = object : Runnable {
            override fun run() {
                checkCount++
                
                // 检查是否有可用的广告平台
                val platform = com.jiankangpaika.app.ad.AdSwitchConfig.getInstance().getSplashAdPlatform()
                val hasAvailablePlatform = adManager.hasAvailablePlatform(platform)
                
                if (hasAvailablePlatform) {
                    Log.d(TAG, "✅ [锚点8成功] 广告管理器初始化完成，开始加载广告")
                    loadSplashAdInternal(adManager)
                } else if (checkCount >= maxChecks) {
                    Log.w(TAG, "⏰ [锚点8超时] 等待广告管理器初始化超时，直接跳转")
                    navigateToMain()
                } else {
                    Log.d(TAG, "⏳ [锚点8等待] 广告管理器未就绪，继续等待... ($checkCount/$maxChecks)")
                    handler.postDelayed(this, 100) // 100ms后再次检查
                }
            }
        }
        
        handler.post(checkRunnable)
    }
    

    
    /**
     * 内部加载开屏广告方法
     */
    private fun loadSplashAdInternal(adManager: UnifiedAdManager) {
        Log.d(TAG, "🚀 [锚点9] 开始加载开屏广告")
        adManager.loadSplashAd(this) { loadSuccess, loadMessage ->
            Log.d(TAG, "📥 [锚点10] 开屏广告加载回调: success=$loadSuccess, message=$loadMessage")
            
            if (loadSuccess) {
                Log.d(TAG, "✅ [锚点10成功] 广告加载成功，准备展示广告")
                // 加载成功后展示广告
                val container = findViewById<android.view.ViewGroup>(android.R.id.content)
                adManager.showSplashAd(this, container) { showSuccess, showMessage ->
                    Log.d(TAG, "📺 [锚点11] 开屏广告展示回调: success=$showSuccess, message=$showMessage")
                    
                    if (showSuccess) {
                        when (showMessage) {
                            "开屏广告展示成功" -> {
                                Log.i(TAG, "🎉 [锚点11成功] 开屏广告展示成功！")
                                isAdShown = true
                                // 记录广告展示
                                com.jiankangpaika.app.ad.AdUtils.recordSplashAdShown(this)
                                val displayTime = com.jiankangpaika.app.ad.AdConfig.Kuaishou.Config.SPLASH_DISPLAY_TIME_MS
                                Log.i(TAG, "⏱️ [锚点12] 广告将展示${displayTime}ms后跳转")
                                // 广告展示成功，延迟跳转到主页面（仅在没有其他关闭事件时）
                                handler.postDelayed({
                                    if (!isFinishing) {
                                        Log.d(TAG, "🏃 [锚点13] 广告展示时间结束，跳转到主页面")
                                        navigateToMain()
                                    }
                                }, displayTime)
                            }
                            "开屏广告展示结束", "开屏广告被跳过" -> {
                                Log.i(TAG, "🏁 [锚点11结束] 开屏广告结束: $showMessage")
                                isAdShown = true
                                // 记录广告展示（如果还没记录的话）
                                com.jiankangpaika.app.ad.AdUtils.recordSplashAdShown(this)
                                // 广告结束或被跳过，立即跳转
                                if (!isTimeout && !isFinishing) {
                                    Log.d(TAG, "🏃 [锚点11结束跳转] 广告结束，立即跳转到主页面")
                                    navigateToMain()
                                }
                            }
                            else -> {
                                Log.i(TAG, "🎉 [锚点11其他] 开屏广告其他成功回调: $showMessage")
                                // 其他成功情况，暂不处理
                            }
                        }
                    } else {
                        // 广告展示失败，直接跳转
                        Log.e(TAG, "❌ [锚点11失败] 开屏广告展示失败: $showMessage")
                        if (!isTimeout) {
                            Log.w(TAG, "🏃 [锚点11失败跳转] 广告展示失败，直接跳转到主页面")
                            navigateToMain()
                        }
                    }
                }
            } else {
                // 广告加载失败，直接跳转
                Log.e(TAG, "❌ [锚点10失败] 开屏广告加载失败: $loadMessage")
                if (!isTimeout) {
                    Log.w(TAG, "🏃 [锚点10失败跳转] 广告加载失败，直接跳转到主页面")
                    navigateToMain()
                }
            }
        }
    }
    
    /**
     * 跳转到主页面
     */
    private fun navigateToMain() {
        Log.d(TAG, "🏃 [锚点跳转] 准备跳转到主页面，isFinishing=$isFinishing")
        if (!isFinishing) {
            Log.d(TAG, "🏃 [锚点跳转执行] 执行跳转到MainActivity")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            Log.d(TAG, "🏃 [锚点跳转完成] 跳转完成，SplashActivity即将结束")
        } else {
            Log.w(TAG, "⚠️ [锚点跳转取消] Activity正在结束，取消跳转")
        }
    }
    
    override fun onDestroy() {
        Log.d(TAG, "💀 [锚点销毁] SplashActivity onDestroy")
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "💀 [锚点销毁完成] SplashActivity销毁完成")
    }
}

/**
 * 开屏页面UI组件
 */
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 应用图标（如果有的话）
            // Image(
            //     painter = painterResource(id = R.drawable.ic_launcher),
            //     contentDescription = "应用图标",
            //     modifier = Modifier.size(120.dp)
            // )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 应用名称
            Text(
                text = "健康派卡",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2196F3)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 应用标语
            Text(
                text = "让生活更美好",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
        
        // 底部加载提示
        Text(
            text = "正在加载...",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}