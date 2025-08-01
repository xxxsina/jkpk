package com.jiankangpaika.app.ui.screens.home

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.jiankangpaika.app.R
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ad.UnifiedAdManager
import com.jiankangpaika.app.data.model.SportItem
import com.jiankangpaika.app.data.model.ArticleItem
import com.jiankangpaika.app.data.model.ArticlePagination
import com.jiankangpaika.app.data.repository.SportRepository
import com.jiankangpaika.app.data.repository.ArticleRepository
import com.jiankangpaika.app.ui.components.BannerAdCard
import com.jiankangpaika.app.ui.components.FeedAdCard
import com.jiankangpaika.app.ui.navigation.NavigationManager
import com.jiankangpaika.app.ui.screens.checkin.CheckInScreen
import com.jiankangpaika.app.ui.screens.profile.ProfileScreen
import com.jiankangpaika.app.ui.screens.sport.ArticleDetailScreen
import com.jiankangpaika.app.ui.screens.sport.SportScreen
import com.jiankangpaika.app.ui.screens.task.DailyTaskListScreen
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    navController: NavController,
    navigationManager: NavigationManager,
    initialTabIndex: Int = 0
) {
    var selectedTabIndex by remember { mutableStateOf(initialTabIndex) }
    var selectedSport by remember { mutableStateOf<SportItem?>(null) }
    val context = LocalContext.current

    // 如果选择了运动项目，显示文章详细页面
    if (selectedSport != null) {
        ArticleDetailScreen(
            title = selectedSport!!.name,
            category = selectedSport!!.category,
            imageRes = selectedSport!!.imageRes,
            content = selectedSport!!.description,
            onBackClick = { selectedSport = null }
        )
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (selectedTabIndex == 2) {
                    Modifier.background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                } else {
                    Modifier.background(
                        when (selectedTabIndex) {
                            0 -> Color(0xFFF5F5F5) // 首页 - 浅灰色
                            1 -> Color.White // 运动页面 - 白色
                            3 -> Color.White // 我的页面 - 白色
                            else -> Color.White
                        }
                    )
                }
            )
    ) {
        // 主要内容区域
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when (selectedTabIndex) {
                0 -> MainHomeContent(
                    onSportClick = { sport -> selectedSport = sport }
                )
                1 -> SportScreen(
                    onArticleClick = { article -> 
                        // 将ArticleItem转换为SportItem以兼容现有的详情页面
                        val sportItem = SportItem(
                            id = article.id.toString(),
                            name = article.title,
                            category = article.type,
                            imageRes = article.cover_image,
                            description = article.content
                        )
                        selectedSport = sportItem
                    }
                )
                2 -> CheckInScreen(
                    navigationManager = navigationManager
                )
                3 -> ProfileScreen(
                    navController = navController,
                    navigationManager = navigationManager
                )
            }
            
            // 悬浮层 - 位于底部导航栏上方（仅在首页显示）
            if (selectedTabIndex == 0) {
                FloatingBottomLayer(selectedTabIndex = selectedTabIndex)
            }
        }
    }
}

/**
 * 首页主要内容组件
 * 整合首页的所有主要UI元素，包括状态栏、Banner和运动卡片区域
 * 
 * @param onSportClick 运动卡片点击事件回调
 * 
 * 布局结构：
 * - 顶部：状态栏显示时间和系统状态
 * - 中上：Banner横幅展示主要信息
 * - 中下：运动卡片网格展示各项运动
 * - 底部：弹性空间，确保内容合理分布
 * 
 * 设计特点：
 * - 垂直线性布局，内容从上到下有序排列
 * - 浅灰色背景，提供舒适的视觉体验
 * - 响应式设计，适配不同屏幕尺寸
 * - 支持下拉刷新和上拉翻页
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHomeContent(onSportClick: (SportItem) -> Unit = {}) {
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
    
    // 分页大小由接口控制，不需要本地设置
    
    // 刷新函数
    val refresh: suspend () -> Unit = refresh@{
        if (isLoading) {
            Log.d("HomeScreen", "正在加载中，跳过刷新请求")
            return@refresh
        }
        isRefreshing = true
        isLoading = true
        try {
            val result = ArticleRepository.getArticleListWithPagination(context, 1)
            val articles = result.first
            val paginationInfo = result.second
            
            // 只有在成功获取到分页信息时才更新状态
            if (paginationInfo != null) {
                articleItems = articles
                currentPage = 1
                pagination = paginationInfo
                totalPages = paginationInfo.total_pages
                hasMoreData = paginationInfo.has_next
                
                Log.d("HomeScreen", "刷新成功，获取${articles.size}篇文章，当前页: $currentPage，总页数: $totalPages，是否有更多: $hasMoreData")
            } else {
                Log.w("HomeScreen", "刷新失败：未获取到分页信息")
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "刷新文章列表失败", e)
        } finally {
            isRefreshing = false
            isLoading = false
        }
    }
    
    // 加载更多函数
    val loadMore: suspend () -> Unit = loadMore@{
        if (isLoading) {
            Log.d("HomeScreen", "跳过加载更多: isLoading=$isLoading")
            return@loadMore
        }
        Log.e("HomeScreen", "加载更多文章 ==============》 ")
        isLoading = true
        try {
            val nextPage = currentPage + 1
            val result = ArticleRepository.getArticleListWithPagination(context, nextPage)
            val newArticles = result.first
            val paginationInfo = result.second
            
            // 只有在成功获取到分页信息时才更新状态
            if (paginationInfo != null) {
                articleItems = articleItems + newArticles
                currentPage = nextPage
                pagination = paginationInfo
                totalPages = paginationInfo.total_pages
                hasMoreData = paginationInfo.has_next
                
                Log.d("HomeScreen", "加载更多完成，第${nextPage}页获取${newArticles.size}篇文章，当前页: $currentPage，总页数: $totalPages，是否有更多: $hasMoreData")
            } else {
                Log.w("HomeScreen", "加载更多失败：未获取到分页信息，保持当前状态")
            }
        } catch (e: Exception) {
            Log.e("HomeScreen", "加载更多文章失败", e)
        } finally {
            Log.e("HomeScreen", "finally isLoading ===> $isLoading")
            isLoading = false
        }
    }
    
    // 监听下拉刷新 - 参考DailyTaskListScreen.kt的实现
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
    
    // 初始化逻辑 - 防抖处理，避免重复请求
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            isInitialized = true
            // 初始加载文章数据
            refresh()
            
            // 延迟1秒后加载插屏广告，确保页面内容已经展示
             delay(1000)
             Log.d("HomeScreen", "🎯 [插屏广告] 首次进入app，准备加载插屏广告")

             if (AdUtils.shouldShowInterstitialAd(context)) {
                 Log.d("HomeScreen", "✅ [插屏广告策略] 满足展示条件，开始加载插屏广告")
                 val adManager = UnifiedAdManager.getInstance()
                 adManager.loadInterstitialAd(context) { success, message ->
                     if (success) {
                         Log.i("HomeScreen", "✅ [插屏广告] 加载成功，准备展示")
                         // 广告加载成功后立即展示
                         val activity = context as? Activity
                         if (activity != null) {
                             adManager.showInterstitialAd(activity) { showSuccess, showMessage ->
                                 if (showSuccess) {
                                     AdUtils.recordInterstitialAdShown(context)
                                     Log.i("HomeScreen", "🎉 [插屏广告] 展示成功，记录展示时间")
                                 } else {
                                     Log.w("HomeScreen", "⚠️ [插屏广告] 展示失败: $showMessage")
                                 }
                             }
                         } else {
                             Log.e("HomeScreen", "❌ [插屏广告] 无法获取Activity实例")
                         }
                     } else {
                         Log.w("HomeScreen", "⚠️ [插屏广告] 加载失败: $message")
                     }
                 }
             } else {
                 Log.e("HomeScreen", "✅ [插屏广告策略] 不满足展示条件，加载插屏广告失败")
             }
         }
     }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        // 可滚动内容区域 - 包含信息流广告位和文章卡片
        ScrollableContentSection(
            articleItems = articleItems,
            isLoading = isLoading,
            hasMoreData = hasMoreData,
            onArticleClick = { article ->
                // 将ArticleItem转换为SportItem以兼容现有的详情页面
                val sportItem = SportItem(
                    name = article.title,
                    category = article.type,
                    imageRes = article.cover_image, // 使用文章封面图片URL
                    description = article.content
                )
                onSportClick(sportItem)
            },
            onLoadMore = {
                coroutineScope.launch {
                    loadMore()
                }
            }
        )
        
        // 下拉刷新指示器
        PullToRefreshContainer(
            modifier = Modifier.align(Alignment.TopCenter),
            state = pullToRefreshState,
        )
    }
}

/**
 * 可滚动内容区域组件
 * 包含信息流广告位和文章卡片网格，支持统一滚动
 * 
 * @param articleItems 文章列表
 * @param isLoading 是否正在加载更多
 * @param hasMoreData 是否还有更多数据
 * @param onArticleClick 文章卡片点击事件回调
 * @param onLoadMore 加载更多回调
 */
@Composable
fun ScrollableContentSection(
    articleItems: List<ArticleItem> = emptyList(),
    isLoading: Boolean = false,
    hasMoreData: Boolean = true,
    onArticleClick: (ArticleItem) -> Unit = {},
    onLoadMore: () -> Unit = {}
) {
    
    // 获取Context
    val context = LocalContext.current
    
    // 广告显示状态
    var isAdVisible by remember { mutableStateOf(true) }
    // 广告加载完成状态
    var isFeedAdLoaded by remember { mutableStateOf(false) }
    
    // 滚动状态
    val lazyGridState = rememberLazyStaggeredGridState()
    
    // 监听滚动状态，实现上拉加载更多 - 参考DailyTaskListScreen.kt的实现
    LaunchedEffect(lazyGridState) {
        snapshotFlow { lazyGridState.layoutInfo }
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
                    Log.d("HomeScreen", "[上拉触发] totalItems: $totalItems, lastVisible: $lastVisibleItem, 开始执行加载更多")
                    onLoadMore()
                }
            }
    }
    
    // 广告显示逻辑由FeedAdCard组件自己处理，无需预加载
    // 当文章数据加载完成后，设置广告为可显示状态
    LaunchedEffect(articleItems.isNotEmpty()) {
        if (articleItems.isNotEmpty()) {
            isFeedAdLoaded = true
            Log.d("HomeScreen", "📝 [广告状态] 文章数据已加载，设置广告为可显示状态")
        }
    }

    LazyVerticalStaggeredGrid(
        state = lazyGridState,
        columns = StaggeredGridCells.Fixed(1), // 固定1列布局，一行显示一条信息
        horizontalArrangement = Arrangement.spacedBy(12.dp), // 水平间距12dp
        verticalItemSpacing = 12.dp, // 垂直间距12dp
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // 左右边距16dp
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp) // 顶部和底部内边距，增加底部内边距避免被浮窗遮挡
    ) {
        // banner广告位 - 占据整行（仅在广告加载完成且可见时显示）
        if (isAdVisible && isFeedAdLoaded) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
//                        .height(adHeight) // 使用动态高度
//                        .padding(vertical = 8.dp)
                        .background(
                            Color(0xFFF5F5F5)
//                            RoundedCornerShape(12.dp)
                        )
//                        .border(width = 1.dp, Color.Gray)
                        .clickable {
                            // 广告点击事件处理
                            Log.d("HomeScreen", "banner广告被点击")
                        }
                ) {
                    // 头部信息流广告位
                    FeedAdCard(
                        modifier = Modifier
                            .fillMaxWidth(),
                        onAdLoaded = { success ->
                            // 广告加载完成回调
                            if (success) {
                                isFeedAdLoaded = true
                                Log.i("HomeScreen", "📱 [信息流广告] 精选页面广告加载成功")
                            } else {
                                Log.w("HomeScreen", "📱 [信息流广告] 精选页面广告加载失败")
                            }
                        },
                        onAdShown = { success ->
                            // 广告展示完成回调
                            if (success) {
                                Log.i("HomeScreen", "🎯 [信息流广告] 精选页面广告展示成功")
                            } else {
                                Log.e("HomeScreen", "🎯 [信息流广告] 精选页面广告展示失败")
                            }
                        }
                    )
                    
                    // 关闭按钮 - 位于右上角
//                    Box(
//                        modifier = Modifier
//                            .align(Alignment.TopEnd)
//                            .padding(8.dp)
//                            .size(24.dp)
//                            .background(
//                                Color.White.copy(alpha = 0f),
//                                CircleShape
//                            )
//                            .clickable {
//                                // 关闭广告
//                                isAdVisible = false
//                                Log.d("HomeScreen", "banner广告被关闭")
//                            },
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "×",
//                            color = Color.Gray,
//                            fontSize = 16.sp,
//                            fontWeight = FontWeight.W100
//                        )
//                    }
                }
            }
        }
        
        // 文章卡片网格
        items(articleItems) { article ->
            ArticleCard(
                article = article,
                onArticleClick = onArticleClick
            )
        }
        
        // 加载更多指示器
        if (isLoading) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "", // 正在加载更多...
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (!hasMoreData && articleItems.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "", // 没有更多内容了
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Banner横幅组件 - 幻灯切换显示
 * 显示首页顶部的幻灯片轮播，支持点击切换和自动播放
 * 
 * 功能特点：
 * - 支持多张幻灯片切换
 * - 点击切换到下一张
 * - 圆角设计，提升视觉效果
 * - 响应式布局，适配不同屏幕尺寸
 * - 底部指示器显示当前位置
 */
@Composable
fun BannerSection() {
    // 幻灯片数据
    val bannerImages = listOf(
        R.drawable.banner_1,
        R.drawable.banner_2
    )
    
    var currentIndex by remember { mutableStateOf(0) }
    
    // 获取屏幕配置信息
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    
    // 根据屏幕高度自适应Banner高度
    // 小屏幕(高度<700dp): Banner高度为屏幕高度的22%
    // 中等屏幕(700dp-900dp): Banner高度为屏幕高度的20%
    // 大屏幕(>900dp): Banner高度为屏幕高度的18%
    val bannerHeight: Dp = when {
        screenHeight < 700.dp -> screenHeight * 0.22f
        screenHeight < 900.dp -> screenHeight * 0.20f
        else -> screenHeight * 0.18f
    }.coerceIn(160.dp, 280.dp) // 限制最小160dp，最大280dp
    
    // 自动切换效果
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000) // 3秒自动切换
            currentIndex = (currentIndex + 1) % bannerImages.size
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                currentIndex = (currentIndex + 1) % bannerImages.size
            }
    ) {
        // 当前幻灯片
        Image(
            painter = painterResource(id = bannerImages[currentIndex]),
            contentDescription = "Banner ${currentIndex + 1}",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 底部指示器
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            bannerImages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            color = if (index == currentIndex) Color.White else Color.White.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * 文章卡片组件
 * 显示单个文章的信息卡片，包含封面图片、标题和类型标签
 * 
 * @param article 文章数据，包含标题、类型和封面图片信息
 * @param onArticleClick 文章卡片点击事件回调
 * 
 * 功能特点：
 * - 自适应高度：根据内容自动调整卡片高度
 * - 响应式设计：支持不同屏幕尺寸
 * - 视觉层次：通过阴影和圆角提升视觉效果
 * - 信息展示：清晰展示文章标题和类型
 * - 网络图片：支持加载网络封面图片
 * - 点击交互：支持点击跳转到详细页面
 */
@Composable
fun ArticleCard(
    article: ArticleItem,
    onArticleClick: (ArticleItem) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // 填充父容器宽度
            .height(120.dp) // 固定高度适合一行显示
            .clickable { onArticleClick(article) }, // 添加点击事件
        shape = RoundedCornerShape(10.dp), // 10dp圆角
        colors = CardDefaults.cardColors(
            containerColor = Color.White // 白色卡片背景
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // 2dp阴影
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize() // 填充卡片大小
                .padding(13.dp), // 内边距13dp
            horizontalArrangement = Arrangement.spacedBy(12.dp) // 水平间距12dp
        ) {
            // 左侧图片区域
            // 使用封面图片作为文章的视觉标识
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp)) // 8dp圆角
            ) {
                // 显示文章的封面图片
                AsyncImage(
                    model = article.cover_image,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 标签盒子
                Box(
                    modifier = Modifier
                        .height(24.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-8).dp, y = 8.dp) // 距离父容器右边和上边8dp
                        .background(
                            Color(0xFFED5548), // 0xFFED5548色背景
                            RoundedCornerShape(4.dp) // 4dp圆角
                        )
                        .padding(horizontal = 6.dp), // 标签内边距
                    contentAlignment = Alignment.Center // 内容垂直居中
                ) {
                    Text(
                        text = article.type,
                        fontSize = 10.sp,
                        lineHeight = 10.sp, // 设置行高等于字体大小
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // 右侧信息区域
            // 文章标题垂直居中显示
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f), // 占据剩余空间
                verticalArrangement = Arrangement.Center // 垂直居中
            ) {
                // 文章标题
                Text(
                    text = article.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black,
                    maxLines = 2, // 允许两行显示
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * 悬浮底部层组件
 * 位于底部导航栏上方的悬浮层，宽度与屏幕宽度相同
 */
@Composable
fun BoxScope.FloatingBottomLayer(selectedTabIndex: Int) {
    // 悬浮层显示状态
    var isFloatingLayerVisible by remember { mutableStateOf(true) }
    // 广告加载状态
    var isAdLoaded by remember { mutableStateOf(false) }
    // 广告高度状态
    var adHeight by remember { mutableStateOf(100.dp) }
    
    // 获取Context
    val context = LocalContext.current

    // 监听标签页变化，当返回首页时重置悬浮层显示状态
    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex == 0) {
            isFloatingLayerVisible = true
            Log.d("HomeScreen", "返回首页，重置悬浮层显示状态")
        }
    }

    // 先加载广告，不依赖UI显示
    LaunchedEffect(Unit) {
        // 检查是否应该显示Banner广告
        val shouldShowAd = AdUtils.shouldShowBannerAd(context)
        Log.d("HomeScreen", "🔍 [ScrollableContentSection] 检查广告显示条件: shouldShowAd=$shouldShowAd")

        if (shouldShowAd) {
            // 开始加载广告
            UnifiedAdManager.getInstance().loadBannerAd(context) { success, message ->
                if (success) {
                    Log.i("HomeScreen", "📱 [Banner广告] 首页 - 广告加载成功")
                    isAdLoaded = true
                    // 根据广告内容动态调整高度
                    // Banner广告通常高度在60-100dp之间
                    adHeight = 100.dp
                } else {
                    Log.w("HomeScreen", "📱 [Banner广告] 首页 - 广告加载失败: $message")
                    isAdLoaded = false
                }
            }
        } else {
            Log.d("HomeScreen", "🚫 [ScrollableContentSection] 不满足广告显示条件，隐藏广告")
            isAdLoaded = false
        }
    }
    
    if (isFloatingLayerVisible && isAdLoaded) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight() // 根据BannerAdCard高度自适应
                .background(
                    Color.White
                )
//                .drawBehind {
//                    val strokeWidth = 1.dp.toPx()
//                    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
//                    drawRect(
//                        color = Color(0xFFEFEDED),
//                        style = Stroke(
//                            width = strokeWidth,
//                            pathEffect = pathEffect
//                        )
//                    )
//                }
                .align(Alignment.BottomCenter) // 悬浮在底部
        ) {
            // 悬浮层内容 - BannerAdCard设置合理高度
            BannerAdCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 75.dp), // 设置最小60dp，最大120dp的高度范围
                onAdLoaded = { success ->
                    // 这里只处理展示回调，加载状态已在LaunchedEffect中处理
                    Log.d("HomeScreen", "📱 [BannerAdCard] 内部加载回调: success=$success")
                },
                onAdShown = { success ->
                    // 广告展示完成回调
                    if (success) {
                        Log.i("HomeScreen", "🎯 [Banner广告] 首页 - 广告展示成功")
                        // 可以在这里添加广告展示统计逻辑
                    } else {
                        Log.e("HomeScreen", "🎯 [Banner广告] 首页 - 广告展示失败")
                    }
                }
            )
            
            // 关闭按钮 - 位于右上角
//            Box(
//                modifier = Modifier
//                    .align(Alignment.TopEnd)
//                    .padding(2.dp)
//                    .size(26.dp)
//                    .background(
//                        Color.White.copy(alpha = 0f)
//                    )
//                    .clickable {
//                        // 关闭悬浮层
//                        isFloatingLayerVisible = false
//                        Log.d("HomeScreen", "悬浮层被关闭")
//                    },
//                contentAlignment = Alignment.Center
//            ) {
//                Text(
//                    text = "×",
//                    color = Color.Gray,
//                    fontSize = 16.sp,
//                    fontWeight = FontWeight.W100
//                )
//            }
        }
    }
}