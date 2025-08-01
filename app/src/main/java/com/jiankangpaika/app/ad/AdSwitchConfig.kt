package com.jiankangpaika.app.ad

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult

/**
 * 广告开关配置管理类
 * 负责管理所有广告类型的开关状态，支持本地缓存和远程配置
 */
class AdSwitchConfig private constructor() {
    
    companion object {
        private const val TAG = "AdSwitchConfig"
        private const val PREF_NAME = "ad_switch_config"
        
        // SharedPreferences键名
        private const val KEY_MASTER_SWITCH = "master_switch"
        private const val KEY_SPLASH_SWITCH = "splash_switch"
        private const val KEY_INTERSTITIAL_SWITCH = "interstitial_switch"
        private const val KEY_FEED_SWITCH = "feed_switch"
        private const val KEY_REWARD_VIDEO_SWITCH = "reward_video_switch"
        private const val KEY_BANNER_SWITCH = "banner_switch"
        private const val KEY_DRAW_SWITCH = "draw_switch"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val KEY_CONFIG_VERSION = "config_version"
        
        // 广告平台选择键名
        private const val KEY_SPLASH_PLATFORM = "splash_platform"
        private const val KEY_BANNER_PLATFORM = "banner_platform"
        private const val KEY_INTERSTITIAL_PLATFORM = "interstitial_platform"
        private const val KEY_FEED_PLATFORM = "feed_platform"
        private const val KEY_REWARD_VIDEO_PLATFORM = "reward_video_platform"
        private const val KEY_DRAW_PLATFORM = "draw_platform"
        
        // 插屏广告配置键名
        private const val KEY_INTERSTITIAL_CONTINUOUS_TIMES = "interstitial_continuous_times"
        private const val KEY_INTERSTITIAL_TIME_INTERVAL = "interstitial_time_interval"
        
        // 默认开关状态
        private const val DEFAULT_MASTER_SWITCH = true
        private const val DEFAULT_SPLASH_SWITCH = true
        private const val DEFAULT_INTERSTITIAL_SWITCH = true
        private const val DEFAULT_FEED_SWITCH = true
        private const val DEFAULT_REWARD_VIDEO_SWITCH = true
        private const val DEFAULT_BANNER_SWITCH = true
        private const val DEFAULT_DRAW_SWITCH = true
        
        // 广告平台常量
        const val PLATFORM_KUAISHOU = "kuaishou"
        const val PLATFORM_CHUANSHANJIA = "chuanshanjia"
        const val PLATFORM_TAKU = "taku"
        
        // 默认广告平台（优先使用快手）
        private const val DEFAULT_SPLASH_PLATFORM = PLATFORM_KUAISHOU
        private const val DEFAULT_BANNER_PLATFORM = PLATFORM_KUAISHOU
        private const val DEFAULT_INTERSTITIAL_PLATFORM = PLATFORM_KUAISHOU
        private const val DEFAULT_FEED_PLATFORM = PLATFORM_KUAISHOU
        private const val DEFAULT_REWARD_VIDEO_PLATFORM = PLATFORM_KUAISHOU
        private const val DEFAULT_DRAW_PLATFORM = PLATFORM_CHUANSHANJIA
        
        // 插屏广告配置默认值
        private const val DEFAULT_INTERSTITIAL_CONTINUOUS_TIMES = 1
        private const val DEFAULT_INTERSTITIAL_TIME_INTERVAL = 60
        
        // 配置更新间隔（毫秒）
        private const val CONFIG_UPDATE_INTERVAL = 10 * 1000L // 10秒
        
        @Volatile
        private var INSTANCE: AdSwitchConfig? = null
        
        fun getInstance(): AdSwitchConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdSwitchConfig().also { INSTANCE = it }
            }
        }
    }
    
    private var preferences: SharedPreferences? = null
    
    /**
     * 初始化配置管理器
     */
    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "📱 [AdSwitchConfig] 初始化完成")
        
        // 确保有默认配置
        ensureDefaultConfig()
        
        // 注意：不在这里调用checkAndUpdateConfig，避免重复请求
        // 配置更新由UnifiedConfigManager统一管理
    }
    
    // ==================== 主开关 ====================
    
    /**
     * 获取广告总开关状态
     */
    fun isMasterSwitchEnabled(): Boolean {
        val enabled = preferences?.getBoolean(KEY_MASTER_SWITCH, DEFAULT_MASTER_SWITCH) ?: DEFAULT_MASTER_SWITCH
        Log.d(TAG, "🔍 [主开关] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置广告总开关状态
     */
    fun setMasterSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_MASTER_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [主开关] 设置为: $enabled")
    }
    
    // ==================== 开屏广告开关 ====================
    
    /**
     * 获取开屏广告开关状态
     */
    fun isSplashAdEnabled(): Boolean {
        if (!isMasterSwitchEnabled()) {
            Log.d(TAG, "🚫 [开屏广告] 主开关已关闭")
            return false
        }
        val enabled = preferences?.getBoolean(KEY_SPLASH_SWITCH, DEFAULT_SPLASH_SWITCH) ?: DEFAULT_SPLASH_SWITCH
        Log.d(TAG, "🔍 [开屏广告] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置开屏广告开关状态
     */
    fun setSplashAdSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_SPLASH_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [开屏广告] 设置为: $enabled")
    }
    
    // ==================== 插屏广告开关 ====================
    
    /**
     * 获取插屏广告开关状态
     */
    fun isInterstitialAdEnabled(): Boolean {
        if (!isMasterSwitchEnabled()) {
            Log.d(TAG, "🚫 [插屏广告] 主开关已关闭")
            return false
        }
        val enabled = preferences?.getBoolean(KEY_INTERSTITIAL_SWITCH, DEFAULT_INTERSTITIAL_SWITCH) ?: DEFAULT_INTERSTITIAL_SWITCH
        Log.d(TAG, "🔍 [插屏广告] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置插屏广告开关状态
     */
    fun setInterstitialAdSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_INTERSTITIAL_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [插屏广告] 设置为: $enabled")
    }
    
    // ==================== 信息流广告开关 ====================
    
    /**
     * 获取信息流广告开关状态
     */
    fun isFeedAdEnabled(): Boolean {
        if (!isMasterSwitchEnabled()) {
            Log.d(TAG, "🚫 [信息流广告] 主开关已关闭")
            return false
        }
        val enabled = preferences?.getBoolean(KEY_FEED_SWITCH, DEFAULT_FEED_SWITCH) ?: DEFAULT_FEED_SWITCH
        Log.d(TAG, "🔍 [信息流广告] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置信息流广告开关状态
     */
    fun setFeedAdSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_FEED_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [信息流广告] 设置为: $enabled")
    }
    
    // ==================== 激励视频广告开关 ====================
    
    /**
     * 获取激励视频广告开关状态
     */
    fun isRewardVideoAdEnabled(): Boolean {
        if (!isMasterSwitchEnabled()) {
            Log.d(TAG, "🚫 [激励视频广告] 主开关已关闭")
            return false
        }
        val enabled = preferences?.getBoolean(KEY_REWARD_VIDEO_SWITCH, DEFAULT_REWARD_VIDEO_SWITCH) ?: DEFAULT_REWARD_VIDEO_SWITCH
        Log.d(TAG, "🔍 [激励视频广告] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置激励视频广告开关状态
     */
    fun setRewardVideoAdSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_REWARD_VIDEO_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [激励视频广告] 设置为: $enabled")
    }
    
    // ==================== Banner广告开关 ====================
    
    /**
     * 获取Banner广告开关状态
     */
    fun isBannerAdEnabled(): Boolean {
        if (!isMasterSwitchEnabled()) {
            Log.d(TAG, "🚫 [Banner广告] 主开关已关闭")
            return false
        }
        val enabled = preferences?.getBoolean(KEY_BANNER_SWITCH, DEFAULT_BANNER_SWITCH) ?: DEFAULT_BANNER_SWITCH
        Log.d(TAG, "🔍 [Banner广告] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置Banner广告开关状态
     */
    fun setBannerAdSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_BANNER_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [Banner广告] 设置为: $enabled")
    }
    
    // ==================== Draw广告开关 ====================
    
    /**
     * 获取Draw广告开关状态
     */
    fun isDrawAdEnabled(): Boolean {
        if (!isMasterSwitchEnabled()) {
            Log.d(TAG, "🚫 [Draw广告] 主开关已关闭")
            return false
        }
        val enabled = preferences?.getBoolean(KEY_DRAW_SWITCH, DEFAULT_DRAW_SWITCH) ?: DEFAULT_DRAW_SWITCH
        Log.d(TAG, "🔍 [Draw广告] 当前状态: $enabled")
        return enabled
    }
    
    /**
     * 设置Draw广告开关状态
     */
    fun setDrawAdSwitch(enabled: Boolean) {
        preferences?.edit()?.putBoolean(KEY_DRAW_SWITCH, enabled)?.apply()
        Log.i(TAG, "⚙️ [Draw广告] 设置为: $enabled")
    }
    
    // ==================== 远程配置更新 ====================
    
    /**
     * 检查并更新配置
     * 公开方法，供外部调用（如Activity生命周期回调）
     */
    fun checkAndUpdateConfig(context: Context) {
        val lastUpdateTime = preferences?.getLong(KEY_LAST_UPDATE_TIME, 0) ?: 0
        val currentTime = System.currentTimeMillis()
        
        if (currentTime - lastUpdateTime > CONFIG_UPDATE_INTERVAL) {
            Log.d(TAG, "🔄 [配置更新] 开始从服务器更新广告开关配置")
            updateConfigFromServer(context)
        } else {
            Log.d(TAG, "⏰ [配置更新] 距离上次更新时间未超过间隔，跳过更新")
        }
    }
    
    /**
     * 从服务器更新配置
     */
    private fun updateConfigFromServer(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val configUrl = ApiConfig.Ad.GET_SWITCH_CONFIG
                Log.d(TAG, "🌐 [配置更新] 开始请求配置: $configUrl")
                
                // 使用NetworkUtils进行网络请求
                val result = NetworkUtils.get(configUrl)
                
                when (result) {
                    is NetworkResult.Success -> {
                        val response = result.data
                        Log.d(TAG, "📡 [配置更新] 服务器响应: $response")
                        
                        parseAndSaveConfig(response)
                        
                        withContext(Dispatchers.Main) {
                            Log.i(TAG, "✅ [配置更新] 广告开关配置更新成功")
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.w(TAG, "⚠️ [配置更新] 服务器响应错误: ${result.code} - ${result.message}，使用默认配置")
                        // 服务器配置获取失败时，确保使用默认配置
                        ensureDefaultConfig()
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "❌ [配置更新] 网络请求异常: ${result.exception.message}，使用默认配置", result.exception)
                        // 网络异常时，确保使用默认配置
                        ensureDefaultConfig()
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [配置更新] 更新配置失败: ${e.message}，使用默认配置", e)
                // 异常时，确保使用默认配置
                ensureDefaultConfig()
            }
        }
    }
    
    /**
     * 解析并保存配置
     */
    private fun parseAndSaveConfig(jsonResponse: String) {
        try {
            val jsonObject = JSONObject(jsonResponse)

            val code = jsonObject.optInt("code", -1)
            if (code == 200) {
                val data = jsonObject.optJSONObject("data")

                if (data != null) {
                    val editor = preferences?.edit()

                    // 更新各种开关状态
                    if (data.has("masterSwitch")) {
                        editor?.putBoolean(KEY_MASTER_SWITCH, data.getBoolean("masterSwitch"))
                    }
                    if (data.has("splashAdSwitch")) {
                        editor?.putBoolean(KEY_SPLASH_SWITCH, data.getBoolean("splashAdSwitch"))
                    }
                    if (data.has("interstitialAdSwitch")) {
                        editor?.putBoolean(KEY_INTERSTITIAL_SWITCH, data.getBoolean("interstitialAdSwitch"))
                    }
                    if (data.has("feedAdSwitch")) {
                        editor?.putBoolean(KEY_FEED_SWITCH, data.getBoolean("feedAdSwitch"))
                    }
                    if (data.has("rewardVideoAdSwitch")) {
                        editor?.putBoolean(KEY_REWARD_VIDEO_SWITCH, data.getBoolean("rewardVideoAdSwitch"))
                    }
                    if (data.has("bannerAdSwitch")) {
                        editor?.putBoolean(KEY_BANNER_SWITCH, data.getBoolean("bannerAdSwitch"))
                    }
                    if (data.has("drawAdSwitch")) {
                        editor?.putBoolean(KEY_DRAW_SWITCH, data.getBoolean("drawAdSwitch"))
                    }
                    
                    // 更新广告平台选择
                    if (data.has("splashAdPlatform")) {
                        editor?.putString(KEY_SPLASH_PLATFORM, data.getString("splashAdPlatform"))
                    }
                    if (data.has("bannerAdPlatform")) {
                        editor?.putString(KEY_BANNER_PLATFORM, data.getString("bannerAdPlatform"))
                    }
                    if (data.has("interstitialAdPlatform")) {
                        editor?.putString(KEY_INTERSTITIAL_PLATFORM, data.getString("interstitialAdPlatform"))
                    }
                    if (data.has("feedAdPlatform")) {
                        editor?.putString(KEY_FEED_PLATFORM, data.getString("feedAdPlatform"))
                    }
                    if (data.has("rewardVideoAdPlatform")) {
                        editor?.putString(KEY_REWARD_VIDEO_PLATFORM, data.getString("rewardVideoAdPlatform"))
                    }
                    if (data.has("drawAdPlatform")) {
                        editor?.putString(KEY_DRAW_PLATFORM, data.getString("drawAdPlatform"))
                    }
                    
                    // 更新插屏广告配置
                    if (data.has("interstitialAdConfig")) {
                        val interstitialConfig = data.getJSONObject("interstitialAdConfig")
                        if (interstitialConfig.has("continuous_times")) {
                            editor?.putInt(KEY_INTERSTITIAL_CONTINUOUS_TIMES, interstitialConfig.getInt("continuous_times"))
                        }
                        if (interstitialConfig.has("time_interval")) {
                            editor?.putInt(KEY_INTERSTITIAL_TIME_INTERVAL, interstitialConfig.getInt("time_interval"))
                        }
                    }

                    // 更新配置版本和时间
                    if (data.has("version")) {
                        editor?.putInt(KEY_CONFIG_VERSION, data.getInt("version"))
                    }
                    editor?.putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())

                    editor?.apply()

                    Log.i(TAG, "💾 [配置更新] 配置保存成功")
                }
            } else {
                Log.w(TAG, "⚠️ [配置更新] 服务器返回失败状态")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ [配置更新] 解析配置失败: ${e.message}", e)
        }
    }
    
    /**
     * 手动刷新配置
     */
    fun refreshConfig(context: Context, callback: (Boolean, String?) -> Unit) {
        Log.d(TAG, "🔄 [手动刷新] 开始手动刷新广告开关配置")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateConfigFromServer(context)
                withContext(Dispatchers.Main) {
                    callback(true, "配置刷新成功")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false, "配置刷新失败: ${e.message}")
                }
            }
        }
    }
    
    /**
     * 从统一配置管理器的响应中解析和保存配置
     * 避免重复网络请求
     */
    fun parseAndSaveConfigFromResponse(response: String) {
        try {
            Log.d(TAG, "📡 [统一配置] 解析广告开关配置响应")
            parseAndSaveConfig(response)
            Log.d(TAG, "✅ [统一配置] 广告开关配置解析完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [统一配置] 广告开关配置解析失败: ${e.message}")
            ensureDefaultConfig()
        }
    }
    
    /**
     * 确保使用默认配置
     * 当服务器配置获取失败时，确保所有广告开关都使用默认值
     */
    private fun ensureDefaultConfig() {
        val editor = preferences?.edit()
        
        // 如果SharedPreferences中没有配置，则设置默认值
        if (preferences?.contains(KEY_MASTER_SWITCH) != true) {
            editor?.putBoolean(KEY_MASTER_SWITCH, DEFAULT_MASTER_SWITCH)
        }
        if (preferences?.contains(KEY_SPLASH_SWITCH) != true) {
            editor?.putBoolean(KEY_SPLASH_SWITCH, DEFAULT_SPLASH_SWITCH)
        }
        if (preferences?.contains(KEY_INTERSTITIAL_SWITCH) != true) {
            editor?.putBoolean(KEY_INTERSTITIAL_SWITCH, DEFAULT_INTERSTITIAL_SWITCH)
        }
        if (preferences?.contains(KEY_FEED_SWITCH) != true) {
            editor?.putBoolean(KEY_FEED_SWITCH, DEFAULT_FEED_SWITCH)
        }
        if (preferences?.contains(KEY_REWARD_VIDEO_SWITCH) != true) {
            editor?.putBoolean(KEY_REWARD_VIDEO_SWITCH, DEFAULT_REWARD_VIDEO_SWITCH)
        }
        if (preferences?.contains(KEY_BANNER_SWITCH) != true) {
            editor?.putBoolean(KEY_BANNER_SWITCH, DEFAULT_BANNER_SWITCH)
        }
        if (preferences?.contains(KEY_DRAW_SWITCH) != true) {
            editor?.putBoolean(KEY_DRAW_SWITCH, DEFAULT_DRAW_SWITCH)
        }
        
        // 设置默认广告平台
        if (preferences?.contains(KEY_SPLASH_PLATFORM) != true) {
            editor?.putString(KEY_SPLASH_PLATFORM, DEFAULT_SPLASH_PLATFORM)
        }
        if (preferences?.contains(KEY_BANNER_PLATFORM) != true) {
            editor?.putString(KEY_BANNER_PLATFORM, DEFAULT_BANNER_PLATFORM)
        }
        if (preferences?.contains(KEY_INTERSTITIAL_PLATFORM) != true) {
            editor?.putString(KEY_INTERSTITIAL_PLATFORM, DEFAULT_INTERSTITIAL_PLATFORM)
        }
        if (preferences?.contains(KEY_FEED_PLATFORM) != true) {
            editor?.putString(KEY_FEED_PLATFORM, DEFAULT_FEED_PLATFORM)
        }
        if (preferences?.contains(KEY_REWARD_VIDEO_PLATFORM) != true) {
            editor?.putString(KEY_REWARD_VIDEO_PLATFORM, DEFAULT_REWARD_VIDEO_PLATFORM)
        }
        if (preferences?.contains(KEY_DRAW_PLATFORM) != true) {
            editor?.putString(KEY_DRAW_PLATFORM, DEFAULT_DRAW_PLATFORM)
        }
        
        // 设置默认插屏广告配置
        if (preferences?.contains(KEY_INTERSTITIAL_CONTINUOUS_TIMES) != true) {
            editor?.putInt(KEY_INTERSTITIAL_CONTINUOUS_TIMES, DEFAULT_INTERSTITIAL_CONTINUOUS_TIMES)
        }
        if (preferences?.contains(KEY_INTERSTITIAL_TIME_INTERVAL) != true) {
            editor?.putInt(KEY_INTERSTITIAL_TIME_INTERVAL, DEFAULT_INTERSTITIAL_TIME_INTERVAL)
        }
        
        editor?.apply()
        Log.d(TAG, "✅ [AdSwitchConfig] 默认配置设置完成")
    }
    
    // ==================== 广告平台选择 ====================
    
    /**
     * 获取开屏广告平台
     */
    fun getSplashAdPlatform(): String {
        val platform = preferences?.getString(KEY_SPLASH_PLATFORM, DEFAULT_SPLASH_PLATFORM) ?: DEFAULT_SPLASH_PLATFORM
        Log.d(TAG, "🔍 [开屏广告平台] 当前平台: $platform")
        return platform
    }
    
    /**
     * 设置开屏广告平台
     */
    fun setSplashAdPlatform(platform: String) {
        preferences?.edit()?.putString(KEY_SPLASH_PLATFORM, platform)?.apply()
        Log.i(TAG, "⚙️ [开屏广告平台] 设置为: $platform")
    }
    
    /**
     * 获取Banner广告平台
     */
    fun getBannerAdPlatform(): String {
        val platform = preferences?.getString(KEY_BANNER_PLATFORM, DEFAULT_BANNER_PLATFORM) ?: DEFAULT_BANNER_PLATFORM
        Log.d(TAG, "🔍 [Banner广告平台] 当前平台: $platform")
        return platform
    }
    
    /**
     * 设置Banner广告平台
     */
    fun setBannerAdPlatform(platform: String) {
        preferences?.edit()?.putString(KEY_BANNER_PLATFORM, platform)?.apply()
        Log.i(TAG, "⚙️ [Banner广告平台] 设置为: $platform")
    }
    
    /**
     * 获取插屏广告平台
     */
    fun getInterstitialAdPlatform(): String {
        val platform = preferences?.getString(KEY_INTERSTITIAL_PLATFORM, DEFAULT_INTERSTITIAL_PLATFORM) ?: DEFAULT_INTERSTITIAL_PLATFORM
        Log.d(TAG, "🔍 [插屏广告平台] 当前平台: $platform")
        return platform
    }
    
    /**
     * 设置插屏广告平台
     */
    fun setInterstitialAdPlatform(platform: String) {
        preferences?.edit()?.putString(KEY_INTERSTITIAL_PLATFORM, platform)?.apply()
        Log.i(TAG, "⚙️ [插屏广告平台] 设置为: $platform")
    }
    
    /**
     * 获取信息流广告平台
     */
    fun getFeedAdPlatform(): String {
        val platform = preferences?.getString(KEY_FEED_PLATFORM, DEFAULT_FEED_PLATFORM) ?: DEFAULT_FEED_PLATFORM
        Log.d(TAG, "🔍 [信息流广告平台] 当前平台: $platform")
        return platform
    }
    
    /**
     * 设置信息流广告平台
     */
    fun setFeedAdPlatform(platform: String) {
        preferences?.edit()?.putString(KEY_FEED_PLATFORM, platform)?.apply()
        Log.i(TAG, "⚙️ [信息流广告平台] 设置为: $platform")
    }
    
    /**
     * 获取激励视频广告平台
     */
    fun getRewardVideoAdPlatform(): String {
        val platform = preferences?.getString(KEY_REWARD_VIDEO_PLATFORM, DEFAULT_REWARD_VIDEO_PLATFORM) ?: DEFAULT_REWARD_VIDEO_PLATFORM
        Log.d(TAG, "🔍 [激励视频广告平台] 当前平台: $platform")
        return platform
    }
    
    /**
     * 设置激励视频广告平台
     */
    fun setRewardVideoAdPlatform(platform: String) {
        preferences?.edit()?.putString(KEY_REWARD_VIDEO_PLATFORM, platform)?.apply()
        Log.i(TAG, "⚙️ [激励视频广告平台] 设置为: $platform")
    }
    
    /**
     * 获取Draw广告平台
     */
    fun getDrawAdPlatform(): String {
        val platform = preferences?.getString(KEY_DRAW_PLATFORM, DEFAULT_DRAW_PLATFORM) ?: DEFAULT_DRAW_PLATFORM
        Log.d(TAG, "🔍 [Draw广告平台] 当前平台: $platform")
        return platform
    }
    
    /**
     * 设置Draw广告平台
     */
    fun setDrawAdPlatform(platform: String) {
        preferences?.edit()?.putString(KEY_DRAW_PLATFORM, platform)?.apply()
        Log.i(TAG, "⚙️ [Draw广告平台] 设置为: $platform")
    }
    
    // ==================== 插屏广告配置 ====================
    
    /**
     * 获取插屏广告连续展示次数
     */
    fun getInterstitialContinuousTimes(): Int {
        val times = preferences?.getInt(KEY_INTERSTITIAL_CONTINUOUS_TIMES, DEFAULT_INTERSTITIAL_CONTINUOUS_TIMES) ?: DEFAULT_INTERSTITIAL_CONTINUOUS_TIMES
        Log.d(TAG, "🔍 [插屏广告配置] 连续展示次数: $times")
        return times
    }
    
    /**
     * 获取插屏广告时间间隔（秒）
     */
    fun getInterstitialTimeInterval(): Int {
        val interval = preferences?.getInt(KEY_INTERSTITIAL_TIME_INTERVAL, DEFAULT_INTERSTITIAL_TIME_INTERVAL) ?: DEFAULT_INTERSTITIAL_TIME_INTERVAL
        Log.d(TAG, "🔍 [插屏广告配置] 时间间隔: ${interval}秒")
        return interval
    }
    
    /**
     * 设置插屏广告连续展示次数
     */
    fun setInterstitialContinuousTimes(times: Int) {
        preferences?.edit()?.putInt(KEY_INTERSTITIAL_CONTINUOUS_TIMES, times)?.apply()
        Log.i(TAG, "⚙️ [插屏广告配置] 连续展示次数设置为: $times")
    }
    
    /**
     * 设置插屏广告时间间隔（秒）
     */
    fun setInterstitialTimeInterval(interval: Int) {
        preferences?.edit()?.putInt(KEY_INTERSTITIAL_TIME_INTERVAL, interval)?.apply()
        Log.i(TAG, "⚙️ [插屏广告配置] 时间间隔设置为: ${interval}秒")
    }
    
    /**
     * 获取所有开关状态（用于调试）
     */
    fun getAllSwitchStatus(): Map<String, Boolean> {
        return mapOf(
            "masterSwitch" to isMasterSwitchEnabled(),
            "splashAd" to isSplashAdEnabled(),
            "interstitialAd" to isInterstitialAdEnabled(),
            "feedAd" to isFeedAdEnabled(),
            "rewardVideoAd" to isRewardVideoAdEnabled(),
            "bannerAd" to isBannerAdEnabled(),
            "drawAd" to isDrawAdEnabled()
        )
    }
}