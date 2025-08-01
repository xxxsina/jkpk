# 版本更新 PHP API 接口文档

## 概述

本文档描述了Android应用版本更新的PHP服务端接口实现，提供版本检查、更新信息获取等功能。

## 文件说明

- **service-php/version_api_server.php**: 主要的PHP接口文件
- **VersionInfo.kt**: Android端数据模型
- **version_api_example.json**: API响应示例

## 数据结构

### 版本信息 (VersionInfo)

```json
{
  "versionCode": 2,
  "versionName": "1.1.0",
  "updateMessage": "更新说明",
  "downloadUrl": "http://shop.blcwg.com/downloads/app.apk",
  "forceUpdate": false,
  "fileSize": "25.6MB",
  "updateTime": "2024-01-15 10:30:00"
}
```

### API响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": VersionInfo,
  "timestamp": 1705123456
}
```

## API接口

### 1. 版本检查接口

**接口地址**: `GET /service-php/version_api_server.php?action=check&versionCode={currentVersion}`

**请求参数**:
- `action`: 操作类型，固定值 `check`
- `versionCode`: 当前应用版本号（整数）

**响应示例**:

#### 有新版本时
```json
{
  "code": 200,
  "message": "发现新版本",
  "data": {
    "versionCode": 2,
    "versionName": "1.1.0",
    "updateMessage": "新版本更新内容：\n\n🎉 新功能：\n• 新增运动数据统计功能\n• 优化签到体验\n• 新增广告奖励机制\n\n🔧 优化改进：\n• 修复已知问题\n• 提升应用性能\n• 优化用户界面",
    "downloadUrl": "http://shop.blcwg.com/downloads/shenhuobao_v1.1.0.apk",
    "forceUpdate": false,
    "fileSize": "25.6MB",
    "updateTime": "2024-01-15 10:30:00"
  },
  "timestamp": 1705123456
}
```

#### 已是最新版本时
```json
{
  "code": 200,
  "message": "当前已是最新版本",
  "data": null,
  "timestamp": 1705123456
}
```

### 2. 版本历史接口

**接口地址**: `GET /service-php/version_api_server.php?action=history`

**响应示例**:
```json
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "versionCode": 2,
      "versionName": "1.1.0",
      "releaseDate": "2024-01-15",
      "updateMessage": "新增运动数据统计功能，优化签到体验"
    },
    {
      "versionCode": 1,
      "versionName": "1.0.0",
      "releaseDate": "2024-01-01",
      "updateMessage": "首个正式版本发布"
    }
  ],
  "timestamp": 1705123456
}
```

## 部署说明

### 1. 服务器要求

- PHP 7.0 或更高版本
- 支持 JSON 扩展
- 支持 PDO 扩展（如果使用数据库）

### 2. 部署步骤

1. **上传文件**
   ```bash
   # 将 version_api_server.php 上传到服务器的service-php目录
   scp service-php/version_api_server.php user@server:/var/www/html/service-php/
   ```

2. **设置权限**
   ```bash
   chmod 644 service-php/version_api_server.php
   ```

3. **配置Web服务器**
   
   **Apache (.htaccess)**:
   ```apache
   RewriteEngine On
   RewriteRule ^version/?$ service-php/version_api_server.php [L]
   ```
   
   **Nginx**:
   ```nginx
   location /version {
       try_files $uri $uri/ /service-php/version_api_server.php?$query_string;
   }
   ```

### 3. 数据库配置（可选）

如果使用数据库存储版本信息，需要创建以下表：

```sql
-- 应用版本表
CREATE TABLE `app_versions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version_code` int(11) NOT NULL COMMENT '版本号',
  `version_name` varchar(50) NOT NULL COMMENT '版本名称',
  `update_message` text COMMENT '更新说明',
  `download_url` varchar(500) NOT NULL COMMENT '下载地址',
  `force_update` tinyint(1) DEFAULT '0' COMMENT '是否强制更新',
  `file_size` varchar(20) DEFAULT NULL COMMENT '文件大小',
  `status` enum('active','inactive') DEFAULT 'active' COMMENT '状态',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `version_code` (`version_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 版本检查日志表
CREATE TABLE `version_check_logs` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `ip` varchar(45) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` text COMMENT '用户代理',
  `current_version` int(11) DEFAULT NULL COMMENT '当前版本',
  `latest_version` int(11) DEFAULT NULL COMMENT '最新版本',
  `check_time` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '检查时间',
  PRIMARY KEY (`id`),
  KEY `check_time` (`check_time`),
  KEY `ip` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

## 使用示例

### 1. 基本版本检查

```bash
# 检查版本更新
curl "http://shop.blcwg.com/service-php/version_api_server.php?action=check&versionCode=1"
```

### 2. 获取版本历史

```bash
# 获取版本历史
curl "http://shop.blcwg.com/service-php/version_api_server.php?action=history"
```

### 3. Android端调用示例

```kotlin
// 在 VersionUpdateManager.kt 中
private suspend fun checkForUpdate(): VersionCheckResponse? {
    return try {
        val currentVersionCode = getCurrentVersionCode()
        val response = apiService.checkVersion(currentVersionCode)
        response
    } catch (e: Exception) {
        Log.e(TAG, "版本检查失败", e)
        null
    }
}
```

## 配置说明

### 1. 版本信息配置

在 `VersionConfig` 类中修改版本信息：

```php
class VersionConfig {
    const LATEST_VERSION = [
        'versionCode' => 3,  // 新版本号
        'versionName' => '1.2.0',  // 新版本名称
        'updateMessage' => '更新内容...',
        'downloadUrl' => 'http://shop.blcwg.com/downloads/app_v1.2.0.apk',
        'forceUpdate' => false,
        'fileSize' => '26.8MB',
        'updateTime' => '2024-02-01 14:30:00'
    ];
    
    // 强制更新的最低版本号
    const MIN_FORCE_UPDATE_VERSION = 1;
}
```

### 2. 强制更新策略

- 当客户端版本号 ≤ `MIN_FORCE_UPDATE_VERSION` 时，会强制更新
- 强制更新时会在更新说明前添加警告信息

### 3. 日志记录

接口会记录每次版本检查的信息，包括：
- 客户端IP地址
- User-Agent信息
- 当前版本和最新版本
- 检查时间

## 错误处理

### 常见错误码

- `200`: 成功
- `400`: 请求参数错误
- `500`: 服务器内部错误

### 错误响应示例

```json
{
  "code": 400,
  "message": "不支持的操作类型",
  "data": null,
  "timestamp": 1705123456
}
```

## 安全建议

1. **HTTPS**: 生产环境建议使用HTTPS协议
2. **访问限制**: 可以添加IP白名单或访问频率限制
3. **输入验证**: 对所有输入参数进行验证
4. **日志监控**: 监控异常访问和错误日志

## 扩展功能

### 1. 渠道管理

可以根据不同渠道提供不同的更新包：

```php
$channel = $_GET['channel'] ?? 'default';
$versionInfo = getVersionByChannel($channel);
```

### 2. 灰度发布

可以根据用户ID或设备ID进行灰度发布：

```php
$userId = $_GET['userId'] ?? '';
if (isInGrayList($userId)) {
    // 返回灰度版本
}
```

### 3. 统计分析

记录更多统计信息用于分析：

```php
$stats = [
    'device_model' => $_GET['deviceModel'] ?? '',
    'os_version' => $_GET['osVersion'] ?? '',
    'app_channel' => $_GET['channel'] ?? ''
];
```

## 维护说明

1. **定期备份**: 定期备份版本信息和日志数据
2. **性能监控**: 监控接口响应时间和成功率
3. **版本管理**: 建立版本发布流程和回滚机制
4. **文档更新**: 及时更新API文档和使用说明