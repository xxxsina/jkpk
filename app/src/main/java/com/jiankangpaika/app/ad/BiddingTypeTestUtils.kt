package com.jiankangpaika.app.ad

import android.content.Context
import android.util.Log

/**
 * 竞价类型测试工具类
 * 用于测试和验证竞价类型切换功能
 */
object BiddingTypeTestUtils {
    
    private const val TAG = "BiddingTypeTestUtils"
    
    /**
     * 测试标准竞价模式
     * 将所有广告位设置为标准竞价
     */
    fun testStandardBidding(context: Context) {
        Log.d(TAG, "🧪 [测试工具] 开始测试标准竞价模式")
        
        BiddingTypeManager.init(context)
        BiddingTypeManager.setAllBiddingType(AdConfig.Chuanshanjia.BiddingType.STANDARD)
        BiddingTypeManager.printCurrentConfig()
        
        Log.d(TAG, "✅ [测试工具] 标准竞价模式设置完成")
    }
    
    /**
     * 测试服务端竞价模式
     * 将所有广告位设置为服务端竞价
     */
    fun testServerSideBidding(context: Context) {
        Log.d(TAG, "🧪 [测试工具] 开始测试服务端竞价模式")
        
        BiddingTypeManager.init(context)
        BiddingTypeManager.setAllBiddingType(AdConfig.Chuanshanjia.BiddingType.SERVER_SIDE)
        BiddingTypeManager.printCurrentConfig()
        
        Log.d(TAG, "✅ [测试工具] 服务端竞价模式设置完成")
    }
    
    /**
     * 测试混合竞价模式
     * 设置不同广告位使用不同的竞价类型
     */
    fun testMixedBidding(context: Context) {
        Log.d(TAG, "🧪 [测试工具] 开始测试混合竞价模式")
        
        BiddingTypeManager.init(context)
        
        // 开屏广告使用标准竞价
        BiddingTypeManager.setBiddingType(
            AdConfig.Chuanshanjia.AdUnitId.SPLASH,
            AdConfig.Chuanshanjia.BiddingType.STANDARD
        )
        
        // 激励视频使用服务端竞价
        BiddingTypeManager.setBiddingType(
            AdConfig.Chuanshanjia.AdUnitId.REWARD_VIDEO,
            AdConfig.Chuanshanjia.BiddingType.SERVER_SIDE
        )
        
        // 插屏广告使用标准竞价
        BiddingTypeManager.setBiddingType(
            AdConfig.Chuanshanjia.AdUnitId.INTERSTITIAL,
            AdConfig.Chuanshanjia.BiddingType.STANDARD
        )
        
        // Banner广告使用服务端竞价
        BiddingTypeManager.setBiddingType(
            AdConfig.Chuanshanjia.AdUnitId.BANNER,
            AdConfig.Chuanshanjia.BiddingType.SERVER_SIDE
        )
        
        // 信息流广告使用标准竞价
        BiddingTypeManager.setBiddingType(
            AdConfig.Chuanshanjia.AdUnitId.FEED,
            AdConfig.Chuanshanjia.BiddingType.STANDARD
        )
        
        BiddingTypeManager.printCurrentConfig()
        
        Log.d(TAG, "✅ [测试工具] 混合竞价模式设置完成")
    }
    
    /**
     * 重置为默认配置
     */
    fun resetToDefault(context: Context) {
        Log.d(TAG, "🔄 [测试工具] 重置为默认配置")
        
        BiddingTypeManager.init(context)
        BiddingTypeManager.resetAllBiddingType()
        BiddingTypeManager.printCurrentConfig()
        
        Log.d(TAG, "✅ [测试工具] 重置完成")
    }
    
    /**
     * 验证配置是否正确应用
     */
    fun verifyConfiguration(context: Context) {
        Log.d(TAG, "🔍 [测试工具] 开始验证配置")
        
        BiddingTypeManager.init(context)
        
        val configs = mapOf(
            "开屏广告" to AdConfig.Chuanshanjia.AdSlotConfigs.SPLASH,
            "Banner广告" to AdConfig.Chuanshanjia.AdSlotConfigs.BANNER,
            "激励视频" to AdConfig.Chuanshanjia.AdSlotConfigs.REWARD_VIDEO,
            "插屏广告" to AdConfig.Chuanshanjia.AdSlotConfigs.INTERSTITIAL,
            "信息流广告" to AdConfig.Chuanshanjia.AdSlotConfigs.FEED
        )
        
        configs.forEach { (name, config) ->
            val dynamicConfig = BiddingTypeManager.getDynamicAdSlotConfig(config)
            val originalType = config.biddingType
            val currentType = dynamicConfig.biddingType
            
            if (originalType != currentType) {
                Log.d(TAG, "🔄 [$name] 竞价类型已切换: $originalType -> $currentType")
            } else {
                Log.d(TAG, "✅ [$name] 竞价类型保持: $currentType")
            }
        }
        
        Log.d(TAG, "✅ [测试工具] 配置验证完成")
    }
    
    /**
     * 模拟错误码40034的解决方案测试
     */
    fun testError40034Solution(context: Context) {
        Log.d(TAG, "🧪 [测试工具] 测试错误码40034解决方案")
        
        BiddingTypeManager.init(context)
        
        Log.d(TAG, "📋 [测试工具] 当前配置:")
        BiddingTypeManager.printCurrentConfig()
        
        Log.d(TAG, "🔧 [测试工具] 切换开屏广告为标准竞价模式（解决40034错误）")
        BiddingTypeManager.setBiddingType(
            AdConfig.Chuanshanjia.AdUnitId.SPLASH,
            AdConfig.Chuanshanjia.BiddingType.STANDARD
        )
        
        Log.d(TAG, "📋 [测试工具] 修改后配置:")
        BiddingTypeManager.printCurrentConfig()
        
        Log.d(TAG, "✅ [测试工具] 错误码40034解决方案测试完成")
        Log.d(TAG, "💡 [测试工具] 建议：重新加载开屏广告以验证修复效果")
    }
    
    /**
     * 打印所有可用的测试方法
     */
    fun printAvailableTests() {
        Log.d(TAG, "📋 [测试工具] 可用的测试方法:")
        Log.d(TAG, "  1. testStandardBidding() - 测试标准竞价模式")
        Log.d(TAG, "  2. testServerSideBidding() - 测试服务端竞价模式")
        Log.d(TAG, "  3. testMixedBidding() - 测试混合竞价模式")
        Log.d(TAG, "  4. resetToDefault() - 重置为默认配置")
        Log.d(TAG, "  5. verifyConfiguration() - 验证配置")
        Log.d(TAG, "  6. testError40034Solution() - 测试错误码40034解决方案")
    }
}