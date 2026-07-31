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

最终验收：Forge 1.20.1 共 35 项测试、NeoForge 1.21.1 共 36 项测试，均无失败；`buildAllTargets --rerun-tasks` 通过。

明确不支持并 fail closed：

- NeoForge v1 allow-list 之外的任何显式非默认 Data Component，以及显式删除 component 的 patch；
- 未注册的直接附魔 Holder、目标注册表不存在的物品或附魔；
- Forge 属性修饰符、CanDestroy/CanPlaceOn、BlockEntityTag、EntityTag、ForgeCaps、未知 display 字段和未知 HideFlags；
- Forge 1.20.1 无法分别表达普通附魔与储存附魔 tooltip 可见性的组合；
- 任何越界数量、损伤、颜色或附魔等级。

旧 compact `{id,count,customData}` 只作为读取兼容输入；读取后会形成仅含 custom data 的 v1 Snapshot。所有新 Snapshot 写入都带 `schemaVersion: 1`。`saveSimple/loadSimple` 暂时保留并标为 deprecated，避免破坏尚未迁移的功能。

## 阶段 B：市场读取与订单创建（下一步，尚未开始）

下一步才迁移协议 `8` 创建销售订单，然后是 `9` 创建求购订单和 `10/11` 市场数据。服务器必须重新验证手中物品、数量、价格和税费，并为扣物品/冻结金额与订单落盘提供补偿路径。

## 后续阶段

- 阶段 C：订单成交、取消与配送箱 `12..18`、`31..33`；
- 阶段 D：领地协议 `19..22`、`36..43`；
- 阶段 E：安全重构后再处理 `23..30`。

## 每轮验证

```powershell
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
rg -n "net\.neoforged|net\.minecraftforge" common/src
git diff --check
```

若仅 ForgeGradle TLS 证书握手失败，当次命令可临时增加 `"-Dnet.minecraftforge.gradle.check.certs=false"`，禁止写入项目配置。每轮还要检查 Forge JAR 不包含 NeoForge target 类。
