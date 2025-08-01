package com.jiankangpaika.app.services

import android.content.Context
import android.util.Log
import com.jiankangpaika.app.data.model.UpdateUserRequest
import com.jiankangpaika.app.data.model.UpdateUserResponse
import com.jiankangpaika.app.data.model.UpdateNicknameRequest
import com.jiankangpaika.app.data.model.UpdatePhoneWithSmsRequest
import com.jiankangpaika.app.data.model.VerifySmsRequest
import com.jiankangpaika.app.data.model.VerifySmsResponse
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.constants.ApiConfig

/**
 * 用户服务类
 * 处理用户相关的网络请求
 */
object UserService {
    private const val TAG = "UserService"
    
    /**
     * 修改用户昵称
     * @param context 上下文
     * @param userId 用户ID
     * @param nickname 新昵称
     * @return 修改结果
     */
    suspend fun updateNickname(
        context: Context,
        userId: String,
        nickname: String
    ): UpdateUserResponse? {
        return try {
            Log.d(TAG, "🔄 [修改昵称] 开始修改用户昵称: userId=$userId, nickname=$nickname")
            
            // 验证昵称格式
            if (nickname.isBlank()) {
                ToastUtils.showErrorToast(context, "昵称不能为空")
                return null
            }
            
            if (nickname.length < 2 || nickname.length > 20) {
                ToastUtils.showErrorToast(context, "昵称长度必须在2-20个字符之间")
                return null
            }
            
            // 检查特殊字符
            val invalidChars = listOf('<', '>', '"', '\'', '/', '\\')
            if (nickname.any { it in invalidChars }) {
                ToastUtils.showErrorToast(context, "昵称不能包含特殊字符: < > \" ' / \\")
                return null
            }
            
            // 构建请求数据
            val request = UpdateNicknameRequest(
                user_id = userId,
                value = nickname
            )
            
            // 发送网络请求（使用带认证的方法）
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.UPDATE_USER, request)
            
            when (result) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "✅ [修改昵称] 网络请求成功")
                    
                    // 解析响应数据
                    val response = NetworkUtils.parseJson<UpdateUserResponse>(result.data)
                    if (response != null) {
                        if (response.isSuccess()) {
                            Log.d(TAG, "🎉 [修改昵称] 昵称修改成功")
                            ToastUtils.showSuccessToast(context, "昵称修改成功")
                            response
                        } else {
                            Log.e(TAG, "❌ [修改昵称] 服务器返回错误: ${response.message}")
                            ToastUtils.showErrorToast(context, response.message)
                            null
                        }
                    } else {
                        Log.e(TAG, "❌ [修改昵称] 响应数据解析失败")
                        ToastUtils.showErrorToast(context, "响应数据解析失败")
                        null
                    }
                }
                
                is NetworkResult.Error -> {
                    val errorMessage = result.parseApiErrorMessage()
                    Log.e(TAG, "❌ [修改昵称] 网络请求失败: ${result.code} - $errorMessage")
                    ToastUtils.showErrorToast(context, errorMessage)
                    null
                }
                
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [修改昵称] 网络请求异常: ${result.exception.message}", result.exception)
                    ToastUtils.showErrorToast(context, "网络请求异常: ${result.exception.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [修改昵称] 修改昵称异常: ${e.message}", e)
            ToastUtils.showErrorToast(context, "修改昵称失败: ${e.message}")
            null
        }
    }
    
    /**
     * 修改用户头像
     * @param context 上下文
     * @param userId 用户ID
     * @param avatar 新头像URL
     * @return 修改结果
     */
    suspend fun updateAvatar(
        context: Context,
        userId: String,
        avatar: String
    ): UpdateUserResponse? {
        return try {
            Log.d(TAG, "🔄 [修改头像] 开始修改用户头像: userId=$userId")
            
            // 验证头像URL格式
            if (avatar.isBlank()) {
                ToastUtils.showErrorToast(context, "头像URL不能为空")
                return null
            }
            
            if (avatar.length > 500) {
                ToastUtils.showErrorToast(context, "头像URL长度不能超过500个字符")
                return null
            }
            
            // 构建请求数据
            val request = UpdateUserRequest(
                action = "update_avatar",
                user_id = userId,
                value = avatar
            )
            
            // 发送网络请求（使用带认证的方法）
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.UPDATE_USER, request)
            
            when (result) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "✅ [修改头像] 网络请求成功")
                    
                    // 解析响应数据
                    val response = NetworkUtils.parseJson<UpdateUserResponse>(result.data)
                    if (response != null) {
                        if (response.isSuccess()) {
                            Log.d(TAG, "🎉 [修改头像] 头像修改成功")
                            ToastUtils.showSuccessToast(context, "头像修改成功")
                            response
                        } else {
                            Log.e(TAG, "❌ [修改头像] 服务器返回错误: ${response.message}")
                            ToastUtils.showErrorToast(context, response.message)
                            null
                        }
                    } else {
                        Log.e(TAG, "❌ [修改头像] 响应数据解析失败")
                        ToastUtils.showErrorToast(context, "响应数据解析失败")
                        null
                    }
                }
                
                is NetworkResult.Error -> {
                    val errorMessage = result.parseApiErrorMessage()
                    Log.e(TAG, "❌ [修改头像] 网络请求失败: ${result.code} - $errorMessage")
                    ToastUtils.showErrorToast(context, errorMessage)
                    null
                }
                
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [修改头像] 网络请求异常: ${result.exception.message}", result.exception)
                    ToastUtils.showErrorToast(context, "网络请求异常: ${result.exception.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [修改头像] 修改头像异常: ${e.message}", e)
            ToastUtils.showErrorToast(context, "修改头像失败: ${e.message}")
            null
        }
    }
    
    /**
     * 修改用户手机号
     * @param context 上下文
     * @param userId 用户ID
     * @param phone 新手机号
     * @return 修改结果
     */
    suspend fun updatePhone(
        context: Context,
        userId: String,
        phone: String
    ): UpdateUserResponse? {
        return try {
            Log.d(TAG, "🔄 [修改手机号] 开始修改用户手机号: userId=$userId")
            
            // 验证手机号格式
            if (phone.isBlank()) {
                ToastUtils.showErrorToast(context, "手机号不能为空")
                return null
            }
            
            val phoneRegex = Regex("^1[3-9]\\d{9}$")
            if (!phoneRegex.matches(phone)) {
                ToastUtils.showErrorToast(context, "手机号格式不正确")
                return null
            }
            
            // 构建请求数据
            val request = UpdateUserRequest(
                action = "update_phone",
                user_id = userId,
                value = phone
            )
            
            // 发送网络请求
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.UPDATE_USER, request)
            
            when (result) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "✅ [修改手机号] 网络请求成功")
                    
                    // 解析响应数据
                    val response = NetworkUtils.parseJson<UpdateUserResponse>(result.data)
                    if (response != null) {
                        if (response.isSuccess()) {
                            Log.d(TAG, "🎉 [修改手机号] 手机号修改成功")
                            ToastUtils.showSuccessToast(context, "手机号修改成功")
                            response
                        } else {
                            Log.e(TAG, "❌ [修改手机号] 服务器返回错误: ${response.message}")
                            ToastUtils.showErrorToast(context, response.message)
                            null
                        }
                    } else {
                        Log.e(TAG, "❌ [修改手机号] 响应数据解析失败")
                        ToastUtils.showErrorToast(context, "响应数据解析失败")
                        null
                    }
                }
                
                is NetworkResult.Error -> {
                    val errorMessage = result.parseApiErrorMessage()
                    Log.e(TAG, "❌ [修改手机号] 网络请求失败: ${result.code} - $errorMessage")
                    ToastUtils.showErrorToast(context, errorMessage)
                    null
                }
                
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [修改手机号] 网络请求异常: ${result.exception.message}", result.exception)
                    ToastUtils.showErrorToast(context, "网络请求异常: ${result.exception.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [修改手机号] 修改手机号异常: ${e.message}", e)
            ToastUtils.showErrorToast(context, "修改手机号失败: ${e.message}")
            null
        }
    }
    
    /**
     * 修改用户邮箱
     * @param context 上下文
     * @param userId 用户ID
     * @param email 邮箱地址
     * @param event 操作类型：bind(绑定) 或 unbind(解绑)
     * @return 修改结果
     */
    suspend fun updateEmail(
        context: Context,
        userId: String,
        email: String,
        event: String
    ): UpdateUserResponse? {
        return try {
            Log.d(TAG, "🔄 [修改邮箱] 开始修改用户邮箱: userId=$userId, event=$event")
            
            // 验证邮箱格式（绑定时需要验证，解绑时不需要）
            if (event == "bind") {
                if (email.isBlank()) {
                    ToastUtils.showErrorToast(context, "邮箱不能为空")
                    return null
                }
                
                if (email.length > 100) {
                    ToastUtils.showErrorToast(context, "邮箱长度不能超过100个字符")
                    return null
                }
                
                val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                if (!emailRegex.matches(email)) {
                    ToastUtils.showErrorToast(context, "邮箱格式不正确")
                    return null
                }
            }
            
            // 构建请求数据
            val request = UpdateUserRequest(
                action = "update_email",
                user_id = userId,
                value = email,
                event = event
            )
            
            // 发送网络请求
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.UPDATE_USER, request)
            
            when (result) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "✅ [修改邮箱] 网络请求成功")
                    
                    // 解析响应数据
                    val response = NetworkUtils.parseJson<UpdateUserResponse>(result.data)
                    if (response != null) {
                        if (response.isSuccess()) {
                            Log.d(TAG, "🎉 [修改邮箱] 邮箱修改成功")
                            ToastUtils.showSuccessToast(context, "邮箱修改成功")
                            response
                        } else {
                            Log.e(TAG, "❌ [修改邮箱] 服务器返回错误: ${response.message}")
                            ToastUtils.showErrorToast(context, response.message)
                            null
                        }
                    } else {
                        Log.e(TAG, "❌ [修改邮箱] 响应数据解析失败")
                        ToastUtils.showErrorToast(context, "响应数据解析失败")
                        null
                    }
                }
                
                is NetworkResult.Error -> {
                    val errorMessage = result.parseApiErrorMessage()
                    Log.e(TAG, "❌ [修改邮箱] 网络请求失败: ${result.code} - $errorMessage")
                    ToastUtils.showErrorToast(context, errorMessage)
                    null
                }
                
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [修改邮箱] 网络请求异常: ${result.exception.message}", result.exception)
                    ToastUtils.showErrorToast(context, "网络请求异常: ${result.exception.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [修改邮箱] 修改邮箱异常: ${e.message}", e)
            ToastUtils.showErrorToast(context, "修改邮箱失败: ${e.message}")
            null
        }
    }
    
    /**
     * 带短信验证的手机号修改
     * @param context 上下文
     * @param userId 用户ID
     * @param phone 新手机号（解绑时传空字符串）
     * @param smsCode 短信验证码
     * @param event 操作类型（bind/unbind）
     * @return 修改结果
     */
    suspend fun updatePhoneWithSms(
        context: Context,
        userId: String,
        phone: String,
        smsCode: String,
        event: String
    ): UpdateUserResponse? {
        return try {
            Log.d(TAG, "🔄 [带验证修改手机号] 开始操作: userId=$userId, event=$event")
            
            // 验证短信验证码
            if (smsCode.isBlank()) {
                ToastUtils.showErrorToast(context, "短信验证码不能为空")
                return null
            }
            
            // 如果是绑定操作，验证手机号格式
            if (event == "bind" && phone.isNotBlank()) {
                val phoneRegex = Regex("^1[3-9]\\d{9}$")
                if (!phoneRegex.matches(phone)) {
                    ToastUtils.showErrorToast(context, "手机号格式不正确")
                    return null
                }
            }
            
            // 构建请求数据
            val request = UpdatePhoneWithSmsRequest(
                action = "update_phone",
                user_id = userId,
                value = phone,
                smsCode = smsCode,
                event = event
            )
            
            // 发送网络请求
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.UPDATE_USER, request)
            
            when (result) {
                is NetworkResult.Success -> {
                    Log.d(TAG, "✅ [带验证修改手机号] 网络请求成功")
                    
                    // 解析响应数据
                    val response = NetworkUtils.parseJson<UpdateUserResponse>(result.data)
                    if (response != null) {
                        if (response.isSuccess()) {
                            val message = if (event == "unbind") "解绑手机成功" else "绑定手机成功"
                            Log.d(TAG, "🎉 [带验证修改手机号] $message")
                            
                            // 更新本地存储的手机号
                            if (event == "unbind") {
                                UserManager.updatePhone(context, "")
                            } else {
                                UserManager.updatePhone(context, phone)
                            }
                            
                            ToastUtils.showSuccessToast(context, message)
                            response
                        } else {
                            Log.e(TAG, "❌ [带验证修改手机号] 服务器返回错误: ${response.message}")
                            ToastUtils.showErrorToast(context, response.message)
                            null
                        }
                    } else {
                        Log.e(TAG, "❌ [带验证修改手机号] 响应数据解析失败")
                        ToastUtils.showErrorToast(context, "响应数据解析失败")
                        null
                    }
                }
                
                is NetworkResult.Error -> {
                    val errorMessage = result.parseApiErrorMessage()
                    Log.e(TAG, "❌ [带验证修改手机号] 网络请求失败: ${result.code} - $errorMessage")
                    ToastUtils.showErrorToast(context, errorMessage)
                    null
                }
                
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [带验证修改手机号] 网络请求异常: ${result.exception.message}", result.exception)
                    ToastUtils.showErrorToast(context, "网络请求异常: ${result.exception.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [带验证修改手机号] 操作异常: ${e.message}", e)
            ToastUtils.showErrorToast(context, "操作失败: ${e.message}")
            null
        }
    }
}