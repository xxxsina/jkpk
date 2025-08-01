package com.jiankangpaika.app

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.jiankangpaika.app.ad.DynamicAdConfig
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.ad.UnifiedConfigManager
/**
 * 健康派卡应用Application类
 * 负责应用的全局初始化工作
 */
class ShenHuoBaoApplication : Application() {
    
    companion object {
        private const val TAG = "ShenHuoBaoApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "🚀 [Application锚点1] 健康派卡应用启动")
        
        // 初始化动态广告配置管理器
        Log.d(TAG, "🔧 [Application锚点1.5] 初始化动态广告配置管理器")
        initDynamicAdConfig()
        
        // 初始化统一广告管理器（包含快手和穿山甲）
        Log.d(TAG, "🔧 [Application锚点2] 准备初始化统一广告管理器")
        initUnifiedAdManager()
        
        // 注册Activity生命周期回调，用于配置更新
        registerActivityLifecycleCallbacks(adConfigUpdateCallbacks)
    }
    
    /**
     * 初始化动态广告配置管理器
     */
    private fun initDynamicAdConfig() {
        try {
            DynamicAdConfig.getInstance().init(this)
            Log.i(TAG, "✅ [Application锚点1.5成功] 动态广告配置管理器初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Application锚点1.5失败] 动态广告配置管理器初始化失败: ${e.message}")
        }
    }
    
    /**
     * 初始化统一广告管理器
     * 包含快手和穿山甲广告SDK的初始化
     */
    private fun initUnifiedAdManager() {
        Log.d(TAG, "🔧 [Application锚点3] 开始调用UnifiedAdManager.initialize")
        UnifiedAdManager.getInstance().initialize(this) { success, message ->
            if (success) {
                Log.i(TAG, "✅ [Application锚点3成功] 统一广告管理器初始化成功: $message")
            } else {
                Log.e(TAG, "❌ [Application锚点3失败] 统一广告管理器初始化失败: $message")
            }
        }
    }
    
    // 防抖机制：避免频繁的配置检查
    private var lastConfigCheckTime: Long = 0L
    private val CONFIG_CHECK_DEBOUNCE: Long = 5000L // 5秒防抖间隔
    private var appStartTime: Long = 0L // 应用启动时间
    private val LONG_OPERATION_THRESHOLD: Long = 10 * 60 * 1000L // 10分钟长操作阈值
    
    /**
     * Activity生命周期回调，用于在Activity恢复时检查广告配置
     * 支持长时间操作检测和强制更新机制
     */
    private val adConfigUpdateCallbacks = object : ActivityLifecycleCallbacks {
        override fun onActivityResumed(activity: Activity) {
            val currentTime = System.currentTimeMillis()
            
            // 初始化应用启动时间
            if (appStartTime == 0L) {
                appStartTime = currentTime
                Log.d(TAG, "📱 [配置更新] 应用启动，记录启动时间")
            }
            
            // 跳过SplashActivity的配置更新，避免重复请求
            if (activity.javaClass.simpleName == "SplashActivity") {
                Log.d(TAG, "⏭️ [配置更新] 跳过SplashActivity的配置更新，避免重复请求")
                return
            }
            
            // 防抖检查：如果距离上次检查时间小于防抖间隔，则跳过
            if (currentTime - lastConfigCheckTime < CONFIG_CHECK_DEBOUNCE) {
                Log.d(TAG, "⏰ [配置更新] 防抖跳过，距离上次检查时间过短: ${activity.javaClass.simpleName}")
                return
            }
            
            // 检查是否为长时间操作
            val isLongOperation = currentTime - appStartTime > LONG_OPERATION_THRESHOLD
            val timeSinceStart = (currentTime - appStartTime) / 1000 / 60 // 转换为分钟
            
            if (isLongOperation) {
                Log.i(TAG, "⏱️ [配置更新] 检测到长时间操作(${timeSinceStart}分钟)，执行强制更新: ${activity.javaClass.simpleName}")
            } else {
                Log.d(TAG, "🔄 [配置更新] Activity恢复，检查广告配置更新: ${activity.javaClass.simpleName}")
            }
            
            lastConfigCheckTime = currentTime
            
            try {
                // 使用统一配置管理器，长时间操作时强制更新
                if (isLongOperation) {
                    UnifiedConfigManager.getInstance().forceUpdateConfig(activity)
                } else {
                    UnifiedConfigManager.getInstance().checkAndUpdateConfig(activity)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [配置更新] 检查广告配置时发生错误: ${e.message}")
            }
        }
        
        // 其他生命周期方法保持空实现
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {}
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}
    }
}