# Forge 1.20.1 Target Status

This target now validates the Gradle/Java 17/Forge toolchain, shared platform
services, the Forge SavedData shells, and seven end-to-end protocol slices:

- balance request/response (`0/1`);
- balance-history request/response (`2/3`);
- atomic online-player transfer (`4`);
- system-shop catalog request/response (`5/6`);
- server-authoritative system-shop purchase (`7`);
- server-authoritative sales-order creation (`8`);
- server-authoritative demand-order creation (`9`);
- server player-list request/response (`34/35`).

The Forge shop adapter reads the authoritative `economy_shop.json` schema and
converts supported 1.21 components to their 1.20 NBT equivalents. Unknown
components and items unavailable in 1.20 are rejected rather than delivered
with data loss. The Forge client handlers publish
immutable common cache snapshots; the root
NeoForge UI tree is intentionally not copied into this target. This is still
not a feature-complete release and must not be distributed as the final
EconomySystem Forge build.

Gameplay is ported feature by feature from the NeoForge 1.21.1 behavior
baseline. Code from the historical `1.20.1` branch may be consulted for API
shape only; obsolete handlers and removed server systems are not restored.

Messages without an implemented Forge codec fail explicitly instead of being
silently dropped. In particular, legacy `check/get/chunk` messages (`23-30`)
are intentionally excluded pending a security redesign.

## ItemStack snapshot bridge

Stage A is complete for Forge 1.20.1. The target reads and writes snapshot
schema v1 using native 1.20.1 item NBT for JSON names/lore, enchantment IDs and
levels, damage, repair cost, unbreakable, dyed color, custom model data, and
custom data. Missing 1.21.1 items and non-equivalent data are rejected.

Known fail-closed limits include attribute modifiers, adventure predicates,
entity/block-entity item data, Forge capabilities, unknown display fields, and
different tooltip visibility for normal versus stored enchantments (1.20.1 has
one shared hide flag). The old `{id,count,customData}` compact format remains a
read/legacy-API compatibility path only.

Protocol `8` is registered as `PLAY_TO_SERVER` with strict decoding. The server
owns item identity, snapshot data, tax, seller, order ID, and timestamps. New
market writes use stable `sales_order` / `demand_order` IDs, a count-one item
template, separate quantity, and exact `expirationTime`. Legacy Java class names
remain readable; unsupported legacy 1.20.1 item data aborts conversion instead
of being silently dropped or overwritten. Forge still has no copied full market
UI. Protocols `9` through `13` and protocol `15` are migrated; protocol `14` is next.

The hardened bridge applies the common snapshot limits at creation, schema
decode/encode, capture, and restore. Nonzero `Damage` on an item with no
durability now fails during capture instead of producing a snapshot that later
fails restoration. A shared schema-v1 golden fixture covers native names,
lore, normal/stored enchantments, tooltip flags, damage, repair cost,
unbreakable, dye, model data, and custom data.

Protocol `8` transaction handling now restores the complete pre-operation inventory
when a multi-stack removal is partial or throws. Repository failure triggers independent
tax and item compensation; either compensation failing produces `ROLLBACK_FAILED` and
an error log. Forge sends a translated success or failure message for every result and
no longer silently discards the service outcome. The compatibility `MarketManager` does
not retain or write back a stale list.

Demand-order delivery persistence now has a common atomic ledger transition and
transaction service so detached compatibility views cannot lose the delivered flag or
permit duplicate payment. This hardens the existing NeoForge protocol `14` path only;
protocol `14` has not been migrated or registered on Forge 1.20.1.

Protocol `9` accepts only `itemId`, `quantity`, and the whole-order `totalPrice`.
Forge resolves the default registered item, captures a count-one schema-v1 template,
rejects air and unsupported data, and enforces the native maximum stack size. The server
generates identity and timestamps, freezes `totalPrice` once without listing tax, and
refunds it if the atomic ledger add fails. Forge registers discriminator `9` exactly once;
later market discriminators remain unregistered.

Protocols 12 through 19 are migrated on both targets. Territory request 17 carries only
the request ID and response 18 uses explicit, bounded snapshots instead of Territory NBT.
Forge captures its existing `territory_data` persistence into full owned snapshots and
minimal authorized summaries; its codec enforces the shared 1 MiB budgets. Protocol 19 is the
UUID-only teleport transaction described below. Protocol 20 uses the common 32-byte territory/target UUID invite request and a server-scoped expiring store. Forge invite acceptance performs a guarded raw-NBT `AuthorizedPlayers` mutation while retaining unknown fields. Protocols 17-21 are migrated; protocol 22 and later remain legacy.
Protocol-20 finalization uses token claims, raw-NBT exact request lookup, canonical bilingual keys, and an owned-row invite button. Player-list revision drives fresh loading/empty state and a 15-tick debounce blocks duplicate sends. Malformed or duplicate raw territory records map to CREATE_FAILED. Forge Chinese non-invite translations were restored from the pre-protocol-20 baseline. The final regression contains 252 shared-source tests, 315 Forge tests, and 305 NeoForge tests.
Protocol 14 is hardened with real failure reporting, expected-order delivery, exact supplier credit,
independent payment/inventory compensation, display-name fallback, and shared inventory contract tests.
Protocol 16 carries only tradeId; server-authoritative expected-order removal and exact owner refund use
compensation, and CHANGED/UNKNOWN failures invalidate clients. Removal/outcome construction invariants,
repository-contract recovery, combined failure logging and real post-plan success semantics are hardened.
The final hardened protocol-19 suite contains 228 shared-source tests, 280 Forge tests, and 274 NeoForge tests.

Market payments and compensation for protocols `8/9` now use the common exact balance
API. Overflow and persistence failure leave balance and logs unchanged. The protocol `9`
client send route is covered by fail-closed dispatch testing. Legacy saturating calls in
unmigrated features remain out of scope; Forge discriminator `16` remains unregistered.

Protocols `10/11` now use the common summary/page market model and strict schema-v1
snapshot codec. Forge can send requests, serve authenticated filtered pages, receive
responses into `ClientMarketState`, and broadcast bounded invalidations. Pages contain at
up to exactly 9 validated orders and a 768 KiB estimated payload budget; no legacy full-market
`MarketItem` NBT is transmitted. Market revision is persisted as `marketRevision` and
is sourced atomically with every response and invalidation.

Protocol `12` is a real Forge C2S UUID-only message using the shared purchase transaction.
It performs authoritative order/Snapshot validation, main-inventory capacity and atomic
insertion, exact two-account payment, recoverable order removal, and independent rollback.
A full inventory rejects the purchase without dropping items. Discriminators `13` and `15`
are also registered common bindings; protocols `14` and `16` remain legacy and protocol `14` is next.
# Sales-order removal bridge

Forge 1.20.1 now registers the common UUID-only sales-order removal message and routes it through the
same common transaction service as NeoForge 1.21.1. Its inventory adapter touches only main inventory,
fills compatible stacks before empty slots, restores all captured slots on failure, and never drops items.
The canonical wire discriminator remains 15; protocol 13 is registered, while protocols 14 and 16 remain legacy.

Demand-order confirmation is now registered at canonical discriminator 13. Purchase, confirmation and sales
removal share `Forge1201TransactionalInventoryAdapter`; it operates only on main inventory and restores every
slot independently on rollback. Protocols 14 and 16 remain on their legacy path and are not migrated here.

## Territory data hardening

Forge protocols 17/18 use the common DATA/ERROR response model and the single shared wire codec. Because the
NeoForge root UI is intentionally excluded from this target's source set, Forge supplies its own read-only
`Screen_Territory` under the same target-specific FQCN and opens it with the existing `I` key. The physical-side
dispatch layer keeps `Minecraft` and screen classes out of the dedicated-server handler linkage surface.
Responses apply only to the current page request ID and commit after complete restore; ERROR, restore failure,
or the 200-tick timeout exposes retry without clearing old page data.

`Forge1201TerritorySnapshotStore` is the sole Forge `territory_data` SavedData type. It provides overworld-normalized
reads for protocols 17-19 and guarded raw-NBT writes for protocol 20 authorization and protocol 21 deletion.
Copy-on-write mutations retain unknown fields and list order. It no longer clamps invalid buff levels or upgrade
steps, unknown/missing permissions retain the historical `MEMBERS` fallback, and non-canonical dimension IDs
fail closed.

Protocol 19 is registered at canonical discriminator 19 as a UUID-only C2S message. Forge registers the missing
`economy_system:recall_potion` as a stack-size-one, fire-resistant item and supplies its model, texture and names.
The target-local territory page provides teleport for every row and invite/delete actions for owned rows; delete
uses a separate two-step confirmation page. All authority remains server-side: the shared service re-reads the store, checks current
owner/member access, validates the exact landing point, reserves one main-inventory potion, verifies arrival and
commits it on success. `Forge1201RecallPotionInventory` scans only `inventory.items` and delegates removal to the
common reserve factory, so dirty-mark failure is compensated before a reservation is returned. Required
`setChanged` and best-effort `broadcastChanges` are separate. Conflict-safe rollback restores exactly
one potion without overwriting a changed slot; it scans mergeable stacks then empty slots and returns
ROLLBACK_FAILED if no safe destination exists. Secondary rollback errors are suppressed onto the primary error.
The page uses non-overlapping 26-pixel rows, render-owned UUID hitboxes and wheel scrolling so every filtered row
is reachable, including zero visible rows in tiny windows. Arrival UNKNOWN keeps the removed potion and reports
state uncertainty; the limiter is weakly scoped to each MinecraftServer and resets on tick epoch rollback.
Full player AABB world-border/collision checks precede teleport. Only an expiring POST_TELEPORT
ticket is used. Protocol 20 is complete. Protocol 21 removes a territory from the authoritative raw
`territory_data` NBT with strict copy-on-write validation, preserving unknown fields and record order; dirty-mark
failure rolls back before reporting retry-safe failure. The wire is only the 16-byte UUID and the server sender
is the owner authority. Protocol 22+ remains legacy.
