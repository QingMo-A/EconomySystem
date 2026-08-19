# EconomySystem Public API v1 设计与实施计划

## 目标

为其他 Minecraft 模组提供一套稳定、可版本化、服务端权威的 EconomySystem 公共 API。第三方模组只允许依赖 `com.mo.economy_system.api.*`，不得依赖 `common.*`、`core.*`、`target.*`、SavedData、网络包或具体 Loader 实现。

API v1 的首要目标不是“把所有内部类 public 化”，而是建立长期兼容边界：内部实现可继续重构，第三方调用代码尽量不受影响。

## 兼容原则

1. API 使用语义版本：`1.0.0` 起步，主版本只在破坏兼容时提升。
2. 单一货币：梦鱼币，不设计多币种接口。
3. 所有写操作服务端权威，要求在服务端线程调用。
4. API 不暴露 SavedData、Ledger、Packet、Loader 私有对象。
5. 所有结果使用 API 自己的 enum/record，不把内部 Result 类型泄漏给第三方。
6. 市场和领地的高风险写操作首版不直接开放，先提供稳定只读查询；后续写入必须通过现有事务 Service 适配。
7. Forge 1.20.1 与 NeoForge 1.21.1 公开入口保持同名、同语义；目标 JAR 内分别提供实现。

## 公共包结构

```text
com.mo.economy_system.api
├── EconomyApiSession
├── EconomyApiCapabilities
├── account/
│   ├── EconomyAccountApi
│   ├── EconomyTransactionNote
│   ├── AccountMutationStatus
│   ├── AccountTransferStatus
│   ├── AccountLogEntry
│   └── AccountLogPage
├── mailbox/
│   ├── EconomyMailboxApi
│   ├── MailDraft
│   ├── MailDeliveryStatus
│   └── MailItemGrant
├── market/
│   ├── EconomyMarketApi
│   ├── MarketOrderTypeView
│   └── MarketOrderView
└── territory/
    ├── EconomyTerritoryApi
    ├── TerritoryPosition
    └── TerritoryView
```

每个目标 JAR 额外提供同一个 FQCN：

```text
com.mo.economy_system.api.EconomySystemApi
```

公开调用形式：

```java
EconomyApiSession api = EconomySystemApi.forLevel(serverLevel);
int balance = api.accounts().balance(playerId);
```

`ServerLevel` 在 Forge/NeoForge 都属于 vanilla 类型，因此第三方跨 Loader 源码可以保持相同调用形式；具体适配代码由各 target 实现。

## v1 功能范围

### 账户 API

开放：
- 查询余额
- 精确增加余额
- 精确扣除余额
- 原子转账
- 余额流水分页查询
- 最大余额常量

所有变更要求提供 namespaced `source` 与人类可读 `reason`，并映射到现有 EconomyLedger 日志。

### 邮箱 API

开放：
- 发送系统通知
- 发送系统补偿
- 补偿支持一封邮件多个简单物品附件（item id + count）
- 在线玩家收到现有 Toast + 提示音
- 离线玩家照常持久化

首版 `MailItemGrant` 只表达注册物品 ID 与数量，保证 loader-neutral。保留后续 native ItemStack/NBT 扩展点，不在 v1 初版公共接口里泄漏内部 ItemStackSnapshot。

### 市场 API

首版只读：
- 查询全部订单快照
- 按 tradeId 查询
- 按玩家查询
- 按销售/求购类型过滤

不直接开放 `MarketLedger.add/remove`，避免第三方绕过库存扣除、退款、DeliveryBox 和补偿事务。

### 领地 API

首版只读：
- 查询当前 level 的所有领地
- 按 territoryId 查询
- 按 owner 查询
- 按坐标查询所在领地
- 查询 owner / member 关系

不直接开放 Territory 对象本身。

## 目标适配

### NeoForge 1.21.1

`EconomySystemApi.forLevel(ServerLevel)` 返回绑定到目标 ServerLevel 的 session：
- accounts -> `EconomySavedData.getInstance(level)`
- mailbox -> `MailboxSavedData` + `DeliveryBoxSavedData` + MailboxAdminService
- market -> `MarketSavedData.getInstance(level)`
- territory -> `TerritoryManager/TerritorySavedData`

### Forge 1.20.1

保持完全相同公开接口，使用 Forge 对应 SavedData / runtime 适配。

## API artifact

第一阶段先把 API 类打进正式 mod JAR，第三方以 `compileOnly`/`implementation` 依赖对应 EconomySystem JAR。

第二阶段增加独立 `-api.jar` / Maven publication，只包含：
- `com.mo.economy_system.api/**`
- 必需的 API metadata

运行时仍要求安装完整 EconomySystem 模组。

## 后续 v1.x

- API 事件：余额变更、转账、邮件创建、市场订单变化、领地变化
- native ItemStack 附件桥接（不破坏 loader-neutral 基础接口）
- 市场安全写 API（必须委托现有事务 Service）
- 领地权限检查与受控管理 API
- API-only artifact / Maven 坐标
- 示例集成模组与 JavaDoc
