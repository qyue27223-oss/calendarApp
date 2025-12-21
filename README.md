# 基于 RFC5545 的日历日程管理 App

一个功能完整的 Android 日历应用，支持日程管理、提醒功能、网络订阅和农历显示。

## ✨ 主要功能

### 核心功能
- **日历视图展示**：支持月视图、周视图、日视图三种视图模式，流畅切换动画
- **日程管理**：完整的增删改查功能，支持日程类型（普通、生日、纪念日、其他）
- **重复日程**：支持仅一次、每天、每周、每月四种重复模式，自动生成最多 365 个重复事件
- **提醒功能**：支持提前提醒（5/15/30/60分钟）和响铃提醒，基于 AlarmManager 实现系统级提醒
- **RFC5545 标准**：完全符合 iCalendar 标准，支持导入导出 .ics 文件

### 扩展功能
- **网络订阅**：支持天气和黄历订阅服务，已配置真实 API（天气：http://t.weather.itboy.net/，黄历：http://v.juhe.cn/）
- **定时同步**：使用 WorkManager 实现每24小时自动同步订阅数据
- **农历显示**：完整的农历日期、天干地支、生肖、节气、节日显示
- **现代化 UI**：采用 Material3 设计规范，统一的共享组件设计

### 代码质量
- **持续优化**：定期进行代码重构和优化，累计减少冗余代码约 160-170 行
- **代码清理**：删除未使用的导入、参数、方法和空代码块，保持代码库整洁
- **性能优化**：优化数据库查询逻辑，减少不必要的全表查询
- **可维护性**：提取共享组件和通用函数，提升代码可维护性

## 🛠️ 技术栈

- **UI 框架**：Jetpack Compose (Material3)
- **架构模式**：MVVM
- **数据库**：Room Database
- **异步编程**：Kotlin Coroutines + Flow
- **网络请求**：Retrofit + OkHttp
- **系统服务**：AlarmManager、NotificationManager

## 📋 项目结构

```
app/src/main/java/com/example/calendar/
├── data/              # 数据层（Entity、Dao、Repository）
├── ui/                # UI 层（Compose 组件、ViewModel）
├── network/           # 网络服务层
├── reminder/          # 提醒服务层
└── util/              # 工具类（ICS 导入导出、农历计算等）
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17 或更高版本
- Android SDK API 24+

### 构建步骤

1. 克隆仓库
```bash
git clone https://github.com/your-username/calendar.git
cd calendar
```

2. 打开项目
- 使用 Android Studio 打开项目
- 等待 Gradle 同步完成

3. 配置（可选）
- 网络订阅功能已配置真实 API，可直接使用
- 如需更换 API 服务，请在 `RetrofitClient.kt` 中修改 API 地址
- 注意：订阅的 URL 字段当前未被使用，同步逻辑直接调用 API 服务

4. 运行
- 连接 Android 设备或启动模拟器
- 点击 Run 按钮运行应用

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 🙏 致谢

- 农历计算库：[lunarcalendar](https://github.com/xhinliang/lunarcalendar)
- Material3 设计规范

---

**注意**：本项目为学习项目，网络订阅功能已配置真实 API，可直接使用。