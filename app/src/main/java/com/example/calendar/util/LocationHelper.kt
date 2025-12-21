package com.example.calendar.util

import android.content.Context

/**
 * 位置获取工具类
 * 获取城市代码（邮编）用于天气API
 */
object LocationHelper {
    
    /**
     * 获取城市代码（邮编）
     * 如果没有位置信息，返回默认城市代码（北京：101010100）
     * 
     * 注意：这里简化实现，直接返回默认的北京城市代码
     * 如果需要根据实际位置获取城市代码，可以通过经纬度查询城市代码API
     */
    suspend fun getCityCode(context: Context?): String {
        return "101010100" // 北京
    }
}
