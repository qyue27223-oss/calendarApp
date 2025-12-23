package com.example.calendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.calendar.ui.theme.CalendarTheme
import kotlinx.coroutines.delay

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            CalendarTheme(darkTheme = true) {
                SplashScreen {
                    // 启动画面显示完成后跳转到主界面
                    startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    finish()
                }
            }
        }
    }
}

@Composable
fun SplashScreen(onFinish: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "alpha"
    )
    
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500) // 显示2.5秒
        onFinish()
    }
    
    // 精美的渐变背景：从深蓝紫色渐变到深蓝色再到黑色
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1a1a2e), // 深蓝紫色
            Color(0xFF16213e), // 深蓝色
            Color(0xFF0f1419), // 深灰蓝色
            Color(0xFF0a0a0f)  // 接近黑色
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )
    
    // 装饰性的光晕效果
    val glowGradient = Brush.radialGradient(
        colors = listOf(
            Color(0xFF4a90e2).copy(alpha = 0.15f),
            Color.Transparent
        ),
        radius = 800f
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush),
        contentAlignment = Alignment.Center
    ) {
        // 背景装饰光晕
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(glowGradient)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 32.dp)
                .alpha(alphaAnim.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 图标容器 - 添加光晕效果
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4a90e2).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 应用图标
                Image(
                    painter = painterResource(id = R.drawable.bg_calendar),
                    contentDescription = "应用图标",
                    modifier = Modifier.size(120.dp),
                    contentScale = ContentScale.Fit
                )
            }
            
            Spacer(modifier = Modifier.height(56.dp))
            
            // 文字容器 - 添加半透明背景以增加层次感
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 第一句话
                Text(
                    text = "聚焦今日，每一刻都值得记录",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF0F0F0),
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 分隔线
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(2.dp)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFF4a90e2).copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 第二句话
                Text(
                    text = "纵览时光，每一天都应该把握",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF0F0F0),
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // 装饰性的加载指示器（三个点）
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 16.dp)
            ) {
                repeat(3) { index ->
                    val delay = index * 200
                    var visible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(key1 = startAnimation) {
                        if (startAnimation) {
                            kotlinx.coroutines.delay(delay.toLong())
                            visible = true
                        }
                    }
                    
                    val dotAlpha = animateFloatAsState(
                        targetValue = if (visible) 1f else 0.3f,
                        animationSpec = tween(durationMillis = 400),
                        label = "dotAlpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = Color(0xFF5ba3f5).copy(alpha = dotAlpha.value),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

