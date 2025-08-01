package com.jiankangpaika.app.ui.screens.question

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.data.model.Question
import com.jiankangpaika.app.data.model.QuestionListResponse
import com.jiankangpaika.app.data.model.QuestionListUiState
import com.jiankangpaika.app.ui.components.TopAppBarWithBack
import com.jiankangpaika.app.ui.components.VideoPlayerDialog
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.ToastUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "QuestionListScreen"

/**
 * 加载常见问题列表
 * @param context 上下文
 * @param page 页码
 * @param onSuccess 成功回调
 * @param onError 错误回调
 */
suspend fun loadQuestionList(
    context: Context,
    page: Int = 1,
    onSuccess: (List<Question>, Int) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val url = "${ApiConfig.Question.GET_QUESTION}?page=$page"
        Log.d(TAG, "请求URL: $url")
        
        val result = NetworkUtils.get(url, context)
        when (result) {
            is NetworkResult.Success -> {
                val gson = Gson()
                val response = gson.fromJson(result.data, QuestionListResponse::class.java)
                if (response.code == 200 && response.data != null) {
                    val questions = response.data.list
                    val totalPages = response.data.pagination.totalPages
                    
                    Log.d(TAG, "加载成功，问题数量: ${questions.size}, 总页数: $totalPages")
                    withContext(Dispatchers.Main) {
                        onSuccess(questions, totalPages)
                    }
                } else {
                    val message = response.message.ifEmpty { "加载失败" }
                    Log.e(TAG, "API错误: $message")
                    withContext(Dispatchers.Main) {
                        onError(message)
                    }
                }
            }
            is NetworkResult.Error -> {
                Log.e(TAG, "网络错误: ${result.message}")
                withContext(Dispatchers.Main) {
                    onError("网络请求失败: ${result.code} - ${result.message}")
                }
            }
            is NetworkResult.Exception -> {
                Log.e(TAG, "请求异常", result.exception)
                withContext(Dispatchers.Main) {
                    onError("网络连接失败: ${result.exception.message}")
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "解析数据失败", e)
        withContext(Dispatchers.Main) {
            onError("解析数据失败: ${e.message}")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionListScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // 状态管理
    var questionListState by remember { mutableStateOf(QuestionListUiState()) }
    
    // 视频播放状态
    var showVideoDialog by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf("") }
    
    // 加载问题列表函数
    fun loadQuestions(page: Int = 1, isRefresh: Boolean = false) {
        if (isRefresh) {
            questionListState = questionListState.copy(
                isLoading = page == 1,
                isLoadingMore = page > 1,
                errorMessage = null
            )
        } else {
            questionListState = questionListState.copy(
                isLoading = page == 1 && questionListState.questions.isEmpty(),
                isLoadingMore = page > 1,
                errorMessage = null
            )
        }
        
        coroutineScope.launch {
            loadQuestionList(
                context = context,
                page = page,
                onSuccess = { questions, totalPages ->
                    questionListState = if (page == 1) {
                        questionListState.copy(
                            questions = questions,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = 1,
                            totalPages = totalPages,
                            hasMoreData = totalPages > 1,
                            errorMessage = null
                        )
                    } else {
                        questionListState.copy(
                            questions = questionListState.questions + questions,
                            isLoading = false,
                            isLoadingMore = false,
                            currentPage = page,
                            totalPages = totalPages,
                            hasMoreData = page < totalPages,
                            errorMessage = null
                        )
                    }
                },
                onError = { error ->
                    questionListState = questionListState.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = error
                    )
                    ToastUtils.showErrorToast(context, error)
                }
            )
        }
    }
    
    // 初始加载
    LaunchedEffect(Unit) {
        loadQuestions()
    }
    
    // 下拉刷新逻辑
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            loadQuestions(page = 1, isRefresh = true)
            pullToRefreshState.endRefresh()
        }
    }
    
    // 上拉加载更多逻辑
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= questionListState.questions.size - 3 &&
                    questionListState.hasMoreData &&
                    !questionListState.isLoadingMore &&
                    !questionListState.isLoading) {
                    loadQuestions(page = questionListState.currentPage + 1)
                }
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBarWithBack(
                title = "常见问题",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
                when {
                    questionListState.isLoading && questionListState.questions.isEmpty() -> {
                        // 初始加载状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "加载中...",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    questionListState.questions.isEmpty() && !questionListState.isLoading -> {
                        // 空状态
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "暂无常见问题",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    else -> {
                        // 问题列表
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(questionListState.questions) { question ->
                                QuestionItem(
                                    question = question,
                                    onPlayClick = { videoUrl ->
                                        currentVideoUrl = videoUrl
                                        showVideoDialog = true
                                    }
                                )
                            }
                            
                            // 加载更多指示器
                            if (questionListState.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "加载更多...",
                                                color = Color.Gray,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            } else if (!questionListState.hasMoreData && questionListState.questions.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "没有更多数据了",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            
            // 下拉刷新指示器
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // 视频全屏播放对话框
        if (showVideoDialog && currentVideoUrl.isNotEmpty()) {
            Dialog(
                onDismissRequest = { showVideoDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                VideoPlayerDialog(
                    videoUrl = currentVideoUrl,
                    onDismiss = {
                        showVideoDialog = false
                        currentVideoUrl = ""
                    }
                )
            }
        }
    }
}

@Composable
fun QuestionItem(
    question: Question,
    onPlayClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧标题
            Text(
                text = question.title,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 右侧播放按钮
            Box(
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        if (question.file.isNotEmpty()) {
                            onPlayClick(question.file)
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "点击播放",
                        color = Color(0xFFFF6B6B),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}