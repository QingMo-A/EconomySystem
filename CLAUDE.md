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

## 构建命令

```bash
# 构建模组（运行测试并创建 JAR）
gradlew.bat build

# 无守护进程构建（Windows）
gradlew.bat build --no-daemon

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

**重要提示：** 在 Windows 上，始终使用 `gradlew.bat` 而不是 `./gradlew`。如果遇到构建问题，添加 `--no-daemon` 标志。

## 架构

### 包结构

```
src/main/java/com/mo/economy_system/
├── commands/              # 命令实现（按子系统组织）
├── core/                  # 核心业务逻辑和管理器
│   ├── economy_system/    # 经济系统（商店、市场、奖励、快递）
│   ├── territory_system/  # 领地管理
│   ├── playerlevel_system/# 玩家等级
│   ├── playerattributes_system/ # RPG 属性（力量、勇气、感染度）
│   ├── login_system/      # 玩家登录认证
│   └── task_system/       # 任务系统
├── network/packets/       # 网络数据包（按子系统组织）
├── server/                # 服务端逻辑（UI、玩家数据、等级）
├── screen/                # GUI 界面
├── item/                  # 自定义物品
├── entity/                # 自定义实体
├── armor/                 # 自定义盔甲（使用 GeckoLib 动画）
├── enchant/               # 自定义附魔
├── mixin/                 # Mixin 类用于修改原版行为
└── init/                  # 初始化工具
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
- `CraftingMenuMixin` - 自定义配方处理
- `RecipeManagerMixin` - 配方系统修改
- `BedBlockMixin` - 床行为更改
- `ServerLevelMixin` - 世界级别修改
- `AnvilBlockMixin` - 铁砧行为
- `PlayerListMixin` - 玩家列表管理

### 依赖项

- **GeckoLib 4.8.2** - 用于 3D 模型和盔甲的高级动画
- **SQLite + HikariCP** - 数据库和连接池（HikariCP 5.0.1）
- **SpongePowered Mixin 0.7+** - 代码注入
- **Gson** - JSON 序列化
- **MCLib 20** - GeckoLib 依赖

## 开发工作流

1. **IDE 设置：** 导入为 Gradle 项目，运行 `genIntellijRuns` 或 `genEclipseRuns`
2. **配置位置：** `config/economy_system/`（由 `Init` 类自动创建）
3. **测试：** 使用 `runClient`/`runServer` 测试更改
4. **数据生成：** 使用 `runData` 生成资源；输出到 `src/generated/resources/`
5. **版本：** 模组版本在构建时从 `src/main/resources/META-INF/mods.toml` 解析

## 常见模式

添加新子系统功能时：
1. 在 `core/` 中创建管理器类，实现配置加载
2. 在 `commands/` 中创建命令类
3. 在 `network/packets/[subsystem]/` 中创建网络数据包
4. 在 `EconomySystem_NetworkManager.register()` 中注册数据包（递增 packetId）
5. 将 JSON 配置文件添加到 `config/economy_system/`
6. 可选：创建配置监听器（继承 `ShopConfigWatcher` 模式）以支持热重载

**数据包注册模式：**
```java
INSTANCE.registerMessage(packetId++, Packet_YourPacket.class,
    Packet_YourPacket::encode, Packet_YourPacket::decode,
    Packet_YourPacket::handle,
    Optional.of(NetworkDirection.PLAY_TO_CLIENT)); // 可选，仅客户端数据包需要
```

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
- `MarketManager` - 市场交易管理
- `TerritoryManager` - 领地管理（QuadTree 空间索引）
- `TerritoryBuffManager` - 领地增益效果
- `PlayerLevelManager` - 玩家等级系统
- `PlayerStrengthManager` - 力量/体力系统（冲刺、跳跃消耗）
- `PlayerCourageManager` - 勇气属性系统
- `PlayerInfectionManager` - 感染度系统
- `TaskDataManager` - 任务数据管理
- `PlayerDataManager` - 玩家数据持久化
- `PlayerTitleManager` - 称号系统
- `PlayerRankManager` - 等级系统

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
