package com.example.calendar.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 天气 API 服务接口
 * 
 * 注意：这里使用示例 API，实际使用时需要替换为真实的天气 API 端点
 */
interface WeatherApiService {
    
    /**
     * 获取15日天气预报
     * @param city 城市名称，如"北京"、"石景山区"
     */
    @GET("weather/15days")
    suspend fun get15DayForecast(
        @Query("city") city: String
    ): WeatherResponse
    
    /**
     * 获取当前天气
     */
    @GET("weather/current")
    suspend fun getCurrentWeather(
        @Query("city") city: String
    ): WeatherResponse
}

