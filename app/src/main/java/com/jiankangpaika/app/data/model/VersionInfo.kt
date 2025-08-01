package com.jiankangpaika.app.data.model

/**
 * 版本信息数据类
 * 用于解析服务器返回的版本更新信息
 */
data class VersionInfo(
    val versionCode: Int,           // 版本号
    val versionName: String,        // 版本名称
    val downloadUrl: String,        // 下载地址
    val updateMessage: String,      // 更新说明
    val forceUpdate: Boolean = false, // 是否强制更新
    val fileSize: String = "",     // 文件大小
    val updateTime: String = ""     // 更新时间
)

/**
 * 版本检查响应数据类
 * 用于解析服务器返回的完整响应
 */
data class VersionCheckResponse(
    val code: Int,                  // 响应码
    val message: String,            // 响应消息
    val data: VersionInfo?          // 版本信息数据
)