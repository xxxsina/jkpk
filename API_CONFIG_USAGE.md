# API配置统一管理使用指南

## 概述

为了便于统一管理所有API接口地址，项目中创建了 `ApiConfig.kt` 配置文件，集中管理所有接口地址。这样做的好处包括：

- 🎯 **统一管理**: 所有接口地址集中在一个文件中
- 🔄 **环境切换**: 轻松切换开发/测试/生产环境
- 🛠️ **维护便利**: 接口地址变更时只需修改一处
- 📝 **代码清晰**: 避免硬编码，提高代码可读性

## 文件结构

```
app/src/main/java/uni/UNI4BC70F5/utils/constants/
└── ApiConfig.kt    # API接口地址统一配置
```

## 配置文件说明

### 基础配置

```kotlin
object ApiConfig {
    // 服务器基础地址
    private const val BASE_URL = "http://shop.blcwg.com"
    
    // 各模块接口配置...
}
```

### 模块分类

接口按功能模块分类管理：

- **Version**: 版本更新相关
- **User**: 用户相关（登录、注册、信息管理）
- **Sport**: 运动数据相关
- **CheckIn**: 签到相关
- **Content**: 内容相关（文章、教程）
- **Feedback**: 反馈相关
- **System**: 系统相关（配置、公告）
- **Upload**: 文件上传相关

## 使用方法

### 1. 导入配置类

```kotlin
import com.jiankangpaika.app.utils.constants.ApiConfig
```

### 2. 使用接口地址

```kotlin
// 版本检查
val versionCheckUrl = ApiConfig.Version.CHECK_UPDATE

// 用户登录
val loginUrl = ApiConfig.User.LOGIN

// 运动数据上传
val uploadDataUrl = ApiConfig.Sport.UPLOAD_DATA

// 签到
val signInUrl = ApiConfig.CheckIn.SIGN_IN
```

### 3. 网络请求示例

```kotlin
// 使用NetworkUtils进行请求
val result = NetworkUtils.get(ApiConfig.Version.CHECK_UPDATE)

// 或者使用其他网络库
Retrofit.Builder()
    .baseUrl(ApiConfig.User.LOGIN)
    .build()
```

### 4. 动态URL构建

```kotlin
// 获取完整URL
val fullUrl = ApiConfig.getFullUrl("api/custom/endpoint")

// 验证URL有效性
if (ApiConfig.isValidApiUrl(url)) {
    // 执行请求
}
```

## 环境切换

### 开发环境
```kotlin
private const val BASE_URL = "http://dev.shop.blcwg.com"
```

### 测试环境
```kotlin
private const val BASE_URL = "http://test.shop.blcwg.com"
```

### 生产环境
```kotlin
private const val BASE_URL = "https://shop.blcwg.com"
```

## 最佳实践

### 1. 新增接口

当需要添加新的API接口时：

```kotlin
object ApiConfig {
    // 现有配置...
    
    /**
     * 新功能模块
     */
    object NewFeature {
        /**
         * 新接口描述
         */
        const val NEW_ENDPOINT = "$BASE_URL/api/new/endpoint"
    }
}
```

### 2. 接口文档注释

为每个接口添加清晰的注释：

```kotlin
/**
 * 用户相关接口
 */
object User {
    /**
     * 用户登录接口
     * POST /api/user/login
     * 参数: username, password
     */
    const val LOGIN = "$BASE_URL/api/user/login"
}
```

### 3. 版本管理

如果API有版本控制：

```kotlin
object ApiConfig {
    private const val BASE_URL = "http://shop.blcwg.com"
    private const val API_VERSION = "v1"
    
    object User {
        const val LOGIN = "$BASE_URL/api/$API_VERSION/user/login"
    }
}
```

### 4. 条件配置

根据构建类型使用不同配置：

```kotlin
object ApiConfig {
    private const val BASE_URL = if (BuildConfig.DEBUG) {
        "http://test.shop.blcwg.com"  // 测试环境
    } else {
        "https://shop.blcwg.com"      // 生产环境
    }
}
```

## 迁移指南

### 从硬编码迁移

**迁移前**:
```kotlin
val url = "http://shop.blcwg.com/version.php"
NetworkUtils.get(url)
```

**迁移后**:
```kotlin
val url = ApiConfig.Version.CHECK_UPDATE
NetworkUtils.get(url)
```

### 批量替换

1. 搜索项目中的硬编码URL
2. 在 `ApiConfig.kt` 中添加对应配置
3. 替换硬编码为配置引用
4. 测试确保功能正常

## 注意事项

1. **安全性**: 不要在配置文件中包含敏感信息（如API密钥）
2. **版本控制**: 配置文件应纳入版本控制
3. **团队协作**: 团队成员应统一使用配置文件中的地址
4. **测试**: 环境切换后要充分测试所有功能
5. **文档更新**: 接口变更时及时更新相关文档

## 相关文件

- `ApiConfig.kt`: API配置文件
- `NetworkUtils.kt`: 网络请求工具
- `VersionUpdateManager.kt`: 版本更新管理器（使用示例）
- `VERSION_UPDATE_README.md`: 版本更新功能说明

通过统一的API配置管理，项目的维护性和可扩展性得到了显著提升！