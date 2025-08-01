package com.jiankangpaika.app.ad

import android.app.Activity
import android.content.Context
import android.view.ViewGroup
import com.jiankangpaika.app.ad.callback.*

/**
 * Taku广告管理器接口
 * 定义Taku广告平台的统一操作接口
 */
interface TakuAdManager : AdManager {
    
    /**
     * 初始化广告SDK
     * @param context 上下文
     * @param callback 初始化回调
     */
    fun initSDK(context: Context, callback: AdInitCallback?)
    
    /**
     * 获取广告平台名称
     */
    override fun getPlatformName(): String
    
    /**
     * 检查SDK是否已初始化
     */
    override fun isInitialized(): Boolean
    
    // ==================== 开屏广告 ====================
    
    /**
     * 加载开屏广告
     * @param context 上下文
     * @param callback 加载回调
     */
    fun loadSplashAd(context: Context, callback: SplashAdCallback?)
    
    /**
     * 展示开屏广告
     * @param activity Activity实例
     * @param container 广告容器
     * @param callback 展示回调
     */
    fun showSplashAd(
        activity: Activity,
        container: ViewGroup?,
        callback: SplashAdCallback?
    )
    
    // ==================== 插屏广告 ====================
    
    /**
     * 加载插屏广告
     * @param context 上下文
     * @param callback 加载回调
     */
    fun loadInterstitialAd(context: Context, callback: InterstitialAdCallback?)
    
    /**
     * 展示插屏广告
     * @param activity Activity实例
     * @param callback 展示回调
     */
    fun showInterstitialAd(activity: Activity, callback: InterstitialAdCallback?)
    
    // ==================== 信息流广告 ====================
    
    /**
     * 加载信息流广告
     * @param context 上下文
     * @param callback 加载回调
     */
    fun loadFeedAd(context: Context, callback: FeedAdCallback?)
    
    // ==================== 激励视频广告 ====================
    
    /**
     * 加载激励视频广告
     * @param context 上下文
     * @param callback 加载回调
     */
    fun loadRewardVideoAd(context: Context, callback: RewardVideoAdCallback?)
    
    /**
     * 展示激励视频广告
     * @param activity Activity实例
     * @param callback 展示回调
     */
    fun showRewardVideoAd(activity: Activity, callback: RewardVideoAdCallback?)
    
    // ==================== Banner广告 ====================
    
    /**
     * 加载Banner广告
     * @param context 上下文
     * @param callback 加载回调
     */
    fun loadBannerAd(context: Context, callback: BannerAdCallback?)
    
    /**
     * 获取Banner广告视图
     * @param context 上下文
     * @return Banner广告视图，如果没有可用广告则返回null
     */
    fun getBannerAdView(context: Context): ViewGroup?
    
    /**
     * 展示Banner广告到指定容器
     * @param container 广告容器
     * @param callback 展示回调
     */
    fun showBannerAd(container: ViewGroup, callback: BannerAdCallback?)
    
    /**
     * 显示Banner广告（设置可见性）
     */
    fun showBannerAd()
    
    /**
     * 隐藏Banner广告
     */
    fun hideBannerAd()
    
    /**
     * 检查Banner广告是否准备就绪
     * @return true表示广告已准备好可以展示
     */
    fun isBannerAdReady(): Boolean

    // ==================== Draw广告 ====================

    /**
     * 加载Draw广告
     * @param context 上下文
     * @param callback 加载回调
     */
    fun loadDrawAd(context: Context, callback: DrawAdCallback?)
    
    // ==================== 资源清理 ====================
    
    /**
     * 销毁广告管理器，释放资源
     */
    override fun destroy()
}