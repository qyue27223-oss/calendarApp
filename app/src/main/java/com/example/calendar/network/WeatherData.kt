package com.example.calendar.network

import com.google.gson.annotations.SerializedName

/**
 * 天气API响应数据模型
 * API地址：http://t.weather.itboy.net/api/weather/city/{citykey}
 */
data class WeatherResponse(
    @SerializedName("message")
    val message: String?,
    @SerializedName("status")
    val status: Int,
    @SerializedName("date")
    val date: String?,
    @SerializedName("time")
    val time: String?,
    @SerializedName("cityInfo")
    val cityInfo: CityInfo?,
    @SerializedName("data")
    val data: WeatherData?
)

data class CityInfo(
    @SerializedName("city")
    val city: String?,
    @SerializedName("citykey")
    val citykey: String?,
    @SerializedName("parent")
    val parent: String?,
    @SerializedName("updateTime")
    val updateTime: String?
)

data class WeatherData(
    @SerializedName("shidu")
    val shidu: String?,  // 湿度
    @SerializedName("pm25")
    val pm25: Double?,
    @SerializedName("pm10")
    val pm10: Double?,
    @SerializedName("quality")
    val quality: String?,  // 空气质量
    @SerializedName("wendu")
    val wendu: String?,  // 当前温度
    @SerializedName("ganmao")
    val ganmao: String?,  // 感冒提醒
    @SerializedName("forecast")
    val forecast: List<Forecast>?,
    @SerializedName("yesterday")
    val yesterday: Forecast?
)

data class Forecast(
    @SerializedName("date")
    val date: String?,  // 日期（如"21"）
    @SerializedName("high")
    val high: String?,  // 高温，如"高温 3℃"
    @SerializedName("low")
    val low: String?,  // 低温，如"低温 -5℃"
    @SerializedName("ymd")
    val ymd: String?,  // 完整日期，如"2025-12-21"
    @SerializedName("week")
    val week: String?,  // 星期，如"星期日"
    @SerializedName("sunrise")
    val sunrise: String?,  // 日出时间
    @SerializedName("sunset")
    val sunset: String?,  // 日落时间
    @SerializedName("aqi")
    val aqi: Int?,  // 空气质量指数
    @SerializedName("fx")
    val fx: String?,  // 风向
    @SerializedName("fl")
    val fl: String?,  // 风力等级
    @SerializedName("type")
    val type: String?,  // 天气类型，如"晴"、"阴"、"多云"
    @SerializedName("notice")
    val notice: String?  // 提示信息
)