package com.jiankangpaika.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * 常见问题数据模型
 */
data class Question(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("file")
    val file: String, // 教学视频URL
    
    @SerializedName("title")
    val title: String, // 问题标题
    
    @SerializedName("switch")
    val switch: Int, // 状态（1=启用，0=禁用）
    
    @SerializedName("createtime")
    val createTime: Long, // 创建时间戳
    
    @SerializedName("updatetime")
    val updateTime: Long // 更新时间戳
)

/**
 * 常见问题列表响应数据模型
 */
data class QuestionListResponse(
    @SerializedName("code")
    val code: Int,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: QuestionListData?,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("datetime")
    val datetime: String
)

/**
 * 常见问题列表数据
 */
data class QuestionListData(
    @SerializedName("list")
    val list: List<Question>,
    
    @SerializedName("pagination")
    val pagination: QuestionPagination
)

/**
 * 常见问题分页信息
 */
data class QuestionPagination(
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
 * 常见问题列表UI状态
 */
data class QuestionListUiState(
    val questions: List<Question> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val hasMoreData: Boolean = true
) {
    fun canLoadMore(): Boolean {
        return !isLoading && !isLoadingMore && hasMoreData
    }
    
    fun hasMoreData(): Boolean {
        return hasMoreData && currentPage < totalPages
    }
}