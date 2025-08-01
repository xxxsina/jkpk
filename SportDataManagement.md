# 运动数据统一管理实现说明

## 概述

本次重构将原本分散在 `HomeScreen.kt` 和 `SportScreen.kt` 中的运动项目数据统一管理，实现了数据的集中存储和联动展示。

## 实现方案

### 1. 创建数据仓库 (SportRepository)

**文件位置**: `app/src/main/java/uni/UNI4BC70F5/data/repository/SportRepository.kt`

**主要功能**:
- 统一存储所有运动项目数据
- 提供多种数据访问方法
- 支持分类筛选和搜索功能

**核心方法**:
```kotlin
// 获取所有运动项目
fun getAllSportItems(): List<SportItem>

// 根据分类获取运动项目
fun getSportItemsByCategory(category: String): List<SportItem>

// 获取首页展示的运动项目（每个分类选取一个代表性项目）
fun getHomeSportItems(): List<SportItem>

// 获取所有分类
fun getAllCategories(): List<String>

// 搜索运动项目
fun searchSportItems(query: String): List<SportItem>
```

### 2. 数据分类体系

运动项目按以下分类组织：
- **田径**: 100米短跑、马拉松、跳高、跳远、铅球、标枪、十项全能
- **游泳**: 自由泳、蝶泳、仰泳、蛙泳、混合泳
- **体操**: 自由体操、鞍马、吊环、跳马、双杠、单杠、平衡木、高低杠
- **球类**: 足球、篮球、排球、网球、乒乓球、羽毛球、手球、水球等
- **格斗**: 拳击、跆拳道、柔道、摔跤、击剑
- **水上**: 赛艇、皮划艇、帆船、冲浪、跳水、花样游泳
- **自行车**: 公路自行车、场地自行车、山地自行车、BMX自行车
- **力量**: 举重、仰卧起坐、引体向上
- **耐力**: 跳绳
- **柔韧**: 坐位体前屈
- **爆发力**: 立定跳远
- **速度**: 50米跑
- **射击**: 射箭、射击
- **综合**: 现代五项、铁人三项
- **马术**: 马术
- **精准**: 高尔夫
- **极限**: 滑板、攀岩

### 3. 首页联动机制

**首页展示策略**:
- 从每个分类中选取一个代表性项目
- 优先选择知名度高、代表性强的项目
- 最多显示6个项目，保持界面简洁

**代表性项目选择**:
- 田径 → 100米短跑
- 游泳 → 自由泳
- 体操 → 自由体操
- 球类 → 足球
- 格斗 → 拳击
- 水上 → 跳水
- 自行车 → 公路自行车
- 力量 → 举重
- 耐力 → 跳绳
- 柔韧 → 坐位体前屈
- 爆发力 → 立定跳远
- 速度 → 50米跑

### 4. 文件修改说明

#### HomeScreen.kt
**修改内容**:
- 添加 `SportRepository` 导入
- 将 `SportCardsSection` 中的硬编码数据替换为 `SportRepository.getHomeSportItems()`
- 移除了原有的6个固定运动项目数据

**修改前**:
```kotlin
val sportItems = listOf(
    SportItem("跳绳", "耐力", R.drawable.sport_card_1, "..."),
    // ... 其他5个项目
)
```

**修改后**:
```kotlin
val sportItems = SportRepository.getHomeSportItems()
```

#### SportScreen.kt
**修改内容**:
- 添加 `SportRepository` 导入
- 将所有硬编码的运动项目数据替换为 `SportRepository.getAllSportItems()`
- 移除了原有的70+个运动项目数据定义

**修改前**:
```kotlin
val sportItems = listOf(
    SportItem("100米短跑", "田径", R.drawable.ic_running, "..."),
    // ... 其他70+个项目
)
```

**修改后**:
```kotlin
val sportItems = SportRepository.getAllSportItems()
```

## 优势

### 1. 数据一致性
- 所有页面使用相同的数据源
- 避免数据不同步问题
- 统一的数据格式和结构

### 2. 维护便利性
- 新增/修改运动项目只需在一个地方操作
- 减少代码重复
- 降低维护成本

### 3. 功能扩展性
- 支持按分类筛选
- 支持搜索功能
- 易于添加新的数据访问方法

### 4. 性能优化
- 使用 `object` 单例模式，避免重复创建
- 数据懒加载，按需获取

### 5. 联动效果
- 首页自动展示各分类的代表性项目
- 点击首页项目可跳转到运动页面查看同分类的所有项目
- 数据修改后自动同步到所有使用的页面

## 使用示例

```kotlin
// 获取所有运动项目
val allSports = SportRepository.getAllSportItems()

// 获取首页展示项目
val homeSports = SportRepository.getHomeSportItems()

// 获取特定分类的项目
val ballSports = SportRepository.getSportItemsByCategory("球类")

// 搜索运动项目
val searchResults = SportRepository.searchSportItems("足球")

// 获取所有分类
val categories = SportRepository.getAllCategories()
```

## 后续扩展建议

1. **添加收藏功能**: 用户可以收藏喜欢的运动项目
2. **个性化推荐**: 根据用户行为推荐相关运动项目
3. **数据持久化**: 将数据存储到数据库，支持离线访问
4. **动态数据**: 从服务器获取最新的运动项目数据
5. **多语言支持**: 支持运动项目名称和描述的多语言显示