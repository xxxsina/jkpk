package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

/**
 * 修改用户信息请求数据类
 * 注意：token现在通过HTTP header传递，不再包含在请求体中
 */
@Serializable
data class UpdateUserRequest(
    val action: String,
    val user_id: String,
    val value: String,
    val event: String? = null
)

/**
 * 修改用户信息响应数据类
 */
@Serializable
data class UpdateUserResponse(
    val code: Int,
    val message: String,
    val data: UpdatedUserInfo?,
    val timestamp: Long,
    val datetime: String
) {
    /**
     * 判断是否修改成功
     */
    fun isSuccess(): Boolean {
        return code == 200 && data != null
    }
    
    /**
     * 获取更新后的用户数据
     */
    fun getUpdatedUserData(): UpdatedUserInfo? {
        return data
    }
}

/**
 * 更新后的用户信息数据类
 */
@Serializable
data class UpdatedUserInfo(
    val user_id: String,
    val username: String,
    val nickname: String?,
    val phone: String?,
    val email: String?,
    val avatar: String?,
    val status: String,
    val created_at: String,
    val updated_at: String
)

/**
 * 修改昵称请求数据类
 * 注意：token现在通过HTTP header传递，不再包含在请求体中
 */
@Serializable
data class UpdateNicknameRequest(
    val action: String = "update_nickname",
    val user_id: String,
    val value: String
)

/**
 * 带短信验证的手机号修改请求数据类
 */
@Serializable
data class UpdatePhoneWithSmsRequest(
    val action: String,
    val user_id: String,
    val value: String,
    val smsCode: String,
    val event: String
)

/**
 * 短信验证请求数据类
 */
@Serializable
data class VerifySmsRequest(
    val mobile: String,
    val sms_code: String,
    val event: String
)

/**
 * 短信验证响应数据类
 */
@Serializable
data class VerifySmsResponse(
    val code: Int,
    val message: String,
    val data: VerifySmsData?,
    val timestamp: Long,
    val datetime: String
) {
    /**
     * 判断是否验证成功
     */
    fun isSuccess(): Boolean {
        return code == 200 && data?.valid == true
    }
}

/**
 * 短信验证数据类
 */
@Serializable
data class VerifySmsData(
    val valid: Boolean,
    val message: String
)