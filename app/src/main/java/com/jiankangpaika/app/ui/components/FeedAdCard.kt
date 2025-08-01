package com.jiankangpaika.app.ui.components

import android.content.Context
import android.content.res.Resources
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ad.UnifiedAdManager

private const val TAG = "FeedAdCard"

/**
 * 信息流广告卡片组件
 * 自动处理广告加载、展示和回调
 */
@Composable
fun FeedAdCard(
    modifier: Modifier = Modifier,
    onAdLoaded: (Boolean) -> Unit = {},
    onAdShown: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var isLoaded by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var shouldShowAd by remember { mutableStateOf(false) }
    
    // 检查是否应该显示信息流广告
    LaunchedEffect(Unit) {
        shouldShowAd = AdUtils.shouldShowFeedAd(context)
        Log.d(TAG, "🔍 [FeedAdCard] 检查广告显示条件: shouldShowAd=$shouldShowAd")
        
        if (shouldShowAd) {
            // 开始加载广告
            isLoading = true
            loadFeedAd(context) { success, message ->
                isLoading = false
                if (success) {
                    isLoaded = true
                    isError = false
                    onAdLoaded(true)
                    Log.i(TAG, "✅ [FeedAdCard] 信息流广告加载成功")
                } else {
                    isLoaded = false
                    isError = true
                    onAdLoaded(false)
                    Log.e(TAG, "❌ [FeedAdCard] 信息流广告加载失败: $message")
                }
            }
        } else {
            isLoading = false
            Log.d(TAG, "🚫 [FeedAdCard] 不满足广告显示条件，隐藏广告")
        }
    }
    
    // 根据状态渲染UI
    if (shouldShowAd) {
        when {
            isLoading -> {
                LoadingAdCard(modifier = modifier)
            }
            isLoaded -> {
                FeedAdView(
                    modifier = modifier,
                    context = context,
                    onAdShown = { success, message ->
                        if (success) {
                            // 记录广告展示
                            AdUtils.recordFeedAdShown(context)
                            onAdShown(true)
                            Log.i(TAG, "🎉 [FeedAdCard] 信息流广告展示成功")
                        } else {
                            onAdShown(false)
                            Log.e(TAG, "❌ [FeedAdCard] 信息流广告展示失败: $message")
                        }
                    }
                )
            }
            isError -> {
                // 错误状态下不显示任何内容，静默失败
                Log.w(TAG, "⚠️ [FeedAdCard] 广告错误状态，不显示内容")
            }
        }
    } else {
        // 隐藏状态下不显示任何内容
        Log.d(TAG, "🙈 [FeedAdCard] 广告隐藏状态，不显示内容")
    }
}

/**
 * 加载中的广告卡片
 */
@Composable
private fun LoadingAdCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "信息加载中...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * 信息流广告视图
 */
@Composable
private fun FeedAdView(
    modifier: Modifier = Modifier,
    context: Context,
    onAdShown: (Boolean, String?) -> Unit
) {
    // 计算屏幕高度的三分之一
//    val screenHeight = Resources.getSystem().displayMetrics.heightPixels
//    val maxAdHeightPx = screenHeight / 3
//    val maxAdHeightDp = (maxAdHeightPx / Resources.getSystem().displayMetrics.density).dp
    
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
//            .heightIn(max = maxAdHeightDp), // 限制最大高度为屏幕的三分之一
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        },
        update = { container ->
            // 展示广告
            val adManager = UnifiedAdManager.getInstance()
            // 需要获取Activity实例来调用showFeedAd
            if (context is android.app.Activity) {
                adManager.getFeedAdView(context) { adView, message ->
                    if (adView != null) {
                        try {
                            // 清空容器
                            container.removeAllViews()
                            // 如果广告视图已有父视图，先移除
                            (adView.parent as? ViewGroup)?.removeView(adView)
                            
                            // 设置广告视图的最大高度
//                            adView.layoutParams = ViewGroup.LayoutParams(
//                                ViewGroup.LayoutParams.MATCH_PARENT,
//                                minOf(adView.layoutParams?.height ?: maxAdHeightPx, maxAdHeightPx)
//                            )
                            
                            // 添加广告视图到容器
                            container.addView(adView)
//                            Log.d(TAG, "📱 [FeedAdView] 信息流广告视图已添加到容器，最大高度限制为: ${maxAdHeightDp}")
                            onAdShown(true, null)
                        } catch (e: Exception) {
                            Log.e(TAG, "💥 [FeedAdView] 添加广告视图异常: ${e.message}", e)
                            onAdShown(false, e.message)
                        }
                    } else {
                        Log.w(TAG, "⚠️ [FeedAdView] 信息流广告视图为空: $message")
                        onAdShown(false, message)
                    }
                }
            } else {
                Log.e(TAG, "❌ [FeedAdView] Context不是Activity实例，无法展示广告")
                onAdShown(false, "Context不是Activity实例")
            }
        }
    )
}

/**
 * 加载信息流广告
 */
private fun loadFeedAd(context: Context, callback: (Boolean, String?) -> Unit) {
    val adManager = UnifiedAdManager.getInstance()
    adManager.loadFeedAd(context, callback)
}