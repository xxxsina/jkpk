package com.jiankangpaika.app.ui.components

import android.content.Context
import android.util.Log
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import com.jiankangpaika.app.ad.AdUtils
import com.jiankangpaika.app.ad.BannerAdCloseManager
import com.jiankangpaika.app.ad.UnifiedAdManager

private const val TAG = "BannerAdCard"

/**
 * Banner广告卡片组件
 * 自动处理广告加载、展示和回调
 */
@Composable
fun BannerAdCard(
    modifier: Modifier = Modifier,
    onAdLoaded: (Boolean) -> Unit = {},
    onAdShown: (Boolean) -> Unit = {},
    onAdClosed: () -> Unit = {}
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var isLoaded by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var isClosed by remember { mutableStateOf(false) }
    var shouldShowAd by remember { mutableStateOf(false) }
    
    // 设置广告关闭回调管理器
    LaunchedEffect(Unit) {
        BannerAdCloseManager.setOnAdClosedCallback {
            isClosed = true
            onAdClosed()
            Log.i(TAG, "🔚 [BannerAdCard] Banner广告已关闭（通过管理器）")
        }
    }
    
    // 组件销毁时清除回调
    DisposableEffect(Unit) {
        onDispose {
            BannerAdCloseManager.clearOnAdClosedCallback()
            Log.d(TAG, "🧹 [BannerAdCard] 清除广告关闭回调")
        }
    }
    
    // 检查是否应该显示Banner广告
    LaunchedEffect(Unit) {
        shouldShowAd = AdUtils.shouldShowBannerAd(context)
        Log.d(TAG, "🔍 [BannerAdCard] 检查广告显示条件: shouldShowAd=$shouldShowAd")
        
        if (shouldShowAd) {
            // 开始加载广告
            isLoading = true
            loadBannerAd(context, {
                // 广告关闭回调
                isClosed = true
                onAdClosed()
                Log.i(TAG, "🔚 [BannerAdCard] Banner广告已关闭（加载时设置）")
            }) { success, message ->
                isLoading = false
                if (success) {
                    isLoaded = true
                    isError = false
                    onAdLoaded(true)
                    Log.i(TAG, "✅ [BannerAdCard] Banner广告加载成功")
                } else {
                    isLoaded = false
                    isError = true
                    onAdLoaded(false)
                    Log.e(TAG, "❌ [BannerAdCard] Banner广告加载失败: $message")
                }
            }
        } else {
            isLoading = false
            Log.d(TAG, "🚫 [BannerAdCard] 不满足广告显示条件，隐藏广告")
        }
    }
    
    // 根据状态渲染UI
    if (shouldShowAd && !isClosed) {
        when {
            isLoading -> {
                LoadingBannerAdCard(modifier = modifier)
            }
            isLoaded -> {
                BannerAdView(
                    modifier = modifier,
                    context = context,
                    onAdShown = { success, message ->
                        if (success) {
                            // 记录广告展示
                            AdUtils.recordBannerAdShown(context)
                            onAdShown(true)
                            Log.i(TAG, "🎉 [BannerAdCard] Banner广告展示成功")
                        } else {
                            onAdShown(false)
                            Log.e(TAG, "❌ [BannerAdCard] Banner广告展示失败: $message")
                        }
                    },
                    onAdClosed = {
                        isClosed = true
                        onAdClosed()
                        Log.i(TAG, "🔚 [BannerAdCard] Banner广告已关闭")
                    }
                )
            }
            isError -> {
                // 错误状态下不显示任何内容，静默失败
                Log.w(TAG, "⚠️ [BannerAdCard] 广告错误状态，不显示内容")
            }
        }
    } else {
        // 隐藏状态下不显示任何内容
        if (isClosed) {
            Log.d(TAG, "🔚 [BannerAdCard] 广告已关闭，不显示内容")
        } else {
            Log.d(TAG, "🙈 [BannerAdCard] 广告隐藏状态，不显示内容")
        }
    }
}

/**
 * 加载中的Banner广告卡片
 */
@Composable
private fun LoadingBannerAdCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth(),
//            .height(100.dp), // Banner广告通常高度较小
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
 * Banner广告视图
 */
@Composable
private fun BannerAdView(
    modifier: Modifier = Modifier,
    context: Context,
    onAdShown: (Boolean, String?) -> Unit,
    onAdClosed: () -> Unit
) {
    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
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
            // 需要获取Activity实例来调用getBannerAdView
            if (context is android.app.Activity) {
                adManager.getBannerAdView(context, onAdClosed) { adView, message ->
                    if (adView != null) {
                        try {
                            // 清空容器
                            container.removeAllViews()
                            // 如果广告视图已有父视图，先移除
                            (adView.parent as? ViewGroup)?.removeView(adView)
                            // 添加广告视图到容器
                            container.addView(adView)
                            Log.d(TAG, "📱 [BannerAdView] Banner广告视图已添加到容器$adView")
                            onAdShown(true, null)
                        } catch (e: Exception) {
                            Log.e(TAG, "💥 [BannerAdView] 添加广告视图异常: ${e.message}", e)
                            onAdShown(false, e.message)
                        }
                    } else {
                        Log.w(TAG, "⚠️ [BannerAdView] Banner广告视图为空: $message")
                        onAdShown(false, message)
                    }
                }
            } else {
                Log.e(TAG, "❌ [BannerAdView] Context不是Activity实例，无法展示广告")
                onAdShown(false, "Context不是Activity实例")
            }
        }
    )
}

/**
 * 加载Banner广告
 */
private fun loadBannerAd(context: Context, onAdClosed: () -> Unit, callback: (Boolean, String?) -> Unit) {
    val adManager = UnifiedAdManager.getInstance()
    adManager.loadBannerAd(context, callback)
}