package com.example.calendar.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 位置获取工具类
 * 获取城市代码（邮编）用于天气API
 */
object LocationHelper {
    private const val PREFS_NAME = "calendar_prefs"
    private const val KEY_CITY_CODE = "selected_city_code"
    private const val KEY_CITY_NAME = "selected_city_name"
    private const val DEFAULT_CITY_CODE = "101010100" // 北京
    private const val DEFAULT_CITY_NAME = "北京"
    
    /**
     * 获取用户选择的城市代码
     */
    fun getCityCode(context: Context?): String {
        if (context == null) return DEFAULT_CITY_CODE
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CITY_CODE, DEFAULT_CITY_CODE) ?: DEFAULT_CITY_CODE
    }
    
    /**
     * 获取用户选择的城市名称
     */
    fun getCityName(context: Context?): String {
        if (context == null) return DEFAULT_CITY_NAME
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CITY_NAME, DEFAULT_CITY_NAME) ?: DEFAULT_CITY_NAME
    }
    
    /**
     * 保存用户选择的城市
     */
    fun saveCity(context: Context?, cityCode: String, cityName: String) {
        if (context == null) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_CITY_CODE, cityCode)
            .putString(KEY_CITY_NAME, cityName)
            .apply()
    }
}
