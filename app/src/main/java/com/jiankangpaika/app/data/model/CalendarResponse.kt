package com.jiankangpaika.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 签到日历API响应数据模型
 * 对应get_calendar.php接口返回的数据结构
 */
data class CalendarResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: CalendarData?,
    
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
     * 获取错误信息
     */
    fun getErrorMessage(): String = message
}

/**
 * 签到日历数据
 */
data class CalendarData(
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("score_earned")
    val scoreEarned: Int,  // 当日签到加的分数
    
    @SerializedName("current_score")
    val currentScore: Int,  // 当日签到获得的总分数
    
    @SerializedName("new_score")
    val newScore: Int,  // 用户得到的总分数（最新）
    
    @SerializedName("today_checkin_count")
    val todayCheckinCount: Int,  // 当日签到总次数
    
    @SerializedName("max_checkin_per_day")
    val maxCheckinPerDay: Int,  // 当日签到上限次数
    
    @SerializedName("checkin_time")
    val checkinTime: String,  // 最后一次签到时间
    
    @SerializedName("calendar_data")
    val calendarData: Map<String, Int>,  // 当月签到日历数据，key为日期（1-31），value为该日签到次数
    
    @SerializedName("field")
    val field: String,  // 查询的月份，格式YYYYMM
    
    @SerializedName("notice_message")
    val noticeMessage: String? = null  // 通知说明信息，支持HTML格式
)

/**
 * 获取签到日历请求数据模型
 */
data class CalendarRequest(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("field")
    val field: String? = null  // 可选参数，格式如202412，默认当月
)