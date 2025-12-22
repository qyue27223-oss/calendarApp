package com.example.calendar.util

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 主题模式枚举
 */
enum class ThemeMode {
    LIGHT,      // 浅色模式
    DARK        // 深色模式
}

/**
 * 主题管理器
 * 负责保存和读取用户的主题偏好
 */
class ThemeManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "calendar_prefs",
        Context.MODE_PRIVATE
    )
    
    private val THEME_MODE_KEY = "theme_mode"
    
    /**
     * 获取当前主题模式
     * @return ThemeMode，如果未设置则返回 null（表示跟随系统）
     */
    fun getThemeMode(): ThemeMode? {
        val modeString = prefs.getString(THEME_MODE_KEY, null)
        return when (modeString) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> null  // null 表示跟随系统
        }
    }
    
    /**
     * 保存主题模式
     */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(THEME_MODE_KEY, mode.name).apply()
    }
    
    /**
     * 清除主题模式设置，恢复跟随系统
     */
    fun clearThemeMode() {
        prefs.edit().remove(THEME_MODE_KEY).apply()
    }
}

/**
 * 在 Compose 中获取主题管理器的扩展函数
 */
@Composable
fun rememberThemeManager(): ThemeManager {
    val context = LocalContext.current
    return androidx.compose.runtime.remember { ThemeManager(context) }
}

/**
 * 根据主题模式和系统主题计算是否使用深色主题
 * @param userThemeMode 用户选择的主题模式，null 表示跟随系统
 * @param systemDarkTheme 系统当前是否为深色主题
 * @return true 表示使用深色主题，false 表示使用浅色主题
 */
fun calculateDarkTheme(userThemeMode: ThemeMode?, systemDarkTheme: Boolean): Boolean {
    return when (userThemeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        null -> systemDarkTheme
    }
}

