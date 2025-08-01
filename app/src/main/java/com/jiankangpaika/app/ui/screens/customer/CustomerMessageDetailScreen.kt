package com.jiankangpaika.app.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jiankangpaika.app.data.model.CustomerMessage
import com.jiankangpaika.app.ui.components.TopAppBarWithBack
import com.jiankangpaika.app.ui.components.VideoPlayerDialog
import com.jiankangpaika.app.ui.components.VideoThumbnail
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.UserManager
import com.jiankangpaika.app.utils.ToastUtils
import com.jiankangpaika.app.utils.constants.ApiConfig
import android.util.Log
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMessageDetailScreen(
    message: CustomerMessage,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showImageDialog by remember { mutableStateOf(false) }
    var showVideoDialog by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    var selectedVideoUrl by remember { mutableStateOf("") }
    
    // 按钮状态管理
    var isLoading by remember { mutableStateOf(false) }
    
    // 消息状态管理 - 用于控制按钮显示
    var currentIsOvercome by remember { mutableIntStateOf(message.isOvercome) }
    
    // 图片缩放相关状态
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offset += offsetChange
    }
    
    // 处理消息状态修改
    fun modifyMessageStatus(isOvercome: Int) {
        val userId = UserManager.getUserId(context)
        val token = UserManager.getToken(context)
        
        if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
            Log.e("CustomerMessageDetail", "用户未登录")
            return
        }
        
        coroutineScope.launch {
            try {
                isLoading = true
                
                val requestData = mapOf(
                    "user_id" to userId,
                    "id" to message.id.toString(),
                    "is_overcome" to isOvercome.toString()
                )
                
                val result = NetworkUtils.postJsonWithAuth(
                    context = context,
                    url = ApiConfig.CustomerService.MODIFY_MESSAGE,
                    data = requestData
                )
                
                when {
                    result.isSuccess -> {
                        val statusText = if (isOvercome == 1) "已解决" else "未解决"
                        ToastUtils.showSuccessToast(context, "提交成功")
                        Log.d("CustomerMessageDetail", "状态修改成功: $statusText")
                        // 更新本地状态，立即隐藏按钮
                        currentIsOvercome = isOvercome
                    }
                    result.isError -> {
                        val errorMsg = when (result) {
                            is NetworkResult.Error -> result.message
                            is NetworkResult.Exception -> result.exception.message ?: "网络异常"
                            else -> "未知错误"
                        }
                        ToastUtils.showErrorToast(context, "修改失败: $errorMsg")
                        Log.e("CustomerMessageDetail", "状态修改失败: $errorMsg")
                    }
                }
            } catch (e: Exception) {
                ToastUtils.showErrorToast(context, "操作异常: ${e.message ?: "未知错误"}")
                Log.e("CustomerMessageDetail", "修改状态异常: ${e.message}", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        topBar = {
            TopAppBarWithBack(
                title = "提问详情",
                onNavigateBack = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 客服回复卡片（如果有回复）- 移到最上面
            if (message.answer.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "客服回复",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = message.answer,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = Color.Black
                        )
                        
                        // 回复图片
                        if (message.answerImage.isNotEmpty()) {
                            Text(
                                text = "回复图片",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(message.answerImage)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "回复图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedImageUrl = message.answerImage
                                        showImageDialog = true
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // 回复视频
                        if (message.answerVideo.isNotEmpty()) {
                                Text(
                                    text = "回复视频",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )

                                VideoThumbnail(
                                videoUrl = message.answerVideo,
                                onClick = {
                                    selectedVideoUrl = message.answerVideo
                                    showVideoDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    
                    // 状态修改按钮 - 只有当isOvercome==0时才显示
                    if (currentIsOvercome == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 已解决按钮
                            Button(
                                onClick = { modifyMessageStatus(1) },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("已解决")
                            }
                            
                            // 未解决按钮
                            Button(
                                onClick = { modifyMessageStatus(2) },
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF44336)
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text("未解决")
                            }
                        }
                    }
                }
            }
            }
            
            // 基本信息卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "基本信息",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    InfoRow(label = "姓名", value = message.realname)
                    InfoRow(label = "手机号", value = message.mobile)
                    InfoRow(label = "状态", value = getStatusText(message.status))
//                    InfoRow(label = "已读状态", value = if (message.looked == 1) "已读" else "未读")
                    InfoRow(
                        label = "提交时间", 
                        value = message.createTimeFormatted ?: formatTimestamp(message.createTime)
                    )
                    InfoRow(label = "问题解决", value = if (currentIsOvercome == 1) "已解决" else if (currentIsOvercome == 2) "未解决" else "待处理")
                    if (message.status == "answer") {
                        InfoRow(
                            label = "回复时间", 
                            value = message.updateTimeFormatted ?: formatTimestamp(message.updateTime)
                        )
                    }
                }
            }
            
            // 问题内容卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "问题描述",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = message.problem,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color.Black
                    )
                }
            }
            
            // 附件展示（图片和视频）
            if (message.image.isNotEmpty() || message.video.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "附件",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // 图片附件
                        if (message.image.isNotEmpty()) {
                            Text(
                                text = "图片",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(message.image)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "附件图片",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedImageUrl = message.image
                                        showImageDialog = true
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // 视频附件
                        if (message.video.isNotEmpty()) {
                            Text(
                                text = "视频",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            
                            VideoThumbnail(
                                videoUrl = message.video,
                                onClick = {
                                    selectedVideoUrl = message.video
                                    showVideoDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 图片全屏查看对话框
    if (showImageDialog) {
        Dialog(
            onDismissRequest = { 
                showImageDialog = false
                // 重置缩放状态
                scale = 1f
                offset = Offset.Zero
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { 
                        showImageDialog = false
                        // 重置缩放状态
                        scale = 1f
                        offset = Offset.Zero
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(selectedImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "全屏图片",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = transformableState),
                    contentScale = ContentScale.Fit
                )
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
    

}

@Composable
private fun InfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.weight(2f),
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getStatusText(status: String): String {
    return when (status) {
        "new" -> "待处理"
        "answer" -> "已回复"
        else -> status
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp * 1000) // 转换为毫秒
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(date)
}