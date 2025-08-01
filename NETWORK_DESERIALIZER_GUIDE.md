# 网络反序列化器使用指南

## 概述

本项目使用了一个通用的网络响应反序列化架构，避免为每个API接口都创建专用的反序列化器类。

## 架构设计

## 2. 核心组件

### 2.1 GenericResponseDeserializer
通用的响应反序列化器，能够处理不同类型的响应数据。

### 2.2 分布式配置系统
每个响应模型都包含自己的反序列化配置，通过伴生对象的 `getDeserializerConfig()` 方法提供：

**在各个模型文件中：**
```kotlin
// LoginModels.kt
data class LoginResponse(...) {
    companion object {
        fun getDeserializerConfig(): Pair<Class<*>, Class<*>> {
            return LoginResponse::class.java to LoginMessage::class.java
        }
    }
}

// RegisterResponse.kt
data class RegisterResponse(...) {
    companion object {
        fun getDeserializerConfig(): Pair<Class<*>, Class<*>> {
            return RegisterResponse::class.java to RegisterMessage::class.java
        }
    }
}
```

**在 NetworkUtils.kt 中动态收集配置：**
```kotlin
private val deserializerConfig = mapOf<Class<*>, Class<*>>(
    LoginResponse.getDeserializerConfig(),
    RegisterResponse.getDeserializerConfig()
)
```

### 2.3 动态注册机制
自动为所有配置的类型创建和注册反序列化器

## 3. 优势

1. **模块化配置**: 每个响应模型管理自己的反序列化配置，提高内聚性
2. **代码复用**: 避免为每个接口创建专用的反序列化器类
3. **分离关注点**: 配置信息与对应的数据模型放在一起，便于维护
4. **类型安全**: 编译时检查类型匹配
5. **扩展性强**: 添加新接口只需要在对应模型中添加配置方法
6. **自动化**: 反序列化器的创建和注册完全自动化
7. **易于理解**: 开发者可以在模型文件中直接看到反序列化配置

## 如何添加新的API接口

### 步骤1: 创建响应数据模型

```kotlin
// 例如：用户资料响应
data class UserProfileResponse(
    val code: Double,
    val message: Any, // 联合类型：成功时为UserProfileMessage对象，失败时为字符串
    val timestamp: Long,
    val datetime: String,
    val data: String
) {
    companion object {
        fun getDeserializerConfig(): Pair<Class<*>, Class<*>> {
            return UserProfileResponse::class.java to UserProfileMessage::class.java
        }
    }
    
    fun getSuccessMessage(): UserProfileMessage? {
        return if (message is UserProfileMessage) message else null
    }
    
    fun getErrorMessage(): String? {
        return when (message) {
            is String -> message
            is UserProfileMessage -> "获取用户资料成功"
            else -> null
        }
    }
    
    fun isSuccess(): Boolean {
        return code == 200.0 && message is UserProfileMessage
    }
}

@Serializable
data class UserProfileMessage(
    val user_id: Double,
    val username: String,
    val email: String,
    val avatar: String
)
```

### 步骤2: 在NetworkUtils.kt中添加配置

在 `deserializerConfig` 映射中添加新的配置调用：

```kotlin
private val deserializerConfig = mapOf<Class<*>, Class<*>>(
    LoginResponse.getDeserializerConfig(),
    RegisterResponse.getDeserializerConfig(),
    UserProfileResponse.getDeserializerConfig(), // 新添加的配置
    // 继续添加更多配置...
)
```

### 步骤3: 添加导入语句

在NetworkUtils.kt文件顶部添加新模型的导入：

```kotlin
import com.jiankangpaika.app.data.model.UserProfileResponse
import com.jiankangpaika.app.data.model.UserProfileMessage
```

### 步骤4: 使用新的API

```kotlin
// 在需要使用的地方
suspend fun getUserProfile(userId: String): UserProfileResponse? {
    val result = NetworkUtils.get("https://api.example.com/user/profile?id=$userId")
    return if (result.isSuccess) {
        NetworkUtils.parseResponse<UserProfileResponse>(result.data)
    } else {
        null
    }
}
```

## 完整示例

假设我们要添加一个订单查询接口：

### 1. 创建模型文件 `OrderResponse.kt`

```kotlin
package com.jiankangpaika.app.data.model

import kotlinx.serialization.Serializable

data class OrderResponse(
    val code: Double,
    val message: Any,
    val timestamp: Long,
    val datetime: String,
    val data: String
) {
    companion object {
        fun getDeserializerConfig(): Pair<Class<*>, Class<*>> {
            return OrderResponse::class.java to OrderMessage::class.java
        }
    }
    
    fun getSuccessMessage(): OrderMessage? {
        return if (message is OrderMessage) message else null
    }
    
    fun getErrorMessage(): String? {
        return when (message) {
            is String -> message
            is OrderMessage -> "订单查询成功"
            else -> null
        }
    }
    
    fun isSuccess(): Boolean {
        return code == 200.0 && message is OrderMessage
    }
}

@Serializable
data class OrderMessage(
    val order_id: String,
    val product_name: String,
    val price: Double,
    val status: String,
    val create_time: String
)
```

### 2. 更新NetworkUtils.kt

```kotlin
// 添加导入
import com.jiankangpaika.app.data.model.OrderResponse
import com.jiankangpaika.app.data.model.OrderMessage

// 更新配置映射
private val deserializerConfig = mapOf<Class<*>, Class<*>>(
    LoginResponse.getDeserializerConfig(),
    RegisterResponse.getDeserializerConfig(),
    OrderResponse.getDeserializerConfig() // 新添加
)
```

### 3. 使用新接口

```kotlin
suspend fun getOrderInfo(orderId: String): OrderResponse? {
    val result = NetworkUtils.get("https://api.example.com/order/$orderId")
    return if (result.isSuccess) {
        NetworkUtils.parseResponse<OrderResponse>(result.data)
    } else {
        null
    }
}
```

## 注意事项

1. **响应模型规范**: 所有响应模型必须遵循统一的结构（code, message, timestamp, datetime, data）
2. **消息类型**: 成功时的message类型必须使用`@Serializable`注解
3. **类型安全**: 确保在配置映射中使用正确的类型对应关系
4. **导入语句**: 不要忘记在NetworkUtils.kt中添加新模型的导入语句

## 故障排除

### 编译错误
- 检查是否添加了正确的导入语句
- 确认类名拼写正确
- 验证模型类是否正确定义

### 运行时错误
- 检查JSON结构是否与模型匹配
- 确认`@Serializable`注解是否正确添加
- 验证类型映射关系是否正确

## 总结

这种架构设计使得添加新的API接口变得非常简单，只需要：
1. 创建响应模型
2. 在配置映射中添加一行
3. 添加导入语句

无需创建专用的反序列化器类，大大减少了代码重复和维护成本。