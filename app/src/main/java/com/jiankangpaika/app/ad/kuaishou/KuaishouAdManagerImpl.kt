package com.jiankangpaika.app.ad.kuaishou

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.ViewGroup
import com.kwad.sdk.api.KsAdSDK
import com.kwad.sdk.api.KsAdVideoPlayConfig
import com.kwad.sdk.api.KsBannerAd
import com.kwad.sdk.api.KsDrawAd
import com.kwad.sdk.api.KsFeedAd
import com.kwad.sdk.api.KsInterstitialAd
import com.kwad.sdk.api.KsLoadManager
import com.kwad.sdk.api.KsRewardVideoAd
import com.kwad.sdk.api.KsScene
import com.kwad.sdk.api.KsSplashScreenAd
import com.kwad.sdk.api.KsVideoPlayConfig
import com.kwad.sdk.api.SdkConfig
import com.jiankangpaika.app.ad.AdConfig
import com.jiankangpaika.app.ad.AdManager

/**
 * 快手广告管理器实现类
 * 实现AdManager接口，提供快手广告SDK的具体实现
 */
class KuaishouAdManagerImpl : AdManager {
    
    companion object {
        private const val TAG = "KuaishouAdManagerImpl"
        
        // 使用AdConfig中的配置
        private val APP_ID = AdConfig.Kuaishou.APP_ID
        private val APP_NAME = AdConfig.Kuaishou.APP_NAME
        private val SPLASH_AD_UNIT_ID = AdConfig.Kuaishou.AdUnitId.SPLASH
        private val BANNER_AD_UNIT_ID = AdConfig.Kuaishou.AdUnitId.BANNER
        private val REWARD_AD_UNIT_ID = AdConfig.Kuaishou.AdUnitId.REWARD_VIDEO
        private val INTERSTITIAL_AD_UNIT_ID = AdConfig.Kuaishou.AdUnitId.INTERSTITIAL
        private val FEED_AD_UNIT_ID = AdConfig.Kuaishou.AdUnitId.FEED
        private val DRAW_AD_UNIT_ID = AdConfig.Kuaishou.AdUnitId.DRAW_VIDEO
    }
    
    private var isInitialized = false
    private var splashAd: KsSplashScreenAd? = null
    private var interstitialAd: KsInterstitialAd? = null
    private var feedAd: KsFeedAd? = null
    private var rewardVideoAd: KsRewardVideoAd? = null
    private var drawAd: KsDrawAd? = null // 快手Draw广告对象
    
    override fun getPlatformName(): String = "快手广告"
    
    override fun isInitialized(): Boolean = isInitialized
    
    override fun initSDK(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🚀 [快手广告] 开始初始化SDK")
        
        if (isInitialized) {
            Log.i(TAG, "✅ [快手广告] SDK已经初始化，跳过重复初始化")
            callback(true, "SDK已初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 [快手广告] 创建SDK配置: APP_ID=$APP_ID, APP_NAME=$APP_NAME")
            val sdkConfig = SdkConfig.Builder()
                .appId(APP_ID)
                .appName(APP_NAME)
                .showNotification(AdConfig.Kuaishou.Config.SHOW_NOTIFICATION)
                .debug(AdConfig.Kuaishou.Config.DEBUG_MODE)
                .build()
            
            Log.d(TAG, "🔧 [快手广告] 调用KsAdSDK.init进行初始化")
            val initResult = KsAdSDK.init(context, sdkConfig)
            if (initResult) {
                Log.d(TAG, "🔧 [快手广告] KsAdSDK.init成功，现在调用KsAdSDK.start方法")
                try {
                    KsAdSDK.start()
                    isInitialized = true
                    Log.i(TAG, "✅ [快手广告] SDK初始化和启动成功")
                    callback(true, "SDK初始化和启动成功")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [快手广告] SDK start方法调用失败", e)
                    callback(false, "SDK start方法调用失败: ${e.message}")
                }
            } else {
                Log.e(TAG, "❌ [快手广告] SDK初始化失败，init方法返回false")
                callback(false, "SDK初始化失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [快手广告] SDK初始化异常", e)
            callback(false, "SDK初始化异常: ${e.message}")
        }
    }
    
    // ==================== 开屏广告 ====================
    
    override fun loadSplashAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [开屏广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [开屏广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 [开屏广告] 创建广告场景: 广告位ID=$SPLASH_AD_UNIT_ID")
            val scene = KsScene.Builder(SPLASH_AD_UNIT_ID)
                .adNum(1)
                .build()
            
            Log.d(TAG, "🔧 [开屏广告] 调用loadSplashScreenAd开始加载")
            KsAdSDK.getLoadManager().loadSplashScreenAd(scene, object : KsLoadManager.SplashScreenAdListener {
                override fun onSplashScreenAdLoad(ad: KsSplashScreenAd?) {
                    if (ad != null) {
                        splashAd = ad
                        Log.i(TAG, "✅ [开屏广告] 加载成功")
                        callback(true, "开屏广告加载成功")
                    } else {
                        Log.w(TAG, "⚠️ [开屏广告] 加载成功但广告对象为空")
                        callback(false, "开屏广告为空")
                    }
                }
                
                override fun onError(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [开屏广告] 加载失败: code=$code, msg=$msg")
                    callback(false, "开屏广告加载失败: $msg")
                }
                
                override fun onRequestResult(code: Int) {
                    Log.d(TAG, "📡 [开屏广告] 请求结果: code=$code")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "💥 [开屏广告] 加载异常", e)
            callback(false, "加载开屏广告异常: ${e.message}")
        }
    }
    
    override fun showSplashAd(
        activity: Activity,
        container: ViewGroup,
        callback: (Boolean, String?) -> Unit
    ) {
        Log.d(TAG, "🎬 [开屏广告] 开始展示")
        
        val ad = splashAd
        if (ad == null) {
            Log.e(TAG, "❌ [开屏广告] 广告未加载或已失效")
            callback(false, "开屏广告未加载")
            return
        }
        
        try {
            Log.d(TAG, "🎬 [开屏广告] 调用getView展示广告")
            val view = ad.getView(activity, object : KsSplashScreenAd.SplashScreenAdInteractionListener {
                override fun onAdClicked() {
                    Log.d(TAG, "👆 [开屏广告] 用户点击广告")
                }
                
                override fun onAdShowError(errorCode: Int, errorMsg: String) {
                    Log.e(TAG, "❌ [开屏广告] 广告展示失败: errorCode=$errorCode, errorMsg=$errorMsg")
                    callback(false, "开屏广告展示失败: $errorMsg")
                }
                
                override fun onAdShowEnd() {
                    Log.d(TAG, "🏁 [开屏广告] 广告展示结束")
                }
                
                override fun onAdShowStart() {
                    Log.d(TAG, "🚀 [开屏广告] 广告开始展示")
                    callback(true, "开屏广告展示成功")
                }
                
                override fun onDownloadTipsDialogShow() {
                    Log.d(TAG, "📥 [开屏广告] 下载提示对话框显示")
                    callback(true, "开屏广告被跳过")
                }
                
                override fun onDownloadTipsDialogDismiss() {
                    Log.d(TAG, "📥 [开屏广告] 下载提示对话框关闭")
                    callback(true, "开屏广告被跳过")
                }
                
                override fun onDownloadTipsDialogCancel() {
                    Log.d(TAG, "📥 [开屏广告] 下载提示对话框取消")
                    callback(true, "开屏广告被跳过")
                }
                
                override fun onSkippedAd() {
                    Log.d(TAG, "⏭️ [开屏广告] 用户跳过广告")
                    splashAd = null // 清理广告对象
                    // 通知广告跳过，触发页面跳转到首页
                    callback(true, "开屏广告被跳过")
                }
            })
            
            if (view != null) {
                container.addView(view)
                Log.i(TAG, "🎉 [开屏广告] View已添加到容器")
            } else {
                Log.e(TAG, "❌ [开屏广告] View创建失败")
                callback(false, "开屏广告View创建失败")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [开屏广告] 展示异常", e)
            callback(false, "展示开屏广告异常: ${e.message}")
        }
    }
    
    // ==================== 插屏广告 ====================
    
    override fun loadInterstitialAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [插屏广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [插屏广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 [插屏广告] 创建广告场景: 广告位ID=$INTERSTITIAL_AD_UNIT_ID")
            val scene = KsScene.Builder(INTERSTITIAL_AD_UNIT_ID)
                .adNum(1)
                .build()
            
            Log.d(TAG, "🔧 [插屏广告] 调用loadInterstitialAd开始加载")
            KsAdSDK.getLoadManager().loadInterstitialAd(scene, object : KsLoadManager.InterstitialAdListener {
                override fun onInterstitialAdLoad(adList: MutableList<KsInterstitialAd>?) {
                    if (!adList.isNullOrEmpty()) {
                        interstitialAd = adList[0]
                        Log.i(TAG, "✅ [插屏广告] 加载成功")
                        callback(true, "插屏广告加载成功")
                    } else {
                        Log.w(TAG, "⚠️ [插屏广告] 加载成功但广告列表为空")
                        callback(false, "插屏广告列表为空")
                    }
                }
                
                override fun onError(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [插屏广告] 加载失败: code=$code, msg=$msg")
                    callback(false, "插屏广告加载失败: $msg")
                }
                
                override fun onRequestResult(code: Int) {
                    Log.d(TAG, "📡 [插屏广告] 请求结果: code=$code")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "💥 [插屏广告] 加载异常", e)
            callback(false, "加载插屏广告异常: ${e.message}")
        }
    }
    
    override fun showInterstitialAd(activity: Activity, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🎬 [插屏广告] 开始展示")
        
        val ad = interstitialAd
        if (ad == null) {
            Log.e(TAG, "❌ [插屏广告] 广告未加载或已失效")
            callback(false, "插屏广告未加载")
            return
        }
        
        try {
            Log.d(TAG, "🎬 [插屏广告] 调用showInterstitialAd展示广告")
            val playConfig = KsVideoPlayConfig.Builder().build()
            ad.showInterstitialAd(activity, playConfig)
            ad.setAdInteractionListener(object : KsInterstitialAd.AdInteractionListener {
                override fun onAdClicked() {
                    Log.d(TAG, "👆 [插屏广告] 用户点击广告")
                }
                
                override fun onAdShow() {
                    Log.i(TAG, "🎉 [插屏广告] 广告展示成功")
                    callback(true, "插屏广告展示成功")
                }
                
                override fun onAdClosed() {
                    Log.d(TAG, "🚪 [插屏广告] 广告关闭")
                    interstitialAd = null // 清理广告对象
                }
                
                override fun onPageDismiss() {
                    Log.d(TAG, "📄 [插屏广告] 页面消失")
                }
                
                override fun onVideoPlayError(code: Int, extra: Int) {
                    Log.e(TAG, "🎥 [插屏广告] 视频播放错误: code=$code, extra=$extra")
                }
                
                override fun onVideoPlayEnd() {
                    Log.d(TAG, "🎥 [插屏广告] 视频播放结束")
                }
                
                override fun onVideoPlayStart() {
                    Log.d(TAG, "🎥 [插屏广告] 视频播放开始")
                }
                
                override fun onSkippedAd() {
                    Log.d(TAG, "⏭️ [插屏广告] 用户跳过广告")
                    // 通知广告跳过，触发相应的处理逻辑
                    callback(true, "插屏广告被跳过")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "💥 [插屏广告] 展示异常", e)
            callback(false, "展示插屏广告异常: ${e.message}")
        }
    }
    
    // ==================== 信息流广告 ====================
    
    override fun loadFeedAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [信息流广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [信息流广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 [信息流广告] 创建广告场景: 广告位ID=$FEED_AD_UNIT_ID")
            val scene = KsScene.Builder(FEED_AD_UNIT_ID)
                .adNum(1)
                .build()
            
            Log.d(TAG, "🔧 [信息流广告] 调用loadFeedAd开始加载")
            KsAdSDK.getLoadManager().loadFeedAd(scene, object : KsLoadManager.FeedAdListener {
                override fun onFeedAdLoad(adList: MutableList<KsFeedAd>?) {
                    if (!adList.isNullOrEmpty()) {
                        feedAd = adList[0]
                        Log.i(TAG, "✅ [信息流广告] 加载成功")
                        callback(true, "信息流广告加载成功")
                    } else {
                        Log.w(TAG, "⚠️ [信息流广告] 加载成功但广告列表为空")
                        callback(false, "信息流广告列表为空")
                    }
                }
                
                override fun onError(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [信息流广告] 加载失败: code=$code, msg=$msg")
                    callback(false, "信息流广告加载失败: $msg")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "💥 [信息流广告] 加载异常", e)
            callback(false, "加载信息流广告异常: ${e.message}")
        }
    }
    
    override fun getFeedAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎬 [信息流广告] 获取广告视图")
        
        val ad = feedAd
        if (ad == null) {
            Log.e(TAG, "❌ [信息流广告] 广告未加载或已失效")
            callback(null, "信息流广告未加载")
            return
        }
        
        try {
            Log.d(TAG, "🎬 [信息流广告] 调用getFeedView获取广告视图")
            val adView = ad.getFeedView(context)
            if (adView != null) {
                Log.i(TAG, "✅ [信息流广告] 获取广告视图成功")
                callback(adView as ViewGroup, "获取信息流广告视图成功")
            } else {
                Log.w(TAG, "⚠️ [信息流广告] 获取广告视图失败，视图为空")
                callback(null, "信息流广告视图为空")
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [信息流广告] 获取广告视图异常", e)
            callback(null, "获取信息流广告视图异常: ${e.message}")
        }
    }
    
    // ==================== 激励视频广告 ====================
    
    override fun loadRewardVideoAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [激励视频广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [激励视频广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            Log.d(TAG, "🔧 [激励视频广告] 创建广告场景: 广告位ID=$REWARD_AD_UNIT_ID")
            val scene = KsScene.Builder(REWARD_AD_UNIT_ID.toLong())
                .build()
            
            Log.d(TAG, "🔧 [激励视频广告] 调用loadRewardVideoAd开始加载")
            KsAdSDK.getLoadManager().loadRewardVideoAd(scene, object : KsLoadManager.RewardVideoAdListener {
                override fun onRewardVideoAdLoad(adList: MutableList<KsRewardVideoAd>?) {
                    if (!adList.isNullOrEmpty()) {
                        rewardVideoAd = adList[0]
                        Log.i(TAG, "✅ [激励视频广告] 加载成功")
                        callback(true, "激励视频广告加载成功")
                    } else {
                        Log.w(TAG, "⚠️ [激励视频广告] 加载成功但广告列表为空")
                        callback(false, "激励视频广告列表为空")
                    }
                }
                
                override fun onError(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [激励视频广告] 加载失败: code=$code, msg=$msg")
                    callback(false, "激励视频广告加载失败: $msg")
                }
                
                override fun onRewardVideoResult(adList: MutableList<KsRewardVideoAd>?) {
                    Log.d(TAG, "📡 [激励视频广告] 请求结果: adList size=${adList?.size ?: 0}")
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "💥 [激励视频广告] 加载异常", e)
            callback(false, "加载激励视频广告异常: ${e.message}")
        }
    }
    
    override fun showRewardVideoAd(activity: Activity, callback: (Boolean, Boolean, String?) -> Unit) {
        Log.d(TAG, "🎬 [激励视频广告] 开始展示")
        
        val ad = rewardVideoAd
        if (ad == null) {
            Log.e(TAG, "❌ [激励视频广告] 广告未加载或已失效")
            callback(false, false, "广告未加载")
            return
        }
        
        try {
            Log.d(TAG, "🎬 [激励视频广告] 调用showRewardVideoAd展示广告")
            val playConfig = KsVideoPlayConfig.Builder().build()
            var hasRewarded = false
            var hasCallbackCalled = false
            
            ad.setRewardAdInteractionListener(object : KsRewardVideoAd.RewardAdInteractionListener {
                override fun onAdClicked() {
                    Log.d(TAG, "👆 [激励视频广告] 用户点击广告")
                }
                
                override fun onPageDismiss() {
                    Log.d(TAG, "📄 [激励视频广告] 页面消失")
                    rewardVideoAd = null // 清理广告对象
                    
                    // 修复：确保在页面关闭时必须有回调，避免签到页面无回调问题
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        if (hasRewarded) {
                            Log.i(TAG, "✅ [激励视频广告] 页面关闭，用户已获得奖励")
                            callback(true, true, "广告展示成功，获得奖励")
                        } else {
                            Log.w(TAG, "⚠️ [激励视频广告] 页面关闭，用户未获得奖励")
                            callback(false, false, "广告未完整观看")
                        }
                    }
                }
                
                override fun onRewardVerify() {
                    Log.i(TAG, "🎁 [激励视频广告] 奖励验证成功")
                    hasRewarded = true
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        callback(true, true, "广告展示成功，获得奖励")
                    }
                }
                
                override fun onRewardVerify(rewardInfo: MutableMap<String, Any>) {
                    Log.i(TAG, "🎁 [激励视频广告] 奖励验证成功（带参数）")
                    hasRewarded = true
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        callback(true, true, "广告展示成功，获得奖励")
                    }
                }
                
                override fun onExtraRewardVerify(extraReward: Int) {
                    Log.d(TAG, "🎁 [激励视频广告] 额外奖励验证: extraReward=$extraReward")
                }
                
                override fun onRewardStepVerify(taskType: Int, currentTaskStatus: Int) {
                    Log.d(TAG, "🎯 [激励视频广告] 奖励步骤验证: taskType=$taskType, status=$currentTaskStatus")
                }
                
                override fun onVideoPlayError(code: Int, extra: Int) {
                    Log.e(TAG, "🎥 [激励视频广告] 视频播放错误: code=$code, extra=$extra")
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        callback(false, false, "广告播放错误")
                    }
                }
                
                override fun onVideoPlayEnd() {
                    Log.d(TAG, "🎥 [激励视频广告] 视频播放结束")
                }
                
                override fun onVideoPlayStart() {
                    Log.d(TAG, "🎥 [激励视频广告] 视频播放开始")
                    // 修复：移除过早的回调，只在获得奖励或出错时才回调
                }
                
                override fun onVideoSkipToEnd(playDuration: Long) {
                    Log.d(TAG, "⏭️ [激励视频广告] 视频跳转到结束: playDuration=$playDuration")
                }
            })
            
            // 在设置监听器后调用展示广告
            ad.showRewardVideoAd(activity, playConfig)
        } catch (e: Exception) {
            Log.e(TAG, "💥 [激励视频广告] 展示异常", e)
            callback(false, false, "广告展示异常: ${e.message}")
        }
    }
    
    // ==================== Banner广告 ====================
    
    private var bannerAd: KsBannerAd? = null
    
    override fun loadBannerAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [Banner广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [Banner广告] SDK未初始化")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            // 构建Banner广告请求场景
            val scene = KsScene.Builder(AdConfig.Kuaishou.AdUnitId.BANNER)
                .screenOrientation(SdkConfig.SCREEN_ORIENTATION_PORTRAIT)
                .build()
            
            Log.d(TAG, "🔧 [Banner广告] 创建广告场景: 广告位ID=${AdConfig.Kuaishou.AdUnitId.BANNER}")
            
            // 加载Banner广告
            KsAdSDK.getLoadManager().loadBannerAd(scene, object : KsLoadManager.BannerAdListener {
                override fun onError(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [Banner广告] 加载失败: code=$code, msg=$msg")
                    callback(false, "Banner广告加载失败: $msg")
                }
                
                override fun onBannerAdLoad(bannerAd: KsBannerAd?) {
                    if (bannerAd != null) {
                        Log.i(TAG, "✅ [Banner广告] 加载成功")
                        this@KuaishouAdManagerImpl.bannerAd = bannerAd
                        callback(true, "Banner广告加载成功")
                    } else {
                        Log.e(TAG, "❌ [Banner广告] 加载失败: 广告对象为空")
                        callback(false, "Banner广告对象为空")
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Banner广告] 加载异常: ${e.message}")
            callback(false, "Banner广告加载异常: ${e.message}")
        }
    }
    
    override fun getBannerAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎬 [Banner广告] 获取广告视图")
        
        val currentBannerAd = bannerAd
        if (currentBannerAd == null) {
            Log.w(TAG, "⚠️ [Banner广告] 广告未加载或已失效")
            callback(null, "Banner广告未加载")
            return
        }
        
        try {
            // 创建视频播放配置
            val videoPlayConfig = KsAdVideoPlayConfig.Builder()
                .videoSoundEnable(false) // 默认静音播放
                .build()
            
            // 创建Banner广告交互监听器
            val interactionListener = object : KsBannerAd.BannerAdInteractionListener {
                override fun onAdClicked() {
                    Log.i(TAG, "🎯 [Banner广告] 广告被点击")
                }
                
                override fun onAdShow() {
                    Log.i(TAG, "👁️ [Banner广告] 广告开始展示")
                }
                
                override fun onAdClose() {
                    Log.i(TAG, "❌ [Banner广告] 广告被关闭")
                }
                
                override fun onAdShowError(code: Int, msg: String?) {
                    Log.e(TAG, "❌ [Banner广告] 展示错误: code=$code, msg=$msg")
                }
            }
            
            // 获取Banner广告视图
            val adView = currentBannerAd.getView(context, interactionListener, videoPlayConfig)
            
            if (adView != null) {
                Log.i(TAG, "✅ [Banner广告] 广告视图创建成功")
                callback(adView as? ViewGroup, "Banner广告视图创建成功")
            } else {
                Log.e(TAG, "❌ [Banner广告] 广告视图创建失败")
                callback(null, "Banner广告视图创建失败")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Banner广告] 获取视图异常: ${e.message}")
            callback(null, "Banner广告视图异常: ${e.message}")
        }
    }
    
    // ==================== Draw广告 ====================
    
    override fun loadDrawAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🎬 [Draw广告] 开始加载Draw广告")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [Draw广告] SDK未初始化")
            callback(false, "快手SDK未初始化")
            return
        }
        
        try {
            // 快手Draw广告加载
            val scene = KsScene.Builder(DRAW_AD_UNIT_ID.toLong())
                .adNum(1)
                .build()
            
            KsAdSDK.getLoadManager().loadDrawAd(scene, object : KsLoadManager.DrawAdListener {
                override fun onError(errorCode: Int, errorMsg: String?) {
                    Log.e(TAG, "❌ [Draw广告] 加载失败: code=$errorCode, msg=$errorMsg")
                    callback(false, "Draw广告加载失败: $errorMsg")
                }
                
                override fun onDrawAdLoad(adList: MutableList<KsDrawAd>?) {
                    if (adList != null && adList.isNotEmpty()) {
                        drawAd = adList[0]
                        Log.d(TAG, "✅ [Draw广告] 加载成功")
                        callback(true, "Draw广告加载成功")
                    } else {
                        Log.e(TAG, "❌ [Draw广告] 广告列表为空")
                        callback(false, "Draw广告列表为空")
                    }
                }
            })
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Draw广告] 加载异常: ${e.message}")
            callback(false, "Draw广告加载异常: ${e.message}")
        }
    }
    
    override fun getDrawAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "📱 [Draw广告] 获取Draw广告视图")
        
        val currentDrawAd = drawAd
        if (currentDrawAd == null) {
            Log.e(TAG, "❌ [Draw广告] 广告未加载")
            callback(null, "Draw广告未加载，请先调用loadDrawAd")
            return
        }
        
        try {
            val adView = currentDrawAd.getDrawView(context)
            if (adView != null) {
                Log.d(TAG, "✅ [Draw广告] 视图获取成功")
                callback(adView as? ViewGroup, "Draw广告视图获取成功")
            } else {
                Log.e(TAG, "❌ [Draw广告] 广告视图为空")
                callback(null, "Draw广告视图为空")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Draw广告] 获取视图异常: ${e.message}")
            callback(null, "Draw广告视图异常: ${e.message}")
        }
    }
    
    // ==================== 资源清理 ====================
    
    override fun destroy() {
        Log.d(TAG, "🗑️ [快手广告] 销毁广告资源")
        splashAd = null
        interstitialAd = null
        feedAd = null
        rewardVideoAd = null
        bannerAd = null
        drawAd = null
        isInitialized = false
    }
}