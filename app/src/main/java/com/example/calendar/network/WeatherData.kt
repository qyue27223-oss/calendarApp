package com.example.calendar.network

import com.google.gson.annotations.SerializedName

/**
 * 天气数据模型
 */
data class WeatherResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: WeatherData?
)

data class WeatherData(
    @SerializedName("city")
    val city: String?,
    @SerializedName("current")
    val current: CurrentWeather?,
    @SerializedName("forecast")
    val forecast: List<DailyForecast>?
)

data class CurrentWeather(
    @SerializedName("temp")
    val temp: String?,
    @SerializedName("weather")
    val weather: String?,
    @SerializedName("aqi")
    val aqi: String?,
    @SerializedName("aqiLevel")
    val aqiLevel: String?
)

data class DailyForecast(
    @SerializedName("date")
    val date: String?,
    @SerializedName("high")
    val high: String?,
    @SerializedName("low")
    val low: String?,
    @SerializedName("weather")
    val weather: String?,
    @SerializedName("icon")
    val icon: String?
)

