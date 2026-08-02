# EconomySystem Shared Sources

`common` contains behavior and data semantics shared by every supported
Minecraft target. It is a shared source directory, not an independently
published Gradle module; each target recompiles it against that target's
Minecraft API.

NeoForge 1.21.1 is the behavior baseline. Forge 1.20.1 adapters must implement
the same behavior and must not restore or copy obsolete business logic from the
old `1.20.1` branch.

Loader APIs, version-specific codecs, registration, events, mixins, metadata,
Data Components, attachments, and NBT persistence implementations belong in
`targets/<loader-version>`.

`protocol/EconomyProtocol` is the canonical NeoForge 1.21.1 message manifest.
Its IDs, directions, and Forge discriminators are append-only wire contracts.
Target codecs and loader payload wrappers bind to that manifest rather than
maintaining independent registration order.

## Current migration status

- Protocol `bridge-1` locks all 44 NeoForge 1.21.1 message IDs, directions,
  and Forge discriminators (`0` through `43`).
- Common messages currently cover balance (`0/1`), balance history (`2/3`),
  transfers (`4`), system-shop catalog synchronization and purchasing (`5-7`),
  sales-order creation (`8`), demand-order creation (`9`), and server player lists (`34/35`). Both targets have real codecs and handlers for these
  messages.
- Transfers are atomic: invalid, self, insufficient-funds, offline-target, and
  recipient-overflow attempts change neither account. Online recipients are
  resolved from the server player list so transfers work across dimensions.
- `EconomyLedger` owns shared account, offline-message, and balance-history
  behavior. Each target retains its own same-FQN `EconomySavedData` persistence
  shell so the 1.20.1 and 1.21.1 SavedData APIs never leak into common logic.
- `check/get/chunk` messages (`23` through `30`) remain Neo-only legacy code.
  They must not be ported until path validation, request ownership, chunk
  limits, timeouts, and per-session accumulation are redesigned.
- Shop purchasing is server authoritative and rolls inventory back before a
  refund when delivery fails. Price-stat persistence happens after delivery
  and cannot produce a refund plus retained items.
- Item snapshot schema v1 is complete. `ItemStackSnapshot` is immutable and
  loader-neutral; its strict codec writes `schemaVersion`, `id`, `count`, and
  `components`. Supported components are custom name, lore, normal and stored
  enchantments, damage, repair cost, unbreakable, dyed color, custom model
  data, and custom data. Text uses stable JSON and enchantments use registry
  IDs. All collections and NBT values are defensively copied.
- Snapshot reads fail closed on unknown schema versions, component names, bad
  types, invalid counts, missing target items, unsupported target data, or a
  conversion that cannot be lossless. NeoForge rejects every non-default data
  component outside the v1 allow-list. Forge rejects native fields such as
  attribute modifiers, can-place/can-destroy predicates, entity/block-entity
  data, capabilities, unknown display fields, and tooltip states that 1.20.1
  cannot represent independently.
- Legacy compact `{id,count,customData}` tags remain read-compatible only.
  `saveSimple/loadSimple` remain deprecated compatibility entry points; every
  new snapshot write uses schema v1. Stage A is closed; protocol `8` consumes
  this schema for sales-order creation.
- Snapshot validation is centralized in `ItemStackSnapshotValidator`; creation
  and strict encoding return explicit results. Limits are: item/enchantment ID
  256 characters, name and each lore line 8,192 characters, 64 lore lines,
  32,768 total lore characters, 64 normal and 64 stored enchantments, 32,767
  estimated custom-data bytes at 16 levels, and 65,536 estimated encoded bytes.
  Limit failures use `DATA_LIMIT_EXCEEDED` and never truncate data.
- Both targets reject nonzero damage on items with no durability, so capture
  and restore now apply the same rule. One shared schema-v1 golden fixture is
  restored and recaptured by both targets. The complete verified suite runs 160
  Forge 1.20.1 tests and 161 NeoForge 1.21.1 tests with no failures.
- Protocol `8` carries only `slot`, `quantity`, and `totalPrice`. The server rereads
  inventory state, stores a count-one template, counts matching stacks with `long`,
  charges `(totalPrice + 9L) / 10L`, and compensates inventory/tax changes if the
  ledger write fails. Quantity remains separate and `totalPrice` is never multiplied.
- New market writes use `sales_order` / `demand_order`, schema-v1 `itemStack`,
  `listedCount`, `basePrice`, seller fields, and exact listing/expiration times.
  Old Java class type names remain readable. Unsupported legacy Forge stack data
  fails conversion before it can be overwritten.
- Protocol `8` transaction hardening is complete. A partial or exceptional multi-stack
  removal restores every inventory slot before returning failure. Repository add is
  explicitly atomic: `false` or an exception may not leave the proposed order behind.
  Tax refund and inventory restoration are attempted independently, so one compensation
  exception never skips the other; any incomplete compensation reports `ROLLBACK_FAILED`.
- Both loaders map every result to stable, user-safe translation keys. Internal failures
  and compensation outcomes are logged with player/trade/stage context. The legacy
  `MarketManager` facade no longer caches or writes back a market view; the bound
  `MarketSavedData`/`MarketLedger` remains the sole authority.
- Legacy NeoForge demand delivery (protocol `14`) now treats facade order objects as
  detached, read-only compatibility views and persists delivery through an explicit,
  atomic `MarketLedger` transition. Validation, item removal, payment, persistence,
  and independent compensation are shared in common code. A repeated delivery cannot
  pay the supplier twice. This is a compatibility safety fix, not protocol `14`
  migration; Forge still has no discriminator `14` registration.
- Protocol `9` carries only `itemId`, `quantity`, and `totalPrice`. The server resolves
  the registered default item, rejects air/unknown/unsupported items, captures a count-one
  schema-v1 template, and limits quantity to that item's maximum stack size. `totalPrice`
  is the one-time frozen amount and is never multiplied by quantity; demand listings have
  no sales tax. UUID, owner, timestamps, expiration, and initial `delivered=false` are
  server-owned. Repository failure refunds the complete frozen amount, with refund failure
  reported explicitly.
- Protocols 12, 13, 14 and 15 are migrated; protocol 16 remains legacy and is next.

## Exact market balance transactions

Legacy `addBalance` keeps its saturating behavior for unmigrated features. Transactions
requiring complete payment or compensation use `creditExact`, `debitExact`, and
`canCreditExact`: overflow fails with `BALANCE_LIMIT`, while dirty failure restores the
affected account and log. Protocols `8/9`, legacy demand delivery, and legacy demand
cancellation now use exact operations. Cancellation removes through a rollback handle
before refunding the original owner; delivery never removes items when full payment
cannot fit. Forge protocol `9` has a client send route. Discriminator `16` remains
unregistered. Protocols `10/11/12/13/15` are migrated; protocol `14` is next.

## Market data protocols 10/11

Market reads now use bounded common messages. Protocol `10` distinguishes `SUMMARY`
from server-filtered `PAGE` requests; pages are fixed at 9 orders and queries to 64
characters. `MINE` always uses the authenticated sender UUID. Protocol `11` carries only
schema-v1 item snapshots and immutable order fields, never dynamic `MarketItem` NBT.
`ClientMarketState` atomically stores summary/page data, ignores older request IDs, and
marks pages stale on lightweight `INVALIDATED` broadcasts without clearing them.
Every response carries the revision and is limited by a conservative 768 KiB estimated
payload budget. PAGE and SUMMARY have independent request IDs; invalidation revisions
prevent older responses from replacing newer statistics. NeoForge restores every item
snapshot before atomically publishing a page. The initial migration was `666fccc`.

Protocol `10/11` is closed. Both decoders reject raw payloads above 768 KiB before
reading order or Snapshot NBT data. Persistence loading and runtime replacement now use
separate `loadFromPersistence` and `replaceAll` ledger APIs.

## Sales-order purchase protocol 12

Protocol `12` sends only the order UUID. The server resolves the authoritative sales
order, validates/restores its schema-v1 Snapshot, checks exact buyer-to-seller payment and
main-inventory capacity, then uses transactional order removal and inventory insertion.
Insufficient space rejects the purchase without ground drops. Payment failure independently
rolls back inventory and restores the order at its original index. Notifications and market
invalidation occur only after commit.
# Bridge migration status (protocols 12 and sales-order removal)

Sales-order purchase and removal now share loader-neutral transactional inventory ports and the single
`MarketLedger.removeSalesTransactional` implementation. Removal requests carry only the authoritative
trade UUID; the server revalidates ownership/operator permission, snapshot, receiver identity and full
main-inventory capacity. Items are returned only to the original online seller. An operator request is
rejected while that seller is offline, and insufficient capacity leaves the order unchanged without drops.

The canonical append-only wire manifest remains authoritative: demand-order confirmation, demand-order
delivery, and sales-order removal retain discriminators 13, 14, and 15. Protocols 12–15 are migrated on
both targets; protocol 16 remains legacy/unmigrated.

Protocol 13 demand-order confirmation is now migrated as a UUID-only common request. Confirming a delivered
demand order transactionally removes it and inserts the item only into the original requester's online main
inventory. Operators may act for an online owner but never receive that owner's item. Protocols 12, 13 and
15 share one transactional inventory adapter per target; their post-transaction feedback, notification and
market invalidation steps are isolated. Canonical discriminators 12–16 remain unchanged; protocol 16 is next.
