package com.jiankangpaika.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

class CheckInPreferences(context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    companion object {
        private const val PREF_NAME = "checkin_preferences"
        private const val KEY_CHECKIN_DATES = "checkin_dates"
        private const val KEY_LAST_CHECKIN_DATE = "last_checkin_date"
        private const val KEY_TOTAL_CHECKIN_DAYS = "total_checkin_days"
        private const val KEY_CONTINUOUS_CHECKIN_DAYS = "continuous_checkin_days"
    }
    
    /**
     * 执行签到
     * @return 是否签到成功（今日未签到返回true，已签到返回false）
     */
    fun checkIn(): Boolean {
        val today = getCurrentDateString()
        
        // 注释掉签到限制，允许一天多次签到用于测试广告
        // if (isCheckedInToday()) {
        //     return false
        // }
        
        // 获取已签到日期集合
        val checkedInDates = getCheckedInDates().toMutableSet()
        checkedInDates.add(today)
        
        // 更新签到数据
        with(sharedPreferences.edit()) {
            putStringSet(KEY_CHECKIN_DATES, checkedInDates)
            putString(KEY_LAST_CHECKIN_DATE, today)
            putInt(KEY_TOTAL_CHECKIN_DAYS, checkedInDates.size)
            putInt(KEY_CONTINUOUS_CHECKIN_DAYS, calculateContinuousDays(checkedInDates))
            apply()
        }
        
        return true
    }
    
    /**
     * 检查今日是否已签到
     */
    fun isCheckedInToday(): Boolean {
        val today = getCurrentDateString()
        return getCheckedInDates().contains(today)
    }
    
    /**
     * 检查指定日期是否已签到
     */
    fun isCheckedIn(date: String): Boolean {
        return getCheckedInDates().contains(date)
    }
    
    /**
     * 获取所有已签到的日期
     */
    fun getCheckedInDates(): Set<String> {
        return sharedPreferences.getStringSet(KEY_CHECKIN_DATES, emptySet()) ?: emptySet()
    }
    
    /**
     * 获取总签到天数
     */
    fun getTotalCheckInDays(): Int {
        return sharedPreferences.getInt(KEY_TOTAL_CHECKIN_DAYS, 0)
    }
    
    /**
     * 获取连续签到天数
     */
    fun getContinuousCheckInDays(): Int {
        return sharedPreferences.getInt(KEY_CONTINUOUS_CHECKIN_DAYS, 0)
    }
    
    /**
     * 获取最后签到日期
     */
    fun getLastCheckInDate(): String? {
        return sharedPreferences.getString(KEY_LAST_CHECKIN_DATE, null)
    }
    
    /**
     * 获取当前日期字符串
     */
    private fun getCurrentDateString(): String {
        return dateFormat.format(Date())
    }
    
    /**
     * 计算连续签到天数
     */
    private fun calculateContinuousDays(checkedInDates: Set<String>): Int {
        if (checkedInDates.isEmpty()) return 0
        
        val sortedDates = checkedInDates.map { dateString ->
            dateFormat.parse(dateString) ?: Date()
        }.sortedDescending()
        
        var continuousDays = 0
        val calendar = Calendar.getInstance()
        calendar.time = Date() // 从今天开始
        
        for (date in sortedDates) {
            val dateStr = dateFormat.format(calendar.time)
            if (checkedInDates.contains(dateStr)) {
                continuousDays++
                calendar.add(Calendar.DAY_OF_YEAR, -1) // 往前一天
            } else {
                break
            }
        }
        
        return continuousDays
    }
    
    /**
     * 获取指定月份的签到日历数据
     * @param year 年份
     * @param month 月份 (1-12)
     * @return 该月每一天的签到状态
     */
    fun getMonthCheckInData(year: Int, month: Int): List<CheckInDayData> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1) // 月份从0开始
        
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val checkedInDates = getCheckedInDates()
        val today = getCurrentDateString()
        val todayCalendar = Calendar.getInstance()
        
        val result = mutableListOf<CheckInDayData>()
        
        for (day in 1..daysInMonth) {
            calendar.set(Calendar.DAY_OF_MONTH, day)
            val dateString = dateFormat.format(calendar.time)
            
            val status = when {
                dateString == today -> CheckInStatus.TODAY
                checkedInDates.contains(dateString) -> CheckInStatus.CHECKED_IN
                calendar.get(Calendar.YEAR) > todayCalendar.get(Calendar.YEAR) ||
                (calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) && 
                 calendar.get(Calendar.DAY_OF_YEAR) > todayCalendar.get(Calendar.DAY_OF_YEAR)) -> CheckInStatus.FUTURE
                calendar.get(Calendar.YEAR) < todayCalendar.get(Calendar.YEAR) ||
                (calendar.get(Calendar.YEAR) == todayCalendar.get(Calendar.YEAR) && 
                 calendar.get(Calendar.DAY_OF_YEAR) < todayCalendar.get(Calendar.DAY_OF_YEAR)) -> CheckInStatus.MISSED
                else -> CheckInStatus.AVAILABLE
            }
            
            // 获取当日签到次数和完成状态
            val checkInCount = if (status == CheckInStatus.CHECKED_IN || status == CheckInStatus.TODAY) {
                val count = sharedPreferences.getInt("today_checkin_count_$dateString", 0)
                android.util.Log.d("CheckInPreferences", "📖 [读取签到次数] 日期: $dateString, 次数: $count")
                count
            } else {
                0
            }
            
            // 获取完成状态
            val isComplete = if (status == CheckInStatus.CHECKED_IN || status == CheckInStatus.TODAY) {
                sharedPreferences.getInt("today_checkin_complete_$dateString", 0) == 1
            } else {
                false
            }
            
            result.add(CheckInDayData(day, dateString, status, checkInCount, isComplete))
        }
        
        return result
    }
    
    /**
     * 清除所有签到数据（用于测试）
     */
    fun clearAllData() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }
}

/**
 * 签到日期数据
 */
data class CheckInDayData(
    val day: Int,
    val dateString: String,
    val status: CheckInStatus,
    val checkInCount: Int = 0,  // 当日签到次数
    val isComplete: Boolean = false  // 是否完成签到（基于API返回的is_complete字段）
)

/**
 * 签到状态枚举
 */
enum class CheckInStatus {
    CHECKED_IN,    // 已签到
    TODAY,         // 今天（可签到）
    AVAILABLE,     // 可签到（历史日期）
    MISSED,        // 已错过
    FUTURE         // 未来日期（不可签到）
}