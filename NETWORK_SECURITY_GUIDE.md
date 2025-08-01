# 网络安全配置指南

## 问题描述

在Android 9.0 (API level 28) 及以上版本中，系统默认禁用明文HTTP流量，以提高应用的网络安全性。当应用尝试访问HTTP接口时，会出现以下错误：

```
Cleartext HTTP traffic to [domain] not permitted
```

## 解决方案

### 方案一：升级到HTTPS（推荐）

这是最安全和推荐的解决方案：

1. **修改API配置**
   ```kotlin
   // 在 ApiConfig.kt 中
   private const val BASE_URL = "https://shop.blcwg.com"  // 使用HTTPS
   ```

2. **优势**
   - ✅ 数据传输加密，提高安全性
   - ✅ 符合现代网络安全标准
   - ✅ 无需额外配置
   - ✅ 兼容所有Android版本

### 方案二：配置网络安全策略（临时方案）

如果服务器暂时不支持HTTPS，可以通过以下步骤允许HTTP流量：

1. **创建网络安全配置文件**
   
   在 `app/src/main/res/xml/` 目录下创建 `network_security_config.xml`：
   ```xml
   <?xml version="1.0" encoding="utf-8"?>
   <network-security-config>
       <domain-config cleartextTrafficPermitted="true">
           <domain includeSubdomains="true">shop.blcwg.com</domain>
       </domain-config>
   </network-security-config>
   ```

2. **在AndroidManifest.xml中引用**
   ```xml
   <application
       android:networkSecurityConfig="@xml/network_security_config"
       android:usesCleartextTraffic="true"
       ... >
   ```

3. **注意事项**
   - ⚠️ 仅在开发和测试阶段使用
   - ⚠️ 生产环境强烈建议使用HTTPS
   - ⚠️ 可能存在安全风险

## 当前项目配置

本项目已采用**方案一**，将所有API接口升级为HTTPS：

- ✅ `ApiConfig.kt` 中的 `BASE_URL` 已更新为 `https://shop.blcwg.com`
- ✅ 移除了AndroidManifest.xml中的明文流量配置
- ✅ 保持了最高的网络安全标准

## 验证方法

1. **编译测试**
   ```bash
   ./gradlew compileDebugKotlin
   ```

2. **功能测试**
   - 测试版本更新功能
   - 测试其他网络请求功能
   - 确保所有API调用正常

3. **网络监控**
   - 使用抓包工具验证HTTPS连接
   - 检查证书有效性

## 服务器要求

使用HTTPS需要服务器满足以下条件：

1. **SSL证书**
   - 有效的SSL/TLS证书
   - 证书链完整
   - 证书未过期

2. **HTTPS支持**
   - 服务器配置HTTPS端口（通常是443）
   - 支持现代TLS版本（TLS 1.2+）

3. **重定向配置**
   - 可选：将HTTP请求重定向到HTTPS

## 故障排除

### 常见问题

1. **证书验证失败**
   ```
   javax.net.ssl.SSLHandshakeException
   ```
   - 检查服务器证书是否有效
   - 确认证书链完整

2. **连接超时**
   ```
   java.net.SocketTimeoutException
   ```
   - 检查服务器HTTPS端口是否开放
   - 调整网络超时设置

3. **域名不匹配**
   ```
   java.security.cert.CertificateException
   ```
   - 确认证书域名与请求域名一致

### 调试方法

1. **启用网络日志**
   ```kotlin
   // 在NetworkUtils.kt中添加详细日志
   Log.d(TAG, "Request URL: $url")
   Log.d(TAG, "Response: $response")
   ```

2. **使用网络调试工具**
   - Charles Proxy
   - Wireshark
   - Android Studio Network Inspector

## 最佳实践

1. **开发阶段**
   - 优先使用HTTPS
   - 如需HTTP，使用网络安全配置限制特定域名

2. **测试阶段**
   - 测试所有网络功能
   - 验证证书有效性
   - 检查网络安全配置

3. **生产环境**
   - 强制使用HTTPS
   - 定期检查证书有效期
   - 监控网络安全状态

4. **代码管理**
   - 在ApiConfig.kt中统一管理所有接口地址
   - 使用环境变量区分开发/测试/生产环境
   - 避免硬编码HTTP地址

## 相关文件

- `ApiConfig.kt` - API接口地址配置
- `AndroidManifest.xml` - 应用清单文件
- `network_security_config.xml` - 网络安全配置（如需要）
- `NetworkUtils.kt` - 网络请求工具类
- `VersionUpdateManager.kt` - 版本更新管理器

通过正确的网络安全配置，可以确保应用在所有Android版本上正常工作，同时保持最高的安全标准！