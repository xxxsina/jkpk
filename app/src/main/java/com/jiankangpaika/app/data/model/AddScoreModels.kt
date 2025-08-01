package com.jiankangpaika.app.data.model

/**
 * 积分增加接口响应数据模型
 */
data class AddScoreResponse(
    val code: Int,
    val message: String,
    val timestamp: Long,
    val datetime: String,
    val data: AddScoreData
)

/**
 * 积分增加数据模型
 */
data class AddScoreData(
    val user_id: String,
    val type: String,
    val score_added: Int,
    val new_score: Int,
    val memo: String,
    val add_time: String
)