package com.example.calendar.data

/**
 * 城市数据类
 */
data class City(
    val name: String,      // 城市名称
    val code: String       // 城市代码（用于天气API）
)

/**
 * 常用城市列表
 */
object CityList {
    val cities = listOf(
        City("北京", "101010100"),
        City("上海", "101020100"),
        City("广州", "101280101"),
        City("深圳", "101280601"),
        City("杭州", "101210101"),
        City("南京", "101190101"),
        City("成都", "101270101"),
        City("武汉", "101200101"),
        City("西安", "101110101"),
        City("重庆", "101040100"),
        City("天津", "101030100"),
        City("苏州", "101190401"),
        City("郑州", "101180101"),
        City("长沙", "101250101"),
        City("沈阳", "101070101"),
        City("青岛", "101120201"),
        City("大连", "101070201"),
        City("厦门", "101230201"),
        City("福州", "101230101"),
        City("济南", "101120101"),
        City("合肥", "101220101"),
        City("石家庄", "101090101"),
        City("哈尔滨", "101050101"),
        City("长春", "101060101"),
        City("南昌", "101240101"),
        City("昆明", "101290101"),
        City("贵阳", "101260101"),
        City("南宁", "101300101"),
        City("海口", "101310101"),
        City("乌鲁木齐", "101130101"),
        City("拉萨", "101140101"),
        City("银川", "101170101"),
        City("西宁", "101150101"),
        City("兰州", "101160101")
    )
    
    /**
     * 根据城市代码获取城市名称
     */
    fun getCityNameByCode(code: String): String {
        return cities.find { it.code == code }?.name ?: "未知城市"
    }
    
    /**
     * 根据城市名称获取城市代码
     */
    fun getCityCodeByName(name: String): String? {
        return cities.find { it.name == name }?.code
    }
}

