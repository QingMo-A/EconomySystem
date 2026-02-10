# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在此代码库中工作的指导。

## 项目概述

Economy System 是一个 Minecraft Forge 模组（版本 1.20.1），实现了综合的经济和 RPG 系统，包括：
- 虚拟货币（金币）系统，包含商店、市场和交易（卖单/求单）
- 领地 claiming 和管理，带有增益效果（使用 QuadTree 空间索引）
- 玩家进度系统（等级、属性如力量/勇气/感染度）
- 社交功能（传送、红包、称号、聊天格式化）
- 登录系统和快递箱系统
- 服务器 UI 系统（自定义提示、系统消息、死亡消息）
- 故事/阶段系统、蓝图系统、公告系统、任务系统

## 构建命令

```bash
# 构建模组（编译 Java）
gradlew.bat compileJava --no-daemon

# 完整构建（运行测试并创建 JAR）
gradlew.bat build

# 清理构建产物
gradlew.bat clean

# 运行客户端进行测试
gradlew.bat runClient

# 运行服务器进行测试
gradlew.bat runServer

# 生成数据（模型、语言文件、战利品表）
gradlew.bat runData

# 生成 IDE 运行配置
gradlew.bat genIntellijRuns    # IntelliJ IDEA
gradlew.bat genEclipseRuns     # Eclipse
```

**重要提示：**
- 在 Windows 上，始终使用 `gradlew.bat` 而不是 `./gradlew`
- 推荐使用 `--no-daemon` 标志（已在 `gradle.properties` 中设置 `org.gradle.daemon=false`）
- 当前版本：1.2.03（在 `mods.toml` 中定义）

## 架构

### 包结构

```
src/main/java/com/mo/economy_system/
├── commands/              # 命令实现（按子系统组织）
│   ├── economy_system/    # 经济命令
│   ├── territory_system/  # 领地命令
│   ├── tpa_system/        # 传送命令
│   ├── rank_system/       # 等级命令
│   ├── title_system/      # 称号命令
│   ├── level_system/      # 等级/生物群系命令
│   ├── task_system/       # 任务命令
│   ├── notice_system/     # 公告命令
│   └── check_system/      # 检查命令
├── core/                  # 核心业务逻辑和管理器
│   ├── economy_system/    # 经济系统（商店、市场、奖励、快递、红包）
│   ├── territory_system/  # 领地管理（QuadTree 空间索引）
│   ├── playerlevel_system/# 玩家等级和进度（生物群系探索、怪物击杀、成就奖励）
│   ├── playerattributes_system/ # RPG 属性（力量、勇气、感染度、死亡重生）
│   ├── login_system/      # 玩家登录认证
│   ├── task_system/       # 任务系统
│   ├── story_system/      # 故事/阶段系统
│   ├── blueprint_system/  # 蓝图系统
│   └── update_checker_system/ # 更新检查
├── network/packets/       # 网络数据包（按子系统组织）
│   ├── economy_system/    # 经济数据包
│   ├── territory_system/  # 领地数据包
│   ├── playerattribute_system/ # 属性数据包
│   ├── playerdata_system/ # 玩家数据包
│   ├── task_system/       # 任务数据包
│   ├── login_system/      # 登录数据包
│   ├── notice_system/     # 公告数据包
│   ├── tip_system/        # 提示数据包
│   └── check_system/      # 检查数据包
├── server/                # 服务端逻辑
│   ├── playerdata/        # 玩家数据管理
│   ├── playerbiomes/      # 生物群系数据
│   ├── rank/              # 等级系统
│   ├── chattitle/         # 聊天称号和格式化
│   ├── notice/            # 公告系统
│   ├── serverui/          # 服务器 GUI 界面（新玩家帮助，服务器故事进度，玩家个人档案，服务器排行榜）
│   └── headdisplay/       # 头部显示
├── screen/                # GUI 界面（经济、市场、领地等）
├── item/                  # 自定义物品（领地权杖、药水、吉他等）
├── entity/                # 自定义实体
├── armor/                 # 自定义盔甲（使用 GeckoLib 动画）
├── enchant/               # 自定义附魔（赏金猎人、小心）
├── mixin/                 # Mixin 类用于修改原版行为
│   ├── death/             # 死亡屏幕相关
│   └── ui/                # UI 屏幕相关
├── datagen/               # 数据生成器
├── init/                  # 初始化工具（创建配置目录）
└── EconomySystem.java     # 主类
```

### 核心架构模式

**管理器模式（Manager Pattern）：** 每个子系统都有专门的管理器类（如 `ShopManager`、`TerritoryManager`、`PlayerStrengthManager`），负责：
- 从 `config/economy_system/` 加载 JSON 配置
- 处理业务逻辑
- 在 `EconomySystem` 主类中实例化为静态字段
- 通过文件监听器支持热重载（如 `ShopConfigWatcher`）

**网络层：** 所有客户端-服务端通信使用 Forge 的 `SimpleChannel`：
- 数据包在 `EconomySystem_NetworkManager.register()` 中注册，使用递增的 packetId
- 按子系统组织在 `network/packets/` 中
- 每个数据包实现 `encode()`、`decode()` 和 `handle()` 方法
- 使用 `EconomySystem_NetworkManager.sendToClient()` 发送数据包到客户端
- 单向数据包（仅客户端接收）注册时指定 `Optional.of(NetworkDirection.PLAY_TO_CLIENT)`

**事件驱动：** 使用 Forge 的事件总线系统：
- 通过 `@SubscribeEvent` 注解订阅
- 每个子系统有模块化的事件处理器

**配置系统：**
- 基于 JSON 的配置，存储在 `config/economy_system/`
- 每个子系统有独立的配置文件（商店、奖励、领地、任务等）
- 文件监听器支持运行时配置更新，无需重启服务器
- 配置目录由 `Init` 类在模组初始化时自动创建

**数据持久化：**
- JSON 文件用于玩家数据（`config/economy_system/data/player_data.json`）
- 服务端数据存储在 `config/economy_system/data/` 目录下
- 玩家数据使用 `ConcurrentHashMap` 缓存，支持自动保存

### Mixins

通过 mixin 修改原版 Minecraft 行为（定义在 `economy_system.mixins.json` 中）：

**服务端 Mixins（server）：**
- `CraftingMenuMixin` - 自定义配方处理
- `RecipeManagerMixin` - 配方系统修改

**通用 Mixins（mixins）：**
- `RespawnMixin` (death/) - 重生行为
- `BedBlockMixin` - 床行为更改
- `ServerLevelMixin` - 世界级别修改
- `AnvilBlockMixin` - 铁砧行为
- `SystemMessageMixin` - 系统消息

**客户端 Mixins（client）：**
- `DeathScreenMixin` (death/) - 死亡屏幕自定义
- `DisconnectedScreenMixin` (ui/) - 断开连接屏幕
- `ConnectScreenMixin` (ui/) - 连接屏幕
- `ReceivingLevelScreenMixin` (ui/) - 接收世界加载屏幕
- `LevelLoadingScreenMixin` (ui/) - 关卡加载屏幕
- `GenericDirtMessageScreenMixin` (ui/) - 通用消息屏幕
- `GenericWaitingScreenMixin` (ui/) - 通用等待屏幕
- `TitleScreenMixin` (ui/) - 标题屏幕
- `SelectWorldScreenMixin` (ui/) - 世界选择屏幕
- `JoinMultiplayerScreenMixin` (ui/) - 多人游戏屏幕

### 依赖项

- **GeckoLib 4.8.2** - 用于 3D 模型和盔甲的高级动画
- **SQLite + HikariCP 5.0.1** - 数据库和连接池
- **SpongePowered Mixin 0.7+** - 代码注入
- **Gson** - JSON 序列化
- **MCLib 20** - GeckoLib 依赖
- **JEI 15.20.0.127** - 物品显示界面（仅编译时）

## 开发工作流

1. **IDE 设置：** 导入为 Gradle 项目，运行 `genIntellijRuns` 或 `genEclipseRuns`
2. **配置位置：** `config/economy_system/`（由 `Init` 类在模组初始化时自动创建）
3. **测试：** 使用 `runClient`/`runServer` 测试更改
4. **数据生成：** 使用 `runData` 生成资源；输出到 `src/generated/resources/`
5. **版本：** 模组版本在 `mods.toml` 中定义，构建时由 Gradle 读取

## 常见模式

**添加新子系统功能：**
1. 在 `core/[subsystem]/` 中创建管理器类，实现配置加载
2. 在 `commands/[subsystem]/` 中创建命令类（使用 `Commands` 类注册）
3. 在 `network/packets/[subsystem]/` 中创建网络数据包
4. 在 `EconomySystem_NetworkManager.register()` 中注册数据包（递增 packetId）
5. 可选：创建配置监听器（参考 `ShopConfigWatcher` 模式）以支持热重载

**数据包注册模式：**
```java
INSTANCE.registerMessage(packetId++, Packet_YourPacket.class,
    Packet_YourPacket::encode, Packet_YourPacket::decode,
    Packet_YourPacket::handle,
    Optional.of(NetworkDirection.PLAY_TO_CLIENT)); // 可选，仅客户端数据包需要
```

**SavedData 模式（持久化到世界数据）：**
- 继承 `SavedData` 类（如 `TerritorySavedData`、`MarketSavedData`、`EconomySavedData`）
- 使用 `ServerLevel.getDataStorage()` 保存和加载
- 数据存储在世界目录的 `data/` 文件夹中

## 模组信息

- **模组 ID：** `economy_system`
- **主类：** `com.mo.economy_system.EconomySystem`
- **包名：** `com.mo.economy_system`
- **Java 版本：** 17
- **Minecraft 版本：** 1.20.1
- **Forge 版本：** 47.3.20
- **日志记录器：** 通过 `EconomySystem.LOGGER` 访问

## 管理器列表

在 `EconomySystem` 主类中实例化的静态管理器：
- `SHOP_MANAGER` - 商店系统管理器
- `REWARD_MANAGER` - 奖励系统管理器

其他重要管理器（通过事件系统或其他方式初始化）：

**经济系统：**
- `MarketManager` - 市场交易管理（卖单/求单）
- `RedPacketManager` - 红包管理

**领地系统：**
- `TerritoryManager` - 领地管理（QuadTree 空间索引）
- `TerritoryBuffManager` - 领地增益效果

**玩家属性：**
- `PlayerLevelManager` - 玩家等级系统
- `PlayerStrengthManager` - 力量/体力系统（冲刺、跳跃消耗）
- `PlayerCourageManager` - 勇气属性系统
- `PlayerInfectionManager` - 感染度系统
- `PlayerCustomHealthManager` - 自定义血量

**数据管理：**
- `TaskDataManager` - 任务数据管理
- `PlayerDataManager` - 玩家数据持久化
- `PlayerBiomesDataManager` - 生物群系数据
- `PlayerTitleManager` - 称号系统
- `PlayerRankManager` - 等级系统
- `PlayerLoginDataManager` - 登录数据
- `NoticeManager` - 公告系统
- `StoryStageManager` - 故事阶段管理

## CRITICAL: File Editing on Windows

### ⚠️ MANDATORY: Always Use Backslashes on Windows for File Paths

**When using Edit or MultiEdit tools on Windows, you MUST use backslashes (`\`) in file paths, NOT forward slashes (`/`).**

#### ❌ WRONG - Will cause errors:
```
Edit(file_path: "D:/repos/project/file.tsx", ...)
MultiEdit(file_path: "D:/repos/project/file.tsx", ...)
```

#### ✅ CORRECT - Always works:
```
Edit(file_path: "D:\repos\project\file.tsx", ...)
MultiEdit(file_path: "D:\repos\project\file.tsx", ...)
```

## 编码规范

### ⚠️ MANDATORY: 字符串必须使用常量

**所有字符串（尤其是 UI 文本、按钮文本、版权信息等）必须定义为常量，禁止在代码中硬编码字符串。**

#### ❌ WRONG - 硬编码字符串：
```java
guiGraphics.drawString(font, "单人游戏", x, y, color, false);
guiGraphics.drawString(font, "§7Minecraft §f1.20.1", 5, virtualH - 10, TEXT_GRAY, false);
```

#### ✅ CORRECT - 使用常量：
```java
// 在类顶部定义常量
private static final String BUTTON_SINGLEPLAYER = "单人游戏";
private static final String MINECRAFT_VERSION = "§7Minecraft §f1.20.1 §8Copyright Mojang AB. Do not distribute!";

// 使用常量
guiGraphics.drawString(font, BUTTON_SINGLEPLAYER, x, y, color, false);
guiGraphics.drawString(font, MINECRAFT_VERSION, 5, virtualH - 10, TEXT_GRAY, false);
```

#### 原因：
1. **易于维护**：修改文本时只需改一处
2. **避免拼写错误**：常量名拼写错误会在编译时报错
3. **代码可读性**：常量名更清晰地表达字符串用途
4. **国际化准备**：未来如需多语言支持，常量更易于提取到资源文件

## 配置文件结构

配置文件位于 `config/economy_system/`，由 `Init` 类在模组初始化时自动创建：

- `economy_shop.json` - 商店配置（物品、价格）
- `economy_rewards.json` - 奖励配置
- `economy_titles.json` - 称号配置
- `territory_buffs.json` - 领地增益配置
- `data/player_data.json` - 玩家数据
- `data/player_attributes_data.json` - 玩家属性数据
- `data/player_biomes_data.json` - 玩家生物群系探索数据
- `task_player_data.json` / `task_server_data.json` - 任务数据
- `economy_system-join_message.toml` - 加入消息配置

## 网络数据包注册

当前已注册 127+ 数据包（packetId 0-127），按系统组织。添加新数据包时：

1. 在 `network/packets/[subsystem]/` 中创建数据包类
2. 实现 `encode()`、`decode()` 和 `handle()` 方法
3. 在 `EconomySystem_NetworkManager.register()` 中注册
4. 客户端专用数据包需要指定 `Optional.of(NetworkDirection.PLAY_TO_CLIENT)`

## Bash 命令在 Windows 上的注意事项

### ⚠️ 路径包含空格、中文或特殊字符时的处理

当在 Windows 上使用 Bash 工具执行命令时，如果路径包含空格、中文字符或特殊字符（如 `#`），直接使用 `cmd /c` 或普通 bash 命令会失败。

#### ❌ WRONG - 会失败：
```bash
# 直接执行会因引号解析问题失败
copy "D:\Desktop\EconomySystem\build\libs\economy_system-1.2.03 #mandatory.jar" "D:\Desktop\mc\.minecraft\versions\Dreamingfish-EP01\mods\"

# cmd /c 也会失败
cmd /c copy "D:\Desktop\EconomySystem\build\libs\economy_system-1.2.03 #mandatory.jar" "D:\Desktop\mc\.minecraft\versions\Dreamingfish-EP01\mods\"
```

#### ✅ CORRECT - 使用 PowerShell：
```bash
# 对于文件操作（复制、移动等）
powershell -Command "Copy-Item 'D:\Desktop\EconomySystem\build\libs\economy_system-1.2.03 #mandatory.jar' 'D:\Desktop\mc\.minecraft\versions\Dreamingfish-EP01\mods\'"

# 对于列出目录
powershell -Command "Get-ChildItem 'D:\Desktop\mc\.minecraft\versions\Dreamingfish-EP01\mods\' | Select-Object Name"

# 对于删除文件
powershell -Command "Remove-Item 'D:\path\to\file.jar'"

# 对于创建目录
powershell -Command "New-Item -ItemType Directory -Path 'D:\path\to\new\folder'"
```

#### 常用 PowerShell 命令对照：
| 操作 | CMD | PowerShell |
|------|-----|------------|
| 复制文件 | `copy` | `Copy-Item` |
| 移动文件 | `move` | `Move-Item` |
| 删除文件 | `del` | `Remove-Item` |
| 列出目录 | `dir` | `Get-ChildItem` |
| 创建目录 | `mkdir` | `New-Item -ItemType Directory` |

#### 原因：
- Git Bash 在 Windows 上对包含中文和特殊字符的路径处理不佳
- `cmd /c` 中的引号嵌套解析容易出错
- PowerShell 对 Unicode 路径的支持更好，且单引号字符串不会解析转义字符
