package com.jiankangpaika.app.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Token验证失败广播接收器
 * 用于处理全局的token验证失败事件
 */
class TokenValidationFailureReceiver(private val onTokenValidationFailure: () -> Unit) : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "TokenValidationFailureReceiver"
        const val ACTION_TOKEN_VALIDATION_FAILURE = "com.jiankangpaika.app.TOKEN_VALIDATION_FAILURE"
        
        /**
         * 发送token验证失败广播
         * @param context 上下文
         */
        fun sendTokenValidationFailureBroadcast(context: Context) {
            Log.d(TAG, "📡 [广播发送] 发送token验证失败广播")
            val intent = Intent(ACTION_TOKEN_VALIDATION_FAILURE)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        }
        
        /**
         * 注册token验证失败广播接收器
         * @param context 上下文
         * @param receiver 接收器实例
         */
        fun registerReceiver(context: Context, receiver: TokenValidationFailureReceiver) {
            Log.d(TAG, "📝 [广播注册] 注册token验证失败广播接收器")
            val filter = IntentFilter(ACTION_TOKEN_VALIDATION_FAILURE)
            LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter)
        }
        
        /**
         * 取消注册token验证失败广播接收器
         * @param context 上下文
         * @param receiver 接收器实例
         */
        fun unregisterReceiver(context: Context, receiver: TokenValidationFailureReceiver) {
            Log.d(TAG, "📝 [广播取消] 取消注册token验证失败广播接收器")
            try {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
            } catch (e: Exception) {
                Log.w(TAG, "取消注册广播接收器时出错: ${e.message}")
            }
        }
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == ACTION_TOKEN_VALIDATION_FAILURE) {
            Log.w(TAG, "🚨 [广播接收] 收到token验证失败广播，执行处理逻辑")
            onTokenValidationFailure()
        }
    }
}