package com.example.calendar.util

/**
 * 拼音工具类
 * 提供城市名称到拼音的转换功能，支持拼音搜索
 */
object PinyinHelper {
    /**
     * 城市名称到拼音的映射表
     * 格式：城市名称 -> (完整拼音, 拼音首字母)
     */
    private val cityPinyinMap = mapOf(
        "北京" to ("beijing" to "bj"),
        "上海" to ("shanghai" to "sh"),
        "广州" to ("guangzhou" to "gz"),
        "深圳" to ("shenzhen" to "sz"),
        "杭州" to ("hangzhou" to "hz"),
        "南京" to ("nanjing" to "nj"),
        "成都" to ("chengdu" to "cd"),
        "武汉" to ("wuhan" to "wh"),
        "西安" to ("xian" to "xa"),
        "重庆" to ("chongqing" to "cq"),
        "天津" to ("tianjin" to "tj"),
        "苏州" to ("suzhou" to "sz"),
        "郑州" to ("zhengzhou" to "zz"),
        "长沙" to ("changsha" to "cs"),
        "沈阳" to ("shenyang" to "sy"),
        "青岛" to ("qingdao" to "qd"),
        "大连" to ("dalian" to "dl"),
        "厦门" to ("xiamen" to "xm"),
        "福州" to ("fuzhou" to "fz"),
        "济南" to ("jinan" to "jn"),
        "合肥" to ("hefei" to "hf"),
        "石家庄" to ("shijiazhuang" to "sjz"),
        "哈尔滨" to ("haerbin" to "heb"),
        "长春" to ("changchun" to "cc"),
        "南昌" to ("nanchang" to "nc"),
        "昆明" to ("kunming" to "km"),
        "贵阳" to ("guiyang" to "gy"),
        "南宁" to ("nanning" to "nn"),
        "海口" to ("haikou" to "hk"),
        "乌鲁木齐" to ("wulumuqi" to "wlmq"),
        "拉萨" to ("lasa" to "ls"),
        "银川" to ("yinchuan" to "yc"),
        "西宁" to ("xining" to "xn"),
        "兰州" to ("lanzhou" to "lz")
    )
    
    /**
     * 获取城市的完整拼音
     */
    fun getPinyin(cityName: String): String {
        return cityPinyinMap[cityName]?.first ?: ""
    }
    
    /**
     * 获取城市的拼音首字母
     */
    fun getPinyinInitials(cityName: String): String {
        return cityPinyinMap[cityName]?.second ?: ""
    }
    
    /**
     * 检查城市名称是否匹配搜索文本（支持中文、拼音、拼音首字母、模糊匹配）
     * @param cityName 城市名称
     * @param searchText 搜索文本
     * @return 是否匹配
     */
    fun matches(cityName: String, searchText: String): Boolean {
        if (searchText.isBlank()) return true
        
        val lowerSearchText = searchText.lowercase().trim()
        
        // 1. 中文名称匹配（不区分大小写，支持部分匹配）
        if (cityName.contains(lowerSearchText, ignoreCase = true)) {
            return true
        }
        
        val pinyin = getPinyin(cityName)
        val initials = getPinyinInitials(cityName)
        
        // 2. 完整拼音匹配（支持部分匹配，如"beij"匹配"beijing"）
        if (pinyin.isNotEmpty() && pinyin.contains(lowerSearchText, ignoreCase = true)) {
            return true
        }
        
        // 3. 拼音首字母匹配（支持连续首字母匹配，如"bj"匹配"北京"）
        if (initials.isNotEmpty() && initials.contains(lowerSearchText, ignoreCase = true)) {
            return true
        }
        
        // 4. 模糊匹配：支持拼音的连续字符匹配（如"bj"在"beijing"中按顺序匹配）
        // 检查搜索文本的每个字符是否按顺序出现在拼音中
        if (pinyin.isNotEmpty() && fuzzyMatch(pinyin, lowerSearchText)) {
            return true
        }
        
        // 5. 支持拼音首字母的模糊匹配（如"bj"在"bj"中按顺序匹配）
        if (initials.isNotEmpty() && fuzzyMatch(initials, lowerSearchText)) {
            return true
        }
        
        return false
    }
    
    /**
     * 模糊匹配：检查搜索文本的字符是否按顺序出现在目标字符串中
     * 例如："bj" 可以匹配 "beijing"（b和j按顺序出现，中间可以有其他字符）
     * 例如："sh" 可以匹配 "shanghai"（s和h按顺序出现）
     */
    private fun fuzzyMatch(target: String, pattern: String): Boolean {
        if (pattern.isEmpty()) return true
        if (pattern.length > target.length) return false
        
        var targetIndex = 0
        var patternIndex = 0
        
        while (targetIndex < target.length && patternIndex < pattern.length) {
            if (target[targetIndex].lowercaseChar() == pattern[patternIndex].lowercaseChar()) {
                patternIndex++
            }
            targetIndex++
        }
        
        return patternIndex == pattern.length
    }
}

