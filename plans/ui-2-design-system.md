# EconomySystem UI 2.0 设计系统

## 目标

将现有 EconomySystem UI 从“每个页面各自拼卡片”收敛为一套统一、低装饰、游戏 HUD 风格的设计语言。核心原则：

- Minecraft 物品图标是主要视觉信息，不用网页式大卡片抢视觉；
- 默认状态安静，Hover / Selected / Error / Disabled 时才出现明显状态；
- 少用完整描边，优先使用背景层级、单侧强调条、细 hairline；
- 间距、文字层级、状态颜色统一使用 token，不继续大量写随意像素常量；
- Forge 1.20.1 与 NeoForge 1.21.1 共用 common View / Component 语义。

## Token

统一入口：`EconomyUiTheme`。

### Spacing

```text
MICRO   4
SMALL   8
COMPACT 12
MEDIUM  16
SECTION 24
PAGE    32
```

新布局优先从该刻度中选值。只允许在像素敏感原生 Item / Minecraft widget 对齐时使用例外值，并在代码中说明原因。

### Text

```text
PRIMARY    主要标题 / 正文
SECONDARY  次要正文 / 辅助信息
MUTED      时间、来源、说明
DISABLED   不可用信息
ERROR      错误
SUCCESS    成功
```

### State

```text
SUCCESS  绿色
INFO     青蓝
WARNING  红色（统一使用错误/危险语义色，避免与页面强调色混淆）
DANGER   红色
NEUTRAL  灰色
```

页面业务 accent 与状态色语义分离，不因为“好看”随意复用。

### Surface

UI 2.0 使用：

- PANEL：页面一级区域；
- SECTION：列表项、小分区；
- ITEM_SLOT：物品槽；
- HAIRLINE：仅用于必要分隔或 Hover/Selected 边缘；
- CLAIMED_MASK：已领取物品遮罩。

## Common Components

### UiPanel

用于页面一级内容面板。默认不画完整边框；需要业务身份时可使用 2px 左侧 accent。

### UiSection

用于列表项、分类项、小型内容块。默认只以轻微背景区分；Hover 才提高背景亮度；Selected 可增加 2px 左侧 accent。

### UiItemSlot

统一所有经济系统物品槽状态：

```text
EMPTY
NORMAL
HOVERED
SELECTED
CLAIMED
DISABLED
```

规则：

- NORMAL：无明显完整边框；
- HOVERED：显示轻边线；
- SELECTED：保持原背景/物品不变，只显示低透明外辉光 + 明亮 accent 边线 + 轻微内辉光；
- CLAIMED：保留原物品图标，叠浅黑遮罩和最高层绿色对勾；
- DISABLED：整体降亮度。

### UiEmptyState

统一空页面：标题居中 + 可选辅助说明。不得仅在大区域中孤零零显示四个字。

## 页面布局类型

新 UI 只优先使用以下结构：

1. Sidebar + Content
2. List + Detail
3. Form -> Inventory
4. Centered Dialog
5. Grid

不得为单页随意发明完全不同的布局语言。

## 第一批迁移

已迁移：

- 邮箱侧栏 / 邮件列表 / 详情面板；
- 邮箱附件槽；
- 写邮件表单 / 9×4 附件物品栏 / 玩家补全；
- 市场上架表单 / 9×4 物品栏；
- 求购物品 ID 补全；
- 邮箱空状态；
- 商店主列表：左侧 3/4 商品浏览 + 右侧 1/4 内联购买详情；
- 商店购买：选择商品发光边框、数量输入、余额/背包容量预检、内联购买按钮。
- 市场主列表：浏览区 + 内联详情区、筛选、排序和局部交易数量；
- 邮箱写信：common controller 统一文本、附件选择、单次发送与结果导航；
- 通用输入框：透明背景、贴底文本基线和单条底线状态反馈。

## 后续迁移顺序

1. 余额与转账；
2. 继续统一 Tooltip、Scrollbar、Badge；
3. 按实际游戏验收结果微调极窄、极矮视口。

## 动画约束

商店、市场、Home、About 的页面进入动画在双端生产 shell 中固定为完成态，不再播放。
保留的 layout animation 参数仅用于兼容现有 common API 和几何测试，不代表生产运行时启用动画。
Hover / Selected 使用即时颜色和边线反馈，不改变点击命中、服务端状态或页面布局稳定性。

## 禁止事项

- 不引入大圆角 Material/Web 风格；
- 不增加高饱和渐变按钮；
- 不用完整边框包围每个静态区域；
- 不在每个页面复制一套 ItemSlot；
- 不让 target-specific Screen 决定业务 UI 风格；
- 不为视觉动画改变 server-authoritative 业务逻辑。

## 验证状态（2026-08-23）

- Forge 1.20.1：1002 tests，0 failures，1 skipped；
- NeoForge 1.21.1：1067 tests，0 failures，1 skipped；
- `buildAllTargets --no-daemon --rerun-tasks`：成功；
- common 生产源码 loader 隔离：通过；
- code/docs checkpoint：见 Git 历史。
