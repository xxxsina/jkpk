# Kotlinx Serialization 使用指南

## 概述

本项目已集成 Kotlinx Serialization 来处理 JSON 序列化和反序列化，特别是解决接口返回数据字段多于 Kotlin 定义字段的问题。

## 配置说明

### 1. 项目配置

已在项目中添加了必要的配置：

**根目录 `build.gradle`：**
```gradle
plugins {
    id 'com.android.application' version '8.2.2' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.9.22' apply false
}
```

**app 模块 `build.gradle`：**
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.serialization'
}

dependencies {
    // Kotlinx Serialization
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0'
    // ... 其他依赖
}
```

### 2. 数据类注解

已为以下数据类添加了 `@Serializable` 注解：

- `LoginRequest` - 登录请求数据类
- `LoginMessage` - 登录消息数据类
- `UserInfo` - 用户信息数据类
- `UserData` - 用户数据类（向后兼容）
- `ApiResponse<T>` - API响应基础类
- `RegisterMessage` - 注册成功时的消息数据
- `RegisterUserInfo` - 注册用户信息

**注意：** `LoginResponse` 和 `RegisterResponse` 由于包含 `Any` 类型字段，暂时未添加 `@Serializable` 注解，继续使用 Gson 处理。

## 使用方式

### 1. 自动忽略未定义字段

Kotlinx Serialization 默认会忽略 JSON 中存在但 Kotlin 数据类中未定义的字段，这正好解决了接口返回字段过多的问题。

**示例：**
```kotlin
@Serializable
data class UserInfo(
    val user_id: Int,
    val username: String,
    val nickname: String,
    val phone: String,
    val email: String,
    val avatar: String,
    val status: Int,
    val created_at: String
    // 即使接口返回更多字段，也会被自动忽略
)
```

### 2. 可选字段处理

对于可能不存在的字段，使用可空类型：

```kotlin
@Serializable
data class RegisterUserInfo(
    val user_id: Int,
    val username: String,
    val phone: String?,      // 可空字段
    val email: String?,      // 可空字段
    val avatar: String?, // 可空字段
    val birthday: String?    // 可空字段
)
```

### 3. 字段名映射

如果需要将 JSON 字段名映射到不同的 Kotlin 属性名：

```kotlin
@Serializable
data class UserData(
    @SerialName("user_id")
    val userId: String,
    val username: String,
    // ...
)
```

### 4. 混合使用 Gson 和 Kotlinx Serialization

当前项目采用混合方案：
- 简单数据类使用 `@Serializable` 注解
- 包含复杂类型（如 `Any`）的响应类继续使用 Gson

## 优势

1. **自动忽略多余字段**：无需手动处理接口返回的额外字段
2. **类型安全**：编译时检查，减少运行时错误
3. **性能优化**：比 Gson 更高效的序列化性能
4. **代码简洁**：减少样板代码
5. **向后兼容**：可以与现有 Gson 代码共存

## 注意事项

1. **Any 类型限制**：包含 `Any` 类型字段的数据类暂时不能使用 `@Serializable`
2. **泛型支持**：复杂泛型类型可能需要额外配置
3. **自定义序列化器**：特殊需求可能需要实现自定义序列化器

## 迁移建议

1. 优先为新的数据类添加 `@Serializable` 注解
2. 逐步迁移现有简单数据类
3. 复杂响应类型可以继续使用 Gson
4. 测试确保序列化/反序列化行为正确

## 相关文件

- `LoginModels.kt` - 登录相关数据模型
- `RegisterResponse.kt` - 注册响应数据模型
- `NetworkUtils.kt` - 网络请求工具类（继续使用 Gson）