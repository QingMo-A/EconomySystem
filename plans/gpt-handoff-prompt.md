# 给 GPT/Codex 的 EconomySystem Bridge 接手提示词

```text
你正在维护 QingMo-A/EconomySystem 仓库的 bridge 分支。Bridge 已完成，不再存在等待迁移的 legacy 协议。

开始前完整阅读：

1. AGENTS_EconomySystem.md
2. plans/bridge-migration-plan.md
3. common/README.md
4. targets/forge-1.20.1/README.md
5. common/src/main/java/com/mo/economy_system/protocol/EconomyProtocol.java
6. common/src/main/java/com/mo/economy_system/common/network/EconomyMessages.java

先执行 git status 和 git branch --show-current。工作区可能存在用户自己的未提交文件；不得删除、覆盖、清理、默认暂存或提交这些文件。禁止 git add .、git add -A、git clean、git reset --hard 和 force push。

当前架构契约：

- NeoForge 1.21.1 是唯一业务基线；Forge 1.20.1 只能实现等价 adapter，不能从旧分支恢复过时业务。
- common 不得导入 net.neoforged 或 net.minecraftforge loader API。
- EconomyProtocol 共 44 条消息，canonical ID、方向、声明顺序及 Forge discriminator 0..43 不得重排、复用或改变。
- 两个 target 均已注册 0..43；新增协议只能追加。
- ItemStackSnapshot schema version 为 1。跨版本物品无法无损表达时必须 fail closed，禁止静默丢组件。
- 协议 23..30 的文件检查/传输安全事务已经完成，必须保留授权绑定、显式同意、SecureDirectoryStream/handle identity、防重放、大小预算、超时与 no-overwrite 保存语义。

最终 Bridge 切片：

- 协议 31/32/33 为配送箱查询、响应、领取。消息和 DeliveryBoxWireCodec 位于 common。
- 配送条目新格式固定为 schemaVersion、entryId、item、source。旧 dataID、itemID、itemStack、source 只允许兼容读取，所有新写入使用 v1。
- 领取必须按 entry UUID one-shot reserve，先事务性插入主物品栏，再持久删除；已证明失败要补偿，无法证明状态返回 STATE_UNKNOWN，成功后的重放返回 NOT_FOUND。
- 协议 36..43 为修改范围、Buff 解锁/升级、单领地查询/响应、成员权限、所有权转让与规则修改。
- 协议 39/40 只传 bounded Owned snapshot；协议 41/42 wire 只传 UUID（41 另有 allowed），玩家名必须服务端解析；协议 43 使用稳定 rule/action ID。
- NeoForge 使用 canonical TerritoryManager；Forge 使用 strict raw-NBT copy-on-write 并保留未知字段、列表顺序和无关数据。
- Forge resize 具有两点选择、第三次点击取消、1200 tick 超时、logout/server-stop/删除清理和 /confirm_modify。prepare/commit 必须实时复验 owner、dimension、旧状态与 overlap；扩张按面积差 * 20 扣款；STATE_UNKNOWN 禁止盲目退款。
- Forge 提供 O 配送箱、I 领地页、管理页和 claim wand；NeoForge UI 已使用 common 消息并有权限管理入口。
- 11 个旧 Packet 已删除，禁止重新引入。

维护时必须运行与改动范围匹配的测试。发布或修改协议/持久化后至少运行：

.\gradlew.bat :targets:forge-1.20.1:test --no-daemon --rerun-tasks
.\gradlew.bat :targets:neoforge-1.21.1:test --no-daemon --rerun-tasks
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
rg -n "net\.neoforged|net\.minecraftforge" common/src
git diff --check

仅当 ForgeGradle TLS 证书握手失败时，允许在当次命令临时增加 -Dnet.minecraftforge.gradle.check.certs=false；禁止写入项目配置。构建后审计两个 JAR：Forge 不得含 NeoForge target/API，NeoForge 不得含 Forge target/API，两端不得含已删除 Packet。
```

Current UI pilot checkpoint: territory management entry is now implemented via
`common/ui/territory` and the common `EconomyUiRenderer` contract. Read
`plans/multiversion-ui-inventory.md` and the `TerritoryManage*` sources before
editing. Both target shells use the same state/controller/layout/theme/view;
only target lifecycle, widgets, GuiGraphics, player-head, clipboard and network
adapters differ. Nested buffs/access/rules/transfer/invite/delete screens are
explicit fallback and are the next territory-family work. Do not migrate shop,
market, delivery, file-check UI, or detach NeoForge root sources in this slice.
