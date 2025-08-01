package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

/**
 * 退出登录请求数据类
 * 注意：token现在通过HTTP header传递，不再包含在请求体中
 */
@Serializable
data class LogoutRequest(
    val user_id: String
)

/**
 * 退出登录响应数据类
 */
@Serializable
data class LogoutResponse(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val datetime: String
) {
    /**
     * 判断是否退出成功
     */
    fun isSuccess(): Boolean {
        return code == 200
    }
}