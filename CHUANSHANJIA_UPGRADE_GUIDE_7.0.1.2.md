# 穿山甲广告SDK升级指南：6.9.1.5 → 7.0.1.2

## 升级概述

本文档记录了将穿山甲广告SDK从6.9.1.5版本升级到7.0.1.2版本的详细步骤和注意事项。

### 主要变化

1. **初始化方式变更**：从单步初始化改为两步初始化（init + start）
2. **SDK版本更新**：open_ad_sdk_6.9.1.5.aar → open_ad_sdk_7.0.1.2.aar
3. **API兼容性**：大部分API保持兼容，主要是初始化流程的变化

## 升级步骤

### 1. 更新依赖文件

#### 1.1 复制新版本AAR文件
```bash
# 复制穿山甲7.0.1.2版本的AAR文件到项目libs目录
cp ./ad/chuanshangjia7.0.1.2/open_ad_sdk_7.0.1.2.aar ./app/libs/
cp ./ad/chuanshangjia7.0.1.2/tools-release.aar ./app/libs/
```

#### 1.2 更新build.gradle依赖

在 `app/build.gradle` 中更新依赖：

```gradle
// 升级到穿山甲7.0.1.2版本
implementation files('libs/open_ad_sdk_7.0.1.2.aar')
implementation files('libs/tools-release.aar')
```

### 2. 代码适配

#### 2.1 初始化方式变更

**6.9.1.5版本（旧版本）：**
```kotlin
// 旧版本：直接初始化并启动
TTAdSdk.init(context, config)
TTAdSdk.start(callback)
```

**7.0.1.2版本（新版本）：**
```kotlin
// 新版本：先初始化，再启动
TTAdSdk.init(context, config)

TTAdSdk.start(object : TTAdSdk.Callback {
    override fun success() {
        Log.d(TAG, "SDK启动成功")
        Log.d(TAG, "SDK就绪状态: ${TTAdSdk.isSdkReady()}")
        // 创建TTAdNative等后续操作
    }
    
    override fun fail(code: Int, msg: String?) {
        Log.e(TAG, "SDK启动失败: code=$code, msg=$msg")
    }
})
```

#### 2.2 SDK就绪状态检查

新版本提供了 `TTAdSdk.isSdkReady()` 方法来检查SDK是否就绪：

```kotlin
if (TTAdSdk.isSdkReady()) {
    // SDK已就绪，可以进行广告操作
} else {
    // SDK未就绪，需要等待或重新初始化
}
```

### 3. 配置文件更新

#### 3.1 TTAdConfig配置保持不变

```kotlin
val config = TTAdConfig.Builder()
    .appId(APP_ID)
    .appName(APP_NAME)
    .debug(false) // 上线前需要关闭
    .useMediation(true) // 启用融合功能
    .supportMultiProcess(false) // 单进程应用
    .allowShowNotify(true) // 允许SDK弹出通知
    .directDownloadNetworkType(
        TTAdConstant.NETWORK_STATE_WIFI, 
        TTAdConstant.NETWORK_STATE_3G, 
        TTAdConstant.NETWORK_STATE_4G, 
        TTAdConstant.NETWORK_STATE_5G
    )
    .build()
```

#### 3.2 广告位配置无需变更

现有的广告位ID配置保持不变，继续使用动态配置系统。

### 4. 测试验证

#### 4.1 初始化测试

1. 验证SDK初始化是否成功
2. 检查 `TTAdSdk.isSdkReady()` 返回值
3. 确认TTAdNative对象创建成功

#### 4.2 广告加载测试

测试各种广告类型的加载和展示：
- 开屏广告
- 信息流广告
- 激励视频广告
- 插屏广告
- Banner广告
- Draw广告

#### 4.3 错误处理测试

验证网络错误、配置错误等异常情况的处理。

### 5. 注意事项

#### 5.1 兼容性

- 7.0.1.2版本与6.9.1.5版本在API层面基本兼容
- 主要变化在初始化流程，其他广告加载和展示API保持不变

#### 5.2 性能优化

- 新版本在启动速度和内存使用方面有所优化
- 建议在应用启动时尽早进行SDK初始化

#### 5.3 调试建议

- 升级后建议开启debug模式进行充分测试
- 关注日志输出，确保没有新的错误或警告
- 测试不同网络环境下的广告加载情况

### 6. 回滚方案

如果升级后出现问题，可以快速回滚到6.9.1.5版本：

1. 恢复build.gradle中的依赖配置
2. 恢复ChuanshanjiaAdManagerImpl.kt中的初始化代码
3. 重新编译和测试

### 7. 升级检查清单

- [ ] 复制新版本AAR文件到libs目录
- [ ] 更新build.gradle依赖配置
- [ ] 修改初始化代码为两步初始化
- [ ] 添加SDK就绪状态检查
- [ ] 测试所有广告类型的加载和展示
- [ ] 验证错误处理逻辑
- [ ] 性能测试和内存泄漏检查
- [ ] 上线前关闭debug模式

## 技术支持

如果在升级过程中遇到问题，可以参考：

1. 穿山甲官方文档：https://www.csjplatform.com/
2. 项目内相关文档：
   - `AD_CONFIG_OPTIMIZATION_GUIDE.md`
   - `AD_SLOT_CONFIG_GUIDE.md`
   - `DEBUG_TYPE_CAST_ISSUE.md`

## 更新日志

- 2025-01-27: 完成从6.9.1.5到7.0.1.2的升级
- 主要变更：初始化方式改为两步初始化，添加SDK就绪状态检查