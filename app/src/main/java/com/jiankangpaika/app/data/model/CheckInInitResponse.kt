package com.jiankangpaika.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 签到初始化API响应数据模型
 * 对应check_in_init.php接口返回的数据结构
 */
data class CheckInInitResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: CheckInInitData?,
    
    @SerializedName("timestamp")
    val timestamp: Long? = null,
    
    @SerializedName("datetime")
    val datetime: String? = null
) {
    /**
     * 判断请求是否成功
     */
    fun isSuccess(): Boolean = code == 200
    
    /**
     * 获取数据，如果请求失败返回null
     */
    fun getDataOrNull(): CheckInInitData? = if (isSuccess()) data else null
}

/**
 * 签到初始化数据
 */
data class CheckInInitData(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("score_earned")
    val scoreEarned: Int,
    
    @SerializedName("score_again_more")
    val scoreAgainMore: Int,
    
    @SerializedName("today_checkin_count")
    val todayCheckinCount: Int,
    
    @SerializedName("max_checkin_per_day")
    val maxCheckinPerDay: Int,
    
    @SerializedName("max_score_again_more")
    val maxScoreAgainMore: Int,
    
    @SerializedName("today_score_again_more")
    val todayScoreAgainMore: Int,
    
    @SerializedName("notice_message")
    val noticeMessage: String?
)

/**
 * 签到初始化结果封装类
 */
sealed class CheckInInitResult {
    data class Success(
        val userId: Int,
        val scoreEarned: Int,
        val scoreAgainMore: Int,
        val todayCheckinCount: Int,
        val maxCheckinPerDay: Int,
        val maxScoreAgainMore: Int,
        val todayScoreAgainMore: Int,
        val noticeMessage: String? = null
    ) : CheckInInitResult()
    
    data class Error(val message: String) : CheckInInitResult()
}