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
UI. Protocol `9` is the next migration slice.

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

The current full target suite contains 141 passing tests. The paired NeoForge
1.21.1 target contains 142 passing tests; `buildAllTargets --rerun-tasks` passes.
Protocols `8` through `11` are closed. Protocol `12` is the next migration slice.

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
A full inventory rejects the purchase without dropping items. Forge discriminators `13-16`
remain unregistered; protocol `13` is next.
# Sales-order removal bridge

Forge 1.20.1 now registers the common UUID-only sales-order removal message and routes it through the
same common transaction service as NeoForge 1.21.1. Its inventory adapter touches only main inventory,
fills compatible stacks before empty slots, restores all captured slots on failure, and never drops items.
The canonical wire discriminator remains 15; protocols 13, 14 and 16 are not registered by this bridge work.
