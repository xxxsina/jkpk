package com.jiankangpaika.app.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.jiankangpaika.app.ad.kuaishou.KuaishouAdManagerImpl
import com.jiankangpaika.app.ad.chuanshanjia.ChuanshanjiaAdManagerImpl
import com.jiankangpaika.app.ad.taku.TakuAdManagerImpl

/**
 * 统一广告管理器
 * 负责管理多个广告平台，提供统一的广告接口
 * 支持广告开关控制和多平台切换
 */
class UnifiedAdManager private constructor() : AdManager {
    
    companion object {
        private const val TAG = "UnifiedAdManager"
        
        @Volatile
        private var INSTANCE: UnifiedAdManager? = null
        
        fun getInstance(): UnifiedAdManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnifiedAdManager().also { INSTANCE = it }
            }
        }
    }
    
    private val adManagers = mutableMapOf<String, AdManager>()
    private val adSwitchConfig = AdSwitchConfig.getInstance()
    private val dynamicAdConfig = DynamicAdConfig.getInstance()
    private var isInitialized = false
    
    // ==================== AdManager接口实现 ====================
    
    /**
     * 初始化广告SDK
     */
    override fun initSDK(context: Context, callback: (Boolean, String?) -> Unit) {
        initialize(context, callback)
    }
    
    /**
     * 获取广告平台名称
     */
    override fun getPlatformName(): String {
        return "UnifiedAdManager"
    }
    
    /**
     * 检查SDK是否已初始化
     */
    override fun isInitialized(): Boolean {
        return isInitialized
    }
    
    /**
     * 初始化统一广告管理器
     */
    fun initialize(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🚀 [统一广告管理器] 开始初始化")
        
        if (isInitialized) {
            Log.i(TAG, "✅ [统一广告管理器] 已经初始化，跳过重复初始化")
            callback(true, "统一广告管理器已初始化")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 初始化广告开关配置
                Log.d(TAG, "🔧 [统一广告管理器] 初始化广告开关配置")
                adSwitchConfig.init(context)
                
                // 初始化快手广告管理器
                initializeKuaishouAd(context)
                
                // 初始化穿山甲广告管理器
                initializeChuanshanjiaAd(context)
                
                // 初始化Taku广告管理器
                initializeTakuAd(context)
                
                // TODO: 初始化其他广告平台
                // initializeTencentAd(context)
                
                isInitialized = true
                Log.i(TAG, "✅ [统一广告管理器] 初始化成功")
                callback(true, "统一广告管理器初始化成功")
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 [统一广告管理器] 初始化异常", e)
                callback(false, "统一广告管理器初始化失败: ${e.message}")
            }
        }
    }
    
    /**
     * 初始化快手广告平台
     */
    private fun initializeKuaishouAd(context: Context) {
        Log.d(TAG, "🔧 [统一广告管理器] 初始化快手广告平台")
        val kuaishouAdManager = KuaishouAdManagerImpl()
        kuaishouAdManager.initSDK(context) { success, message ->
            if (success) {
                adManagers[AdSwitchConfig.PLATFORM_KUAISHOU] = kuaishouAdManager
                Log.i(TAG, "✅ [快手广告] 初始化成功")
            } else {
                Log.e(TAG, "❌ [快手广告] 初始化失败: $message")
            }
        }
    }
    
    /**
     * 初始化穿山甲广告平台
     */
    private fun initializeChuanshanjiaAd(context: Context) {
        Log.d(TAG, "🔧 [统一广告管理器] 初始化穿山甲广告平台")
        val chuanshanjiaAdManager = ChuanshanjiaAdManagerImpl()
        chuanshanjiaAdManager.initSDK(context) { success, message ->
            if (success) {
                adManagers[AdSwitchConfig.PLATFORM_CHUANSHANJIA] = chuanshanjiaAdManager
                Log.i(TAG, "✅ [穿山甲广告] 初始化成功")
            } else {
                Log.e(TAG, "❌ [穿山甲广告] 初始化失败: $message")
            }
        }
    }
    
    /**
     * 初始化Taku广告平台
     */
    private fun initializeTakuAd(context: Context) {
        Log.d(TAG, "🔧 [统一广告管理器] 初始化Taku广告平台")
        val takuAdManager = TakuAdManagerImpl()
        takuAdManager.initSDK(context) { success, message ->
            if (success) {
                adManagers[AdSwitchConfig.PLATFORM_TAKU] = takuAdManager
                Log.i(TAG, "✅ [Taku广告] 初始化成功")
            } else {
                Log.e(TAG, "❌ [Taku广告] 初始化失败: $message")
            }
        }
    }
    
    /**
     * 获取可用的广告管理器
     * 按优先级返回: 快手 -> 穿山甲 -> Taku -> 其他平台
     */
    private fun getAvailableAdManager(): AdManager? {
        // 优先使用快手广告
        val kuaishouAd = adManagers[AdSwitchConfig.PLATFORM_KUAISHOU]
        if (kuaishouAd != null && kuaishouAd.isInitialized()) {
            return kuaishouAd
        }
        
        // 其次使用穿山甲广告
        val chuanshanjiaAd = adManagers[AdSwitchConfig.PLATFORM_CHUANSHANJIA]
        if (chuanshanjiaAd != null && chuanshanjiaAd.isInitialized()) {
            return chuanshanjiaAd
        }
        
        // 再次使用Taku广告
        val takuAd = adManagers[AdSwitchConfig.PLATFORM_TAKU]
        if (takuAd != null && takuAd.isInitialized()) {
            return takuAd
        }
        
        // TODO: 添加其他广告平台的优先级选择
        // val tencentAd = adManagers["tencent"]
        // if (tencentAd != null && tencentAd.isInitialized()) {
        //     return tencentAd
        // }
        
        // val byteDanceAd = adManagers["bytedance"]
        // if (byteDanceAd != null && byteDanceAd.isInitialized()) {
        //     return byteDanceAd
        // }
        
        return null
    }
    
    /**
     * 根据平台名称获取指定的广告管理器
     */
    private fun getAdManagerByPlatform(platform: String): AdManager? {
        val adManager = adManagers[platform]
        if (adManager != null && adManager.isInitialized()) {
            Log.d(TAG, "🎯 [统一广告管理器] 使用指定平台: $platform")
            return adManager
        }
        Log.w(TAG, "⚠️ [统一广告管理器] 指定平台不可用: $platform")
        return null
    }
    
    /**
     * 检查指定平台是否可用
     */
    fun hasAvailablePlatform(platform: String): Boolean {
        val adManager = adManagers[platform]
        return adManager != null && adManager.isInitialized()
    }
    
    /**
     * 检查是否有任何可用的广告平台
     */
    fun hasAnyAvailablePlatform(): Boolean {
        return adManagers.values.any { it.isInitialized() }
    }
    
    /**
     * 初始化所有广告管理器
     */
    private fun initAllAdManagers(context: Context, callback: (Boolean, String?) -> Unit) {
        if (adManagers.isEmpty()) {
            callback(false, "没有可用的广告平台")
            return
        }
        
        var successCount = 0
        var totalCount = adManagers.size
        val errors = mutableListOf<String>()
        
        adManagers.values.forEach { manager ->
            manager.initSDK(context) { success, message ->
                if (success) {
                    successCount++
                    Log.i(TAG, "✅ [${manager.getPlatformName()}] 初始化成功")
                } else {
                    errors.add("${manager.getPlatformName()}: $message")
                    Log.e(TAG, "❌ [${manager.getPlatformName()}] 初始化失败: $message")
                }
                
                // 检查是否所有平台都已初始化完成
                if (successCount + errors.size == totalCount) {
                    if (successCount > 0) {
                        // 至少有一个平台初始化成功
                        val resultMessage = if (errors.isEmpty()) {
                            "所有广告平台初始化成功"
                        } else {
                            "部分广告平台初始化成功，失败的平台: ${errors.joinToString(", ")}"
                        }
                        callback(true, resultMessage)
                    } else {
                        // 所有平台都初始化失败
                        callback(false, "所有广告平台初始化失败: ${errors.joinToString(", ")}")
                    }
                }
            }
        }
    }
    
    // ==================== 开屏广告 ====================
    
    /**
     * 加载开屏广告
     */
    override fun loadSplashAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [开屏广告] 开始加载")
        
        // 在每次请求广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(context)
        
        if (!checkAdEnabled("开屏广告") { adSwitchConfig.isSplashAdEnabled() }) {
            callback(false, "开屏广告已关闭")
            return
        }
        
        loadAdWithFallback("开屏广告", { manager, adCallback ->
            manager.loadSplashAd(context, adCallback)
        }, callback)
    }
    
    /**
     * 展示开屏广告
     */
    override fun showSplashAd(
        activity: Activity,
        container: ViewGroup,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "🎬 [开屏广告] 开始展示")
        
        // 在每次展示广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(activity)
        
        if (!checkAdEnabled("开屏广告") { adSwitchConfig.isSplashAdEnabled() }) {
            callback(false, "开屏广告已关闭")
            return
        }
        
        showAdWithFallback("开屏广告", { manager, adCallback ->
            manager.showSplashAd(activity, container, adCallback)
        }, callback)
    }
    
    // ==================== 插屏广告 ====================
    
    /**
     * 加载插屏广告
     */
    override fun loadInterstitialAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [插屏广告] 开始加载")
        
        // 在每次请求广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(context)
        
        if (!checkAdEnabled("插屏广告") { adSwitchConfig.isInterstitialAdEnabled() }) {
            callback(false, "插屏广告已关闭")
            return
        }
        
        loadAdWithFallback("插屏广告", { manager, adCallback ->
            manager.loadInterstitialAd(context, adCallback)
        }, callback)
    }
    
    /**
     * 展示插屏广告
     */
    override fun showInterstitialAd(activity: Activity, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🎬 [插屏广告] 开始展示")
        
        // 在每次展示广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(activity)
        
        if (!checkAdEnabled("插屏广告") { adSwitchConfig.isInterstitialAdEnabled() }) {
            callback(false, "插屏广告已关闭")
            return
        }
        
        showAdWithFallback("插屏广告", { manager, adCallback ->
            manager.showInterstitialAd(activity, adCallback)
        }, callback)
    }
    
    // ==================== 信息流广告 ====================
    
    /**
     * 加载信息流广告
     */
    override fun loadFeedAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [信息流广告] 开始加载")
        
        // 在每次请求广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(context)
        
        if (!checkAdEnabled("信息流广告") { adSwitchConfig.isFeedAdEnabled() }) {
            callback(false, "信息流广告已关闭")
            return
        }
        
        loadAdWithFallback("信息流广告", { manager, adCallback ->
            manager.loadFeedAd(context, adCallback)
        }, callback)
    }
    
    /**
     * 获取信息流广告视图
     */
    override fun getFeedAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎬 [信息流广告] 获取广告视图")
        
        if (!checkAdEnabled("信息流广告") { adSwitchConfig.isFeedAdEnabled() }) {
            callback(null, "信息流广告已关闭")
            return
        }
        
        getAdViewWithFallback("信息流广告", { manager, adCallback ->
            manager.getFeedAdView(context, adCallback)
        }, callback)
    }
    
    // ==================== 激励视频广告 ====================
    
    /**
     * 加载激励视频广告
     */
    override fun loadRewardVideoAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [激励视频广告] 开始加载")
        
        // 在每次请求广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(context)
        
        if (!checkAdEnabled("激励视频广告") { adSwitchConfig.isRewardVideoAdEnabled() }) {
            callback(false, "激励视频广告已关闭")
            return
        }
        
        loadAdWithFallback("激励视频广告", { manager, adCallback ->
            manager.loadRewardVideoAd(context, adCallback)
        }, callback)
    }
    
    /**
     * 展示激励视频广告
     */
    override fun showRewardVideoAd(activity: Activity, callback: (Boolean, Boolean, String?) -> Unit) {
        Log.d(TAG, "🎬 [激励视频广告] 开始展示")
        
        // 在每次展示广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(activity)
        
        if (!checkAdEnabled("激励视频广告") { adSwitchConfig.isRewardVideoAdEnabled() }) {
            callback(false, false, "激励视频广告已关闭")
            return
        }
        
        // 激励视频广告需要特殊处理，因为回调参数不同
        val availableManagers = adManagers.values.filter { it.isInitialized() }
        if (availableManagers.isEmpty()) {
            callback(false, false, "没有可用的广告平台")
            return
        }
        
        // 根据配置选择广告平台
        val configuredPlatform = adSwitchConfig.getRewardVideoAdPlatform()
        val preferredManager = adManagers[configuredPlatform]
        val manager = if (preferredManager?.isInitialized() == true) {
            Log.d(TAG, "📱 [激励视频广告] 使用配置的 ${preferredManager.getPlatformName()} 平台")
            preferredManager
        } else {
            // 配置的平台不可用时，使用第一个可用的管理器作为降级
            val fallbackManager = availableManagers.first()
            Log.w(TAG, "⚠️ [激励视频广告] 配置的平台 $configuredPlatform 不可用，降级使用 ${fallbackManager.getPlatformName()} 平台")
            fallbackManager
        }
        
        manager.showRewardVideoAd(activity, callback)
    }
    
    // ==================== Banner广告 ====================
    
    /**
     * 加载Banner广告
     */
    override fun loadBannerAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [Banner广告] 开始加载")
        
        // 在每次请求广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(context)
        
        if (!checkAdEnabled("Banner广告") { adSwitchConfig.isBannerAdEnabled() }) {
            callback(false, "Banner广告已关闭")
            return
        }
        
        loadAdWithFallback("Banner广告", { manager, adCallback ->
            manager.loadBannerAd(context, adCallback)
        }, callback)
    }
    
    /**
     * 获取Banner广告视图
     */
    override fun getBannerAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎬 [Banner广告] 获取广告视图")
        
        if (!checkAdEnabled("Banner广告") { adSwitchConfig.isBannerAdEnabled() }) {
            callback(null, "Banner广告已关闭")
            return
        }
        
        getAdViewWithFallback("Banner广告", { manager, adCallback ->
            manager.getBannerAdView(context, adCallback)
        }, callback)
    }
    
    /**
     * 获取Banner广告视图（支持关闭回调）
     */
    override fun getBannerAdView(context: Context, onAdClosed: () -> Unit, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎬 [Banner广告] 获取广告视图（支持关闭回调）")
        
        if (!checkAdEnabled("Banner广告") { adSwitchConfig.isBannerAdEnabled() }) {
            callback(null, "Banner广告已关闭")
            return
        }
        
        getAdViewWithFallback("Banner广告", { manager, adCallback ->
            if (manager is com.jiankangpaika.app.ad.taku.TakuAdManagerImpl) {
                // Taku广告平台支持关闭回调，但需要通过其他方式处理
                manager.getBannerAdView(context, adCallback)
            } else {
                manager.getBannerAdView(context, adCallback)
            }
        }, callback)
    }
    
    // ==================== Draw广告 ====================
    
    /**
     * 加载Draw广告
     */
    override fun loadDrawAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [Draw广告] 开始加载")
        
        // 在每次请求广告前检查配置更新
        UnifiedConfigManager.getInstance().checkAndUpdateConfig(context)
        
        if (!checkAdEnabled("Draw广告") { adSwitchConfig.isDrawAdEnabled() }) {
            callback(false, "Draw广告已关闭")
            return
        }
        
        loadAdWithFallback("Draw广告", { manager, adCallback ->
            manager.loadDrawAd(context, adCallback)
        }, callback)
    }
    
    /**
     * 获取Draw广告视图
     */
    override fun getDrawAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎬 [Draw广告] 获取广告视图")
        
        if (!checkAdEnabled("Draw广告") { adSwitchConfig.isDrawAdEnabled() }) {
            callback(null, "Draw广告已关闭")
            return
        }
        
        getAdViewWithFallback("Draw广告", { manager, adCallback ->
            manager.getDrawAdView(context, adCallback)
        }, callback)
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查广告是否启用
     */
    private fun checkAdEnabled(adType: String, enabledCheck: () -> Boolean): Boolean {
        val enabled = enabledCheck()
        if (!enabled) {
            Log.d(TAG, "🚫 [$adType] 广告开关已关闭")
        }
        return enabled
    }
    
    /**
     * 使用降级策略加载广告
     */
    private fun loadAdWithFallback(
        adType: String,
        loadAction: (AdManager, (Boolean, String?) -> Unit) -> Unit,
        callback: (Boolean, String?) -> Unit
    ) {
        val availableManagers = adManagers.values.filter { it.isInitialized() }
        if (availableManagers.isEmpty()) {
            Log.w(TAG, "⚠️ [$adType] 没有可用的广告平台")
            callback(false, "没有可用的广告平台")
            return
        }
        
        // 根据广告类型获取配置的平台
        val configuredPlatform = when (adType) {
            "开屏广告" -> adSwitchConfig.getSplashAdPlatform()
            "插屏广告" -> adSwitchConfig.getInterstitialAdPlatform()
            "信息流广告" -> adSwitchConfig.getFeedAdPlatform()
            "激励视频广告" -> adSwitchConfig.getRewardVideoAdPlatform()
            "Banner广告" -> adSwitchConfig.getBannerAdPlatform()
            "Draw广告" -> adSwitchConfig.getDrawAdPlatform()
            else -> AdSwitchConfig.PLATFORM_KUAISHOU // 默认使用快手
        }
        
        // 优先使用配置的平台
        val preferredManager = adManagers[configuredPlatform]
        val manager = if (preferredManager?.isInitialized() == true) {
            Log.d(TAG, "📱 [$adType] 使用配置的 ${preferredManager.getPlatformName()} 平台")
            preferredManager
        } else {
            // 配置的平台不可用时，使用第一个可用的管理器作为降级
            val fallbackManager = availableManagers.first()
            Log.w(TAG, "⚠️ [$adType] 配置的平台 $configuredPlatform 不可用，降级使用 ${fallbackManager.getPlatformName()} 平台")
            fallbackManager
        }
        
        loadAction(manager) { success, message ->
            callback(success, message)
        }
    }
    
    /**
     * 使用降级策略展示广告
     */
    private fun showAdWithFallback(
        adType: String,
        showAction: (AdManager, (Boolean, String?) -> Unit) -> Unit,
        callback: (Boolean, String?) -> Unit
    ) {
        val availableManagers = adManagers.values.filter { it.isInitialized() }
        if (availableManagers.isEmpty()) {
            Log.w(TAG, "⚠️ [$adType] 没有可用的广告平台")
            callback(false, "没有可用的广告平台")
            return
        }
        
        // 根据广告类型获取配置的平台
         val configuredPlatform = when (adType) {
             "开屏广告" -> adSwitchConfig.getSplashAdPlatform()
             "插屏广告" -> adSwitchConfig.getInterstitialAdPlatform()
             "信息流广告" -> adSwitchConfig.getFeedAdPlatform()
             "激励视频广告" -> adSwitchConfig.getRewardVideoAdPlatform()
             "Banner广告" -> adSwitchConfig.getBannerAdPlatform()
             "Draw广告" -> adSwitchConfig.getDrawAdPlatform()
             else -> AdSwitchConfig.PLATFORM_KUAISHOU // 默认使用快手
         }
         
         // 优先使用配置的平台
         val preferredManager = adManagers[configuredPlatform]
         val manager = if (preferredManager?.isInitialized() == true) {
             Log.d(TAG, "📱 [$adType] 使用配置的 ${preferredManager.getPlatformName()} 平台")
             preferredManager
         } else {
             // 配置的平台不可用时，使用第一个可用的管理器作为降级
             val fallbackManager = availableManagers.first()
             Log.w(TAG, "⚠️ [$adType] 配置的平台 $configuredPlatform 不可用，降级使用 ${fallbackManager.getPlatformName()} 平台")
             fallbackManager
         }
         
         showAction(manager) { success, message ->
             callback(success, message)
         }
    }

    /**
     * 使用降级策略获取广告视图
     */
    private fun getAdViewWithFallback(
        adType: String,
        getViewAction: (AdManager, (ViewGroup?, String?) -> Unit) -> Unit,
        callback: (ViewGroup?, String?) -> Unit
    ) {
        val availableManagers = adManagers.values.filter { it.isInitialized() }
        if (availableManagers.isEmpty()) {
            Log.w(TAG, "⚠️ [$adType] 没有可用的广告平台")
            callback(null, "没有可用的广告平台")
            return
        }
        
        // 根据广告类型获取配置的平台
        val configuredPlatform = when (adType) {
            "开屏广告" -> adSwitchConfig.getSplashAdPlatform()
            "插屏广告" -> adSwitchConfig.getInterstitialAdPlatform()
            "信息流广告" -> adSwitchConfig.getFeedAdPlatform()
            "激励视频广告" -> adSwitchConfig.getRewardVideoAdPlatform()
            "Banner广告" -> adSwitchConfig.getBannerAdPlatform()
            "Draw广告" -> adSwitchConfig.getDrawAdPlatform()
            else -> AdSwitchConfig.PLATFORM_KUAISHOU // 默认使用快手
        }
        
        // 优先使用配置的平台
        val preferredManager = adManagers[configuredPlatform]
        val manager = if (preferredManager?.isInitialized() == true) {
            Log.d(TAG, "📱 [$adType] 使用配置的 ${preferredManager.getPlatformName()} 平台")
            preferredManager
        } else {
            // 配置的平台不可用时，使用第一个可用的管理器作为降级
            val fallbackManager = availableManagers.first()
            Log.w(TAG, "⚠️ [$adType] 配置的平台 $configuredPlatform 不可用，降级使用 ${fallbackManager.getPlatformName()} 平台")
            fallbackManager
        }
        
        getViewAction(manager) { view, message ->
            callback(view, message)
        }
    }
    
    /**
     * 获取广告开关配置
     */
    fun getAdSwitchConfig(): AdSwitchConfig {
        return adSwitchConfig
    }
    
    /**
     * 清除所有平台的SDK缓存
     * 用于解决网络错误导致的素材下载失败问题
     */
    fun clearAllCache(context: Context): Map<String, Boolean> {
        Log.d(TAG, "🧹 [统一广告管理器] 开始清除所有平台缓存")
        val results = mutableMapOf<String, Boolean>()
        
        adManagers.forEach { (platform, manager) ->
            try {
                when (manager) {
                    is ChuanshanjiaAdManagerImpl -> {
                        val success = manager.clearCache(context)
                        results[platform] = success
                        Log.d(TAG, "🧹 [缓存清理] $platform 缓存清理${if (success) "成功" else "失败"}")
                    }
                    is KuaishouAdManagerImpl -> {
                        // 快手SDK可能有不同的缓存清理方法，这里可以扩展
                        Log.d(TAG, "🧹 [缓存清理] $platform 暂不支持缓存清理")
                        results[platform] = false
                    }
                    else -> {
                        Log.d(TAG, "🧹 [缓存清理] $platform 未知平台类型")
                        results[platform] = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [缓存清理] $platform 缓存清理异常", e)
                results[platform] = false
            }
        }
        
        val successCount = results.values.count { it }
        val totalCount = results.size
        Log.i(TAG, "✅ [缓存清理] 完成，成功: $successCount/$totalCount")
        
        return results
    }
    
    /**
     * 清除指定平台的SDK缓存
     */
    fun clearCacheByPlatform(context: Context, platform: String): Boolean {
        Log.d(TAG, "🧹 [统一广告管理器] 清除 $platform 平台缓存")
        
        val manager = adManagers[platform]
        if (manager == null) {
            Log.w(TAG, "⚠️ [缓存清理] 平台 $platform 不存在")
            return false
        }
        
        return when (manager) {
            is ChuanshanjiaAdManagerImpl -> {
                manager.clearCache(context)
            }
            else -> {
                Log.w(TAG, "⚠️ [缓存清理] 平台 $platform 不支持缓存清理")
                false
            }
        }
    }
    
    /**
     * 销毁所有广告资源
     */
    override fun destroy() {
        Log.d(TAG, "🗑️ [统一广告管理器] 销毁所有广告资源")
        adManagers.values.forEach { it.destroy() }
        adManagers.clear()
        isInitialized = false
    }
}