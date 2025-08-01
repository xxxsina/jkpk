package com.jiankangpaika.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.data.model.LogoutResponse
import com.jiankangpaika.app.data.model.LogoutRequest
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult

/**
 * 用户管理工具类
 * 负责用户登录状态管理、用户信息存储等功能
 */
object UserManager {
    private const val TAG = "UserManager"
    private const val PREF_NAME = "user_preferences"
    
    // SharedPreferences键名
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_PHONE = "phone"
    private const val KEY_EMAIL = "email"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_LOGIN_TIME = "login_time"
    private const val KEY_TOKEN = "token"
    
    /**
     * 获取SharedPreferences实例
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 检查用户是否已登录
     * @param context 上下文
     * @return 是否已登录
     */
    fun isLoggedIn(context: Context): Boolean {
        val isLoggedIn = getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
        Log.d(TAG, "🔍 [登录状态检查] 用户登录状态: $isLoggedIn")
        return isLoggedIn
    }
    
    /**
     * 保存用户登录信息
     * @param context 上下文
     * @param userId 用户ID
     * @param username 用户名
     * @param nickname 昵称
     * @param phone 手机号
     * @param email 邮箱
     * @param avatarUrl 头像URL
     * @param token 登录令牌
     */
    fun saveUserInfo(
        context: Context,
        userId: String,
        username: String,
        nickname: String? = null,
        phone: String? = null,
        email: String? = null,
        avatarUrl: String? = null,
        token: String? = null
    ) {
        Log.d(TAG, "💾 [用户信息保存] 开始保存用户信息: userId=$userId, username=$username")
        
        with(getPreferences(context).edit()) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_NICKNAME, nickname)
            putString(KEY_PHONE, phone)
            putString(KEY_EMAIL, email)
            putString(KEY_AVATAR, avatarUrl)
            putString(KEY_TOKEN, token)
            putLong(KEY_LOGIN_TIME, System.currentTimeMillis())
            apply()
        }
        
        Log.i(TAG, "✅ [用户信息保存] 用户信息保存成功")
    }
    
    /**
     * 获取用户ID
     */
    fun getUserId(context: Context): String? {
        return getPreferences(context).getString(KEY_USER_ID, null)
    }
    
    /**
     * 获取用户名
     */
    fun getUsername(context: Context): String? {
        return getPreferences(context).getString(KEY_USERNAME, null)
    }
    
    /**
     * 获取昵称
     */
    fun getNickname(context: Context): String? {
        return getPreferences(context).getString(KEY_NICKNAME, null)
    }
    
    /**
     * 获取手机号
     */
    fun getPhone(context: Context): String? {
        return getPreferences(context).getString(KEY_PHONE, null)
    }
    
    /**
     * 获取邮箱
     */
    fun getEmail(context: Context): String? {
        return getPreferences(context).getString(KEY_EMAIL, null)
    }
    
    /**
     * 获取头像URL
     */
    fun getAvatarUrl(context: Context): String? {
        return getPreferences(context).getString(KEY_AVATAR, null)
    }
    
    /**
     * 获取登录令牌
     */
    fun getToken(context: Context): String? {
        return getPreferences(context).getString(KEY_TOKEN, null)
    }
    
    /**
     * 获取登录时间
     */
    fun getLoginTime(context: Context): Long {
        return getPreferences(context).getLong(KEY_LOGIN_TIME, 0)
    }
    
    /**
     * 用户退出登录
     * @param context 上下文
     * @return 退出登录结果
     */
    suspend fun logout(context: Context): LogoutResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🚪 [用户退出] 开始退出登录流程")
                
                // 获取用户ID和token
                val userId = getUserId(context)
                val token = getToken(context)
                
                // 验证必要参数
                if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
                    Log.w(TAG, "⚠️ [用户退出] 用户ID或token为空，直接清除本地数据")
                    clearLocalUserData(context)
                    return@withContext LogoutResult.Success("退出登录成功")
                }
                
                // 构建退出登录请求（只需要user_id，token通过header传递）
                val logoutRequest = LogoutRequest(
                    user_id = userId
                )
                
                // 调用logout.php接口（使用带认证的方法）
                val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.LOGOUT, logoutRequest)
                
                when (result) {
                    is NetworkResult.Success -> {
                        try {
                            val logoutResponse = NetworkUtils.parseJson<LogoutResponse>(result.data)
                            if (logoutResponse?.isSuccess() == true) {
                                Log.d(TAG, "✅ [用户退出] 服务器退出登录成功: ${logoutResponse.message}")
                                
                                // 清除本地用户数据
                                clearLocalUserData(context)
                                
                                LogoutResult.Success(logoutResponse.message)
                            } else {
                                Log.w(TAG, "⚠️ [用户退出] 服务器退出登录失败: ${logoutResponse?.message ?: "未知错误"}")
                                
                                // 即使服务器返回失败，也清除本地数据
                                clearLocalUserData(context)
                                
                                LogoutResult.Success("退出登录成功")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ [用户退出] 解析退出登录响应失败: ${e.message}")
                            
                            // 解析失败也清除本地数据
                            clearLocalUserData(context)
                            
                            LogoutResult.Success("退出登录成功")
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.w(TAG, "⚠️ [用户退出] 网络请求失败: ${result.message}")
                        
                        // 网络失败也清除本地数据
                        clearLocalUserData(context)
                        
                        LogoutResult.Success("退出登录成功")
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "💥 [用户退出] 网络请求异常: ${result.exception.message}")
                        
                        // 网络异常也清除本地数据
                        clearLocalUserData(context)
                        
                        LogoutResult.Success("退出登录成功")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [用户退出] 退出登录异常: ${e.message}", e)
                
                // 发生异常也清除本地数据
                clearLocalUserData(context)
                
                LogoutResult.Success("退出登录成功")
            }
        }
    }
    
    /**
     * 清除本地用户数据
     * @param context 上下文
     */
    fun clearLocalUserData(context: Context) {
        Log.d(TAG, "🧹 [数据清理] 开始清除本地用户数据")
        
        // 清除UserManager的用户数据
        with(getPreferences(context).edit()) {
            clear()
            apply()
        }
        
        // 清除user_prefs中的用户相关数据
        val userPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(userPrefs.edit()) {
            // 清除头像相关数据
            remove("selected_avatar_index")
            remove("has_custom_avatar")
            remove("custom_avatar_uri")
            // 清除绑定的联系方式
            remove("binded_phone")
            remove("binded_email")
            // 清除昵称
            remove("user_nickname")
            apply()
        }
        
        Log.i(TAG, "✅ [数据清理] 本地用户数据清除成功")
    }
    
    /**
     * 更新用户昵称
     */
    fun updateNickname(context: Context, nickname: String) {
        Log.d(TAG, "📝 [昵称更新] 更新用户昵称: $nickname")
        
        with(getPreferences(context).edit()) {
            putString(KEY_NICKNAME, nickname)
            apply()
        }
        
        Log.i(TAG, "✅ [昵称更新] 昵称更新成功")
    }
    
    /**
     * 更新用户手机号
     */
    fun updatePhone(context: Context, phone: String) {
        Log.d(TAG, "📱 [手机号更新] 更新用户手机号: $phone")
        
        with(getPreferences(context).edit()) {
            putString(KEY_PHONE, phone)
            apply()
        }
        
        Log.i(TAG, "✅ [手机号更新] 手机号更新成功")
    }
    
    /**
     * 更新用户邮箱
     */
    fun updateEmail(context: Context, email: String) {
        Log.d(TAG, "📧 [邮箱更新] 更新用户邮箱: $email")
        
        with(getPreferences(context).edit()) {
            putString(KEY_EMAIL, email)
            apply()
        }
        
        Log.i(TAG, "✅ [邮箱更新] 邮箱更新成功")
    }
    
    /**
     * 更新用户头像URL
     */
    fun updateAvatarUrl(context: Context, avatarUrl: String) {
        Log.d(TAG, "🖼️ [头像更新] 更新用户头像URL: $avatarUrl")
        
        with(getPreferences(context).edit()) {
            putString(KEY_AVATAR, avatarUrl)
            apply()
        }
        
        Log.i(TAG, "✅ [头像更新] 头像URL更新成功")
    }
}

/**
 * 退出登录结果密封类
 */
sealed class LogoutResult {
    data class Success(val message: String) : LogoutResult()
    data class Error(val message: String) : LogoutResult()
}