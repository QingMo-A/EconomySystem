# Multiversion UI / Logic Inventory

Baseline: NeoForge 1.21.1 root `src/main/java` implementation. Forge 1.20.1
must consume the same common behavior and visual contracts and only translate
Minecraft/loader APIs.

## Inventory Matrix

| Feature / screen | NeoForge 1.21.1 | Forge 1.20.1 | Common status | Logic / visual source | Target API dependency | Current drift | Destination | Stage |
|---|---|---|---|---|---|---|---|---|
| Home navigation | `src/.../screen/Screen_Home` | target-local `screen/Screen_Home` | route model exists | NeoForge visual baseline | Screen, GuiGraphics, key events | Forge has explicit unavailable routes | common route/menu + target shells | foundation complete |
| Territory list | `screen/territory_system/Screen_Territory` | target-local `Screen_Territory` | data applier/request IDs exist | NeoForge list/paging baseline | Screen, EditBox, rendering | layout and state duplicated | common territory list controller/layout | inventory |
| Territory manage | `screen/territory_system/Screen_ManageTerritory` | target-local `Screen_ManageTerritory` | `common/ui/territory` state/controller/layout/view complete | NeoForge cards/actions baseline | Screen, EditBox, player-head/item rendering | nested pages remain explicit fallback | `common/ui/territory` + two shells | pilot complete; nested family next |
| Territory buffs | `Screen_TerritoryBuff` | no equivalent full page | messages/services common | NeoForge baseline | Screen, widgets, item rendering | Forge missing | common controller + target shells | later |
| Territory player actions | `Screen_TerritoryPlayerAction` | partial target pages | messages common | NeoForge baseline | Screen, text input | Forge incomplete | common action state + target shells | later |
| Territory confirmations/invites | confirm/invite screens | target-local equivalents | common transactions/messages | NeoForge flow baseline | Screen, widgets | layout drift | common navigation/state + target shells | later |
| Shop | `screen/economy_system/shop/*` | no full equivalent | shop data/network common | NeoForge visual baseline | Screen, item rendering | Forge unavailable | common shop state/view + target shells | later |
| Market/orders | `screen/economy_system/market/*` | no full equivalent | market protocols common | NeoForge visual baseline | Screen, item rendering, text input | Forge unavailable | common market controllers/views | later |
| Delivery box | root delivery screen | target-local delivery screen | common snapshots/protocols | NeoForge baseline | Screen, item rendering | target implementation differs | common delivery state/layout | later |
| Balance log | root balance-log screen | unavailable | common ledger/query exists | NeoForge baseline | Screen, chart/list rendering | Forge missing | common balance-log view | later |
| About | root about screen | unavailable | route exists | NeoForge visual baseline | Screen, texture/clipboard APIs | Forge missing | common about model + target renderer | later |
| File check / transfer | target-specific NeoForge screens | target-specific Forge screens | lifecycle/network common | target consent shell | disk/UI APIs | separate shell state | common controller/view state | later |

## Shared Components

| Component | Current location | Common destination | Decision |
|---|---|---|---|
| Card geometry/style | root `CardRenderer` | `common/ui/theme` and `common/ui/geometry` | move data semantics, keep draw calls target-local |
| Button style | root `UiButtonStyle` | `common/ui/theme` | immutable token model; no GuiGraphics |
| Button drawing | root `UiButtonRenderer` | `EconomyUiRenderer` | semantic renderer contract |
| Animation | root `UiAnimation` | `common/ui/animation` | common timing/easing; target supplies frame clock |
| Paging/scroll bounds | duplicated in screens | controller/layout | common state and layout tests |
| Text input | Minecraft EditBox wrappers | controller state/validation | target owns widget lifecycle |

## Pilot Acceptance

The territory-management pilot is complete only when both targets use the same
`TerritoryManageState`, controller, layout, theme tokens and action semantics;
only the Screen shell and renderer differ. Shop, market, delivery, file-check,
root-source detachment and future-version skeletons remain outside this pilot.

## Pilot result

The territory-management entry pilot is complete. Both target shells consume
the same immutable member state, request-ID state machine, viewport-aware
640x360 layout, theme tokens, semantic renderer contract, and stable action
IDs. Common tests cover initial/loading/success/empty/error, timeout/retry,
stale and duplicate responses, filtering, paging and wheel-scroll bounds,
viewport bounds, hitbox containment, non-overlap, and recording-renderer parity.

Nested buffs, access/rules, transfer, invite, and delete pages remain an
explicit documented target fallback. They are the next territory-family input;
shop, market, delivery, file-check UI, root-source detachment, and future target
skeletons remain outside this checkpoint.
