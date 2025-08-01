package com.jiankangpaika.app.ui.screens.task

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.jiankangpaika.app.ui.components.VideoThumbnail
import com.jiankangpaika.app.ui.components.VideoPlayerDialog
import com.jiankangpaika.app.data.model.DailyTask
import com.jiankangpaika.app.data.model.DailyTaskListResponse

import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.constants.ApiConfig
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyTaskListScreen(
    onNavigateBack: () -> Unit,
    navigationManager: com.jiankangpaika.app.ui.navigation.NavigationManager? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    var taskList by remember { mutableStateOf<List<DailyTask>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableStateOf(1) }
    var hasNextPage by remember { mutableStateOf(false) }
    
    // 视频播放对话框状态
    var showVideoDialog by remember { mutableStateOf(false) }
    var selectedVideoUrl by remember { mutableStateOf("") }
    
    // 下载状态管理
    var downloadProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var downloadingVideos by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadIds by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    
    // 下载进度弹窗状态
    var showDownloadDialog by remember { mutableStateOf(false) }
    var currentDownloadUrl by remember { mutableStateOf("") }
    var currentDownloadProgress by remember { mutableStateOf(0) }
    
    // 加载任务列表
    fun loadTasks(page: Int = 1, append: Boolean = false) {
        coroutineScope.launch {
            try {
                if (!append) {
                    isLoading = true
                    errorMessage = null
                } else {
                    isLoadingMore = true
                }
                
                val url = "${ApiConfig.DailyTask.GET_LIST}?page=$page"
                Log.d("DailyTaskListScreen", "请求URL: $url")
                
                val result = NetworkUtils.get(url, context)
                when (result) {
                    is com.jiankangpaika.app.utils.NetworkResult.Success -> {
                        val response = Json.decodeFromString<DailyTaskListResponse>(result.data)
                        if (response.code == 200 && response.data != null) {
                            if (append) {
                                taskList = taskList + response.data.list
                            } else {
                                taskList = response.data.list
                            }
                            currentPage = response.data.pagination.current_page
                            hasNextPage = response.data.pagination.has_next
                            Log.d("DailyTaskListScreen", "加载成功，任务数量: ${response.data.list.size}")
                        } else {
                            errorMessage = response.message
                            Log.e("DailyTaskListScreen", "API错误: ${response.message}")
                        }
                    }
                    is com.jiankangpaika.app.utils.NetworkResult.Error -> {
                        errorMessage = "网络错误: ${result.message}"
                        Log.e("DailyTaskListScreen", "网络错误: ${result.message}")
                    }
                    is com.jiankangpaika.app.utils.NetworkResult.Exception -> {
                        errorMessage = "请求异常: ${result.exception.message}"
                        Log.e("DailyTaskListScreen", "请求异常", result.exception)
                    }
                }
            } catch (e: Exception) {
                errorMessage = "解析数据失败: ${e.message}"
                Log.e("DailyTaskListScreen", "解析数据失败", e)
            } finally {
                isLoading = false
                isLoadingMore = false
            }
        }
    }
    
    // 视频全屏播放对话框
    if (showVideoDialog) {
        Dialog(
            onDismissRequest = { showVideoDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            VideoPlayerDialog(
                videoUrl = selectedVideoUrl,
                onDismiss = { showVideoDialog = false }
            )
        }
    }
    
    // 下载进度弹窗
    if (showDownloadDialog) {
        DownloadProgressDialog(
            progress = currentDownloadProgress,
            onDismiss = { 
                showDownloadDialog = false
                // 取消下载
                downloadIds[currentDownloadUrl]?.let { downloadId ->
                    try {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        downloadManager.remove(downloadId)
                        downloadingVideos = downloadingVideos - currentDownloadUrl
                        downloadProgress = downloadProgress - currentDownloadUrl
                        downloadIds = downloadIds - currentDownloadUrl
                        ToastUtils.showInfoToast(context, "已取消下载")
                    } catch (e: Exception) {
                        Log.e("DailyTaskListScreen", "取消下载失败", e)
                    }
                }
            },
            onBackgroundDownload = {
                // 关闭弹窗，继续后台下载
                showDownloadDialog = false
                ToastUtils.showInfoToast(context, "下载将在后台继续")
            }
        )
    }
    
    // 初始加载
    LaunchedEffect(Unit) {
        loadTasks()
    }
    
    // 下拉刷新处理
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            loadTasks(1)
        }
    }
    
    // 监听加载状态，完成刷新
    LaunchedEffect(isLoading) {
        if (!isLoading && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    // 监听滚动状态，实现上拉加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                
                // 当滚动到倒数第3个item时，触发加载更多
                if (totalItems > 0 && lastVisibleItem >= totalItems - 3 && hasNextPage && !isLoading && !isLoadingMore) {
                    loadTasks(currentPage + 1, append = true)
                }
            }
    }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            when {
                isLoading && taskList.isEmpty() -> {
                    // 首次加载显示loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFEA3323)
                        )
                    }
                }
                errorMessage != null && taskList.isEmpty() -> {
                    // 错误状态
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage ?: "加载失败",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadTasks() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEA3323)
                            )
                        ) {
                            Text("重试", color = Color.White)
                        }
                    }
                }
                else -> {
                    // 任务列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(taskList) { task ->
                            DailyTaskCard(
                                task = task,
                                onStartTask = { url ->
                                    // 跳转到WebView浏览
                                    openWebView(context, url, navigationManager)
                                },
                                onShare = { url ->
                                    // 复制链接到剪贴板
                                    copyToClipboard(context, url)
                                },
                                onDownloadVideo = { videoUrl -> 
                                    downloadVideo(
                                        context = context,
                                        videoUrl = videoUrl,
                                        onDownloadStart = { url, downloadId ->
                                            downloadingVideos = downloadingVideos + url
                                            downloadIds = downloadIds + (url to downloadId)
                                            currentDownloadUrl = url
                                            currentDownloadProgress = 0
                                            showDownloadDialog = true
                                        },
                                        onProgressUpdate = { url, progress ->
                                            downloadProgress = downloadProgress + (url to progress)
                                            if (url == currentDownloadUrl) {
                                                currentDownloadProgress = progress
                                            }
                                        },
                                        onDownloadComplete = { url ->
                                            downloadingVideos = downloadingVideos - url
                                            downloadProgress = downloadProgress - url
                                            downloadIds = downloadIds - url
                                            if (url == currentDownloadUrl) {
                                                showDownloadDialog = false
                                            }
                                        }
                                    )
                                },
                                onPlayVideo = { videoUrl ->
                                    // 播放教学视频
                                    selectedVideoUrl = videoUrl
                                    showVideoDialog = true
                                },
                                isDownloading = downloadingVideos.contains(task.file),
                                downloadProgress = downloadProgress[task.file] ?: 0
                            )
                        }
                        
                        // 加载更多指示器
                        if (hasNextPage) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isLoadingMore) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "加载中...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "上拉加载更多",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        } else if (taskList.isNotEmpty()) {
                            // 没有更多数据时显示提示
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "没有更多数据了",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
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
    }
}

@Composable
fun DailyTaskCard(
    task: DailyTask,
    onStartTask: (String) -> Unit,
    onShare: (String) -> Unit,
    onDownloadVideo: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    isDownloading: Boolean = false,
    downloadProgress: Int = 0
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 视频播放区域
            VideoThumbnail(
                videoUrl = task.file,
                onClick = {
                    onPlayVideo(task.file)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp)),
                showHintText = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 任务标题和下载视频按钮在同一行
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 任务标题（红色文字）
                Text(
                    text = task.title,
                    color = Color(0xFFEA3323),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(7f)
                )
                
                // 下载视频按钮（缩小版）
                OutlinedButton(
                    modifier = Modifier.weight(3f).height(28.dp),
                    onClick = { if (!isDownloading) onDownloadVideo(task.file) },
                    border = null,
                    shape = RoundedCornerShape(40.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isDownloading) Color(0xFFFFE0B2) else Color(0xFFE1E2FD)
                    ),
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        Text(
                            text = "${downloadProgress}%",
                            color = Color(0xFF666666),
                            fontSize = 10.sp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "下载视频",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "下载视频",
                            color = Color.Blue,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 操作按钮区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 开始任务按钮
                Button(
                    onClick = { onStartTask(task.url) },
                    modifier = Modifier.weight(5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEA3323)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "开始任务",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
                
                // 分享按钮
                OutlinedButton(
                    onClick = { onShare(task.url) },
                    modifier = Modifier.weight(3f),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        width = 1.dp
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "分享",
                        color = Color(0xFF666666),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// 打开WebView浏览
private fun openWebView(context: Context, url: String, navigationManager: com.jiankangpaika.app.ui.navigation.NavigationManager?) {
    try {
        if (navigationManager != null) {
            // 使用内部WebView
            navigationManager.navigateToWebView(url, "任务详情")
            Log.d("DailyTaskListScreen", "使用内部WebView打开: $url")
        } else {
            // 降级到外部浏览器
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            Log.d("DailyTaskListScreen", "使用外部浏览器打开: $url")
        }
    } catch (e: Exception) {
        Log.e("DailyTaskListScreen", "打开WebView失败", e)
        ToastUtils.showErrorToast(context, "打开链接失败")
    }
}

// 复制链接到剪贴板
private fun copyToClipboard(context: Context, url: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("任务链接", url)
        clipboard.setPrimaryClip(clip)
        ToastUtils.showSuccessToast(context, "已复制链接，请发送给好友")
        Log.d("DailyTaskListScreen", "复制链接: $url")
    } catch (e: Exception) {
        Log.e("DailyTaskListScreen", "复制链接失败", e)
        ToastUtils.showErrorToast(context, "复制失败")
    }
}

// 下载视频
private fun downloadVideo(
    context: Context, 
    videoUrl: String,
    onDownloadStart: (String, Long) -> Unit = { _, _ -> },
    onProgressUpdate: (String, Int) -> Unit = { _, _ -> },
    onDownloadComplete: (String) -> Unit = { _ -> }
) {
    // 用于防止重复调用onDownloadComplete
    val isCompleted = java.util.concurrent.atomic.AtomicBoolean(false)
    try {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(videoUrl)
        
        // 获取文件名
        val fileName = "video_${System.currentTimeMillis()}.mp4"
        
        val request = DownloadManager.Request(uri).apply {
            setTitle("视频下载")
            setDescription("正在下载任务视频...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, fileName)
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            setAllowedOverRoaming(false)
        }
        
        val downloadId = downloadManager.enqueue(request)
        onDownloadStart(videoUrl, downloadId)

        // 启动进度监控
        startProgressMonitoring(context, isCompleted, downloadManager, downloadId, videoUrl, onProgressUpdate, onDownloadComplete)

        Log.d("DailyTaskListScreen", "开始下载视频: $videoUrl, downloadId: $downloadId")
    } catch (e: Exception) {
        Log.e("DailyTaskListScreen", "下载视频失败", e)
        ToastUtils.showErrorToast(context, "下载失败: ${e.message}")
        onDownloadComplete(videoUrl)
    }
}

// 下载进度弹窗组件
@Composable
fun DownloadProgressDialog(
    progress: Int,
    onDismiss: () -> Unit,
    onBackgroundDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* 防止意外关闭 */ },
        title = {
            Text(
                text = "下载进度",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "正在下载视频...",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 进度条
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFE0E0E0)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 进度百分比
                Text(
                    text = "$progress%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(
                    text = "取消下载",
                    color = Color(0xFFEA3323)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onBackgroundDownload
            ) {
                Text(
                    text = "后台下载",
                    color = Color(0xFF666666)
                )
            }
        }
    )
}

// 监控下载进度
private fun startProgressMonitoring(
    context: Context,
    isCompleted: AtomicBoolean,
    downloadManager: DownloadManager,
    downloadId: Long,
    videoUrl: String,
    onProgressUpdate: (String, Int) -> Unit,
    onDownloadComplete: (String) -> Unit = { _ -> }
) {
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val progressRunnable = object : Runnable {
        override fun run() {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor = downloadManager.query(query)
            Log.d("DailyTaskListScreen", "Progress monitoring check")
            
            if (cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = cursor.getInt(statusIndex)
                Log.d("DailyTaskListScreen", "Download status in progress monitoring: $status")

                when (status) {
                    DownloadManager.STATUS_RUNNING -> {
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        
                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex)
                        
                        if (bytesTotal > 0) {
                            val progress = ((bytesDownloaded * 100) / bytesTotal).toInt()
                            onProgressUpdate(videoUrl, progress)
                            Log.d("DailyTaskListScreen", "Download progress: $progress%")
                        }
                        
                        // 继续监控
                        handler.postDelayed(this, 500)
                    }
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Log.d("DailyTaskListScreen", "Download successful in progress monitoring, calling onDownloadComplete")
                        if (isCompleted.compareAndSet(false, true)) {
                            // 设置进度为100%
                            onProgressUpdate(videoUrl, 100)
                            
                            // 确保在主线程中调用回调
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onDownloadComplete(videoUrl)
                            }
                            ToastUtils.showSuccessToast(context, "视频下载完成并已保存到相册")
                            Log.d("DailyTaskListScreen", "视频下载完成: $videoUrl")
                        }
                        // 停止监控
                        return
                    }
                    DownloadManager.STATUS_FAILED -> {
                        Log.d("DailyTaskListScreen", "Download failed in progress monitoring, calling onDownloadComplete")
                        if (isCompleted.compareAndSet(false, true)) {
                            // 设置进度为0%表示失败
                            onProgressUpdate(videoUrl, 0)
                            
                            // 确保在主线程中调用回调
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onDownloadComplete(videoUrl)
                            }
                            ToastUtils.showErrorToast(context, "视频下载失败")
                            Log.e("DailyTaskListScreen", "视频下载失败: $videoUrl")
                        }
                        // 停止监控
                        return
                    }
                    else -> {
                        Log.d("DailyTaskListScreen", "Download status: $status, continuing monitoring")
                        // 继续监控其他状态
                        handler.postDelayed(this, 500)
                    }
                }
            }
            cursor.close()
        }
    }

    // 开始监控
    handler.post(progressRunnable)
}