# 日历应用扩展功能开发 TODO

## 📋 项目总览与实现情况评估

### 基本要求实现情况

#### ✅ 1. 日历视图展示（月视图、周视图、日视图）
- **实现状态**：✅ **已完成**
- **实现细节**：
  - 月视图（MonthView）：完整实现，支持日期选择、事件标记、农历显示、节日节气显示
  - 周视图（WeekView）：完整实现，支持周导航、事件标记、农历显示
  - 日视图（DayView）：完整实现，显示当日所有事件和订阅事件，包含完整农历信息卡片
  - 视图切换：支持平滑动画切换（AnimatedContent）
  - 导航功能：支持上一月/周/日、下一月/周/日、快速跳转到今天

#### ✅ 2. 日程添加、编辑、查看和删除
- **实现状态**：✅ **已完成**
- **实现细节**：
  - ✅ 添加日程：通过 FloatingActionButton 打开 EventEditorDialog，采用模块化设计
  - ✅ 编辑日程：点击日程卡片打开编辑对话框，支持修改所有字段
  - ✅ 查看日程：在月/周/日视图中显示日程，点击查看详情
  - ✅ 删除日程：在编辑对话框中提供删除按钮，删除时同步清理提醒记录
  - ✅ 数据模型：完全符合 RFC5545 标准（UID、SUMMARY、DESCRIPTION、DTSTART、DTEND、LOCATION等）
  - ✅ **日程类型选择**：支持选择"普通日程"、"生日"、"纪念日"、"其他"四种类型，已持久化到数据库
  - ✅ **时间选择器**：使用 Material3 TimePicker，精确到分钟
  - ✅ **重复次数选择**：支持选择"仅一次"、"每天"、"每周"、"每月"，后端已实现重复事件生成逻辑
  - ✅ **响铃提醒开关**：支持响铃提醒开关控制，后端已实现响铃控制逻辑

#### ✅ 3. 日程提醒功能
- **实现状态**：✅ **已完成**
- **实现细节**：
  - ✅ 提醒方式：支持提前提醒（5/15/30/60分钟），默认5分钟提醒
  - ✅ 提醒调度：使用 AlarmManager 和 BroadcastReceiver 实现系统级提醒
  - ✅ 提醒通知：通过 NotificationManager 发送通知，包含日程标题
  - ✅ 提醒管理：创建/更新/删除日程时自动管理提醒，保证数据一致性
  - ✅ 提醒存储：使用 Reminder 实体存储提醒记录
  - ✅ **响铃提醒**：已完全实现，响铃时包含声音、震动和灯光，不响铃时仅显示灯光
- **待完善部分**：
  - ⚠️ **固定时间提醒**：当前未实现，代码中只有提前提醒功能（可选功能）

### 扩展要求实现情况

#### ✅ 1. 日历事件的导入导出
- **实现状态**：✅ **已完成**
- **实现细节**：
  - 导入功能：支持从 .ics 文件导入，完全符合 RFC5545 标准
    - 解析 VEVENT 组件（UID、DTSTART、DTEND、SUMMARY、DESCRIPTION、LOCATION等）
    - 处理时区转换（UTC 到本地时区）
    - 处理文本字段转义字符
    - UID 冲突检测（支持覆盖或跳过策略）
    - 导入结果反馈（成功/跳过/错误统计）
  - 导出功能：支持导出为 .ics 格式
    - 导出当前选中日期的事件
    - 导出指定日期范围的事件
    - 导出所有事件
    - 支持保存到文件和复制到剪贴板

#### ⚠️ 2. 网络订阅功能
- **实现状态**：⚠️ **基础架构已完成，部分功能待完善**
- **已完成部分**：
  - ✅ 数据模型：Subscription 和 SubscriptionEvent 实体，支持天气和黄历两种类型
  - ✅ 网络服务层：Retrofit + OkHttp + Gson，定义了 WeatherApiService 和 HuangliApiService
  - ✅ 订阅管理 UI：SubscriptionScreen 支持添加、编辑、删除、启用/禁用订阅
  - ✅ 订阅事件展示：SubscriptionEventItem 支持天气和黄历两种卡片样式
  - ✅ 数据同步：支持手动同步订阅数据
- **待完善部分**：
  - ⚠️ **API 配置**：当前使用示例 URL，需要配置真实的天气和黄历 API
  - ⚠️ **定时同步**：未实现 WorkManager 定时自动同步
  - ⚠️ **15日天气预报展开**：UI 中预留了位置但未实现展开/收起功能
  - ⚠️ **快速订阅按钮**：订阅事件卡片中未添加快速订阅/取消订阅按钮

#### ⚠️ 3. 农历相关功能
- **实现状态**：⚠️ **大部分已完成，部分功能待完善**
- **已完成部分**：
  - ✅ 农历计算：使用第三方库 `io.github.xhinliang:lunarcalendar` 实现公历转农历
  - ✅ 天干地支计算：支持年、月、日的天干地支计算
  - ✅ 生肖计算：根据年份计算生肖
  - ✅ 节气判断：使用库提供的节气信息
  - ✅ 节日判断：支持农历节日和公历节日
  - ✅ 农历显示：在月/周/日视图中完整显示农历信息
- **待完善部分**：
  - ⚠️ **农历转公历**：未实现农历日期转公历日期功能
  - ⚠️ **按农历创建事件**：未实现按农历日期创建事件的功能
  - ⚠️ **农历重复事件**：未支持农历重复事件（如每年农历生日）

---

## 最新代码优化（2024年）

### ✅ 已完成的代码优化

#### 1. CalendarViewModel 代码重构
- **优化内容**：提取重复的订阅事件获取逻辑为通用函数 `getSubscriptionEventsFlow()`
- **优化效果**：
  - 减少代码重复约 40 行
  - `subscriptionEventsForSelectedDate` 和 `subscriptionEventsForNext5Days` 两个 StateFlow 共享通用逻辑
  - 提升代码可维护性，后续如需修改订阅事件获取逻辑只需修改一处
- **文件位置**：`app/src/main/java/com/example/calendar/ui/CalendarViewModel.kt`

#### 2. EventRepository 性能优化
- **优化内容**：改进保存事件后的查询逻辑，直接使用已保存的事件对象
- **优化效果**：
  - 避免了不必要的 `getAllEvents()` 全表查询
  - 在保存事件后直接使用已保存的事件对象设置提醒，提升性能
  - 减少数据库查询次数，特别是在处理重复事件时效果明显
- **文件位置**：`app/src/main/java/com/example/calendar/data/EventRepository.kt` (第 87-112 行)

#### 3. MainActivity 代码结构优化与冗余代码清理
- **优化内容**：
  - 删除冗余的默认订阅初始化代码（约 30 行）：移除了首次启动时自动创建天气和黄历订阅的逻辑，因为用户可以在 UI 中手动创建订阅，且 URL 字段在实际同步时未被使用
  - 简化并重命名函数：将 `initializeDefaultSubscriptions()` 重命名为 `syncSubscriptionsOnStartup()`，仅保留启动时的订阅同步检查逻辑
  - 删除未使用的导入：移除 `android.content.Context` 导入（`applicationContext` 是 `ComponentActivity` 的成员）
  - 删除空代码块：移除 `navigationIcon` 中空的 if 语句块
- **优化效果**：
  - 减少代码约 30 行冗余代码
  - 函数职责更单一，名称更准确
  - 代码更简洁，无冗余导入和空代码块
  - 逻辑更合理：用户可在 UI 中按需创建订阅，无需自动创建默认订阅
- **文件位置**：`app/src/main/java/com/example/calendar/MainActivity.kt`

#### 4. SubscriptionScreen 代码清理
- **优化内容**：
  - 删除未使用的导入：
    - `androidx.compose.foundation.clickable`
    - `androidx.compose.material.icons.filled.ArrowBack`
    - `androidx.compose.material3.Icon`
    - `androidx.compose.material3.IconButton`
    - `androidx.compose.material3.TopAppBar`
    - `androidx.compose.material3.TopAppBarDefaults`
    - `androidx.compose.foundation.layout.width`
    - `androidx.compose.material.icons.Icons`
  - 添加注释说明：为 URL 字段添加注释，说明当前未被使用（同步逻辑直接调用 API 服务）
- **优化效果**：
  - 代码更简洁，无冗余导入
  - 注释更完善，避免混淆
- **文件位置**：`app/src/main/java/com/example/calendar/ui/SubscriptionScreen.kt`

#### 5. 代码清理
- **优化内容**：
  - 删除未使用的导入（`CalendarViewModel.kt` 中的 `flow` 导入）
  - 更新 `todo.md`，删除已实现功能的过时内容
- **优化效果**：
  - 代码更简洁，无冗余导入
  - 文档更准确，反映当前实现状态

### 优化效果统计
- **代码行数减少**：约 80-90 行冗余代码（包括重复代码和未使用的导入）
- **性能提升**：避免了一次全表查询，特别是在处理重复事件时
- **可维护性提升**：代码结构更清晰，函数职责更单一，无冗余导入和空代码块
- **文档准确性**：`todo.md` 反映当前实现状态

---

## 需要完善的内容

### 待完善功能（可选功能）

#### 1. 固定时间提醒（低优先级）
- **当前状态**：当前只支持提前提醒，不支持固定时间提醒
- **需要完善**：
  - 在 `EventEditorDialog` 中添加"固定时间提醒"选项
  - 扩展 `Reminder` 实体，支持固定时间提醒类型
  - 在 `EventRepository` 中处理固定时间提醒逻辑

#### 2. 农历转公历功能（中优先级）
- **当前状态**：已使用第三方库 `io.github.xhinliang:lunarcalendar` 实现公历转农历，但未实现农历转公历
- **需要完善**：
  - 实现农历转公历功能（可使用第三方库或自行实现算法）
  - 支持闰月处理
  - 用于实现"按农历日期创建事件"功能

#### 3. 网络 API 配置（高优先级）⚠️
- **当前状态**：API 服务接口已创建，但使用示例 URL（`http://example.com/weather` 和 `http://example.com/huangli`）
- **注意**：URL 字段当前未被使用，同步逻辑直接调用 API 服务（`weatherApiService` 和 `huangliApiService`），但字段在数据库中存在，未来可能用到
- **需要完善**：
  - 在 `RetrofitClient.kt` 中配置真实的天气 API 地址（推荐：和风天气、OpenWeatherMap、心知天气等）
  - 在 `RetrofitClient.kt` 中配置真实的黄历 API 地址（推荐：聚合数据、天行数据等）
  - 根据实际 API 文档调整数据模型（`WeatherData.kt`、`HuangliData.kt`）
  - 添加 API Key 配置（建议使用 `local.properties` 或环境变量，不要提交到版本控制）
  - 如需使用 URL 字段，可在 `SubscriptionScreen.kt` 中更新创建订阅时的 URL（第89行）

#### 4. 后台同步服务（中优先级）⚠️
- **当前状态**：支持手动同步（在订阅管理界面点击"立即同步"），但未实现定时自动同步
- **需要完善**：
  - 添加 WorkManager 依赖到 `build.gradle.kts`
  - 创建 `SubscriptionSyncWorker` 实现定时同步任务
  - 配置同步策略（如每天凌晨2点自动同步，或每6小时同步一次）
  - 添加同步状态和进度显示（在订阅管理界面显示"最后同步时间"和"同步中..."状态）
  - 实现同步失败重试机制（WorkManager 自动重试，或手动重试按钮）
  - 处理网络不可用情况（延迟同步直到网络可用）

#### 5. 订阅事件展示优化（中优先级）⚠️
- **当前状态**：基础展示已实现，天气卡片显示当前天气，黄历卡片显示完整信息
- **需要完善**：
  - **15日天气预报展开功能**：在 `SubscriptionEventItem.kt` 中实现展开/收起功能（第160行有 TODO 注释）
    - 点击"15日天气 >"展开显示15日预报列表
    - 使用 `AnimatedVisibility` 实现平滑展开/收起动画
  - **快速订阅按钮**：在订阅事件卡片中添加"订阅"/"取消订阅"按钮（当前需要在订阅管理界面操作）
  - **排序优化**：确保订阅事件和普通日程按时间顺序正确排列（当前已实现，但可进一步优化）

#### 6. 农历事件创建（低优先级）⚠️
- **当前状态**：未实现，所有事件都按公历日期创建
- **需要完善**：
  - 在 `EventEditorDialog.kt` 中添加"按农历日期"选项（单选按钮：公历/农历）
  - 实现农历日期选择器（年、月、日、是否闰月）
  - 将农历日期转换为公历日期进行存储（需要先实现农历转公历功能）
  - 支持农历重复事件（如每年农历生日，需要计算每年的对应公历日期）
  - 在事件详情中显示"按农历日期创建"标记

#### 7. 错误处理和测试（中优先级）
- **当前状态**：基础错误处理已实现
- **需要完善**：
  - 完善网络请求错误处理（网络超时、API 错误等）
  - 添加单元测试
  - 优化错误提示的用户体验
