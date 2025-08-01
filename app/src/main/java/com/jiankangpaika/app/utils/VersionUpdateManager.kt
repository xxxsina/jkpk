package com.jiankangpaika.app.utils

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.jiankangpaika.app.data.model.VersionCheckResponse
import com.jiankangpaika.app.data.model.VersionInfo
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.utils.constants.ApiConfig.Version.DOWNLOAD_BASE
import com.jiankangpaika.app.utils.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 版本更新管理器
 * 负责版本检查、下载和安装功能
 */
class VersionUpdateManager(private val context: Context) {
    companion object {
        private const val TAG = "VersionUpdateManager"
        private const val DOWNLOAD_DIR = "downloads"
        private const val APK_FILE_NAME = "shenhuobao_update.apk"
        
        // SharedPreferences键名
        private const val PREFS_NAME = "version_update_prefs"
        private const val KEY_LAST_CHECK_TIME = "last_check_time"
        private const val KEY_IGNORED_VERSION = "ignored_version"
        private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 24小时检查一次
    }
    
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * 检查版本更新
     * @param showNoUpdateDialog 是否在没有更新时显示提示
     * @param bypassTimeCheck 是否绕过时间间隔检查（用于监控调用）
     * @param callback 检查结果回调
     */
    fun checkForUpdate(
        showNoUpdateDialog: Boolean = false,
        bypassTimeCheck: Boolean = false,
        callback: ((Boolean, VersionInfo?) -> Unit)? = null
    ) {
        // 检查是否有待安装的APK（用户开启权限后重新检查更新）
        checkPendingInstallApk()
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "🔍 [版本检查] 开始检查版本更新")
                
                // 检查版本监控是否处于暂停状态（有更新弹窗显示时）
                // 无论是否绕过时间检查，都需要检查监控状态
                if (!showNoUpdateDialog) {
                    try {
                        val monitorManager = VersionMonitorManager.getInstance(context)
                        val monitorStatus = monitorManager.getMonitorStatus()
                        if (monitorStatus.isPaused) {
                            Log.d(TAG, "⏸️ [版本检查] 版本监控已暂停（有更新弹窗显示），跳过检查")
                            callback?.invoke(false, null)
                            return@launch
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ [版本检查] 获取监控状态失败: ${e.message}")
                    }
                }
                
                // 检查是否需要进行版本检查（监控调用可以绕过时间限制）
                if (!showNoUpdateDialog && !bypassTimeCheck && !shouldCheckUpdate()) {
                    Log.d(TAG, "⏰ [版本检查] 距离上次检查时间未超过间隔，跳过检查")
                    callback?.invoke(false, null)
                    return@launch
                }
                
                if (bypassTimeCheck) {
                    Log.d(TAG, "🔄 [版本检查] 监控调用，绕过24小时时间限制")
                }
                
                val result = withContext(Dispatchers.IO) {
                    // 构建POST请求参数
                    val requestData = mutableMapOf<String, Any>(
                        "versionName" to getCurrentVersionName(),
                        "versionCode" to getCurrentVersionCode(),
                        "action" to "check"
                    )
                    
                    // 添加用户ID（如果用户已登录）
                    val userId = UserManager.getUserId(context)
                    if (!userId.isNullOrEmpty()) {
                        requestData["user_id"] = userId.toIntOrNull() ?: userId
                    }
                    
                    Log.d(TAG, "🔍 [版本检查] POST请求参数: $requestData")
                    NetworkUtils.postJson(ApiConfig.Version.CHECK_UPDATE, requestData, context)
                }
                
                when (result) {
                    is NetworkResult.Success -> {
                        val response = NetworkUtils.parseJson<VersionCheckResponse>(result.data)
                        if (response != null && response.code == 200) {
                            if (response.data != null) {
                                handleVersionCheckSuccess(response.data, showNoUpdateDialog, callback)
                            } else {
                                // 当前已是最新版本（data为null）
                                Log.d(TAG, "✅ [版本检查] ${response.message}")
                                if (showNoUpdateDialog) {
                                    showNoUpdateDialog()
                                }
                                callback?.invoke(false, null)
                            }
                        } else {
                            Log.e(TAG, "❌ [版本检查] 服务器响应格式错误或无数据")
                            handleVersionCheckError("服务器响应格式错误", showNoUpdateDialog, callback)
                        }
                    }
                    is NetworkResult.Error -> {
                        val errorMessage = result.parseApiErrorMessage()
                        Log.e(TAG, "❌ [版本检查] 网络请求失败: ${result.code} - $errorMessage")
                        handleVersionCheckError(errorMessage, showNoUpdateDialog, callback)
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "💥 [版本检查] 请求异常: ${result.exception.message}", result.exception)
                        handleVersionCheckError("网络连接异常", showNoUpdateDialog, callback)
                    }
                }
                
                // 更新最后检查时间
                updateLastCheckTime()
                
            } catch (e: Exception) {
                Log.e(TAG, "💥 [版本检查] 检查过程异常: ${e.message}", e)
                handleVersionCheckError("版本检查异常: ${e.message}", showNoUpdateDialog, callback)
            }
        }
    }
    
    /**
     * 处理版本检查成功
     */
    private fun handleVersionCheckSuccess(
        versionInfo: VersionInfo,
        showNoUpdateDialog: Boolean,
        callback: ((Boolean, VersionInfo?) -> Unit)?
    ) {
        val currentVersionCode = getCurrentVersionCode()
        Log.d(TAG, "📊 [版本检查] 当前版本: $currentVersionCode, 服务器版本: ${versionInfo.versionCode}")
        
        if (versionInfo.versionCode > currentVersionCode) {
            // 检查是否已忽略此版本
//            val ignoredVersion = prefs.getInt(KEY_IGNORED_VERSION, 0)
//            if (!versionInfo.forceUpdate && ignoredVersion == versionInfo.versionCode) {
//                Log.d(TAG, "🙈 [版本检查] 用户已忽略版本 ${versionInfo.versionCode}")
//                callback?.invoke(false, null)
//                return
//            }
            
            Log.i(TAG, "🆕 [版本检查] 发现新版本: ${versionInfo.versionName}")
            showUpdateDialog(versionInfo)
            callback?.invoke(true, versionInfo)
        } else {
            Log.d(TAG, "✅ [版本检查] 当前已是最新版本")
            if (showNoUpdateDialog) {
                showNoUpdateDialog()
            }
            callback?.invoke(false, null)
        }
    }
    
    /**
     * 处理版本检查错误
     */
    private fun handleVersionCheckError(
        errorMessage: String,
        showNoUpdateDialog: Boolean,
        callback: ((Boolean, VersionInfo?) -> Unit)?
    ) {
        if (showNoUpdateDialog) {
            ToastUtils.showErrorToast(context, "检查更新失败: $errorMessage")
        }
        callback?.invoke(false, null)
    }
    
    /**
     * 显示更新对话框
     */
    private fun showUpdateDialog(versionInfo: VersionInfo) {
        if (context !is Activity) {
            Log.w(TAG, "⚠️ [更新对话框] Context不是Activity，无法显示对话框")
            return
        }
        
        // 暂停版本监控
        try {
            Log.d(TAG, "⏸️ [更新对话框] 已暂停版本监控")
            VersionMonitorManager.getInstance(context).pauseMonitoring()
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [更新对话框] 暂停版本监控失败: ${e.message}")
        }
        
        val dialog = AlertDialog.Builder(context)
            .setTitle("发现新版本 ${versionInfo.versionName}")
            .setMessage(buildUpdateMessage(versionInfo))
            .setCancelable(!versionInfo.forceUpdate)
//            .setNegativeButton("立即更新") { _, _ ->
//                resumeVersionMonitoring()
//                downloadAndInstall(versionInfo)
//            }
            .setPositiveButton("立即更新") { _, _ ->
                Log.d(TAG, "👤 [用户操作] 用户选择浏览器下载")
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(DOWNLOAD_BASE))
                    context.startActivity(intent)
                    Log.i(TAG, "🌐 [浏览器下载] 成功打开浏览器下载页面")
                    ToastUtils.showInfoToast(context, "已在浏览器中打开下载页面")
                } catch (e: Exception) {
                    Log.e(TAG, "💥 [浏览器下载] 打开浏览器失败: ${e.message}", e)
                    ToastUtils.showErrorToast(context, "无法打开浏览器，请手动复制下载链接")
                }
                resumeVersionMonitoring()
            }
        
        if (!versionInfo.forceUpdate) {
            dialog.setNeutralButton("忽略此版本") { _, _ ->
                ignoreVersion(versionInfo.versionCode)
                Log.d(TAG, "👤 [用户操作] 用户选择忽略版本 ${versionInfo.versionCode}")
                resumeVersionMonitoring()
            }
        } else {
            // 强制更新时提供关闭按钮
//            dialog.setNeutralButton("关闭") { _, _ ->
//                Log.d(TAG, "👤 [用户操作] 用户在强制更新时选择关闭")
//                ToastUtils.showInfoToast(context, "这是重要更新，建议您及时更新")
//                resumeVersionMonitoring()
//            }
        }
        
        dialog.show()
    }
    
    /**
     * 构建更新消息
     */
    private fun buildUpdateMessage(versionInfo: VersionInfo): String {
        return buildString {
            append(versionInfo.updateMessage)
            if (versionInfo.fileSize.isNotEmpty()) {
                append("\n\n文件大小: ${versionInfo.fileSize}")
            }
            if (versionInfo.updateTime.isNotEmpty()) {
                append("\n更新时间: ${versionInfo.updateTime}")
            }
            if (versionInfo.forceUpdate) {
                append("\n\n⚠️ 此版本为必要更新")
            }
        }
    }
    
    /**
     * 下载并安装APK
     */
    private fun downloadAndInstall(versionInfo: VersionInfo) {
        Log.d(TAG, "🚀 [下载安装] 开始下载安装流程")
        Log.d(TAG, "📋 [下载安装] 版本信息: ${versionInfo.versionName} (${versionInfo.versionCode})")
        Log.d(TAG, "🔗 [下载安装] 下载地址: ${versionInfo.downloadUrl}")
        Log.d(TAG, "📱 [下载安装] Context类型: ${context.javaClass.simpleName}")
        Log.d(TAG, "🔧 [下载安装] Android版本: ${Build.VERSION.SDK_INT}")
        
        // 暂停版本监控
        try {
            VersionMonitorManager.getInstance(context).pauseMonitoring()
            Log.d(TAG, "⏸️ [下载安装] 已暂停版本监控")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [下载安装] 暂停版本监控失败: ${e.message}")
        }
        
        // 创建进度对话框
        val progressDialog = ProgressDialog(context).apply {
            setTitle("下载更新")
            setMessage("正在下载更新包，请稍候...")
            setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            progress = 0
            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "📥 [APK下载] 开始下载APK: ${versionInfo.downloadUrl}")
                progressDialog.show()
                
                val apkFile = withContext(Dispatchers.IO) {
                    downloadApk(versionInfo.downloadUrl) { progress ->
                        // 在主线程更新进度
                        CoroutineScope(Dispatchers.Main).launch {
                            progressDialog.progress = progress
                            progressDialog.setMessage("正在下载更新包... ${progress}%")
                        }
                    }
                }
                
                progressDialog.dismiss()
                
                if (apkFile != null && apkFile.exists()) {
                    Log.i(TAG, "✅ [APK下载] 下载完成: ${apkFile.absolutePath}")
                    Log.d(TAG, "📊 [APK下载] 文件大小: ${apkFile.length()} bytes")
                    Log.d(TAG, "🔍 [APK下载] 文件是否可读: ${apkFile.canRead()}")
                    Log.d(TAG, "📁 [APK下载] 文件权限: ${apkFile.canRead()}, ${apkFile.canWrite()}, ${apkFile.canExecute()}")
                    Log.d(TAG, "🏷️ [APK下载] 包名: ${context.packageName}")
                    ToastUtils.showSuccessToast(context, "下载完成，准备安装...")
                    
                    // 验证文件完整性
                    if (!apkFile.exists()) {
                        Log.e(TAG, "❌ [APK下载] 文件不存在，无法安装")
                        ToastUtils.showErrorToast(context, "下载的APK文件不存在")
                        return@launch
                    }
                    
                    if (apkFile.length() == 0L) {
                        Log.e(TAG, "❌ [APK下载] 文件大小为0，可能下载失败")
                        ToastUtils.showErrorToast(context, "下载的APK文件损坏")
                        return@launch
                    }
                    
                    // 立即调用安装方法
                    Log.d(TAG, "🎯 [APK下载] 即将调用installApk方法")
                    
                    // 检查Android版本和权限状态，给用户明确提示
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val canInstall = context.packageManager.canRequestPackageInstalls()
                        if (!canInstall) {
                            ToastUtils.showInfoToast(context, "下载完成，需要开启安装权限")
                        } else {
                            ToastUtils.showInfoToast(context, "下载完成，正在启动安装程序...")
                        }
                    } else {
                        ToastUtils.showInfoToast(context, "下载完成，正在启动安装程序...")
                    }
                    
                    // 调用安装方法（版本监控将在安装完成或失败后恢复）
                    installApk(apkFile)
                } else {
                    Log.e(TAG, "❌ [APK下载] 下载失败 - apkFile: $apkFile")
                    if (apkFile != null) {
                        Log.e(TAG, "❌ [APK下载] 文件存在性检查: ${apkFile.exists()}")
                    }
                    ToastUtils.showErrorToast(context, "下载失败，请检查网络连接")
                    
                    // 恢复版本监控
                    resumeVersionMonitoring()
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(TAG, "💥 [APK下载] 下载异常: ${e.message}", e)
                ToastUtils.showErrorToast(context, "下载异常: ${e.message}")
                
                // 恢复版本监控
                resumeVersionMonitoring()
            }
        }
    }
    
    /**
     * 下载APK文件
     */
    private suspend fun downloadApk(downloadUrl: String, onProgress: (Int) -> Unit = {}): File? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(downloadUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.connect()
                
                val fileLength = connection.contentLength
                Log.d(TAG, "📊 [APK下载] 文件总大小: $fileLength bytes")
                
                val downloadDir = File(context.getExternalFilesDir(null), DOWNLOAD_DIR)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                
                val apkFile = File(downloadDir, APK_FILE_NAME)
                if (apkFile.exists()) {
                    apkFile.delete()
                }
                
                connection.inputStream.use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8192)
                        var totalBytesRead = 0L
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 计算并更新进度
                            if (fileLength > 0) {
                                val progress = ((totalBytesRead * 100) / fileLength).toInt()
                                onProgress(progress)
//                                Log.v(TAG, "📈 [APK下载] 下载进度: $progress% ($totalBytesRead/$fileLength)")
                            }
                        }
                    }
                }
                
                Log.d(TAG, "✅ [APK下载] 文件下载完成: ${apkFile.absolutePath}")
                apkFile
            } catch (e: Exception) {
                Log.e(TAG, "💥 [APK下载] 下载文件异常: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 安装APK
     */
    private fun installApk(apkFile: File) {
        Log.d(TAG, "🎯 [APK安装] 进入installApk方法")
        Log.d(TAG, "📁 [APK安装] APK文件路径: ${apkFile.absolutePath}")
        Log.d(TAG, "✅ [APK安装] 文件存在: ${apkFile.exists()}")
        Log.d(TAG, "📊 [APK安装] 文件大小: ${apkFile.length()} bytes")
        Log.d(TAG, "🔧 [APK安装] Android版本: ${Build.VERSION.SDK_INT}")
        Log.d(TAG, "📱 [APK安装] Context类型: ${context.javaClass.simpleName}")
        
        try {
            // Android 8.0+ 需要检查安装权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "🔍 [APK安装] Android 8.0+，检查安装权限")
                val canInstall = context.packageManager.canRequestPackageInstalls()
                Log.d(TAG, "🔐 [APK安装] 安装权限状态: $canInstall")
                
                if (!canInstall) {
                    Log.w(TAG, "⚠️ [APK安装] 缺少安装权限，引导用户开启")
                    showInstallPermissionDialog(apkFile)
                    return
                }
            } else {
                Log.d(TAG, "✅ [APK安装] Android 8.0以下，无需检查安装权限")
            }
            
            Log.d(TAG, "🔨 [APK安装] 开始构建安装Intent")
            val intent = Intent(Intent.ACTION_VIEW)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG, "📦 [APK安装] Android 7.0+，使用FileProvider")
                // Android 7.0及以上使用FileProvider
                val authority = "${context.packageName}.fileprovider"
                Log.d(TAG, "🏷️ [APK安装] FileProvider authority: $authority")
                
                val uri = FileProvider.getUriForFile(
                    context,
                    authority,
                    apkFile
                )
                Log.d(TAG, "🔗 [APK安装] FileProvider URI: $uri")
                
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                Log.d(TAG, "✅ [APK安装] Intent配置完成（FileProvider模式）")
            } else {
                Log.d(TAG, "📁 [APK安装] Android 7.0以下，使用文件URI")
                val fileUri = Uri.fromFile(apkFile)
                Log.d(TAG, "🔗 [APK安装] 文件URI: $fileUri")
                intent.setDataAndType(fileUri, "application/vnd.android.package-archive")
                Log.d(TAG, "✅ [APK安装] Intent配置完成（文件URI模式）")
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.d(TAG, "🚀 [APK安装] 准备启动安装Intent")
            
            context.startActivity(intent)
            
            Log.i(TAG, "📱 [APK安装] 成功启动安装界面")
            ToastUtils.showSuccessToast(context, "安装程序已启动，请按照提示完成安装")
            
            // 安装程序启动成功，恢复版本监控
            resumeVersionMonitoring()
            
            // 显示签名冲突处理提示
            showSignatureConflictTips()
        } catch (e: Exception) {
            Log.e(TAG, "💥 [APK安装] 安装异常: ${e.message}", e)
            Log.e(TAG, "💥 [APK安装] 异常堆栈: ", e)
            
            // 检查是否是签名冲突错误
            val errorMessage = e.message?.lowercase() ?: ""
            if (errorMessage.contains("signature") || errorMessage.contains("签名")) {
                showSignatureConflictDialog()
            } else {
                ToastUtils.showErrorToast(context, "安装失败: ${e.message}")
                // 恢复版本监控
                resumeVersionMonitoring()
            }
        }
    }
    
    /**
     * 显示签名冲突处理提示
     */
    private fun showSignatureConflictTips() {
        // 延迟3秒显示提示，给用户时间看到安装界面
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(3000)
            ToastUtils.showInfoToast(context, "如果安装失败提示签名不同，请先卸载旧版本再安装")
        }
    }
    
    /**
     * 显示签名冲突对话框
     */
    private fun showSignatureConflictDialog() {
        Log.d(TAG, "⚠️ [签名冲突] 显示签名冲突处理对话框")
        
        if (context !is Activity) {
            Log.e(TAG, "💥 [签名冲突] Context不是Activity，无法显示对话框")
            ToastUtils.showErrorToast(context, "安装失败：应用签名不同，请先卸载旧版本")
            return
        }
        
        // 暂停版本监控
        try {
            VersionMonitorManager.getInstance(context).pauseMonitoring()
            Log.d(TAG, "⏸️ [签名冲突] 已暂停版本监控")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [签名冲突] 暂停版本监控失败: ${e.message}")
        }
        
        // 检测是否为小米/红米设备
        val isMiui = isMiuiDevice()
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}"
        Log.d(TAG, "📱 [设备检测] 设备信息: $deviceInfo, MIUI: $isMiui")
        
        val message = if (isMiui) {
            "检测到您使用的是红米/小米设备，MIUI系统对应用安装有特殊限制。\n\n解决方案：\n1. 先卸载当前版本的应用\n2. 在MIUI设置中关闭\"纯净模式\"\n3. 开启\"允许安装未知来源应用\"\n4. 重新安装新版本\n\n注意：卸载后应用数据可能会丢失，建议先备份重要数据。"
        } else {
            "检测到应用签名不同，这通常是因为当前安装的版本与新版本使用了不同的签名证书。\n\n解决方案：\n1. 先卸载当前版本的应用\n2. 重新安装新版本\n\n注意：卸载后应用数据可能会丢失，建议先备份重要数据。"
        }
        
        AlertDialog.Builder(context)
            .setTitle("安装失败")
            .setMessage(message)
            .setPositiveButton("去卸载") { _, _ ->
                Log.d(TAG, "👆 [签名冲突] 用户选择去卸载")
                try {
                    val intent = Intent(Intent.ACTION_DELETE)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                    Log.i(TAG, "🗑️ [签名冲突] 引导用户卸载应用")
                } catch (e: Exception) {
                    Log.e(TAG, "💥 [签名冲突] 打开卸载页面失败: ${e.message}", e)
                    ToastUtils.showErrorToast(context, "无法打开卸载页面，请手动卸载应用")
                }
                resumeVersionMonitoring()
            }
            .setNeutralButton(if (isMiui) "MIUI设置" else "系统设置") { _, _ ->
                Log.d(TAG, "⚙️ [设置] 用户选择打开设置")
                openDeviceSettings(isMiui)
                resumeVersionMonitoring()
            }
            .setNegativeButton("取消") { _, _ ->
                Log.d(TAG, "❌ [签名冲突] 用户取消卸载")
                resumeVersionMonitoring()
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 检测是否为MIUI设备
     */
    private fun isMiuiDevice(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()
        
        return manufacturer.contains("xiaomi") || 
               brand.contains("xiaomi") || 
               brand.contains("redmi") ||
               model.contains("redmi") ||
               !android.text.TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"))
    }
    
    /**
     * 获取系统属性
     */
    private fun getSystemProperty(key: String): String {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java)
            method.invoke(null, key) as String
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 打开设备特定的设置页面
     */
    private fun openDeviceSettings(isMiui: Boolean) {
        try {
            if (isMiui) {
                // 尝试打开MIUI的安全中心或应用管理
                val miuiIntents = listOf(
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.install.AdbInstallActivity"
                        )
                    },
                    Intent().apply {
                        component = android.content.ComponentName(
                            "com.android.settings",
                            "com.android.settings.applications.ManageApplications"
                        )
                    },
                    Intent(Settings.ACTION_APPLICATION_SETTINGS)
                )
                
                for (intent in miuiIntents) {
                    try {
                        context.startActivity(intent)
                        Log.i(TAG, "✅ [MIUI设置] 成功打开MIUI设置页面")
                        ToastUtils.showInfoToast(context, "请在设置中关闭纯净模式，开启未知来源安装")
                        return
                    } catch (e: Exception) {
                        Log.d(TAG, "⚠️ [MIUI设置] 尝试打开设置失败: ${e.message}")
                        continue
                    }
                }
            } else {
                // 通用Android设置
                val intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
                context.startActivity(intent)
                Log.i(TAG, "✅ [系统设置] 成功打开应用设置页面")
                ToastUtils.showInfoToast(context, "请在设置中开启未知来源安装权限")
                return
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [设置] 打开设置页面失败: ${e.message}", e)
        }
        
        // 兜底方案
        ToastUtils.showInfoToast(context, "请手动进入设置 > 应用管理 > 权限管理，开启安装权限")
    }
    
    /**
     * 显示安装权限对话框
     */
    private fun showInstallPermissionDialog(apkFile: File) {
        Log.d(TAG, "🔐 [权限对话框] 显示安装权限对话框")
        Log.d(TAG, "📁 [权限对话框] APK文件: ${apkFile.absolutePath}")
        
        if (context !is Activity) {
            Log.e(TAG, "💥 [权限对话框] Context不是Activity，无法显示对话框")
            ToastUtils.showErrorToast(context, "需要开启安装权限才能安装更新")
            return
        }
        
        // 暂停版本监控
        try {
            VersionMonitorManager.getInstance(context).pauseMonitoring()
            Log.d(TAG, "⏸️ [权限对话框] 已暂停版本监控")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [权限对话框] 暂停版本监控失败: ${e.message}")
        }
        
        AlertDialog.Builder(context)
            .setTitle("需要安装权限")
            .setMessage("为了安装更新，需要开启\"安装未知应用\"权限。\n\n点击\"去设置\"后，请找到\"安装未知应用\"选项并开启。")
            .setPositiveButton("去设置") { _, _ ->
                Log.d(TAG, "👆 [权限对话框] 用户点击去设置")
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                    intent.data = Uri.parse("package:${context.packageName}")
                    context.startActivity(intent)
                    
                    // 保存APK文件路径，用户开启权限后可以重新尝试安装
                    prefs.edit().putString("pending_install_apk", apkFile.absolutePath).apply()
                    Log.d(TAG, "💾 [权限对话框] 已保存待安装APK路径: ${apkFile.absolutePath}")
                    
                    ToastUtils.showInfoToast(context, "开启权限后，返回应用将自动安装更新")
                    Log.i(TAG, "🔧 [权限设置] 引导用户开启安装权限")
                    Log.d(TAG, "✅ [权限对话框] 成功跳转到设置页面")
                } catch (e: Exception) {
                    Log.e(TAG, "💥 [权限设置] 打开设置页面失败: ${e.message}", e)
                    ToastUtils.showErrorToast(context, "无法打开设置页面，请手动开启安装权限")
                }
                resumeVersionMonitoring()
            }
            .setNegativeButton("取消") { _, _ ->
                Log.d(TAG, "❌ [权限对话框] 用户取消开启安装权限")
                ToastUtils.showInfoToast(context, "取消安装，您可以稍后重新检查更新")
                resumeVersionMonitoring()
            }
            .setCancelable(false)
            .show()
        
        Log.d(TAG, "📱 [权限对话框] 对话框已显示")
    }
    
    /**
     * 显示无更新对话框
     */
    private fun showNoUpdateDialog() {
        if (context !is Activity) return
        
        // 暂停版本监控
        try {
            VersionMonitorManager.getInstance(context).pauseMonitoring()
            Log.d(TAG, "⏸️ [无更新对话框] 已暂停版本监控")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [无更新对话框] 暂停版本监控失败: ${e.message}")
        }
        
        AlertDialog.Builder(context)
            .setTitle("版本检查")
            .setMessage("当前已是最新版本")
            .setPositiveButton("确定") { _, _ ->
                resumeVersionMonitoring()
            }
            .show()
    }
    
    /**
     * 恢复版本监控
     */
    private fun resumeVersionMonitoring() {
        try {
            VersionMonitorManager.getInstance(context).resumeMonitoring()
            Log.d(TAG, "▶️ [版本监控] 已恢复版本监控")
        } catch (e: Exception) {
            Log.w(TAG, "⚠️ [版本监控] 恢复版本监控失败: ${e.message}")
        }
    }
    
    /**
     * 忽略指定版本
     */
    private fun ignoreVersion(versionCode: Int) {
        prefs.edit().putInt(KEY_IGNORED_VERSION, versionCode).apply()
    }
    
    /**
     * 是否需要检查更新
     */
    private fun shouldCheckUpdate(): Boolean {
        val lastCheckTime = prefs.getLong(KEY_LAST_CHECK_TIME, 0)
        val currentTime = System.currentTimeMillis()
        return (currentTime - lastCheckTime) >= CHECK_INTERVAL
    }
    
    /**
     * 更新最后检查时间
     */
    private fun updateLastCheckTime() {
        prefs.edit().putLong(KEY_LAST_CHECK_TIME, System.currentTimeMillis()).apply()
    }
    
    /**
     * 获取当前应用版本号
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "💥 [版本信息] 获取版本号失败: ${e.message}", e)
            1
        }
    }
    
    /**
     * 检查是否有待安装的APK
     */
    private fun checkPendingInstallApk() {
        Log.d(TAG, "🔍 [待安装检查] 开始检查待安装APK")
        
        val pendingApkPath = prefs.getString("pending_install_apk", null)
        
        Log.d(TAG, "📁 [待安装检查] 待安装APK路径: $pendingApkPath")
        
        if (pendingApkPath != null) {
            val apkFile = File(pendingApkPath)
            Log.d(TAG, "✅ [待安装检查] APK文件存在: ${apkFile.exists()}")
            
            if (apkFile.exists()) {
                Log.d(TAG, "📊 [待安装检查] APK文件大小: ${apkFile.length()} bytes")
                
                // 检查是否已有安装权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val canInstall = context.packageManager.canRequestPackageInstalls()
                    Log.d(TAG, "🔐 [待安装检查] Android 8.0+，安装权限状态: $canInstall")
                    
                    if (canInstall) {
                        Log.i(TAG, "🔄 [待安装检查] 发现待安装APK，权限已开启，开始安装")
                        installApk(apkFile)
                        // 清除待安装记录
                        prefs.edit().remove("pending_install_apk").apply()
                        Log.d(TAG, "🗑️ [待安装检查] 已清除待安装记录")
                    } else {
                        Log.d(TAG, "⏳ [待安装检查] 权限未开启，等待用户授权")
                    }
                } else {
                    // Android 8.0以下直接安装
                    Log.i(TAG, "🔄 [待安装检查] 发现待安装APK，Android 8.0以下直接安装")
                    installApk(apkFile)
                    // 清除待安装记录
                    prefs.edit().remove("pending_install_apk").apply()
                    Log.d(TAG, "🗑️ [待安装检查] 已清除待安装记录")
                }
            } else {
                // 文件不存在，清除记录
                Log.w(TAG, "❌ [待安装检查] APK文件不存在，清除记录")
                prefs.edit().remove("pending_install_apk").apply()
                Log.d(TAG, "🗑️ [待安装检查] 已清除无效记录")
            }
        } else {
            Log.d(TAG, "📭 [待安装检查] 无待安装APK")
        }
    }
    
    /**
     * 获取当前应用版本名称
     */
    fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "💥 [版本信息] 获取版本名称失败: ${e.message}", e)
            "1.0"
        }
    }
}