# 类型判断调试指南

## 问题描述
用户遇到 `message is LoginMessage` 判断为 `false` 的问题。

## 已完成的修改

### 1. 数据模型类型修改
已将以下字段从 `Int` 改为 `Double` 类型：

**LoginModels.kt:**
- `LoginResponse.code`: Int → Double
- `LoginMessage.user_id`: Int → Double  
- `UserInfo.user_id`: Int → Double
- `UserInfo.status`: Int → Double
- `ApiResponse.code`: Int → Double

**RegisterResponse.kt:**
- `RegisterResponse.code`: Int → Double
- `RegisterMessage.user_id`: Int → Double
- `RegisterUserInfo.user_id`: Int → Double
- `RegisterUserInfo.status`: Int → Double

### 2. 比较逻辑修改
- `LoginResponse.isSuccess()`: `code == 200` → `code == 200.0`
- `RegisterResponse.isSuccess()`: `code == 200` → `code == 200.0`
- `ApiResponse.isSuccess`: `code == 200` → `code == 200.0`

### 3. 使用时的类型转换
- `LoginScreen.kt`: `userInfo.user_id.toString()` → `userInfo.user_id.toInt().toString()`
- `RegisterScreen.kt`: `userInfo.user_id.toString()` → `userInfo.user_id.toInt().toString()`

## 调试步骤

### 1. 添加详细日志
在 `LoginResponse.getSuccessMessage()` 方法中添加更多调试信息：

```kotlin
fun getSuccessMessage(): LoginMessage? {
    Log.d("LoginModels", "=== 类型判断调试 ===")
    Log.d("LoginModels", "message 对象: $message")
    Log.d("LoginModels", "message 类型: ${message.javaClass.name}")
    Log.d("LoginModels", "message 是否为 LoginMessage: ${message is LoginMessage}")
    
    if (message is Map<*, *>) {
        Log.d("LoginModels", "message 是 Map 类型，内容: $message")
        // 尝试手动转换
        try {
            val gson = Gson()
            val json = gson.toJson(message)
            val loginMessage = gson.fromJson(json, LoginMessage::class.java)
            Log.d("LoginModels", "手动转换结果: $loginMessage")
            return loginMessage
        } catch (e: Exception) {
            Log.e("LoginModels", "手动转换失败: ${e.message}")
        }
    }
    
    return if (message is LoginMessage) message else null
}
```

### 2. 检查 JSON 解析过程
在 `NetworkUtils.parseJson` 方法中添加日志：

```kotlin
fun <T> parseJson(json: String, clazz: Class<T>): T? {
    return try {
        Log.d(TAG, "🔧 [JSON解析] 原始JSON: $json")
        val result = gson.fromJson(json, clazz)
        Log.d(TAG, "🔧 [JSON解析] 解析结果: $result")
        Log.d(TAG, "🔧 [JSON解析] 结果类型: ${result?.javaClass?.name}")
        result
    } catch (e: Exception) {
        Log.e(TAG, "🔧 [JSON解析] 解析失败: ${e.message}", e)
        null
    }
}
```

### 3. 可能的问题原因

1. **Gson 配置问题**: 可能需要自定义 TypeAdapter
2. **嵌套对象解析**: message 字段可能被解析为 Map 而不是 LoginMessage
3. **类加载问题**: 可能存在类加载器相关的问题

### 4. 解决方案建议

#### 方案A: 使用自定义反序列化器
```kotlin
class LoginResponseDeserializer : JsonDeserializer<LoginResponse> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LoginResponse {
        val jsonObject = json.asJsonObject
        val code = jsonObject.get("code").asDouble
        val timestamp = jsonObject.get("timestamp").asLong
        val datetime = jsonObject.get("datetime").asString
        val data = jsonObject.get("data").asString
        
        val messageElement = jsonObject.get("message")
        val message: Any = if (messageElement.isJsonObject) {
            context.deserialize(messageElement, LoginMessage::class.java)
        } else {
            messageElement.asString
        }
        
        return LoginResponse(code, message, timestamp, datetime, data)
    }
}
```

#### 方案B: 修改 NetworkUtils Gson 配置
```kotlin
private val gson: Gson = GsonBuilder()
    .registerTypeAdapter(LoginResponse::class.java, LoginResponseDeserializer())
    .create()
```

## 测试验证

运行应用并查看日志输出，确认：
1. JSON 原始数据格式
2. 解析后的对象类型
3. 类型判断的具体结果

根据日志输出选择合适的解决方案。