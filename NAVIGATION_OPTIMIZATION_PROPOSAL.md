# 导航架构优化方案

## 当前问题分析

### 1. 导航逻辑复杂性
当前的导航架构存在以下问题：

- **双重导航路径**：`CheckInScreen` 既作为独立路由 `composable("checkin")` 存在，又嵌入在 `HomeScreen` 的 Tab 中
- **回调传递不一致**：联系客服功能的导航回调在两种情况下传递路径不同
- **代码重复**：`HomeScreen` 的两个路由定义（`"home"` 和 `"home/{tabIndex}"`）包含大量重复的回调参数
- **维护困难**：修改导航逻辑需要在多个地方同步更新

### 2. 具体问题点

```kotlin
// 问题1：独立路由中的 CheckInScreen
composable("checkin") {
    CheckInScreen(
        onNavigateToCustomerService = {
            showInterstitialAdOnNavigation("customer_service_form")
        }
    )
}

// 问题2：HomeScreen Tab 中的 CheckInScreen
2 -> CheckInScreen(
    onNavigateToCustomerService = onNavigateToCustomerService // 来自 HomeScreen 参数
)

// 问题3：HomeScreen 重复的回调参数定义
composable("home") { /* 大量重复参数 */ }
composable("home/{tabIndex}") { /* 相同的重复参数 */ }
```

## 优化方案

### 方案一：统一导航管理器（推荐）

#### 1. 创建全局导航管理器

```kotlin
@Composable
fun rememberNavigationManager(
    navController: NavController,
    context: Context
): NavigationManager {
    return remember {
        NavigationManager(navController, context)
    }
}

class NavigationManager(
    private val navController: NavController,
    private val context: Context
) {
    // 统一的导航方法
    fun navigateToCustomerService() {
        Log.d("NavigationManager", "导航到客服页面")
        showInterstitialAdOnNavigation("customer_service_form")
    }
    
    fun navigateToLogin(returnTo: String = "home/3") {
        navController.navigate("login?returnTo=$returnTo")
    }
    
    fun navigateToEditAvatar(source: String = "home") {
        checkLoginAndNavigate("edit_avatar/$source", requireLogin = true)
    }
    
    // 其他导航方法...
    
    private fun showInterstitialAdOnNavigation(targetRoute: String) {
        // 插屏广告逻辑
    }
    
    private fun checkLoginAndNavigate(targetRoute: String, requireLogin: Boolean = false) {
        // 登录检查逻辑
    }
}
```

#### 2. 简化 HomeScreen 参数

```kotlin
@Composable
fun HomeScreen(
    navController: NavController,
    navigationManager: NavigationManager,
    initialTabIndex: Int = 0
) {
    // 移除所有导航回调参数，统一使用 navigationManager
}
```

#### 3. 统一 CheckInScreen 调用

```kotlin
// 移除独立的 checkin 路由，只在 HomeScreen 中使用
2 -> CheckInScreen(
    navigationManager = navigationManager
)

// CheckInScreen 修改为接收 NavigationManager
@Composable
fun CheckInScreen(
    navigationManager: NavigationManager
) {
    // 直接使用 navigationManager.navigateToCustomerService()
    // 直接使用 navigationManager.navigateToLogin("checkin")
}
```

### 方案二：Context-based 导航（备选）

#### 1. 使用 CompositionLocal 提供导航上下文

```kotlin
val LocalNavigationActions = compositionLocalOf<NavigationActions> {
    error("NavigationActions not provided")
}

data class NavigationActions(
    val navigateToCustomerService: () -> Unit,
    val navigateToLogin: (String) -> Unit,
    val navigateToEditAvatar: (String) -> Unit,
    // 其他导航动作...
)

@Composable
fun ShenHuoBaoApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val navigationActions = remember {
        NavigationActions(
            navigateToCustomerService = {
                showInterstitialAdOnNavigation("customer_service_form")
            },
            navigateToLogin = { returnTo ->
                navController.navigate("login?returnTo=$returnTo")
            },
            // 其他实现...
        )
    }
    
    CompositionLocalProvider(LocalNavigationActions provides navigationActions) {
        NavHost(navController = navController, startDestination = "home") {
            // 路由定义...
        }
    }
}
```

#### 2. 在组件中使用

```kotlin
@Composable
fun CheckInScreen() {
    val navigationActions = LocalNavigationActions.current
    
    // 使用统一的导航动作
    Button(
        onClick = { navigationActions.navigateToCustomerService() }
    ) {
        Text("联系客服")
    }
}
```

### 方案三：简化现有架构（最小改动）

#### 1. 移除独立的 checkin 路由

```kotlin
// 删除这个路由定义
// composable("checkin") { ... }
```

#### 2. 统一 HomeScreen 路由定义

```kotlin
// 创建通用的 HomeScreen 组合函数
@Composable
fun createHomeScreenComposable(
    navController: NavController,
    tabIndex: Int
): @Composable () -> Unit = {
    HomeScreen(
        navController = navController,
        initialTabIndex = tabIndex,
        onNavigateToEditAvatar = {
            checkLoginAndNavigate("edit_avatar/home", requireLogin = true)
        },
        // 其他统一的回调...
        onNavigateToCustomerService = {
            showInterstitialAdOnNavigation("customer_service_form")
        }
    )
}

// 使用统一的定义
composable("home") {
    createHomeScreenComposable(navController, 0)()
}

composable("home/{tabIndex}") { backStackEntry ->
    val tabIndex = backStackEntry.arguments?.getString("tabIndex")?.toIntOrNull() ?: 0
    createHomeScreenComposable(navController, tabIndex)()
}
```

## 推荐实施步骤

### 第一阶段：创建导航管理器
1. 创建 `NavigationManager` 类
2. 将现有的导航逻辑迁移到管理器中
3. 在 `ShenHuoBaoApp` 中创建管理器实例

### 第二阶段：重构 HomeScreen
1. 简化 `HomeScreen` 的参数列表
2. 传递 `NavigationManager` 实例
3. 更新 `CheckInScreen` 以使用管理器

### 第三阶段：清理冗余代码
1. 移除独立的 `checkin` 路由
2. 合并重复的 `HomeScreen` 路由定义
3. 清理不再需要的回调参数

### 第四阶段：测试和验证
1. 测试所有导航功能
2. 验证插屏广告逻辑
3. 确保登录检查正常工作

## 优化效果

### 1. 代码简化
- 减少 50% 的导航相关代码
- 消除重复的回调参数定义
- 统一的导航逻辑入口

### 2. 维护性提升
- 单一职责：导航逻辑集中管理
- 易于扩展：新增导航功能只需在管理器中添加
- 易于测试：导航逻辑可独立测试

### 3. 一致性保证
- 所有导航都通过统一的接口
- 插屏广告和登录检查逻辑一致
- 减少因不同调用路径导致的 bug

## 结论

推荐采用**方案一：统一导航管理器**，这种方案能够：

1. **最大程度简化代码**：消除重复和复杂的回调传递
2. **提高可维护性**：集中管理所有导航逻辑
3. **保持向后兼容**：不破坏现有功能
4. **易于扩展**：新增导航功能更加简单

这种架构更符合单一职责原则，使代码更加清晰和易于维护。