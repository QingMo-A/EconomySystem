# EconomySystem Public API v1

EconomySystem 从 Public API `1.0.0` 开始为第三方模组提供稳定的服务端集成入口。

> 公共包边界：第三方代码只应依赖 `com.mo.economy_system.api.*`。  
> `common.*`、`core.*`、`target.*`、SavedData、Ledger 和网络消息均属于内部实现，不提供兼容承诺。

## 1. 支持目标

| Minecraft | Loader | Java | API |
|---|---|---:|---:|
| 1.21.1 | NeoForge | 21 | 1.0.0 |
| 1.20.1 | Forge | 17 | 1.0.0 |

两个目标 JAR 都暴露同一个入口：

```java
import com.mo.economy_system.api.EconomySystemApi;
```

所以大部分第三方业务代码不需要区分 Forge / NeoForge。

## 2. 构建 API JAR

在 EconomySystem 仓库执行：

```bat
gradlew.bat buildApiAllTargets
```

也可以只构建一个目标：

```bat
gradlew.bat :targets:neoforge-1.21.1:apiJar
gradlew.bat :targets:forge-1.20.1:apiJar
```

每个目标还提供 `apiSourcesJar`，方便 IDE 查看源码和 JavaDoc。

API-only JAR 仅用于第三方模组的**编译依赖**；玩家/服务器运行时仍应安装对应版本的完整 EconomySystem 模组 JAR。

## 3. 获取 API Session

所有写操作均为服务端权威操作，并要求在 Minecraft 服务端线程调用。

### 从 ServerLevel 获取

```java
EconomyApiSession api = EconomySystemApi.forLevel(serverLevel);
```

### 从 ServerPlayer 获取

```java
EconomyApiSession api = EconomySystemApi.forPlayer(serverPlayer);
```

### 从 MinecraftServer 获取

```java
EconomyApiSession api = EconomySystemApi.forServer(server);
```

不要把 session 保存成跨服务器生命周期的静态单例。推荐在事件/命令/任务处理时按需获取。

可以检查 API 主版本：

```java
if (!EconomySystemApi.isCompatibleMajor(1)) {
    // 当前集成不兼容
}
```

当前货币名称：

```java
EconomySystemApi.CURRENCY_NAME // "梦鱼币"
```

EconomySystem v1 不提供多币种接口。

## 4. 账户 API

```java
import com.mo.economy_system.api.account.EconomyAccountApi;

EconomyAccountApi accounts = api.accounts();
UUID playerId = player.getUUID();

int balance = accounts.balance(playerId);
boolean enough = accounts.hasAtLeast(playerId, 100);
```

### 增加梦鱼币

每次写操作都必须带 namespaced source 和原因，便于流水审计：

```java
var result = accounts.credit(
    playerId,
    100,
    EconomyAccountApi.TransactionNote.of(
        "examplemod:quest_reward",
        "完成任务：矿工入门"
    )
);

if (result == EconomyAccountApi.MutationStatus.SUCCESS) {
    // 成功
}
```

### 扣除梦鱼币

```java
var result = accounts.debit(
    playerId,
    50,
    EconomyAccountApi.TransactionNote.of(
        "examplemod:shop",
        "购买特殊服务"
    )
);
```

扣款不会产生负余额。余额不足时返回：

```java
EconomyAccountApi.MutationStatus.INSUFFICIENT_FUNDS
```

### 原子转账

```java
var result = accounts.transfer(
    senderId,
    recipientId,
    250,
    EconomyAccountApi.TransactionNote.of(
        "examplemod:trade",
        "交易付款"
    ),
    EconomyAccountApi.TransactionNote.of(
        "examplemod:trade",
        "交易收款"
    )
);
```

转账是 all-or-nothing：不会只扣发送者却不给接收者。

### 查询流水

```java
EconomyAccountApi.LogPage page = accounts.history(
    playerId,
    "examplemod:quest_reward", // 空字符串表示全部来源
    0,
    20
);

for (EconomyAccountApi.LogEntry entry : page.entries()) {
    int delta = entry.delta();
    String reason = entry.reason();
}
```

API v1 故意不开放无审计的 `setBalance`。

## 5. 邮箱 API

```java
import com.mo.economy_system.api.mailbox.EconomyMailboxApi;

EconomyMailboxApi mailbox = api.mailbox();
```

### 发送系统通知

```java
var result = mailbox.sendNotice(
    playerId,
    EconomyMailboxApi.MailDraft.of(
        "examplemod:quest",
        "任务完成",
        "你已经完成了矿工入门任务。"
    )
);
```

收件人可以离线。在线时会使用 EconomySystem 的新邮件 Toast / 提示音。

如果需要随通知发放梦鱼币，在 `MailDraft` 的第四个参数填写金额：

```java
var result = mailbox.sendNotice(
    playerId,
    EconomyMailboxApi.MailDraft.of(
        "examplemod:quest_bonus",
        "任务奖金",
        "奖励已发放。",
        250
    )
);
```

金额在邮件写入成功的同一服务端操作中立即计入收件人账户，并作为邮件记录中的发放凭据展示；它不会在打开邮件时重复入账。余额达到上限时调用返回 `BALANCE_LIMIT`。

### 发送带附件的系统补偿/奖励

```java
var result = mailbox.sendCompensation(
    playerId,
    EconomyMailboxApi.MailDraft.of(
        "examplemod:event_reward",
        "活动奖励",
        "感谢参与本次活动。"
    ),
    List.of(
        EconomyMailboxApi.MailItemGrant.of("minecraft:diamond", 16),
        EconomyMailboxApi.MailItemGrant.of("minecraft:gold_ingot", 64)
    )
);
```

API 会按物品原生最大堆叠数拆分附件。拆分后最多 27 个附件；超出时整个调用返回 `TOO_MANY_ATTACHMENTS`，不会少发一部分物品。

补偿邮件也可以在 `MailDraft` 中携带金额；金额与物品附件一起写入同一封邮件，发放成功后立即计入收件人账户。

附件写入仍使用 EconomySystem 原有的 DeliveryBox + Mailbox 事务语义，避免复制或静默丢失。

### 发布全局公告

```java
long expiresAt = System.currentTimeMillis() + 24L * 60L * 60L * 1000L;

mailbox.publishAnnouncement(
    EconomyMailboxApi.MailDraft.of(
        "examplemod:announcement",
        "限时活动",
        "活动将在今晚开启。"
    ),
    expiresAt
);
```

`expiresAtEpochMillis = 0` 表示不设置显式过期时间。

全局公告是面向所有玩家的文本消息，不能携带金额或物品。公告会保存在世界数据中；只要尚未过期或被玩家关闭，新进服玩家之后打开邮箱仍可看到该公告。定向的系统/补偿邮件只会显示给指定 UUID 的收件人，新玩家不会收到发给其他玩家的历史邮件。

## 6. 市场 API

v1 市场接口目前**只读**。这是刻意限制：第三方直接插入/删除 MarketLedger 条目会绕过物品扣除、余额事务、退款和补偿流程。

```java
EconomyMarketApi market = api.market();

List<EconomyMarketApi.OrderView> all = market.orders();

Optional<EconomyMarketApi.OrderView> order = market.order(tradeId);

List<EconomyMarketApi.OrderView> playerOrders = market.ordersByOwner(playerId);

List<EconomyMarketApi.OrderView> sales =
    market.ordersByType(EconomyMarketApi.OrderType.SALES);
```

`OrderView` 暴露稳定字段：

- tradeId
- SALES / DEMAND
- itemId
- quantity
- totalPrice
- ownerId / ownerName
- listingTime / expirationTime
- delivered
- `unitPrice()`
- `expired(now)`

API 不暴露内部 `MarketOrder` 或 `ItemStackSnapshot`。

## 7. 领地 API

v1 领地接口同样先保持只读。

```java
EconomyTerritoryApi territories = api.territories();

List<EconomyTerritoryApi.TerritoryView> all = territories.territories();

Optional<EconomyTerritoryApi.TerritoryView> current =
    territories.territoryAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
```

查询玩家与领地关系：

```java
var relation = territories.relationship(territoryId, playerId);

if (relation == EconomyTerritoryApi.Relationship.OWNER) {
    // 领主
} else if (relation == EconomyTerritoryApi.Relationship.MEMBER) {
    // 授权成员
}
```

`territoryAt` 会按当前 session 绑定的维度查询，不会把不同维度的同坐标领地混在一起。

## 8. 错误处理

预期业务失败使用 enum 返回值，而不是通过异常表达。例如：

- 余额不足
- 达到余额上限
- 邮箱已满
- 附件区已满
- 未知物品 ID
- 附件过多

编程错误或违反调用契约仍可能抛出异常，例如：

- null 参数
- 非法 namespaced source
- 在非服务端线程调用 API
- Overworld 尚未加载

## 9. 线程规则

Public API v1 不会自动把写操作调度到服务器线程。

第三方模组应从服务器事件、命令、服务器 tick、服务端网络 handler 等已经处于 server thread 的上下文调用。

错误示例：

```java
CompletableFuture.runAsync(() -> {
    EconomySystemApi.forLevel(level).accounts().credit(...); // 不允许
});
```

如确实从异步任务返回，请先用 MinecraftServer 调度回服务器线程。

## 10. 兼容承诺

`1.x` 范围内：

- `com.mo.economy_system.api.*` 已发布方法不会随意删除或改变语义；
- 可以增加新方法、结果状态和 capability；
- 内部 common/core/target 实现仍可自由重构；
- 不承诺第三方直接调用内部类的兼容性。

破坏公共 API 的修改应提升 `API_MAJOR`。

## 11. 计划中的 v1.x 扩展

后续可在保持 1.0 调用兼容的基础上增加：

- 余额变化 / 转账事件
- 邮件创建事件
- 市场订单生命周期事件
- 领地变化事件
- 安全的市场写 API
- 受控领地管理 API
- 更完整的物品附件桥接
- Maven 仓库发布与示例集成模组
