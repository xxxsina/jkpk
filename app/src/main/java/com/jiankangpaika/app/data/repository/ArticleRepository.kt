package com.jiankangpaika.app.data.repository

import android.content.Context
import android.util.Log
import com.jiankangpaika.app.data.model.ArticleItem
import com.jiankangpaika.app.data.model.ArticleListResponse
import com.jiankangpaika.app.data.model.ArticlePagination
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 文章数据仓库
 * 负责从API获取文章数据
 */
object ArticleRepository {
    private const val TAG = "ArticleRepository"
    
    /**
     * 获取文章列表
     * @param context 上下文
     * @param page 页码，默认为1
     * @param pageSize 每页数量，默认为10
     * @return 文章列表，如果获取失败返回空列表
     */
    suspend fun getArticleList(context: Context, page: Int = 1): List<ArticleItem> {
        val result = getArticleListWithPagination(context, page)
        return result.first
    }
    
    /**
     * 获取文章列表和分页信息
     * @param context 上下文
     * @param page 页码，默认为1
     * @param type 文章类型，可选参数
     * @return Pair<文章列表, 分页信息>
     */
    suspend fun getArticleListWithPagination(context: Context, page: Int = 1, type: String? = null): Pair<List<ArticleItem>, ArticlePagination?> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始获取文章列表，页码: $page, 类型: $type")
                
                val url = if (type != null) {
                    "${ApiConfig.Article.GET_LIST}?page=$page&type=$type"
                } else {
                    "${ApiConfig.Article.GET_LIST}?page=$page"
                }
                val result = NetworkUtils.get(url, context)
                
                if (result.isSuccess) {
                    val responseData = result.getDataOrNull()
                    if (responseData != null) {
                        parseArticleListResponseWithPagination(responseData)
                    } else {
                        Log.w(TAG, "获取文章列表成功但数据为空")
                        Pair(emptyList(), null)
                    }
                } else {
                    Log.w(TAG, "获取文章列表失败: $result")
                    Pair(emptyList(), null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取文章列表异常", e)
                Pair(emptyList(), null)
            }
        }
    }
    
    /**
     * 解析文章列表响应数据（包含分页信息）
     * @param responseData 响应数据JSON字符串
     * @return Pair<解析后的文章列表, 分页信息>
     */
    private fun parseArticleListResponseWithPagination(responseData: String): Pair<List<ArticleItem>, ArticlePagination?> {
        return try {
            val gson = Gson()
            val response = gson.fromJson(responseData, ArticleListResponse::class.java)
            
            if (response.isSuccess()) {
                val articles = response.getArticles()
                val pagination = response.getPagination()
                Log.d(TAG, "成功解析文章列表，共${articles.size}篇文章，总页数: ${pagination?.total_pages ?: 0}")
                Pair(articles, pagination)
            } else {
                Log.w(TAG, "API返回错误: code=${response.code}, message=${response.message}")
                Pair(emptyList(), null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析文章列表响应数据失败", e)
            Pair(emptyList(), null)
        }
    }
    
    /**
     * 解析文章列表响应数据（仅返回文章列表，保持向后兼容）
     * @param responseData 响应数据JSON字符串
     * @return 解析后的文章列表
     */
    private fun parseArticleListResponse(responseData: String): List<ArticleItem> {
        val result = parseArticleListResponseWithPagination(responseData)
        return result.first
    }
    
    /**
     * 获取首页展示的文章列表
     * @param context 上下文
     * @param page 页码，默认为1
     * @param pageSize 每页数量，默认为10
     * @return 首页文章列表
     */
    suspend fun getHomeArticleList(context: Context, page: Int = 1): List<ArticleItem> {
        return getArticleList(context, page)
    }
    
    /**
     * 刷新文章列表（获取第一页）
     * @param context 上下文
     * @param pageSize 每页数量，默认为10
     * @return 刷新后的文章列表
     */
    suspend fun refreshArticleList(context: Context): List<ArticleItem> {
        return getArticleList(context, 1)
    }
    
    /**
     * 加载更多文章
     * @param context 上下文
     * @param page 页码
     * @param pageSize 每页数量，默认为10
     * @return 更多文章列表
     */
    suspend fun loadMoreArticles(context: Context, page: Int): List<ArticleItem> {
        return getArticleList(context, page)
    }
}