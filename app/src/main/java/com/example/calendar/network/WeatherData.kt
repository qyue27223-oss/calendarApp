package com.example.calendar.network

import com.google.gson.annotations.SerializedName

/**
 * 和风天气API响应数据模型
 */
data class WeatherResponse(
    @SerializedName("code")
    val code: String,
    @SerializedName("updateTime")
    val updateTime: String?,
    @SerializedName("daily")
    val daily: List<DailyForecast>?
)

data class DailyForecast(
    @SerializedName("fxDate")
    val fxDate: String?,  // 预报日期 (yyyy-MM-dd)
    @SerializedName("tempMax")
    val tempMax: String?,  // 当日最高气温 (℃)
    @SerializedName("tempMin")
    val tempMin: String?,  // 当日最低气温 (℃)
    @SerializedName("textDay")
    val textDay: String?,  // 白天天气状况
    @SerializedName("textNight")
    val textNight: String?,  // 夜间天气状况
    @SerializedName("windDirDay")
    val windDirDay: String?,  // 白天风向
    @SerializedName("windScaleDay")
    val windScaleDay: String?,  // 白天风力等级
    @SerializedName("humidity")
    val humidity: String?,  // 相对湿度 (%)
    @SerializedName("uvIndex")
    val uvIndex: String?  // 紫外线指数
)