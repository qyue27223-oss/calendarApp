package com.example.calendar.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 黄历 API 服务接口
 * 使用聚合数据的黄历API
 * API地址：http://v.juhe.cn/laohuangli/d
 */
interface HuangliApiService {
    
    /**
     * 获取指定日期的黄历信息
     * @param key API密钥
     * @param date 日期，格式：yyyy-MM-dd，如"2024-09-11"
     */
    @GET("laohuangli/d")
    suspend fun getHuangli(
        @Query("key") key: String,
        @Query("date") date: String
    ): HuangliResponse
}

