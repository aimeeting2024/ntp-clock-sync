# NTP后台同步服务

## 项目概述

这是一个纯净的NTP（Network Time Protocol）后台同步服务，配置完成后在后台运行，不会干扰其他程序。

## 主要特性

- ✅ **后台运行**：配置完成后在后台运行，不显示界面
- ✅ **不干扰其他程序**：完全在后台运行，不影响其他应用
- ✅ **通知栏状态**：通过通知栏显示同步状态
- ✅ **可配置服务器**：支持自定义NTP服务器地址
- ✅ **可配置间隔**：支持自定义同步间隔
- ✅ **自动重启**：服务被杀死后自动重启

## 使用方法

### 1. 启动应用
启动应用后会显示配置界面。

### 2. 配置NTP服务器
- 在输入框中输入NTP服务器地址
- 或点击预设服务器按钮快速选择：
  - Windows (time.windows.com)
  - Google (time.google.com)
  - 中国NTP (cn.pool.ntp.org)

### 3. 配置同步间隔
选择同步间隔：
- 5分钟（测试用）
- 15分钟（频繁同步）
- 30分钟（推荐）
- 60分钟（省电模式）

### 4. 启动后台服务
点击"启动后台服务"按钮，应用会：
- 启动NTP后台服务
- 显示启动成功提示
- 2秒后自动退出应用

### 5. 查看运行状态
- **通知栏**：显示同步状态、偏移量、服务器信息
- **日志**：通过logcat查看详细日志

## 后台服务特性

### 前台服务
- 使用前台服务确保不被系统杀死
- 低优先级通知，不打扰用户
- 显示同步状态和偏移信息

### 自动重启
- 服务被杀死后自动重启
- 保持NTP同步的连续性

### 状态监控
- 实时监控同步状态
- 定期更新通知信息
- 记录详细日志

## 通知栏信息

通知栏会显示以下信息：
- 同步状态（同步中/已停止）
- 时间偏移量
- 服务器地址
- 最后同步时间

## 技术实现

### 核心组件
- **NTPBackgroundService.kt**：后台服务实现
- **MainActivity.kt**：配置界面
- **NTPManager.kt**：NTP管理器
- **NTPClient.kt**：NTP客户端

### 权限要求
- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态检查
- `FOREGROUND_SERVICE` - 前台服务
- `FOREGROUND_SERVICE_DATA_SYNC` - 数据同步服务
- `POST_NOTIFICATIONS` - 发送通知

### 服务生命周期
1. 应用启动 → 显示配置界面
2. 用户配置 → 启动后台服务
3. 服务启动 → 应用退出
4. 后台运行 → 定期同步
5. 服务被杀死 → 自动重启

## 日志输出

```
D/MainActivity: 启动NTP后台服务: 服务器=time.windows.com, 间隔=30分钟
D/NTPBackgroundService: NTP后台服务创建
D/NTPBackgroundService: NTP后台服务启动
D/NTPBackgroundService: 启动NTP后台同步，服务器: time.windows.com，间隔: 30分钟
D/NTPClient: 开始从NTP服务器获取时间: time.windows.com
D/NTPClient: NTP时间获取成功: 1703123456789, 延迟: 45ms, 偏移: 123ms
D/NTPBackgroundService: NTP初始同步成功: 偏移=123ms
D/NTPBackgroundService: NTP状态: 同步中, 偏移: +123ms
```

## 停止服务

如需停止NTP后台服务，可以：
1. 在系统设置中强制停止应用
2. 通过ADB命令：`adb shell am stopservice com.mms.meetingroom/.NTPBackgroundService`

## 配置建议

### NTP服务器选择
- **国内用户**：推荐使用 `cn.pool.ntp.org`
- **国际用户**：推荐使用 `time.google.com`
- **企业环境**：建议使用企业内部的NTP服务器

### 同步间隔选择
- **测试环境**：5-15分钟
- **生产环境**：30-60分钟
- **省电模式**：60分钟或更长

## 注意事项

1. **前台服务**：应用会显示持续通知，这是Android系统的要求
2. **电池优化**：建议将应用加入电池优化白名单
3. **网络权限**：确保应用有网络访问权限
4. **通知权限**：Android 13+需要通知权限

## 故障排除

### 服务无法启动
- 检查网络权限
- 检查前台服务权限
- 查看logcat错误信息

### 同步失败
- 检查网络连接
- 尝试更换NTP服务器
- 检查防火墙设置

### 通知不显示
- 检查通知权限
- 检查通知渠道设置
- 重启应用

## 项目结构

```
app/src/main/java/com/mms/meetingroom/
├── MainActivity.kt              # 配置界面
├── NTPSyncApplication.kt        # 应用类
├── NTPBackgroundService.kt      # 后台服务
├── ntp/
│   ├── NTPClient.kt            # NTP客户端
│   └── NTPManager.kt           # NTP管理器
└── ui/component/
    └── NTPStatusComponent.kt   # 状态显示组件
```

## 构建和运行

1. 确保Android Studio已安装
2. 打开项目
3. 连接Android设备或启动模拟器
4. 点击运行按钮
5. 配置NTP服务器和同步间隔
6. 点击"启动后台服务"
7. 应用退出后，服务在后台运行

## 扩展功能

如需添加更多功能，可以考虑：
- 开机自启动
- 多服务器轮询
- 同步历史记录
- 更详细的统计信息
- 远程控制接口 