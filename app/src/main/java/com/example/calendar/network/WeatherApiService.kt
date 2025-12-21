package com.example.calendar.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 和风天气 API 服务接口
 * API地址：https://devapi.qweather.com/v7/weather/7d
 */
interface WeatherApiService {
    
    /**
     * 获取7日天气预报
     * @param key API密钥
     * @param location 城市ID/经纬度，如"101010100"（北京）或"116.41,39.92"（经纬度）
     */
    @GET("v7/weather/7d")
    suspend fun get7DayForecast(
        @Query("key") key: String,
        @Query("location") location: String
    ): WeatherResponse
}

