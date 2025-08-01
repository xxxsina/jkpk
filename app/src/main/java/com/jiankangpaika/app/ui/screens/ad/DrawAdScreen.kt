package com.jiankangpaika.app.ui.screens.ad

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jiankangpaika.app.ad.AdManager

/**
 * Draw广告展示页面
 * 提供Draw广告的加载和展示功能
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawAdScreen(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var adView by remember { mutableStateOf<ViewGroup?>(null) }
    var isAdLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 顶部标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← 返回")
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Draw广告测试",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // 控制按钮区域
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Draw广告控制面板",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // 加载按钮
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        adView = null
                        isAdLoaded = false
                        
                        adManager.loadDrawAd(context) { success, error ->
                            isLoading = false
                            if (success) {
                                isAdLoaded = true
                                // 加载成功后自动获取视图
                                adManager.getDrawAdView(context) { view, viewError ->
                                    if (view != null) {
                                        adView = view
                                        errorMessage = null
                                    } else {
                                        errorMessage = viewError ?: "获取广告视图失败"
                                    }
                                }
                            } else {
                                errorMessage = error ?: "加载Draw广告失败"
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("加载中...")
                    } else {
                        Text("加载Draw广告")
                    }
                }

                // 状态信息
                Spacer(modifier = Modifier.height(8.dp))
                when {
                    isLoading -> {
                        Text(
                            text = "正在加载Draw广告...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    isAdLoaded && adView != null -> {
                        Text(
                            text = "✅ Draw广告加载成功",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    isAdLoaded && adView == null -> {
                        Text(
                            text = "⏳ 广告已加载，正在获取视图...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // 错误信息显示
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "❌ 错误信息",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Draw广告展示区域
        adView?.let { view ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(
                        text = "📱 Draw广告展示",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(8.dp)
                    )
                    
                    // Draw广告视图容器
                    AndroidView(
                        factory = { view },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(9f / 16f) // Draw广告通常是9:16的垂直比例
                    )
                }
            }
        } ?: run {
            // 空状态展示
            if (!isLoading && errorMessage == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "📺",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "点击上方按钮加载Draw广告",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Draw广告是垂直视频流广告\n类似抖音短视频形式",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 底部说明信息
        if (adView == null && !isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "💡 提示：Draw广告需要网络连接，首次加载可能需要较长时间",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Draw广告预览组件（用于其他页面嵌入）
 */
@Composable
fun DrawAdPreview(
    adManager: AdManager,
    modifier: Modifier = Modifier,
    onAdLoaded: (Boolean) -> Unit = {},
    onAdError: (String?) -> Unit = {}
) {
    var adView by remember { mutableStateOf<ViewGroup?>(null) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        adManager.loadDrawAd(context) { success, error ->
            if (success) {
                adManager.getDrawAdView(context) { view, viewError ->
                    if (view != null) {
                        adView = view
                        onAdLoaded(true)
                    } else {
                        onAdError(viewError)
                    }
                }
            } else {
                onAdError(error)
            }
        }
    }

    adView?.let { view ->
        AndroidView(
            factory = { view },
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(9f / 16f)
        )
    }
}