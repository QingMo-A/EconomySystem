# EconomySystem 多版本 Bridge 迁移计划

## 基线与约束

- NeoForge 1.21.1 当前代码是唯一业务基线。
- Forge 1.20.1 历史分支只用于查询旧 API，不恢复过时业务逻辑。
- `common` 保存共享语义和数据模型，不得导入 `net.neoforged` 或 `net.minecraftforge`。
- loader 注册、网络 codec、事件、SavedData API 以及 Data Components/NBT 差异留在对应 target。
- `EconomyProtocol` 的 44 条消息、方向和 Forge discriminator `0..43` 是追加式契约，不得重排或复用。
- 跨版本物品转换必须 fail closed；不能无损表示时拒绝，禁止静默丢字段。
- 不删除、覆盖或默认暂存用户的 `dev2/`、素材库、运行配置和其他未纳入迁移的文件。

## 已完成协议

两端已完成：余额 `0/1`、余额日志 `2/3`、转账 `4`、系统商店目录 `5/6`、系统商店购买 `7`、玩家列表 `34/35`。`23..30` 的远程文件分块协议必须先完成安全重构，暂不迁移。

## 阶段 A：稳定跨版本物品快照（已完成）

当前 schema version：`1`。

新写入固定结构：

```text
schemaVersion
id
count
components
```

`components` v1 支持：

- `customName`：稳定 JSON Component；
- `lore`：稳定 JSON Component 列表；
- `enchantments`、`storedEnchantments`：注册 ID、等级及 tooltip 标志；
- `damage`；
- `repairCost`；
- `unbreakable` 及 tooltip 标志；
- `dyedColor`（RGB）及 tooltip 标志；
- `customModelData`；
- `customData`（防御性复制的 CompoundTag）。

实现内容：

1. common 中的不可变 `ItemStackSnapshot`、严格 `ItemStackSnapshotCodec`、`ItemStackSnapshotResult<T>` 和明确错误枚举；
2. NeoForge 1.21.1 从 Data Components 捕获/恢复，捕获前检查整个非默认 component patch；
3. Forge 1.20.1 从原生 NBT/display/enchantment 结构捕获/恢复；
4. 未知 schema version、未知组件、错误字段类型、非法数量、不存在物品、未知附魔和无法无损转换均明确失败；
5. 集合与 NBT 输入、访问器及 codec 边界均做防御性复制；
6. 固定黄金 schema 样例、common 严格编解码测试和两个 target 的往返测试。

安全加固内容：

- 集中限制：物品/附魔 ID 256 字符；名称与单行 Lore 8192 字符；Lore 64 行、总计 32768 字符；普通/储存附魔各 64；customData 估算 32767 字节、最大 16 层；编码后 Snapshot 估算 65536 字节；
- `ItemStackSnapshot.create`、validator、decode、严格 encode、两端 capture/restore 使用相同限制，超限返回 `DATA_LIMIT_EXCEEDED`，禁止截断；
- Damage 规则对称：仅允许 0，或具有正最大耐久且不超过最大耐久的正值；
- 两端共同使用一份 schema-v1 黄金 fixture，执行 decode、restore、capture、encode 全链路比较。

最终验收：Forge 1.20.1 共 43 项测试、NeoForge 1.21.1 共 44 项测试，均无失败；`buildAllTargets --rerun-tasks` 通过后阶段 A 正式关闭。

明确不支持并 fail closed：

- NeoForge v1 allow-list 之外的任何显式非默认 Data Component，以及显式删除 component 的 patch；
- 未注册的直接附魔 Holder、目标注册表不存在的物品或附魔；
- Forge 属性修饰符、CanDestroy/CanPlaceOn、BlockEntityTag、EntityTag、ForgeCaps、未知 display 字段和未知 HideFlags；
- Forge 1.20.1 无法分别表达普通附魔与储存附魔 tooltip 可见性的组合；
- 任何越界数量、损伤、颜色或附魔等级。

旧 compact `{id,count,customData}` 只作为读取兼容输入；读取后会形成仅含 custom data 的 v1 Snapshot。所有新 Snapshot 写入都带 `schemaVersion: 1`。`saveSimple/loadSimple` 暂时保留并标为 deprecated，避免破坏尚未迁移的功能。

## 阶段 B：市场读取与订单创建（下一步，尚未开始）

下一步才迁移协议 `8` 创建销售订单，然后是 `9` 创建求购订单和 `10/11` 市场数据。服务器必须重新验证手中物品、数量、价格和税费，并为扣物品/冻结金额与订单落盘提供补偿路径。

- 订单持久化类型使用稳定 ID，并兼容旧存档类名；
- 金额乘法先使用 `long` 检查，禁止整数溢出；
- 市场响应限制条目数量和单条物品数据大小；
- 不信任客户端提供的 ItemStack、价格、税费或订单所有权。

## 后续阶段

- 阶段 C：订单成交、取消与配送箱 `12..18`、`31..33`；
- 阶段 D：领地协议 `19..22`、`36..43`；
- 阶段 E：安全重构后再处理 `23..30`。

阶段 C 必须保证成交、取消和领取幂等；扣款、入账、订单移除和配送采用明确事务顺序及补偿路径；配送列表返回不可变副本，成功放入背包后才删除记录。阶段 D 继续以 NeoForge 1.21.1 权限行为为基线。阶段 E 开始前必须完成路径规范化、允许目录、会话所有权、单块/总大小/块数限制、超时清理、防重放、防跨请求拼接和服务端权限开关。

## 每轮验证

```powershell
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
rg -n "net\.neoforged|net\.minecraftforge" common/src
git diff --check
```

若仅 ForgeGradle TLS 证书握手失败，当次命令可临时增加 `"-Dnet.minecraftforge.gradle.check.certs=false"`，禁止写入项目配置。每轮还要检查 Forge JAR 不包含 NeoForge target 类。
