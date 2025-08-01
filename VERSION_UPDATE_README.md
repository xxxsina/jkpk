# 版本更新功能说明

## 功能概述

本项目已集成完整的版本更新功能，支持自动检查更新、手动检查更新、APK下载和安装等功能。

## 主要特性

### 🔄 自动版本检查
- 应用启动时自动检查版本更新（24小时检查一次）
- 静默检查，不打扰用户正常使用
- 支持版本忽略功能

### 📱 手动版本检查
- 在「个人设置」页面点击「检查更新」
- 显示详细的更新信息和版本对比
- 支持强制更新和可选更新

### 📥 APK下载安装
- 自动下载APK文件到应用私有目录
- 支持Android 7.0+的FileProvider安装方式
- 下载进度提示和错误处理

### 🛡️ 安全特性
- 使用HTTPS请求（如果服务器支持）
- 文件完整性验证
- 权限安全检查

## 服务器API接口

### 请求地址
```
GET http://shop.blcwg.com/version.php
```

**注意**: 接口地址已统一管理在 `ApiConfig.kt` 中，使用 `ApiConfig.Version.CHECK_UPDATE` 获取。

### 响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "versionCode": 2,
    "versionName": "1.1.0",
    "updateMessage": "更新内容描述",
    "downloadUrl": "http://shop.blcwg.com/downloads/app.apk",
    "forceUpdate": false,
    "fileSize": "25.6MB",
    "updateTime": "2024-01-15 10:30:00"
  }
}
```

### 字段说明
- `code`: 响应状态码，200表示成功
- `message`: 响应消息
- `data.versionCode`: 版本号（整数），用于版本比较
- `data.versionName`: 版本名称（字符串），显示给用户
- `data.updateMessage`: 更新内容描述，支持换行符
- `data.downloadUrl`: APK下载地址
- `data.forceUpdate`: 是否强制更新（true/false）
- `data.fileSize`: 文件大小（可选）
- `data.updateTime`: 更新时间（可选）

## 使用方式

### 1. 自动检查（已集成）
应用启动时会自动检查版本更新，无需额外配置。

### 2. 手动检查
在「个人设置」页面点击「检查更新」按钮。

### 3. 程序化调用
```kotlin
val versionUpdateManager = VersionUpdateManager(context)

// 静默检查
versionUpdateManager.checkForUpdate(showNoUpdateDialog = false) { hasUpdate, versionInfo ->
    if (hasUpdate) {
        // 发现新版本
    }
}

// 显示结果检查
versionUpdateManager.checkForUpdate(showNoUpdateDialog = true)
```

## 配置说明

### 检查间隔
默认24小时检查一次，可在 `VersionUpdateManager.kt` 中修改：
```kotlin
private const val CHECK_INTERVAL = 24 * 60 * 60 * 1000L // 毫秒
```

### 服务器地址
接口地址统一在 `ApiConfig.kt` 中管理：
```kotlin
// 在 ApiConfig.kt 中修改
object Version {
    const val CHECK_UPDATE = "$BASE_URL/version.php"
}
```

如需修改基础服务器地址，请修改 `ApiConfig.kt` 中的 `BASE_URL` 常量。

### 下载目录
 APK文件下载到应用私有目录的downloads文件夹：
```kotlin
private const val DOWNLOAD_DIR = "downloads"
```

## 权限要求

应用已配置以下必要权限：
- `INTERNET`: 网络访问
- `WRITE_EXTERNAL_STORAGE`: 文件写入
- `REQUEST_INSTALL_PACKAGES`: 安装APK

## 文件结构

```
app/src/main/java/uni/UNI4BC70F5/
├── data/model/
│   ├── VersionInfo.kt              # 版本信息数据模型
│   └── VersionCheckResponse.kt     # API响应数据模型
├── utils/
│   ├── VersionUpdateManager.kt     # 版本更新管理器
│   ├── NetworkUtils.kt             # 网络请求工具
│   └── constants/
│       └── ApiConfig.kt            # API接口地址统一配置
├── MainActivity.kt                 # 主活动（集成自动检查）
└── ui/screens/profile/
    └── PersonalSettingsScreen.kt   # 设置页面（手动检查）
```

## 测试建议

1. **版本号测试**：修改 `app/build.gradle` 中的 `versionCode` 为较小值进行测试
2. **网络测试**：确保设备能访问服务器地址
3. **权限测试**：在不同Android版本上测试安装权限
4. **强制更新测试**：设置 `forceUpdate: true` 测试强制更新流程

## 注意事项

1. **接口地址管理**：所有API接口地址统一在 `ApiConfig.kt` 中管理，便于维护和环境切换
2. **HTTPS支持**：建议服务器支持HTTPS以提高安全性
3. **文件大小**：大文件下载时注意网络超时设置
4. **存储空间**：下载前检查设备存储空间
5. **权限引导**：首次安装时可能需要引导用户开启「未知来源」权限
6. **测试环境**：建议在测试环境中充分验证更新流程
7. **环境切换**：可通过修改 `ApiConfig.kt` 中的 `BASE_URL` 快速切换开发/测试/生产环境

## 日志标签

版本更新相关日志使用以下标签：
- `VersionUpdateManager`: 版本更新管理器
- `NetworkUtils`: 网络请求工具
- `MainActivity`: 主活动相关日志

可通过以下命令查看相关日志：
```bash
adb logcat -s VersionUpdateManager NetworkUtils MainActivity
```