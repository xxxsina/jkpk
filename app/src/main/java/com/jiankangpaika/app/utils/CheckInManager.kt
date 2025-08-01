package com.jiankangpaika.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.jiankangpaika.app.utils.constants.ApiConfig
import com.jiankangpaika.app.data.model.AddScoreResponse
import com.jiankangpaika.app.utils.NetworkUtils
import com.jiankangpaika.app.utils.NetworkResult
import com.jiankangpaika.app.utils.UserManager
import com.google.gson.Gson
import com.jiankangpaika.app.data.model.CheckInInitResponse
import com.jiankangpaika.app.data.model.CheckInInitResult
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * 签到管理工具类
 * 负责签到数据的本地存储和API调用
 */
object CheckInManager {
    private const val TAG = "CheckInManager"
    private const val PREF_NAME = "checkin_preferences"
    
    // SharedPreferences键名
    private const val KEY_TODAY_CHECKIN_COUNT = "today_checkin_count_"
    private const val KEY_LAST_CHECKIN_DATE = "last_checkin_date"
    private const val KEY_TOTAL_SCORE = "total_score"
    private const val KEY_CHECKIN_DATES = "checkin_dates"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    /**
     * 获取SharedPreferences实例
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取今日签到次数
     */
    fun getTodayCheckInCount(context: Context): Int {
        val today = getCurrentDateString()
        return getPreferences(context).getInt(KEY_TODAY_CHECKIN_COUNT + today, 0)
    }
    
    /**
     * 获取最大签到次数
     */
    fun getMaxCheckInPerDay(): Int {
        return maxCheckInPerDay
    }
    
    /**
     * 刷新签到初始化数据
     */
    suspend fun refreshCheckInInitData(context: Context, userIdOverride: String? = null): CheckInInitResult {
        return getCheckInInitData(context, userIdOverride)
    }
    
    /**
     * 刷新日历数据（保持兼容性）
     */
    suspend fun refreshCalendarData(context: Context): CalendarResult {
        // 调用新的CHECK_IN_INIT接口并转换为CalendarResult
        return when (val initResult = getCheckInInitData(context)) {
            is CheckInInitResult.Success -> {
                CalendarResult.Success(
                    todayCheckinCount = initResult.todayCheckinCount,
                    maxCheckinPerDay = initResult.maxCheckinPerDay,
                    currentScore = 0, // CHECK_IN_INIT接口不返回总分数
                    calendarData = emptyMap(), // 取消日历功能
                    noticeMessage = initResult.noticeMessage
                )
            }
            is CheckInInitResult.Error -> CalendarResult.Error(initResult.message)
        }
    }
    
    /**
     * 刷新指定年月的日历数据（保持兼容性）
     */
    suspend fun refreshCalendarData(context: Context, year: Int, month: Int): CalendarResult {
        return refreshCalendarData(context)
    }
    
    /**
     * 强制刷新日历数据（忽略时间间隔限制）
     */
    suspend fun forceRefreshCalendarData(context: Context): CalendarResult = withContext(Dispatchers.IO) {
        try {
            val userId = UserManager.getUserId(context)
            if (userId.isNullOrEmpty()) {
                return@withContext CalendarResult.Error("用户未登录")
            }
            
            val requestData = JSONObject().apply {
                put("user_id", userId)
                put("field", SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()))
            }
            
            Log.d(TAG, "📅 [日历] 强制刷新请求参数: $requestData")
            
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.GET_CALENDAR, requestData)
            
            when (result) {
                is NetworkResult.Success -> {
                    try {
                        val responseJson = JSONObject(result.data)
                        val code = responseJson.getInt("code")
                        
                        if (code == 200) {
                            val data = responseJson.getJSONObject("data")
                            val todayCheckinCount = data.getInt("today_checkin_count")
                            val maxCheckinPerDay = data.getInt("max_checkin_per_day")
                            val currentScore = data.getInt("new_score")  // 修复：使用new_score字段获取用户总分数
                            val calendarData = data.getJSONObject("calendar_data")
                            
                            // 更新最大签到次数和API调用时间
                            this@CheckInManager.maxCheckInPerDay = maxCheckinPerDay
                            lastApiCallTime = System.currentTimeMillis()
                            
                            // 转换calendar_data为Map
                            val calendarMap = mutableMapOf<String, Any>()
                            calendarData.keys().forEach { key ->
                                val value = calendarData.get(key)
                                calendarMap[key] = value
                                Log.d(TAG, "🔍 [API数据] 日期: $key, 值: $value, 类型: ${value.javaClass.simpleName}")
                            }
                            
                            // 获取当前年月
            val currentYearMonth = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
            
            // 更新本地数据
            updateLocalCalendarData(context, todayCheckinCount, currentScore, calendarMap, currentYearMonth)
                            
                            Log.i(TAG, "✅ [日历] 强制刷新成功: 今日签到${todayCheckinCount}次, 上限${maxCheckinPerDay}次")
                            
                            CalendarResult.Success(
                                todayCheckinCount = todayCheckinCount,
                                maxCheckinPerDay = maxCheckinPerDay,
                                currentScore = currentScore,
                                calendarData = calendarMap
                            )
                        } else {
                            val message = responseJson.getString("message")
                            Log.w(TAG, "⚠️ [日历] 强制刷新失败: $message")
                            CalendarResult.Error(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [日历] 强制刷新解析失败: ${e.message}")
                        CalendarResult.Error("日历数据解析失败")
                    }
                }
                is NetworkResult.Error -> {
                    Log.w(TAG, "⚠️ [日历] 强制刷新网络失败: ${result.message}")
                    try {
                        val errorJson = JSONObject(result.message)
                        val errorMessage = errorJson.optString("message", "获取日历数据失败")
                        CalendarResult.Error(errorMessage)
                    } catch (e: Exception) {
                        CalendarResult.Error(result.message)
                    }
                }
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [日历] 强制刷新网络异常: ${result.exception.message}")
                    CalendarResult.Error("网络异常，请稍后重试")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [日历] 强制刷新时发生异常: ${e.message}")
            CalendarResult.Error("获取日历数据失败，请稍后重试")
        }
    }
    
    // 动态获取的每日签到上限次数
    private var maxCheckInPerDay: Int = 10
    
    // 最后一次访问接口的时间
    private var lastApiCallTime: Long = 0
    
    // 接口访问间隔（5分钟）
    private const val API_CALL_INTERVAL = 5 * 60 * 1000L
    
    /**
     * 获取总积分
     */
    fun getTotalScore(context: Context): Int {
        return getPreferences(context).getInt(KEY_TOTAL_SCORE, 0)
    }
    
    /**
     * 更新总积分
     */
    fun updateTotalScore(context: Context, score: Int) {
        with(getPreferences(context).edit()) {
            putInt(KEY_TOTAL_SCORE, score)
            apply()
        }
    }
    
    /**
     * 检查今日是否可以签到
     */
    fun canCheckInToday(context: Context): Boolean {
        return getTodayCheckInCount(context) < getMaxCheckInPerDay()
    }
    
    /**
     * 执行签到API调用
     */
    suspend fun performCheckIn(context: Context): CheckInResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🎯 [签到] 开始执行签到")
                
                // 检查是否可以签到
                if (!canCheckInToday(context)) {
                    return@withContext CheckInResult.Error("今日签到次数已达上限")
                }
                
                // 获取用户ID和token
                val userId = UserManager.getUserId(context)
                val token = UserManager.getToken(context)
                
                if (userId.isNullOrEmpty() || token.isNullOrEmpty()) {
                    return@withContext CheckInResult.Error("用户未登录")
                }
                
                // 构建请求参数
                val requestData = mapOf(
                    "user_id" to userId.toInt()
                )
                
                // 先调用get_calendar.php获取最新数据
                val calendarResult = getCalendarData(context)
                when (calendarResult) {
                    is CalendarResult.Success -> {
                        // 更新最大签到次数
                        maxCheckInPerDay = calendarResult.maxCheckinPerDay
                        
                        // 检查是否可以签到（使用最新的限制）
                        if (calendarResult.todayCheckinCount >= maxCheckInPerDay) {
                            return@withContext CheckInResult.Error("今日签到次数已达上限(${maxCheckInPerDay}次)")
                        }
                        
                        // 调用签到API
                        val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.CHECK_IN, requestData)
                        
                        when (result) {
                            is NetworkResult.Success -> {
                                try {
                                    val responseJson = JSONObject(result.data)
                                    val code = responseJson.getInt("code")
                                    
                                    if (code == 200) {
                                        val data = responseJson.getJSONObject("data")
                                        val scoreEarned = data.getInt("score_earned")
                                        val newScore = data.getInt("new_score")
                                        val todayCheckinCount = data.getInt("today_checkin_count")
                                        val checkinTime = data.getString("checkin_time")
                                        
                                        // 更新本地数据
                                        updateLocalCheckInData(context, todayCheckinCount, newScore)
                                        
                                        Log.i(TAG, "✅ [签到] 签到成功: 获得${scoreEarned}积分")
                                        
                                        CheckInResult.Success(
                                            scoreEarned = scoreEarned,
                                            newScore = newScore,
                                            todayCount = todayCheckinCount,
                                            maxCount = maxCheckInPerDay,
                                            checkinTime = checkinTime
                                        )
                                    } else {
                                        val message = responseJson.getString("message")
                                        Log.w(TAG, "⚠️ [签到] 签到失败: $message")
                                        CheckInResult.Error(message)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ [签到] 解析响应失败: ${e.message}")
                                    CheckInResult.Error("签到响应解析失败")
                                }
                            }
                            is NetworkResult.Error -> {
                                Log.w(TAG, "⚠️ [签到] 网络请求失败: ${result.message}")
                                try {
                                    val errorJson = JSONObject(result.message)
                                    val errorMessage = errorJson.optString("message", "签到失败")
                                    CheckInResult.Error(errorMessage)
                                } catch (e: Exception) {
                                    CheckInResult.Error(result.message)
                                }
                            }
                            is NetworkResult.Exception -> {
                                Log.e(TAG, "💥 [签到] 网络请求异常: ${result.exception.message}")
                                CheckInResult.Error("网络异常，请稍后重试")
                            }
                        }
                    }
                    is CalendarResult.Error -> {
                        Log.w(TAG, "⚠️ [签到] 获取日历数据失败: ${calendarResult.message}")
                        // 如果获取日历数据失败，使用默认值继续签到流程
                        val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.CHECK_IN, requestData)
                        
                        when (result) {
                            is NetworkResult.Success -> {
                                try {
                                    val responseJson = JSONObject(result.data)
                                    val code = responseJson.getInt("code")
                                    
                                    if (code == 200) {
                                        val data = responseJson.getJSONObject("data")
                                        val scoreEarned = data.getInt("score_earned")
                                        val newScore = data.getInt("new_score")
                                        val todayCheckinCount = data.getInt("today_checkin_count")
                                        val maxCheckinPerDay = data.getInt("max_checkin_per_day")
                                        val checkinTime = data.getString("checkin_time")
                                        
                                        // 更新本地数据，包括max_checkin_per_day
                                        updateLocalCheckInData(context, todayCheckinCount, newScore)
                                        maxCheckInPerDay = maxCheckinPerDay
                                        
                                        Log.i(TAG, "✅ [签到] 签到成功: 获得${scoreEarned}积分")
                                        
                                        CheckInResult.Success(
                                            scoreEarned = scoreEarned,
                                            newScore = newScore,
                                            todayCount = todayCheckinCount,
                                            maxCount = maxCheckinPerDay,
                                            checkinTime = checkinTime
                                        )
                                    } else {
                                        val message = responseJson.getString("message")
                                        Log.w(TAG, "⚠️ [签到] 签到失败: $message")
                                        CheckInResult.Error(message)
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ [签到] 解析响应失败: ${e.message}")
                                    CheckInResult.Error("签到响应解析失败")
                                }
                            }
                            is NetworkResult.Error -> {
                                Log.w(TAG, "⚠️ [签到] 网络请求失败: ${result.message}")
                                try {
                                    val errorJson = JSONObject(result.message)
                                    val errorMessage = errorJson.optString("message", "签到失败")
                                    CheckInResult.Error(errorMessage)
                                } catch (e: Exception) {
                                    CheckInResult.Error(result.message)
                                }
                            }
                            is NetworkResult.Exception -> {
                                Log.e(TAG, "💥 [签到] 网络请求异常: ${result.exception.message}")
                                CheckInResult.Error("网络异常，请稍后重试")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💥 [签到] 签到异常: ${e.message}", e)
                CheckInResult.Error("签到失败，请稍后重试")
            }
        }
    }
    
    /**
     * 获取签到初始化数据
     */
    private suspend fun getCheckInInitData(context: Context, userIdOverride: String? = null): CheckInInitResult = withContext(Dispatchers.IO) {
        try {
            val userId = userIdOverride ?: UserManager.getUserId(context) ?: "0"
            
            val requestData = JSONObject().apply {
                put("user_id", userId)
            }
            
            Log.d(TAG, "🔄 [签到初始化] 请求参数: $requestData")
            
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.CHECK_IN_INIT, requestData)
            
            when (result) {
                is NetworkResult.Success -> {
                    try {
                        val responseJson = JSONObject(result.data)
                        val code = responseJson.getInt("code")
                        
                        if (code == 200) {
                            val data = responseJson.getJSONObject("data")
                            val userId = data.getInt("user_id")
                            val scoreEarned = data.getInt("score_earned")
                            val scoreAgainMore = data.getInt("score_again_more")
                            val todayCheckinCount = data.getInt("today_checkin_count")
                            val maxCheckinPerDay = data.getInt("max_checkin_per_day")
                            val maxScoreAgainMore = data.getInt("max_score_again_more")
                            val todayScoreAgainMore = data.getInt("today_score_again_more")
                            val noticeMessage = data.optString("notice_message", null)
                            
                            // 更新本地maxCheckInPerDay
                            this@CheckInManager.maxCheckInPerDay = maxCheckinPerDay
                            
                            Log.i(TAG, "✅ [签到初始化] 获取数据成功: 今日签到${todayCheckinCount}次, 上限${maxCheckinPerDay}次, 赚取更多积分已获得${scoreAgainMore}次, 上限${maxScoreAgainMore}次, 今日已赚取${todayScoreAgainMore}次")
                            
                            CheckInInitResult.Success(
                                userId = userId,
                                scoreEarned = scoreEarned,
                                scoreAgainMore = scoreAgainMore,
                                todayCheckinCount = todayCheckinCount,
                                maxCheckinPerDay = maxCheckinPerDay,
                                maxScoreAgainMore = maxScoreAgainMore,
                                todayScoreAgainMore = todayScoreAgainMore,
                                noticeMessage = noticeMessage
                            )
                        } else {
                            val message = responseJson.getString("message")
                            Log.w(TAG, "⚠️ [签到初始化] 获取数据失败: $message")
                            CheckInInitResult.Error(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [签到初始化] 解析响应失败: ${e.message}")
                        CheckInInitResult.Error("签到初始化数据解析失败")
                    }
                }
                is NetworkResult.Error -> {
                    Log.w(TAG, "⚠️ [签到初始化] 网络请求失败: ${result.message}")
                    try {
                        val errorJson = JSONObject(result.message)
                        val errorMessage = errorJson.optString("message", "获取签到初始化数据失败")
                        CheckInInitResult.Error(errorMessage)
                    } catch (e: Exception) {
                        CheckInInitResult.Error(result.message)
                    }
                }
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [签到初始化] 网络请求异常: ${result.exception.message}")
                    CheckInInitResult.Error("网络异常，请稍后重试")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [签到初始化] 获取数据异常: ${e.message}", e)
            CheckInInitResult.Error("获取签到初始化数据失败，请稍后重试")
        }
    }
    
    /**
     * 获取日历数据（保持兼容性）
     */
    private suspend fun getCalendarData(context: Context): CalendarResult = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return@withContext getCalendarData(context, year, month)
    }
    
    /**
     * 获取指定年月的日历数据
     */
    private suspend fun getCalendarData(context: Context, year: Int, month: Int): CalendarResult = withContext(Dispatchers.IO) {
        try {
            val currentTime = System.currentTimeMillis()
            
            // 检查是否需要调用API（5分钟间隔限制）
//            if (currentTime - lastApiCallTime < API_CALL_INTERVAL) {
//                Log.d(TAG, "📅 [日历] API调用间隔未到，跳过请求")
//                return@withContext CalendarResult.Error("请求过于频繁")
//            }
            
            val userId = UserManager.getUserId(context)
            if (userId.isNullOrEmpty()) {
                return@withContext CalendarResult.Error("用户未登录")
            }
            
            val fieldValue = String.format("%04d%02d", year, month)
            val requestData = JSONObject().apply {
                put("user_id", userId)
                put("field", fieldValue)
            }
            
            Log.d(TAG, "📅 [日历] 请求参数: $requestData")
            
            val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.GET_CALENDAR, requestData)
            
            when (result) {
                is NetworkResult.Success -> {
                    try {
                        val responseJson = JSONObject(result.data)
                        val code = responseJson.getInt("code")
                        
                        if (code == 200) {
                            val data = responseJson.getJSONObject("data")
                            val todayCheckinCount = data.getInt("today_checkin_count")
                            val maxCheckinPerDay = data.getInt("max_checkin_per_day")
                            val currentScore = data.getInt("new_score")  // 修复：使用new_score字段获取用户总分数
                            val calendarData = data.getJSONObject("calendar_data")
                            val noticeMessage = data.optString("notice_message", null)
                            
                            // 更新API调用时间和本地maxCheckInPerDay
                            lastApiCallTime = currentTime
                            maxCheckInPerDay = maxCheckinPerDay
                            
                            // 转换calendar_data为Map
                            val calendarMap = mutableMapOf<String, Any>()
                            calendarData.keys().forEach { key ->
                                calendarMap[key] = calendarData.get(key)
                            }
                            
                            // 获取请求的年月
                            val requestYearMonth = String.format("%04d%02d", year, month)
                            
                            // 更新本地数据
                            updateLocalCalendarData(context, todayCheckinCount, currentScore, calendarMap, requestYearMonth)
                            
                            Log.i(TAG, "✅ [日历] 获取日历数据成功: 今日签到${todayCheckinCount}次, 上限${maxCheckinPerDay}次")
                            
                            CalendarResult.Success(
                                todayCheckinCount = todayCheckinCount,
                                maxCheckinPerDay = maxCheckinPerDay,
                                currentScore = currentScore,
                                calendarData = calendarMap,
                                noticeMessage = noticeMessage
                            )
                        } else {
                            val message = responseJson.getString("message")
                            Log.w(TAG, "⚠️ [日历] 获取日历数据失败: $message")
                            CalendarResult.Error(message)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ [日历] 解析响应失败: ${e.message}")
                        CalendarResult.Error("日历数据解析失败")
                    }
                }
                is NetworkResult.Error -> {
                    Log.w(TAG, "⚠️ [日历] 网络请求失败: ${result.message}")
                    try {
                        val errorJson = JSONObject(result.message)
                        val errorMessage = errorJson.optString("message", "获取日历数据失败")
                        CalendarResult.Error(errorMessage)
                    } catch (e: Exception) {
                        CalendarResult.Error(result.message)
                    }
                }
                is NetworkResult.Exception -> {
                    Log.e(TAG, "💥 [日历] 网络请求异常: ${result.exception.message}")
                    CalendarResult.Error("网络异常，请稍后重试")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 [日历] 获取日历数据时发生异常: ${e.message}")
            CalendarResult.Error("获取日历数据失败，请稍后重试")
        }
    }
    
    /**
     * 更新本地签到数据
     */
    private fun updateLocalCheckInData(context: Context, todayCount: Int, totalScore: Int) {
        val today = getCurrentDateString()
        
        with(getPreferences(context).edit()) {
            putInt(KEY_TODAY_CHECKIN_COUNT + today, todayCount)
            putString(KEY_LAST_CHECKIN_DATE, today)
            putInt(KEY_TOTAL_SCORE, totalScore)
            
            // 更新签到日期集合
            val checkedInDates = getCheckedInDates(context).toMutableSet()
            checkedInDates.add(today)
            putStringSet(KEY_CHECKIN_DATES, checkedInDates)
            
            apply()
        }
        
        Log.d(TAG, "💾 [数据更新] 本地签到数据更新成功")
    }
    
    /**
     * 更新本地日历数据
     */
    private fun updateLocalCalendarData(context: Context, todayCount: Int, currentScore: Int, calendarData: Map<String, Any>, yearMonth: String) {
        val today = getCurrentDateString()
        
        // 从日历数据中提取已签到日期和签到次数
        val checkedInDates = mutableSetOf<String>()
        val editor = getPreferences(context).edit()
        
        calendarData.forEach { (date, value) ->
            try {
                when (value) {
                    is org.json.JSONObject -> {
                        // 新的数据结构：{"count": 3, "is_complete": 1}
                        val count = value.optInt("count", 0)
                        val isComplete = value.optInt("is_complete", 0)
                        
                        if (count > 0) {
                            val fullDateString = convertToFullDateString(date, yearMonth)
                            checkedInDates.add(fullDateString)
                            editor.putInt("today_checkin_count_$fullDateString", count)
                            editor.putInt("today_checkin_complete_$fullDateString", isComplete)
                            Log.d(TAG, "📝 [存储签到数据] 日期: $date -> $fullDateString, 次数: $count, 完成状态: $isComplete")
                        }
                    }
                    is Int -> {
                        // 兼容旧的数据结构：直接是次数
                        if (value > 0) {
                            val fullDateString = convertToFullDateString(date, yearMonth)
                            checkedInDates.add(fullDateString)
                            editor.putInt("today_checkin_count_$fullDateString", value)
                            editor.putInt("today_checkin_complete_$fullDateString", 0) // 默认未完成
                            Log.d(TAG, "📝 [存储签到次数] 日期: $date -> $fullDateString, 次数: $value (兼容模式)")
                        }
                    }
                    else -> {
                        Log.w(TAG, "⚠️ [数据格式] 未知的日历数据格式: $date -> $value (${value.javaClass.simpleName})")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ [数据解析] 解析日历数据失败: $date -> $value, 错误: ${e.message}")
            }
        }
        
        editor.putInt(KEY_TODAY_CHECKIN_COUNT + today, todayCount)
        editor.putInt(KEY_TOTAL_SCORE, currentScore)
        editor.putStringSet(KEY_CHECKIN_DATES, checkedInDates)
        editor.apply()
        
        Log.d(TAG, "📝 [日历] 本地日历数据已更新: 今日签到${todayCount}次, 总积分${currentScore}, 已签到日期${checkedInDates.size}天")
    }
    
    /**
     * 获取所有已签到的日期
     */
    fun getCheckedInDates(context: Context): Set<String> {
        return getPreferences(context).getStringSet(KEY_CHECKIN_DATES, emptySet()) ?: emptySet()
    }
    
    /**
     * 检查指定日期是否已签到
     */
    fun isCheckedIn(context: Context, date: String): Boolean {
        return getCheckedInDates(context).contains(date)
    }
    

    
    /**
     * 将API返回的日期格式转换为完整的日期字符串
     * @param dayString API返回的日期字符串（如 "1", "8", "26"）
     * @param yearMonth 年月字符串（如 "202506"）
     * @return 完整的日期字符串（如 "2025-06-01", "2025-06-08", "2025-06-26"）
     */
    private fun convertToFullDateString(dayString: String, yearMonth: String): String {
        // 解析年月字符串
        val year = yearMonth.substring(0, 4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val month = yearMonth.substring(4, 6).toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
        
        // 将日期字符串转换为整数
        val day = dayString.toIntOrNull() ?: 1
        
        // 格式化为完整的日期字符串
        return String.format("%04d-%02d-%02d", year, month, day)
    }
    
    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * 更新今日统计数据（在点击签到栏目时调用）
     */
    suspend fun updateTodayStats(context: Context): CalendarSyncResult {
        return try {
            val calendarResult = getCalendarData(context)
            when (calendarResult) {
                is CalendarResult.Success -> {
                    // 获取当前年月
                    val currentYearMonth = SimpleDateFormat("yyyyMM", Locale.getDefault()).format(Date())
                    
                    // 更新本地数据
                    updateLocalCalendarData(
                        context,
                        calendarResult.todayCheckinCount,
                        calendarResult.currentScore,
                        calendarResult.calendarData,
                        currentYearMonth
                    )
                    
                    CalendarSyncResult.Success(
                        todayCheckinCount = calendarResult.todayCheckinCount,
                        maxCheckinPerDay = calendarResult.maxCheckinPerDay,
                        currentScore = calendarResult.currentScore,
                        calendarData = calendarResult.calendarData,
                        noticeMessage = calendarResult.noticeMessage
                    )
                }
                is CalendarResult.Error -> {
                    CalendarSyncResult.Error(calendarResult.message)
                }
            }
        } catch (e: Exception) {
            CalendarSyncResult.Error("数据同步失败: ${e.message}")
        }
    }
    
    /**
     * 调用积分增加接口
     * @param context 上下文
     * @param type 积分类型（如 "score_again_more"）
     * @return AddScoreResult 积分增加结果
     */
    suspend fun addScore(context: Context, type: String): AddScoreResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "💰 [积分] 开始增加积分，类型: $type")
                
                // 获取用户ID
                val userId = UserManager.getUserId(context)
                if (userId.isNullOrEmpty()) {
                    return@withContext AddScoreResult.Error("用户未登录")
                }
                
                // 构建请求参数
                val requestData = JSONObject().apply {
                    put("user_id", userId)
                    put("type", type)
                }
                
                Log.d(TAG, "💰 [积分] 请求参数: $requestData")
                
                // 调用API
                val result = NetworkUtils.postJsonWithAuth(context, ApiConfig.User.ADD_SCORE, requestData)
                
                when (result) {
                    is NetworkResult.Success -> {
                        try {
                            val gson = Gson()
                            val response = gson.fromJson(result.data, AddScoreResponse::class.java)
                            
                            if (response.code == 200) {
                                val scoreAdded = response.data.score_added
                                val newTotalScore = response.data.new_score
                                
                                // 更新本地总积分
                                with(getPreferences(context).edit()) {
                                    putInt(KEY_TOTAL_SCORE, newTotalScore)
                                    apply()
                                }
                                
                                Log.d(TAG, "💰 [积分] 积分增加成功: +$scoreAdded, 新总分: $newTotalScore")
                                
                                AddScoreResult.Success(
                                    scoreAdded = scoreAdded,
                                    newTotalScore = newTotalScore
                                )
                            } else {
                                Log.e(TAG, "💰 [积分] 积分增加失败: ${response.message}")
                                AddScoreResult.Error(response.message)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "💰 [积分] 解析响应数据异常: ${e.message}")
                            AddScoreResult.Error("数据解析失败")
                        }
                    }
                    is NetworkResult.Error -> {
                        Log.e(TAG, "💰 [积分] 网络请求失败: ${result.message}")
                        AddScoreResult.Error(result.message)
                    }
                    is NetworkResult.Exception -> {
                        Log.e(TAG, "💰 [积分] 网络请求异常: ${result.exception.message}")
                        AddScoreResult.Error("网络异常，请稍后重试")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "💰 [积分] 增加积分时发生异常: ${e.message}")
                AddScoreResult.Error("积分增加失败，请稍后重试")
            }
        }
    }
    
    /**
     * 清除所有签到数据（用于测试）
     */
    fun clearAllData(context: Context) {
        with(getPreferences(context).edit()) {
            clear()
            apply()
        }
        Log.d(TAG, "🧹 [数据清理] 签到数据清除成功")
    }
}

/**
 * 签到结果密封类
 */
sealed class CheckInResult {
        data class Success(
            val scoreEarned: Int,
            val newScore: Int,
            val todayCount: Int,
            val maxCount: Int,
            val checkinTime: String
        ) : CheckInResult()
        
        data class Error(val message: String) : CheckInResult()
    }
    
    sealed class CalendarResult {
        data class Success(
            val todayCheckinCount: Int,
            val maxCheckinPerDay: Int,
            val currentScore: Int,
            val calendarData: Map<String, Any>,
            val noticeMessage: String? = null
        ) : CalendarResult()
        
        data class Error(val message: String) : CalendarResult()
    }

/**
 * 日历同步结果密封类
 */
sealed class CalendarSyncResult {
    data class Success(
        val todayCheckinCount: Int,
        val maxCheckinPerDay: Int,
        val currentScore: Int,
        val calendarData: Map<String, Any>,
        val noticeMessage: String? = null
    ) : CalendarSyncResult()
    
    data class Error(val message: String) : CalendarSyncResult()
}

/**
 * 积分增加结果密封类
 */
sealed class AddScoreResult {
    data class Success(
        val scoreAdded: Int,
        val newTotalScore: Int
    ) : AddScoreResult()
    
    data class Error(val message: String) : AddScoreResult()
}