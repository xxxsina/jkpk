package com.jiankangpaika.app.data.model

/**
 * 注册请求数据模型
 */
data class RegisterRequest(
    val username: String,
    val password: String,
    val confirm_password: String
)