# 健康派卡应用登录功能实现说明

## 功能概述

本次实现了完整的用户登录验证功能，包括：

1. **Android端登录界面** - 用户输入用户名和密码进行登录
2. **登录状态管理** - 本地存储用户登录状态和信息
3. **页面访问控制** - "我的"页面需要登录后才能访问
4. **PHP后端接口** - 提供用户登录验证服务
5. **数据库设计** - 完整的用户数据表结构

## 实现的文件

### Android端文件

1. **`app/src/main/java/uni/UNI4BC70F5/utils/UserManager.kt`**
   - 用户状态管理工具类
   - 提供登录状态检查、用户信息存储等功能

2. **`app/src/main/java/uni/UNI4BC70F5/data/model/LoginModels.kt`**
   - 登录相关的数据模型
   - 包含登录请求、响应和用户数据类

3. **`app/src/main/java/uni/UNI4BC70F5/ui/screens/auth/LoginScreen.kt`** (已更新)
   - 登录界面实现
   - 集成网络请求和错误处理

4. **`app/src/main/java/uni/UNI4BC70F5/ui/navigation/ShenHuoBaoApp.kt`** (已更新)
   - 添加登录验证逻辑
   - 添加登录页面路由

5. **`app/src/main/java/uni/UNI4BC70F5/utils/constants/ApiConfig.kt`** (已更新)
   - 更新登录接口地址

### 后端文件

1. **`service-php/login_api.php`**
   - PHP登录接口实现
   - 提供用户登录验证服务

2. **`service-php/database_schema.sql`**
   - 数据库表结构设计
   - 包含用户表、登录日志等

## 功能流程

### 1. 用户点击"我的"按钮

```kotlin
// 在ShenHuoBaoApp.kt中
checkLoginAndNavigate("personal_settings", requireLogin = true)
```

- 系统检查用户是否已登录
- 如果未登录，跳转到登录页面
- 如果已登录，正常跳转到"我的"页面

### 2. 登录页面操作

- 用户输入用户名和密码
- 点击登录按钮触发网络请求
- 显示加载状态和错误提示

### 3. 登录验证流程

```kotlin
// 构建请求URL
val url = "${ApiConfig.User.LOGIN}?username=${username}&password=${password}"

// 发送网络请求
val result = NetworkUtils.get(url)

// 处理响应
if (result.isSuccess) {
    // 保存用户信息
    UserManager.saveUserInfo(...)
    // 跳转到目标页面
    onLoginSuccess()
}
```

### 4. 后端验证

```php
// 验证用户名和密码
$result = validateLogin($pdo, $username, $password);

// 返回JSON响应
if ($result['success']) {
    jsonResponse(200, '登录成功', $result['data']);
} else {
    jsonResponse(401, $result['message']);
}
```

## 数据库设计

### 用户表 (users)

| 字段 | 类型 | 说明 |
|------|------|------|
| user_id | VARCHAR(32) | 用户ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password_hash | VARCHAR(255) | 密码哈希 |
| nickname | VARCHAR(50) | 昵称 |
| phone | VARCHAR(20) | 手机号 |
| email | VARCHAR(100) | 邮箱 |
| avatar | VARCHAR(500) | 头像URL |
| status | TINYINT(1) | 用户状态 |
| login_token | VARCHAR(64) | 登录令牌 |
| create_time | TIMESTAMP | 创建时间 |
| last_login_time | TIMESTAMP | 最后登录时间 |

### 测试账户

系统预置了两个测试账户：

1. **普通用户**
   - 用户名：`testuser`
   - 密码：`password`

2. **管理员**
   - 用户名：`admin`
   - 密码：`password`

## 安全特性

1. **密码加密** - 使用PHP的`password_hash()`函数加密存储
2. **SQL注入防护** - 使用PDO预处理语句
3. **登录令牌** - 生成随机令牌用于会话管理
4. **状态验证** - 检查用户账户状态
5. **登录日志** - 记录登录尝试和结果

## 使用说明

### 1. 数据库配置

1. 创建MySQL数据库：`shenhuobao`
2. 执行SQL文件：`service-php/database_schema.sql`
3. 修改`service-php/login_api.php`中的数据库配置

### 2. 服务器部署

1. 将`service-php/`目录部署到Web服务器
2. 确保PHP支持PDO和MySQL扩展
3. 配置正确的数据库连接参数

### 3. Android应用配置

1. 确保`ApiConfig.kt`中的`BASE_URL`指向正确的服务器地址
2. 编译并运行应用
3. 测试登录功能

## 编译和运行

### 编译命令

```bash
# 在项目根目录执行
./gradlew assembleDebug
```

### 运行命令

```bash
# 安装到设备
./gradlew installDebug

# 或者直接运行
./gradlew run
```

## 错误处理

应用集成了完善的错误处理机制：

1. **网络错误** - 显示网络异常提示
2. **服务器错误** - 显示服务器返回的错误信息
3. **参数验证** - 客户端和服务端双重验证
4. **用户体验** - 加载状态和友好的错误提示

## 扩展功能

基于当前实现，可以轻松扩展以下功能：

1. **用户注册** - 添加注册接口和页面
2. **忘记密码** - 实现密码重置功能
3. **第三方登录** - 集成微信、QQ等登录
4. **自动登录** - 记住密码功能
5. **多设备管理** - 设备登录管理

## 注意事项

1. **生产环境** - 请修改数据库配置和安全设置
2. **HTTPS** - 生产环境建议使用HTTPS协议
3. **令牌管理** - 考虑使用JWT等标准令牌格式
4. **缓存策略** - 合理设置用户信息缓存时间
5. **日志记录** - 生产环境注意日志安全性