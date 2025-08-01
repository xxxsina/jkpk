package com.jiankangpaika.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 系统配置响应模型
 */
data class SystemConfigResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: SystemConfigData?
) {
    fun isSuccess(): Boolean = code == 200
}

/**
 * 系统配置数据
 */
data class SystemConfigData(
    @SerializedName("customer_service")
    val customerService: CustomerServiceConfig,
    
    @SerializedName("quick_app")
    val quickApp: QuickAppConfig
)

/**
 * 客服表单配置
 */
data class CustomerServiceConfig(
    @SerializedName("image_upload_enabled")
    val imageUploadEnabled: Boolean = true,
    
    @SerializedName("video_upload_enabled")
    val videoUploadEnabled: Boolean = true,
    
    @SerializedName("max_image_size")
    val maxImageSizeMB: String = "10",
    
    @SerializedName("max_video_size")
    val maxVideoSizeMB: String = "50",
    
    @SerializedName("allowed_image_types")
    val allowedImageTypes: String = "jpg,jpeg,png,gif",
    
    @SerializedName("allowed_video_types")
    val allowedVideoTypes: String = "mp4,avi,mov,wmv"
)

/**
 * 快应用配置
 */
data class QuickAppConfig(
    @SerializedName("enabled")
    val enabled: Boolean = false,
    
    @SerializedName("app_name")
    val appName: String = ""
)

/**
 * 客服表单提交请求
 */
data class CustomerServiceRequest(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("realname")
    val realname: String,
    
    @SerializedName("mobile")
    val mobile: String,
    
    @SerializedName("problem")
    val problem: String,
    
    @SerializedName("image")
    val image: String = "",
    
    @SerializedName("vedio")
    val video: String = ""
)

/**
 * 客服表单提交响应
 */
data class CustomerServiceResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: Any? = null
) {
    fun isSuccess(): Boolean = code == 200
}

/**
 * 客服消息数据
 * 对应customer_message_list.php接口返回的消息数据结构
 */
@kotlinx.serialization.Serializable
data class CustomerMessage(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("status")
    val status: String, // new, answer
    
    @SerializedName("looked")
    val looked: Int, // 0或1，表示是否已查看
    
    @SerializedName("realname")
    val realname: String,
    
    @SerializedName("mobile")
    val mobile: String,
    
    @SerializedName("problem")
    val problem: String,
    
    @SerializedName("answer")
    val answer: String = "",
    
    @SerializedName("image")
    val image: String = "",
    
    @SerializedName("video")
    val video: String = "",
    
    @SerializedName("answer_image")
    val answerImage: String = "",
    
    @SerializedName("answer_video")
    val answerVideo: String = "",
    
    @SerializedName("is_overcome")
    val isOvercome: Int = 0, // 0否，1是
    
    @SerializedName("createtime")
    val createTime: Long,
    
    @SerializedName("updatetime")
    val updateTime: Long,
    
    @SerializedName("createtime_formatted")
    val createTimeFormatted: String? = null,
    
    @SerializedName("updatetime_formatted")
    val updateTimeFormatted: String? = null
)

/**
 * 分页信息
 */
data class PaginationInfo(
    @SerializedName("current_page")
    val currentPage: Int,
    
    @SerializedName("page_size")
    val pageSize: Int,
    
    @SerializedName("total_count")
    val totalCount: Int,
    
    @SerializedName("total_pages")
    val totalPages: Int,
    
    @SerializedName("has_next")
    val hasNext: Boolean,
    
    @SerializedName("has_prev")
    val hasPrev: Boolean
)

/**
 * 客服消息列表数据
 */
data class CustomerMessagesData(
    @SerializedName("list")
    val list: List<CustomerMessage>,
    
    @SerializedName("pagination")
    val pagination: PaginationInfo
)

/**
 * 客服消息列表响应
 * 对应customer_message_list.php接口返回的数据结构
 */
data class CustomerMessagesResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: CustomerMessagesData?,
    
    @SerializedName("timestamp")
    val timestamp: Long? = null,
    
    @SerializedName("datetime")
    val datetime: String? = null
) {
    fun isSuccess(): Boolean = code == 200
    
    fun getMessages(): List<CustomerMessage> = data?.list ?: emptyList()
    
    fun getPagination(): PaginationInfo? = data?.pagination
}