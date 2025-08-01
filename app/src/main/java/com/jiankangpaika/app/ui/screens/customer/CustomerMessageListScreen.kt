package com.jiankangpaika.app.ui.screens.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jiankangpaika.app.data.model.CustomerMessage
import com.jiankangpaika.app.ui.components.TopAppBarWithBack
import com.jiankangpaika.app.utils.ToastUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMessageListScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (CustomerMessage) -> Unit = {},
    viewModel: CustomerServiceViewModel = viewModel()
) {
    val context = LocalContext.current
    val messageListState by viewModel.messageListState.collectAsState()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    
    // 页面加载时获取消息列表
    LaunchedEffect(Unit) {
        viewModel.loadMessageList(context)
    }
    
    // 监听错误信息
    LaunchedEffect(messageListState.errorMessage) {
        messageListState.errorMessage?.let { message ->
            ToastUtils.showErrorToast(context, message)
            viewModel.clearMessageListError()
        }
    }
    
    // 下拉刷新处理
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.loadMessageList(context, 1)
        }
    }
    
    // 监听加载状态，完成刷新
    LaunchedEffect(messageListState.isLoading) {
        if (!messageListState.isLoading && pullToRefreshState.isRefreshing) {
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
                if (totalItems > 0 && lastVisibleItem >= totalItems - 3 && messageListState.canLoadMore()) {
                    viewModel.loadMoreMessages(context)
                }
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBarWithBack(
                title = "问题列表",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.loadMessageList(context, 1) },
                        enabled = !messageListState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            when {
                messageListState.isLoading -> {
                    // 加载中
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "加载中...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                messageListState.messages.isEmpty() -> {
                    // 空状态
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "暂无问题记录",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "您还没有提交过问题，可以返回提交新问题",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    // 消息列表
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(messageListState.messages) { message ->
                            MessageItem(
                                message = message,
                                onClick = { onNavigateToDetail(message) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // 加载更多指示器
                        if (messageListState.hasMoreData()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (messageListState.isLoading) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                            Text(
                                                text = "加载中...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "上拉加载更多",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else if (messageListState.messages.isNotEmpty()) {
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
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
fun MessageItem(
    message: CustomerMessage,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 第一行：问题内容（超过一行省略）
            Text(
                text = message.problem,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // 第二行：左边状态，右边时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusChip(status = message.status)
                    
                    // 问题解决状态
                    when (message.isOvercome) {
                        1 -> {
                            Surface(
                                color = Color(0xFFDCFCE7),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "已解决",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF16A34A),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        2 -> {
                            Surface(
                                color = Color(0xFFFEE2E2),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "未解决",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFDC2626),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // 如果有图片，显示图片图标
                    if (message.image.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "图片",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // 如果有视频，显示视频图标
                    if (message.video.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "视频",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = message.createTimeFormatted ?: formatTime(message.createTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (text, backgroundColor, textColor) = when (status) {
        "pending" -> Triple("待回复", Color(0xFFFEF3C7), Color(0xFFD97706))
        "processing" -> Triple("处理中", Color(0xFFDCFDF7), Color(0xFF059669))
        "answer" -> Triple("已回复", Color(0xFFDCFCE7), Color(0xFF16A34A))
        "closed" -> Triple("已关闭", Color(0xFFF3F4F6), Color(0xFF6B7280))
        "new" -> Triple("待回复", Color(0xFFFEF3C7), Color(0xFFD97706))
        else -> Triple("待回复", Color(0xFFFEF3C7), Color(0xFFD97706))
    }
    
    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(0.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * 格式化时间戳
 */
private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp * 1000) // 假设时间戳是秒级的
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return formatter.format(date)
}