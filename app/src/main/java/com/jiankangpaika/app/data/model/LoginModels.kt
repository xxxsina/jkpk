package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

/**
 * 登录请求数据类
 */
@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * 登录响应数据类
 */
data class LoginResponse(
    val code: Int,
    val message: String, // 消息字符串
    val timestamp: Long,
    val datetime: String,
    val data: LoginData? // 成功时返回用户数据，失败时为null
) {
    /**
     * 判断是否登录成功
     */
    fun isSuccess(): Boolean {
        return code == 200 && data != null
    }
    
    /**
     * 获取用户数据
     */
    fun getUserData(): LoginData? {
        return data
    }
}

/**
 * 登录数据类
 */
@Serializable
data class LoginData(
    val user_id: Int,
    val token: String,
    val user_info: UserInfo
)

/**
 * 用户信息数据类
 */
@Serializable
data class UserInfo(
    val id: Int,
    val username: String,
    val nickname: String?,
    val mobile: String?,
    val email: String?,
    val avatar: String?,
    val status: String
)

/**
 * API响应基础类
 */
@Serializable
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    val isSuccess: Boolean
        get() = code == 200
}