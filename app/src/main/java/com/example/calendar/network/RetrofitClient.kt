package com.example.calendar.network

import com.example.calendar.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit 客户端配置
 */
object RetrofitClient {
    
    // 天气API基础URL
    private const val BASE_URL_WEATHER = "http://t.weather.itboy.net/"
    private const val BASE_URL_HUANGLI = "http://v.juhe.cn/"

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
        
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val weatherRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_WEATHER)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val huangliRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_HUANGLI)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherApiService: WeatherApiService by lazy {
        weatherRetrofit.create(WeatherApiService::class.java)
    }

    val huangliApiService: HuangliApiService by lazy {
        huangliRetrofit.create(HuangliApiService::class.java)
    }
}

