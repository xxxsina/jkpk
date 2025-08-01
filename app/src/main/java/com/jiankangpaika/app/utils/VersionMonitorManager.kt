package com.jiankangpaika.app.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.jiankangpaika.app.data.model.VersionInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * 版本监控管理器
 * 负责实时监控版本更新，支持配置监控间隔和暂停/恢复机制
 */
class VersionMonitorManager private constructor(private val context: Context) : Application.ActivityLifecycleCallbacks {
    
    companion object {
        private const val TAG = "VersionMonitorManager"
        private const val DEFAULT_MONITOR_INTERVAL = 5 * 60 * 1000L // 默认5分钟
        private const val PREFS_NAME = "version_monitor_prefs"
        private const val KEY_MONITOR_INTERVAL = "monitor_interval"
        private const val KEY_MONITOR_ENABLED = "monitor_enabled"
        
        @Volatile
        private var INSTANCE: VersionMonitorManager? = null
        
        fun getInstance(context: Context): VersionMonitorManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VersionMonitorManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // 移除固定的versionUpdateManager，改为动态创建
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 当前Activity的弱引用
    private var currentActivityRef: WeakReference<Activity>? = null
    
    init {
         // 注册Activity生命周期回调
         try {
             val application = context.applicationContext as? Application
             application?.registerActivityLifecycleCallbacks(this)
             Log.d(TAG, "📱 [版本监控] 已注册Activity生命周期回调")
         } catch (e: Exception) {
             Log.w(TAG, "⚠️ [版本监控] 注册Activity生命周期回调失败: ${e.message}")
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
     * 启动版本监控
     */
    fun startMonitoring() {
        if (!isMonitorEnabled) {
            Log.d(TAG, "📴 [版本监控] 监控已被禁用，跳过启动")
            return
        }
        
        if (isMonitoring) {
            Log.d(TAG, "⚠️ [版本监控] 监控已在运行中")
            return
        }
        
        Log.i(TAG, "🚀 [版本监控] 启动版本监控，间隔: ${monitorInterval / 1000}秒")
        isMonitoring = true
        isPaused = false
        
        monitorJob = CoroutineScope(Dispatchers.Main).launch {
            while (isMonitoring) {
                try {
                    if (!isPaused) {
                        Log.d(TAG, "🔍 [版本监控] 执行定时版本检查")
                        performVersionCheck()
                    } else {
                        Log.d(TAG, "⏸️ [版本监控] 监控已暂停，跳过检查")
                    }
                    
                    // 等待下一次检查
                    delay(monitorInterval)
                } catch (e: Exception) {
                    Log.e(TAG, "💥 [版本监控] 监控过程中发生异常: ${e.message}", e)
                    // 发生异常时等待一段时间再继续
                    delay(30000) // 30秒后重试
                }
            }
        }
    }
    
    /**
     * 停止版本监控
     */
    fun stopMonitoring() {
        Log.i(TAG, "🛑 [版本监控] 停止版本监控")
        isMonitoring = false
        isPaused = false
        monitorJob?.cancel()
        monitorJob = null
        
        // 取消注册Activity生命周期回调
        try {
            val application = context.applicationContext as? Application
            application?.unregisterActivityLifecycleCallbacks(this)
            Log.d(TAG, "📱 [版本监控] 已取消注册Activity生命周期回调")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [版本监控] 取消注册Activity生命周期回调失败: ${e.message}")
        }
    }
    
    /**
     * 暂停版本监控
     * 当出现提示层时调用
     */
    fun pauseMonitoring() {
        if (isMonitoring) {
            Log.d(TAG, "⏸️ [版本监控] 暂停版本监控（提示层显示）")
            isPaused = true
        }
    }
    
    /**
     * 恢复版本监控
     * 当提示层关闭时调用
     */
    fun resumeMonitoring() {
        if (isMonitoring && isPaused) {
            Log.i(TAG, "▶️ [版本监控] 恢复版本监控（提示层关闭）")
            isPaused = false
        }
    }
    
    /**
     * 执行一次版本检查（应用启动时调用）
     */
    fun performStartupCheck() {
        Log.i(TAG, "🔍 [版本监控] 执行应用启动版本检查")
        performVersionCheck()
    }
    
    /**
     * 获取当前Activity
     */
    private fun getCurrentActivity(): Activity? {
        return currentActivityRef?.get()
    }
    
    /**
     * 执行版本检查
     */
    private fun performVersionCheck() {
        // 获取当前Activity Context
        val activityContext = getCurrentActivity()
        if (activityContext == null) {
            Log.w(TAG, "⚠️ [版本监控] 无法获取当前Activity，跳过版本检查")
            return
        }
        
        // 使用Activity Context创建VersionUpdateManager
        val versionUpdateManager = VersionUpdateManager(activityContext)
        versionUpdateManager.checkForUpdate(
            showNoUpdateDialog = false,
            bypassTimeCheck = true  // 监控调用绕过24小时时间限制
        ) { hasUpdate, versionInfo ->
            if (hasUpdate && versionInfo != null) {
                Log.i(TAG, "🆕 [版本监控] 监控发现新版本: ${versionInfo.versionName}")
                // 暂停监控，等待用户处理更新提示
                pauseMonitoring()
            } else {
                Log.d(TAG, "✅ [版本监控] 当前已是最新版本")
            }
        }
    }
    
    /**
     * 设置监控间隔
     * @param intervalMs 监控间隔（毫秒）
     */
    fun updateMonitorInterval(intervalMs: Long) {
        if (intervalMs < 60000) { // 最小1分钟
            Log.w(TAG, "⚠️ [版本监控] 监控间隔不能小于1分钟，已设置为1分钟")
            monitorInterval = 60000
        } else {
            monitorInterval = intervalMs
            Log.i(TAG, "⚙️ [版本监控] 设置监控间隔: ${intervalMs / 1000}秒")
        }
        
        // 如果正在监控，重启监控以应用新间隔
        if (isMonitoring) {
            Log.d(TAG, "🔄 [版本监控] 重启监控以应用新间隔")
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
        Log.i(TAG, "⚙️ [版本监控] 设置监控状态: ${if (enabled) "启用" else "禁用"}")
        
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
        Log.d(TAG, "📱 [版本监控] 当前Activity: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivityRef?.get() == activity) {
            currentActivityRef = null
            Log.d(TAG, "📱 [版本监控] 清除Activity引用: ${activity.javaClass.simpleName}")
        }
    }
}