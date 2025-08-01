package com.jiankangpaika.app.ui.screens.sport

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.data.model.ArticleItem
import com.jiankangpaika.app.data.model.ArticlePagination
import com.jiankangpaika.app.data.repository.ArticleRepository
import com.jiankangpaika.app.ui.components.FeedAdCard
import com.jiankangpaika.app.ui.screens.home.ArticleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SportScreen(onArticleClick: (ArticleItem) -> Unit = {}) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // 文章列表状态
    var articleItems by remember { mutableStateOf<List<ArticleItem>>(emptyList()) }
    var currentPage by remember { mutableStateOf(1) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMoreData by remember { mutableStateOf(true) }
    var totalPages by remember { mutableStateOf(0) }
    var pagination by remember { mutableStateOf<ArticlePagination?>(null) }
    
    // 下拉刷新状态
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 防抖状态 - 防止重复请求
    var isInitialized by remember { mutableStateOf(false) }
    
    // 滑动状态管理
    val listState = rememberLazyListState()
    
    // 广告加载状态
    var isAdLoaded by remember { mutableStateOf(false) }
    var isAdLoading by remember { mutableStateOf(false) }
    
    // 刷新函数
    val refresh: suspend () -> Unit = refresh@{
        if (isLoading) {
            Log.d("SportScreen", "正在加载中，跳过刷新请求")
            return@refresh
        }
        isRefreshing = true
        isLoading = true
        try {
            val result = ArticleRepository.getArticleListWithPagination(context, 1, "jx")
            val articles = result.first
            val paginationInfo = result.second
            
            // 只有在成功获取到分页信息时才更新状态
            if (paginationInfo != null) {
                articleItems = articles
                currentPage = 1
                pagination = paginationInfo
                totalPages = paginationInfo.total_pages
                hasMoreData = paginationInfo.has_next
                
                Log.d("SportScreen", "刷新成功，获取${articles.size}篇文章，当前页: $currentPage，总页数: $totalPages，是否有更多: $hasMoreData")
            } else {
                Log.w("SportScreen", "刷新失败：未获取到分页信息")
            }
        } catch (e: Exception) {
            Log.e("SportScreen", "刷新文章列表失败", e)
        } finally {
            isRefreshing = false
            isLoading = false
        }
    }
    
    // 加载更多函数
    val loadMore: suspend () -> Unit = loadMore@{
        if (isLoading) {
            Log.d("SportScreen", "跳过加载更多: isLoading=$isLoading")
            return@loadMore
        }
        Log.d("SportScreen", "加载更多文章")
        isLoading = true
        try {
            val nextPage = currentPage + 1
            val result = ArticleRepository.getArticleListWithPagination(context, nextPage, "jx")
            val newArticles = result.first
            val paginationInfo = result.second
            
            // 只有在成功获取到分页信息时才更新状态
            if (paginationInfo != null) {
                articleItems = articleItems + newArticles
                currentPage = nextPage
                pagination = paginationInfo
                totalPages = paginationInfo.total_pages
                hasMoreData = paginationInfo.has_next
                
                Log.d("SportScreen", "加载更多完成，第${nextPage}页获取${newArticles.size}篇文章，当前页: $currentPage，总页数: $totalPages，是否有更多: $hasMoreData")
            } else {
                Log.w("SportScreen", "加载更多失败：未获取到分页信息，保持当前状态")
            }
        } catch (e: Exception) {
            Log.e("SportScreen", "加载更多文章失败", e)
        } finally {
            isLoading = false
        }
    }
    
    // 监听下拉刷新
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            refresh()
        }
    }
    
    // 监听刷新状态，完成刷新
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    // 监听滚动状态，实现上拉加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged { old, new ->
                // 只有当关键状态发生变化时才触发
                val oldLastVisible = old.visibleItemsInfo.lastOrNull()?.index ?: 0
                val newLastVisible = new.visibleItemsInfo.lastOrNull()?.index ?: 0
                old.totalItemsCount == new.totalItemsCount && oldLastVisible == newLastVisible
            }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                
                // 当滚动到倒数第3个item时，触发加载更多
                if (totalItems > 0 && lastVisibleItem >= totalItems - 3 && !isLoading && hasMoreData) {
                    Log.d("SportScreen", "[上拉触发] totalItems: $totalItems, lastVisible: $lastVisibleItem, 开始执行加载更多")
                    coroutineScope.launch {
                        loadMore()
                    }
                }
            }
    }
    
    // 初始化逻辑 - 防抖处理，避免重复请求
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            isInitialized = true
            // 初始加载文章数据
            refresh()
        }
    }
    
    // 预加载广告
    LaunchedEffect(articleItems.isNotEmpty()) {
        if (articleItems.isNotEmpty() && AdUtils.shouldShowFeedAd(context) && !isAdLoading && !isAdLoaded) {
            isAdLoading = true
            Log.d("SportScreen", "🔄 [广告预加载] 开始预加载信息流广告")
            
            val adManager = UnifiedAdManager.getInstance()
            adManager.loadFeedAd(context) { success, message ->
                isAdLoading = false
                if (success) {
                    isAdLoaded = true
                    Log.i("SportScreen", "✅ [广告预加载] 信息流广告预加载成功")
                } else {
                    Log.e("SportScreen", "❌ [广告预加载] 信息流广告预加载失败: $message")
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp)
        ) {
            // 头部信息流广告位
            if (isAdLoaded) {
                item {
                    FeedAdCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        onAdLoaded = { success ->
                            // 广告加载完成回调
                            if (success) {
                                Log.i("SportScreen", "📱 [信息流广告] 精选页面广告加载成功")
                            } else {
                                Log.w("SportScreen", "📱 [信息流广告] 精选页面广告加载失败")
                            }
                        },
                        onAdShown = { success ->
                            // 广告展示完成回调
                            if (success) {
                                Log.i("SportScreen", "🎯 [信息流广告] 精选页面广告展示成功")
                            } else {
                                Log.e("SportScreen", "🎯 [信息流广告] 精选页面广告展示失败")
                            }
                        }
                    )
                }
            }
            
            // 精选页面内容区域 - 文章列表
            if (articleItems.isEmpty() && isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                items(articleItems) { article ->
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        ArticleCard(
                            article = article,
                            onArticleClick = onArticleClick
                        )
                    }
                }
                
                // 加载更多指示器
                if (isLoading && articleItems.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "正在加载更多...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else if (!hasMoreData && articleItems.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "没有更多内容了",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
        
        // 下拉刷新指示器
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
        )
    }
}