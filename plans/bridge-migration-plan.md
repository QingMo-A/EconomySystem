# EconomySystem 多版本 Bridge 迁移计划

## 1. 目标与基线

- 业务行为唯一基线：NeoForge 1.21.1 当前代码。
- Forge 1.20.1 历史分支仅用于查询旧版本 API，不恢复过时业务逻辑。
- Bridge 架构参考：QingMo-s-Grid-Inventory 的 `BridgeDevIn1.20.1` 分支。
- 支持目标：
  - `targets/neoforge-1.21.1`：Java 21；
  - `targets/forge-1.20.1`：Java 17；
  - `common`：两端重新编译的共享行为与协议，不是独立发布模块。

## 2. 不可破坏的约束

1. `EconomyProtocol` 是 44 条消息的唯一协议清单，版本为 `bridge-1`。
2. 消息 ID、方向和 Forge discriminator `0..43` 是追加式线协议，不得重排或复用。
3. common 禁止导入 `net.neoforged` 或 `net.minecraftforge`。
4. loader 注册、codec、事件、SavedData API 和 Data Components/NBT 差异放在对应 target。
5. 金钱、物品、订单和权限操作必须由服务端重新校验。
6. 所有金额乘法先用 `long` 检查，再安全转换为 `int`。
7. 修改 SavedData 后必须标记 dirty；经济数据统一存储于主世界。
8. 跨版本物品转换必须 fail closed：无法无损转换时拒绝操作，不得静默丢组件。
9. 不覆盖或清理用户的 `dev2/`、素材库、运行配置和其他未明确纳入迁移的文件。

## 3. 当前已完成

### 构建与平台层

- 根 Gradle 聚合两个 target，并提供 `compileAllTargets`、`buildAllTargets`。
- common platform services、网络桥和 ItemStack 基础桥已经建立。
- NeoForge/Forge 各自拥有同 FQN 的 `EconomySavedData` 壳，共享 `EconomyLedger`。
- 账户、离线消息、余额日志使用一致的 NBT schema。

### 已迁移协议

| discriminator | 消息 | 状态 |
|---:|---|---|
| 0/1 | 余额请求/响应 | 两端完成 |
| 2/3 | 余额日志请求/响应 | 两端完成 |
| 4 | 在线玩家转账 | 两端完成；原子转账、跨维度查找、溢出保护 |
| 5/6 | 系统商店目录请求/响应 | 两端完成；不可变客户端快照、长度/数量上限 |
| 7 | 系统商店购买 | 两端完成；事务式发货、背包回滚、退款保护 |
| 34/35 | 服务器玩家列表请求/响应 | 两端完成 |

当前 common 中有 10 个语义消息类；旧 NeoForge packet 仍保留 34 个，后续逐组替换。

### 已修复问题

- 收款方余额溢出时不再先扣除转出方余额。
- 转账通过服务器玩家列表查找，可跨维度完成。
- 商店购买总价不再发生 `int` 乘法溢出。
- 发货失败会先恢复背包快照，再退款。
- 价格统计保存失败不再造成“物品到账且退款”的双重收益。
- Forge 对支持的 1.21 组件显式转换为 1.20 NBT；未知组件和不存在物品直接拒绝。
- Forge 更新商店 JSON 时使用临时文件和原子替换。

## 4. 下一阶段顺序

### 阶段 A：稳定跨版本物品快照

在迁移市场和配送箱前，建立版本化、可测试的组件 schema：

1. 定义 loader-neutral `ItemStackSnapshot`，包含物品 ID、数量和明确列出的组件。
2. 覆盖名称、Lore、附魔、耐久、修复花费、不可破坏、染色、自定义模型数据、自定义数据。
3. 为未知组件设置显式错误结果，禁止静默降级。
4. NeoForge 1.21.1 和 Forge 1.20.1 分别实现 encode/decode。
5. 保留旧 compact NBT schema 的读取兼容；新写入使用带 schema version 的格式。
6. 增加黄金样例测试和两个版本的往返测试。

### 阶段 B：市场读取与订单创建

按协议顺序迁移：

1. `8` 创建销售订单；
2. `9` 创建求购订单；
3. `10/11` 市场数据请求/响应。

迁移时先修复以下设计：

- 将订单持久化类型从 Java 完整类名改为稳定 ID，同时兼容旧存档类名。
- 服务器重新读取玩家手中物品、数量、价格和税费；不相信客户端 ItemStack/价格。
- 创建销售订单时，扣物品与写订单必须具备回滚路径。
- 创建求购订单时，冻结金额与写订单必须具备回滚路径。
- 市场响应必须设置条目数量和单条物品数据上限。

### 阶段 C：订单成交、取消与配送箱

迁移 `12..18`，然后迁移配送箱 `31..33`：

- 成交状态变化必须幂等，防止重复点击或重放包重复交付。
- 买家扣款、卖家入账、订单移除、配送物品采用明确事务顺序与补偿路径。
- 订单取消/过期必须验证所有者并保证退款或退物只执行一次。
- `DeliveryItem.getItemStack()` 返回副本，列表查询返回不可变快照。
- 配送箱领取先验证所有权和背包容量，成功放入后再删除记录。

### 阶段 D：领地协议

迁移 `19..22`、`36..43`。当前 NeoForge 1.21.1 的领地权限/转让代码是业务基线，Forge 只实现等价适配。

### 阶段 E：高风险远程数据协议

`check/get/chunk`（`23..30`）暂不直接移植。开始前必须完成安全重构：

- 路径规范化与允许目录限制；
- 请求所有权和会话绑定；
- 单块/总大小/块数量上限；
- 超时和断线清理；
- 防重放、防跨请求拼接；
- 权限与服务端配置开关。

## 5. 每组消息的迁移模板

1. 在 common 定义不可变 message/DTO 和防御性 limits。
2. 抽取共享业务服务，确保服务端权威与失败补偿。
3. 在 `EconomyMessages` 绑定现有 `EconomyProtocol` spec。
4. 实现 NeoForge 1.21.1 codec/handler，并删除被替代的旧 packet。
5. 实现 Forge 1.20.1 SimpleChannel codec/handler/bridge 分派。
6. 客户端状态使用不可变、原子发布的 common cache。
7. 添加协议、边界、溢出、不可变性和失败回滚测试。
8. 验证两个 target、扫描跨 loader import、检查 Forge JAR 不含 NeoForge 类。
9. 更新 `common/README.md` 和 `targets/forge-1.20.1/README.md`。

## 6. 验证命令

Windows：

```powershell
.\gradlew.bat compileAllTargets --no-daemon
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
```

若 ForgeGradle 仅因远端 TLS 握手失败，可临时运行：

```powershell
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" buildAllTargets --no-daemon --rerun-tasks
```

不要把关闭证书检查写入项目配置。

完成每轮后还要执行：

```powershell
rg -n "net\.neoforged|net\.minecraftforge" common/src
git diff --check
```

并检查 Forge JAR 中不存在 NeoForge 类。

## 7. 当前验收状态

- NeoForge 1.21.1：26 项测试通过。
- Forge 1.20.1：26 项测试通过。
- `buildAllTargets --rerun-tasks` 通过。
- Forge JAR 已检查，不含 NeoForge target 类。
- Forge 1.20.1 仍是迁移中的开发 target，不应作为功能完整发行版发布。
