package com.example.calendar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.calendar.util.LunarCalendarUtil
import com.example.calendar.util.formatChineseDate
import java.time.LocalDate

/**
 * 日历视图的底部信息栏，显示今天的日期和选中日期的农历信息
 * 用于月视图和周视图
 */
@Composable
fun CalendarViewFooter(selectedDate: LocalDate) {
    val today = LocalDate.now()
    val lunarDate = LunarCalendarUtil.solarToLunar(selectedDate)
    val lunarDateStr = LunarCalendarUtil.formatLunarDate(lunarDate, showYear = true)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = today.formatChineseDate(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "农历$lunarDateStr",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

