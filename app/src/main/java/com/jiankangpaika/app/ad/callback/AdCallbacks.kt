package com.jiankangpaika.app.ad.callback

import android.view.ViewGroup

/**
 * 广告初始化回调接口
 */
interface AdInitCallback {
    fun onInitSuccess()
    fun onInitFailed(error: String)
}

/**
 * 开屏广告回调接口
 */
interface SplashAdCallback {
    fun onAdLoaded()
    fun onAdLoadFailed(error: String)
    fun onAdShowed()
    fun onAdClicked()
    fun onAdClosed()
}

/**
 * 插屏广告回调接口
 */
interface InterstitialAdCallback {
    fun onAdLoaded()
    fun onAdLoadFailed(error: String)
    fun onAdShowed()
    fun onAdClicked()
    fun onAdClosed()
}

/**
 * 信息流广告回调接口
 */
interface FeedAdCallback {
    fun onAdLoaded()
    fun onAdLoadFailed(error: String)
    fun onAdShowed()
    fun onAdClicked()
    fun onAdClosed()
}

/**
 * 激励视频广告回调接口
 */
interface RewardVideoAdCallback {
    fun onAdLoaded()
    fun onAdLoadFailed(error: String)
    fun onAdShowed()
    fun onAdClicked()
    fun onAdClosed()
    fun onRewarded()
}

/**
 * Banner广告回调接口
 */
interface BannerAdCallback {
    fun onAdLoaded(bannerView: ViewGroup?)
    fun onAdLoadFailed(error: String)
    fun onAdShowed()
    fun onAdClicked()
    fun onAdClosed()
}

/**
 * Draw广告回调接口
 */
interface DrawAdCallback {
    fun onAdLoaded()
    fun onAdLoadFailed(error: String)
    fun onAdShowed()
    fun onAdClicked()
    fun onAdClosed()
}