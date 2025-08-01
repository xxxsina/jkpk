# 健康派卡App - 快手广告SDK接入文档

## 概述

本文档描述了健康派卡App中快手广告SDK的接入实现，包括开屏广告的完整实现和其他广告类型的预留接口。

## 已实现功能

### 1. 开屏广告
- ✅ SDK初始化
- ✅ 开屏广告加载和展示
- ✅ 广告展示策略（时间间隔控制）
- ✅ 启动页面集成
- ✅ 错误处理和日志记录

### 2. 基础架构
- ✅ 广告管理器（KuaishouAdManager）
- ✅ 广告配置管理（AdConfig）
- ✅ 广告工具类（AdUtils）
- ✅ Application初始化

## 项目结构

```
app/src/main/java/uni/UNI4BC70F5/
├── ad/
│   ├── AdConfig.kt             # 广告配置
│   └── AdUtils.kt              # 广告工具类
├── ShenHuoBaoApplication.kt    # Application类
├── SplashActivity.kt           # 开屏页面
└── MainActivity.kt             # 主页面
```

## 配置说明

### 1. 广告位ID配置

在 `AdConfig.kt` 中配置您的广告位ID：

```kotlin
object AdUnitId {
    const val SPLASH = "YOUR_SPLASH_AD_UNIT_ID"        // 开屏广告位ID
    const val BANNER = "YOUR_BANNER_AD_UNIT_ID"         // Banner广告位ID
    const val REWARD_VIDEO = "YOUR_REWARD_AD_UNIT_ID"   // 激励视频广告位ID
    const val INTERSTITIAL = "YOUR_INTERSTITIAL_AD_UNIT_ID" // 插屏广告位ID
    // ...
}
```

### 2. APP ID配置

```kotlin
const val APP_ID = "YOUR_KUAISHOU_APP_ID"
```

### 3. 广告策略配置

```kotlin
object Strategy {
    const val SPLASH_AD_INTERVAL_HOURS = 24        // 开屏广告展示间隔（小时）
    const val SPLASH_TIMEOUT_MS = 5000L            // 开屏广告超时时间
    const val SPLASH_DISPLAY_TIME_MS = 3000L       // 开屏广告展示时间
    // ...
}
```

## 使用方法

### 1. 开屏广告

开屏广告已集成在 `SplashActivity` 中，应用启动时自动处理：

- 检查展示策略（时间间隔）
- 加载广告
- 展示广告
- 处理超时和错误情况
- 跳转到主页面

### 2. 其他广告类型（预留接口）

#### Banner广告
```kotlin
KuaishouAdManager.getInstance().loadBannerAd(context) { success, message ->
    // TODO: 处理加载结果
}
```

#### 激励视频广告
```kotlin
KuaishouAdManager.getInstance().loadRewardVideoAd(context) { success, message ->
    // TODO: 处理加载结果
}
```

#### 插屏广告
```kotlin
KuaishouAdManager.getInstance().loadInterstitialAd(context) { success, message ->
    // TODO: 处理加载结果
}
```

## 权限配置

已在 `AndroidManifest.xml` 中添加必要权限：

```xml
<!-- 网络权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- 快手广告SDK所需权限 -->
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<!-- 更多权限... -->
```

## 依赖配置

已在 `build.gradle` 中添加必要依赖：

```gradle
// 快手广告SDK依赖
implementation fileTree(include: ['*.jar','*.aar'], dir: '../ad/kuaishou')
implementation 'com.github.bumptech.glide:glide:4.15.1'
implementation 'androidx.multidex:multidex:2.0.1'
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'androidx.recyclerview:recyclerview:1.3.2'
```

## 接入步骤

### 1. 获取快手广告账号
1. 注册快手广告开发者账号
2. 创建应用并获取APP ID
3. 创建广告位并获取广告位ID

### 2. 配置项目
1. 将快手广告SDK的aar文件放入 `ad/kuaishou/` 目录
2. 在 `AdConfig.kt` 中配置APP ID和广告位ID
3. 根据需要调整广告策略配置

### 3. 测试
1. 在测试环境中验证广告加载和展示
2. 检查日志输出确认SDK初始化成功
3. 测试各种异常情况的处理

### 4. 上线
1. 将 `AdConfig.Kuaishou.Config.DEBUG_MODE` 设为 `false`
2. 确认所有广告位ID为正式环境ID
3. 进行最终测试

## 日志说明

所有广告相关操作都有详细日志输出，标签为：
- `KuaishouAdManager`: 广告SDK操作
- `SplashActivity`: 开屏页面操作
- `AdUtils`: 广告策略操作
- `ShenHuoBaoApplication`: 应用初始化

## 注意事项

1. **隐私合规**: 确保在用户同意隐私政策后再初始化广告SDK
2. **网络环境**: 广告加载需要网络连接，注意处理网络异常
3. **性能优化**: 广告加载是异步操作，不会阻塞主线程
4. **错误处理**: 所有广告操作都有完善的错误处理机制
5. **测试环境**: 开发阶段建议使用测试广告位ID

## 后续扩展

当需要接入其他广告类型时，可以：

1. 在 `KuaishouAdManager` 中实现对应的加载和展示方法
2. 在 `AdConfig` 中添加相应的配置
3. 在 `AdUtils` 中添加展示策略
4. 在需要的页面中调用相应的广告方法

## 技术支持

如有问题，请参考：
1. 快手广告SDK官方文档
2. 项目中的示例代码
3. 日志输出信息