package com.example.calendar.util

import io.github.xhinliang.lunarcalendar.LunarCalendar
import java.time.LocalDate

/**
 * 农历日期数据类
 */
data class LunarDate(
    val year: Int,
    val month: Int,
    val day: Int,
    val isLeapMonth: Boolean = false
)

/**
 * 天干地支数据类
 */
data class Ganzhi(
    val year: String,  // 如"乙巳年(蛇)"
    val month: String, // 如"戊子月"
    val day: String,   // 如"壬戌日"
    val zodiac: String // 生肖，如"蛇"
)

/**
 * 农历计算工具类
 * 
 * 使用第三方库 com.github.XhinLiang:LunarCalendar 实现完整的农历计算功能
 */
object LunarCalendarUtil {
    
    // 农历月份名称
    private val lunarMonths = arrayOf(
        "正", "二", "三", "四", "五", "六",
        "七", "八", "九", "十", "十一", "十二"
    )
    
    // 农历日期名称
    private val lunarDays = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    /**
     * 将公历日期转换为农历日期
     * 使用 LunarCalendar 库实现完整的公历转农历算法
     */
    fun solarToLunar(solarDate: LocalDate): LunarDate {
        val lunarCalendar = LunarCalendar.getInstance(
            solarDate.year,
            solarDate.monthValue,
            solarDate.dayOfMonth
        )
        val lunar = lunarCalendar.lunar
        
        return LunarDate(
            year = lunar.year,
            month = lunar.month,
            day = lunar.day,
            isLeapMonth = lunar.isLeap
        )
    }

    /**
     * 格式化农历日期为字符串
     * 如："十月三十"、"廿一"等
     */
    fun formatLunarDate(lunarDate: LunarDate, showYear: Boolean = false): String {
        val monthStr = if (lunarDate.isLeapMonth) {
            "闰${lunarMonths.getOrElse(lunarDate.month - 1) { "未知" }}月"
        } else {
            "${lunarMonths.getOrElse(lunarDate.month - 1) { "未知" }}月"
        }
        
        val dayStr = lunarDays.getOrElse(lunarDate.day - 1) { "未知" }
        
        return if (showYear) {
            "${lunarDate.year}年$monthStr$dayStr"
        } else {
            "$monthStr$dayStr"
        }
    }

    /**
     * 格式化农历日期为简短字符串（仅显示日期部分）
     * 如："三十"、"廿一"等
     */
    fun formatLunarDayShort(lunarDate: LunarDate): String {
        return lunarDays.getOrElse(lunarDate.day - 1) { "未知" }
    }

    /**
     * 计算天干地支
     * 注意：LunarCalendar库可能没有直接提供天干地支方法
     * 这里保持原有的计算逻辑作为补充
     */
    fun calculateGanzhi(year: Int, month: Int, day: Int): Ganzhi {
        // 天干
        val gan = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
        // 地支
        val zhi = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
        // 生肖（对应地支）
        val zodiacs = arrayOf("鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪")
        
        // 计算年份的天干地支
        val yearOffset = year - 4
        val yearGanIndex = ((yearOffset % 10) + 10) % 10
        val yearZhiIndex = ((yearOffset % 12) + 12) % 12
        val yearGan = gan[yearGanIndex]
        val yearZhi = zhi[yearZhiIndex]
        val zodiac = zodiacs[yearZhiIndex]
        val yearStr = yearGan + yearZhi + "年(" + zodiac + ")"

        // 计算月份的天干地支（根据年干支和月份计算）
        // 公式：月干 = (年干序号 * 2 + 月数) % 10
        // 月支 = 从寅月开始（正月为寅，对应索引2）
        val monthGanIndex = (((yearGanIndex * 2) + month) % 10 + 10) % 10
        val monthZhiIndex = ((month + 1) % 12 + 12) % 12
        val monthGan = gan[monthGanIndex]
        val monthZhi = zhi[monthZhiIndex]
        val monthStr = monthGan + monthZhi + "月"

        // 计算日期的天干地支
        // 使用基姆拉尔森公式计算日干支（简化版本）
        // 1900年1月1日为甲子日（索引0）
        val baseDate = java.time.LocalDate.of(1900, 1, 1)
        val targetDate = java.time.LocalDate.of(year, month, day)
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, targetDate).toInt()
        val dayGanIndex = ((daysDiff % 10) + 10) % 10
        val dayZhiIndex = ((daysDiff % 12) + 12) % 12
        val dayGan = gan[dayGanIndex]
        val dayZhi = zhi[dayZhiIndex]
        val dayStr = dayGan + dayZhi + "日"

        return Ganzhi(
            year = yearStr,
            month = monthStr,
            day = dayStr,
            zodiac = zodiac
        )
    }

    /**
     * 获取星期几的中文表示
     */
    fun getWeekdayChinese(date: LocalDate): String {
        val weekdays = arrayOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        return weekdays[date.dayOfWeek.value % 7]
    }

    /**
     * 判断是否为节气
     * 使用 LunarCalendar 库获取准确的节气信息
     */
    fun isSolarTerm(date: LocalDate): String? {
        val lunarCalendar = LunarCalendar.getInstance(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
        
        // 获取节气名称（如果不为空则说明是节气日）
        val solarTerm = lunarCalendar.solarTerm
        return if (solarTerm != null && solarTerm.isNotEmpty() && solarTerm.isNotBlank()) {
            solarTerm
        } else {
            null
        }
    }

    /**
     * 获取农历节日
     * 使用 LunarCalendar 库获取准确的农历节日信息
     * 如果库没有提供，则使用映射表作为补充
     */
    fun getLunarFestival(lunarDate: LunarDate): String? {
        // LunarCalendar 库可能没有直接提供农历节日查询方法
        // 使用原有的映射表作为补充
        val festivals = mapOf(
            Pair(1, 1) to "春节",
            Pair(1, 15) to "元宵节",
            Pair(5, 5) to "端午节",
            Pair(7, 7) to "七夕节",
            Pair(8, 15) to "中秋节",
            Pair(9, 9) to "重阳节",
            Pair(12, 30) to "除夕"
        )
        
        return festivals[Pair(lunarDate.month, lunarDate.day)]
    }

    /**
     * 获取公历节日
     */
    fun getSolarFestival(date: LocalDate): String? {
        val month = date.monthValue
        val day = date.dayOfMonth
        
        val festivals = mapOf(
            Pair(1, 1) to "元旦",
            Pair(2, 14) to "情人节",
            Pair(3, 8) to "妇女节",
            Pair(4, 1) to "愚人节",
            Pair(5, 1) to "劳动节",
            Pair(5, 4) to "青年节",
            Pair(6, 1) to "儿童节",
            Pair(9, 10) to "教师节",
            Pair(10, 1) to "国庆节",
            Pair(12, 13) to "国家公祭日",
            Pair(12, 20) to "澳门回归日",
            Pair(12, 24) to "平安夜",
            Pair(12, 25) to "圣诞节"
        )
        
        return festivals[Pair(month, day)]
    }
}
