# 类型转换错误调试指南

## 问题描述
`LoginMessage cannot be cast to java.lang.String` 类型转换错误

## 已实施的解决方案

### 1. 自定义反序列化器
在 `NetworkUtils.kt` 中实现了 `LoginResponseDeserializer`，专门处理 `message` 字段的联合类型解析。

### 2. 数据模型类型修改
- 将所有数字字段从 `Int` 改为 `Double`
- 修改了 `getErrorMessage()` 方法使用 `when` 表达式

### 3. 增强的日志记录
添加了详细的日志输出来追踪 JSON 解析过程：

```kotlin
Log.d("NetworkUtils", "Message element type: ${messageElement.javaClass.name}")
Log.d("NetworkUtils", "Message element content: $messageElement")
```

## 调试步骤

### 1. 查看日志输出
运行应用并尝试登录，在 Android Studio 的 Logcat 中查找以下标签的日志：
- `NetworkUtils`: JSON 解析过程
- `LoginModels`: 类型判断过程

### 2. 关键日志信息
注意以下日志内容：
- `Message element type`: 显示 message 字段的实际类型
- `Message element content`: 显示 message 字段的内容
- `Parsed LoginMessage`: 成功解析的 LoginMessage 对象
- `Parsed String message`: 成功解析的字符串消息
- `Actual type`: 在 LoginModels 中显示的实际类型

### 3. 可能的问题原因

#### A. JSON 结构不匹配
如果服务器返回的 JSON 结构与预期不符，可能导致解析失败。

#### B. Gson 配置问题
自定义反序列化器可能没有正确注册或执行。

#### C. 类型判断逻辑错误
在 `getSuccessMessage()` 或 `getErrorMessage()` 方法中的类型判断可能有问题。

## 进一步的解决方案

### 方案1: 完全重写 message 字段处理
```kotlin
fun getSuccessMessage(): LoginMessage? {
    return when (message) {
        is LoginMessage -> message
        is Map<*, *> -> {
            // 尝试从 Map 转换为 LoginMessage
            try {
                val gson = Gson()
                val json = gson.toJson(message)
                gson.fromJson(json, LoginMessage::class.java)
            } catch (e: Exception) {
                null
            }
        }
        else -> null
    }
}
```

### 方案2: 使用 sealed class
```kotlin
sealed class MessageContent {
    data class Success(val loginMessage: LoginMessage) : MessageContent()
    data class Error(val errorMessage: String) : MessageContent()
}
```

### 方案3: 分离成功和失败响应
创建不同的响应类型：
- `LoginSuccessResponse`
- `LoginErrorResponse`

## 测试建议

1. 使用真实的登录请求测试
2. 模拟不同的服务器响应（成功/失败）
3. 检查日志输出确认解析过程
4. 验证类型判断是否正确工作

## 联系信息
如果问题仍然存在，请提供：
1. 完整的错误日志
2. 服务器返回的 JSON 响应
3. 具体的复现步骤