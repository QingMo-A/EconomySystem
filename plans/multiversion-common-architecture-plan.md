# EconomySystem 多版本 Common 架构统一计划

> 文档状态：Architecture Plan / Proposed Baseline  
> 适用仓库：`QingMo-A/EconomySystem`  
> 适用分支：`bridge`  
> 当前主要目标版本：Forge 1.20.1、NeoForge 1.21.1  
> 当前行为与视觉基准：NeoForge 1.21.1 现有成熟实现  
> 最终唯一 Source of Truth：`common`  
> 目标：将现有 1.21.1 的模组业务逻辑、UI 行为、布局、视觉规范和交互状态抽象为版本无关的 common 层，各 Minecraft/Loader target 仅负责使用对应版本 API 表达同一套 common 语义。

---

## 1. 背景与问题定义

EconomySystem 已经建立了 `common + targets/<version>` 的多版本 Bridge 基础，并已经将大量协议、数据模型、事务逻辑和跨版本行为迁移到 loader-neutral common 层。但是当前 UI 和部分业务入口仍处于过渡状态：NeoForge 1.21.1 继续编译根目录 `src/main/java` 中的原始成熟实现，而 Forge 1.20.1 明确只编译 `common` 与 `targets/forge-1.20.1`，因此部分 Forge 页面为了先完成协议和功能闭环而重新实现了“最小可用 UI”。

这造成以下问题：

1. **同一个功能在 1.20.1 与 1.21.1 上视觉不一致。**
2. **同一个 Screen 的布局、分页、按钮、动画、交互状态分别维护。**
3. **Forge target 容易出现“为了兼容 API 而重新设计 UI”的情况。**
4. **1.21.1 根 `src/main/java` 事实上仍承担隐藏的业务与视觉 Source of Truth。**
5. **后续新增 1.22.x、1.23.x 等版本时，若继续复制 Screen 和业务入口，维护成本会按版本数量线性增长。**
6. **一次 UI 或业务修改可能需要分别修改 common、Forge Screen、NeoForge Screen，容易发生行为漂移。**
7. **测试只能证明各 target 自己能运行，难以证明“两个版本表达的是同一个功能”。**

本计划的核心不是简单将 1.21.1 的类复制到 1.20.1，也不是让 common 直接继承某个 Minecraft 版本的 `Screen`。目标是建立稳定的 **Domain / Application / UI Core / Platform Adapter** 分层，使版本变化只影响最下层的 API 表达。

---

## 2. 最终架构目标

最终目标架构：

```text
                       EconomySystem
                            │
                            ▼
                    common (唯一真源)
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
     Domain             Application            UI Core
  数据模型/规则        用例/事务/状态机      布局/主题/Controller
        │                   │                   │
        └───────────────────┼───────────────────┘
                            │
                            ▼
                      Platform Ports
                            │
              ┌─────────────┴─────────────┐
              │                           │
              ▼                           ▼
       Forge 1.20.1 Adapter       NeoForge 1.21.1 Adapter
              │                           │
              ▼                           ▼
       Minecraft 1.20.1 API        Minecraft 1.21.1 API
```

迁移完成后必须满足：

```text
Forge 1.20.1 = common + forge-1.20.1 adapter
NeoForge 1.21.1 = common + neoforge-1.21.1 adapter
未来版本      = common + 新版本 adapter
```

根目录 `src/main/java` 不再作为 1.21.1 的隐藏业务实现来源。当前 1.21.1 原实现只在迁移期间承担“行为与视觉参考基准”职责；迁移完成后，common 升级为新的唯一权威实现。

---

## 3. 架构原则

### 3.1 1.21.1 Behavior & Visual Baseline Rule

在迁移期间，当前 NeoForge 1.21.1 已存在的成熟行为和视觉效果是基准。

任何向 common 或低版本 target 的迁移必须满足：

- 不因为 1.20.1 API 较旧而主动删减现有功能；
- 不因为移植困难而改成原版简易按钮 UI；
- 不改变原有分页、卡片、主题、动画、信息密度和操作流程，除非用户明确批准新的跨版本设计；
- 版本差异只能来自 API 能力，不得来自 target 自行设计。

若某视觉能力在旧版本确实无法完全表达，必须记录为显式 capability deviation，而不是静默降级。

### 3.2 Single Source of Business Truth

业务规则只允许存在一份权威实现。

以下内容必须优先位于 common：

- 账户与余额规则；
- 市场订单状态机；
- 配送箱领取事务；
- 领地权限规则；
- buff / rule / transfer 业务规则；
- 文件检查与文件传输状态机；
- 数据验证；
- 分页、筛选、排序等与版本无关的 UI 行为；
- 错误码和结果语义。

Target 层不得重新实现一套“等价业务算法”。Target 只能实现 common 定义的 Port。

### 3.3 Single Source of UI Truth

以下 UI 设计语义必须只有一份 common 实现：

- 虚拟设计分辨率；
- 页面区域；
- 卡片位置与尺寸；
- padding / margin / spacing；
- 主题颜色；
- 文本层级；
- hover / active / disabled 状态；
- 分页与滚动规则；
- tooltip 数据；
- 动画曲线和时长；
- 页面状态机；
- 按钮显示条件；
- Tiny screen / narrow screen 布局策略；
- 键盘/鼠标语义。

Target 只能负责把这些语义转换成当前 Minecraft API 的 draw / widget / event 调用。

### 3.4 Common 不直接继承 Minecraft Screen

禁止设计：

```java
public abstract class CommonScreen extends net.minecraft.client.gui.screens.Screen
```

原因：

- `Screen` 构造、render、事件方法签名可能随 MC 版本变化；
- GuiGraphics / PoseStack / Component / Widget API 会变化；
- 这会重新把 common 锁死到某个 Minecraft 版本。

推荐设计：

```java
public abstract class AbstractEconomyScreenController<S, E> {
    protected S state;

    public abstract void initialize();
    public abstract void tick(long nowNanos);
    public abstract void handle(E event);
    public abstract S state();
}
```

然后 target Screen 组合 controller：

```java
public final class Forge1201TerritoryManageScreen extends Screen {
    private final TerritoryManageController controller;
}
```

```java
public final class NeoForge1211TerritoryManageScreen extends Screen {
    private final TerritoryManageController controller;
}
```

也就是说：**common 的“父类”是行为父类 / Controller 父类，而不是 Minecraft Screen 父类。**

### 3.5 Composition First

跨版本边界优先使用：

- interface；
- immutable record；
- controller composition；
- renderer port；
- repository port；
- network port。

只有真正稳定且版本无关的行为骨架才使用 abstract class。

---

## 4. 推荐目录结构

长期目标：

```text
common/src/main/java/com/mo/economy_system/common/
├─ domain/
│  ├─ economy/
│  ├─ market/
│  ├─ delivery/
│  ├─ territory/
│  └─ check/
│
├─ application/
│  ├─ economy/
│  ├─ market/
│  ├─ delivery/
│  ├─ territory/
│  └─ check/
│
├─ network/
│  ├─ message/
│  ├─ validation/
│  └─ protocol/
│
├─ platform/
│  ├─ ClientNetworkPort.java
│  ├─ ServerPlayerPort.java
│  ├─ InventoryPort.java
│  ├─ StoragePort.java
│  ├─ RegistryPort.java
│  ├─ ClientClock.java
│  └─ UiPlatformPort.java
│
└─ ui/
   ├─ core/
   │  ├─ AbstractEconomyScreenController.java
   │  ├─ ScreenState.java
   │  ├─ UiEvent.java
   │  └─ UiNavigation.java
   ├─ geometry/
   │  ├─ UiRect.java
   │  ├─ UiPoint.java
   │  ├─ UiInsets.java
   │  └─ UiScale.java
   ├─ theme/
   │  ├─ EconomyUiTheme.java
   │  ├─ UiColors.java
   │  ├─ UiTypography.java
   │  ├─ UiCardStyle.java
   │  └─ UiButtonStyle.java
   ├─ animation/
   │  ├─ HoverAnimation.java
   │  ├─ ValueAnimation.java
   │  └─ AnimationClock.java
   ├─ renderer/
   │  ├─ EconomyUiRenderer.java
   │  ├─ UiRenderCommand.java
   │  └─ UiIcon.java
   ├─ component/
   │  ├─ CardModel.java
   │  ├─ ButtonModel.java
   │  ├─ TextModel.java
   │  ├─ ListModel.java
   │  └─ TooltipModel.java
   ├─ home/
   ├─ market/
   ├─ delivery/
   ├─ territory/
   └─ check/
```

Target 目录长期目标：

```text
targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/
├─ platform/
├─ network/
├─ persistence/
├─ registry/
├─ event/
└─ ui/
   ├─ Forge1201UiRenderer.java
   ├─ Forge1201ScreenFactory.java
   └─ screen/
```

```text
targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/
├─ platform/
├─ network/
├─ persistence/
├─ registry/
├─ event/
└─ ui/
   ├─ NeoForge1211UiRenderer.java
   ├─ NeoForge1211ScreenFactory.java
   └─ screen/
```

---

## 5. 业务逻辑分层

### 5.1 Domain

Domain 只描述经济系统本身，不描述 Minecraft API。

例如：

```text
Account
Balance
MarketOrder
DeliveryEntry
TerritorySnapshot
TerritoryRule
TerritoryBuff
CheckedFileMetadata
```

Domain 可以依赖：

```text
java.lang
java.util
UUID
record
enum
不可变集合
```

Domain 不应直接依赖：

```text
ServerPlayer
Player
MinecraftServer
Level
ItemStack
CompoundTag
SavedData
GuiGraphics
Screen
Forge / NeoForge event
```

当 Minecraft 数据无法直接放入 common 时，应先转换为稳定 Snapshot / DTO。

### 5.2 Application

Application 负责用例和事务，例如：

```text
TransferMoneyUseCase
CreateSalesOrderUseCase
ClaimDeliveryEntryUseCase
ResizeTerritoryUseCase
UpdateTerritoryRuleUseCase
CheckedFileTransferCoordinator
```

它通过 Port 获取外部能力。

示例：

```java
public interface InventoryPort {
    InventoryInsertResult insert(UUID playerId, ItemStackSnapshot item);
    InventorySnapshot snapshot(UUID playerId);
    RestoreResult restore(UUID playerId, InventorySnapshot snapshot);
}
```

不同版本负责实现它，而不是在每个版本重写 ClaimDeliveryEntry 的事务逻辑。

---

## 6. Platform Port 设计

建议逐步形成明确的版本边界。

### 6.1 ClientNetworkPort

```java
public interface ClientNetworkPort {
    void send(Object message);
}
```

UI Controller 只调用：

```java
network.send(new UpdateTerritoryRuleMessage(...));
```

不调用：

```text
EconomySystem_NetworkManager.sendToServer
PacketDistributor
CustomPacketPayload API
Forge SimpleChannel
NeoForge PayloadRegistrar
```

### 6.2 Server Player / Inventory Port

提供：

- authenticated UUID；
- display name；
- inventory snapshot / insert / remove；
- dimension ID；
- position snapshot；
- permission checks。

禁止 common 直接持有可跨 tick 失效的 loader/player 对象作为长期状态。

### 6.3 Storage Port

Common Repository 只定义事务语义。

Target 负责：

```text
1.20.1 SavedData / NBT API
1.21.1 SavedData / Data Components / NBT API
```

持久化差异不能反向污染业务 service。

### 6.4 Registry Port

用于：

- item ID → target item；
- enchantment ID；
- component compatibility；
- snapshot capture / restore。

必须继续保持已有 fail-closed 原则。

---

## 7. UI Core 设计

## 7.1 ViewState

每个 Screen 应有 immutable 或受控可变的 common ViewState。

领地管理示例：

```java
public record TerritoryManageState(
    TerritorySummary territory,
    TerritoryManageView view,
    List<MemberRow> members,
    List<BuffRow> buffs,
    List<RuleRow> rules,
    List<PlayerRow> transferTargets,
    int page,
    int scroll,
    boolean loading,
    String errorCode
) {}
```

Target Screen 不应自己维护另一套：

```text
currentPage
scroll
loading
failed
selectedMember
```

除非它只是纯 Minecraft widget 生命周期状态。

## 7.2 Controller

Controller 负责：

- 初始化；
- 发起数据请求；
- 接收 common client state；
- 页面切换；
- 分页；
- 筛选；
- 滚动；
- 按钮 enabled 条件；
- 用户动作转换成 common message / use case；
- timeout；
- retry；
- navigation intent。

例如：

```java
public final class TerritoryManageController
    extends AbstractEconomyScreenController<TerritoryManageState, TerritoryManageEvent> {

    public void nextPage() {}
    public void previousPage() {}
    public void kickMember(UUID id) {}
    public void updatePermission(UUID id, boolean value) {}
    public void transferOwnership(UUID id) {}
}
```

Target Screen 不得复制这些判断。

## 7.3 Layout

布局必须独立于绘制。

例如：

```java
public final class TerritoryManageLayout {
    public static Layout calculate(
        int physicalWidth,
        int physicalHeight,
        TerritoryManageState state
    ) { ... }
}
```

返回：

```java
public record Layout(
    float scale,
    int virtualWidth,
    int virtualHeight,
    UiRect playerPanel,
    UiRect actionPanel,
    List<MemberCardLayout> cards,
    UiRect previousButton,
    UiRect nextButton
) {}
```

当前 1.21.1 已存在的 640×360 虚拟画布思想应优先保留，并转化为 common layout contract，而不是仅在某个 NeoForge Screen 中存在。

## 7.4 Theme

将现有 `CardRenderer`、`UiButtonRenderer`、`UiButtonStyle` 中的视觉语义分离。

例如：

```java
public final class EconomyUiTheme {
    public static final int BASE_WIDTH = 640;
    public static final int BASE_HEIGHT = 360;

    public static final UiCardStyle TERRITORY_CARD = ...;
    public static final UiCardStyle MARKET_CARD = ...;
    public static final UiButtonStyle PRIMARY = ...;
    public static final UiButtonStyle DANGER = ...;
}
```

Theme 持有：

- ARGB；
- stripe width；
- border alpha；
- hover alpha；
- disabled alpha；
- padding；
- typography token；
- card emphasis；
- icon semantic ID。

Theme 不持有 GuiGraphics。

## 7.5 Renderer

建议 common 定义绘制语义接口：

```java
public interface EconomyUiRenderer {
    void fill(UiRect rect, int argb);
    void text(String text, int x, int y, int argb);
    void card(UiRect rect, UiCardStyle style, boolean hovered);
    void button(UiRect rect, UiButtonStyle style, String text,
                boolean hovered, boolean enabled);
    void icon(UiIcon icon, UiRect rect);
    void item(UiItemSnapshot item, UiRect rect);
    void playerHead(UUID playerId, String playerName, UiRect rect);
}
```

实际实现：

```text
Forge1201UiRenderer
NeoForge1211UiRenderer
```

两端可以内部使用不同 GuiGraphics / Pose API，但必须渲染同一份 layout/theme/model。

## 7.6 Render Command 模式（可选）

若直接 renderer interface 仍容易产生 target 分支，可进一步让 common 构造：

```text
List<UiRenderCommand>
```

例如：

```text
FillRect
DrawText
DrawCard
DrawButton
DrawItem
DrawPlayerHead
PushClip
PopClip
```

Target renderer 只执行命令。

该模式非常适合做跨版本 UI 结构测试，但不要求第一阶段一次性全部采用。

---

## 8. 动画统一

当前动画不能由两个 target 分别调参数。

动画 common 化：

```java
public final class HoverAnimation {
    private float value;

    public float update(boolean target, float deltaSeconds) { ... }
}
```

Common 决定：

- duration；
- easing；
- target value；
- opacity；
- translation；
- scale；
- highlight intensity。

Target 只提供：

- frame delta；
- hover input；
- 最终 draw API。

因此 1.20.1 与 1.21.1 的动画速度和视觉响应必须一致。

---

## 9. 输入与交互抽象

建议定义 common event：

```java
sealed interface UiEvent {
    record Click(int x, int y, int button) implements UiEvent {}
    record Scroll(double delta) implements UiEvent {}
    record Key(int key, int modifiers) implements UiEvent {}
    record Tick(long nowNanos) implements UiEvent {}
}
```

不要求所有低层输入都必须传给 Controller，但以下交互语义必须 common：

- 列表选中；
- 上一页/下一页；
- tab/view 切换；
- retry；
- confirm/cancel；
- ESC 返回目标；
- 搜索与过滤；
- 滚动边界；
- disable 条件；
- timeout 行为。

Minecraft 特有的 focus / narration / widget 生命周期保留在 target shell。

---

## 10. Screen Shell 模式

每个版本 Screen 最终应该非常薄。

理想结构：

```java
public final class Forge1201TerritoryManageScreen extends Screen {
    private final TerritoryManageController controller;
    private final Forge1201UiRenderer renderer;

    @Override
    public void tick() {
        controller.tick(System.nanoTime());
        rebuildWidgetsIfNeeded();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var state = controller.state();
        var layout = TerritoryManageLayout.calculate(width, height, state);
        renderer.begin(graphics);
        TerritoryManageView.render(renderer, state, layout, mouseX, mouseY);
        renderer.end();
    }
}
```

NeoForge 版本结构应近似相同，只改变：

```text
Screen API
GuiGraphics API
事件注册
widget 构造方式
item/head 绘制 API
网络发送 adapter
```

若两个 target Screen 中出现大段完全相同的业务 if/switch/layout 数学，说明抽象还不够完整。

---

## 11. 1.21.1 → Common 的迁移规则

每迁移一个类，不允许直接“移动文件并修到能编译”。必须先分类其职责。

### A. 纯业务

直接迁移 common。

### B. 业务 + Minecraft API 混合

拆成：

```text
common service/controller
+
target adapter
```

### C. UI 布局 + 绘制混合

拆成：

```text
common layout/theme/view model
+
target renderer
```

### D. UI 行为 + Screen 生命周期混合

拆成：

```text
common controller/state
+
target Screen shell
```

### E. 完全版本 API

保留 target。

例如：

```text
注册表
事件总线
CustomPayload 注册
SavedData factory API
Data Components
ForgeCapabilities
NeoForge attachment/API
GuiGraphics 具体调用
```

---

## 12. 现有 UI 组件迁移策略

当前 1.21.1 根目录中的组件应逐个审计，而不是直接复制。

已知重点包括：

```text
CardRenderer
UiButtonRenderer
UiButtonStyle
AnimatedButton
AnimatedHighLevelTextField
HighLevelTextField
TextAnimation
ItemIconAnimation
```

推荐分类：

### `UiButtonStyle`

优先迁到 common，保持纯数据。

### `CardRenderer`

拆为：

```text
CardStyle / CardTheme / CardLayout → common
具体 GuiGraphics draw → target renderer
```

### `UiButtonRenderer`

拆为：

```text
button visual model → common
pixel draw implementation → target
```

### `AnimatedButton`

拆为：

```text
animation state / easing / timing → common
Minecraft widget shell → target
```

### TextField

文本编辑是 Minecraft widget API 变化较大的区域，不应强行完全 common 化。

Common 负责：

- value；
- validation；
- max length；
- placeholder semantic；
- error state；
- submit behavior。

Target 负责具体 EditBox / text field widget。

---

## 13. 首个试点：领地管理 UI

领地管理是最适合的首个完整 UI parity 试点，因为当前 1.21.1 与 1.20.1 差异明显。

### 13.1 以 1.21.1 为基准冻结视觉规格

记录：

- 640×360 虚拟画布；
- 背景；
- 标题位置；
- ESC hint；
- 左侧成员列表；
- 右侧操作面板；
- member card 尺寸；
- player head；
- UUID secondary text；
- kick danger button；
- 分页控件；
- theme stripe；
- hover；
- tiny screen 退化策略。

### 13.2 抽离 Common

建议创建：

```text
common/ui/territory/TerritoryManageController
common/ui/territory/TerritoryManageState
common/ui/territory/TerritoryManageLayout
common/ui/territory/TerritoryManageView
```

### 13.3 NeoForge Adapter

先让 1.21.1 使用 common 重新表达，并通过视觉/行为验收。

**只有 1.21.1 common 化后仍与旧基准一致，才能开始 Forge parity。**

### 13.4 Forge Adapter

删除/替换当前 Forge 自己设计的简化页面，使其使用同一个 controller/layout/theme。

### 13.5 验收

在相同窗口尺寸、相同数据输入下：

- card bounds 相同；
- button bounds 相同；
- 字符串相同；
- 分页相同；
- hover state 相同；
- enabled state 相同；
- controller action 相同；
- network message 相同；
- tiny-screen 决策相同。

像素层可允许字体 rasterization / Minecraft 内部渲染存在极小版本差异，但设计语义不得不同。

---

## 14. UI 迁移优先级

完成试点后按以下优先级推进。

### P0：核心公共组件

- Ui geometry；
- Theme；
- Card style；
- Button style；
- Animation；
- Layout helpers；
- Renderer contract；
- Screen controller base。

### P1：主导航

- Home；
- About；
- EconomySystem 主 Screen。

### P2：高频经济 UI

- Balance；
- Transfer；
- Shop；
- Market；
- Sales/Demand order；
- Delivery Box。

### P3：领地 UI

- Territory main；
- Manage Territory；
- Buff；
- Members / Player Action；
- Confirm screens；
- resize/modify related UI。

### P4：文件检查 / 系统类 UI

- Check consent；
- transfer consent；
- transfer result；
- terminal/result pages。

---

## 15. Root `src/main/java` 退出计划

迁移期间当前 NeoForge build 可以继续把根源码作为行为基线，但这必须是临时状态。

最终阶段：

1. 统计根 `src/main/java` 所有生产类。
2. 每个类明确归属：
   - common；
   - neoforge target；
   - 删除/废弃。
3. 不允许出现“暂时留根目录但 target 也有另一份”的长期双实现。
4. NeoForge `sourceSets.main` 最终移除根 `src/main/java`。
5. NeoForge 只编译：

   ```text
   common/src/main/java
   targets/neoforge-1.21.1/src/main/java
   ```

6. Forge 继续编译：

   ```text
   common/src/main/java
   targets/forge-1.20.1/src/main/java
   ```

7. 两端 source-set 结构对称。

该阶段完成后，common 才真正成为项目唯一实现基线。

---

## 16. 版本 Capability 模型

确实存在无法完全统一的 API 能力时，不允许 target 偷偷改变行为。

建议：

```java
public record PlatformCapabilities(
    boolean supportsFeatureX,
    boolean supportsFeatureY
) {}
```

但 capability 必须满足：

- 只描述真实 API 能力差异；
- 不描述“这个版本 UI 想做得简单一点”；
- common 决定 fallback 行为；
- fallback 必须经过明确测试；
- 用户可见差异必须写入兼容文档。

优先级：

```text
完全一致 > common 定义 fallback > 显式 unsupported > target 私自变更
```

---

## 17. 测试体系

多版本 common 化不能只靠“两个客户端看起来差不多”。

### 17.1 Common Controller Tests

每个 Controller 至少覆盖：

- initial state；
- loading；
- success；
- empty；
- error；
- retry；
- paging；
- scrolling；
- button enable；
- timeout；
- duplicate response；
- stale response；
- navigation。

### 17.2 Layout Contract Tests

给固定输入：

```text
320×180
640×360
854×480
1280×720
1920×1080
ultra narrow
ultra short
```

断言：

- 不越界；
- 控件不重叠；
- 可点击区域与绘制区域一致；
- 文字空间 >= 最低值；
- 页码/返回按钮始终可达；
- scale 稳定。

### 17.3 Theme Tests

断言所有 target 使用相同 token，而不是 target 自己 hardcode：

```text
color
padding
button height
card spacing
stripe width
animation duration
```

### 17.4 Renderer Contract Tests

Target renderer 可以用 recording backend 测试：

```text
common view 生成同一组 semantic render operations
Forge adapter 正确翻译
NeoForge adapter 正确翻译
```

### 17.5 Behavior Parity Tests

同一用户操作序列输入两个 target adapter，必须产生同一 common message / action。

例如：

```text
点击成员 kick
→ 两端都生成 RemoveTerritoryMemberMessage(same territoryId, same memberId)
```

### 17.6 Structural Tests

需要自动禁止架构回退：

- common 不导入 `net.minecraftforge.*`；
- common 不导入 `net.neoforged.*`；
- common UI Core 不继承 Minecraft Screen；
- target 不重复实现 common service；
- target 不出现 common theme token 的重复 hardcode；
- root 退出完成后，NeoForge build 不再编译根生产源码。

---

## 18. UI Parity 验收标准

一个 Screen 只有同时满足以下条件才算迁移完成。

### 行为一致

- 相同可用操作；
- 相同状态机；
- 相同网络消息；
- 相同错误；
- 相同 retry；
- 相同 timeout；
- 相同分页/滚动。

### 视觉一致

- 同一 virtual coordinate system；
- 同一布局；
- 同一 theme；
- 同一组件层级；
- 同一按钮尺寸；
- 同一卡片尺寸；
- 同一颜色 token；
- 同一 animation timing。

### 版本边界正确

Target Screen 只能包含：

- Minecraft widget 实例；
- GuiGraphics 调用；
- event method；
- target renderer；
- target navigation shell；
- platform adapter。

若 target Screen 中出现以下代码，应视为警告：

```text
业务价格计算
权限算法
分页数学重复
市场状态判断重复
领地事务
订单事务
独立 UI 颜色常量
独立 card spacing
独立 animation 参数
```

---

## 19. 迁移阶段

## Phase 0 — Inventory & Freeze

目标：建立完整迁移清单，暂不大规模改代码。

工作：

1. 枚举根 `src/main/java` 全部 Screen、UI component、业务 manager、service。
2. 枚举 Forge/NeoForge target 中同名/等价实现。
3. 建立矩阵：

```text
Class / Feature
1.21.1 implementation
1.20.1 implementation
common status
visual parity
logic parity
target API dependency
migration destination
```

4. 对每个 UI 标记：
   - SAME；
   - MINOR_DRIFT；
   - FORGE_TEMP_UI；
   - NEO_ONLY；
   - MISSING。
5. 冻结 1.21.1 视觉基准。

完成标准：没有未分类的 Screen。

## Phase 1 — Common UI Foundation

建立：

- geometry；
- theme token；
- layout helpers；
- animation core；
- controller base；
- renderer interface；
- navigation result；
- common UI tests。

不得先一次性改所有 Screen。

## Phase 2 — Territory Pilot

用领地管理页完成第一条完整链：

```text
old 1.21.1 UI
→ common state/controller/layout/theme
→ NeoForge shell
→ Forge shell
→ parity tests
```

只有试点达到验收标准，才能扩展 foundation API。

## Phase 3 — Shared Component Migration

迁移：

```text
CardRenderer
UiButtonRenderer
UiButtonStyle
animation
text validation
common list/pagination layout
```

要求旧 1.21.1 视觉不退化。

## Phase 4 — Screen Families

按模块一个个迁移：

```text
home
shop
market
delivery
territory
check
```

每个 family 单独提交和验收。

## Phase 5 — Business Root Extraction

审计根目录 remaining manager/service，把剩余稳定行为迁到 common application/domain。

Target 只留 API adapter。

## Phase 6 — NeoForge Root Detachment

从 NeoForge sourceSet 移除根 `src/main/java`。

这是架构迁移最重要的最终 Gate。

## Phase 7 — Future Version Readiness

用一个空的新 target skeleton 验证：

> 不复制任何业务 service 或 UI layout，仅实现 Platform Port 和 Screen shell，是否能够接入 EconomySystem？

若答案是否定，说明 common 边界仍不完整。

---

## 20. 提交策略

该架构禁止“一次提交重写整个 UI”。

推荐提交粒度：

```text
add common ui geometry primitives
add common ui theme model
add common ui renderer contract
extract territory manage controller
extract territory manage layout
adapt neoforge territory manage ui
adapt forge territory manage ui
migrate common card styles
...
```

规则：

1. 一个提交只有一个可解释架构目标。
2. 每个迁移提交必须能编译两个 target。
3. 每个 Screen 迁移必须先保持 1.21.1 基准，再做 Forge parity。
4. 不允许删除原 1.21.1 实现后才开始猜测它的行为。
5. 原实现只在 parity 已证明后删除。
6. 不用 `git add .` / `git add -A` 混入无关文件。
7. 不使用 force push。

---

## 21. 风险与控制

### 风险 A：过度抽象

表现：为了“理论纯净”创建大量只有一个实现的 interface。

控制：

- 只在版本差异真实存在的位置建 Port；
- 纯稳定 Java 行为直接 common concrete class；
- 不为每个 getter 创建 adapter。

### 风险 B：Common 变成最低版本 API

表现：为了 1.20.1，把 1.21.1 的功能全部降级。

控制：

- 1.21.1 是迁移视觉/行为基准；
- fallback 由 common 显式定义；
- target 不允许私自删功能。

### 风险 C：Renderer Interface 过于底层

表现：common 只是把每个 `fill()` 包了一层，真正布局仍在 target。

控制：

- layout、component model、theme 必须 common；
- renderer 只负责最终 draw translation。

### 风险 D：Target 偷偷拥有状态

表现：Forge 与 NeoForge Screen 各自维护分页/selection/loading。

控制：

- interaction state 优先进入 Controller；
- target 只保留 Minecraft widget 生命周期状态。

### 风险 E：迁移过程中 UI 回归

控制：

- 先冻结 1.21.1 baseline；
- 每个 Screen 先完成 NeoForge common 重表达；
- 再适配 Forge；
- 再删除旧实现。

### 风险 F：Java 版本差异

Forge 1.20.1 当前使用 Java 17，NeoForge 1.21.1 使用 Java 21。

因此 common 生产源码必须以 **Java 17 可编译语法/API** 为最低兼容基线，除非构建架构未来明确把 common 编译为独立 artifact 并提供其他兼容策略。

禁止 common 无意使用仅 Java 21 提供的语言/API 特性。

---

## 22. 新增 Minecraft 版本的标准流程

未来例如新增 `neoforge-1.22.x`：

1. 创建 target sourceSet。
2. 实现注册表 adapter。
3. 实现 network registrar / codec binding。
4. 实现 persistence adapter。
5. 实现 item snapshot bridge。
6. 实现 player/inventory/world Port。
7. 实现 `EconomyUiRenderer`。
8. 实现薄 Screen shell / ScreenFactory。
9. 注册事件与快捷键。
10. 跑 common contract tests。
11. 跑 target-specific compatibility tests。
12. 跑 UI parity contract。

不应执行：

```text
复制整个 1.21.1 src
全局替换 API
逐个修编译错误
保留第三份业务/UI 实现
```

衡量架构成功的一个核心指标是：

> 新版本适配工作的绝大多数 diff 应集中在 `targets/<version>` 的 API Adapter，而不是复制 common service 或 Screen layout。

---

## 23. Definition of Done

整个多版本 Common 架构计划完成，必须同时满足：

### 源码结构

- `common` 是业务逻辑唯一 Source of Truth；
- `common` 是 UI layout/theme/controller 唯一 Source of Truth；
- Forge 与 NeoForge 不维护独立业务算法；
- Forge 与 NeoForge 不维护独立 UI 设计；
- NeoForge build 不再依赖根 `src/main/java` 行为基线；
- target source set 结构对称。

### UI

- 1.20.1 与 1.21.1 同功能页面视觉设计一致；
- layout 参数来自 common；
- theme 参数来自 common；
- interaction state 来自 common controller；
- target 只做 renderer/widget/API translation。

### 逻辑

- 服务端权威规则 common 化；
- 事务 common 化；
- target persistence/network/player API 均通过 Port；
- 业务行为 parity tests 通过。

### 构建

必须继续通过：

```powershell
.\gradlew.bat :targets:forge-1.20.1:test --no-daemon --rerun-tasks
.\gradlew.bat :targets:neoforge-1.21.1:test --no-daemon --rerun-tasks
.\gradlew.bat buildAllTargets --no-daemon --rerun-tasks
```

不得依赖 `:common:test` 作为最终目标验收。

### 架构 Gate

- common 不导入 loader 包；
- UI Core 不继承 Minecraft Screen；
- target 不存在明显业务复制；
- target 不存在明显 theme/layout 复制；
- 新 target 可以仅靠实现 adapter 接入。

---

## 24. 推荐立即执行的下一阶段

### Step 1：UI / Logic Inventory

先只盘点，不重构。

生成：

```text
plans/multiversion-ui-inventory.md
```

至少记录：

- 根 1.21.1 Screen；
- Forge target Screen；
- NeoForge target Screen；
- common controller/service；
- 使用的 renderer/component；
- 版本 API 依赖；
- 视觉差异；
- 业务差异；
- 推荐迁移目的地。

### Step 2：设计最小 Common UI Foundation

只先创建领地试点需要的：

```text
UiRect
UiScale
UiInsets
EconomyUiTheme
UiCardStyle
UiButtonStyle
TerritoryManageState
TerritoryManageController
TerritoryManageLayout
EconomyUiRenderer
```

不要一开始设计一个过于庞大的通用 GUI framework。

### Step 3：NeoForge 领地管理重表达

要求效果与当前 1.21.1 基准一致。

### Step 4：Forge 领地管理 parity

Forge 使用同一个 common UI Core，不再使用独立简化设计。

### Step 5：从试点结果反推 Foundation

只把实际证明可复用的部分提升为 shared component。

---

## 25. 最终开发规则摘要

今后 EconomySystem 多版本开发统一遵守：

1. **先改 common 语义，再适配 target API。**
2. **1.21.1 当前成熟实现是迁移参考，不是永久第二套实现。**
3. **迁移完成后 common 是唯一业务与 UI 真源。**
4. **Target 不允许重新设计同功能 UI。**
5. **Target 不允许重新实现同一业务事务。**
6. **Common Controller 不继承 Minecraft Screen。**
7. **Layout / Theme / State / Animation 优先 common。**
8. **GuiGraphics / Widget / Registry / Event / SavedData API 留 target。**
9. **旧版本不能成为全项目功能上限。**
10. **真实 API 差异通过显式 capability/fallback 表达。**
11. **新增版本的主要工作必须是 adapter，而不是复制代码。**
12. **任何跨版本差异都必须可以解释为 API 差异，而不能只是两份代码逐渐漂移。**

该规则应作为后续 UI、业务模块和新增 Minecraft 版本适配的长期架构约束。