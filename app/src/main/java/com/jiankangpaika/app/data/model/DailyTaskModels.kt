package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

/**
 * 每日任务数据类
 * 用于解析服务器返回的每日任务信息
 */
@Serializable
data class DailyTask(
    val id: Int,                    // 任务ID
    val file: String,               // 教学视频URL
    val url: String,                // 融码URL
    val title: String,              // 任务标题
    val image: String,              // 封面图片URL
    val switch: Int,                // 状态（1=启用，0=禁用）
    val createtime: Long,           // 创建时间戳
    val updatetime: Long            // 更新时间戳
)

/**
 * 分页信息数据类
 */
@Serializable
data class Pagination(
    val current_page: Int,          // 当前页码
    val page_size: Int,             // 每页条数
    val total_count: Int,           // 总记录数
    val total_pages: Int,           // 总页数
    val has_next: Boolean,          // 是否有下一页
    val has_prev: Boolean           // 是否有上一页
)

/**
 * 每日任务列表响应数据类
 * 用于解析服务器返回的完整响应
 */
@Serializable
data class DailyTaskListResponse(
    val code: Int,                  // 响应码
    val message: String,            // 响应消息
    val data: DailyTaskListData?,   // 任务列表数据
    val timestamp: Long,            // 时间戳
    val datetime: String            // 日期时间
)

/**
 * 每日任务列表数据类
 */
@Serializable
data class DailyTaskListData(
    val list: List<DailyTask>,      // 任务列表
    val pagination: Pagination      // 分页信息
)