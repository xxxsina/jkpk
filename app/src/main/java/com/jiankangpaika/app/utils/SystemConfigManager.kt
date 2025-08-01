package com.jiankangpaika.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.jiankangpaika.app.data.model.SystemConfigResponse
import com.jiankangpaika.app.data.model.CustomerServiceConfig
import com.jiankangpaika.app.data.model.QuickAppConfig
import com.jiankangpaika.app.utils.constants.ApiConfig
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 系统配置管理器
 * 负责系统配置的获取、缓存和自动更新
 */
object SystemConfigManager {
    private const val TAG = "SystemConfigManager"
    private const val PREF_NAME = "system_config_preferences"
    private const val UPDATE_INTERVAL = 5 * 60 * 1000L // 5分钟
    
    // SharedPreferences键名
    private const val KEY_LAST_UPDATE_TIME = "last_update_time"
    private const val KEY_IMAGE_UPLOAD_ENABLED = "image_upload_enabled"
    private const val KEY_VIDEO_UPLOAD_ENABLED = "video_upload_enabled"
    private const val KEY_MAX_IMAGE_SIZE = "max_image_size"
    private const val KEY_MAX_VIDEO_SIZE = "max_video_size"
    private const val KEY_ALLOWED_IMAGE_TYPES = "allowed_image_types"
    private const val KEY_ALLOWED_VIDEO_TYPES = "allowed_video_types"
    private const val KEY_QUICK_APP_ENABLED = "quick_app_enabled"
    private const val KEY_QUICK_APP_NAME = "quick_app_name"
    
    private var updateJob: Job? = null
    private val isUpdating = AtomicBoolean(false)
    
    /**
     * 获取SharedPreferences实例
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 启动自动更新
     * @param context 上下文
     */
    fun startAutoUpdate(context: Context) {
        Log.d(TAG, "🔄 [自动更新] 启动系统配置自动更新")
        
        // 取消之前的更新任务
        stopAutoUpdate()
        
        updateJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    updateConfig(context)
                    delay(UPDATE_INTERVAL)
                } catch (e: Exception) {
                    Log.e(TAG, "💥 [自动更新] 自动更新异常: ${e.message}", e)
                    delay(UPDATE_INTERVAL) // 出错后也要等待
                }
            }
        }
    }
    
    /**
     * 停止自动更新
     */
    fun stopAutoUpdate() {
        updateJob?.cancel()
        updateJob = null
        Log.d(TAG, "⏹️ [自动更新] 停止系统配置自动更新")
    }
    
    /**
     * 更新系统配置
     * @param context 上下文
     */
    private suspend fun updateConfig(context: Context) {
        if (isUpdating.get()) {
            Log.d(TAG, "⏳ [配置更新] 正在更新中，跳过本次更新")
            return
        }
        
        isUpdating.set(true)
        
        try {
            Log.d(TAG, "🌐 [配置更新] 开始获取系统配置")
            
            val result = NetworkUtils.get(ApiConfig.System.GET_CONFIG, context)
            
            when (result) {
                is NetworkResult.Success -> {
                    val configResponse = NetworkUtils.parseJson<SystemConfigResponse>(result.data)
                    if (configResponse?.isSuccess() == true && configResponse.data != null) {
                        saveConfig(context, configResponse.data.customerService, configResponse.data.quickApp)
                        Log.d(TAG, "✅ [配置更新] 系统配置更新成功")
                    } else {
                        Log.w(TAG, "⚠️ [配置更新] 系统配置响应格式错误: ${configResponse?.message}")
                    }
                }
                is NetworkResult.Error -> {
                    Log.w(TAG, "⚠️ [配置更新] 获取系统配置失败: ${result.message}")
                }
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [配置更新] 获取系统配置异常: ${result.exception.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [配置更新] 更新配置异常: ${e.message}", e)
        } finally {
            isUpdating.set(false)
        }
    }
    
    /**
     * 保存配置到本地
     */
    private fun saveConfig(context: Context, customerService: CustomerServiceConfig, quickApp: QuickAppConfig) {
        with(getPreferences(context).edit()) {
            putLong(KEY_LAST_UPDATE_TIME, System.currentTimeMillis())
            putBoolean(KEY_IMAGE_UPLOAD_ENABLED, customerService.imageUploadEnabled)
            putBoolean(KEY_VIDEO_UPLOAD_ENABLED, customerService.videoUploadEnabled)
            putString(KEY_MAX_IMAGE_SIZE, customerService.maxImageSizeMB)
            putString(KEY_MAX_VIDEO_SIZE, customerService.maxVideoSizeMB)
            putString(KEY_ALLOWED_IMAGE_TYPES, customerService.allowedImageTypes)
            putString(KEY_ALLOWED_VIDEO_TYPES, customerService.allowedVideoTypes)
            putBoolean(KEY_QUICK_APP_ENABLED, quickApp.enabled)
            putString(KEY_QUICK_APP_NAME, quickApp.appName)
            apply()
        }
        
        Log.d(TAG, "💾 [配置保存] 系统配置已保存到本地")
    }
    
    /**
     * 获取客服配置
     * @param context 上下文
     * @return 客服配置
     */
    fun getCustomerServiceConfig(context: Context): CustomerServiceConfig {
        val prefs = getPreferences(context)
        return CustomerServiceConfig(
            imageUploadEnabled = prefs.getBoolean(KEY_IMAGE_UPLOAD_ENABLED, true),
            videoUploadEnabled = prefs.getBoolean(KEY_VIDEO_UPLOAD_ENABLED, true),
            maxImageSizeMB = prefs.getString(KEY_MAX_IMAGE_SIZE, "10") ?: "10",
            maxVideoSizeMB = prefs.getString(KEY_MAX_VIDEO_SIZE, "50") ?: "50",
            allowedImageTypes = prefs.getString(KEY_ALLOWED_IMAGE_TYPES, "jpg,jpeg,png,gif") ?: "jpg,jpeg,png,gif",
            allowedVideoTypes = prefs.getString(KEY_ALLOWED_VIDEO_TYPES, "mp4,avi,mov,wmv") ?: "mp4,avi,mov,wmv"
        )
    }
    
    /**
     * 获取快应用配置
     * @param context 上下文
     * @return 快应用配置
     */
    fun getQuickAppConfig(context: Context): QuickAppConfig {
        val prefs = getPreferences(context)
        return QuickAppConfig(
            enabled = prefs.getBoolean(KEY_QUICK_APP_ENABLED, false),
            appName = prefs.getString(KEY_QUICK_APP_NAME, "") ?: ""
        )
    }
    
    /**
     * 检查是否需要更新配置
     * @param context 上下文
     * @return 是否需要更新
     */
    fun needsUpdate(context: Context): Boolean {
        val lastUpdateTime = getPreferences(context).getLong(KEY_LAST_UPDATE_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastUpdateTime) > UPDATE_INTERVAL
    }
    
    /**
     * 手动更新配置
     * @param context 上下文
     */
    suspend fun manualUpdate(context: Context) {
        updateConfig(context)
    }
    
    /**
     * 图片上传是否启用
     * @param context 上下文
     * @return 是否启用
     */
    fun isImageUploadEnabled(context: Context): Boolean {
        return getCustomerServiceConfig(context).imageUploadEnabled
    }
    
    /**
     * 视频上传是否启用
     * @param context 上下文
     * @return 是否启用
     */
    fun isVideoUploadEnabled(context: Context): Boolean {
        return getCustomerServiceConfig(context).videoUploadEnabled
    }
    
    /**
     * 快应用是否启用
     * @param context 上下文
     * @return 是否启用
     */
    fun isQuickAppEnabled(context: Context): Boolean {
        return getQuickAppConfig(context).enabled
    }
    
    /**
     * 获取快应用名称
     * @param context 上下文
     * @return 快应用名称
     */
    fun getQuickAppName(context: Context): String {
        return getQuickAppConfig(context).appName
    }
}