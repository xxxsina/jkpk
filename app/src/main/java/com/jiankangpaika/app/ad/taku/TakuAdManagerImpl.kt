package com.jiankangpaika.app.ad.taku

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.anythink.core.api.ATSDK
import com.anythink.core.api.AdError
import com.anythink.core.api.ATAdInfo
import com.anythink.splashad.api.ATSplashAd
import com.anythink.splashad.api.ATSplashAdListener
import com.anythink.interstitial.api.ATInterstitial
import com.anythink.interstitial.api.ATInterstitialListener
import com.anythink.rewardvideo.api.ATRewardVideoAd
import com.anythink.rewardvideo.api.ATRewardVideoListener
import com.anythink.banner.api.ATBannerView
import com.anythink.banner.api.ATBannerListener
import com.anythink.nativead.api.ATNative
import com.anythink.nativead.api.ATNativeNetworkListener
import com.anythink.nativead.api.ATNativeView
import com.anythink.nativead.api.ATNativeAdView
import com.anythink.nativead.api.ATNativeEventListener
import com.anythink.nativead.api.ATNativeDislikeListener
import com.anythink.nativead.api.ATNativePrepareInfo
import com.anythink.nativead.api.ATNativePrepareExInfo
import com.anythink.nativead.api.NativeAd
import com.anythink.core.api.ATAdConst
import com.jiankangpaika.app.ad.TakuAdManager
import com.jiankangpaika.app.ad.AdConfig
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ad.callback.*

/**
 * Taku广告管理器实现
 * 基于AnyThink SDK实现各种广告类型的加载和展示
 */
class TakuAdManagerImpl : TakuAdManager {
    
    companion object {
        private const val TAG = "TakuAdManager"
    }
    
    private var isInitialized = false
    private var appContext: Context? = null
    
    // 广告实例
    private var splashAd: ATSplashAd? = null
    private var interstitialAd: ATInterstitial? = null
    private var rewardVideoAd: ATRewardVideoAd? = null
    private var bannerView: ATBannerView? = null
    private var feedNativeAd: ATNative? = null
    private var feedAdView: NativeAd? = null
    
    override fun initSDK(context: Context, callback: (Boolean, String?) -> Unit) {
        initSDK(context, 
            onInitSuccess = { callback(true, "Taku SDK初始化成功") },
            onInitFail = { error -> callback(false, error) }
        )
    }
    
    /**
     * 初始化Taku广告SDK（带AdInitCallback）
     */
    override fun initSDK(context: Context, callback: AdInitCallback?) {
        initSDK(context, 
            onInitSuccess = { callback?.onInitSuccess() },
            onInitFail = { error -> callback?.onInitFailed(error) }
        )
    }
    
    /**
     * 初始化Taku广告SDK（内部实现）
     */
    private fun initSDK(context: Context, onInitSuccess: () -> Unit, onInitFail: (String) -> Unit) {
        try {
            appContext = context.applicationContext
            
            val takuConfig = AdConfig.getTakuConfig()
            val appId = takuConfig["APP_ID"] as? String
            val appKey = takuConfig["APP_KEY"] as? String
            
            if (appId.isNullOrEmpty() || appKey.isNullOrEmpty()) {
                Log.e(TAG, "Taku SDK初始化失败: APP_ID或APP_KEY为空")
                onInitFail("APP_ID或APP_KEY为空")
                return
            }
            
            Log.d(TAG, "开始初始化Taku SDK, APP_ID: $appId")
            
            // 设置调试模式
            ATSDK.setNetworkLogDebug(AdConfig.DEBUG_MODE)
            
            // 设置个性化广告状态
            ATSDK.setPersonalizedAdStatus(ATSDK.PERSONALIZED)
            
            // 初始化SDK
            ATSDK.init(context, appId, appKey)
            
            // Taku SDK初始化是同步的，但需要一些时间才能真正可用
            // 添加延迟确保SDK完全初始化
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isInitialized = true
                Log.d(TAG, "Taku SDK初始化完成")
                onInitSuccess()
            }, 500) // 延迟500ms确保SDK完全初始化
            
        } catch (e: Exception) {
            Log.e(TAG, "Taku SDK初始化异常", e)
            onInitFail("初始化异常: ${e.message}")
        }
    }
    
    override fun getPlatformName(): String = "taku"
    
    override fun isInitialized(): Boolean = isInitialized
    
    /**
     * 实现AdManager接口的loadSplashAd方法
     */
    override fun loadSplashAd(context: Context, callback: (Boolean, String?) -> Unit) {
         loadSplashAdInternal(context, object : SplashAdCallback {
             override fun onAdLoaded() {
                 callback(true, "开屏广告加载成功")
             }
             
             override fun onAdLoadFailed(error: String) {
                 callback(false, error)
             }
             
             override fun onAdShowed() {}
             override fun onAdClicked() {}
             override fun onAdClosed() {}
         })
     }
     
     /**
      * 加载开屏广告（TakuAdManager接口实现）
      */
     override fun loadSplashAd(context: Context, callback: SplashAdCallback?) {
         loadSplashAdInternal(context, callback)
     }
     
     /**
      * 加载开屏广告（内部实现）
      * 优化：统一配置管理，改进日志输出和错误处理
      */
     private fun loadSplashAdInternal(context: Context, callback: SplashAdCallback?) {
        Log.d(TAG, "🔍 [Taku开屏广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [Taku开屏广告] SDK未初始化，无法加载")
            callback?.onAdLoadFailed("SDK未初始化")
            return
        }
        
        try {
            val placementId = AdConfig.getTakuSplashAdUnitId()
            if (placementId.isEmpty()) {
                Log.e(TAG, "❌ [Taku开屏广告] 广告位ID为空")
                callback?.onAdLoadFailed("广告位ID为空")
                return
            }
            
            val timeoutMs = AdConfig.Taku.Config.SPLASH_TIMEOUT_MS.toInt()
            Log.d(TAG, "🔧 [Taku开屏广告] 创建广告实例: placementId=$placementId, timeout=${timeoutMs}ms")
            
            // 创建开屏广告实例
            splashAd = ATSplashAd(context, placementId, object : ATSplashAdListener {
                override fun onAdLoaded(isTimeout: Boolean) {
                    if (!isTimeout) {
                        Log.i(TAG, "✅ [Taku开屏广告] 加载成功")
                        callback?.onAdLoaded()
                    } else {
                        Log.w(TAG, "⚠️ [Taku开屏广告] 加载成功但已超时")
                        callback?.onAdLoadFailed("加载超时")
                    }
                }
                
                override fun onAdLoadTimeout() {
                    Log.e(TAG, "❌ [Taku开屏广告] 加载超时")
                    callback?.onAdLoadFailed("加载超时")
                }
                
                override fun onNoAdError(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "无广告"
                    val errorCode = adError?.code ?: -1
                    Log.e(TAG, "❌ [Taku开屏广告] 加载失败: code=$errorCode, message=$errorMsg")
                    callback?.onAdLoadFailed("$errorMsg (code: $errorCode)")
                }
                
                override fun onAdShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku开屏广告] 开始展示")
                    callback?.onAdShowed()
                }
                
                override fun onAdClick(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👆 [Taku开屏广告] 用户点击")
                    callback?.onAdClicked()
                }
                
                override fun onAdDismiss(atAdInfo: ATAdInfo?, extraInfo: com.anythink.splashad.api.ATSplashAdExtraInfo?) {
                    val dismissType = extraInfo?.dismissType
                    Log.d(TAG, "🔚 [Taku开屏广告] 广告关闭, dismissType: $dismissType")
                    callback?.onAdClosed()
                }
            }, timeoutMs)
            
            // 开始加载广告
            Log.d(TAG, "🚀 [Taku开屏广告] 开始请求广告")
            splashAd?.loadAd()
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Taku开屏广告] 加载异常", e)
            callback?.onAdLoadFailed("加载异常: ${e.message}")
        }
    }
    
    /**
     * 实现AdManager接口的showSplashAd方法
     */
    override fun showSplashAd(activity: Activity, container: ViewGroup, callback: (Boolean, String?) -> Unit) {
        showSplashAdInternal(activity, container, callback, object : SplashAdCallback {
            override fun onAdLoaded() {}
            override fun onAdLoadFailed(error: String) {
                callback(false, error)
            }
            override fun onAdShowed() {
                callback(true, "开屏广告展示成功")
            }
            override fun onAdClicked() {}
            override fun onAdClosed() {}
        })
    }
    
    /**
     * 展示开屏广告（TakuAdManager接口实现）
     * 优化：复用AdManager接口实现，避免代码重复
     */
    override fun showSplashAd(activity: Activity, container: ViewGroup?, callback: SplashAdCallback?) {
        if (container == null) {
            Log.e(TAG, "❌ [Taku开屏广告] 容器为空，无法展示")
            callback?.onAdLoadFailed("容器为空")
            return
        }
        
        // 直接调用内部实现，不需要跳转回调
        showSplashAdInternal(activity, container, null, callback)
    }
    
    /**
     * 展示开屏广告（内部实现）
     * 优化：改进回调处理逻辑，参考穿山甲实现
     */
    private fun showSplashAdInternal(activity: Activity, container: ViewGroup, jumpCallback: ((Boolean, String?) -> Unit)?, callback: SplashAdCallback?) {
        try {
            val ad = splashAd
            if (ad == null) {
                Log.e(TAG, "❌ [Taku开屏广告] 广告实例为空，无法展示")
                callback?.onAdLoadFailed("广告实例为空")
                return
            }
            
            if (!ad.isAdReady()) {
                Log.e(TAG, "❌ [Taku开屏广告] 广告未准备好，无法展示")
                callback?.onAdLoadFailed("广告未准备好")
                return
            }
            
            // 保存callback引用，避免在监听器中被覆盖
            val showCallback = callback
            var hasCalledShowCallback = false
            
            // 重新设置监听器以处理展示时的回调
            ad.setAdListener(object : ATSplashAdListener {
                override fun onAdLoaded(isTimeout: Boolean) {
                    // 展示时不需要处理加载回调
                }
                
                override fun onAdLoadTimeout() {
                    // 展示时不需要处理加载超时
                }
                
                override fun onNoAdError(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "展示失败"
                    Log.e(TAG, "❌ [Taku开屏广告] 展示失败: $errorMsg")
                    if (!hasCalledShowCallback) {
                        hasCalledShowCallback = true
                        showCallback?.onAdLoadFailed(errorMsg)
                    }
                }
                
                override fun onAdShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku开屏广告] 广告展示成功")
                    if (!hasCalledShowCallback) {
                        hasCalledShowCallback = true
                        showCallback?.onAdShowed()
                    }
                }
                
                override fun onAdClick(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👆 [Taku开屏广告] 用户点击")
                    showCallback?.onAdClicked()
                }
                
                override fun onAdDismiss(atAdInfo: ATAdInfo?, extraInfo: com.anythink.splashad.api.ATSplashAdExtraInfo?) {
                    val dismissType = extraInfo?.dismissType
                    Log.d(TAG, "🔚 [Taku开屏广告] 广告关闭, dismissType: $dismissType")

                    // 调用跳转回调，实现跳转到首页
                    jumpCallback?.invoke(true, "开屏广告被跳过")

                    showCallback?.onAdClosed()
                }
            })
            
            Log.d(TAG, "🎬 [Taku开屏广告] 开始展示")
            ad.show(activity, container)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Taku开屏广告] 展示异常: ${e.message}", e)
            callback?.onAdLoadFailed("展示异常: ${e.message}")
        }
    }
    
    // 移除不存在的isSplashAdReady方法
    
    // ==================== 激励视频广告 ====================
    
    /**
     * 实现AdManager接口的loadRewardVideoAd方法
     */
    override fun loadRewardVideoAd(context: Context, callback: (Boolean, String?) -> Unit) {
        loadRewardVideoAdInternal(context, object : RewardVideoAdCallback {
            override fun onAdLoaded() {
                callback(true, "激励视频广告加载成功")
            }
            
            override fun onAdLoadFailed(error: String) {
                callback(false, error)
            }
            
            override fun onAdShowed() {}
            override fun onAdClicked() {}
            override fun onAdClosed() {}
            override fun onRewarded() {}
        })
    }
    
    /**
     * 加载激励视频广告（TakuAdManager接口实现）
     */
    override fun loadRewardVideoAd(context: Context, callback: RewardVideoAdCallback?) {
         loadRewardVideoAdInternal(context, callback)
     }
    
    /**
     * 加载激励视频广告（内部实现）
     * 基于Taku SDK激励视频广告接入最佳实践
     */
    private fun loadRewardVideoAdInternal(context: Context, callback: RewardVideoAdCallback?) {
        if (!isInitialized) {
            Log.e(TAG, "❌ [Taku激励视频] SDK未初始化，无法加载")
            callback?.onAdLoadFailed("SDK未初始化")
            return
        }
        
        try {
            val placementId = AdConfig.getTakuRewardVideoAdUnitId()
            if (placementId.isEmpty()) {
                Log.e(TAG, "❌ [Taku激励视频] 广告位ID为空")
                callback?.onAdLoadFailed("广告位ID为空")
                return
            }
            
            Log.d(TAG, "🔍 [Taku激励视频] 开始加载, placementId: $placementId")
            
            // 创建激励视频广告实例
            rewardVideoAd = ATRewardVideoAd(context, placementId)
            rewardVideoAd?.setAdListener(object : ATRewardVideoListener {
                override fun onRewardedVideoAdLoaded() {
                    Log.d(TAG, "✅ [Taku激励视频] 加载成功")
                    callback?.onAdLoaded()
                }
                
                override fun onRewardedVideoAdFailed(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "未知错误"
                    Log.e(TAG, "❌ [Taku激励视频] 加载失败: $errorMsg")
                    callback?.onAdLoadFailed(errorMsg)
                }
                
                override fun onRewardedVideoAdPlayStart(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku激励视频] 开始播放")
                    callback?.onAdShowed()
                }
                
                override fun onRewardedVideoAdPlayEnd(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🏁 [Taku激励视频] 播放结束")
                }
                
                override fun onRewardedVideoAdPlayFailed(adError: AdError?, atAdInfo: ATAdInfo?) {
                    val errorMsg = adError?.fullErrorInfo ?: "播放失败"
                    Log.e(TAG, "❌ [Taku激励视频] 播放失败: $errorMsg")
                }
                
                override fun onRewardedVideoAdClosed(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🔚 [Taku激励视频] 广告关闭")
                    callback?.onAdClosed()
                }
                
                override fun onRewardedVideoAdPlayClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👆 [Taku激励视频] 用户点击")
                    callback?.onAdClicked()
                }
                
                override fun onReward(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎁 [Taku激励视频] 获得奖励")
                    callback?.onRewarded()
                }
            })
            
            // 开始加载激励视频广告
            rewardVideoAd?.load()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Taku激励视频] 加载异常: ${e.message}", e)
            callback?.onAdLoadFailed("加载异常: ${e.message}")
        }
    }
    
    /**
      * 实现AdManager接口的showRewardVideoAd方法
      * 修改回调处理，确保签到按钮状态恢复
      */
     override fun showRewardVideoAd(activity: Activity, callback: (Boolean, Boolean, String?) -> Unit) {
        var hasCallbackCalled = false
        
        showRewardVideoAdInternal(activity, object : RewardVideoAdCallback {
             override fun onAdLoaded() {}
             override fun onAdLoadFailed(error: String) {
                 if (!hasCallbackCalled) {
                     hasCallbackCalled = true
                     callback(false, false, error)
                 }
             }
             override fun onAdShowed() {
                 // 只记录展示，不触发回调，避免过早更新UI状态
             }
             override fun onAdClicked() {}
             override fun onAdClosed() {
                 // 用户关闭广告但未获得奖励时，恢复签到按钮状态
                 if (!hasCallbackCalled) {
                     hasCallbackCalled = true
                     callback(false, false, "用户关闭广告，未获得奖励")
                 }
             }
             override fun onRewarded() {
                 // 用户获得奖励，自动加积分并更新UI
                 if (!hasCallbackCalled) {
                     hasCallbackCalled = true
                     callback(true, true, "用户完成观看，获得奖励")
                 }
             }
         })
    }
    
    /**
      * 展示激励视频广告（TakuAdManager接口实现）
      */
     override fun showRewardVideoAd(activity: Activity, callback: RewardVideoAdCallback?) {
         showRewardVideoAdInternal(activity, callback)
     }
    
    /**
     * 展示激励视频广告（内部实现）
     * 基于Taku SDK激励视频广告接入最佳实践
     * 参考ChuanshanjiaAdManagerImpl的回调处理逻辑
     */
    private fun showRewardVideoAdInternal(activity: Activity, callback: RewardVideoAdCallback?) {
        try {
            val ad = rewardVideoAd
            if (ad == null) {
                Log.e(TAG, "❌ [Taku激励视频] 广告实例为空，无法展示")
                callback?.onAdLoadFailed("广告实例为空")
                return
            }
            
            if (!ad.isAdReady()) {
                Log.e(TAG, "❌ [Taku激励视频] 广告未准备好，无法展示")
                callback?.onAdLoadFailed("广告未准备好")
                return
            }
            
            var hasCallbackCalled = false
            var hasRewarded = false
            
            // 重新设置监听器以处理展示时的回调
            ad.setAdListener(object : ATRewardVideoListener {
                override fun onRewardedVideoAdLoaded() {
                    // 展示时不需要处理加载回调
                }
                
                override fun onRewardedVideoAdFailed(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "未知错误"
                    Log.e(TAG, "❌ [Taku激励视频] 播放失败: $errorMsg")
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        callback?.onAdLoadFailed(errorMsg)
                    }
                }
                
                override fun onRewardedVideoAdPlayStart(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku激励视频] 开始播放")
                    callback?.onAdShowed()
                }
                
                override fun onRewardedVideoAdPlayEnd(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🏁 [Taku激励视频] 播放结束")
                }
                
                override fun onRewardedVideoAdPlayFailed(adError: AdError?, atAdInfo: ATAdInfo?) {
                    val errorMsg = adError?.fullErrorInfo ?: "播放失败"
                    Log.e(TAG, "❌ [Taku激励视频] 播放失败: $errorMsg")
                    
                    // 解析并提取platformMSG内容
                    val platformMsg = extractPlatformMsg(errorMsg)
                    val finalErrorMsg = platformMsg ?: errorMsg
                    
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        callback?.onAdLoadFailed(finalErrorMsg)
                    }
                }
                
                override fun onRewardedVideoAdClosed(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🔚 [Taku激励视频] 广告关闭")
                    rewardVideoAd = null // 清理广告对象
                    
                    // 修复：确保在广告关闭时必须有回调，避免签到页面无回调问题
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        if (hasRewarded) {
                            Log.i(TAG, "✅ [Taku激励视频] 广告关闭，用户已获得奖励")
                            callback?.onRewarded()
                        } else {
                            Log.w(TAG, "⚠️ [Taku激励视频] 广告关闭，用户未获得奖励")
                            callback?.onAdClosed()
                        }
                    } else {
                        // 如果已经回调过，只调用关闭回调
                        callback?.onAdClosed()
                    }
                }
                
                override fun onRewardedVideoAdPlayClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👆 [Taku激励视频] 用户点击")
                    callback?.onAdClicked()
                }
                
                override fun onReward(atAdInfo: ATAdInfo?) {
//                    Log.d(TAG, "🎁 [Taku激励视频] 奖励修改到 onRewardedVideoAdPlayEnd 播放结束时下发奖励")
                    Log.d(TAG, "🎁 [Taku激励视频] 奖励还是要在这里下发")
                    // 视频播放结束，但不立即回调，等待奖励验证
                    hasRewarded = true

                    // 视频播放结束时自动加积分
                    if (!hasCallbackCalled) {
                        hasCallbackCalled = true
                        Log.i(TAG, "✅ [Taku激励视频] 用户获得奖励，自动加积分")
                        callback?.onRewarded()
                    }
                }
            })
            
            Log.d(TAG, "🎬 [Taku激励视频] 开始展示")
            ad.show(activity)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Taku激励视频] 展示异常: ${e.message}", e)
            callback?.onAdLoadFailed("展示异常: ${e.message}")
        }
    }
    
    // 移除不存在的方法
    
    // ==================== 插屏广告 ====================
    
    /**
     * 实现AdManager接口的loadInterstitialAd方法
     */
    override fun loadInterstitialAd(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔍 [Taku插屏广告] 开始加载")
        
        if (!isInitialized) {
            Log.e(TAG, "❌ [Taku插屏广告] SDK未初始化，无法加载")
            callback(false, "SDK未初始化")
            return
        }
        
        try {
            val placementId = AdConfig.getTakuInterstitialAdUnitId()
            if (placementId.isEmpty()) {
                Log.e(TAG, "❌ [Taku插屏广告] 广告位ID为空")
                callback(false, "广告位ID为空")
                return
            }
            
            Log.d(TAG, "🔧 [Taku插屏广告] 使用广告位ID: $placementId")
            
            // 创建插屏广告实例
            interstitialAd = ATInterstitial(context, placementId)
            interstitialAd?.setAdListener(object : ATInterstitialListener {
                override fun onInterstitialAdLoaded() {
                    Log.i(TAG, "✅ [Taku插屏广告] 加载成功")
                    callback(true, "插屏广告加载成功")
                }
                
                override fun onInterstitialAdLoadFail(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "未知错误"
                    Log.e(TAG, "❌ [Taku插屏广告] 加载失败: $errorMsg")
                    callback(false, "插屏广告加载失败: $errorMsg")
                }
                
                override fun onInterstitialAdShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👁️ [Taku插屏广告] 广告展示")
                }
                
                override fun onInterstitialAdClose(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "❌ [Taku插屏广告] 广告关闭")
                    // 清空广告实例，下次需要重新加载
                    interstitialAd = null
                }
                
                override fun onInterstitialAdVideoStart(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku插屏广告] 视频开始播放")
                }
                
                override fun onInterstitialAdVideoEnd(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "✅ [Taku插屏广告] 视频播放完成")
                }
                
                override fun onInterstitialAdVideoError(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "视频播放错误"
                    Log.e(TAG, "❌ [Taku插屏广告] 视频播放错误: $errorMsg")
                }
                
                override fun onInterstitialAdClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🖱️ [Taku插屏广告] 广告被点击")
                }
            })
            
            // 开始加载插屏广告
            interstitialAd?.load()
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Taku插屏广告] 加载异常", e)
            callback(false, "插屏广告加载异常: ${e.message}")
        }
    }
    
    /**
     * 加载插屏广告（TakuAdManager接口实现）
     */
    override fun loadInterstitialAd(context: Context, callback: InterstitialAdCallback?) {
        loadInterstitialAdInternal(context, callback)
    }
    
    /**
     * 加载插屏广告（内部实现）
     */
    private fun loadInterstitialAdInternal(context: Context, callback: InterstitialAdCallback?) {
        if (!isInitialized) {
            Log.e(TAG, "❌ [Taku插屏广告] SDK未初始化，无法加载")
            callback?.onAdLoadFailed("SDK未初始化")
            return
        }
        
        try {
            val placementId = AdConfig.getTakuInterstitialAdUnitId()
            if (placementId.isEmpty()) {
                Log.e(TAG, "❌ [Taku插屏广告] 广告位ID为空")
                callback?.onAdLoadFailed("广告位ID为空")
                return
            }
            
            Log.d(TAG, "🔧 [Taku插屏广告] 使用广告位ID: $placementId")
            
            // 创建插屏广告实例
            interstitialAd = ATInterstitial(context, placementId)
            interstitialAd?.setAdListener(object : ATInterstitialListener {
                override fun onInterstitialAdLoaded() {
                    Log.i(TAG, "✅ [Taku插屏广告] 加载成功")
                    callback?.onAdLoaded()
                }
                
                override fun onInterstitialAdLoadFail(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "未知错误"
                    Log.e(TAG, "❌ [Taku插屏广告] 加载失败: $errorMsg")
                    callback?.onAdLoadFailed(errorMsg)
                }
                
                override fun onInterstitialAdShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👁️ [Taku插屏广告] 广告展示")
                    callback?.onAdShowed()
                }
                
                override fun onInterstitialAdClose(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "❌ [Taku插屏广告] 广告关闭")
                    callback?.onAdClosed()
                    // 清空广告实例，下次需要重新加载
                    interstitialAd = null
                }
                
                override fun onInterstitialAdVideoStart(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku插屏广告] 视频开始播放")
                }
                
                override fun onInterstitialAdVideoEnd(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "✅ [Taku插屏广告] 视频播放完成")
                }
                
                override fun onInterstitialAdVideoError(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "视频播放错误"
                    Log.e(TAG, "❌ [Taku插屏广告] 视频播放错误: $errorMsg")
                }
                
                override fun onInterstitialAdClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🖱️ [Taku插屏广告] 广告被点击")
                    callback?.onAdClicked()
                }
            })
            
            // 开始加载插屏广告
            interstitialAd?.load()
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Taku插屏广告] 加载异常", e)
            callback?.onAdLoadFailed("加载异常: ${e.message}")
        }
    }
    
    /**
     * 实现AdManager接口的showInterstitialAd方法
     */
    override fun showInterstitialAd(activity: Activity, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "📺 [Taku插屏广告] 开始展示")
        
        val ad = interstitialAd
        if (ad == null) {
            Log.e(TAG, "❌ [Taku插屏广告] 广告未加载，无法展示")
            callback(false, "插屏广告未加载")
            return
        }
        
        if (!ad.isAdReady()) {
            Log.e(TAG, "❌ [Taku插屏广告] 广告未准备好，无法展示")
            callback(false, "插屏广告未准备好")
            return
        }
        
        try {
            // 设置展示回调监听器
            ad.setAdListener(object : ATInterstitialListener {
                override fun onInterstitialAdLoaded() {
                    // 展示时不会触发此回调
                }
                
                override fun onInterstitialAdLoadFail(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "展示失败"
                    Log.e(TAG, "❌ [Taku插屏广告] 展示失败: $errorMsg")
                    callback(false, "插屏广告展示失败: $errorMsg")
                }
                
                override fun onInterstitialAdShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👁️ [Taku插屏广告] 广告展示成功")
                    callback(true, "插屏广告展示成功")
                }
                
                override fun onInterstitialAdClose(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "❌ [Taku插屏广告] 广告关闭")
                    // 清空广告实例，下次需要重新加载
                    interstitialAd = null
                }
                
                override fun onInterstitialAdVideoStart(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku插屏广告] 视频开始播放")
                }
                
                override fun onInterstitialAdVideoEnd(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "✅ [Taku插屏广告] 视频播放完成")
                }
                
                override fun onInterstitialAdVideoError(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "视频播放错误"
                    Log.e(TAG, "❌ [Taku插屏广告] 视频播放错误: $errorMsg")
                }
                
                override fun onInterstitialAdClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🖱️ [Taku插屏广告] 广告被点击")
                }
            })
            
            // 展示插屏广告
            ad.show(activity)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Taku插屏广告] 展示异常", e)
            callback(false, "插屏广告展示异常: ${e.message}")
        }
    }
    
    /**
     * 展示插屏广告（TakuAdManager接口实现）
     */
    override fun showInterstitialAd(activity: Activity, callback: InterstitialAdCallback?) {
        showInterstitialAdInternal(activity, callback)
    }
    
    /**
     * 展示插屏广告（内部实现）
     */
    private fun showInterstitialAdInternal(activity: Activity, callback: InterstitialAdCallback?) {
        Log.d(TAG, "📺 [Taku插屏广告] 开始展示（内部）")
        
        val ad = interstitialAd
        if (ad == null) {
            Log.e(TAG, "❌ [Taku插屏广告] 广告实例为空，无法展示")
            callback?.onAdLoadFailed("广告实例为空")
            return
        }
        
        if (!ad.isAdReady()) {
            Log.e(TAG, "❌ [Taku插屏广告] 广告未准备好，无法展示")
            callback?.onAdLoadFailed("广告未准备好")
            return
        }
        
        try {
            // 设置展示回调监听器
            ad.setAdListener(object : ATInterstitialListener {
                override fun onInterstitialAdLoaded() {
                    // 展示时不会触发此回调
                }
                
                override fun onInterstitialAdLoadFail(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "展示失败"
                    Log.e(TAG, "❌ [Taku插屏广告] 展示失败: $errorMsg")
                    callback?.onAdLoadFailed(errorMsg)
                }
                
                override fun onInterstitialAdShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👁️ [Taku插屏广告] 广告展示成功")
                    callback?.onAdShowed()
                }
                
                override fun onInterstitialAdClose(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "❌ [Taku插屏广告] 广告关闭")
                    callback?.onAdClosed()
                    // 清空广告实例，下次需要重新加载
                    interstitialAd = null
                }
                
                override fun onInterstitialAdVideoStart(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku插屏广告] 视频开始播放")
                }
                
                override fun onInterstitialAdVideoEnd(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "✅ [Taku插屏广告] 视频播放完成")
                }
                
                override fun onInterstitialAdVideoError(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "视频播放错误"
                    Log.e(TAG, "❌ [Taku插屏广告] 视频播放错误: $errorMsg")
                }
                
                override fun onInterstitialAdClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🖱️ [Taku插屏广告] 广告被点击")
                    callback?.onAdClicked()
                }
            })
            
            // 展示插屏广告
            ad.show(activity)
            
        } catch (e: Exception) {
            Log.e(TAG, "💥 [Taku插屏广告] 展示异常", e)
            callback?.onAdLoadFailed("展示异常: ${e.message}")
        }
    }
    
    // 移除不存在的方法
    
    // ==================== Banner广告 ====================
    
    /**
     * 实现AdManager接口的loadBannerAd方法
     */
    override fun loadBannerAd(context: Context, callback: (Boolean, String?) -> Unit) {
        loadBannerAdInternal(context, object : BannerAdCallback {
            override fun onAdLoaded(bannerView: ViewGroup?) {
                callback(true, "Banner广告加载成功")
            }
            
            override fun onAdLoadFailed(error: String) {
                callback(false, error)
            }
            
            override fun onAdShowed() {}
            override fun onAdClicked() {}
            override fun onAdClosed() {}
        })
    }
    
    /**
     * 加载Banner广告（TakuAdManager接口实现）
     */
    override fun loadBannerAd(context: Context, callback: BannerAdCallback?) {
        loadBannerAdInternal(context, callback)
    }
    
    /**
     * 加载Banner广告（内部实现）
     */
    private fun loadBannerAdInternal(context: Context, callback: BannerAdCallback?) {
        if (!isInitialized) {
            Log.e(TAG, "❌ [TakuBanner广告] SDK未初始化，无法加载")
            callback?.onAdLoadFailed("SDK未初始化")
            return
        }
        
        try {
            val placementId = AdConfig.getTakuBannerAdUnitId()
            if (placementId.isEmpty()) {
                Log.e(TAG, "❌ [TakuBanner广告] 广告位ID为空")
                callback?.onAdLoadFailed("广告位ID为空")
                return
            }
            
            Log.d(TAG, "🔍 [TakuBanner广告] 开始加载, placementId: $placementId")
            
            // 创建Banner广告视图
            bannerView = ATBannerView(context)
            bannerView?.setPlacementId(placementId)
            bannerView?.setBannerAdListener(object : ATBannerListener {
                override fun onBannerLoaded() {
                    Log.d(TAG, "✅ [TakuBanner广告] 加载成功")
                    callback?.onAdLoaded(bannerView)
                }
                
                override fun onBannerFailed(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "未知错误"
                    Log.e(TAG, "❌ [TakuBanner广告] 加载失败: $errorMsg")
                    callback?.onAdLoadFailed(errorMsg)
                }
                
                override fun onBannerClicked(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "👆 [TakuBanner广告] 用户点击")
                    callback?.onAdClicked()
                }
                
                override fun onBannerShow(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [TakuBanner广告] 开始展示")
                    callback?.onAdShowed()
                }
                
                override fun onBannerClose(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🔚 [TakuBanner广告] 广告关闭")
                    callback?.onAdClosed()
                    // 通知广告关闭管理器
                    com.jiankangpaika.app.ad.BannerAdCloseManager.notifyAdClosed()
                }
                
                override fun onBannerAutoRefreshed(atAdInfo: ATAdInfo?) {
                    Log.d(TAG, "🔄 [TakuBanner广告] 自动刷新")
                }
                
                override fun onBannerAutoRefreshFail(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "自动刷新失败"
                    Log.e(TAG, "❌ [TakuBanner广告] 自动刷新失败: $errorMsg")
                }
            })
            
            // 开始加载Banner广告
            bannerView?.loadAd()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [TakuBanner广告] 加载异常: ${e.message}", e)
            callback?.onAdLoadFailed("加载异常: ${e.message}")
        }
    }
    
    /**
     * 获取Banner广告视图（TakuAdManager接口实现）
     */
    override fun getBannerAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎨 [TakuBanner广告] 获取广告视图")
        
        val banner = bannerView
        if (banner == null) {
            Log.e(TAG, "❌ [TakuBanner广告] 广告未加载，无法获取视图")
            callback(null, "Banner广告未加载")
            return
        }
        
        try {
            Log.i(TAG, "✅ [TakuBanner广告] 获取视图成功")
            callback(banner, "Banner广告视图获取成功")
        } catch (e: Exception) {
            Log.e(TAG, "💥 [TakuBanner广告] 获取视图异常", e)
            callback(null, "Banner广告视图获取异常: ${e.message}")
        }
    }
    
    /**
     * 获取Banner广告视图（TakuAdManager接口实现）
     * @param context 上下文
     * @return Banner广告视图，如果没有可用广告则返回null
     */
    override fun getBannerAdView(context: Context): ViewGroup? {
        Log.d(TAG, "🎨 [TakuBanner广告] 获取广告视图（同步方法）")
        
        val banner = bannerView
        if (banner == null) {
            Log.e(TAG, "❌ [TakuBanner广告] 广告未加载，无法获取视图")
            return null
        }
        
        Log.i(TAG, "✅ [TakuBanner广告] 获取视图成功")
        return banner
    }
    
    /**
     * 展示Banner广告到指定容器
     */
    override fun showBannerAd(container: ViewGroup, callback: BannerAdCallback?) {
        try {
            val banner = bannerView
            if (banner == null) {
                Log.e(TAG, "❌ [TakuBanner广告] 广告未加载，无法展示")
                callback?.onAdLoadFailed("Banner广告未加载")
                return
            }
            
            // 将Banner添加到容器中
            container.removeAllViews()
            container.addView(banner)
            
            Log.d(TAG, "🎬 [TakuBanner广告] 展示到容器")
            callback?.onAdShowed()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [TakuBanner广告] 展示异常: ${e.message}", e)
            callback?.onAdLoadFailed("展示异常: ${e.message}")
        }
    }
    
    /**
     * 显示Banner广告（设置可见性）
     */
    override fun showBannerAd() {
        bannerView?.visibility = android.view.View.VISIBLE
        Log.d(TAG, "🎬 [TakuBanner广告] 设置为可见")
    }
    
    /**
     * 隐藏Banner广告
     */
    override fun hideBannerAd() {
        bannerView?.visibility = android.view.View.GONE
        Log.d(TAG, "🙈 [TakuBanner广告] 设置为隐藏")
    }
    
    /**
     * 检查Banner广告是否准备好
     */
    override fun isBannerAdReady(): Boolean {
        return bannerView != null
    }
    
    // ==================== 信息流广告 ====================
    
    /**
     * 实现AdManager接口的loadFeedAd方法
     */
    override fun loadFeedAd(context: Context, callback: (Boolean, String?) -> Unit) {
        loadFeedAdInternal(context, object : FeedAdCallback {
            override fun onAdLoaded() {
                callback(true, "信息流广告加载成功")
            }
            
            override fun onAdLoadFailed(error: String) {
                callback(false, error)
            }
            
            override fun onAdShowed() {}
            override fun onAdClicked() {}
            override fun onAdClosed() {}
        })
    }
    
    /**
     * 加载信息流广告（TakuAdManager接口实现）
     */
    override fun loadFeedAd(context: Context, callback: FeedAdCallback?) {
        loadFeedAdInternal(context, callback)
    }
    
    /**
     * 加载信息流广告（内部实现）
     * 根据Taku信息流广告接入最佳实践实现
     */
    private fun loadFeedAdInternal(context: Context, callback: FeedAdCallback?) {
        if (!isInitialized) {
            Log.e(TAG, "SDK未初始化，无法加载信息流广告")
            callback?.onAdLoadFailed("SDK未初始化")
            return
        }
        
        try {
            val placementId = AdConfig.getTakuFeedAdUnitId()
            if (placementId.isEmpty()) {
                Log.e(TAG, "信息流广告位ID为空")
                callback?.onAdLoadFailed("广告位ID为空")
                return
            }
            
            Log.d(TAG, "🔍 [Taku信息流广告] 开始加载, placementId: $placementId")
            
            // 创建信息流广告实例
            feedNativeAd = ATNative(context, placementId, object : ATNativeNetworkListener {
                override fun onNativeAdLoaded() {
                    Log.d(TAG, "✅ [Taku信息流广告] 加载成功")
                    callback?.onAdLoaded()
                }
                
                override fun onNativeAdLoadFail(adError: AdError?) {
                    val errorMsg = adError?.fullErrorInfo ?: "加载失败"
                    Log.e(TAG, "❌ [Taku信息流广告] 加载失败: $errorMsg")
                    callback?.onAdLoadFailed(errorMsg)
                }
            })
            
            // 设置广告尺寸
            val localExtra = HashMap<String, Any>()
            // 动态获取屏幕宽度
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            localExtra[ATAdConst.KEY.AD_WIDTH] = screenWidth - 88 // 广告宽度（动态获取屏幕宽度）
            localExtra[ATAdConst.KEY.AD_HEIGHT] = 0 // 广告高度
            feedNativeAd?.setLocalExtra(localExtra)
            
            // 开始加载广告
            feedNativeAd?.makeAdRequest()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Taku信息流广告] 加载异常: ${e.message}", e)
            callback?.onAdLoadFailed("加载异常: ${e.message}")
        }
    }
    
    /**
     * 获取信息流广告视图（AdManager接口实现）
     */
    override fun getFeedAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎨 [Taku信息流广告] 获取广告视图")
        
        try {
            val nativeAd = feedNativeAd
            if (nativeAd == null) {
                Log.e(TAG, "❌ [Taku信息流广告] 广告未加载，无法获取视图")
                callback(null, "信息流广告未加载")
                return
            }
            
            // 检查广告是否准备好
            if (!nativeAd.checkAdStatus().isReady()) {
                Log.e(TAG, "❌ [Taku信息流广告] 广告未准备好")
                callback(null, "信息流广告未准备好")
                return
            }
            
            // 获取原生广告实例
            val feedAd = nativeAd.getNativeAd()
            if (feedAd == null) {
                Log.e(TAG, "❌ [Taku信息流广告] 获取原生广告实例失败")
                callback(null, "获取原生广告实例失败")
                return
            }
            
            // 保存广告实例用于后续操作
            feedAdView = feedAd
            
            // 创建广告容器视图
            val adContainer = ATNativeView(context)
            adContainer.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            
            // 设置广告事件监听器
            feedAd.setNativeEventListener(object : ATNativeEventListener {
                override fun onAdImpressed(view: ATNativeAdView?, adInfo: ATAdInfo?) {
                    Log.d(TAG, "🎬 [Taku信息流广告] 广告展示")
                    // 这里可以添加展示回调，但当前接口不支持
                }
                
                override fun onAdClicked(view: ATNativeAdView?, adInfo: ATAdInfo?) {
                    Log.d(TAG, "👆 [Taku信息流广告] 广告点击")
                    // 这里可以添加点击回调，但当前接口不支持
                }
                
                override fun onAdVideoStart(view: ATNativeAdView?) {
                    Log.d(TAG, "▶️ [Taku信息流广告] 视频开始播放")
                }
                
                override fun onAdVideoEnd(view: ATNativeAdView?) {
                    Log.d(TAG, "⏹️ [Taku信息流广告] 视频播放结束")
                }
                
                override fun onAdVideoProgress(view: ATNativeAdView?, progress: Int) {
                    Log.d(TAG, "📊 [Taku信息流广告] 视频播放进度: $progress")
                }
            })
            
            // 注意：ATNativeDislikeListener在当前版本中可能需要特定的构造参数
            // 暂时移除dislike监听器设置，避免编译错误
            // TODO: 根据实际SDK版本调整dislike监听器的实现方式
            
            // 渲染广告
            try {
                val prepareInfo = ATNativePrepareExInfo()
                
                if (feedAd.isNativeExpress()) {
                    // 模板渲染
                    Log.d(TAG, "📱 [Taku信息流广告] 使用模板渲染")
                    feedAd.renderAdContainer(adContainer, null)
                } else {
                    // 自渲染（暂不支持，使用模板渲染）
                    Log.d(TAG, "🎨 [Taku信息流广告] 使用模板渲染（自渲染暂不支持）")
                    feedAd.renderAdContainer(adContainer, null)
                }
                
                // 准备广告
                feedAd.prepare(adContainer, prepareInfo)
                
                Log.i(TAG, "✅ [Taku信息流广告] 获取视图成功")
                callback(adContainer, null)
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [Taku信息流广告] 渲染广告异常: ${e.message}", e)
                callback(null, "渲染广告异常: ${e.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Taku信息流广告] 获取视图异常: ${e.message}", e)
            callback(null, "获取视图异常: ${e.message}")
        }
    }
    
    // ==================== Draw广告 ====================
    
    /**
     * 实现AdManager接口的loadDrawAd方法
     */
    override fun loadDrawAd(context: Context, callback: (Boolean, String?) -> Unit) {
        loadDrawAdInternal(context, object : DrawAdCallback {
            override fun onAdLoaded() {
                callback(true, "Draw广告加载成功")
            }
            
            override fun onAdLoadFailed(error: String) {
                callback(false, error)
            }
            
            override fun onAdShowed() {}
            override fun onAdClicked() {}
            override fun onAdClosed() {}
        })
    }
    
    /**
     * 加载Draw广告（TakuAdManager接口实现）
     */
    override fun loadDrawAd(context: Context, callback: DrawAdCallback?) {
        loadDrawAdInternal(context, callback)
    }
    
    /**
     * 加载Draw广告（内部实现）
     */
    private fun loadDrawAdInternal(context: Context, callback: DrawAdCallback?) {
        // Taku SDK暂不支持Draw广告，使用信息流广告替代
        Log.w(TAG, "Taku SDK暂不支持Draw广告，使用信息流广告替代")
        loadFeedAd(context, object : FeedAdCallback {
            override fun onAdLoaded() {
                callback?.onAdLoaded()
            }
            
            override fun onAdLoadFailed(error: String) {
                callback?.onAdLoadFailed(error)
            }
            
            override fun onAdShowed() {
                callback?.onAdShowed()
            }
            
            override fun onAdClicked() {
                callback?.onAdClicked()
            }
            
            override fun onAdClosed() {
                callback?.onAdClosed()
            }
        })
    }
    
    /**
     * 获取Draw广告视图（AdManager接口实现）
     */
    override fun getDrawAdView(context: Context, callback: (ViewGroup?, String?) -> Unit) {
        Log.d(TAG, "🎨 [TakuDraw广告] 获取广告视图")
        
        // Taku平台暂不支持Draw广告
        Log.w(TAG, "⚠️ [TakuDraw广告] Taku平台暂不支持Draw广告")
        callback(null, "Taku平台暂不支持Draw广告")
    }
    
    // ==================== 原生广告（暂不支持）====================
    
    // 移除原生广告相关方法，TakuAdManager接口不包含这些方法
    
    // ==================== 清理资源 ====================
    
    /**
     * 清理所有广告资源
     */
    override fun destroy() {
        Log.d(TAG, "🧹 [Taku广告] 开始清理资源")
        
        try {
            // 清理开屏广告
            splashAd = null
            
            // 清理插屏广告
            interstitialAd = null
            
            // 清理激励视频广告
            rewardVideoAd = null
            
            // 清理Banner广告
            bannerView?.destroy()
            bannerView = null
            
            // 清理信息流广告
            feedAdView?.destory()
            feedAdView = null
            feedNativeAd?.setAdListener(null)
            feedNativeAd = null
            
            Log.i(TAG, "✅ [Taku广告] 资源清理完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [Taku广告] 资源清理异常", e)
        }
    }
    
    /**
     * 解析错误信息中的platformMSG内容
     * 错误信息格式: code:[ 4006 ]desc:[ Ad show failed ]platformCode:[ 5002 ]platformMSG:[ 视频素材下载错误... ]
     * @param errorMsg 完整的错误信息
     * @return 提取的platformMSG内容，如果解析失败则返回null
     */
    private fun extractPlatformMsg(errorMsg: String): String? {
        return try {
            // 使用正则表达式匹配platformMSG内容
            val regex = "platformMSG:\\s*\\[\\s*(.+?)\\s*\\]".toRegex()
            val matchResult = regex.find(errorMsg)
            matchResult?.groupValues?.get(1)?.trim()
        } catch (e: Exception) {
            Log.w(TAG, "解析platformMSG失败: ${e.message}")
            null
        }
    }
}