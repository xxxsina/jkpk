package com.jiankangpaika.app.data.repository

import android.util.Log
import com.jiankangpaika.app.R
import com.jiankangpaika.app.data.model.SportItem

/**
 * 运动项目数据仓库
 * 统一管理所有运动项目数据，提供数据访问和筛选功能
 */
object SportRepository {

    /**
     * 首页完整数据列表
     */
    private val indexSportsItems = listOf(
        // 首页 中医，科技，运动，食物
        SportItem("夏季养生三要诀", "中医", "https://example.com/tcm.jpg", "夏季养生三要诀\n\n晨起喝温水：空腹饮用200ml温水，加3片生姜暖胃\n\n午间养心：11点-13点静坐15分钟，按摩内关穴（手腕横纹三指处）\n\n傍晚散步：太阳落山后快走30分钟，穿薄底布鞋刺激脚底穴位\n\n注意：三伏天避免直接吹空调，备件棉背心护住后颈大椎穴。社区医院现可免费领取《老年养生手册》。"),
        SportItem("智能药盒", "科技", "https://example.com/tech.jpg", "智能药盒\n\n• 语音提醒：设定后会用方言播报\"阿公该吃降压药啦\"\n\n• 紧急呼叫：长按红色按钮3秒自动联系子女\n\n• 用药记录：子女手机随时查看是否漏服\n\n小贴士：新一代药盒已适配老花镜操作界面，按钮增大50%。朝阳区为80岁以上老人免费配发。"),
        SportItem("椅子健身操", "运动", "https://example.com/sports.jpg", "椅子健身操\n\n护膝练习：坐姿交替抬腿（早晚各20次）\n\n防跌倒训练：扶椅背金鸡独立（左右各10秒）\n\n肩颈放松：双手握毛巾做搓背动作\n\n安全提示：运动时穿防滑鞋，旁边放稳固椅子支撑。每周3次可提升腿部力量37%。"),
        SportItem("健骨营养餐", "食物", "https://example.com/food.jpg", "健骨营养餐\n\n• 早餐：黑芝麻糊+虾皮蒸蛋（补钙黄金组合）\n\n• 加餐：酸奶拌熟香蕉（改善便秘）\n\n• 晚餐：紫菜豆腐汤（含天然维生素D）\n\n提醒：菠菜、苋菜需焯水去草酸，以免影响钙吸收。各社区长者食堂已推出老年营养套餐。"),
    )
    
    /**
     * 所有运动项目的完整数据列表
     */
    private val allSportItems = listOf(
        // 首页 中医，科技，运动，食物
        SportItem("夏季养生三要诀", "中医", "https://example.com/tcm.jpg", "夏季养生三要诀\n\n晨起喝温水：空腹饮用200ml温水，加3片生姜暖胃\n\n午间养心：11点-13点静坐15分钟，按摩内关穴（手腕横纹三指处）\n\n傍晚散步：太阳落山后快走30分钟，穿薄底布鞋刺激脚底穴位\n\n注意：三伏天避免直接吹空调，备件棉背心护住后颈大椎穴。社区医院现可免费领取《老年养生手册》。"),
        SportItem("智能药盒", "科技", "https://example.com/tech.jpg", "智能药盒\n\n• 语音提醒：设定后会用方言播报\"阿公该吃降压药啦\"\n\n• 紧急呼叫：长按红色按钮3秒自动联系子女\n\n• 用药记录：子女手机随时查看是否漏服\n\n小贴士：新一代药盒已适配老花镜操作界面，按钮增大50%。朝阳区为80岁以上老人免费配发。"),
        SportItem("椅子健身操", "运动", "https://example.com/sports.jpg", "椅子健身操\n\n护膝练习：坐姿交替抬腿（早晚各20次）\n\n防跌倒训练：扶椅背金鸡独立（左右各10秒）\n\n肩颈放松：双手握毛巾做搓背动作\n\n安全提示：运动时穿防滑鞋，旁边放稳固椅子支撑。每周3次可提升腿部力量37%。"),
        SportItem("健骨营养餐", "食物", "https://example.com/food.jpg", "健骨营养餐\n\n• 早餐：黑芝麻糊+虾皮蒸蛋（补钙黄金组合）\n\n• 加餐：酸奶拌熟香蕉（改善便秘）\n\n• 晚餐：紫菜豆腐汤（含天然维生素D）\n\n提醒：菠菜、苋菜需焯水去草酸，以免影响钙吸收。各社区长者食堂已推出老年营养套餐。")
    )
    
    /**
     * 获取所有运动项目
     */
    fun getAllSportItems(): List<SportItem> {
        return allSportItems
    }
    
    /**
     * 根据分类获取运动项目
     */
    fun getSportItemsByCategory(category: String): List<SportItem> {
        return allSportItems.filter { it.category == category }
    }

    /**
     * 根据首页分类获取运动项目
     */
    fun getIndexSportItemsByCategory(category: String): List<SportItem> {
        return indexSportsItems.filter { it.category == category }
    }
    
    /**
     * 获取首页分类
     */
    fun getIndexCategories(): List<String> {
        return indexSportsItems.map { it.category }.distinct().sorted()
    }

    /**
     * 获取所有分类
     */
    fun getAllCategories(): List<String> {
        return allSportItems.map { it.category }.distinct().sorted()
    }
    
    /**
     * 获取首页展示的运动项目（每个分类选取一个代表性项目）
     */
    fun getHomeSportItems(): List<SportItem> {
        val categories = getIndexCategories()
        return categories.mapNotNull { category ->
            // 为每个分类选择一个代表性项目
            when (category) {
                "夏季养生三要诀" -> getIndexSportItemsByCategory(category).find { it.name == "食物" }
                "智能药盒" -> getIndexSportItemsByCategory(category).find { it.name == "科技" }
                "椅子健身操" -> getIndexSportItemsByCategory(category).find { it.name == "运动" }
                "健骨营养餐" -> getIndexSportItemsByCategory(category).find { it.name == "中医" }
                else -> getIndexSportItemsByCategory(category).firstOrNull()
            }
        } // 显示所有分类的代表性项目
    }
    
    /**
     * 根据名称搜索运动项目
     */
    fun searchSportItems(query: String): List<SportItem> {
        return allSportItems.filter { 
            it.name.contains(query, ignoreCase = true) || 
            it.category.contains(query, ignoreCase = true) ||
            it.description.contains(query, ignoreCase = true)
        }
    }
    
    /**
     * 根据ID获取运动项目
     */
    fun getSportItemById(id: String): SportItem? {
        return allSportItems.find { "${it.name}_${it.category}" == id }
    }
}