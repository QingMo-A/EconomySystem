# 给 GPT 的 EconomySystem Bridge 接手提示词

将下面整段提示词交给新的 GPT/Codex 会话：

```text
你正在接手 QingMo-A/EconomySystem 的多版本 Bridge 开发。请先完整阅读：

1. AGENTS_EconomySystem.md
2. plans/bridge-migration-plan.md
3. common/README.md
4. targets/forge-1.20.1/README.md
5. common/src/main/java/com/mo/economy_system/protocol/EconomyProtocol.java

当前分支是 bridge。开发原则如下：

- NeoForge 1.21.1 当前代码是唯一业务基线。
- Forge 1.20.1 旧分支仅可参考 API，禁止复制旧业务逻辑覆盖 1.21.1 行为。
- 架构参考 QingMo-s-Grid-Inventory 的 BridgeDevIn1.20.1 分支。
- common 放共享语义和数据模型；loader API、注册、codec、SavedData 差异和 Data Components/NBT 转换放在 targets 下。
- EconomyProtocol 的 44 条消息 ID、方向、Forge discriminator 0..43 已锁定，不得重排或复用。
- 所有经济、物品、订单、领地权限操作必须服务端权威校验。
- 跨版本物品无法无损转换时必须拒绝，不能静默丢失组件。
- 不要迁移 check/get/chunk（23..30），除非先完成计划中列出的安全重构。
- 工作区可能存在用户自己的未提交文件；不得删除、覆盖或默认全部暂存。

目前已完成：余额 0/1、余额日志 2/3、转账 4、商店目录 5/6、商店购买 7、玩家列表 34/35。两端各 26 项测试通过。

你的下一项任务：按照 plans/bridge-migration-plan.md 的阶段 A，先设计并实现版本化的 loader-neutral ItemStackSnapshot。不要立即批量迁移市场包。完成物品 schema、NeoForge/Forge 双向适配、旧 compact schema 读取兼容和黄金样例测试后，再迁移协议 8（创建销售订单）。

工作方式：

1. 先检查 git status 和当前代码，不要覆盖用户改动。
2. 给出简短的影响范围说明后直接实施，不要只输出建议。
3. 每次只迁移一个可验证的协议切片。
4. 遇到现有 bug 可顺手修复，但必须补测试或说明验证证据。
5. 每轮运行两个 target 的强制构建和测试，并扫描 common 的 loader import。
6. 更新迁移状态文档，明确仍未完成的功能和风险。

Windows 验证命令：
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks

如果只遇到 ForgeGradle TLS 握手问题，可在当次命令临时加：
"-Dnet.minecraftforge.gradle.check.certs=false"
禁止把该设置写入项目配置。

开始时先汇报你对当前架构、已迁移范围、下一步风险的理解，然后进入 ItemStackSnapshot 阶段 A。
```
