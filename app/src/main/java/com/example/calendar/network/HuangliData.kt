package com.example.calendar.network

import com.google.gson.annotations.SerializedName

/**
 * 黄历数据模型 - 匹配聚合数据API响应格式
 */
data class HuangliResponse(
    @SerializedName("error_code")
    val errorCode: Int,  // 返回码，0表示成功
    @SerializedName("reason")
    val reason: String,  // 返回说明
    @SerializedName("result")
    val result: HuangliResult?  // 黄历结果数据
)

data class HuangliResult(
    @SerializedName("id")
    val id: String?,  // 标识
    @SerializedName("yangli")
    val yangli: String?,  // 阳历，格式：yyyy-MM-dd
    @SerializedName("yinli")
    val yinli: String?,  // 阴历，如"甲午(马)年八月十八"
    @SerializedName("wuxing")
    val wuxing: String?,  // 五行，如"井泉水建执位"
    @SerializedName("chongsha")
    val chongsha: String?,  // 冲煞，如"冲兔(己卯)煞东"
    @SerializedName("baiji")
    val baiji: String?,  // 彭祖百忌
    @SerializedName("jishen")
    val jishen: String?,  // 吉神宜趋
    @SerializedName("yi")
    val yi: String?,  // 宜（字符串，需要拆分）
    @SerializedName("xiongshen")
    val xiongshen: String?,  // 凶神宜忌
    @SerializedName("ji")
    val ji: String?   // 忌（字符串，需要拆分）
)

