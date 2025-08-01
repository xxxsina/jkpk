package com.jiankangpaika.app.ad.chuanshanjia

// Banner广告使用TTNativeExpressAd
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.jiankangpaika.app.ad.AdConfig
import com.jiankangpaika.app.ad.AdManager
import com.jiankangpaika.app.ad.BiddingTypeManager
import com.jiankangpaika.app.ad.DynamicAdConfig
import com.bytedance.sdk.openadsdk.AdSlot
import com.bytedance.sdk.openadsdk.CSJAdError
import com.bytedance.sdk.openadsdk.CSJSplashAd
import com.bytedance.sdk.openadsdk.CSJSplashCloseType
import com.bytedance.sdk.openadsdk.TTAdConfig
import com.bytedance.sdk.openadsdk.TTAdConstant
import com.bytedance.sdk.openadsdk.TTAdLoadType
import com.bytedance.sdk.openadsdk.TTAdManager
import com.bytedance.sdk.openadsdk.TTAdNative
import com.bytedance.sdk.openadsdk.TTAdSdk
import com.bytedance.sdk.openadsdk.TTDrawFeedAd
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd
import com.bytedance.sdk.openadsdk.TTNativeExpressAd
import com.bytedance.sdk.openadsdk.*
import com.bytedance.sdk.openadsdk.mediation.init.MediationConfigUserInfoForSegment
import com.bytedance.sdk.openadsdk.mediation.init.MediationPrivacyConfig

/**
 * 穿山甲广告管理器实现类
 * 实现AdManager接口，提供穿山甲广告SDK的具体实现
 * 基于TTAdSdk API
 */
class ChuanshanjiaAdManagerImpl : AdManager {
    
    companion object {
        private const val TAG = "ChuanshanjiaAdManagerImpl"
        
        // 使用AdConfig中的配置
        private val APP_ID = AdConfig.Chuanshanjia.APP_ID
        private val APP_NAME = AdConfig.Chuanshanjia.APP_NAME
//        private val SPLASH_AD_UNIT_ID = AdConfig.Chuanshanjia.AdUnitId.SPLASH
//        private val BANNER_AD_UNIT_ID = AdConfig.Chuanshanjia.AdUnitId.BANNER
//        private val REWARD_AD_UNIT_ID = AdConfig.Chuanshanjia.AdUnitId.REWARD_VIDEO
//        private val INTERSTITIAL_AD_UNIT_ID = AdConfig.Chuanshanjia.AdUnitId.INTERSTITIAL
//        private val FEED_AD_UNIT_ID = AdConfig.Chuanshanjia.AdUnitId.FEED
    }
    
    /**
     * 动态获取屏幕尺寸
     * @param context 上下文
     * @return Pair<width, height> 屏幕宽高
     */
    private fun getScreenSize(context: Context): Pair<Int, Int> {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    /**
     * 根据竞价类型创建AdSlot
     * @param adSlotConfig 广告位配置
     * @param context 上下文，用于动态获取屏幕尺寸
     * @param width 广告宽度（可选，如果不提供则动态获取）
     * @param height 广告高度（可选，如果不提供则动态获取）
     * @return 配置好的AdSlot
     */
    private fun createAdSlot(
        adSlotConfig: AdConfig.Chuanshanjia.AdSlotConfig,
        context: Context,
        width: Int = 0,
        height: Int = 0
    ): AdSlot {
        // 获取动态配置的竞价类型
        val dynamicConfig = if (BiddingTypeManager.isBiddingTypeSwitchEnabled()) {
            BiddingTypeManager.getDynamicAdSlotConfig(adSlotConfig)
        } else {
            adSlotConfig
        }
        
        val builder = AdSlot.Builder()
            .setCodeId(dynamicConfig.adUnitId)
            .setSupportDeepLink(true)
        
        // 根据竞价类型配置不同的参数
        when (dynamicConfig.biddingType) {
            AdConfig.Chuanshanjia.BiddingType.STANDARD -> {
                Log.d(TAG, "🔧 [AdSlot配置] 使用标准竞价模式: ${dynamicConfig.description}")
                // 标准竞价无需特殊配置
            }
            AdConfig.Chuanshanjia.BiddingType.SERVER_SIDE -> {
                Log.d(TAG, "🔧 [AdSlot配置] 使用服务端竞价模式: ${dynamicConfig.description}")
                // 服务端竞价需要特殊配置
                builder.setAdLoadType(TTAdLoadType.PRELOAD)
                // 注意：服务端竞价使用PRELOAD类型，不需要额外的userData设置
            }
        }
        
        // 设置尺寸（动态获取或使用提供的尺寸）
        val (finalWidth, finalHeight) = if (width > 0 && height > 0) {
            Pair(width, height)
        } else {
            getScreenSize(context)
        }
        
        Log.d(TAG, "🔧 [AdSlot配置] 使用尺寸: ${finalWidth}x${finalHeight}")
        builder.setImageAcceptedSize(finalWidth, finalHeight)
        
        // 注释掉融合SDK配置，当前版本不支持
        // val mediationAdSlot = MediationAdSlot.Builder()
        //     .setMediationAdSlotId(dynamicConfig.adUnitId)
        //     .setMediationAdSlotType(getMediationAdSlotType(adSlotConfig.adType))
        //     .build()
        // builder.setMediationAdSlot(mediationAdSlot)
        Log.d(TAG, "🔧 [AdSlot配置] AdSlot配置完成")
        
        return builder.build()
    }
    
    // 移除getMediationAdSlotType方法，当前版本不支持融合SDK
    // private fun getMediationAdSlotType(adType: String): Int { ... }
    
    private var isInitialized = false
    private var ttAdNative: TTAdNative? = null
    private var splashAd: CSJSplashAd? = null
    private var interstitialAd: TTFullScreenVideoAd? = null
    private var feedAd: TTNativeExpressAd? = null
    private var rewardVideoAd: TTRewardVideoAd? = null
    private var bannerAd: TTNativeExpressAd? = null
    private var drawAd: TTDrawFeedAd? = null

    override fun getPlatformName(): String = "穿山甲广告"
    
    override fun isInitialized(): Boolean = isInitialized
    
    /**
     * 检查是否为网络相关错误
     */
    private fun isNetworkError(errorMsg: String, errorCode: Int): Boolean {
        val networkErrorKeywords = listOf(
            "ERR_TIMED_OUT",
            "SSL handshake failed",
            "timeout",
            "网络",
            "network",
            "connection",
            "连接",
            "超时"
        )
        
        // 检查错误消息中是否包含网络相关关键词
        val hasNetworkKeyword = networkErrorKeywords.any { keyword ->
            errorMsg.contains(keyword, ignoreCase = true)
        }
        
        // 检查特定的网络错误代码（根据穿山甲SDK文档）
        val isNetworkErrorCode = when (errorCode) {
            40000, 40001, 40002, 40003, 40004, 40005 -> true // 网络相关错误码
            else -> false
        }
        
        return hasNetworkKeyword || isNetworkErrorCode
    }
    
    /**
     * 清除SDK缓存
     * 注意：穿山甲SDK没有提供直接的clearCache方法
     * 这里提供一个占位实现，实际清除缓存需要通过其他方式
     */
    fun clearCache(context: Context): Boolean {
        return try {
            Log.d(TAG, "🧹 [缓存清理] 穿山甲SDK暂不支持直接清除缓存")
            Log.i(TAG, "💡 [缓存清理] 建议重启应用或清除应用数据来清理缓存")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ [缓存清理] 清除缓存失败", e)
            false
        }
    }
    
    /**
     * 输出当前广告配置状态（用于调试）
     */
    private fun logCurrentAdConfig() {
        Log.d(TAG, "📋 [配置检查] ==========================")
        Log.d(TAG, "📋 [配置检查] 穿山甲广告配置状态:")
        Log.d(TAG, "📋 [配置检查] APP_ID: ${AdConfig.Chuanshanjia.APP_ID}")
        Log.d(TAG, "📋 [配置检查] APP_NAME: ${AdConfig.Chuanshanjia.APP_NAME}")
        Log.d(TAG, "📋 [配置检查] 开屏广告ID: ${AdConfig.Chuanshanjia.AdUnitId.SPLASH}")
        Log.d(TAG, "📋 [配置检查] 信息流广告ID: ${AdConfig.Chuanshanjia.AdUnitId.FEED}")
        Log.d(TAG, "📋 [配置检查] 激励视频ID: ${AdConfig.Chuanshanjia.AdUnitId.REWARD_VIDEO}")
        Log.d(TAG, "📋 [配置检查] 插屏广告ID: ${AdConfig.Chuanshanjia.AdUnitId.INTERSTITIAL}")
        Log.d(TAG, "📋 [配置检查] Banner广告ID: ${AdConfig.Chuanshanjia.AdUnitId.BANNER}")
        Log.d(TAG, "📋 [配置检查] Draw广告ID: ${AdConfig.Chuanshanjia.AdUnitId.DRAW_VIDEO}")
        Log.d(TAG, "📋 [配置检查] ==========================")
        
        // 检查是否与服务器配置一致
        val dynamicConfig = DynamicAdConfig.getInstance()
        Log.d(TAG, "🔍 [配置对比] 动态配置状态:")
        Log.d(TAG, "🔍 [配置对比] ${dynamicConfig.getAllConfigStatus()}")
        
        // 特别检查信息流广告ID配置问题
        val currentFeedId = AdConfig.Chuanshanjia.AdUnitId.FEED
        val adSlotConfigFeedId = AdConfig.Chuanshanjia.AdSlotConfigs.FEED.adUnitId
        Log.w(TAG, "⚠️ [信息流配置检查] 当前动态配置ID: $currentFeedId")
        Log.w(TAG, "⚠️ [信息流配置检查] AdSlotConfig配置ID: $adSlotConfigFeedId")
        if (currentFeedId != adSlotConfigFeedId) {
            Log.e(TAG, "❌ [信息流配置检查] 发现配置不一致! 这可能导致广告加载失败")
            Log.e(TAG, "❌ [信息流配置检查] 建议检查服务器配置: http://shb.blcwg.com/api/ad_config.php")
        }
    }
    
    override fun initSDK(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🚀 [穿山甲广告] 开始初始化SDK")
        
        if (isInitialized) {
            Log.i(TAG, "✅ [穿山甲广告] SDK已经初始化，跳过重复初始化")
            callback(true, "SDK已初始化")
            return
        }
        
        // 初始化竞价类型管理器
        BiddingTypeManager.init(context)
        Log.d(TAG, "🔧 [穿山甲广告] 竞价类型管理器初始化完成")
        
        // 输出当前广告配置状态
        logCurrentAdConfig()
        
        Log.d(TAG, "🔧 [穿山甲广告] 创建SDK配置: APP_ID=${APP_ID}, APP_NAME=${APP_NAME}")
        try {
            // 构建TTAdConfig配置 - 升级到7.0.1.2版本，支持融合SDK和adapter
            val config = TTAdConfig.Builder()
                .appId(APP_ID)
                .appName(APP_NAME)
                .debug(false) // 上线前需要关闭
                .useMediation(true) // 启用融合功能
                .supportMultiProcess(false) // 单进程应用
                .allowShowNotify(true) // 允许SDK弹出通知
                .directDownloadNetworkType(TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_3G, TTAdConstant.NETWORK_STATE_4G, TTAdConstant.NETWORK_STATE_5G) // 允许直接下载的网络状态
                .customController(getTTCustomController()) // 设置隐私策略
                .build()
            
            Log.d(TAG, "🔧 [穿山甲广告] 融合SDK配置完成，支持快手、优量汇等ADN")
            
            Log.d(TAG, "🔧 [穿山甲广告] 调用TTAdSdk.init进行初始化")
            
            // 7.0.1.2版本：先初始化SDK
            TTAdSdk.init(context, config)
            
            Log.d(TAG, "🔧 [穿山甲广告] 调用TTAdSdk.start启动SDK")
            
            // 7.0.1.2版本：再启动SDK
            TTAdSdk.start(object : TTAdSdk.Callback {
                override fun success() {
                    Log.d(TAG, "🚀 [穿山甲广告] SDK启动成功")
                    Log.d(TAG, "🔍 [穿山甲广告] SDK就绪状态: ${TTAdSdk.isSdkReady()}")
                    
                    // 创建TTAdNative对象
                    try {
                        ttAdNative = TTAdSdk.getAdManager().createAdNative(context)
                        Log.d(TAG, "✅ [穿山甲广告] TTAdNative创建成功")
                        isInitialized = true
                        callback(true, "穿山甲广告SDK初始化成功")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [穿山甲广告] TTAdNative创建失败", e)
                        callback(false, "TTAdNative创建失败: ${e.message}")
                    }
                }
                
                override fun fail(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [穿山甲广告] SDK启动失败: code=$code, msg=$msg")
                    callback(false, "SDK启动失败: $msg")
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [穿山甲广告] 初始化异常", e)
            callback(false, "初始化异常: ${e.message}")
        }
    }
    
    // ==================== 开屏广告 ====================
    
    override fun loadSplashAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [开屏广告] 开始加载")
        
        if (!isInitialized || ttAdNative == null) {
            Log.e(TAG, "❌ [开屏广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }

        try {
            // 使用配置化的AdSlot创建方法
            val adSlotConfig = AdConfig.Chuanshanjia.AdSlotConfigs.SPLASH
            val adSlot = createAdSlot(adSlotConfig, context)

            Log.d(TAG, "🔧 [开屏广告] 创建广告场景: 广告位ID=${adSlotConfig.adUnitId}")
            Log.d(TAG, "🔧 [开屏广告] 使用竞价类型: ${adSlotConfig.biddingType}")
            
            ttAdNative?.loadSplashAd(adSlot, object : TTAdNative.CSJSplashAdListener {
                override fun onSplashLoadFail(error: CSJAdError?) {
                    val errorMsg = error?.msg ?: "未知错误"
                    val errorCode = error?.code ?: -1
                    
                    Log.e(TAG, "❌ [开屏广告] 加载失败: code=$errorCode, message=$errorMsg")
                    
                    // 检查是否为网络相关错误
                    if (isNetworkError(errorMsg, errorCode)) {
                        Log.w(TAG, "🌐 [开屏广告] 检测到网络错误，建议清除缓存")
                        callback(false, "开屏广告加载失败(网络错误): $errorMsg (code: $errorCode)")
                    } else {
                        callback(false, "开屏广告加载失败: $errorMsg (code: $errorCode)")
                    }
                }
                
                override fun onSplashLoadSuccess(ad: CSJSplashAd?) {
                    Log.i(TAG, "✅ [开屏广告] 加载成功 onSplash Load Success")
//                    splashAd = ad
//                    callback(true, "开屏广告加载成功")
                }
                
                override fun onSplashRenderSuccess(ad: CSJSplashAd?) {
                    Log.i(TAG, "✅ [开屏广告] 渲染成功 onSplash Render Success")
                    splashAd = ad
                    callback(true, "开屏广告加载成功")
                }
                
                override fun onSplashRenderFail(ad: CSJSplashAd?, error: CSJAdError?) {
                    val errorMsg = error?.msg ?: "未知错误"
                    val errorCode = error?.code ?: -1
                    
                    Log.e(TAG, "❌ [开屏广告] 渲染失败: code=$errorCode, message=$errorMsg")
                    
                    // 检查是否为网络相关错误
                    if (isNetworkError(errorMsg, errorCode)) {
                        Log.w(TAG, "🌐 [开屏广告] 渲染失败，检测到网络错误")
                        callback(false, "开屏广告渲染失败(网络错误): $errorMsg")
                    } else {
                        callback(false, "开屏广告渲染失败: $errorMsg")
                    }
                }
            }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [开屏广告] 加载异常", e)
            callback(false, "开屏广告加载异常: ${e.message}")
        }
    }
    
    override fun showSplashAd(
        activity: Activity,
        container: ViewGroup,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "📺 [开屏广告] 开始展示")
        
        val ad = splashAd
        if (ad == null) {
            Log.e(TAG, "❌ [开屏广告] 广告未加载，无法展示")
            callback(false, "开屏广告未加载")
            return
        }
        
        try {
            // 保存callback引用，避免在监听器中被覆盖
            val showCallback = callback
            var hasCalledShowCallback = false

            ad.setSplashAdListener(object : CSJSplashAd.SplashAdListener {
                override fun onSplashAdShow(ad: CSJSplashAd?) {
                    Log.d(TAG, "👁️ [开屏广告] 广告展示")
                    
                    // 获取第三方广告平台信息
                    try {
                        val mediationManager = ad?.mediationManager
                        val ecpmList = mediationManager?.showEcpm
                        Log.i(TAG, "📊 [开屏广告] 第三方广告平台: ${ecpmList?.sdkName}")
//                        if (ecpmList != null) {
//                            val iterator = ecpmList.iterator()
//                            while (iterator.hasNext()) {
//                                val ecpmInfo = iterator.next()
//                                Log.i(TAG, "📊 [开屏广告] 第三方广告平台: ${ecpmInfo.sdkName}")
//                                logEcpmInfo(ecpmInfo)
//                            }
//                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [开屏广告] 获取第三方平台信息失败", e)
                    }
                    
                    if (!hasCalledShowCallback) {
                        hasCalledShowCallback = true
                        showCallback(true, "开屏广告展示成功")
                    }
                }
                
                override fun onSplashAdClick(ad: CSJSplashAd?) {
                    Log.d(TAG, "🖱️ [开屏广告] 广告被点击")
                }
                
                override fun onSplashAdClose(ad: CSJSplashAd?, closeType: Int) {
                    Log.d(TAG, "❌ [开屏广告] 广告关闭，closeType: $closeType")
                    splashAd = null // 清理广告对象
                    
                    // 根据关闭类型返回相应的消息，确保与SplashActivity的逻辑匹配
                    when (closeType) {
                        CSJSplashCloseType.CLICK_SKIP -> {
                            Log.d(TAG, "开屏广告点击跳过")
                            showCallback(true, "开屏广告被跳过")
                        }
                        CSJSplashCloseType.COUNT_DOWN_OVER -> {
                            Log.d(TAG, "开屏广告倒计时结束")
                            showCallback(true, "开屏广告展示结束")
                        }
                        CSJSplashCloseType.CLICK_JUMP -> {
                            Log.d(TAG, "点击跳转")
                            showCallback(true, "开屏广告展示结束")
                        }
                        else -> {
                            Log.d(TAG, "开屏广告其他关闭方式: $closeType")
                            showCallback(true, "开屏广告展示结束")
                        }
                    }
                }
            })
            
            val splashView = ad.getSplashView()
            if (splashView != null) {
                container.removeAllViews()
                container.addView(splashView)
                Log.d(TAG, "✅ [开屏广告] 广告视图已添加到容器")
            } else {
                Log.e(TAG, "❌ [开屏广告] 获取广告视图失败")
                callback(false, "获取开屏广告视图失败")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [开屏广告] 展示异常", e)
            callback(false, "开屏广告展示异常: ${e.message}")
        }
    }
    
    // ==================== 插屏广告 ====================
    
    override fun loadInterstitialAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [插屏广告] 开始加载")
        
        if (!isInitialized || ttAdNative == null) {
            Log.e(TAG, "❌ [插屏广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            // 使用配置化的AdSlot创建方法
            val adSlotConfig = AdConfig.Chuanshanjia.AdSlotConfigs.INTERSTITIAL
            val adSlot = createAdSlot(adSlotConfig, context)
            
            Log.d(TAG, "🔧 [插屏广告] 使用竞价类型: ${adSlotConfig.biddingType}")
            
            ttAdNative?.loadFullScreenVideoAd(adSlot, object : TTAdNative.FullScreenVideoAdListener {
                override fun onError(code: Int, message: String) {
                    Log.e(TAG, "❌ [插屏广告] 加载失败: code=$code, message=$message")
                    callback(false, "插屏广告加载失败: $message (code: $code)")
                }
                
                override fun onFullScreenVideoAdLoad(ad: TTFullScreenVideoAd?) {
                    Log.i(TAG, "✅ [插屏广告] 加载成功")
                    interstitialAd = ad
                    callback(true, "插屏广告加载成功")
                }

                override fun onFullScreenVideoCached() {
                    Log.i(TAG, "✅ [插屏广告] 视频缓存完成")
                }

                override fun onFullScreenVideoCached(p0: TTFullScreenVideoAd?) {
                    Log.d(TAG, "💾 [插屏广告] 视频缓存完成，广告对象: $p0")
                    // 可以在这里处理带参数的缓存完成回调
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [插屏广告] 加载异常", e)
            callback(false, "插屏广告加载异常: ${e.message}")
        }
    }
    
    override fun showInterstitialAd(activity: Activity, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "📺 [插屏广告] 开始展示")
        
        val ad = interstitialAd
        if (ad == null) {
            Log.e(TAG, "❌ [插屏广告] 广告未加载，无法展示")
            callback(false, "插屏广告未加载")
            return
        }
        
        try {
            ad.setFullScreenVideoAdInteractionListener(object : TTFullScreenVideoAd.FullScreenVideoAdInteractionListener {
                override fun onAdShow() {
                    Log.d(TAG, "👁️ [插屏广告] 广告展示")
                    
                    // 获取第三方广告平台信息
//                    try {
//                        val mediationManager = ad.mediationManager
//                        val ecpmList = mediationManager?.showEcpm
//                        Log.i(TAG, "📊 [开屏广告] 第三方广告平台: ${ecpmList?.sdkName}")
//                        if (ecpmList != null) {
//                            val iterator = ecpmList.iterator()
//                            while (iterator.hasNext()) {
//                                val ecpmInfo = iterator.next()
//                                Log.d(TAG, "📊 [插屏广告] 第三方广告平台: ${ecpmInfo.sdkName}")
//                                logEcpmInfo(ecpmInfo)
//                            }
//                        }
//                    } catch (e: Exception) {
//                        Log.e(TAG, "❌ [插屏广告] 获取第三方平台信息失败", e)
//                    }
                    
                    callback(true, "插屏广告展示成功")
                }
                
                override fun onAdVideoBarClick() {
                    Log.d(TAG, "🖱️ [插屏广告] 广告被点击")
                }
                
                override fun onAdClose() {
                    Log.d(TAG, "❌ [插屏广告] 广告关闭")
                    interstitialAd = null
                }
                
                override fun onVideoComplete() {
                    Log.d(TAG, "✅ [插屏广告] 视频播放完成")
                }
                
                override fun onSkippedVideo() {
                    Log.d(TAG, "⏭️ [插屏广告] 视频被跳过")
                }
            })
            
            ad.showFullScreenVideoAd(activity)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [插屏广告] 展示异常", e)
            callback(false, "插屏广告展示异常: ${e.message}")
        }
    }
    
    // ==================== 信息流广告 ====================
    
    override fun loadFeedAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [信息流广告] 开始加载")
        
        if (!isInitialized || ttAdNative == null) {
            Log.e(TAG, "❌ [信息流广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }

        try {
            // 强制刷新动态配置，确保使用最新的服务器配置
//            val dynamicConfig = DynamicAdConfig.getInstance()
//            dynamicConfig.checkAndUpdateConfig(context)
            
            // 获取最新的服务器配置ID
            val serverFeedId = AdConfig.Chuanshanjia.AdUnitId.FEED
            val adSlotConfig = AdConfig.Chuanshanjia.AdSlotConfigs.FEED
            
            Log.d(TAG, "🔧 [信息流广告] 服务器最新配置ID: $serverFeedId")
            Log.d(TAG, "🔧 [信息流广告] AdSlotConfig配置ID: ${adSlotConfig.adUnitId}")
            Log.d(TAG, "🔧 [信息流广告] 使用竞价类型: ${adSlotConfig.biddingType}")
            
            // 检查配置一致性
            if (adSlotConfig.adUnitId != serverFeedId) {
                Log.w(TAG, "⚠️ [信息流广告] 检测到配置不一致!")
                Log.w(TAG, "⚠️ [信息流广告] AdSlotConfig ID: ${adSlotConfig.adUnitId}")
                Log.w(TAG, "⚠️ [信息流广告] 服务器配置 ID: $serverFeedId")
                Log.w(TAG, "⚠️ [信息流广告] 将强制使用服务器配置以确保广告正常加载")
            }

            // 设置尺寸（动态获取或使用提供的尺寸）
            val (finalWidth, finalHeight) = getScreenSize(context)
            Log.w(TAG, "⚠️ [信息流广告] 自动获取屏幕宽度 width = ${finalWidth}")
            // 始终使用服务器的最新配置创建AdSlot，确保配置同步
            val adSlot = AdSlot.Builder()
                .setCodeId(serverFeedId)  // 强制使用服务器配置
                .setSupportDeepLink(true)
                .setImageAcceptedSize(finalWidth, 0)
                .setExpressViewAcceptedSize(350f, 0f)
                .setAdCount(1)
                // 移除MediationAdSlot配置，当前版本不支持
                // .setMediationAdSlot(
                //     MediationAdSlot.Builder()
                //         .setMuted(true)
                //         .build()
                // )
                .apply {
                    // 根据竞价类型配置
                    when (adSlotConfig.biddingType) {
                        AdConfig.Chuanshanjia.BiddingType.SERVER_SIDE -> {
                            Log.d(TAG, "🔧 [信息流广告] 使用服务端竞价模式")
                            setAdLoadType(TTAdLoadType.PRELOAD)
                        }
                        else -> {
                            Log.d(TAG, "🔧 [信息流广告] 使用标准竞价模式")
                        }
                    }
                }
                .build()
            
            Log.d(TAG, "🔧 [信息流广告] 最终使用广告位ID: $serverFeedId")
            loadFeedAdWithSlot(adSlot, callback)
             
        } catch (e: Exception) {
            Log.e(TAG, "💥 [信息流广告] 加载异常", e)
            callback(false, "信息流广告加载异常: ${e.message}")
        }
    }
    
    /**
     * 使用指定的AdSlot加载信息流广告
     */
    private fun loadFeedAdWithSlot(adSlot: AdSlot, callback: (Boolean, String?) -> Unit) {
        ttAdNative?.loadNativeExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
                override fun onError(code: Int, message: String) {
                    Log.e(TAG, "❌ [信息流广告] 加载失败: code=$code, message=$message")
                    callback(false, "信息流广告加载失败: $message (code: $code)")
                }
                
                override fun onNativeExpressAdLoad(ads: MutableList<TTNativeExpressAd>?) {
                    if (ads != null && ads.isNotEmpty()) {
                        Log.i(TAG, "✅ [信息流广告] 加载成功")
                        feedAd = ads[0]
                        callback(true, "信息流广告加载成功")
                    } else {
                        Log.e(TAG, "❌ [信息流广告] 加载成功但广告列表为空")
                        callback(false, "信息流广告列表为空")
                    }
                }
            })
    }
    
    override fun getFeedAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎨 [信息流广告] 获取广告视图")
        
        val ad = feedAd
        if (ad == null) {
            Log.e(TAG, "❌ [信息流广告] 广告未加载，无法获取视图")
            callback(null, "信息流广告未加载")
            return
        }
        
        try {
            ad.setExpressInteractionListener(object : TTNativeExpressAd.ExpressAdInteractionListener {
                override fun onAdClicked(view: android.view.View?, type: Int) {
                    Log.d(TAG, "🖱️ [信息流广告] 广告被点击")
                }
                
                override fun onAdShow(view: android.view.View?, type: Int) {
                    Log.d(TAG, "👁️ [信息流广告] 广告展示")
                    
                    // 获取第三方广告平台信息
                    try {
                        val mediationManager = ad.mediationManager
                        val ecpmList = mediationManager?.showEcpm

                        Log.i(TAG, "📊 [开屏广告] 第三方广告平台: ${ecpmList?.sdkName}")
//                        if (ecpmList != null) {
//                            val iterator = ecpmList.iterator()
//                            while (iterator.hasNext()) {
//                                val ecpmInfo = iterator.next()
//                                Log.d(TAG, "📊 [信息流广告] 第三方广告平台: ${ecpmInfo.sdkName}")
//                                logEcpmInfo(ecpmInfo)
//                            }
//                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [信息流广告] 获取第三方平台信息失败", e)
                    }
                }
                
                override fun onRenderFail(view: android.view.View?, msg: String?, code: Int) {
                    Log.e(TAG, "❌ [信息流广告] 渲染失败: code=$code, msg=$msg")
                    callback(null, "信息流广告渲染失败: $msg")
                }
                
                override fun onRenderSuccess(view: View?, width: Float, height: Float) {
                    Log.d(TAG, "✅ [信息流广告] 渲染成功: width=$width, height=$height")
                    val feedView = feedAd?.expressAdView
                    callback(feedView as? ViewGroup, "信息流广告视图获取成功")
                }
            })

            ad.render()
        } catch (e: Exception) {
            Log.e(TAG, "💥 [信息流广告] 获取视图异常", e)
            callback(null, "信息流广告视图获取异常: ${e.message}")
        }
    }
    
    // ==================== 激励视频广告 ====================
    
    override fun loadRewardVideoAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [激励视频] 开始加载")
        
        if (!isInitialized || ttAdNative == null) {
            Log.e(TAG, "❌ [激励视频] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            // 使用配置化的AdSlot创建方法
            val adSlotConfig = AdConfig.Chuanshanjia.AdSlotConfigs.REWARD_VIDEO
            // 获取最新的服务器配置ID
            val serverReWardId = AdConfig.Chuanshanjia.AdUnitId.REWARD_VIDEO
            Log.d(TAG, "🔧 [激励视频] 创建广告场景: 广告位ID=${adSlotConfig.adUnitId}；新的服务器配置ID=${serverReWardId}")
            val (screenWidth, screenHeight) = getScreenSize(context)
            Log.d(TAG, "🔧 [激励视频] 使用动态尺寸: ${screenWidth}x${screenHeight}")
            val adSlot = AdSlot.Builder()
                .setCodeId(serverReWardId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(screenWidth, screenHeight)
                .setRewardName("金币")
                .setRewardAmount(10)
                .setUserID("user123")
                .setOrientation(TTAdConstant.VERTICAL)
                // 移除MediationAdSlot配置，当前版本不支持
                // .setMediationAdSlot(MediationAdSlot.Builder().setMuted(true).build())
                .apply {
                    // 根据竞价类型配置
                    when (adSlotConfig.biddingType) {
                        AdConfig.Chuanshanjia.BiddingType.SERVER_SIDE -> {
                            Log.d(TAG, "🔧 [激励视频] 使用服务端竞价模式")
                            setAdLoadType(TTAdLoadType.LOAD)
                            setUserData("server_bidding=true")
                            // 注意：服务端竞价的具体实现需要根据实际SDK版本调整
                        }
                        else -> {
                            Log.d(TAG, "🔧 [激励视频] 使用标准竞价模式")
                        }
                    }
                }
                .build()
            
            Log.d(TAG, "🔧 [激励视频] 使用竞价类型: ${adSlotConfig.biddingType}")
            
            ttAdNative?.loadRewardVideoAd(adSlot, object : TTAdNative.RewardVideoAdListener {
                override fun onError(code: Int, message: String) {
                    Log.e(TAG, "❌ [激励视频] 加载失败: code=$code, message=$message")
                    callback(false, "激励视频加载失败: $message (code: $code)")
                }
                
                override fun onRewardVideoAdLoad(ad: TTRewardVideoAd?) {
                    Log.i(TAG, "✅ [激励视频] 加载成功")
                    rewardVideoAd = ad
                    callback(true, "激励视频加载成功")
                }
                
                override fun onRewardVideoCached() {
                    Log.d(TAG, "💾 [激励视频] 视频缓存完成")
                }

                override fun onRewardVideoCached(p0: TTRewardVideoAd?) {
                    Log.d(TAG, "💾 [激励视频] 视频缓存完成，广告对象: $p0")
                    // 可以在这里处理带参数的缓存完成回调
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [激励视频] 加载异常", e)
            callback(false, "激励视频加载异常: ${e.message}")
        }
    }
    
    override fun showRewardVideoAd(activity: Activity, callback: (Boolean, Boolean, String?) -> Unit) {
        Log.d(TAG, "📺 [激励视频] 开始展示")
        
        val ad = rewardVideoAd
        if (ad == null) {
            Log.e(TAG, "❌ [激励视频] 广告未加载，无法展示")
            callback(false, false, "广告未加载")
            return
        }
        
        try {
            var hasCallbackCalled = false
            
            ad.setRewardAdInteractionListener(object : TTRewardVideoAd.RewardAdInteractionListener {
                override fun onAdShow() {
                    Log.d(TAG, "👁️ [激励视频] 广告展示")
                    
                    // 获取第三方广告平台信息
                    try {
                        val mediationManager = ad.mediationManager
                        val ecpmList = mediationManager?.showEcpm
                        Log.i(TAG, "📊 [开屏广告] 第三方广告平台: ${ecpmList?.sdkName}")
//                        if (ecpmList != null) {
//                            val iterator = ecpmList.iterator()
//                            while (iterator.hasNext()) {
//                                val ecpmInfo = iterator.next()
//                                Log.d(TAG, "📊 [激励视频] 第三方广告平台: ${ecpmInfo.sdkName}")
//                                logEcpmInfo(ecpmInfo)
//                            }
//                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [激励视频] 获取第三方平台信息失败", e)
                    }
                    
                    // 修复：移除过早的回调，只在获得奖励或出错时才回调
                }
                
                override fun onAdVideoBarClick() {
                    Log.d(TAG, "🖱️ [激励视频] 广告被点击")
                }
                
                override fun onAdClose() {
                    Log.d(TAG, "❌ [激励视频] 广告关闭")
                    rewardVideoAd = null
                    
                    // 修复：确保在广告关闭时必须有回调，避免签到页面无回调问题
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        Log.w(TAG, "⚠️ [激励视频] 广告关闭，用户未获得奖励")
                        callback(false, false, "广告未完整观看")
                    }
                }
                
                override fun onVideoComplete() {
                    Log.d(TAG, "✅ [激励视频] 视频播放完成")
                }
                
                override fun onVideoError() {
                    Log.e(TAG, "❌ [激励视频] 视频播放错误")
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        callback(false, false, "广告播放错误")
                    }
                }
                
                override fun onRewardVerify(rewardVerify: Boolean, rewardAmount: Int, rewardName: String?, errorCode: Int, errorMsg: String?) {
                    Log.d(TAG, "🎁 [激励视频] 奖励验证: verify=$rewardVerify, amount=$rewardAmount, name=$rewardName")
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        if (rewardVerify) {
                            callback(true, true, "广告展示成功，获得奖励")
                        } else {
                            callback(true, false, "广告未完整观看")
                        }
                    }
                }
                
                override fun onSkippedVideo() {
                    Log.d(TAG, "⏭️ [激励视频] 视频被跳过")
                }
                
                override fun onRewardArrived(p0: Boolean, p1: Int, p2: Bundle?) {
                    Log.d(TAG, "🎁 [激励视频] 奖励到达: verify=$p0, amount=$p1")
                }
            })
            
            ad.showRewardVideoAd(activity)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [激励视频] 展示异常", e)
            callback(false, false, "广告展示异常: ${e.message}")
        }
    }
    
    // ==================== Banner广告 ====================
    
    override fun loadBannerAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [Banner广告] 开始加载")
        
        if (!isInitialized || ttAdNative == null) {
            Log.e(TAG, "❌ [Banner广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            // 使用配置化的AdSlot创建方法
            val adSlotConfig = AdConfig.Chuanshanjia.AdSlotConfigs.BANNER
            // 获取最新的服务器配置ID
            val serverBannerId = AdConfig.Chuanshanjia.AdUnitId.BANNER
            
            Log.d(TAG, "🔧 [Banner广告] 使用竞价类型: ${adSlotConfig.biddingType}")
            Log.w(TAG, "⚠️ [Banner广告] 广告位ID: ${adSlotConfig.adUnitId}；最新的服务器配置ID=${serverBannerId}")

            val adSlot = AdSlot.Builder()
                .setCodeId(serverBannerId)
                .setImageAcceptedSize(300, 75) // 单位px
                .setAdLoadType(TTAdLoadType.PRELOAD)
                .setExpressViewAcceptedSize(350f, 0f)
                .build()

            ttAdNative?.loadBannerExpressAd(adSlot, object : TTAdNative.NativeExpressAdListener {
                override fun onError(code: Int, message: String) {
                    Log.e(TAG, "❌ [Banner广告] 加载失败: code=$code, message=$message")
                    callback(false, "Banner广告加载失败: $message (code: $code)")
                }

                override fun onNativeExpressAdLoad(ads: MutableList<TTNativeExpressAd>?) {
                    if (ads != null && ads.isNotEmpty()) {
                        Log.i(TAG, "✅ [Banner广告] 加载成功")
                        bannerAd = ads[0]
                        callback(true, "Banner广告加载成功")
                    } else {
                        Log.e(TAG, "❌ [Banner广告] 加载失败: 广告列表为空")
                        callback(false, "Banner广告加载失败: 广告列表为空")
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Banner广告] 加载异常", e)
            callback(false, "Banner广告加载异常: ${e.message}")
        }
    }
    
    override fun getBannerAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎨 [Banner广告] 获取广告视图")
        
        val ad = bannerAd
        if (ad == null) {
            Log.e(TAG, "❌ [Banner广告] 广告未加载，无法获取视图")
            callback(null, "Banner广告未加载")
            return
        }
        
        try {
            ad.setExpressInteractionListener(object : TTNativeExpressAd.ExpressAdInteractionListener {
                override fun onAdClicked(view: android.view.View?, type: Int) {
                    Log.d(TAG, "🖱️ [Banner广告] 广告被点击")
                }
                
                override fun onAdShow(view: android.view.View?, type: Int) {
                    Log.d(TAG, "👁️ [Banner广告] 广告展示")
                    
                    // 获取第三方广告平台信息
                    try {
                        val mediationManager = ad.mediationManager
                        val ecpmList = mediationManager?.showEcpm
                        Log.i(TAG, "📊 [开屏广告] 第三方广告平台: ${ecpmList?.sdkName}")
//                        if (ecpmList != null) {
//                            val size = ecpmList.size
//                            for (i in 0 until size) {
//                                val ecpmInfo = ecpmList[i]
//                                Log.d(TAG, "📊 [Banner广告] 第三方广告平台: ${ecpmInfo.sdkName}")
//                                logEcpmInfo(ecpmInfo)
//                            }
//                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [Banner广告] 获取第三方平台信息失败", e)
                    }
                }
                
                override fun onRenderFail(view: android.view.View?, msg: String?, code: Int) {
                    Log.e(TAG, "❌ [Banner广告] 渲染失败: $msg (code: $code)")
                }
                
                override fun onRenderSuccess(view: android.view.View?, width: Float, height: Float) {
                    Log.d(TAG, "✅ [Banner广告] 渲染成功")
                }
            })
            
            ad.render()
            val bannerView = ad.expressAdView
            if (bannerView != null) {
                Log.i(TAG, "✅ [Banner广告] 获取视图成功")
                callback(bannerView as? ViewGroup, "Banner广告视图获取成功")
            } else {
                Log.e(TAG, "❌ [Banner广告] 广告视图为空")
                callback(null, "Banner广告视图为空")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Banner广告] 获取视图异常", e)
            callback(null, "Banner广告视图获取异常: ${e.message}")
        }
    }

    // ==================== Braw广告 ====================

    override fun loadDrawAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [Draw广告] 开始加载")

        if (!isInitialized || ttAdNative == null) {
            Log.e(TAG, "❌ [Draw广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }

        try {
            // 使用配置化的AdSlot创建方法
            val adSlotConfig = AdConfig.Chuanshanjia.AdSlotConfigs.DRAW_VIDEO
            val adSlot = createAdSlot(adSlotConfig, context)

            Log.d(TAG, "🔧 [Draw广告] 使用竞价类型: ${adSlotConfig.biddingType}")

            ttAdNative?.loadDrawFeedAd(adSlot, object : TTAdNative.DrawFeedAdListener {
                override fun onError(code: Int, message: String?) {
                    Log.e(TAG, "❌ [Draw广告] 加载失败: code=$code, message=$message")
                    callback(false, "Draw广告加载失败: $message (code: $code)")
                }

                override fun onDrawFeedAdLoad(ads: MutableList<TTDrawFeedAd>?) {
                    if (ads != null && ads.isNotEmpty()) {
                        Log.i(TAG, "✅ [Draw广告] 加载成功")
                        drawAd = ads[0]
                        callback(true, "Draw广告加载成功")
                    } else {
                        Log.e(TAG, "❌ [Draw广告] 加载失败: 广告列表为空")
                        callback(false, "Draw广告加载失败: 广告列表为空")
                    }
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "💥 [Draw广告] 加载异常", e)
            callback(false, "Draw广告加载异常: ${e.message}")
        }
    }

    override fun getDrawAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎨 [Draw广告] 获取广告视图")

        val ad = drawAd
        if (ad == null) {
            Log.e(TAG, "❌ [Draw广告] 广告未加载，无法获取视图")
            callback(null, "Draw广告未加载")
            return
        }

        try {
            // 移除融合SDK相关代码，使用标准的TTDrawFeedAd渲染
            val drawView = ad.adView
            if (drawView != null) {
                Log.d(TAG, "✅ [Draw广告] 获取视图成功")
                callback(drawView as? ViewGroup, "Draw广告视图获取成功")
            } else {
                Log.e(TAG, "❌ [Draw广告] 广告视图为空")
                callback(null, "Draw广告视图为空")
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 [Draw广告] 获取视图异常", e)
            callback(null, "Draw广告视图获取异常: ${e.message}")
        }
    }
    
    // ==================== 隐私策略和融合配置方法 ====================
    
    /**
      * 获取隐私策略控制器
      */
     private fun getTTCustomController(): TTCustomController {
         return object : TTCustomController() {
             override fun isCanUseWifiState(): Boolean {
                 return super.isCanUseWifiState()
             }
             
             override fun getMacAddress(): String? {
                 return super.getMacAddress()
             }
             
             override fun isCanUseWriteExternal(): Boolean {
                 return super.isCanUseWriteExternal()
             }
             
             override fun getDevOaid(): String? {
                 return super.getDevOaid()
             }
             
             override fun isCanUseAndroidId(): Boolean {
                 return super.isCanUseAndroidId()
             }
             
             override fun getAndroidId(): String? {
                 return super.getAndroidId()
             }
             
             override fun getMediationPrivacyConfig(): MediationPrivacyConfig? {
                 return object : MediationPrivacyConfig() {
                     override fun isLimitPersonalAds(): Boolean {
                         return super.isLimitPersonalAds()
                     }
                     
                     override fun isProgrammaticRecommend(): Boolean {
                         return super.isProgrammaticRecommend()
                     }
                 }
             }
             
             override fun isCanUsePermissionRecordAudio(): Boolean {
                 return super.isCanUsePermissionRecordAudio()
             }
         }
     }
    
    /**
      * 获取用户信息配置（用于流量分组）
      */
     private fun getUserInfoForSegment(): MediationConfigUserInfoForSegment {
         val userInfo = MediationConfigUserInfoForSegment()
         userInfo.userId = "shb-user-123"
         userInfo.gender = MediationConfigUserInfoForSegment.GENDER_MALE
         userInfo.channel = "shb-channel"
         userInfo.subChannel = "shb-sub-channel"
         userInfo.age = 25
         userInfo.userValueGroup = "shb-user-value-group"
         
         val customInfos = hashMapOf<String, String>()
         customInfos["app_version"] = "1.1.7"
         customInfos["user_type"] = "normal"
         userInfo.customInfos = customInfos
         
         return userInfo
     }
    
    // ==================== 销毁方法 ====================
    
    override fun destroy() {
        Log.d(TAG, "🗑️ [穿山甲广告] 开始销毁资源")
        
        try {
            splashAd = null
            interstitialAd = null
            feedAd = null
            rewardVideoAd = null
            bannerAd = null
            ttAdNative = null
            
            Log.i(TAG, "✅ [穿山甲广告] 资源销毁完成")
        } catch (e: Exception) {
            Log.e(TAG, "💥 [穿山甲广告] 销毁异常", e)
        }
    }

    // ==================== 展示广告信息方法 LOG ====================
    // 移除logEcpmInfo方法，当前版本不支持融合SDK
    // fun logEcpmInfo(item: MediationAdEcpmInfo) { ... }

}