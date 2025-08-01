package com.jiankangpaika.app.ad

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * 配置监控管理器
 * 负责实时监控广告配置更新，支持配置监控间隔和暂停/恢复机制
 * 类似于版本监控，但专门用于广告配置的自动更新
 */
class ConfigMonitorManager private constructor(private val context: Context) : Application.ActivityLifecycleCallbacks {
    
    companion object {
        private const val TAG = "ConfigMonitorManager"
        // 使用AdSwitchConfig中的CONFIG_UPDATE_INTERVAL作为默认间隔
        private const val DEFAULT_MONITOR_INTERVAL = 300 * 1000L // 300秒，与AdSwitchConfig保持一致
        private const val PREFS_NAME = "config_monitor_prefs"
        private const val KEY_MONITOR_INTERVAL = "config_monitor_interval"
        private const val KEY_MONITOR_ENABLED = "config_monitor_enabled"
        
        @Volatile
        private var INSTANCE: ConfigMonitorManager? = null
        
        fun getInstance(context: Context): ConfigMonitorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ConfigMonitorManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 当前Activity的弱引用
    private var currentActivityRef: WeakReference<Activity>? = null
    
    init {
        // 注册Activity生命周期回调
        try {
            val application = context.applicationContext as? Application
            application?.registerActivityLifecycleCallbacks(this)
            Log.d(TAG, "📱 [配置监控] 已注册Activity生命周期回调")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [配置监控] 注册Activity生命周期回调失败: ${e.message}")
        }
    }
    
    private var monitorJob: Job? = null
    private var isMonitoring = false
    private var isPaused = false
    
    // 监控间隔，可配置
    private var monitorInterval: Long
        get() = prefs.getLong(KEY_MONITOR_INTERVAL, DEFAULT_MONITOR_INTERVAL)
        set(value) = prefs.edit().putLong(KEY_MONITOR_INTERVAL, value).apply()
    
    // 监控开关，可配置
    private var isMonitorEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_ENABLED, value).apply()
    
    /**
     * 启动配置监控
     */
    fun startMonitoring() {
        if (!isMonitorEnabled) {
            Log.d(TAG, "📴 [配置监控] 监控已被禁用，跳过启动")
            return
        }
        
        if (isMonitoring) {
            Log.d(TAG, "⚠️ [配置监控] 监控已在运行中")
            return
        }
        
        Log.i(TAG, "🚀 [配置监控] 启动配置监控，间隔: ${monitorInterval / 1000}秒")
        isMonitoring = true
        isPaused = false
        
        monitorJob = CoroutineScope(Dispatchers.Main).launch {
            while (isMonitoring) {
                try {
                    if (!isPaused) {
                        Log.d(TAG, "🔍 [配置监控] 执行定时配置检查")
                        performConfigCheck()
                    } else {
                        Log.d(TAG, "⏸️ [配置监控] 监控已暂停，跳过检查")
                    }
                    
                    // 等待下一次检查
                    delay(monitorInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "💥 [配置监控] 监控过程中发生异常: ${e.message}", e)
                    // 发生异常时等待一段时间再继续
                    delay(30000) // 30秒后重试
                }
            }
        }
    }
    
    /**
     * 停止配置监控
     */
    fun stopMonitoring() {
        Log.i(TAG, "🛑 [配置监控] 停止配置监控")
        isMonitoring = false
        isPaused = false
        monitorJob?.cancel()
        monitorJob = null
        
        // 取消注册Activity生命周期回调
        try {
            val application = context.applicationContext as? Application
            application?.unregisterActivityLifecycleCallbacks(this)
            Log.d(TAG, "📱 [配置监控] 已取消注册Activity生命周期回调")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [配置监控] 取消注册Activity生命周期回调失败: ${e.message}")
        }
    }
    
    /**
     * 暂停配置监控
     * 当出现提示层或其他需要暂停的情况时调用
     */
    fun pauseMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "⏸️ [配置监控] 暂停配置监控")
            isPaused = true
        }
    }
    
    /**
     * 恢复配置监控
     * 当暂停条件解除时调用
     */
    fun resumeMonitoring() {
        if (isMonitoring && isPaused) {
            Log.i(TAG, "▶️ [配置监控] 恢复配置监控")
            isPaused = false
        }
    }
    
    /**
     * 执行一次配置检查（应用启动时调用）
     */
    fun performStartupCheck() {
        Log.i(TAG, "🔍 [配置监控] 执行应用启动配置检查")
        performConfigCheck()
    }
    
    /**
     * 获取当前Activity
     */
    private fun getCurrentActivity(): Activity? {
        return currentActivityRef?.get()
    }
    
    /**
     * 执行配置检查
     * 使用UnifiedConfigManager进行统一配置更新
     * 配置监控系统使用强制更新，忽略UnifiedConfigManager的时间间隔限制
     */
    private fun performConfigCheck() {
        try {
            // 配置监控系统使用强制更新，确保按照监控间隔进行更新
            // 这样可以实现真正的自动配置监控，而不受UnifiedConfigManager的5分钟限制
            UnifiedConfigManager.getInstance().checkAndUpdateConfig(context, true)
            Log.d(TAG, "✅ [配置监控] 配置检查完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [配置监控] 配置检查失败: ${e.message}", e)
        }
    }
    
    /**
     * 强制执行一次配置更新
     * 忽略时间间隔限制
     */
    fun forceConfigUpdate() {
        Log.i(TAG, "🔄 [配置监控] 强制执行配置更新")
        try {
            UnifiedConfigManager.getInstance().forceUpdateConfig(context)
            Log.d(TAG, "✅ [配置监控] 强制配置更新完成")
        } catch (e: Exception) {
            Log.e(TAG, "❌ [配置监控] 强制配置更新失败: ${e.message}", e)
        }
    }
    
    /**
     * 设置监控间隔
     * @param intervalMs 监控间隔（毫秒）
     */
    fun updateMonitorInterval(intervalMs: Long) {
        if (intervalMs < 5000) { // 最小5秒
            Log.w(TAG, "⚠️ [配置监控] 监控间隔不能小于5秒，已设置为5秒")
            monitorInterval = 5000
        } else {
            monitorInterval = intervalMs
            Log.i(TAG, "⚙️ [配置监控] 设置监控间隔: ${intervalMs / 1000}秒")
        }
        
        // 如果正在监控，重启监控以应用新间隔
        if (isMonitoring) {
            Log.d(TAG, "🔄 [配置监控] 重启监控以应用新间隔")
            stopMonitoring()
            startMonitoring()
        }
    }
    
    /**
     * 启用/禁用监控
     * @param enabled 是否启用
     */
    fun updateMonitorEnabled(enabled: Boolean) {
        isMonitorEnabled = enabled
        Log.i(TAG, "⚙️ [配置监控] 设置监控状态: ${if (enabled) "启用" else "禁用"}")
        
        if (enabled) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }
    
    /**
     * 获取当前监控状态
     */
    fun getMonitorStatus(): MonitorStatus {
        return MonitorStatus(
            isEnabled = isMonitorEnabled,
            isRunning = isMonitoring,
            isPaused = isPaused,
            intervalMs = monitorInterval
        )
    }
    
    /**
     * 监控状态数据类
     */
    data class MonitorStatus(
        val isEnabled: Boolean,
        val isRunning: Boolean,
        val isPaused: Boolean,
        val intervalMs: Long
    )
    
    // Activity生命周期回调实现
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    
    override fun onActivityStarted(activity: Activity) {}
    
    override fun onActivityResumed(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        Log.d(TAG, "📱 [配置监控] 当前Activity: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
            Log.d(TAG, "📱 [配置监控] 清除Activity引用: ${activity.javaClass.simpleName}")
        }
    }
}