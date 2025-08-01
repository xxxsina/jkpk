package com.jiankangpaika.app.ad

import android.app.Activity
import android.content.Context
import android.view.ViewGroup

/**
 * 广告管理器接口
 * 定义统一的广告操作接口，支持多个广告平台
 */
interface AdManager {
    
    /**
     * 初始化广告SDK
     * @param context 上下文
     * @param callback 初始化回调 (成功状态, 错误信息)
     */
    fun initSDK(context: Context, callback: (Boolean, String?) -> Unit)
    
    /**
     * 获取广告平台名称
     */
    fun getPlatformName(): String
    
    /**
     * 检查SDK是否已初始化
     */
    fun isInitialized(): Boolean
    
    // ==================== 开屏广告 ====================
    
    /**
     * 加载开屏广告
     * @param context 上下文
     * @param callback 加载回调 (成功状态, 错误信息)
     */
    fun loadSplashAd(context: Context, callback: (Boolean, String?) -> Unit)
    
    /**
     * 展示开屏广告
     * @param activity Activity实例
     * @param container 广告容器
     * @param callback 展示回调 (成功状态, 错误信息)
     */
    fun showSplashAd(
        activity: Activity,
        container: ViewGroup,
        callback: (Boolean, String?) -> Unit
    )
    
    // ==================== 插屏广告 ====================
    
    /**
     * 加载插屏广告
     * @param context 上下文
     * @param callback 加载回调 (成功状态, 错误信息)
     */
    fun loadInterstitialAd(context: Context, callback: (Boolean, String?) -> Unit)
    
    /**
     * 展示插屏广告
     * @param activity Activity实例
     * @param callback 展示回调 (成功状态, 错误信息)
     */
    fun showInterstitialAd(activity: Activity, callback: (Boolean, String?) -> Unit)
    
    // ==================== 信息流广告 ====================
    
    /**
     * 加载信息流广告
     * @param context 上下文
     * @param callback 加载回调 (成功状态, 错误信息)
     */
    fun loadFeedAd(context: Context, callback: (Boolean, String?) -> Unit)
    
    /**
     * 获取信息流广告视图
     * @param context 上下文
     * @param callback 获取回调 (广告视图, 错误信息)
     */
    fun getFeedAdView(context: Context, callback: (ViewGroup?, String?) -> Unit)
    
    // ==================== 激励视频广告 ====================
    
    /**
     * 加载激励视频广告
     * @param context 上下文
     * @param callback 加载回调 (成功状态, 错误信息)
     */
    fun loadRewardVideoAd(context: Context, callback: (Boolean, String?) -> Unit)
    
    /**
     * 展示激励视频广告
     * @param activity Activity实例
     * @param callback 展示回调 (成功状态, 是否获得奖励, 错误信息)
     */
    fun showRewardVideoAd(activity: Activity, callback: (Boolean, Boolean, String?) -> Unit)
    
    // ==================== Banner广告 ====================
    
    /**
     * 加载Banner广告
     * @param context 上下文
     * @param callback 加载回调 (成功状态, 错误信息)
     */
    fun loadBannerAd(context: Context, callback: (Boolean, String?) -> Unit)
    
    /**
     * 获取Banner广告视图
     * @param context 上下文
     * @param callback 获取回调 (广告视图, 错误信息)
     */
    fun getBannerAdView(context: Context, callback: (ViewGroup?, String?) -> Unit)
    
    /**
     * 获取Banner广告视图（支持关闭回调）
     * @param context 上下文
     * @param onAdClosed 广告关闭回调
     * @param callback 获取回调 (广告视图, 错误信息)
     */
    fun getBannerAdView(context: Context, onAdClosed: () -> Unit, callback: (ViewGroup?, String?) -> Unit) {
        // 默认实现，调用原方法
        getBannerAdView(context, callback)
    }

    // ==================== Draw广告 ====================

    /**
     * 加载Draw广告
     * @param context 上下文
     * @param callback 加载回调 (成功状态, 错误信息)
     */
    fun loadDrawAd(context: Context, callback: (Boolean, String?) -> Unit)

    /**
     * 获取Draw广告视图
     * @param context 上下文
     * @param callback 获取回调 (广告视图, 错误信息)
     */
    fun getDrawAdView(context: Context, callback: (ViewGroup?, String?) -> Unit)
    
    // ==================== 资源清理 ====================
    
    /**
     * 销毁广告资源
     */
    fun destroy()
}