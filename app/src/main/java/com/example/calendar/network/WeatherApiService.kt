package com.example.calendar.network

import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 天气 API 服务接口
 * API地址：http://t.weather.itboy.net/api/weather/city/{citykey}
 */
interface WeatherApiService {
    
    /**
     * 获取天气预报
     * @param citykey 城市代码（邮编），如"101010100"（北京）
     */
    @GET("api/weather/city/{citykey}")
    suspend fun getWeatherForecast(
        @Path("citykey") citykey: String
    ): WeatherResponse
}

