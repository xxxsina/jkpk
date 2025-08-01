package com.jiankangpaika.app.ad

/**
 * 广告配置类
 * 统一管理广告相关的配置信息
 * 现在支持从服务器动态获取配置参数
 */
object AdConfig {
    
    // 动态配置管理器
    private val dynamicConfig = DynamicAdConfig.getInstance()
    
    // 快手广告SDK配置
    object Kuaishou {
        // 快手广告APP ID（动态获取）
        val APP_ID: String get() = dynamicConfig.getKuaishouAppId()
        val APP_NAME: String get() = dynamicConfig.getKuaishouAppName()
        
        // 广告位ID配置（动态获取）
        object AdUnitId {
            val SPLASH: Long get() = dynamicConfig.getKuaishouSplash().toLongOrNull() ?: 4000000042L
            val BANNER: Long get() = dynamicConfig.getKuaishouBanner().toLongOrNull() ?: 4000001623L
            val REWARD_VIDEO: Long get() = dynamicConfig.getKuaishouRewardVideo().toLongOrNull() ?: 90009001L
            val INTERSTITIAL: Long get() = dynamicConfig.getKuaishouInterstitial().toLongOrNull() ?: 4000000276L
            val FEED: Long get() = dynamicConfig.getKuaishouFeed().toLongOrNull() ?: 4000000079L
            val DRAW_VIDEO: Long get() = dynamicConfig.getKuaishouDrawVideo().toLongOrNull() ?: 4000000020L
        }
        
        // 广告配置参数
        object Config {
            const val DEBUG_MODE = true  // 测试环境设为true，正式环境设为false
            const val SHOW_NOTIFICATION = true
            const val SPLASH_TIMEOUT_MS = 15000L  // 开屏广告超时时间（毫秒）
            const val SPLASH_DISPLAY_TIME_MS = 15000L  // 开屏广告展示时间（毫秒）
        }
    }
    
    // 穿山甲广告SDK配置
    object Chuanshanjia {
        // 穿山甲广告APP ID（动态获取）
        val APP_ID: String get() = dynamicConfig.getChuanshanjiaAppId()
        val APP_NAME: String get() = dynamicConfig.getChuanshanjiaAppName()
        
        // 竞价类型枚举
        enum class BiddingType {
            STANDARD,      // 标准竞价
            SERVER_SIDE    // 服务端竞价
        }
        
        // 广告位配置类
        data class AdSlotConfig(
            val adUnitId: String,
            val biddingType: BiddingType,
            val description: String
        )
        
        // 穿山甲广告位ID配置（动态获取）
        object AdUnitId {
            val SPLASH: String get() = dynamicConfig.getChuanshanjiaSplash()
            val FEED: String get() = dynamicConfig.getChuanshanjiaFeed()
            val REWARD_VIDEO: String get() = dynamicConfig.getChuanshanjiaRewardVideo()
            val INTERSTITIAL: String get() = dynamicConfig.getChuanshanjiaInterstitial()
            val BANNER: String get() = dynamicConfig.getChuanshanjiaBanner()
            val DRAW_VIDEO: String get() = dynamicConfig.getChuanshanjiaDrawVideo()
        }
        
        // 广告位配置映射（包含竞价类型）
        object AdSlotConfigs {
            val SPLASH = AdSlotConfig(
                adUnitId = AdUnitId.SPLASH,
                biddingType = BiddingType.SERVER_SIDE,
                description = "开屏广告位"
            )
            
            val BANNER = AdSlotConfig(
                adUnitId = AdUnitId.BANNER,
                biddingType = BiddingType.SERVER_SIDE,
                description = "Banner广告位"
            )
            
            val REWARD_VIDEO = AdSlotConfig(
                adUnitId = AdUnitId.REWARD_VIDEO,
                biddingType = BiddingType.STANDARD,
                description = "激励视频广告位"
            )
            
            val INTERSTITIAL = AdSlotConfig(
                adUnitId = AdUnitId.INTERSTITIAL,
                biddingType = BiddingType.STANDARD,
                description = "插屏广告位"
            )
            
            val FEED = AdSlotConfig(
                adUnitId = AdUnitId.FEED,
                biddingType = BiddingType.SERVER_SIDE,
                description = "信息流广告位"
            )

            val DRAW_VIDEO = AdSlotConfig(
                adUnitId = AdUnitId.DRAW_VIDEO,
                biddingType = BiddingType.STANDARD,
                description = "Draw视频广告位"
            )
        }
        
        // 广告配置参数
        object Config {
            const val DEBUG_MODE = true  // 测试环境设为true，正式环境设为false
            const val SUPPORT_MULTI_PROCESS = false  // 单进程应用设为false
            const val SPLASH_TIMEOUT_MS = 5000L  // 开屏广告超时时间（毫秒）
            
            // 竞价配置
            const val ENABLE_BIDDING_TYPE_SWITCH = true  // 是否启用竞价类型切换
            val DEFAULT_BIDDING_TYPE = BiddingType.STANDARD  // 默认竞价类型
        }
    }
    
    // Taku广告SDK配置
    object Taku {
        // Taku广告APP ID和APP KEY（动态获取）
        val APP_ID: String get() = dynamicConfig.getTakuAppId()
        val APP_KEY: String get() = dynamicConfig.getTakuAppKey()
        val APP_NAME: String get() = dynamicConfig.getTakuAppName()
        
        // 广告位ID配置（动态获取）
        object AdUnitId {
            val SPLASH: String get() = dynamicConfig.getTakuSplash()
            val BANNER: String get() = dynamicConfig.getTakuBanner()
            val REWARD_VIDEO: String get() = dynamicConfig.getTakuRewardVideo()
            val INTERSTITIAL: String get() = dynamicConfig.getTakuInterstitial()
            val FEED: String get() = dynamicConfig.getTakuFeed()
            val DRAW_VIDEO: String get() = dynamicConfig.getTakuDrawVideo()
        }
        
        // 广告配置参数
        object Config {
            const val DEBUG_MODE = true  // 测试环境设为true，正式环境设为false
            const val SPLASH_TIMEOUT_MS = 5000L  // 开屏广告超时时间（毫秒）
            const val ENABLE_PERSONALIZED_AD = true  // 是否启用个性化广告
        }
    }
    
    // 其他广告平台配置（预留）
    object Other {
        // TODO: 可以在这里添加其他广告平台的配置
        // 例如：腾讯优量汇等
    }
    
    // 广告展示策略配置
    object Strategy {
        const val SPLASH_AD_INTERVAL_HOURS = 1 // 开屏广告间隔时间（小时）
        const val INTERSTITIAL_AD_INTERVAL_MINUTES = 30 // 插屏广告间隔时间（分钟）
        const val INTERSTITIAL_AD_INTERVAL_SECONDS = 5 * 60 // 插屏广告间隔时间（秒）
        const val REWARD_VIDEO_AD_MAX_COUNT_PER_DAY = 5 // 激励视频广告每日最大观看次数
        const val REWARD_VIDEO_DAILY_LIMIT = 5 // 激励视频广告每日限制次数（别名）
        const val FEED_AD_INTERVAL_MINUTES = 0 // 信息流广告展示间隔（分钟）
        const val BANNER_AD_INTERVAL_MINUTES = 0 // Banner广告展示间隔（分钟）
        const val FEED_AD_INSERT_INTERVAL = 3 // 信息流广告插入间隔（每N个内容项插入一个广告）
    }
    
    // 全局配置参数
    const val DEBUG_MODE = true  // 全局调试模式
    
    // Taku配置便捷访问方法
    fun getTakuConfig(): Map<String, Any> {
        return mapOf(
            "APP_ID" to Taku.APP_ID,
            "APP_KEY" to Taku.APP_KEY,
            "APP_NAME" to Taku.APP_NAME
        )
    }
    
    fun getTakuSplashAdUnitId(): String = Taku.AdUnitId.SPLASH
    fun getTakuBannerAdUnitId(): String = Taku.AdUnitId.BANNER
    fun getTakuRewardVideoAdUnitId(): String = Taku.AdUnitId.REWARD_VIDEO
    fun getTakuInterstitialAdUnitId(): String = Taku.AdUnitId.INTERSTITIAL
    fun getTakuFeedAdUnitId(): String = Taku.AdUnitId.FEED
    fun getTakuDrawVideoAdUnitId(): String = Taku.AdUnitId.DRAW_VIDEO
}