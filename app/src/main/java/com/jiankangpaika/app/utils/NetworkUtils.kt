package com.jiankangpaika.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.jiankangpaika.app.config.DeviceModelMapping
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * 网络请求工具类
 * 提供基础的HTTP请求功能
 */
object NetworkUtils {
    private const val TAG = "NetworkUtils"
    private const val CONNECT_TIMEOUT = 10000 // 连接超时时间（毫秒）
    private const val READ_TIMEOUT = 15000     // 读取超时时间（毫秒）

    // 缓存User-Agent，避免重复计算
    private var cachedUserAgent: String? = null
    private var lastContext: WeakReference<Context>? = null
    

    
    /**
     * 标准Gson实例
     */
    private val gson: Gson = Gson()
    
    /**
     * 获取系统属性
     */
    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val result = reader.readLine()?.trim()
            reader.close()
            process.waitFor()
            if (result.isNullOrBlank()) null else result
        } catch (e: Exception) {
            Log.w(TAG, "获取系统属性失败: $key", e)
            null
        }
    }
    
    /**
     * 获取设备的市场名称
     */
    private fun getMarketingName(): String {
        return try {
            val marketingName = getSystemProperty("ro.product.marketname")
                ?: getSystemProperty("ro.product.model.for.attestation")
                ?: getSystemProperty("ro.config.marketing_name")
            
            if (!marketingName.isNullOrBlank()) {
                marketingName
            } else {
                Build.MODEL // 回退到默认值
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取市场名称失败", e)
            Build.MODEL
        }
    }
    
    /**
     * 获取显示用的设备型号
     */
    private fun getDisplayDeviceModel(): String {
        val internalModel = Build.MODEL
        return DeviceModelMapping.getMarketingName(internalModel) ?: getMarketingName()
    }
    
    /**
     * 获取网络类型
     */
    private fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            when {
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WiFi"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "Mobile"
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            Log.w(TAG, "获取网络类型失败", e)
            "Unknown"
        }
    }
    
    /**
     * 生成动态User-Agent
     * @param context 上下文，用于获取设备信息
     * @return 包含设备信息的User-Agent字符串
     */
    private fun generateUserAgent(context: Context?): String {
        // 如果context相同且已有缓存，直接返回
        if (context != null && 
            lastContext?.get() === context && 
            !cachedUserAgent.isNullOrEmpty()) {
            return cachedUserAgent!!
        }
        
        val userAgent = if (context != null) {
            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (e: Exception) {
                Log.w(TAG, "获取包信息失败", e)
                null
            }
            
            val appVersion = packageInfo?.versionName ?: "1.0"
            val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo?.longVersionCode?.toString() ?: "1"
            } else {
                @Suppress("DEPRECATION")
                packageInfo?.versionCode?.toString() ?: "1"
            }
            
            // 使用改进的设备型号获取方法
            val deviceModel = getDisplayDeviceModel()
            val androidVersion = Build.VERSION.RELEASE
            val apiLevel = Build.VERSION.SDK_INT
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            
            // 添加网络类型信息
            val networkType = getNetworkType(context)
            
            "JianKangPaiKa/$appVersion ($appVersionCode; Android $androidVersion; API $apiLevel; $brand $deviceModel; $networkType)"
        } else {
            "JianKangPaiKa/1.0 (Android)" // fallback
        }
        
        // 更新缓存
        if (context != null) {
            cachedUserAgent = userAgent
            lastContext = WeakReference(context)
        }
        
        Log.d(TAG, "🔧 [User-Agent] 生成: $userAgent")
        return userAgent
    }
    
    /**
     * 添加认证头信息
     * @param context 上下文，用于获取token
     * @param headers 现有的headers，如果为null则创建新的
     * @return 包含token的headers
     */
    private fun addAuthHeaders(context: Context?, headers: Map<String, String>?): Map<String, String> {
        val authHeaders = headers?.toMutableMap() ?: mutableMapOf()
        
        // 如果context不为null，尝试获取token并添加到header
        context?.let {
            val token = UserManager.getToken(it)
            if (!token.isNullOrEmpty()) {
                authHeaders["token"] = token
                Log.d(TAG, "🔑 [认证头] 已添加token到请求头")
            }
        }
        
        return authHeaders
    }
    
    /**
     * 检查响应是否为token验证失败，如果是则执行全局处理
     * @param context 上下文
     * @param responseCode HTTP响应码
     * @param responseBody 响应体内容
     * @return 是否为token验证失败
     */
    private suspend fun handleTokenValidationFailure(context: Context?, responseCode: Int, responseBody: String): Boolean {
        if (context == null || responseCode != 401) {
            return false
        }
        
        try {
            val jsonObject = JSONObject(responseBody)
            val code = jsonObject.optInt("code", -1)
            val message = jsonObject.optString("message", "")
            
            if (code == 401 && message == "token验证失败") {
                Log.w(TAG, "🚨 [Token验证失败] 检测到token验证失败，执行全局处理")
                
                // 在主线程中执行UI相关操作
                withContext(Dispatchers.Main) {
                    // 清除本地用户数据
                    UserManager.clearLocalUserData(context)
                    
                    // 发送广播通知UI层进行导航
                    TokenValidationFailureReceiver.sendTokenValidationFailureBroadcast(context)
                    
                    Log.d(TAG, "🔄 [Token验证失败] 已清除用户数据并发送广播，需要重新登录")
                }
                
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析token验证失败响应时出错: ${e.message}")
        }
        
        return false
    }
    
    /**
     * 执行GET请求
     * @param url 请求地址
     * @param context 上下文，用于生成动态User-Agent（可选）
     * @param headers 请求头（可选）
     * @return 响应结果
     */
    suspend fun get(url: String, context: Context? = null, headers: Map<String, String>? = null): NetworkResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🌐 [网络请求] 开始GET请求: $url")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    setRequestProperty("User-Agent", generateUserAgent(context))
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Accept-Charset", "UTF-8")
                    
                    // 添加自定义请求头
                    headers?.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "📡 [网络请求] 响应码: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    val response = reader.use { it.readText() }
                    
                    Log.d(TAG, "✅ [网络请求] 请求成功，响应长度: ${response.length}")
                    Log.v(TAG, "📄 [网络请求] 响应内容: $response")
                    
                    NetworkResult.Success(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream, StandardCharsets.UTF_8)).use { it.readText() }
                    } else {
                        "HTTP Error $responseCode"
                    }
                    
                    Log.e(TAG, "❌ [网络请求] 请求失败: $responseCode - $errorMessage")
                    
                    // 检查是否为token验证失败
                    handleTokenValidationFailure(context, responseCode, errorMessage)
                    
                    NetworkResult.Error(responseCode, errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [网络请求] 请求异常: ${e.message}", e)
                NetworkResult.Exception(e)
            }
        }
    }
    
    /**
     * 执行POST请求
     * @param url 请求地址
     * @param data 请求数据（JSON字符串或表单数据）
     * @param context 上下文，用于生成动态User-Agent（可选）
     * @param headers 请求头（可选）
     * @param contentType 内容类型（默认为application/json）
     * @return 响应结果
     */
    suspend fun post(
        url: String, 
        data: String,
        context: Context? = null,
        headers: Map<String, String>? = null,
        contentType: String = "application/json; charset=UTF-8"
    ): NetworkResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🌐 [网络请求] 开始POST请求: $url")
                Log.v(TAG, "📤 [网络请求] 请求数据: $data")
                
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT
                    doOutput = true
                    doInput = true
                    setRequestProperty("User-Agent", generateUserAgent(context))
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Accept-Charset", "UTF-8")
                    setRequestProperty("Content-Type", contentType)
                    
                    // 添加自定义请求头
                    headers?.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }
                
                // 写入请求数据
                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream, StandardCharsets.UTF_8)
                writer.use {
                    it.write(data)
                    it.flush()
                }
                
                val responseCode = connection.responseCode
                Log.d(TAG, "📡 [网络请求] 响应码: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    val response = reader.use { it.readText() }
                    
                    Log.d(TAG, "✅ [网络请求] 请求成功，响应长度: ${response.length}")
                    Log.v(TAG, "📄 [网络请求] 响应内容: $response")
                    
                    NetworkResult.Success(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream, StandardCharsets.UTF_8)).use { it.readText() }
                    } else {
                        "HTTP Error $responseCode"
                    }
                    
                    Log.e(TAG, "❌ [网络请求] 请求失败: $responseCode - $errorMessage")
                    
                    // 检查是否为token验证失败
                    handleTokenValidationFailure(context, responseCode, errorMessage)
                    
                    NetworkResult.Error(responseCode, errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [网络请求] 请求异常: ${e.message}", e)
                NetworkResult.Exception(e)
            }
        }
    }
    
    /**
     * 执行POST请求（发送JSON对象）
     * @param url 请求地址
     * @param data 请求数据对象
     * @param context 上下文，用于生成动态User-Agent（可选）
     * @param headers 请求头（可选）
     * @return 响应结果
     */
    suspend fun postJson(
        url: String, 
        data: Any,
        context: Context? = null,
        headers: Map<String, String>? = null
    ): NetworkResult {
        val json = Gson().toJson(data)
        return post(url, json, context, headers)
    }
    
    /**
     * 执行POST请求（发送JSON对象，自动添加token）
     * @param context 上下文，用于获取token
     * @param url 请求地址
     * @param data 请求数据对象
     * @param headers 请求头（可选）
     * @return 响应结果
     */
    suspend fun postJsonWithAuth(
        context: Context,
        url: String, 
        data: Any, 
        headers: Map<String, String>? = null
    ): NetworkResult {
        val authHeaders = addAuthHeaders(context, headers)
        val json = when (data) {
            is JSONObject -> data.toString()
            else -> Gson().toJson(data)
        }
        return post(url, json, context, authHeaders)
    }
    
    /**
     * 执行POST请求（发送表单数据）
     * @param url 请求地址
     * @param formData 表单数据
     * @param context 上下文，用于生成动态User-Agent（可选）
     * @param headers 请求头（可选）
     * @return 响应结果
     */
    suspend fun postForm(
        url: String,
        formData: Map<String, String>,
        context: Context? = null,
        headers: Map<String, String>? = null
    ): NetworkResult {
        val formString = formData.entries.joinToString("&") { (key, value) ->
            "${java.net.URLEncoder.encode(key, "UTF-8")}=${java.net.URLEncoder.encode(value, "UTF-8")}"
        }
        return post(url, formString, context, headers, "application/x-www-form-urlencoded; charset=UTF-8")
    }
    
    /**
     * 解析JSON字符串为指定类型
     * @param json JSON字符串
     * @param clazz 目标类型
     * @return 解析结果
     */
    fun <T> parseJson(json: String, clazz: Class<T>): T? {
        return try {
            gson.fromJson(json, clazz)
        } catch (e: Exception) {
            Log.e(TAG, "🔧 [JSON解析] 解析失败: ${e.message}", e)
            null
        }
    }
    
    /**
     * 解析JSON字符串为指定类型（内联版本）
     */
    inline fun <reified T> parseJson(json: String): T? {
        return parseJson(json, T::class.java)
    }
    
    /**
     * 将对象转换为JSON字符串
     * @param obj 要转换的对象
     * @return JSON字符串
     */
    fun toJson(obj: Any): String {
        return try {
            gson.toJson(obj)
        } catch (e: Exception) {
            Log.e(TAG, "🔧 [JSON转换] 转换失败: ${e.message}", e)
            "{}"
        }
    }
    
    /**
     * 执行文件上传请求（multipart/form-data）
     * @param url 请求地址
     * @param formData 表单数据
     * @param fileData 文件数据（键为字段名，值为文件信息）
     * @param context 上下文，用于生成动态User-Agent（可选）
     * @param headers 请求头（可选）
     * @return 响应结果
     */
    suspend fun uploadFile(
        url: String,
        formData: Map<String, String> = emptyMap(),
        fileData: Map<String, FileUploadData> = emptyMap(),
        context: Context? = null,
        headers: Map<String, String>? = null
    ): NetworkResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🌐 [文件上传] 开始上传请求: $url")
                Log.v(TAG, "📤 [文件上传] 表单数据: $formData")
                Log.v(TAG, "📁 [文件上传] 文件数据: ${fileData.keys}")
                
                val boundary = "----WebKitFormBoundary" + System.currentTimeMillis()
                val connection = URL(url).openConnection() as HttpURLConnection
                
                connection.apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT
                    readTimeout = READ_TIMEOUT * 2 // 文件上传需要更长的超时时间
                    doOutput = true
                    doInput = true
                    setRequestProperty("User-Agent", generateUserAgent(context))
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                    
                    // 添加自定义请求头
                    headers?.forEach { (key, value) ->
                        setRequestProperty(key, value)
                    }
                }
                
                val outputStream = connection.outputStream
                val writer = PrintWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)
                
                // 写入表单数据
                formData.forEach { (key, value) ->
                    writer.append("--$boundary").append("\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$key\"").append("\r\n")
                    writer.append("\r\n")
                    writer.append(value).append("\r\n")
                    writer.flush()
                }
                
                // 写入文件数据
                fileData.forEach { (fieldName, fileInfo) ->
                    writer.append("--$boundary").append("\r\n")
                    writer.append("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${fileInfo.fileName}\"").append("\r\n")
                    writer.append("Content-Type: ${fileInfo.mimeType}").append("\r\n")
                    writer.append("\r\n")
                    writer.flush()
                    
                    // 写入文件内容
                    fileInfo.inputStream.use { input ->
                        input.copyTo(outputStream)
                    }
                    outputStream.flush()
                    
                    writer.append("\r\n")
                    writer.flush()
                }
                
                // 结束边界
                writer.append("--$boundary--").append("\r\n")
                writer.flush()
                writer.close()
                
                val responseCode = connection.responseCode
                Log.d(TAG, "📡 [文件上传] 响应码: $responseCode")
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    val response = reader.use { it.readText() }
                    
                    Log.d(TAG, "✅ [文件上传] 上传成功，响应长度: ${response.length}")
                    Log.v(TAG, "📄 [文件上传] 响应内容: $response")
                    
                    NetworkResult.Success(response)
                } else {
                    val errorStream = connection.errorStream
                    val errorMessage = if (errorStream != null) {
                        BufferedReader(InputStreamReader(errorStream, StandardCharsets.UTF_8)).use { it.readText() }
                    } else {
                        "HTTP Error $responseCode"
                    }
                    
                    Log.e(TAG, "❌ [文件上传] 上传失败: $responseCode - $errorMessage")
                    
                    // 检查是否为token验证失败
                    handleTokenValidationFailure(context, responseCode, errorMessage)
                    
                    NetworkResult.Error(responseCode, errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [文件上传] 上传异常: ${e.message}", e)
                NetworkResult.Exception(e)
            }
        }
    }
    
    /**
     * 执行带认证的文件上传请求（multipart/form-data）
     * @param context 上下文，用于获取认证token
     * @param url 请求地址
     * @param formData 表单数据
     * @param fileData 文件数据（键为字段名，值为文件信息）
     * @param headers 请求头（可选）
     * @return 响应结果
     */
    suspend fun uploadFileWithAuth(
        context: Context,
        url: String,
        formData: Map<String, String> = emptyMap(),
        fileData: Map<String, FileUploadData> = emptyMap(),
        headers: Map<String, String>? = null
    ): NetworkResult {
        val authHeaders = addAuthHeaders(context, headers)
        return uploadFile(url, formData, fileData, context, authHeaders)
    }
    
    /**
     * 文件上传数据类
     */
    data class FileUploadData(
        val fileName: String,
        val mimeType: String,
        val inputStream: InputStream
    )
}

/**
 * 网络请求结果封装类
 */
sealed class NetworkResult {
    data class Success(val data: String) : NetworkResult()
    data class Error(val code: Int, val message: String) : NetworkResult()
    data class Exception(val exception: Throwable) : NetworkResult()
    
    val isSuccess: Boolean
        get() = this is Success
        
    val isError: Boolean
        get() = this is Error || this is Exception
        
    /**
     * 获取成功时的数据，失败时返回null
     */
    fun getDataOrNull(): String? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }
    
    /**
     * 获取错误信息
     */
    fun getErrorMessage(): String {
        return when (this) {
            is Error -> "HTTP $code: $message"
            is NetworkResult.Exception -> this.exception.message ?: "未知错误"
            is Success -> "请求成功"
        }
    }
    
    /**
     * 解析API错误响应中的message字段
     * @return 解析出的错误消息
     */
    fun parseApiErrorMessage(): String {
        return when (this) {
            is Error -> {
                try {
                    val jsonObject = org.json.JSONObject(message)
                    jsonObject.optString("message", "请求失败")
                } catch (e: kotlin.Exception) {
                    android.util.Log.w("NetworkUtils", "解析API错误响应失败: ${e.message}")
                    "请求失败"
                }
            }
            is NetworkResult.Exception -> this.exception.message ?: "网络异常"
            is Success -> "请求成功"
        }
    }
}