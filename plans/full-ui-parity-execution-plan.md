# EconomySystem 全 UI Parity 自治执行计划

> 状态：Codex 执行中（Forensic Reconciliation Pass）
> 仓库：`QingMo-A/EconomySystem`  
> 分支：`bridge`  
> 执行锚点：本计划创建前的已验收 UI 基线 `a2cb32c42ad7438ea9dd71b29e1818ac06f1415d`（`document home pixel parity`）  
> 视觉 / 交互参考：`f334e640ca1e24157511b7e06f1f76efba90152b` 时期的成熟 NeoForge 1.21.1 root UI  
> 当前已验收模板：Territory Manage pixel parity、Home pixel parity  
> 最终目标：一次启动 Codex 后连续完成其余全部 active UI family 的 1.21.1 reference parity，并在 Forge 1.20.1 / NeoForge 1.21.1 上保持相同 common UI truth。

---

## 0. 使用方式

本文件设计为 Codex 的**一次性总任务**。执行者读取完本计划后，应自行连续完成所有阶段，不需要用户在阶段之间转述提示词。

用户只需要给 Codex：

```text
读取 plans/full-ui-parity-execution-plan.md，严格按计划在 bridge 分支自治执行全部阶段；每个 UI family 独立验收、独立提交，全部完成后推送到 origin/bridge，并输出最终报告。不要中途向我索要阶段性提示词。
```

除非出现以下真正阻塞条件，否则不要询问用户：

1. 需要改变现有业务规则 / wire schema / 持久化格式；
2. legacy 参考存在互相矛盾且无法从代码、资源或测试判定的设计；
3. 需要删除用户数据或执行不可逆仓库操作；
4. 当前 `origin/bridge` 在执行期间被第三方推进且无法安全同步。

普通视觉取舍、布局细节、target API 写法、测试设计由 Codex依据本计划和 legacy reference 自主完成。

---

# 1. 当前已确认架构状态

当前 active UI inventory 已全部存在 common contract + 两个 target shell：

- Home
- About
- Balance Log
- Shop Catalog
- Shop Purchase
- Market List
- Market Create
- Market Confirm
- Delivery Box
- Territory List
- Territory Manage
- Territory Detail / Access / Rules
- Territory Buffs
- Territory Invite
- Territory Delete / Member Removal Confirmation
- Client File Check
- Checked File Transfer

当前问题不是“缺 UI 架构”，而是：

> Territory Manage 与 Home 已完成 pixel parity；其余页面虽然 migrated，但尚未逐页完成 legacy 1.21.1 visual/reference parity audit。

最终状态必须从：

```text
migrated
```

提升为：

```text
structural parity accepted
behavior parity accepted
visual/pixel parity accepted
```

---

# 2. 最高优先级架构规则

## 2.1 Common 是 active UI 唯一 Source of Truth

所有版本无关内容必须由 common 定义：

- layout geometry；
- 640×360 virtual canvas；
- padding / margin / spacing；
- theme token；
- card/button chrome；
- icon semantic；
- text metrics usage；
- truncation；
- paging / scrolling / filtering；
- enabled / disabled / hover state；
- animation duration/easing/offset；
- tooltip model；
- loading / ready / empty / error / timeout 状态；
- action enablement；
- UI navigation intent。

Target 只负责当前 Minecraft API 的表达：

- `Screen` lifecycle；
- `GuiGraphics` / pose API；
- `Font` adapter；
- `EditBox` lifecycle；
- native item / player head rendering；
- clipboard / filesystem dialog；
- network send adapter；
- native screen switching。

禁止在 Forge 与 NeoForge 各写一套布局或视觉常量。

## 2.2 Common 不得 import Minecraft / Loader

始终检查：

```powershell
rg -n "net\.minecraft|net\.minecraftforge|net\.neoforged" common/src/main/java/com/mo/economy_system/ui
```

结果必须为空。

## 2.3 Legacy root UI 是只读参考

以下目录不重新 attach 为 production：

```text
src/main/java/com/mo/economy_system/screen/**
src/main/java/com/mo/economy_system/client/util/**  # 仅视觉/动画参考时读取
```

需要精确对照时使用：

```powershell
git show f334e640ca1e24157511b7e06f1f76efba90152b:<legacy-path>
```

不要修改 root legacy Screen 来让新版“看起来一样”。

## 2.4 READY 正常路径必须匹配 legacy；增强状态可以保留

现 common 中合理新增的：

- LOADING；
- ERROR；
- TIMEOUT；
- RETRY；
- stale-response protection；
- request-id / revision safety；

可以保留。

但是 `READY` 正常页面不得因为这些增强而改变 legacy：

- 正常布局；
- 信息密度；
- 按钮顺序；
- hover；
- 翻页 / 滚动方式；
- 点击流程；
- 颜色；
- 图标；
- footer；
- 标题；
- 动画。

## 2.5 业务层冻结

本计划是 UI parity 计划，不是第二次业务大迁移。

默认 READ-ONLY：

```text
common/redpacket/**
common/reward/**
common/tpa/**
common/starter/**
common/update/**
common/src/main/java/com/mo/economy_system/common/economy/**
common/src/main/java/com/mo/economy_system/common/market/**
common/src/main/java/com/mo/economy_system/common/delivery/**
common/src/main/java/com/mo/economy_system/network/**
```

禁止无关修改：

- protocol discriminator；
- wire schema；
- message direction；
- SavedData / NBT schema；
- transaction semantics；
- repository semantics；
- server gameplay；
- pricing policy；
- inventory transaction；
- territory transaction。

如果在 UI 验收过程中发现一个明确的非 UI bug：

- 不要顺手大改；
- 在 `plans/full-ui-parity-followups.md` 记录路径、复现条件、风险和建议；
- 继续下一个 UI family；
- 只有“UI 无法工作且最小 client adapter 修复即可解决”时，才允许做最小修复并增加回归测试。

---

# 3. 已验收 UI 基础设施：禁止退化

以下基础已经通过 Territory Manage / Home 两轮精确 audit：

- `UiScale` legacy truncation semantics；
- `UiTextMetrics` target-real metrics；
- `UiChromePlan`；
- `UiFillCommand`；
- `UiCardStyle`；
- `UiButtonStyle`；
- `UiIcon` + common 64×64 resource authority；
- `EconomyUiRenderer` pixel-sensitive fail-closed semantic；
- `VERSION_CARD`；
- `scaledIconText`；
- `scaledIconStyledText`；
- `translatedIconButton`；
- `UiEasing`；
- physical-background-before-virtual-pose pattern（页面需要时）；
- Layout rect = render rect = hit-test rect 原则。

迁移其他页面时优先复用这些基础。

不要为了某一页方便，重新在 target 内硬编码一套：

```text
card fill
button glow
border
icon size
text width
page geometry
footer geometry
```

如果 legacy 页面需要新的视觉 primitive：

1. 先抽成 loader-neutral common semantic / plan；
2. 写 exact primitive test；
3. 两个 target 仅做机械 API translation；
4. 再由具体页面使用。

---

# 4. Preflight 与 Git 安全规则

开始执行时：

```powershell
git status --short --branch
git fetch origin bridge
git rev-parse HEAD
git rev-parse origin/bridge
git log --oneline --decorate -15
```

要求：

- 工作区干净；
- 本地 HEAD == `origin/bridge`；
- 历史包含：

```text
a2cb32c document home pixel parity
ec4730d finish home pixel parity
b4275ec document home visual parity
beebaa5 restore home visual parity
cb7228f document territory manage pixel parity
6976818 finish territory manage pixel parity
```

本计划本身会位于上述基线之后的一个 plan-only commit；执行时以**当前最新 `origin/bridge`**作为实际 execution base，并把 SHA 写入本文件的 Execution Ledger。

禁止：

```text
git reset --hard
git reset --soft
git clean -fd
git add .
git add -A
git push --force
git push --force-with-lease
```

只允许显式暂存文件。

---

# 5. 自治阶段机制

Codex 必须按下面 Phase 顺序连续执行。

每个 Phase 都遵循同一个循环：

```text
A. Audit legacy reference
B. Audit current common/target implementation
C. 建立差异清单
D. 只实现当前 family 的 parity
E. 增加/修正 common tests
F. static scan
G. Forge target tests
H. NeoForge target tests
I. buildAllTargets
J. jar/resource/isolation audit（涉及资源时）
K. code commit
L. docs / ledger commit
M. 进入下一 Phase
```

任何 Phase 在测试通过前不得提交 production code。

若某 Phase 无法在 UI scope 内完成：

1. 不提交半成品 production code；
2. 仅显式恢复当前 Phase 修改的文件；
3. 写入 `plans/full-ui-parity-followups.md`；
4. 做 docs-only blocked checkpoint；
5. 继续下一 Phase。

不要因为单页困难而停止整份计划。

---

# 6. 每个 UI family 的统一 Parity Acceptance Checklist

每一页都必须逐项检查。

## 6.1 Geometry

至少测试：

```text
640×360
854×480
1280×720
1920×1080
800×600
1000×563
fractional-scale viewport
narrow viewport
short viewport
```

canonical viewport 使用 exact assertions。

确认：

- virtual size；
- panel rect；
- card rect；
- button rect；
- title/icon positions；
- footer；
- page controls；
- search/filter widgets；
- item grid/list positions；
- tooltip anchor；
- empty/error center；
- hitboxes 与 render geometry 同源。

## 6.2 Visual Primitive

逐页面对照 legacy：

- background layer；
- card normal / hover；
- accent direction / width / alpha；
- border 哪几条边；
- button background alpha；
- stripe normal/hover；
- glow rows；
- disabled style；
- page button style；
- text color hierarchy；
- text shadow；
- alignment；
- separator lines；
- real texture icons；
- item render size；
- player head size；
- footer/version card；
- animation。

禁止用字母或字符假装图标。

## 6.3 Text Metrics

凡涉及：

- 居中；
- 右对齐；
- ellipsis；
- long name；
- price；
- balance；
- page count；
- footer scale；

必须使用 `UiTextMetrics` 或 target-native Font adapter，不得 `length()*N` 猜宽度。

## 6.4 Behavior

确保：

- 正常点击 action 与 legacy 一致；
- 一个 click 不发送重复 packet；
- destructive action 保留 confirmation；
- ESC / Back 语义一致；
- pagination / wheel scroll 不互换；
- search/filter/sort 与 legacy 一致；
- disabled button 不产生 action；
- loading/error 不锁死本可用导航；
- stale response 不覆盖新状态。

## 6.5 Renderer contract

`RecordingRenderer` 只能证明 common View deterministic output，不得命名为“两个 target parity”。

Target parity 通过：

- mandatory interface implementation；
- architecture/source gate；
- target tests；
- common semantic plan；

共同证明。

---

# 7. Phase 1 — About + Balance Log

## 7.1 About

Reference：

```text
src/main/java/com/mo/economy_system/screen/Screen_About.java
相关 CardRenderer / resource
```

检查：

- 页面布局；
- title/footer；
- logo / QR / texture；
- 文本层级；
- link / clipboard 行为；
- ESC/back；
- hover；
- shared texture 是否只存在 common 一份。

当前 About 已有 shared texture 修复，不得重新复制 target resource。

## 7.2 Balance Log

Reference：

```text
src/main/java/com/mo/economy_system/screen/economy_system/logs/Screen_BalanceLog.java
```

重点：

- 搜索框 / 输入框真实位置尺寸；
- log card/list；
- 正负金额颜色；
- 时间/描述文本；
- 分页/滚动；
- empty/loading/error；
- 返回行为；
- 数字格式；
- EditBox 只由 target 管 lifecycle，common 管 value/validation/layout。

### Phase 1 code commit

```text
restore about and balance visual parity
```

### Phase 1 docs commit

```text
document about and balance visual parity
```

---

# 8. Phase 2 — Shop Family

范围：

```text
ui/shop/Shop*
ui/shop/ShopPurchase*
Forge1201ShopScreen
Forge1201ShopPurchaseScreen
NeoForge1211ShopScreen
NeoForge1211ShopPurchaseScreen
```

Reference：

```text
legacy shop catalog Screen
legacy purchase Screen
CardRenderer shop item card
相关 button/text field renderer
```

必须逐项恢复：

- catalog layout；
- item card size；
- item icon size；
- item name；
- price；
- dynamic price/change indicator；
- color / border / top accent；
- hover；
- disabled/insufficient balance state；
- pagination/scroll；
- category/filter/search（仅 legacy 存在的）；
- purchase quantity EditBox；
- confirm/cancel；
- price summary；
- long item name truncation；
- tooltip；
- footer/title；
- ESC/back。

不要修改已经 common 化的 pricing / transaction semantics。

需要新增 Shop-specific card primitive 时，放 common + exact primitive test。

### Phase 2 code commit

```text
restore shop ui visual parity
```

### Phase 2 docs commit

```text
document shop ui visual parity
```

---

# 9. Phase 3 — Market Family

范围：

- Market List；
- Market Create；
- Market Confirm；
- 对应两个 target shells/widgets。

Reference：legacy market Screen family。

重点：

## Market List

- item/order cards；
- seller/buyer text；
- price；
- quantity；
- order type；
- item render；
- search / sort / page；
- own-order visual；
- click/confirm path；
- legacy page density；
- no new invented filters。

## Create

- inventory selection；
- selected item highlight；
- quantity/price EditBox；
- validation feedback；
- confirm button enablement；
- tooltip；
- layout/spacing。

## Confirm

- exact item summary；
- amount/price/total；
- destructive/financial confirmation visual；
- cancel/confirm order；
- one-shot submit protection。

现有 wire page fix / market common transaction 保持不变。

### Phase 3 code commit

```text
restore market ui visual parity
```

### Phase 3 docs commit

```text
document market ui visual parity
```

---

# 10. Phase 4 — Delivery Box

Reference：legacy Delivery Box Screen + delivery card renderer。

重点：

- delivery entry card geometry；
- item icon；
- source/reason/count text；
- claim button；
- claim-all（若 legacy 有）；
- available/claimed/expired/disabled state；
- pagination/scroll；
- empty/loading/error；
- tooltip；
- footer/title；
- one-click one claim intent。

不要修改 delivery transaction / ledger semantics。

### Phase 4 code commit

```text
restore delivery ui visual parity
```

### Phase 4 docs commit

```text
document delivery ui visual parity
```

---

# 11. Phase 5 — Remaining Territory Family

Territory Manage 已 pixel parity accepted，production code 冻结。

本 Phase 只处理：

1. Territory List；
2. Territory Detail；
3. Access；
4. Rules / permissions；
5. Buffs；
6. Invite；
7. Delete confirmation；
8. Member removal confirmation；
9. 其他 active territory nested screen。

每一个 nested page 都必须回到 legacy reference。

重点审计：

- territory icon/title/footer；
- list card accent；
- dimension/icon；
- owner/auth status；
- teleport/manage/action buttons；
- detail field order；
- mode / access / permission controls；
- rule enable/disable state；
- buff item icons、等级、价格、upgrade/unlock style；
- invite player list；
- confirmation danger style；
- destructive action exactly once；
- long name/UUID/player name truncation；
- pagination；
- ESC/back；
- latest authoritative territory snapshot。

不得为了 nested 页面重新修改已验收 Territory Manage primitive，除非发现的是**通用 primitive 的真实 bug**；如必须改通用 primitive，必须先确保 Territory Manage golden/primitive tests 全通过。

### Phase 5 code commit

```text
restore remaining territory ui parity
```

### Phase 5 docs commit

```text
document remaining territory ui parity
```

---

# 12. Phase 6 — Client File Check + Checked File Transfer

范围：

```text
ui/check/**
ui/transfer/**
Forge consent/result screens
NeoForge consent/result screens
```

这是高敏感 UI，必须同时保证：

- 视觉 parity；
- consent 语义清晰；
- 不扩大文件访问能力；
- 不自动接受；
- user-visible path / status / reason；
- cancel / deny 可达；
- result page；
- save dialog / target API 只在 target；
- common 仅持 loader-neutral state；
- 文件生命周期与现有安全策略不改变。

如果 legacy 视觉和当前安全增强冲突：

> 保留更安全的行为，但视觉尽量使用 legacy chrome/layout；在 ledger 记录显式 safety deviation。

### Phase 6 code commit

```text
restore file workflow ui visual parity
```

### Phase 6 docs commit

```text
document file workflow ui visual parity
```

---

# 13. Phase 7 — 全局 UI Consistency / Architecture Audit

所有 family 完成后执行一次全局审计。

## 13.1 Active Screen coverage

列出两个 target 下所有 active `*Screen.java`，要求每个都有：

- common controller/state/layout/view 或明确 common contract；
- architecture gate；
- 对应 parity 状态。

不得遗漏新增 Screen。

## 13.2 Target-local visual authority scan

搜索 target 中可疑 UI magic constants：

```powershell
rg -n "640|360|CARD_|PANEL_|ACCENT|0x[0-9A-Fa-f]{6,8}|NAV_|PAGE_|SPACING|PADDING" targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client
```

逐条判断：

- target API translation 必需：允许；
- 页面布局/颜色/视觉 policy：迁回 common。

## 13.3 Resource authority

UI shared resource 应位于：

```text
common/src/main/resources/assets/economy_system/
```

检查：

```powershell
git ls-files "common/src/main/resources/assets/economy_system/textures/gui/**/*"
git ls-files "targets/*/src/main/resources/assets/economy_system/textures/gui/**/*"
```

相同跨版本资源不得重复 target copy。

Target-specific resource 只有在版本 API/资源格式确实不同且有说明时允许。

## 13.4 Fake icon scan

```powershell
rg -n "substring\(0,\s*1\)|icon\.name\(\)|\"<\"|\">\"" common/src/main/java/com/mo/economy_system/ui targets/forge-1.20.1/src/main/java targets/neoforge-1.21.1/src/main/java
```

人工区分合法文字箭头与假 icon fallback。

禁止语义图标退化为首字母。

## 13.5 Metrics scan

搜索：

```powershell
rg -n "length\(\) \* [0-9]+|Math\.round\(.*width|APPROXIMATE" common/src/main/java/com/mo/economy_system/ui targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client
```

正式 target hitbox/layout 不得依赖 approximate metrics。

## 13.6 Renderer fail-closed

Pixel-sensitive operations不允许危险 fallback。

如果某 semantic 不可准确 fallback：

- interface mandatory；
- target explicit implementation；
- test RecordingRenderer explicit implementation。

## 13.7 Misleading tests

任何只用两个相同 RecordingRenderer 的测试不得命名：

```text
both targets...
both adapters...
cross-loader renderer parity...
```

应改为：

```text
commonViewProducesDeterministicSemanticOperations
```

或其他准确名称。

## 13.8 UI status matrix

更新 `plans/multiversion-ui-inventory.md`，最终每个 family 状态应明确：

```text
pixel/reference parity accepted
```

若有显式 deviation，链接到 followups/ledger。

---

# 14. 每个 Phase 必须运行的测试

只要 production Java 有修改，每个 Phase 提交前运行：

```powershell
.\gradlew.bat :targets:forge-1.20.1:test --no-daemon --rerun-tasks
.\gradlew.bat :targets:neoforge-1.21.1:test --no-daemon --rerun-tasks
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
```

禁止：

```text
:common:test
```

仅当明确是 ForgeGradle certificate check 问题时，允许当次：

```text
-Dnet.minecraftforge.gradle.check.certs=false
```

不得写入仓库配置。

本地测试不得称为 GitHub CI。

---

# 15. JAR Audit

涉及 UI texture / lang / resource 的 Phase，检查两个最终 JAR：

- required shared icon/texture 存在；
- common lang/resource 被打包；
- Forge JAR 不含 NeoForge target/API；
- NeoForge JAR 不含 Forge target/API；
- 无 target duplicate shared GUI assets。

最终 Phase 7 必须做一次完整 JAR audit。

---

# 16. Commit / Push 规则

## 16.1 每阶段 code/docs 分离

统一风格：

```text
restore <family> ui visual parity
document <family> ui visual parity
```

最后 audit：

```text
finish full ui parity audit
document full ui parity completion
```

不要一个 commit 完成所有 UI。

## 16.2 Explicit staging only

每次：

```powershell
git add -- <明确文件1> <明确文件2> ...
git diff --cached --name-only
git diff --cached --stat
git diff --cached --check
git diff --cached
```

禁止：

```text
git add .
git add -A
```

## 16.3 Remote concurrency

每阶段 commit 后可继续本地工作，但最终 push 前必须：

```powershell
git fetch origin bridge
git rev-parse HEAD
git rev-parse origin/bridge
```

如果远程在执行期间变化：

- 不 force；
- 安全 rebase/merge only when changes do not conflict with current plan；
- 同步后重跑 Forge tests、NeoForge tests、buildAllTargets。

## 16.4 Push

全部阶段完成后：

```powershell
git push origin HEAD:bridge
```

然后：

```powershell
$local = git rev-parse HEAD
$remote = (git ls-remote origin refs/heads/bridge).Split("`t")[0]

Write-Host "local=$local"
Write-Host "remote=$remote"

if ($local -ne $remote) {
    throw "Remote bridge does not match local HEAD"
}
```

禁止 force push。

---

# 17. Execution Ledger

### Forensic Reconciliation Pass

The original Phase 1–7 entries below are historical first-pass checkpoints.
Their `ACCEPTED` labels mean `FIRST_PASS_ACCEPTED` only; they are not final
reference-parity evidence. This pass reopens every active family against the
legacy source at `f334e640ca1e24157511b7e06f1f76efba90152b` and records exact
legacy tests, production corrections, target test counts, and any explicit
deviation in the separate ledger below.

### Forensic Verification Ledger

| Family | Status | Legacy source | New exact tests | Code SHA | Docs SHA | Forge | NeoForge | Build | Deviation |
|---|---|---|---|---|---|---:|---:|---|---|
| Shop | VERIFIED | `src/main/java/com/mo/economy_system/screen/economy_system/shop/Screen_Shop.java`, `Screen_BuyItem.java`, `CardRenderer.java` at forensic reference | `ShopLegacyReferenceParityTest` (3 exact tests) | `c697c0af` | pending | 824 | 889 | passed | fixed rows/grid centering, card hit target, native item names, price/tooltip, search frame, metrics paging, page styles and 12x12 texture arrows |
| Market | VERIFIED | `src/main/java/com/mo/economy_system/screen/economy_system/market/Screen_Market.java`, create/confirm screens, `CardRenderer.java` at forensic reference | `MarketLegacyReferenceParityTest` (5 exact tests; initial pre-fix failures recorded) | `bd5a6db2` | pending | 829 | 894 | passed | operator removal is supported by existing server authorization; UI exposes a separate force-remove action over the existing remove-sales wire message |
| Delivery | VERIFIED | `src/main/java/com/mo/economy_system/screen/economy_system/deliver_box/Screen_DeliveryBox.java`, `CardRenderer.java` at forensic reference | `DeliveryLegacyReferenceParityTest` (4 exact tests; initial pre-fix failures recorded) | `6b1c1d3c` | pending | 833 | 898 | passed | legacy shop-orange card accent, localized title/search frame, native item/count/source, delivery claim style, metrics footer and real 12x12 arrows |
| About + Balance | VERIFIED | `src/main/java/com/mo/economy_system/screen/Screen_About.java`, `Screen_BalanceLog.java` at forensic reference | `AboutLegacyReferenceParityTest` + `BalanceLogLegacyReferenceParityTest` (2 exact tests; initial pre-fix failures recorded) | `5d89f765` | pending | 835 | 900 | passed | localized About title/mod name, gray About cards, gold Balance panel/actions, legacy alternating table rows and footer geometry |
| Remaining Territory | PENDING | list/detail/access/rules/buff/invite/confirmation screens at forensic reference | pending | pending | pending | pending | pending | pending | Territory Manage frozen |
| File Check + Transfer | PENDING | file-check and checked-transfer screens at forensic reference | pending | pending | pending | pending | pending | pending | safety deviations recorded explicitly |
| Strict global audit | PENDING | all active screens and shared UI primitives | pending | pending | pending | pending | pending | pending | pending |

### First-pass status mapping

All historical Phase 1–7 rows below are `FIRST_PASS_ACCEPTED` until their
corresponding forensic row above reaches `VERIFIED` or
`BLOCKED_WITH_FOLLOWUP`.

Codex 在开始时填写：

```text
Execution base SHA: `9c2797bca0ca4f44e27ee800d61699065616274b`
Started: `2026-08-09 America/Caracas`
```

每个 Phase 的 docs commit 更新下面状态：

| Phase | Family | Status | Code SHA | Docs SHA | Forge tests | NeoForge tests | buildAllTargets | Notes |
|---|---|---|---|---|---:|---:|---|---|
| 0 | Territory Manage | ACCEPTED | `6976818` | `cb7228f` | inherited | inherited | passed | pixel parity template |
| 0 | Home | ACCEPTED | `ec4730d` | `a2cb32c` | 821 | 886 | passed | pixel parity template |
| 1 | About + Balance | FIRST_PASS_ACCEPTED | `21f37d1a` | `921a1193` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |
| 2 | Shop family | FIRST_PASS_ACCEPTED | `82dbf067` | `fec80f38` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |
| 3 | Market family | FIRST_PASS_ACCEPTED | `6ab64e31` | `4ffaef18` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |
| 4 | Delivery | FIRST_PASS_ACCEPTED | `2b5ede4a` | `9199d91e` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |
| 5 | Remaining Territory | FIRST_PASS_ACCEPTED | `65094073` | `7090ee7b` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |
| 6 | File Check + Transfer | FIRST_PASS_ACCEPTED | `f260728d` | `e481a46e` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |
| 7 | Global audit | FIRST_PASS_ACCEPTED | `49387b1` | `ddadf482` | 821 | 886 | passed | Historical checkpoint; forensic row above is authoritative |

允许状态：

```text
PENDING
IN_PROGRESS
ACCEPTED
BLOCKED_WITH_FOLLOWUP
```

最终除有明确 followup 的项目外，应全部 ACCEPTED。

---

# 18. Final Completion Criteria

只有全部满足才能宣布“全 UI parity 完成”：

1. Territory Manage = accepted；
2. Home = accepted；
3. About = accepted；
4. Balance Log = accepted；
5. Shop Catalog = accepted；
6. Shop Purchase = accepted；
7. Market List = accepted；
8. Market Create = accepted；
9. Market Confirm = accepted；
10. Delivery Box = accepted；
11. Territory List = accepted；
12. Territory Detail / Access / Rules = accepted；
13. Territory Buffs = accepted；
14. Territory Invite = accepted；
15. Territory Confirmation screens = accepted；
16. Client File Check = accepted 或 safety deviation documented；
17. Checked File Transfer = accepted 或 safety deviation documented；
18. common UI 无 Minecraft/loader import；
19. active target Screens 全部有 common contract；
20. target 不拥有页面视觉 policy；
21. shared UI resource common-only；
22. no fake semantic icon fallback；
23. real metrics 用于正式 layout/hitbox；
24. all common parity/golden/controller tests pass；
25. Forge target tests pass；
26. NeoForge target tests pass；
27. `buildAllTargets` pass；
28. both JAR resource/isolation audit pass；
29. `plans/multiversion-ui-inventory.md` 更新为最终状态；
30. `git status --short` 为空；
31. local HEAD == remote `bridge` after push。

---

# 19. Final Report

全部完成后一次性报告，不要求用户逐阶段响应。

报告至少包含：

```text
execution base SHA
final local SHA
final remote SHA

phase 1 code/docs SHA
phase 2 code/docs SHA
phase 3 code/docs SHA
phase 4 code/docs SHA
phase 5 code/docs SHA
phase 6 code/docs SHA
phase 7 code/docs SHA

all changed UI families
blocked followups（若有）
explicit safety deviations（若有）

final UI inventory status
common/target architecture result
shared resource authority result
fake icon scan result
metrics scan result
renderer contract result

final Forge test count
final NeoForge test count
buildAllTargets result
Forge JAR audit
NeoForge JAR audit
target isolation result

git status
local SHA == remote SHA
```

不要把本地 Gradle test 称为 GitHub CI。

---

# 20. 最终停止条件

完成并推送本计划后立即停止。

不要在同一任务中继续：

- 新功能；
- 新协议；
- 新经济机制；
- gameplay 重构；
- server transaction redesign；
- 版本 1.22.x / 1.23.x adapter；
- UI redesign beyond legacy parity。

下一步应由后续总体 audit 决定，而不是本计划自动扩张。
