package com.jiankangpaika.app.ad

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 竞价类型管理器
 * 用于动态管理和切换广告位的竞价类型
 */
object BiddingTypeManager {
    
    private const val TAG = "BiddingTypeManager"
    private const val PREFS_NAME = "bidding_type_prefs"
    private const val KEY_PREFIX = "bidding_type_"
    
    private var sharedPrefs: SharedPreferences? = null
    
    /**
     * 初始化管理器
     */
    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d(TAG, "🔧 [竞价管理器] 初始化完成")
    }
    
    /**
     * 设置指定广告位的竞价类型
     * @param adUnitId 广告位ID
     * @param biddingType 竞价类型
     */
    fun setBiddingType(adUnitId: String, biddingType: AdConfig.Chuanshanjia.BiddingType) {
        val key = KEY_PREFIX + adUnitId
        sharedPrefs?.edit()?.putString(key, biddingType.name)?.apply()
        Log.d(TAG, "🔧 [竞价管理器] 设置广告位 $adUnitId 竞价类型为: $biddingType")
    }
    
    /**
     * 获取指定广告位的竞价类型
     * @param adUnitId 广告位ID
     * @param defaultType 默认竞价类型
     * @return 竞价类型
     */
    fun getBiddingType(
        adUnitId: String, 
        defaultType: AdConfig.Chuanshanjia.BiddingType = AdConfig.Chuanshanjia.BiddingType.STANDARD
    ): AdConfig.Chuanshanjia.BiddingType {
        val key = KEY_PREFIX + adUnitId
        val typeName = sharedPrefs?.getString(key, defaultType.name) ?: defaultType.name
        
        return try {
            AdConfig.Chuanshanjia.BiddingType.valueOf(typeName)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "⚠️ [竞价管理器] 无效的竞价类型: $typeName，使用默认类型: $defaultType")
            defaultType
        }
    }
    
    /**
     * 获取动态配置的广告位配置
     * @param baseConfig 基础配置
     * @return 动态配置后的广告位配置
     */
    fun getDynamicAdSlotConfig(baseConfig: AdConfig.Chuanshanjia.AdSlotConfig): AdConfig.Chuanshanjia.AdSlotConfig {
        val dynamicBiddingType = getBiddingType(baseConfig.adUnitId, baseConfig.biddingType)
        
        return if (dynamicBiddingType != baseConfig.biddingType) {
            Log.d(TAG, "🔄 [竞价管理器] 广告位 ${baseConfig.adUnitId} 竞价类型从 ${baseConfig.biddingType} 切换为 $dynamicBiddingType")
            baseConfig.copy(biddingType = dynamicBiddingType)
        } else {
            baseConfig
        }
    }
    
    /**
     * 批量设置所有广告位的竞价类型
     * @param biddingType 竞价类型
     */
    fun setAllBiddingType(biddingType: AdConfig.Chuanshanjia.BiddingType) {
        val adSlots = listOf(
            AdConfig.Chuanshanjia.AdUnitId.SPLASH,
            AdConfig.Chuanshanjia.AdUnitId.BANNER,
            AdConfig.Chuanshanjia.AdUnitId.REWARD_VIDEO,
            AdConfig.Chuanshanjia.AdUnitId.INTERSTITIAL,
            AdConfig.Chuanshanjia.AdUnitId.FEED
        )
        
        adSlots.forEach { adUnitId ->
            setBiddingType(adUnitId, biddingType)
        }
        
        Log.d(TAG, "🔧 [竞价管理器] 批量设置所有广告位竞价类型为: $biddingType")
    }
    
    /**
     * 重置所有广告位的竞价类型为默认值
     */
    fun resetAllBiddingType() {
        setAllBiddingType(AdConfig.Chuanshanjia.Config.DEFAULT_BIDDING_TYPE)
        Log.d(TAG, "🔄 [竞价管理器] 重置所有广告位竞价类型为默认值")
    }
    
    /**
     * 获取所有广告位的竞价类型配置
     * @return 广告位ID到竞价类型的映射
     */
    fun getAllBiddingTypes(): Map<String, AdConfig.Chuanshanjia.BiddingType> {
        val adSlots = listOf(
            AdConfig.Chuanshanjia.AdUnitId.SPLASH,
            AdConfig.Chuanshanjia.AdUnitId.BANNER,
            AdConfig.Chuanshanjia.AdUnitId.REWARD_VIDEO,
            AdConfig.Chuanshanjia.AdUnitId.INTERSTITIAL,
            AdConfig.Chuanshanjia.AdUnitId.FEED
        )
        
        return adSlots.associateWith { adUnitId ->
            getBiddingType(adUnitId)
        }
    }
    
    /**
     * 清除所有竞价类型配置
     */
    fun clearAllBiddingTypes() {
        sharedPrefs?.edit()?.clear()?.apply()
        Log.d(TAG, "🧹 [竞价管理器] 清除所有竞价类型配置")
    }
    
    /**
     * 检查是否启用了竞价类型切换功能
     */
    fun isBiddingTypeSwitchEnabled(): Boolean {
        return AdConfig.Chuanshanjia.Config.ENABLE_BIDDING_TYPE_SWITCH
    }
    
    /**
     * 打印当前所有广告位的竞价类型配置
     */
    fun printCurrentConfig() {
        Log.d(TAG, "📊 [竞价管理器] 当前竞价类型配置:")
        getAllBiddingTypes().forEach { (adUnitId, biddingType) ->
            Log.d(TAG, "  - 广告位 $adUnitId: $biddingType")
        }
    }
}