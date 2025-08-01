# Android项目修改包名操作手册

基于对您的ShenHuoBao Android项目的分析，我为您制作了一份完整的修改包名操作手册。当前项目使用的包名是 `uni.UNI4BC70F5`，以下是修改包名的详细步骤：

## 1. 修改Gradle配置文件

### 1.1 修改 `app/build.gradle`
需要修改以下两个地方：
```gradle
android {
    namespace 'your.new.package.name'  // 原: 'uni.UNI4BC70F5'
    
    defaultConfig {
        applicationId "your.new.package.name"  // 原: "uni.UNI4BC70F5.debug"
        // 其他配置保持不变
    }
}
```

## 2. 修改AndroidManifest.xml

### 2.1 检查权限声明
在 `app/src/main/AndroidManifest.xml` 中，确保以下权限声明使用了正确的包名变量：
```xml
<permission
    android:name="${applicationId}.openadsdk.permission.TT_PANGOLIN"
    android:protectionLevel="signature" />
<uses-permission android:name="${applicationId}.openadsdk.permission.TT_PANGOLIN" />
```

### 2.2 检查FileProvider配置
确保FileProvider的authorities使用了包名变量：
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
</provider>

<provider
    android:name="com.bytedance.sdk.openadsdk.TTFileProvider"
    android:authorities="${applicationId}.TTFileProvider"
    android:exported="false"
    android:grantUriPermissions="true">
</provider>
```

## 3. 重构源码目录结构

### 3.1 创建新的包目录结构
在 `app/src/main/java/` 下创建新的包目录结构，例如：
- 原路径：`app/src/main/java/uni/UNI4BC70F5/`
- 新路径：`app/src/main/java/your/new/package/name/`

### 3.2 移动所有源码文件
将以下目录及其所有子目录和文件移动到新的包路径下：
- `MainActivity.kt`
- `ShenHuoBaoApplication.kt`
- `SplashActivity.kt`
- `ad/` 目录及所有子文件
- `data/` 目录及所有子文件
- `domain/` 目录及所有子文件
- `services/` 目录及所有子文件
- `ui/` 目录及所有子文件
- `utils/` 目录及所有子文件

## 4. 批量修改包声明和导入语句

### 4.1 修改所有Kotlin文件的package声明
需要修改所有 `.kt` 文件顶部的package声明，将：
```kotlin
package uni.UNI4BC70F5.xxx
```
改为：
```kotlin
package your.new.package.name.xxx
```

### 4.2 修改所有import语句
将所有文件中的import语句从：
```kotlin
import uni.UNI4BC70F5.xxx
```
改为：
```kotlin
import your.new.package.name.xxx
```

### 4.3 修改硬编码的包名引用
在以下文件中找到并修改硬编码的包名：
- `LoginScreen.kt` 和 `RegisterScreen.kt` 中的资源URI：
  ```kotlin
  // 原代码
  "android.resource://uni.UNI4BC70F5/drawable/logo"
  // 修改为
  "android.resource://your.new.package.name/drawable/logo"
  ```

## 5. 清理和重新构建

### 5.1 清理项目
```bash
./gradlew clean
```

### 5.2 删除旧的包目录
确认所有文件都已移动到新包目录后，删除旧的 `uni/UNI4BC70F5/` 目录。

### 5.3 重新构建项目
```bash
./gradlew build
```

## 6. 验证修改结果

### 6.1 检查编译错误
确保项目能够成功编译，没有包名相关的错误。

### 6.2 检查生成的APK
验证生成的APK文件中的包名是否正确更新。

### 6.3 测试应用功能
- 测试应用启动
- 测试广告功能（快手和穿山甲SDK）
- 测试文件上传功能
- 测试用户登录注册功能

## 7. 注意事项

### 7.1 签名配置
如果修改了包名，可能需要重新配置应用签名，特别是keystore中的keyAlias。

### 7.2 广告SDK配置
修改包名后，需要在广告平台（快手、穿山甲）重新配置新的包名。

### 7.3 API服务端配置
如果后端API有包名验证，需要同步更新服务端配置。

### 7.4 版本发布
修改包名后，这将被视为一个全新的应用，无法通过应用商店进行增量更新。

## 8. 推荐工具

### 8.1 Android Studio重构功能
使用Android Studio的"Refactor" -> "Rename"功能可以自动处理大部分包名修改工作。

### 8.2 批量替换工具
可以使用IDE的"Find and Replace in Files"功能批量替换包名引用。

## 9. 项目特定的修改点

基于对当前项目的分析，以下是需要特别注意的文件和配置：

### 9.1 关键配置文件
- `app/build.gradle` - 包含namespace和applicationId配置
- `app/src/main/AndroidManifest.xml` - 包含权限和Provider配置
- `app/release/output-metadata.json` - 发布配置文件

### 9.2 需要修改的核心文件数量
项目中共有约60个Kotlin文件需要修改package声明和import语句，包括：
- 主要Activity文件（MainActivity.kt, SplashActivity.kt等）
- 广告相关文件（ad目录下的所有文件）
- 数据模型文件（data目录下的所有文件）
- UI组件文件（ui目录下的所有文件）
- 工具类文件（utils目录下的所有文件）

### 9.3 特殊注意的硬编码引用
- `LoginScreen.kt` 第87行
- `RegisterScreen.kt` 第113行
- 文档文件中的示例代码引用

完成以上步骤后，您的Android项目包名就成功修改了。建议在修改前备份整个项目，以防出现意外情况。