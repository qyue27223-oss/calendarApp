# 基于 RFC5545 的日历日程管理 App

一个功能完整的 Android 日历应用，支持日程管理、提醒功能、网络订阅和农历显示。

## ✨ 主要功能

### 核心功能
- **日历视图展示**：支持月视图、周视图、日视图三种视图模式，流畅切换动画
- **日程管理**：完整的增删改查功能，支持日程类型（普通、生日、纪念日、其他）
- **日程状态区分**：清晰区分已完成、正在进行中和未完成三种状态，使用不同颜色和样式，所有状态都显示状态标签，**状态判断逻辑优化，确保时间戳比较准确**
- **重复日程**：支持仅一次、每天、每周、每月四种重复模式，自动生成最多 365 个重复事件
- **提醒功能**：支持提前提醒（5/15/30/60分钟，**新建日程时默认选择5分钟**）和响铃提醒，基于 AlarmManager 实现系统级提醒，响铃控制精确（只有开启响铃开关时才会响铃）
- **RFC5545 标准**：完全符合 iCalendar 标准，支持完整的导入导出 .ics 文件功能，包括：
  - **导入功能**：支持从文件导入日程，显示预览和冲突处理选项（覆盖/跳过），提供详细的导入结果反馈
  - **导出功能**：支持三种导出模式（当前日期、自定义日期范围、所有日程），提供友好的UI界面和错误提示

### 扩展功能
- **网络订阅**：支持天气和黄历订阅服务，已配置真实 API（天气：http://t.weather.itboy.net/，黄历：http://v.juhe.cn/）
- **天气城市定位**：支持选择34个常用城市，所有天气卡片都显示城市切换按钮，城市选择对话框支持**拼音搜索和模糊匹配**（支持中文、完整拼音、拼音首字母、连续字符匹配）
- **定时同步**：使用 WorkManager 实现每24小时自动同步订阅数据
- **农历显示**：完整的农历日期、天干地支、生肖、节气、节日显示
- **现代化 UI**：采用 Material3 设计规范，统一的共享组件设计，优化的日程状态样式和天气卡片样式

### 代码质量
- **持续优化**：定期进行代码重构和优化，累计减少冗余代码约 734 行（MainActivity.kt 从 1514 行优化到 780 行，减少 48.5%）
- **组件提取**：提取了 5 个对话框组件到独立文件 `ImportExportDialogs.kt`（768 行，包含 `ImportConfirmDialog`、`ImportResultDialog`、`ExportOptionsDialog`、`DateRangePickerDialog`、`ExportConfirmDialog`），提升代码模块化
- **依赖统一**：Retrofit/OkHttp/Gson 依赖改为使用版本清单 `libs.versions.toml` 管理，减少重复声明
- **日志安全**：网络层仅在 Debug 版输出 BODY 级别日志，Release 版降级为 BASIC，降低泄露风险
- **结构优化**：提取公共组件（`CalendarTopBarTitle`、`NavigationIconButton`、`ForwardIconButton`），统一时区处理（使用系统默认时区），简化数据结构
- **主题功能优化**：提取 `calculateDarkTheme()` 函数统一主题计算逻辑，优化 Repository 层代码，消除重复逻辑
- **工具函数优化**：将 `getWeekNumber` 函数移至 `util/TimeExtensions.kt` 作为扩展函数，提升代码复用性
- **冗余代码清理**：删除了冗余的 `syncSubscriptionsOnStartup` 函数（已在 `CalendarApp.kt` 中实现），清理未使用的导入
- **性能优化**：优化数据库查询逻辑，减少不必要的全表查询
- **可维护性**：提取共享组件和通用函数，提升代码可维护性和可扩展性

### 主题功能
- **主题切换**：支持浅色模式、深色模式和跟随系统三种选项
- **菜单整合**：三个主题选项整合到一个菜单中，提供流畅的主题切换体验
- **自动切换**：选择"跟随系统"后，应用会自动跟随系统主题变化

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
- `buildFeatures.buildConfig` 已启用，可直接使用 `BuildConfig.DEBUG` 进行环境判断

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