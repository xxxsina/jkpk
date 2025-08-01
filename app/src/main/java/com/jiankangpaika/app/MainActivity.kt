package com.jiankangpaika.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import com.jiankangpaika.app.ui.navigation.ShenHuoBaoApp
import com.jiankangpaika.app.utils.VersionUpdateManager
import com.jiankangpaika.app.utils.VersionMonitorManager
import com.jiankangpaika.app.ad.ConfigMonitorManager
import com.jiankangpaika.app.utils.TokenValidationFailureReceiver

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var versionUpdateManager: VersionUpdateManager
    private lateinit var versionMonitorManager: VersionMonitorManager
    private lateinit var configMonitorManager: ConfigMonitorManager
    private lateinit var tokenValidationFailureReceiver: TokenValidationFailureReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置状态栏颜色为红色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.parseColor("#EA3323")
        }

        Log.d(TAG, "MainActivity启动，SDK已在Application中初始化")

        // 初始化版本更新管理器
        versionUpdateManager = VersionUpdateManager(this)
        
        // 初始化版本监控管理器
        versionMonitorManager = VersionMonitorManager.getInstance(this)
        
        // 初始化配置监控管理器
        configMonitorManager = ConfigMonitorManager.getInstance(this)

        // 应用启动时进行一次版本检查
        performStartupVersionCheck()
        
        // 应用启动时进行一次配置检查
        performStartupConfigCheck()
        
        // 启动实时版本监控
        startVersionMonitoring()
        
        // 启动实时配置监控
        startConfigMonitoring()
        
        // 注册token验证失败广播接收器
        registerTokenValidationFailureReceiver()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ShenHuoBaoApp()
                }
            }
        }
    }

    /**
     * 应用启动时的版本检查
     */
    private fun performStartupVersionCheck() {
        Log.d(TAG, "🚀 [启动检查] 应用启动时进行版本检查")
        versionMonitorManager.performStartupCheck()
    }
    
    /**
     * 启动版本监控
     */
    private fun startVersionMonitoring() {
        Log.d(TAG, "📡 [版本监控] 启动实时版本监控")
        versionMonitorManager.startMonitoring()
    }
    
    /**
     * 应用启动时的配置检查
     */
    private fun performStartupConfigCheck() {
        Log.d(TAG, "🚀 [启动检查] 应用启动时进行配置检查")
        configMonitorManager.performStartupCheck()
    }
    
    /**
     * 启动配置监控
     */
    private fun startConfigMonitoring() {
        Log.d(TAG, "📡 [配置监控] 启动实时配置监控")
        configMonitorManager.startMonitoring()
    }
    
    /**
     * 检查版本更新（保留原有方法，用于兼容）
     */
    private fun checkVersionUpdate() {
        Log.d(TAG, "开始检查版本更新")
        versionUpdateManager.checkForUpdate(
            showNoUpdateDialog = false,
            bypassTimeCheck = false  // 启动检查遵循24小时时间限制
        ) { hasUpdate, versionInfo ->
            if (hasUpdate && versionInfo != null) {
                Log.i(TAG, "发现新版本: ${versionInfo.versionName}")
            } else {
                Log.d(TAG, "当前已是最新版本")
            }
        }
    }

    /**
     * 手动检查版本更新（显示无更新对话框）
     */
    fun checkVersionUpdateManually() {
        Log.d(TAG, "🔍 [手动检查] 手动检查版本更新")
        versionUpdateManager.checkForUpdate(
            showNoUpdateDialog = true,
            bypassTimeCheck = false  // 手动检查遵循24小时时间限制
        )
    }
    
    override fun onResume() {
        super.onResume()
        // Activity恢复时，恢复版本监控和配置监控
        versionMonitorManager.resumeMonitoring()
        configMonitorManager.resumeMonitoring()
    }
    
    override fun onPause() {
        super.onPause()
        // Activity暂停时，暂停版本监控和配置监控
        versionMonitorManager.pauseMonitoring()
        configMonitorManager.pauseMonitoring()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Activity销毁时，停止版本监控和配置监控
        versionMonitorManager.stopMonitoring()
        configMonitorManager.stopMonitoring()
        
        // 取消注册token验证失败广播接收器
        unregisterTokenValidationFailureReceiver()
    }
    
    /**
     * 注册token验证失败广播接收器
     */
    private fun registerTokenValidationFailureReceiver() {
        tokenValidationFailureReceiver = TokenValidationFailureReceiver {
            Log.w(TAG, "🔐 [Token验证] 收到token验证失败广播，重新启动应用到登录页面")
            // 重新启动MainActivity，这会触发导航到登录页面的逻辑
            recreate()
        }
        TokenValidationFailureReceiver.registerReceiver(this, tokenValidationFailureReceiver)
    }
    
    /**
     * 取消注册token验证失败广播接收器
     */
    private fun unregisterTokenValidationFailureReceiver() {
        if (::tokenValidationFailureReceiver.isInitialized) {
            TokenValidationFailureReceiver.unregisterReceiver(this, tokenValidationFailureReceiver)
        }
    }
}
