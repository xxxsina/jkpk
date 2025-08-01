package com.jiankangpaika.app.ad

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.constants.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 动态广告配置管理类
 * 负责从服务器获取广告配置参数并本地存储
 */
class DynamicAdConfig private constructor() {
    
    companion object {
        private const val TAG = "DynamicAdConfig"
        private const val PREFS_NAME = "dynamic_ad_config"
        private val CONFIG_URL = ApiConfig.Ad.GET_SWITCH_CONFIG
//        private const val UPDATE_INTERVAL_MS = 20 * 1000L // 20秒间隔，与AdSwitchConfig保持一致
        
        // 配置键名
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val KEY_KUAISHOU_CONFIG = "kuaishou_config"
        private const val KEY_CHUANSHANJIA_CONFIG = "chuanshanjia_config"
        private const val KEY_TAKU_CONFIG = "taku_config"
        
        // 快手默认配置
        private const val DEFAULT_KUAISHOU_APP_ID = "2652700003"
        private const val DEFAULT_KUAISHOU_APP_NAME = "健康派卡"
        private const val DEFAULT_KUAISHOU_SPLASH = "26527000008"
        private const val DEFAULT_KUAISHOU_FEED = "26527000009"
        private const val DEFAULT_KUAISHOU_REWARD_VIDEO = "26527000011"
        private const val DEFAULT_KUAISHOU_INTERSTITIAL = "26527000010"
        private const val DEFAULT_KUAISHOU_BANNER = "26527000013"
        private const val DEFAULT_KUAISHOU_DRAW_VIDEO = "26527000014"
        
        // 穿山甲默认配置
        private const val DEFAULT_CHUANSHANJIA_APP_ID = "5718785"
        private const val DEFAULT_CHUANSHANJIA_APP_NAME = "健康派卡"
        private const val DEFAULT_CHUANSHANJIA_SPLASH = "892015291"
        private const val DEFAULT_CHUANSHANJIA_FEED = "968273037"
        private const val DEFAULT_CHUANSHANJIA_REWARD_VIDEO = "968273038"
        private const val DEFAULT_CHUANSHANJIA_INTERSTITIAL = "968276216"
        private const val DEFAULT_CHUANSHANJIA_BANNER = "968276215"
        private const val DEFAULT_CHUANSHANJIA_DRAW_VIDEO = "968276217"
        
        // Taku广告默认配置
        private const val DEFAULT_TAKU_APP_ID = "a687e57e2e68d0"
        private const val DEFAULT_TAKU_APP_KEY = "af361b82bcdb21b0c88cbf630fd016b97"
        private const val DEFAULT_TAKU_APP_NAME = "健康派卡"
        private const val DEFAULT_TAKU_SPLASH = "b687e61873b69a"
        private const val DEFAULT_TAKU_BANNER = "b687e617160a6e"
        private const val DEFAULT_TAKU_REWARD_VIDEO = "b687e60efa56a9"
        private const val DEFAULT_TAKU_INTERSTITIAL = "b687e6100364a8"
        private const val DEFAULT_TAKU_FEED = "b687e6160a5b18"
        private const val DEFAULT_TAKU_DRAW_VIDEO = "b66e8c8b5b5e8c91"
        
        @Volatile
        private var INSTANCE: DynamicAdConfig? = null
        
        fun getInstance(): DynamicAdConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DynamicAdConfig().also { INSTANCE = it }
            }
        }
    }
    
    private var sharedPreferences: SharedPreferences? = null
    
    /**
     * 初始化配置管理器
     */
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 确保有默认配置
        ensureDefaultConfig()
    }
    
    /**
     * 检查并更新配置
     */
//    fun checkAndUpdateConfig(context: Context) {
//        val currentTime = System.currentTimeMillis()
//        val lastUpdateTime = sharedPreferences?.getLong(KEY_LAST_UPDATE_TIME, 0) ?: 0
//
//        if (currentTime - lastUpdateTime >= UPDATE_INTERVAL_MS) {
//            Log.d(TAG, "配置更新间隔已到，开始更新配置")
//            updateConfigFromServer(context)
//        } else {
//            Log.d(TAG, "配置更新间隔未到，跳过更新")
//        }
//    }
    
    /**
     * 从服务器更新配置
     */
    private fun updateConfigFromServer(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "开始从服务器获取动态广告配置")
                
                when (val result = NetworkUtils.get(CONFIG_URL)) {
                    is NetworkResult.Success -> {
                        val jsonResponse = JSONObject(result.data)
                        val code = jsonResponse.optInt("code", -1)
                        
                        if (code == 200) {
                            val data = jsonResponse.optJSONObject("data")
                            if (data != null) {
                                parseAndSaveConfig(data)
                                Log.d(TAG, "动态广告配置更新成功")
                            } else {
                                Log.e(TAG, "服务器返回数据为空")
                            }
                        } else {
                            val message = jsonResponse.optString("message", "未知错误")
                            Log.e(TAG, "服务器返回错误: code=$code, message=$message")
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "网络请求失败: ${result.message}")
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "网络请求异常: ${result.exception.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "更新配置时发生异常", e)
            }
        }
    }
    
    /**
     * 解析并保存配置
     */
    private fun parseAndSaveConfig(data: JSONObject) {
        try {
            val editor = sharedPreferences?.edit() ?: return
            
            // 解析快手配置
            val kuaishouConfig = data.optJSONObject("kuaishouConfig")
            if (kuaishouConfig != null) {
                editor.putString("${KEY_KUAISHOU_CONFIG}_APP_ID", kuaishouConfig.optString("APP_ID", DEFAULT_KUAISHOU_APP_ID))
                editor.putString("${KEY_KUAISHOU_CONFIG}_APP_NAME", kuaishouConfig.optString("APP_NAME", DEFAULT_KUAISHOU_APP_NAME))
                editor.putString("${KEY_KUAISHOU_CONFIG}_SPLASH", kuaishouConfig.optString("SPLASH", DEFAULT_KUAISHOU_SPLASH))
                editor.putString("${KEY_KUAISHOU_CONFIG}_FEED", kuaishouConfig.optString("FEED", DEFAULT_KUAISHOU_FEED))
                editor.putString("${KEY_KUAISHOU_CONFIG}_REWARD_VIDEO", kuaishouConfig.optString("REWARD_VIDEO", DEFAULT_KUAISHOU_REWARD_VIDEO))
                editor.putString("${KEY_KUAISHOU_CONFIG}_INTERSTITIAL", kuaishouConfig.optString("INTERSTITIAL", DEFAULT_KUAISHOU_INTERSTITIAL))
                editor.putString("${KEY_KUAISHOU_CONFIG}_BANNER", kuaishouConfig.optString("BANNER", DEFAULT_KUAISHOU_BANNER))
                editor.putString("${KEY_KUAISHOU_CONFIG}_DRAW_VIDEO", kuaishouConfig.optString("DRAW_VIDEO", DEFAULT_KUAISHOU_DRAW_VIDEO))
            }
            
            // 解析穿山甲配置
            val chuanshanjiaConfig = data.optJSONObject("chuanshanjiaConfig")
            if (chuanshanjiaConfig != null) {
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_APP_ID", chuanshanjiaConfig.optString("APP_ID", DEFAULT_CHUANSHANJIA_APP_ID))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_APP_NAME", chuanshanjiaConfig.optString("APP_NAME", DEFAULT_CHUANSHANJIA_APP_NAME))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_SPLASH", chuanshanjiaConfig.optString("SPLASH", DEFAULT_CHUANSHANJIA_SPLASH))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_FEED", chuanshanjiaConfig.optString("FEED", DEFAULT_CHUANSHANJIA_FEED))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_REWARD_VIDEO", chuanshanjiaConfig.optString("REWARD_VIDEO", DEFAULT_CHUANSHANJIA_REWARD_VIDEO))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_INTERSTITIAL", chuanshanjiaConfig.optString("INTERSTITIAL", DEFAULT_CHUANSHANJIA_INTERSTITIAL))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_BANNER", chuanshanjiaConfig.optString("BANNER", DEFAULT_CHUANSHANJIA_BANNER))
                editor.putString("${KEY_CHUANSHANJIA_CONFIG}_DRAW_VIDEO", chuanshanjiaConfig.optString("DRAW_VIDEO", DEFAULT_CHUANSHANJIA_DRAW_VIDEO))
            }
            
            // 解析Taku配置
            val takuConfig = data.optJSONObject("takuConfig")
            if (takuConfig != null) {
                editor.putString("${KEY_TAKU_CONFIG}_APP_ID", takuConfig.optString("APP_ID", DEFAULT_TAKU_APP_ID))
                editor.putString("${KEY_TAKU_CONFIG}_APP_KEY", takuConfig.optString("APP_KEY", DEFAULT_TAKU_APP_KEY))
                editor.putString("${KEY_TAKU_CONFIG}_APP_NAME", takuConfig.optString("APP_NAME", DEFAULT_TAKU_APP_NAME))
                editor.putString("${KEY_TAKU_CONFIG}_SPLASH", takuConfig.optString("SPLASH", DEFAULT_TAKU_SPLASH))
                editor.putString("${KEY_TAKU_CONFIG}_BANNER", takuConfig.optString("BANNER", DEFAULT_TAKU_BANNER))
                editor.putString("${KEY_TAKU_CONFIG}_REWARD_VIDEO", takuConfig.optString("REWARD_VIDEO", DEFAULT_TAKU_REWARD_VIDEO))
                editor.putString("${KEY_TAKU_CONFIG}_INTERSTITIAL", takuConfig.optString("INTERSTITIAL", DEFAULT_TAKU_INTERSTITIAL))
                editor.putString("${KEY_TAKU_CONFIG}_FEED", takuConfig.optString("FEED", DEFAULT_TAKU_FEED))
                editor.putString("${KEY_TAKU_CONFIG}_DRAW_VIDEO", takuConfig.optString("DRAW_VIDEO", DEFAULT_TAKU_DRAW_VIDEO))
            }
            
            // 更新最后更新时间
            editor.putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            editor.apply()
            
            Log.d(TAG, "动态广告配置保存成功")
        } catch (e: Exception) {
            Log.e(TAG, "解析配置时发生异常", e)
        }
    }
    
    /**
     * 确保有默认配置
     */
    private fun ensureDefaultConfig() {
        val editor = sharedPreferences?.edit() ?: return
        
        // 快手默认配置
        if (!sharedPreferences!!.contains("${KEY_KUAISHOU_CONFIG}_APP_ID")) {
            editor.putString("${KEY_KUAISHOU_CONFIG}_APP_ID", DEFAULT_KUAISHOU_APP_ID)
            editor.putString("${KEY_KUAISHOU_CONFIG}_APP_NAME", DEFAULT_KUAISHOU_APP_NAME)
            editor.putString("${KEY_KUAISHOU_CONFIG}_SPLASH", DEFAULT_KUAISHOU_SPLASH)
            editor.putString("${KEY_KUAISHOU_CONFIG}_FEED", DEFAULT_KUAISHOU_FEED)
            editor.putString("${KEY_KUAISHOU_CONFIG}_REWARD_VIDEO", DEFAULT_KUAISHOU_REWARD_VIDEO)
            editor.putString("${KEY_KUAISHOU_CONFIG}_INTERSTITIAL", DEFAULT_KUAISHOU_INTERSTITIAL)
            editor.putString("${KEY_KUAISHOU_CONFIG}_BANNER", DEFAULT_KUAISHOU_BANNER)
            editor.putString("${KEY_KUAISHOU_CONFIG}_DRAW_VIDEO", DEFAULT_KUAISHOU_DRAW_VIDEO)
        }
        
        // 穿山甲默认配置
        if (!sharedPreferences!!.contains("${KEY_CHUANSHANJIA_CONFIG}_APP_ID")) {
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_APP_ID", DEFAULT_CHUANSHANJIA_APP_ID)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_APP_NAME", DEFAULT_CHUANSHANJIA_APP_NAME)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_SPLASH", DEFAULT_CHUANSHANJIA_SPLASH)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_FEED", DEFAULT_CHUANSHANJIA_FEED)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_REWARD_VIDEO", DEFAULT_CHUANSHANJIA_REWARD_VIDEO)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_INTERSTITIAL", DEFAULT_CHUANSHANJIA_INTERSTITIAL)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_BANNER", DEFAULT_CHUANSHANJIA_BANNER)
            editor.putString("${KEY_CHUANSHANJIA_CONFIG}_DRAW_VIDEO", DEFAULT_CHUANSHANJIA_DRAW_VIDEO)
        }
        
        // Taku默认配置
        if (!sharedPreferences!!.contains("${KEY_TAKU_CONFIG}_APP_ID")) {
            editor.putString("${KEY_TAKU_CONFIG}_APP_ID", DEFAULT_TAKU_APP_ID)
            editor.putString("${KEY_TAKU_CONFIG}_APP_KEY", DEFAULT_TAKU_APP_KEY)
            editor.putString("${KEY_TAKU_CONFIG}_APP_NAME", DEFAULT_TAKU_APP_NAME)
            editor.putString("${KEY_TAKU_CONFIG}_SPLASH", DEFAULT_TAKU_SPLASH)
            editor.putString("${KEY_TAKU_CONFIG}_BANNER", DEFAULT_TAKU_BANNER)
            editor.putString("${KEY_TAKU_CONFIG}_REWARD_VIDEO", DEFAULT_TAKU_REWARD_VIDEO)
            editor.putString("${KEY_TAKU_CONFIG}_INTERSTITIAL", DEFAULT_TAKU_INTERSTITIAL)
            editor.putString("${KEY_TAKU_CONFIG}_FEED", DEFAULT_TAKU_FEED)
            editor.putString("${KEY_TAKU_CONFIG}_DRAW_VIDEO", DEFAULT_TAKU_DRAW_VIDEO)
        }
        
        editor.apply()
    }
    
    // 快手配置获取方法
    fun getKuaishouAppId(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_APP_ID", DEFAULT_KUAISHOU_APP_ID) ?: DEFAULT_KUAISHOU_APP_ID
    fun getKuaishouAppName(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_APP_NAME", DEFAULT_KUAISHOU_APP_NAME) ?: DEFAULT_KUAISHOU_APP_NAME
    fun getKuaishouSplash(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_SPLASH", DEFAULT_KUAISHOU_SPLASH) ?: DEFAULT_KUAISHOU_SPLASH
    fun getKuaishouFeed(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_FEED", DEFAULT_KUAISHOU_FEED) ?: DEFAULT_KUAISHOU_FEED
    fun getKuaishouRewardVideo(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_REWARD_VIDEO", DEFAULT_KUAISHOU_REWARD_VIDEO) ?: DEFAULT_KUAISHOU_REWARD_VIDEO
    fun getKuaishouInterstitial(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_INTERSTITIAL", DEFAULT_KUAISHOU_INTERSTITIAL) ?: DEFAULT_KUAISHOU_INTERSTITIAL
    fun getKuaishouBanner(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_BANNER", DEFAULT_KUAISHOU_BANNER) ?: DEFAULT_KUAISHOU_BANNER
    fun getKuaishouDrawVideo(): String = sharedPreferences?.getString("${KEY_KUAISHOU_CONFIG}_DRAW_VIDEO", DEFAULT_KUAISHOU_DRAW_VIDEO) ?: DEFAULT_KUAISHOU_DRAW_VIDEO
    
    // 穿山甲配置获取方法
    fun getChuanshanjiaAppId(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_APP_ID", DEFAULT_CHUANSHANJIA_APP_ID) ?: DEFAULT_CHUANSHANJIA_APP_ID
    fun getChuanshanjiaAppName(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_APP_NAME", DEFAULT_CHUANSHANJIA_APP_NAME) ?: DEFAULT_CHUANSHANJIA_APP_NAME
    fun getChuanshanjiaSplash(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_SPLASH", DEFAULT_CHUANSHANJIA_SPLASH) ?: DEFAULT_CHUANSHANJIA_SPLASH
    fun getChuanshanjiaFeed(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_FEED", DEFAULT_CHUANSHANJIA_FEED) ?: DEFAULT_CHUANSHANJIA_FEED
    fun getChuanshanjiaRewardVideo(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_REWARD_VIDEO", DEFAULT_CHUANSHANJIA_REWARD_VIDEO) ?: DEFAULT_CHUANSHANJIA_REWARD_VIDEO
    fun getChuanshanjiaInterstitial(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_INTERSTITIAL", DEFAULT_CHUANSHANJIA_INTERSTITIAL) ?: DEFAULT_CHUANSHANJIA_INTERSTITIAL
    fun getChuanshanjiaBanner(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_BANNER", DEFAULT_CHUANSHANJIA_BANNER) ?: DEFAULT_CHUANSHANJIA_BANNER
    fun getChuanshanjiaDrawVideo(): String = sharedPreferences?.getString("${KEY_CHUANSHANJIA_CONFIG}_DRAW_VIDEO", DEFAULT_CHUANSHANJIA_DRAW_VIDEO) ?: DEFAULT_CHUANSHANJIA_DRAW_VIDEO
    
    // Taku配置获取方法
    fun getTakuAppId(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_APP_ID", DEFAULT_TAKU_APP_ID) ?: DEFAULT_TAKU_APP_ID
    fun getTakuAppKey(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_APP_KEY", DEFAULT_TAKU_APP_KEY) ?: DEFAULT_TAKU_APP_KEY
    fun getTakuAppName(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_APP_NAME", DEFAULT_TAKU_APP_NAME) ?: DEFAULT_TAKU_APP_NAME
    fun getTakuSplash(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_SPLASH", DEFAULT_TAKU_SPLASH) ?: DEFAULT_TAKU_SPLASH
    fun getTakuBanner(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_BANNER", DEFAULT_TAKU_BANNER) ?: DEFAULT_TAKU_BANNER
    fun getTakuRewardVideo(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_REWARD_VIDEO", DEFAULT_TAKU_REWARD_VIDEO) ?: DEFAULT_TAKU_REWARD_VIDEO
    fun getTakuInterstitial(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_INTERSTITIAL", DEFAULT_TAKU_INTERSTITIAL) ?: DEFAULT_TAKU_INTERSTITIAL
    fun getTakuFeed(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_FEED", DEFAULT_TAKU_FEED) ?: DEFAULT_TAKU_FEED
    fun getTakuDrawVideo(): String = sharedPreferences?.getString("${KEY_TAKU_CONFIG}_DRAW_VIDEO", DEFAULT_TAKU_DRAW_VIDEO) ?: DEFAULT_TAKU_DRAW_VIDEO
    
    /**
     * 手动刷新配置
     */
    fun refreshConfig(context: Context) {
        Log.d(TAG, "手动刷新动态广告配置")
        updateConfigFromServer(context)
    }
    
    /**
     * 从统一配置管理器的响应中解析和保存配置
     * 避免重复网络请求
     */
    fun parseAndSaveConfigFromResponse(response: String) {
        try {
            Log.d(TAG, "📡 [统一配置] 解析动态广告配置响应")
            val jsonResponse = org.json.JSONObject(response)
            val code = jsonResponse.optInt("code", -1)
            
            if (code == 200) {
                val data = jsonResponse.optJSONObject("data")
                if (data != null) {
                    parseAndSaveConfig(data)
                    Log.d(TAG, "✅ [统一配置] 动态广告配置解析完成")
                } else {
                    Log.e(TAG, "❌ [统一配置] 服务器返回数据为空")
                }
            } else {
                val message = jsonResponse.optString("message", "未知错误")
                Log.e(TAG, "❌ [统一配置] 服务器返回错误: code=$code, message=$message")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ [统一配置] 动态广告配置解析失败: ${e.message}")
        }
    }
    
    /**
     * 获取所有配置状态（用于调试）
     */
    fun getAllConfigStatus(): String {
        return buildString {
            appendLine("=== 动态广告配置状态 ===")
            appendLine("快手配置:")
            appendLine("  APP_ID: ${getKuaishouAppId()}")
            appendLine("  APP_NAME: ${getKuaishouAppName()}")
            appendLine("  SPLASH: ${getKuaishouSplash()}")
            appendLine("  FEED: ${getKuaishouFeed()}")
            appendLine("  REWARD_VIDEO: ${getKuaishouRewardVideo()}")
            appendLine("  INTERSTITIAL: ${getKuaishouInterstitial()}")
            appendLine("  BANNER: ${getKuaishouBanner()}")
            appendLine("  DRAW_VIDEO: ${getKuaishouDrawVideo()}")
            appendLine("穿山甲配置:")
            appendLine("  APP_ID: ${getChuanshanjiaAppId()}")
            appendLine("  APP_NAME: ${getChuanshanjiaAppName()}")
            appendLine("  SPLASH: ${getChuanshanjiaSplash()}")
            appendLine("  FEED: ${getChuanshanjiaFeed()}")
            appendLine("  REWARD_VIDEO: ${getChuanshanjiaRewardVideo()}")
            appendLine("  INTERSTITIAL: ${getChuanshanjiaInterstitial()}")
            appendLine("  BANNER: ${getChuanshanjiaBanner()}")
            appendLine("  DRAW_VIDEO: ${getChuanshanjiaDrawVideo()}")
            appendLine("Taku配置:")
            appendLine("  APP_ID: ${getTakuAppId()}")
            appendLine("  APP_KEY: ${getTakuAppKey()}")
            appendLine("  APP_NAME: ${getTakuAppName()}")
            appendLine("  SPLASH: ${getTakuSplash()}")
            appendLine("  BANNER: ${getTakuBanner()}")
            appendLine("  REWARD_VIDEO: ${getTakuRewardVideo()}")
            appendLine("  INTERSTITIAL: ${getTakuInterstitial()}")
            appendLine("  FEED: ${getTakuFeed()}")
            appendLine("  DRAW_VIDEO: ${getTakuDrawVideo()}")
            val lastUpdateTime = sharedPreferences?.getLong(KEY_LAST_UPDATE_TIME, 0) ?: 0
            appendLine("最后更新时间: ${if (lastUpdateTime > 0) java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastUpdateTime)) else "未更新"}")
        }
    }
}