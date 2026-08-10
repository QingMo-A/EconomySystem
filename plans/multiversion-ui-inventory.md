# Multiversion UI / Logic Inventory

Status: implementation inventory, 2026-08-10. NeoForge 1.21.1 remains the
behavior and visual reference, but active production code no longer compiles
the root `src/main/java` tree. Both targets compile `common/src/main/java` plus
their own Minecraft/loader adapters.

## Active UI Matrix

| Feature family | Common source of truth | Forge 1.20.1 shell | NeoForge 1.21.1 shell | Target-only API boundary | Status |
|---|---|---|---|---|---|
| Home navigation | `ui/home` and `common/client/ui` | `Forge1201HomeScreen` | `NeoForge1211HomeScreen` | Screen lifecycle, key/mouse events | pixel/reference parity accepted |
| About | `ui/about` | `Forge1201AboutScreen` | `NeoForge1211AboutScreen` | texture and clipboard translation | pixel/reference parity accepted |
| Balance log | `ui/balance` | `Forge1201BalanceLogScreen` | `NeoForge1211BalanceLogScreen` | EditBox, network send, drawing | pixel/reference parity accepted |
| Shop catalog | `ui/shop/Shop*` | `Forge1201ShopScreen` | `NeoForge1211ShopScreen` | item rendering and widgets | FORENSIC_REFERENCE_VERIFIED (`c697c0af`, `ShopLegacyReferenceParityTest`) |
| Shop purchase | `ui/shop/ShopPurchase*` | `Forge1201ShopPurchaseScreen` | `NeoForge1211ShopPurchaseScreen` | EditBox and network send | FORENSIC_REFERENCE_VERIFIED (`c697c0af`, `ShopLegacyReferenceParityTest`) |
| Market list | `ui/market/Market*` | `Forge1201MarketScreen` | `NeoForge1211MarketScreen` | item rendering, EditBox, network send | FORENSIC_REFERENCE_VERIFIED (`bd5a6db2`, `MarketLegacyReferenceParityTest`) |
| Market create / confirm | `ui/market/MarketCreate*`, `MarketConfirm*` | Forge create/confirm shells | NeoForge create/confirm shells | inventory snapshot, registry lookup, widgets | FORENSIC_REFERENCE_VERIFIED (`bd5a6db2`, `MarketLegacyReferenceParityTest`) |
| Delivery box | `ui/delivery` | `Forge1201DeliveryBoxScreen` | `NeoForge1211DeliveryBoxScreen` | item rendering and network send | pixel/reference parity accepted |
| Territory list | `ui/territory/list` | `Forge1201TerritoryListScreen` | `NeoForge1211TerritoryListScreen` | EditBox and navigation shell | pixel/reference parity accepted |
| Territory management | `ui/territory` | `Forge1201TerritoryManageScreen` | `NeoForge1211TerritoryManageScreen` | player head/item rendering and network send | pixel/reference parity accepted |
| Territory detail / access / rules | `ui/territory/detail` | `Forge1201TerritoryDetailScreen` | `NeoForge1211TerritoryDetailScreen` | widgets and network send | pixel/reference parity accepted |
| Territory buffs | `ui/territory/buff` | `Forge1201BuffManageScreen` | `NeoForge1211BuffManageScreen` | item rendering and network send | pixel/reference parity accepted |
| Territory invite | `ui/territory/invite` | `Forge1201TerritoryInviteScreen` | `NeoForge1211TerritoryInviteScreen` | player list request and network send | pixel/reference parity accepted |
| Territory delete / member removal | `ui/territory/confirm` | `Forge1201TerritoryConfirmationScreen` | `NeoForge1211TerritoryConfirmationScreen` | destructive request send | pixel/reference parity accepted; one-shot decision |
| Client file check | `ui/check` | `Forge1201ClientFileCheckScreens` | NeoForge consent/result shells | disk scan lifecycle and Screen API | pixel/reference parity accepted; consent and local scan safety preserved |
| Checked file transfer | `ui/transfer` | Forge consent/result shells | NeoForge consent/result shells | filesystem handles, save dialog behavior, Screen API | pixel/reference parity accepted; explicit consent and target-only save APIs preserved |

Every active `*Screen.java` under the two target adapter packages is covered by
an architecture gate requiring a common UI contract. The legacy
`com/mo/economy_system/screen/**` trees remain reference material only and are
excluded from both production and test source sets.

## Shared UI Foundation

| Concern | Source of truth | Target responsibility |
|---|---|---|
| Geometry and scaling | `ui/geometry`, 640x360 base canvas | apply physical scale |
| Theme, cards and buttons | `ui/theme` | translate semantic style to draw calls |
| State and navigation | common controller/state/event records | forward lifecycle/input events |
| Paging, filtering and scrolling | common controllers/layouts | report viewport and wheel direction |
| Semantic rendering | `EconomyUiRenderer` and common views | draw text, item, player head, texture and tooltip |
| Text entry | common validation/state | own Minecraft `EditBox` lifecycle |

Controller tests cover initial/loading/ready/empty/error, retry, timeout, stale
and duplicate responses, action enablement, paging/scrolling and navigation.
Layout tests cover the documented normal, narrow and short viewports. Recording
renderer tests prove that both target backends receive the same semantic view.

## Business Boundary

| Domain | Common authority | Target adapter retained |
|---|---|---|
| Accounts / transfer | ledger and transaction services | SavedData/NBT and player lookup |
| Shop | purchase transaction, dynamic pricing policy and refresh schedule | JSON/config I/O and native ItemStack creation |
| Market | order model, ledger, query and mutation transactions | SavedData/NBT and inventory/player adapters |
| Delivery | ledger, query and claim transaction | SavedData/NBT and native inventory adapter |
| Territory | snapshots, geometry, pricing, resize/removal/admin/buff transactions | SavedData, native territory model and spatial index |
| File check / transfer | validation, authorization and lifecycle state machines | secure filesystem and loader network adapters |
| Red packet | packet state, lucky/even allocation, claim/cancel/expiry transactions and compensation | SavedData/NBT, account ledger and chat/command translation |
| Mob reward | reward catalog, random calculation, enchantment bonuses and exact credit policy | entity/enchantment registry lookup, RNG source, account ledger and death event |
| TPA | request store, TTL, potion reservation transaction and fail-closed teleport outcomes | player/inventory/chunk/teleport/effect APIs and command translation |
| Starter kit | exactly-once claim, marker/account compensation and outcome policy | persistent player marker, account ledger, clone/login events and command translation |
| Update check | SemVer parsing/comparison, release JSON validation and result policy | HTTP executor, server-thread dispatch and clickable chat components |

Global audit (2026-08-10): both target active-screen inventories contain 19
Screen classes and pass the common semantic View/Controller/Layout architecture
gate. Shared GUI textures and language resources are packaged from `common`
only; Forge and NeoForge JARs contain no opposite-loader classes or target
source paths. Target visual-authority scans contain no page layout/theme
constants, and semantic icon fallback scans are clean (pagination arrows are
intentional text controls). Production target layout hitboxes use real target
font metrics where text width affects geometry; the remaining approximate
metric helper is confined to loader-neutral defaults/tests. Renderer contracts
remain mandatory with explicit Forge/NeoForge implementations.

Target-local classes named `Manager`, `SavedData` or `Store` are not by
themselves evidence of duplicated policy. They are permitted only when they
translate Minecraft persistence, registry, player, inventory, filesystem or
index APIs into a common service port. Architecture gates pin the extracted
resize, geometry, pricing, shop-pricing and UI contracts.

The gameplay and lifecycle entries that were previously outside the Bridge/UI
scope are now included in the parity boundary. `common/redpacket`,
`common/reward`, `common/tpa`, `common/starter` and `common/update` own their
version-independent state machines, policies and transaction outcomes. Forge
and NeoForge retain only loader/API adapters and use the same common services.
The old root `src/main/java` implementations remain excluded reference material;
they are not compiled by either target and are not an alternative source of
behavior.
