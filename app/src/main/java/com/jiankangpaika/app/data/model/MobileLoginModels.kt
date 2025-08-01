package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

/**
 * 手机短信登录请求数据类
 */
@Serializable
data class MobileLoginRequest(
    val mobile: String,
    val sms_code: String,
    val captcha_code: String,
    val session_id: String
)

/**
 * 获取图形验证码响应数据类
 */
@Serializable
data class CaptchaResponse(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val datetime: String,
    val data: CaptchaData?
) {
    /**
     * 判断是否获取成功
     */
    fun isSuccess(): Boolean {
        return code == 200 && data != null
    }
    
    /**
     * 获取验证码数据
     */
    fun getCaptchaData(): CaptchaData? {
        return data
    }
}

/**
 * 图形验证码数据类
 */
@Serializable
data class CaptchaData(
    val session_id: String,
    val image: String, // base64编码的图片数据
    val image_url: String? = null // 图片URL
)

/**
 * 发送短信验证码请求数据类
 */
@Serializable
data class SendSmsRequest(
    val mobile: String,
    val captcha_code: String,
    val session_id: String,
    val event: String = "bind" // 事件类型：bind/unbind
)

/**
 * 发送短信验证码响应数据类
 */
@Serializable
data class SendSmsResponse(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val datetime: String,
    val data: SendSmsData? = null
) {
    /**
     * 判断是否发送成功
     */
    fun isSuccess(): Boolean {
        return code == 200
    }
}

/**
 * 发送短信验证码成功时的数据类
 */
@Serializable
data class SendSmsData(
    val phone: String,
    val code_id: Boolean
)