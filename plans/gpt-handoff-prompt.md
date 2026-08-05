# 给 GPT/Codex 的 EconomySystem Bridge 接手提示词

## 当前领地迁移状态

协议 17–22 已迁移且协议 22 完成最终集成闭环。其 C2S wire 固定为 territory UUID、target player UUID，共 32 字节；sender 来自 authenticated context。实时 owner 才能移除实时成员，owner 不可移除，target 可以离线。NeoForge 保存原始 canonical 名称，使用可注入精确 mutation/rollback，manager 集成复验全部身份与空间索引；Forge raw NBT copy-on-write 在 dirty 后再次证明 candidate raw/cache，静默漂移进入补偿，完整 rollback 证明引用、深内容、strict parse、名称与顺序。成功后只 fail-closed 清理精确 invite。双端 confirmation 为 one-shot，Forge tiny-height/窄宽度隐藏放不下的控件。严格注册、limiter 和四资源 parity 测试已通过。最终实测 shared-source 285、Forge 362、NeoForge 422，双目标构建通过。协议 23 及以后仍为 legacy，不得迁移。

协议 21 删除已完成最终空间边界加固，wire 仍严格为一个 16 字节 Territory UUID。后续必须保留闭区间 Bounds（`width/height = max - min`）、全树 identity-based QuadTree 删除，以及覆盖 identity 数、UUID 数、预期节点路径和代表点查询的事务验证。不得把 package-private 的 manager resize 方法重新公开，也不得绕过 `TerritoryResizeTransactionService`。最终实测 shared-source 266、Forge 333、NeoForge 382，完整双目标构建通过。协议 22 `REMOVE_PLAYER` 及以后仍为 legacy，只能在独立任务中迁移。

```text
你正在继续开发 QingMo-A/EconomySystem 的 bridge 分支。开始前完整阅读：

1. AGENTS_EconomySystem.md
2. plans/bridge-migration-plan.md
3. common/README.md
4. targets/forge-1.20.1/README.md
5. common/src/main/java/com/mo/economy_system/platform/item/ 下的 Snapshot 与 Bridge 文件

先检查 git status 和当前分支。工作区可能有用户自己的未提交文件；不得删除、覆盖、清理或默认全部暂存。

架构原则：
- NeoForge 1.21.1 是唯一业务基线；Forge 1.20.1 只做等价适配。
- common 不得导入 NeoForge/Forge loader API。
- EconomyProtocol 的 44 条消息、方向和 Forge discriminator 0..43 不得重排或复用。
- 跨版本物品转换必须 fail closed，不能静默丢组件。
- check/get/chunk 23..30 在安全重构前禁止迁移。

阶段 A 已完成并经过安全加固：ItemStackSnapshot schema v1、集中 validator/limits、严格结果式 codec、DATA_LIMIT_EXCEEDED、旧 compact 读取兼容、Damage 对称校验、NeoForge Data Components 适配、Forge 原生 NBT 适配以及双端共享黄金 fixture 已建立。限制值以 common/README.md 和迁移计划为准。旧 saveSimple/loadSimple 只是 deprecated 兼容入口；新代码必须使用 Snapshot schema v1。

阶段 B 的协议 8（创建销售订单）与协议 9（创建求购订单）已经完成。协议 9 的消息只有 itemId、quantity、totalPrice；totalPrice 是一次冻结的整单金额，不乘 quantity、无销售税。服务端解析默认注册物品、强制 count-one Snapshot、限制 maxStackSize，并生成 UUID、所有者、时间和 delivered=false。ledger 添加失败会退款。下一项任务是协议 10/11 市场数据读取；不要同时迁移购买、确认、交付、取消、配送箱或领地。

兼容性修复说明：旧 NeoForge 协议 14 的求购交付已使用 common 事务服务和
MarketLedger 原子 delivered 转换。MarketManager 返回的订单对象是分离的只读兼容
视图，不得通过 setDelivered 等 setter 假设持久化。该改动不是协议 14 迁移，Forge
不得注册 discriminator 14。协议 9 已关闭，下一项任务是协议 10/11。

市场余额事务已经加固。协议 16 已迁移为 common UUID 请求，使用 expected-order 删除、原求购者精确退款和显式市场状态；不要恢复旧 Packet。

协议 10/11 已迁移为有界分页读取：SUMMARY 不携带订单，PAGE 固定 9 条并由服务端执行 ALL/MINE/SALES/DEMAND 过滤以及物品 ID/创建者搜索；MINE 身份只取真实发送者。响应只传 schema-v1 Snapshot。ClientMarketState 会忽略旧 requestId，市场变化只广播 INVALIDATED。旧 Packet_MarketDataRequest/Response 已删除。下一步是协议 12，不要同时迁移 13-16。

协议 10/11 初始提交为 `666fccc`，随后已加固为固定 PAGE_SIZE=9、持久化单调 marketRevision 和 768 KiB 整包估算预算。SUMMARY/PAGE 分别维护请求 ID；INVALIDATED revision 阻止旧统计或页面覆盖。NeoForge 必须先完整恢复一页全部 Snapshot，再提交 ClientMarketState。广播仅允许在权威账本成功修改后发生。下一步仍是协议 12。

完成后运行：
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
rg -n "net\.neoforged|net\.minecraftforge" common/src
git diff --check

协议 10/11 已正式关闭，协议 12–21 已迁移。协议 21 wire 仅为 16 字节 territoryId，删除权限来自 authenticated sender，不退款；NeoForge 使用四索引事务，Forge 使用 raw NBT copy-on-write，成功后清理 pending invite，NeoForge 还会清理 resize session。协议 22 及之后尚未开始。
并检查 Forge JAR 不包含 NeoForge target 类。只在 ForgeGradle TLS 握手失败时，对当次命令临时使用 -Dnet.minecraftforge.gradle.check.certs=false，禁止写入配置。
```
# Current bridge handoff

Sales purchase and UUID-only sales-order removal are now migrated. Both use the common transactional
inventory ports and `MarketLedger.removeSalesTransactional`. Do not restore business behavior from the
Forge 1.20.1 legacy packet. Items removed by an operator must still go only to the original online seller.
Protocols 12–21 are migrated. Protocol 21 is an exact 16-byte territory UUID delete request; authenticated sender authority is revalidated server-side and deletion never refunds. Do not renumber the append-only manifest; protocol 22 and later territory actions have not started.

Protocol 13 is now migrated. Protocols 12, 13 and 15 use shared per-target transactional main-inventory
adapters, `MarketMutationState`, `MarketActionPostPlan`, and `IsolatedPostActions`. Protocol 14 hardening is complete, including real
failure reporting, expected-order transition, exact supplier credit, independent compensation, and
shared inventory transaction contract coverage. Protocol 16 cancellation is also migrated with server-owned
refund semantics and invalidation on CHANGED/UNKNOWN. Territory response 18 is NBT-free,
uses full owned/minimal authorized snapshots, enforces 1 MiB budgets, and commits only the
active request ID after complete restore. Protocols 19/20 are complete; do not start protocol 21 without a new task.
Protocol 16 hardening is closed: illegal result/outcome combinations are rejected, mismatched removals are
restored before failure, ORDER_CHANGED has no stale transaction order, and target logs combine all errors.

Territory protocols 17/18 are now hardened. Response 18 has stable `data` and `error` kind IDs, uses the one
common NBT-free wire codec, and encodes through a temporary buffer before touching the destination. The active
`Screen_Territory` request ID is the sole stale-response authority; the old global tracker no longer exists.
Both targets use `TerritoryDataClientApplier` for complete restore followed by one atomic commit. ERROR,
restore failure, and a 200-tick timeout end loading and allow a new-ID retry. Forge has a target-local
territory page because the NeoForge root UI is excluded from its build, and its client dispatch is hidden behind
a physical-side-safe indirection. Forge `territory_data` remains a sole overworld read-only protocol-17/18
adapter: invalid buffs/dimensions fail closed and unknown permissions fall back to MEMBERS. NeoForge treats
historical null buff upgrade costs as empty. Protocol 19 final hardening is complete: the common reserve factory owns the remaining-stack write and dirty mark, compensates before returning, and reports compensation failure as ROLLBACK_FAILED. Dirty marking is required; client sync is best effort. Commit is an I/O-free state transition, arrival UNKNOWN never auto-refunds and maps to TELEPORT_STATE_UNKNOWN, and cooldowns are weakly server-scoped with tick-epoch reset. Forge/NeoForge production inventory helpers scan only `inventory.items` and provide their native full-stack equality. Protocol 20 finalization restores Forge Chinese resources, verifies canonical invite keys/placeholders with strict JSON parsing, uses local player-list revision for loading/empty, and applies a 15-tick Forge click debounce. Malformed/duplicate Forge raw territory data maps to CREATE_FAILED; NeoForge real membership mutation has dirty/add/rollback fault injection. Protocol 21 removal and resize integration are hardened: the removal request remains a 16-byte UUID; synchronized prepare prices from live bounds, commit revalidates captured state, equal-area reshape is free, explicit repository failure kinds replace message parsing, uncertain mutation never auto-refunds, closed integer bounds cover degenerate rectangles, and QuadTree removal traverses by identity. Do not migrate protocol 22 or later operations without a new task.
historical null buff upgrade costs as empty. Protocol 19 final hardening is complete: the common reserve factory owns the remaining-stack write and dirty mark, compensates before returning, and reports compensation failure as ROLLBACK_FAILED. Dirty marking is required; client sync is best effort. Commit is an I/O-free state transition, arrival UNKNOWN never auto-refunds and maps to TELEPORT_STATE_UNKNOWN, and cooldowns are weakly server-scoped with tick-epoch reset. Forge/NeoForge production inventory helpers scan only `inventory.items` and provide their native full-stack equality. Protocol 20 finalization restores Forge Chinese resources, verifies canonical invite keys/placeholders with strict JSON parsing, uses local player-list revision for loading/empty, and applies a 15-tick Forge click debounce. Malformed/duplicate Forge raw territory data maps to CREATE_FAILED; NeoForge real membership mutation has dirty/add/rollback fault injection. Protocol 21 removal and its resize integration are hardened: removal remains a 16-byte UUID request; authoritative prepare computes price from live bounds, commit revalidates captured state, equal-area reshape is free, and uncertain mutation never auto-refunds. QuadTree validation proves storage path and representative queries. Repository failure kinds are explicit and both handlers use one overworld game-time value. Do not migrate protocol 22 or later operations without a new task.

Protocols 23-25 are now migrated atomically. Treat the common `ClientFileCheck*` messages, schema-1 strict JSON,
pending store, scanner, executor and comparison model as authoritative. Forge and NeoForge both expose `/check`
and explicit consent/result screens; never restore the removed legacy packets, automatic scanning, game-root
fallback, `Files.readAllBytes`, or remote JSON file writes. The server trusts only the authenticated C2S sender
and its exact pending key. Protocol 26 GET and every later file-transfer packet remain legacy; do not migrate them
without a separate task.

Protocols 23-25 are lifecycle-hardened. Preserve connection-generation cancellation, no stale protocol 24 send,
the 4096-entry enumeration cap, deadline coverage through sort/hash, single-channel open-time `NOFOLLOW_LINKS`,
exact consent-busy semantics, expiring processing claims, shared one-shot result routing, and status-specific UI.
Consent ownership now spans CONSENT, SCANNING and SENDING. Preserve the shared session-aware protocol-24
dispatcher, SecureDirectoryStream relative opens or fallback root identity revalidation, task failure callbacks,
status-specific local result UI, and complete skipped-row rendering. Protocol 26+ remains legacy.
The protocol 23-25 safety closure is complete: local SUCCESS compares fully, FAILED has no comparison, and TRUNCATED is READY_INCOMPLETE with preserved skipped/error data. Pre-created task tokens are passed directly to callbacks; scheduling/callback failures terminate and run common-state abandonment cleanup. Scanner roots are authenticated from the opened SecureDirectoryStream handle, and unsupported providers fail closed with DIRECTORY_PROVIDER_UNSAFE. Protocol 26+ is still legacy.

Final verified counts: 352 shared-source tests, 428 Forge tests, 488 NeoForge tests.
