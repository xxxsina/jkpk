package com.jiankangpaika.app.ad

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.ad.AdSwitchConfig
import com.jiankangpaika.app.ad.DynamicAdConfig

/**
 * 统一配置管理器
 * 避免重复请求同一个配置接口，统一管理广告开关配置和动态广告配置
 * 解决多次请求ad_config.php接口的问题，并支持长时间操作时的及时更新
 */
class UnifiedConfigManager private constructor() {
    
    companion object {
        private const val TAG = "UnifiedConfigManager"
        private const val PREFS_NAME = "unified_ad_config"
        private const val KEY_LAST_UPDATE_TIME = "last_update_time"
        private const val UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5分钟更新间隔 废弃
//        private const val FORCE_UPDATE_INTERVAL_MS = 5 * 60 * 1000L // 5分钟强制更新间隔
        
        @Volatile
        private var INSTANCE: UnifiedConfigManager? = null
        
        fun getInstance(): UnifiedConfigManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnifiedConfigManager().also { INSTANCE = it }
            }
        }
    }
    
    private var isUpdating = false
    private val updateMutex = Mutex() // 防止并发更新
    private var lastForceUpdateTime = 0L // 记录上次强制更新时间
    
    /**
     * 检查并更新配置
     * 统一管理所有广告配置的更新，避免重复请求
     * 支持强制更新机制，解决长时间操作时无法及时更新的问题
     */
    fun checkAndUpdateConfig(context: Context, forceUpdate: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            updateMutex.withLock {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val currentTime = System.currentTimeMillis()
                val lastUpdateTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0)
                val timeSinceLastUpdate = currentTime - lastUpdateTime
                val timeSinceLastForceUpdate = currentTime - lastForceUpdateTime
                
                val shouldUpdate = !isUpdating && (forceUpdate || 
                    timeSinceLastUpdate > UPDATE_INTERVAL_MS)
                
                Log.d(TAG, "配置更新检查 - 强制更新: $forceUpdate, 正在更新: $isUpdating, 距离上次更新: ${timeSinceLastUpdate}ms, 更新间隔: ${UPDATE_INTERVAL_MS}ms, 应该更新: $shouldUpdate")
                
                if (shouldUpdate) {
                    Log.d(TAG, "开始更新配置 - 强制更新: $forceUpdate, 距离上次更新: ${timeSinceLastUpdate}ms, 距离上次强制更新: ${timeSinceLastForceUpdate}ms")
                    updateConfigFromServer()
                    
                    // 无论成功与否都更新时间戳，避免频繁重试
                    prefs.edit().putLong(KEY_LAST_UPDATE_TIME, currentTime).apply()
                    
                    if (forceUpdate) {
                        lastForceUpdateTime = currentTime
                    }
                } else {
                    Log.d(TAG, "跳过配置更新 - 正在更新: $isUpdating, 强制更新: $forceUpdate, 距离上次更新: ${timeSinceLastUpdate}ms")
                }
            }
        }
    }
    
    /**
     * 从服务器更新配置
     * 统一请求ad_config.php接口，然后分发给各个配置管理器处理
     */
    private suspend fun updateConfigFromServer() {
        isUpdating = true
        Log.d(TAG, "开始从服务器更新配置")
        
        try {
            when (val result = NetworkUtils.get(ApiConfig.Ad.GET_SWITCH_CONFIG)) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "配置更新成功: ${result.data}")
                    
                    // 分发给AdSwitchConfig处理
                    AdSwitchConfig.getInstance().parseAndSaveConfigFromResponse(result.data)
                    
                    // 分发给DynamicAdConfig处理
                    DynamicAdConfig.getInstance().parseAndSaveConfigFromResponse(result.data)
                }
                is NetworkResult.Error -> {
                    Log.e(TAG, "配置更新失败: ${result.message}")
                }
                is NetworkResult.Exception -> {
                    Log.e(TAG, "配置更新异常: ${result.exception.message}", result.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "配置更新过程中发生异常", e)
        } finally {
            isUpdating = false
        }
    }
    
    /**
     * 强制更新配置
     * 立即从服务器获取最新配置
     */
    fun forceUpdateConfig(context: Context) {
        checkAndUpdateConfig(context, true)
    }
    
    /**
     * 获取是否正在更新状态
     */
    fun isUpdating(): Boolean {
        return isUpdating
    }
}