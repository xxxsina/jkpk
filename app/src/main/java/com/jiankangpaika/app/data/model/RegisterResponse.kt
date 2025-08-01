package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

/**
 * 注册响应数据模型
 */
data class RegisterResponse(
    val code: Int,
    val message: String, // 消息字符串
    val timestamp: Long,
    val datetime: String,
    val data: RegisterData? // 成功时返回用户数据，失败时为null
) {
    /**
     * 判断是否注册成功
     */
    fun isSuccess(): Boolean {
        return code == 200 && data != null
    }
    
    /**
     * 获取用户数据
     */
    fun getUserData(): RegisterData? {
        return data
    }
}

/**
 * 注册数据类
 */
@Serializable
data class RegisterData(
    val user_id: Int,
    val token: String,
    val user_info: RegisterUserInfo
)

/**
 * 注册用户信息
 */
@Serializable
data class RegisterUserInfo(
    val id: Int,
    val username: String,
    val nickname: String,
    val mobile: String?,
    val email: String?,
    val avatar: String?,
    val gender: String,
    val birthday: String?,
    val status: String,
)