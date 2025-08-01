package com.jiankangpaika.app.utils.constants

/**
 * API接口配置类
 * 统一管理所有接口地址
 */
object ApiConfig {
    
    /**
     * 服务器基础地址
     */
    private const val BASE_URL = "http://jiankangpaika.blcwg.com/jkpk"
    
    /**
     * 版本更新相关接口
     */
    object Version {
        /**
         * 版本检查接口
         */
        const val CHECK_UPDATE = "$BASE_URL/api/version_server.php"
        
        /**
         * APK下载基础路径
         */
        const val DOWNLOAD_BASE = "$BASE_URL/download.php"
    }

    /**
     * 广告配置相关接口
     */
    object Ad {
        /**
         * 获取广告开关配置
         */
        const val GET_SWITCH_CONFIG = "$BASE_URL/api/ad_config.php"
    }
    
    /**
     * 用户相关接口
     */
    object User {
        /**
         * 用户登录
         */
        const val LOGIN = "$BASE_URL/api/login.php"
        
        /**
         * 手机短信登录
         */
        const val LOGIN_MOBILE = "$BASE_URL/api/login_mobile.php"
        
        /**
         * 获取图形验证码
         */
        const val GET_CAPTCHA = "$BASE_URL/api/get_captcha.php"
        
        /**
         * 发送短信验证码
         */
        const val SEND_SMS = "$BASE_URL/api/send_sms.php"
        
        /**
         * 用户注册
         */
        const val REGISTER = "$BASE_URL/api/register.php"

        /**
         * 修改用户信息（昵称、头像、手机号、邮箱）
         */
        const val UPDATE_USER = "$BASE_URL/api/update_user.php"
        
        /**
         * 用户退出登录
         */
        const val LOGOUT = "$BASE_URL/api/logout.php"
        
        /**
         * 每日签到初始化数据
         */
        const val CHECK_IN_INIT = "$BASE_URL/api/check_in_init.php"

        /**
         * 用户签到
         */
        const val CHECK_IN = "$BASE_URL/api/check_in.php"

        /**
         * 加积分
         */
        const val ADD_SCORE = "$BASE_URL/api/add_score.php"

        /**
         * 获取签到日历
         */
        const val GET_CALENDAR = "$BASE_URL/api/get_calendar.php"
    }
    
    /**
     * 短信相关接口
     */
    object Sms {
        /**
         * 验证短信验证码
         */
        const val VERIFY_SMS = "$BASE_URL/api/verify_sms.php"
    }

    /**
     * 系统相关接口
     */
    object System {
        /**
         * 获取系统配置
         */
        const val GET_CONFIG = "$BASE_URL/api/system_config.php"
    }
    
    /**
     * 客服相关接口
     */
    object CustomerService {
        /**
         * 提交客服表单
         */
        const val SUBMIT_FORM = "$BASE_URL/api/customer_message.php"
        
        /**
         * 获取客服问题列表
         */
        const val GET_MESSAGES = "$BASE_URL/api/customer_message_list.php"
        
        /**
         * 获取用户最近的问题（签到页面用）
         */
        const val CHECK_IN_MESSAGE = "$BASE_URL/api/customer_message_checkin.php"
        
        /**
         * 修改客服消息状态
         */
        const val MODIFY_MESSAGE = "$BASE_URL/api/customer_message_modify.php"
    }
    
    /**
     * 每日任务相关接口
     */
    object DailyTask {
        /**
         * 获取每日任务列表
         */
        const val GET_LIST = "$BASE_URL/api/daily_task_list.php"
    }

    /**
     * 常见问题相关接口
     */
    object Question {
        /**
         * 获取常见问题列表
         */
        const val GET_QUESTION = "$BASE_URL/api/question_list.php"
    }

    /**
     * 文章相关接口
     */
    object Article {
        /**
         * 获取文章列表
         */
        const val GET_LIST = "$BASE_URL/api/article_list.php"
    }

    /**
     * 获取完整的URL地址
     * @param endpoint 接口端点
     * @return 完整的URL
     */
    fun getFullUrl(endpoint: String): String {
        return if (endpoint.startsWith("http")) {
            endpoint
        } else {
            "$BASE_URL/$endpoint"
        }
    }
    
    /**
     * 检查是否为有效的API地址
     * @param url 待检查的URL
     * @return 是否有效
     */
    fun isValidApiUrl(url: String): Boolean {
        return url.startsWith(BASE_URL) || url.startsWith("https://")
    }
}