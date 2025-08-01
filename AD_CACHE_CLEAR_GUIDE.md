# 广告素材下载失败解决方案

## 问题描述

当广告素材下载失败时，可能出现以下错误：
- `ERR_TIMED_OUT` - 网络超时
- `SSL handshake failed` - SSL握手失败
- 其他网络连接相关错误

## 解决方案

### 1. 自动网络错误检测

系统已自动集成网络错误检测功能，当检测到网络相关错误时，会在日志中显示特殊标识：

```
🌐 [开屏广告] 检测到网络错误，建议清除缓存
```

### 2. 手动清除缓存

#### 2.1 清除所有平台缓存

```kotlin
// 获取统一广告管理器实例
val adManager = UnifiedAdManager.getInstance()

// 清除所有平台的缓存
val results = adManager.clearAllCache(context)

// 检查清理结果
results.forEach { (platform, success) ->
    Log.d(TAG, "平台 $platform 缓存清理${if (success) "成功" else "失败"}")
}
```

#### 2.2 清除指定平台缓存

```kotlin
// 清除穿山甲平台缓存
val success = adManager.clearCacheByPlatform(context, "穿山甲")
if (success) {
    Log.d(TAG, "穿山甲缓存清理成功")
} else {
    Log.e(TAG, "穿山甲缓存清理失败")
}
```

#### 2.3 直接调用穿山甲SDK清除缓存

```kotlin
// 获取穿山甲广告管理器实例
val chuanshanjiaManager = adManager.getAdManagerByPlatform("穿山甲") as? ChuanshanjiaAdManagerImpl

// 清除缓存
val success = chuanshanjiaManager?.clearCache(context) ?: false
```

### 3. 网络错误类型

系统会自动检测以下网络错误类型：

#### 3.1 错误关键词
- `ERR_TIMED_OUT`
- `SSL handshake failed`
- `timeout`
- `网络`
- `network`
- `connection`
- `连接`
- `超时`

#### 3.2 错误代码
- `40000` - 网络请求失败
- `40001` - 网络超时
- `40002` - 网络连接失败
- `40003` - DNS解析失败
- `40004` - SSL连接失败
- `40005` - 其他网络错误

### 4. 最佳实践

1. **监控日志**：定期检查日志中的网络错误标识
2. **自动重试**：检测到网络错误后，可以自动清除缓存并重试
3. **用户提示**：向用户提供清除缓存的选项
4. **定期清理**：建议定期清除广告缓存以避免累积问题

### 5. 故障排除步骤

1. **检查网络连接**：确保设备网络连接正常
2. **查看错误日志**：查找包含网络错误关键词的日志
3. **清除缓存**：使用上述方法清除SDK缓存
4. **重新加载广告**：清除缓存后重新尝试加载广告
5. **检查防火墙**：确保广告服务器地址未被防火墙阻止

### 6. 注意事项

- 清除缓存会删除已下载的广告素材，下次加载时需要重新下载
- 建议在网络状况良好时进行缓存清理
- 频繁清除缓存可能影响广告加载性能
- 清除缓存操作是异步的，建议在主线程中调用

## 代码示例

### 完整的错误处理和缓存清理示例

```kotlin
class AdErrorHandler {
    private val adManager = UnifiedAdManager.getInstance()
    
    fun handleAdLoadError(context: Context, errorMessage: String) {
        // 检查是否为网络错误
        if (isNetworkError(errorMessage)) {
            Log.w(TAG, "检测到网络错误，开始清除缓存")
            
            // 清除所有平台缓存
            val results = adManager.clearAllCache(context)
            
            // 等待一段时间后重试
            Handler(Looper.getMainLooper()).postDelayed({
                retryLoadAd(context)
            }, 2000)
        }
    }
    
    private fun isNetworkError(errorMessage: String): Boolean {
        val networkKeywords = listOf(
            "ERR_TIMED_OUT", "SSL handshake failed", 
            "timeout", "网络", "network", "connection"
        )
        return networkKeywords.any { 
            errorMessage.contains(it, ignoreCase = true) 
        }
    }
    
    private fun retryLoadAd(context: Context) {
        // 重新加载广告的逻辑
        adManager.loadSplashAd(context) { success, message ->
            if (success) {
                Log.i(TAG, "广告重新加载成功")
            } else {
                Log.e(TAG, "广告重新加载失败: $message")
            }
        }
    }
}
```