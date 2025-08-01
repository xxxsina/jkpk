package com.jiankangpaika.app.ui.screens.sport

import android.util.Log
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.jiankangpaika.app.utils.HtmlUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jiankangpaika.app.ui.components.BannerAdCard
import com.jiankangpaika.app.ui.components.FeedAdCard

/**
 * 文章详细页面
 * 显示运动项目的详细介绍和指导内容
 * 
 * @param title 文章标题（运动项目名称）
 * @param category 运动分类
 * @param imageRes 图片资源ID
 * @param onBackClick 返回按钮点击事件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    title: String,
    category: String,
    imageRes: String,
    content: String,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFE2E8F0)
                    )
                )
            )
    ) {
        // 顶部应用栏
        CenterAlignedTopAppBar(
            title = {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.White
            )
        )
        
        // 滚动内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 信息流广告卡片
            // 在文章详情页面中间位置展示信息流广告，提升用户体验和商业价值
            FeedAdCard(
                modifier = Modifier.fillMaxWidth(),
                onAdLoaded = { success ->
                    // 广告加载完成回调
                    if (success) {
                        Log.i("ArticleDetailScreen", "📱 [信息流广告] 广告加载成功 - 文章: $title")
                    } else {
                        Log.w("ArticleDetailScreen", "📱 [信息流广告] 广告加载失败 - 文章: $title")
                    }
                },
                onAdShown = { success ->
                    // 广告展示完成回调
                    if (success) {
                        Log.i("ArticleDetailScreen", "🎯 [信息流广告] 广告展示成功 - 文章: $title, 分类: $category")
                        // 可以在这里添加广告展示统计逻辑
                    } else {
                        Log.e("ArticleDetailScreen", "🎯 [信息流广告] 广告展示失败 - 文章: $title")
                    }
                }
            )

            // 头部图片卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box {
                    AsyncImage(
                        model = imageRes,
                        contentDescription = title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    // 分类标签
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(
                                Color(0xFFED5548).copy(alpha = 0.9f), // 颜色
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Banner广告卡片
            // 在文章详情页面底部展示Banner广告，提升商业价值
            BannerAdCard(
                modifier = Modifier.fillMaxWidth()
                    .height(50.dp),
                onAdLoaded = { success ->
                    // 广告加载完成回调
                    if (success) {
                        Log.i("ArticleDetailScreen", "📱 [Banner广告] 广告加载成功 - 文章: $title")
                    } else {
                        Log.w("ArticleDetailScreen", "📱 [Banner广告] 广告加载失败 - 文章: $title")
                    }
                },
                onAdShown = { success ->
                    // 广告展示完成回调
                    if (success) {
                        Log.i("ArticleDetailScreen", "🎯 [Banner广告] 广告展示成功 - 文章: $title, 分类: $category")
                        // 可以在这里添加广告展示统计逻辑
                    } else {
                        Log.e("ArticleDetailScreen", "🎯 [Banner广告] 广告展示失败 - 文章: $title")
                    }
                }
            )
                        
            // 内容介绍卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 文章内容 - 支持HTML标签
                    Text(
                        text = HtmlUtils.htmlToAnnotatedString(content),
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = Color(0xFF475569)
                    )
                }
            }
            
            // 底部间距
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}