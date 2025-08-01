package com.jiankangpaika.app.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// 运动项目数据类
data class SportItem(
    val id: String = "", // 项目ID
    val name: String,
    val category: String,
    val imageRes: String, // 图片URL或资源路径
    val description: String = "", // 项目介绍信息
    val isCompleted: Boolean = false
)

// 文章项目数据类
data class ArticleItem(
    val id: Int,
    val type: String,
    val title: String,
    val cover_image: String,
    val content: String
)

// 文章列表分页信息
data class ArticlePagination(
    val current_page: Int,
    val page_size: Int,
    val total_count: Int,
    val total_pages: Int,
    val has_next: Boolean,
    val has_prev: Boolean
)

// 文章列表响应数据
data class ArticleListData(
    val list: List<ArticleItem>,
    val pagination: ArticlePagination
)

// 文章列表API响应
data class ArticleListResponse(
    val code: Int,
    val message: String,
    val data: ArticleListData?,
    val timestamp: Long? = null,
    val datetime: String? = null
) {
    fun isSuccess(): Boolean = code == 200
    fun getArticles(): List<ArticleItem> = data?.list ?: emptyList()
    fun getPagination(): ArticlePagination? = data?.pagination
}

// 运动练习卡片数据类
data class SportExerciseItem(
    val title: String,
    val imageRes: Int
)

// 底部导航项数据类
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val isSelected: Boolean = false
)

// 签到任务数据类
data class CheckInTask(
    val title: String,
    val isCompleted: Boolean = false
)

// 用户菜单项数据类
data class ProfileMenuItem(
    val title: String,
    val subtitle: String = "",
    val onClick: () -> Unit = {}
)

// 幻灯片数据类
data class SlideData(
    val imageRes: Int,
    val title: String
)