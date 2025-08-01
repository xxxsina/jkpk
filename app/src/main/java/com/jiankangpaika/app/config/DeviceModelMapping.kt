package com.jiankangpaika.app.config

/**
 * 设备型号映射配置
 * 将Build.MODEL的内部型号转换为用户友好的营销名称
 * 
 * 数据来源：
 * - GitHub开源项目：MobileModels
 * - 各厂商官方文档和系统属性
 * - 社区收集的设备信息
 * 
 * 最后更新时间：2024年12月
 * 数据版本：v1.0
 */
object DeviceModelMapping {
    
    /**
     * 设备型号映射表
     * Key: Build.MODEL (内部型号)
     * Value: 营销名称
     */
    val deviceModelMapping: Map<String, String> = mapOf(
        // 小米/Redmi/POCO 设备映射表
        // 最新机型（2024-2025）
        "24117RK2CC" to "REDMI K80",
        "24117RK2PC" to "REDMI K80 Pro",
        "2412DRT0AC" to "Xiaomi 15",
        "2412DRT0CC" to "Xiaomi 15 Pro",
        "24091PN0DC" to "Xiaomi 14T",
        "24091PN0EC" to "Xiaomi 14T Pro",
        "2405CPX3DG" to "REDMI K70 Ultra",
        "24031PN0DC" to "REDMI Note 13 Pro+ 5G",
        "24031PN0DG" to "REDMI Note 13 Pro 5G",
        "24031PN0DI" to "REDMI Note 13 5G",
        
        // 2023年机型
        "23117RK66C" to "REDMI K70",
        "23117RK66G" to "REDMI K70 Pro",
        "23090RA98C" to "Xiaomi 14",
        "23116PN5BC" to "Xiaomi 14 Pro",
        "23078RKD5C" to "REDMI K60 Ultra",
        "23013RK75C" to "REDMI K60",
        "22127RK46C" to "REDMI K60 Pro",
        "2304FPN6DG" to "REDMI Note 12 Turbo",
        "22101316G" to "REDMI Note 12 Pro+ 5G",
        "22101316C" to "REDMI Note 12 Pro 5G",
        "22101316UCP" to "REDMI Note 12 5G",
        
        // 2022年机型
        "22081212C" to "Xiaomi 12S Ultra",
        "2206123SC" to "Xiaomi 12S Pro",
        "2203121C" to "Xiaomi 12S",
        "2211133C" to "Xiaomi 12T Pro",
        "22071212AG" to "Xiaomi 12T",
        "21121210C" to "Xiaomi 12",
        "2201123G" to "Xiaomi 12 Pro",
        "2112123AC" to "Xiaomi 12X",
        "22041216C" to "REDMI K50",
        "22041216G" to "REDMI K50 Pro",
        "22041216UC" to "REDMI K50 Gaming",
        "2201116SG" to "REDMI Note 11T Pro",
        "2201116SC" to "REDMI Note 11T Pro+",
        "2201117SY" to "REDMI Note 11 Pro",
        "2201117SG" to "REDMI Note 11 Pro+ 5G",
        
        // 华为/荣耀 设备映射表
        // 华为 Mate 系列
        "LIO-AL00" to "HUAWEI Mate 40",
        "NOH-AL00" to "HUAWEI Mate 40 Pro",
        "NOP-AL00" to "HUAWEI Mate 40 Pro+",
        "OCE-AL00" to "HUAWEI Mate 40E",
        "TAS-AL00" to "HUAWEI Mate 30",
        "LIO-AL00" to "HUAWEI Mate 30 Pro",
        "MRR-AL00" to "HUAWEI Mate 30 RS",
        
        // 华为 P 系列
        "ELS-AN00" to "HUAWEI P40",
        "ELS-TN00" to "HUAWEI P40 Pro",
        "ELS-AN10" to "HUAWEI P40 Pro+",
        "ANA-AN00" to "HUAWEI P40 lite",
        "ELE-AL00" to "HUAWEI P30",
        "VOG-AL00" to "HUAWEI P30 Pro",
        "MAR-AL00" to "HUAWEI P30 lite",
        
        // 荣耀系列
        "BMH-AN10" to "HONOR Magic4 Pro",
        "BMH-AN20" to "HONOR Magic4",
        "BMH-AN40" to "HONOR Magic4 Ultimate",
        "NTN-AL00" to "HONOR Magic3",
        "NTN-AL20" to "HONOR Magic3 Pro",
        "NTN-AL40" to "HONOR Magic3 Pro+",
        "TNY-AL00" to "HONOR 50",
        "NTH-AN00" to "HONOR 50 Pro",
        
        // OPPO 设备映射表
        // Find 系列
        "CPH2451" to "OPPO Find X6 Pro",
        "CPH2449" to "OPPO Find X6",
        "CPH2371" to "OPPO Find X5 Pro",
        "CPH2307" to "OPPO Find X5",
        "CPH2173" to "OPPO Find X3 Pro",
        "CPH2127" to "OPPO Find X3",
        "CPH2025" to "OPPO Find X2 Pro",
        "CPH2023" to "OPPO Find X2",
        
        // Reno 系列
        "CPH2487" to "OPPO Reno10 Pro+",
        "CPH2485" to "OPPO Reno10 Pro",
        "CPH2481" to "OPPO Reno10",
        "CPH2413" to "OPPO Reno9 Pro+",
        "CPH2411" to "OPPO Reno9 Pro",
        "CPH2409" to "OPPO Reno9",
        "CPH2321" to "OPPO Reno8 Pro+",
        "CPH2319" to "OPPO Reno8 Pro",
        "CPH2317" to "OPPO Reno8",
        
        // vivo 设备映射表
        // X 系列
        "V2309A" to "vivo X100 Pro+",
        "V2307A" to "vivo X100 Pro",
        "V2305A" to "vivo X100",
        "V2266A" to "vivo X90 Pro+",
        "V2254A" to "vivo X90 Pro",
        "V2239A" to "vivo X90",
        "V2200A" to "vivo X80 Pro",
        "V2183A" to "vivo X80",
        "V2145A" to "vivo X70 Pro+",
        "V2134A" to "vivo X70 Pro",
        "V2117A" to "vivo X70",
        
        // S 系列
        "V2318A" to "vivo S18 Pro",
        "V2316A" to "vivo S18",
        "V2244A" to "vivo S17 Pro",
        "V2238A" to "vivo S17",
        "V2204A" to "vivo S16 Pro",
        "V2199A" to "vivo S16",
        "V2162A" to "vivo S15 Pro",
        "V2118A" to "vivo S15",
        
        // 三星 设备映射表
        // Galaxy S 系列
        "SM-S9280" to "Galaxy S24 Ultra",
        "SM-S9260" to "Galaxy S24+",
        "SM-S9210" to "Galaxy S24",
        "SM-S9180" to "Galaxy S23 Ultra",
        "SM-S9160" to "Galaxy S23+",
        "SM-S9110" to "Galaxy S23",
        "SM-S9080" to "Galaxy S22 Ultra",
        "SM-S9060" to "Galaxy S22+",
        "SM-S9010" to "Galaxy S22",
        
        // Galaxy Note 系列
        "SM-N9860" to "Galaxy Note20 Ultra",
        "SM-N9810" to "Galaxy Note20",
        "SM-N9750" to "Galaxy Note10+",
        "SM-N9700" to "Galaxy Note10",
        "SM-N9600" to "Galaxy Note9",
        "SM-N9500" to "Galaxy Note8",
        
        // 一加 设备映射表
        "CPH2581" to "OnePlus 12",
        "CPH2573" to "OnePlus 12R",
        "CPH2449" to "OnePlus 11",
        "CPH2413" to "OnePlus 10 Pro",
        "CPH2399" to "OnePlus 10T",
        "CPH2305" to "OnePlus 9 Pro",
        "CPH2293" to "OnePlus 9",
        "CPH2207" to "OnePlus 8T",
        "CPH2025" to "OnePlus 8 Pro",
        "CPH2023" to "OnePlus 8",
        
        // 魅族 设备映射表
        "M2392" to "魅族21 Pro",
        "M2391" to "魅族21",
        "M2331" to "魅族20 Pro",
        "M2321" to "魅族20",
        "M2211" to "魅族19 Pro",
        "M2201" to "魅族19",
        "M2182" to "魅族18s Pro",
        "M2171" to "魅族18s",
        "M2162" to "魅族18 Pro",
        "M2151" to "魅族18",
        
        // 兼容旧版本的映射（保持向后兼容）
        "M2012K11AC" to "REDMI K30S",
        "M2007J3SY" to "REDMI K30",
        "M1903F2A" to "REDMI K20",
        "M1805E2A" to "REDMI 6",
        "M1804C3DH" to "REDMI 6A",
        "SM-G9980" to "Galaxy S21",
        "SM-G998B" to "Galaxy S21 Ultra",
        "iPhone14,2" to "iPhone 13 Pro",
        "iPhone14,3" to "iPhone 13 Pro Max",
        "iPhone13,2" to "iPhone 12",
        "iPhone13,3" to "iPhone 12 Pro"
    )
    
    /**
     * 获取设备的营销名称
     * @param buildModel Build.MODEL的值
     * @return 对应的营销名称，如果没有找到则返回null
     */
    fun getMarketingName(buildModel: String): String? {
        return deviceModelMapping[buildModel]
    }
    
    /**
     * 检查是否包含指定的设备型号
     * @param buildModel Build.MODEL的值
     * @return 是否包含该设备型号的映射
     */
    fun containsModel(buildModel: String): Boolean {
        return deviceModelMapping.containsKey(buildModel)
    }
    
    /**
     * 获取所有支持的设备型号列表
     * @return 所有支持的Build.MODEL列表
     */
    fun getAllSupportedModels(): Set<String> {
        return deviceModelMapping.keys
    }
    
    /**
     * 获取映射表的大小
     * @return 映射表中包含的设备数量
     */
    fun getMappingSize(): Int {
        return deviceModelMapping.size
    }
}