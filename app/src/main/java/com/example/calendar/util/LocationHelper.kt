package com.example.calendar.util

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * 位置获取工具类
 * 简化实现：使用系统LocationManager获取最后已知位置
 */
object LocationHelper {
    
    /**
     * 获取设备当前位置（经纬度格式：经度,纬度）
     * 如果没有位置信息，返回默认位置（北京）
     */
    suspend fun getCurrentLocation(context: Context): String {
        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = LocationManagerCompat.isLocationEnabled(locationManager)
            
            if (isGpsEnabled) {
                // 尝试获取GPS位置
                val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                // 尝试获取网络位置
                val networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                
                // 选择更精确的位置
                val location = when {
                    gpsLocation != null && networkLocation != null -> {
                        if (gpsLocation.accuracy > networkLocation.accuracy) networkLocation else gpsLocation
                    }
                    gpsLocation != null -> gpsLocation
                    networkLocation != null -> networkLocation
                    else -> null
                }
                
                location?.let {
                    "${it.longitude},${it.latitude}"
                } ?: "116.41,39.92" // 默认北京位置
            } else {
                "116.41,39.92" // 默认北京位置
            }
        } catch (e: Exception) {
            // 如果获取位置失败，返回默认位置（北京）
            "116.41,39.92"
        }
    }
}
