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

## 阶段 B：市场读取与订单创建（进行中）

协议 `8` 创建销售订单和协议 `9` 创建求购订单均已完成；下一步才处理 `10/11` 市场数据。本轮未迁移购买、确认、交付、取消、配送箱或领地。

- common 消息只含 `slot`、`quantity`、`totalPrice`，discriminator 固定为 `8`；
- 服务端重读槽位，保存 count=1 的 schema-v1 模板，数量与整单总价分别保存；
- 税费使用 `(totalPrice + 9L) / 10L`，校验期间无状态修改，库存/税费/订单失败有补偿路径；
- common `MarketLedger` 保证 ID 唯一、列表不可变、容量有界，target SavedData 只处理版本 API；
- 新写入使用 `sales_order` / `demand_order`，精确保留 `expirationTime`；
- 兼容旧 Java 类名和旧订单字段；Forge 无法无损转换旧物品数据时中止并阻止覆盖。
- 协议 `8` 已完成事务加固：多堆删除不完整或抛异常会先恢复整个背包；删除结果不再用 `null` 表意；
- repository 的 `false`/异常均保证不残留新订单，重复 ID 不会删除旧订单；
- 退税与物品恢复分别捕获并始终全部尝试，任一补偿失败返回 `ROLLBACK_FAILED` 并记录事务阶段日志；
- Forge 与 NeoForge 均向玩家返回稳定翻译消息，内部错误不泄漏枚举或堆栈；
- `MarketManager` 不再缓存和回写兼容视图，当前世界的 `MarketSavedData/MarketLedger` 是唯一权威；
- 协议 `8` 的事务加固保持关闭状态；协议 `9` 在下述独立切片中完成。

协议 `9` 完成内容：

- common 消息只含 `itemId`、`quantity`、`totalPrice`，discriminator 固定为 `9`；
- `totalPrice` 是整笔求购单冻结金额，只扣除一次，不乘 quantity，也不收销售税；
- 服务端解析注册表默认物品，拒绝非法 ID、不存在物品、air 和无法严格捕获的默认形态；
- 模板 Snapshot count 固定为 1，quantity 独立保存且限制为物品 `maxStackSize`；
- UUID、求购者身份、listing/expiration 时间及初始 `delivered=false` 全部由服务端生成；
- 扣款后 ledger 添加失败会退还完整冻结金额，退款失败返回明确结果并记录日志；
- NeoForge UI 不再构造或发送 `DemandOrder`、ItemStack 或 NBT；Forge 仅注册协议 9 的服务端接收路径；
- 新订单继续写入稳定 `demand_order` schema，并可由现有交付服务查找、单次支付和持久化 delivered；
- 最终回归套件为 Forge 98 项、NeoForge 100 项。协议 `9` 正式关闭，下一步为协议 `10/11`。

- 订单持久化类型使用稳定 ID，并兼容旧存档类名；
- 金额乘法先使用 `long` 检查，禁止整数溢出；
- 市场响应限制条目数量和单条物品数据大小；
- 不信任客户端提供的 ItemStack、价格、税费或订单所有权。

### 兼容性安全修复：旧协议 14 求购交付

旧 NeoForge `Packet_DeliverDemandOrder` 已改为通过 common 事务服务和
`MarketLedger.markDemandDelivered` 写入权威账本。`MarketManager` 返回的旧订单对象
只是分离的只读兼容视图，禁止依靠 setter 回写。交付流程先校验和移除物品，再付款，
最后原子标记 delivered；账本失败时付款撤销和库存恢复会独立尝试。重复交付不会再次
付款。这不是协议 `14` 的正式迁移：旧 NeoForge payload/注册保留，Forge 不注册
discriminator `14`，下一迁移切片仍是协议 `9`。

## 后续阶段

### 协议 10/11：市场分页读取（已完成）

- 协议 10 使用 `SUMMARY/PAGE`，页面固定 9 条、查询最大 64 字符；过滤与物品 ID/创建者搜索由服务端执行。
- `MINE` 使用真实网络发送者 UUID，客户端不发送目标玩家身份。
- 协议 11 使用不可变 `MarketOrderSnapshot` 与 schema-v1 `ItemStackSnapshot`，不再传输动态 `MarketItem` NBT。
- `ClientMarketState` 以递增 requestId 拒绝过期页面；`INVALIDATED` 保留旧页并标记 stale。
- 首页只请求 summary；市场变更向在线玩家广播只含实时统计的轻量失效通知。
- 协议 `12/13/15` 已迁移并完成质量加固；协议 `14/16` 仍为 legacy，下一迁移切片是协议 `14`。
- 验证套件包含 Forge 1.20.1 212 项测试和 NeoForge 1.21.1 213 项测试。
- 初始迁移提交为 `666fccc`；后续加固加入持久化单调 revision 和 768 KiB 整包估算预算。
- SUMMARY/PAGE 使用独立 requestId；INVALIDATED 的 revision 会使旧响应失效，NeoForge 页面先完整恢复 Snapshot 再原子提交。
- `CHANGED/UNKNOWN` 广播失效，`UNCHANGED` 不广播；下一步是协议 `14`。

### 市场余额事务加固（已完成）

- 普通 `addBalance` 保留历史封顶语义；未迁移的奖励、指令、红包、领地、商店购买、销售购买与过期任务暂不扩大修改范围。
- 市场事务使用 `creditExact/debitExact/canCreditExact`；溢出完整失败，dirty 异常会恢复该账户余额与日志。
- 协议 `8` 退税、协议 `9` 冻结款退款、旧求购交付付款均采用完整到账语义。
- 旧求购取消先验证并事务删除，再退款原所有者；退款失败通过原索引恢复句柄还原订单。
- Forge 协议 `9` 客户端发送路由已补齐；未注册 discriminator `16`，也未开始协议 `10/11`。
- 完整回归为 Forge 1.20.1 共 109 项、NeoForge 1.21.1 共 110 项，`buildAllTargets --rerun-tasks` 通过。

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

### 协议 12：购买销售订单（已完成）

- 协议 10/11 已关闭；双端 decoder 在读取订单或 Snapshot NBT 前拒绝超过 768 KiB 的 raw payload。
- 市场持久化加载与运行时替换分别使用 `loadFromPersistence` 和 `replaceAll`，不再存在无 revision 修改旁路。
- 协议 12 客户端只发送 `tradeId`；价格、数量、卖家与物品全部由服务端权威订单提供。
- 买卖双方通过单次 dirty 的 `transferExact` 原子转账，dirty 失败恢复双方账户存在性、余额和日志。
- 销售订单删除提供原索引恢复句柄；物品只事务性插入主物品栏，空间不足直接拒绝且不生成掉落。
- 成功提交后才发送通知与 INVALIDATED；协议 13、15 已迁移，协议 14、16 保持 legacy。

若仅 ForgeGradle TLS 证书握手失败，当次命令可临时增加 `"-Dnet.minecraftforge.gradle.check.certs=false"`，禁止写入项目配置。每轮还要检查 Forge JAR 不包含 NeoForge target 类。
# Protocol 12/removal checkpoint

- Protocol 12 now uses shared materialization, transactional inventory insertion and rollback ports, with
  fail-closed null handling for repository/insertion results and caught preview/capacity exceptions.
- Sales-order removal is migrated to a UUID-only common message and a common service. The original seller
  is the only receiver; offline-owner operator removal and insufficient inventory capacity are rejected.
- Purchase and removal share `removeSalesTransactional`; the legacy removal packet has been deleted.
- The append-only manifest is unchanged: removal is discriminator 15 because discriminator 13 is already
  demand-order confirmation. Protocol 13 is migrated; 14 and 16 remain legacy.

## Protocol 13 completion

- Canonical 12/13/14/15/16 discriminators remain fixed.
- Protocol 12, 13 and 15 post-transaction side effects use `MarketActionPostPlan` and
  `IsolatedPostActions`; all outcomes expose only explicit `MarketMutationState` factories.
- Protocol 13 is a UUID-only common message backed by transactional delivered-demand removal.
- Protocols 12/13/15 share one transactional main-inventory adapter per target.
- Confirmation always delivers to the original online requester; operator actions never redirect items.
- Offline owners and insufficient inventory leave the delivered order unchanged; no item drops are created.
- Protocols 14 and 16 are migrated; protocols 17/18 have not started.
- Protocol 14 hardening is closed: authoritative requester reporting, expected-order transition,
  exact supplier credit, independent payment/inventory compensation, and shared adapter contract
  coverage are complete. Protocol 16 now uses a UUID-only common request, expected-order removal,
  exact requester refund and explicit CHANGED/UNKNOWN invalidation.
- Protocol 16 hardening is closed: result/outcome invariants reject illegal states, repository contract
  violations attempt restoration, ORDER_CHANGED carries no stale expected order, and all transaction
  exceptions reach structured target logs. Protocol 14 no longer reports restoration before compensation.
