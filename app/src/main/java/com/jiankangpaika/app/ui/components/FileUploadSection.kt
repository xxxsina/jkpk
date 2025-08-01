package com.jiankangpaika.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * 文件上传区域组件
 */
@Composable
fun FileUploadSection(
    title: String,
    fileUris: List<Uri>,
    onAddFile: () -> Unit,
    onRemoveFile: (Uri) -> Unit,
    isEnabled: Boolean = true,
    maxFiles: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        // 标题
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF374151)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 文件列表
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 已选择的文件
            items(fileUris) { uri ->
                FileItem(
                    uri = uri,
                    onRemove = { onRemoveFile(uri) }
                )
            }
            
            // 添加按钮
            if (isEnabled && fileUris.size < maxFiles) {
                item {
                    AddFileButton(
                        onClick = onAddFile
                    )
                }
            }
        }
        
        // 提示信息
        if (isEnabled) {
            Text(
                text = "最多可上传${maxFiles}个文件",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Text(
                text = "该功能已关闭",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * 文件项组件
 */
@Composable
fun FileItem(
    uri: Uri,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFE5E7EB),
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        // 图片预览
        AsyncImage(
            model = uri,
            contentDescription = "上传的文件",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // 删除按钮
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 添加文件按钮组件
 */
@Composable
fun AddFileButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFE5E7EB),
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "添加文件",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "添加",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}