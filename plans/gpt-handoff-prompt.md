# 给 GPT/Codex 的 EconomySystem Bridge 接手提示词

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

阶段 B 的第一个切片协议 8（创建销售订单）已经完成并经过事务加固：部分删除自动恢复，退税与物品补偿独立尝试，repository 添加为原子契约，双端有安全反馈与事务日志，MarketManager 不再缓存回写旧视图。下一项任务才是协议 9（创建求购订单）；开始前审查 common 的 MarketOrder、MarketLedger、MarketOrderCodec 和双端 SavedData 外壳，不要同时迁移 10/11、购买、取消、配送箱或领地。

兼容性修复说明：旧 NeoForge 协议 14 的求购交付已使用 common 事务服务和
MarketLedger 原子 delivered 转换。MarketManager 返回的订单对象是分离的只读兼容
视图，不得通过 setDelivered 等 setter 假设持久化。该改动不是协议 14 迁移，Forge
不得注册 discriminator 14。下一项任务仍是协议 9。

完成后运行：
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
rg -n "net\.neoforged|net\.minecraftforge" common/src
git diff --check
并检查 Forge JAR 不包含 NeoForge target 类。只在 ForgeGradle TLS 握手失败时，对当次命令临时使用 -Dnet.minecraftforge.gradle.check.certs=false，禁止写入配置。
```
