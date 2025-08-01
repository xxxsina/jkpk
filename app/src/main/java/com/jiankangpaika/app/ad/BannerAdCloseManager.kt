package com.jiankangpaika.app.ad

import android.util.Log

/**
 * Banner广告关闭回调管理器
 * 用于在广告关闭时通知UI组件
 */
object BannerAdCloseManager {
    private const val TAG = "BannerAdCloseManager"
    
    private var onAdClosedCallback: (() -> Unit)? = null
    
    /**
     * 设置广告关闭回调
     */
    fun setOnAdClosedCallback(callback: () -> Unit) {
        onAdClosedCallback = callback
        Log.d(TAG, "🔗 [Banner广告关闭管理器] 设置关闭回调")
    }
    
    /**
     * 清除广告关闭回调
     */
    fun clearOnAdClosedCallback() {
        onAdClosedCallback = null
        Log.d(TAG, "🧹 [Banner广告关闭管理器] 清除关闭回调")
    }
    
    /**
     * 触发广告关闭回调
     */
    fun notifyAdClosed() {
        Log.d(TAG, "🔚 [Banner广告关闭管理器] 触发广告关闭回调")
        onAdClosedCallback?.invoke()
    }
}