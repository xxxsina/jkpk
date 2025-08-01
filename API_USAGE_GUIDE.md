# 统一 API 工具使用指南

本项目已实现统一的 API 响应处理方案，解决了接口请求增加时的消息处理问题。

## 核心组件

### 1. BaseApiResponse<T> - 通用响应基类

位置：`app/src/main/java/uni/UNI4BC70F5/data/model/BaseApiResponse.kt`

```kotlin
data class BaseApiResponse<T>(
    val code: Int,
    val message: Any, // 联合类型：成功时为对象，失败时为字符串
    val timestamp: Long,
    val datetime: String,
    val data: String
) {
    fun isSuccess(): Boolean = code == 200
    inline fun <reified T> getSuccessMessage(): T?
    fun getErrorMessage(): String
    fun getMessageContent(): String
}
```

### 2. BaseApiResponseDeserializer<T> - 通用反序列化器

自动处理 `message` 字段的联合类型（成功时为对象，失败时为字符串）。

### 3. ApiUtils - 统一 API 工具类

位置：`app/src/main/java/uni/UNI4BC70F5/utils/ApiUtils.kt`

提供统一的接口调用方法：
- `login(loginRequest)` - 登录
- `register(registerRequest)` - 注册
- `request<T>(url, requestBody, responseClass)` - 通用请求

### 4. ApiResult<T> - 统一结果封装

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String) : ApiResult<Nothing>()
}
```

## 使用方法

### 现有接口（登录/注册）

已更新的文件：
- `LoginScreen.kt` - 使用 `ApiUtils.login()`
- `RegisterScreen.kt` - 使用 `ApiUtils.register()`

### 新增接口的标准流程

#### 1. 定义响应消息类

```kotlin
// 例如：用户信息更新响应
data class UpdateUserMessage(
    val user_info: UserInfo,
    val message: String
)
```

#### 2. 创建类型别名

```kotlin
typealias UpdateUserApiResponse = BaseApiResponse<UpdateUserMessage>
```

#### 3. 注册反序列化器

在 `NetworkUtils.kt` 中添加：

```kotlin
private val gson: Gson = GsonBuilder()
    // ... 现有配置
    .registerTypeAdapter(
        UpdateUserApiResponse::class.java, 
        BaseApiResponseDeserializer(UpdateUserMessage::class.java)
    )
    .create()
```

#### 4. 在 ApiUtils 中添加方法

```kotlin
suspend fun updateUser(updateRequest: Any): ApiResult<UpdateUserMessage> {
    return request(
        url = ApiConfig.User.UPDATE,
        requestBody = updateRequest,
        responseClass = UpdateUserMessage::class.java
    )
}
```

#### 5. 在 UI 中使用

```kotlin
coroutineScope.launch {
    val result = ApiUtils.updateUser(updateRequest)
    when (result) {
        is ApiResult.Success -> {
            val userMessage = result.data
            // 处理成功逻辑
        }
        is ApiResult.Error -> {
            // 显示错误信息
            ToastUtils.showErrorToast(context, result.parseApiErrorMessage())
        }
    }
}
```

## 优势

1. **统一处理**：所有接口使用相同的响应格式和错误处理逻辑
2. **类型安全**：编译时检查，避免运行时类型错误
3. **代码复用**：减少重复的网络请求和解析代码
4. **易于维护**：集中管理 API 调用逻辑
5. **错误处理**：统一的错误处理和用户提示
6. **扩展性强**：新增接口只需要几行代码

## 注意事项

1. 所有新接口都应该遵循相同的响应格式
2. `message` 字段必须支持联合类型（成功时为对象，失败时为字符串）
3. 记得在 `NetworkUtils.kt` 中注册新的反序列化器
4. 使用 `ApiResult` 进行结果处理，避免直接处理网络异常

## 迁移现有代码

对于现有的其他接口，建议按照以下步骤迁移：

1. 分析现有接口的响应格式
2. 创建对应的消息类和类型别名
3. 注册反序列化器
4. 在 `ApiUtils` 中添加对应方法
5. 更新 UI 代码使用新的 API 工具
6. 测试确保功能正常

这样可以逐步将所有接口统一到新的处理方案中。