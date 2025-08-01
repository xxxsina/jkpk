package com.jiankangpaika.app.ad

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.*

/**
 * 广告工具类
 * 负责广告展示策略的管理
 */
object AdUtils {
    
    private const val TAG = "AdUtils"
    private const val PREF_NAME = "ad_preferences"
    
    // SharedPreferences键名
    private const val KEY_LAST_SPLASH_AD_TIME = "last_splash_ad_time"
    private const val KEY_LAST_INTERSTITIAL_AD_TIME = "last_interstitial_ad_time"
    private const val KEY_INTERSTITIAL_CONTINUOUS_COUNT = "interstitial_continuous_count"
    private const val KEY_INTERSTITIAL_LAST_RESET_TIME = "interstitial_last_reset_time"
    private const val KEY_FEED_AD_LAST_SHOW_TIME = "feed_ad_last_show_time"
    private const val KEY_BANNER_AD_LAST_SHOW_TIME = "banner_ad_last_show_time"
    private const val KEY_REWARD_VIDEO_COUNT_TODAY = "reward_video_count_today"
    private const val KEY_REWARD_VIDEO_DATE = "reward_video_date"
    
    // 兼容性常量
    private const val PREFS_NAME = PREF_NAME
    
    /**
     * 获取SharedPreferences
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 检查是否可以展示开屏广告
     * 根据时间间隔策略判断
     */
    fun canShowSplashAd(context: Context): Boolean {
        Log.d(TAG, "🔍 [AdUtils锚点1] 开始检查开屏广告展示策略")
        // 临时禁用时间间隔限制，用于调试广告展示问题
        // TODO: 正式发布时需要恢复时间间隔限制
        Log.d(TAG, "✅ [AdUtils锚点1结果] 临时禁用开屏广告时间间隔限制，允许展示广告")
        return true
        
        /*
        val prefs = getPreferences(context)
        val lastShowTime = prefs.getLong(KEY_LAST_SPLASH_AD_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val intervalMs = AdConfig.Strategy.SPLASH_AD_INTERVAL_HOURS * 60 * 60 * 1000L
        
        val canShow = (currentTime - lastShowTime) >= intervalMs
        Log.d(TAG, "检查开屏广告展示条件: canShow=$canShow, 距离上次展示=${(currentTime - lastShowTime) / 1000}秒")
        
        return canShow
        */
    }
    
    /**
     * 记录开屏广告展示时间
     */
    fun recordSplashAdShown(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putLong(KEY_LAST_SPLASH_AD_TIME, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "记录开屏广告展示时间")
    }
    
    /**
     * 检查是否可以展示插屏广告
     * 根据接口返回的continuous_times连续展示插屏广告，并由time_interval控制展示时间间隔
     */
    fun canShowInterstitialAd(context: Context): Boolean {
        val prefs = getPreferences(context)
        val adSwitchConfig = AdSwitchConfig.getInstance()
        
        // 获取配置参数
        val continuousTimes = adSwitchConfig.getInterstitialContinuousTimes()
        val timeInterval = adSwitchConfig.getInterstitialTimeInterval()
        
        val currentTime = System.currentTimeMillis()
        val lastShowTime = prefs.getLong(KEY_LAST_INTERSTITIAL_AD_TIME, 0)
        val continuousCount = prefs.getInt(KEY_INTERSTITIAL_CONTINUOUS_COUNT, 0)
        val lastResetTime = prefs.getLong(KEY_INTERSTITIAL_LAST_RESET_TIME, 0)
        
        // 检查是否需要重置连续展示计数（超过时间间隔）
        val intervalMs = timeInterval * 1000L
        val shouldReset = (currentTime - lastResetTime) >= intervalMs
        
        if (shouldReset && continuousCount > 0) {
            // 重置连续展示计数
            prefs.edit()
                .putInt(KEY_INTERSTITIAL_CONTINUOUS_COUNT, 0)
                .putLong(KEY_INTERSTITIAL_LAST_RESET_TIME, currentTime)
                .apply()
            Log.d(TAG, "🔄 [插屏广告] 重置连续展示计数，时间间隔已到")
        }
        
        val currentCount = if (shouldReset) 0 else continuousCount
        val canShow = currentCount < continuousTimes
        
        Log.d(TAG, "🔍 [插屏广告] 展示条件检查: 当前连续次数=$currentCount, 配置连续次数=$continuousTimes, 时间间隔=${timeInterval}秒, 可展示=$canShow")
        
        return canShow
    }
    
    /**
     * 记录插屏广告展示时间
     */
    fun recordInterstitialAdShown(context: Context) {
        val prefs = getPreferences(context)
        val currentTime = System.currentTimeMillis()
        val continuousCount = prefs.getInt(KEY_INTERSTITIAL_CONTINUOUS_COUNT, 0)
        val newCount = continuousCount + 1
        
        prefs.edit()
            .putLong(KEY_LAST_INTERSTITIAL_AD_TIME, currentTime)
            .putInt(KEY_INTERSTITIAL_CONTINUOUS_COUNT, newCount)
            .putLong(KEY_INTERSTITIAL_LAST_RESET_TIME, currentTime)
            .apply()
            
        Log.d(TAG, "📝 [插屏广告] 记录展示时间，连续展示次数: $newCount")
    }
    
    /**
     * 检查是否应该展示插屏广告（别名方法）
     */
    fun shouldShowInterstitialAd(context: Context): Boolean {
        return canShowInterstitialAd(context)
    }
    

    
    /**
     * 检查是否可以展示信息流广告
     */
    fun canShowFeedAd(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShowTime = prefs.getLong(KEY_FEED_AD_LAST_SHOW_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val intervalMs = AdConfig.Strategy.FEED_AD_INTERVAL_MINUTES * 60 * 1000L
        
        val canShow = (currentTime - lastShowTime) >= intervalMs
        Log.d(TAG, "检查信息流广告展示条件: 上次展示时间=${lastShowTime}, 当前时间=${currentTime}, 间隔=${currentTime - lastShowTime}ms, 阈值=${intervalMs}ms, canShow=$canShow")
        
        return canShow
    }
    
    /**
     * 记录信息流广告展示时间
     */
    fun recordFeedAdShowTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_FEED_AD_LAST_SHOW_TIME, currentTime)
            .apply()
        
        Log.d(TAG, "记录信息流广告展示时间: $currentTime")
    }
    
    /**
     * 检查是否应该展示信息流广告（别名方法）
     */
    fun shouldShowFeedAd(context: Context): Boolean {
        return canShowFeedAd(context)
    }
    
    /**
     * 记录信息流广告展示（别名方法）
     */
    fun recordFeedAdShown(context: Context) {
        recordFeedAdShowTime(context)
    }
    
    /**
     * 检查是否可以展示Banner广告
     */
    fun canShowBannerAd(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastShowTime = prefs.getLong(KEY_BANNER_AD_LAST_SHOW_TIME, 0)
        val currentTime = System.currentTimeMillis()
        val intervalMs = AdConfig.Strategy.BANNER_AD_INTERVAL_MINUTES * 60 * 1000L
        
        val canShow = (currentTime - lastShowTime) >= intervalMs
        Log.d(TAG, "检查Banner广告展示条件: 上次展示时间=${lastShowTime}, 当前时间=${currentTime}, 间隔=${currentTime - lastShowTime}ms, 阈值=${intervalMs}ms, canShow=$canShow")
        
        return canShow
    }
    
    /**
     * 记录Banner广告展示时间
     */
    fun recordBannerAdShowTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentTime = System.currentTimeMillis()
        prefs.edit()
            .putLong(KEY_BANNER_AD_LAST_SHOW_TIME, currentTime)
            .apply()
        
        Log.d(TAG, "记录Banner广告展示时间: $currentTime")
    }
    
    /**
     * 检查是否应该展示Banner广告（别名方法）
     */
    fun shouldShowBannerAd(context: Context): Boolean {
        return canShowBannerAd(context)
    }
    
    /**
     * 记录Banner广告展示（别名方法）
     */
    fun recordBannerAdShown(context: Context) {
        recordBannerAdShowTime(context)
    }
    
    /**
     * 检查今日激励视频观看次数是否达到限制
     */
    fun canShowRewardVideoAd(context: Context): Boolean {
        val prefs = getPreferences(context)
        val today = getTodayDateString()
        val recordDate = prefs.getString(KEY_REWARD_VIDEO_DATE, "")
        
        // 如果日期不同，重置计数
        if (recordDate != today) {
            prefs.edit()
                .putString(KEY_REWARD_VIDEO_DATE, today)
                .putInt(KEY_REWARD_VIDEO_COUNT_TODAY, 0)
                .apply()
        }
        
        val todayCount = prefs.getInt(KEY_REWARD_VIDEO_COUNT_TODAY, 0)
        val canShow = todayCount < AdConfig.Strategy.REWARD_VIDEO_DAILY_LIMIT
        
        Log.d(TAG, "检查激励视频广告展示条件: canShow=$canShow, 今日已观看=$todayCount")
        return canShow
    }
    
    /**
     * 记录激励视频观看次数
     */
    fun recordRewardVideoAdShown(context: Context) {
        val prefs = getPreferences(context)
        val today = getTodayDateString()
        val recordDate = prefs.getString(KEY_REWARD_VIDEO_DATE, "")
        
        // 确保日期正确
        if (recordDate != today) {
            prefs.edit()
                .putString(KEY_REWARD_VIDEO_DATE, today)
                .putInt(KEY_REWARD_VIDEO_COUNT_TODAY, 1)
                .apply()
        } else {
            val currentCount = prefs.getInt(KEY_REWARD_VIDEO_COUNT_TODAY, 0)
            prefs.edit()
                .putInt(KEY_REWARD_VIDEO_COUNT_TODAY, currentCount + 1)
                .apply()
        }
        
        Log.d(TAG, "记录激励视频广告观看次数")
    }
    
    /**
     * 获取今日激励视频剩余观看次数
     */
    fun getRemainingRewardVideoCount(context: Context): Int {
        val prefs = getPreferences(context)
        val today = getTodayDateString()
        val recordDate = prefs.getString(KEY_REWARD_VIDEO_DATE, "")
        
        if (recordDate != today) {
            return AdConfig.Strategy.REWARD_VIDEO_DAILY_LIMIT
        }
        
        val todayCount = prefs.getInt(KEY_REWARD_VIDEO_COUNT_TODAY, 0)
        return maxOf(0, AdConfig.Strategy.REWARD_VIDEO_DAILY_LIMIT - todayCount)
    }
    
    /**
     * 获取今日日期字符串（格式：yyyy-MM-dd）
     */
    private fun getTodayDateString(): String {
        val calendar = Calendar.getInstance()
        return String.format(
            "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
    
    /**
     * 清除所有广告记录（用于测试或重置）
     */
    fun clearAllAdRecords(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit().clear().apply()
        Log.d(TAG, "清除所有广告记录")
    }
}