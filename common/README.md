# EconomySystem Shared Sources

## Protocol 21 spatial boundary hardening

## Protocol 22 territory member removal

Protocol 22 is migrated as the loader-neutral `RemoveTerritoryMemberMessage`. Its C2S wire is exactly 32 bytes in `territoryId`, `targetPlayerId` order. The authenticated sender is the expected live owner; owners cannot remove themselves, targets must currently be members, and offline targets remain removable. A dedicated server-scoped limiter is independent from protocol 21. Successful removal performs fail-closed, exact pending-invite cleanup for only the target and territory. NeoForge preserves and validates raw canonical member names through injectable mutation/rollback tests; manager integration rechecks identity, SavedData, owner buckets and spatial indexes. Forge revalidates raw/cache after dirty and proves complete rollback. Strict protocol registration, language parity and narrow-layout tests are included. Final validation: 285 shared-source, 362 Forge, and 422 NeoForge tests. Protocol 23 and later remain legacy.

Protocol 21 remains a C2S request containing exactly one 16-byte territory UUID. Its NeoForge authoritative repository treats territory rectangles as closed integer intervals and verifies QuadTree identity count, UUID count, expected node path, and representative-point queries during deletion, resize, and compensation. Single-cell, single-column, single-row, root-edge, and split-boundary territories are supported. Low-level resize manager methods are package-private; protocol 22 and later remain legacy. Final validation executed 266 shared-source tests, 333 Forge tests, and 382 NeoForge tests.

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
  restored and recaptured by both targets. The complete verified suite runs 199 shared-source,
  280 Forge 1.20.1 and 274 NeoForge 1.21.1 tests with no failures (228 shared-source tests).
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
- Protocols 12 through 18 are migrated. Territory request 17 carries only a monotonic
  request ID; the server derives the requester from the authenticated sender. Response
  18 uses bounded, NBT-free snapshots: owners receive complete management data while
  authorized members receive summary-only data. Both codecs enforce the same 1 MiB
  estimated and raw wire budgets, and clients reject stale request IDs before atomically
  committing restored lists. Protocol 19 is migrated separately. Protocol 20 hardening uses a 32-byte `territoryId + targetPlayerId` message and a tokenized PENDING/PROCESSING store. Invite-ID collisions retry eight times without overwriting indexes. `PERSIST_FAILED` releases a claim for retry; `STATE_UNKNOWN` consumes it. Invitations remain server-session scoped, expire after 1200 ticks, are capped at 4096 entries, and are deduplicated per target/territory. Protocols 17-21 are migrated; protocol 22 and later remain legacy.
- Protocol 14 hardening is complete: delivery uses expected-order atomic transition, exact supplier
  credit without charging the requester again, transactional main-inventory removal, and independent
  payment/inventory compensation. Failure reports carry the authoritative requester ID and per-stage
  mutation/exception details. Both target adapters use the tested loader-neutral removal kernel.

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
both targets; protocol 16 is also migrated as a UUID-only common cancellation request.

Protocol 13 demand-order confirmation is now migrated as a UUID-only common request. Confirming a delivered
demand order transactionally removes it and inserts the item only into the original requester's online main
inventory. Operators may act for an online owner but never receive that owner's item. Protocols 12, 13 and
15 share one transactional inventory adapter per target; their post-transaction feedback, notification and
market invalidation steps are isolated. Canonical discriminators 12–16 remain unchanged.

Protocol 16 cancellation resolves ownership, operator permission, refund recipient and the frozen total
price exclusively on the server. Expected-order removal and exact refund form a compensating transaction;
failed restoration reports CHANGED or UNKNOWN and broadcasts INVALIDATED. A restored
repository-contract mismatch is also CHANGED because the authoritative order differs
from the preview. Protocol 14 credit failures now report active inventory rollback only
through `inventoryRollbackSucceeded`.
Protocol 16 hardening closes construction invariants and repository-contract recovery: ORDER_CHANGED never
carries a stale preview as authoritative data, mismatched removed orders are restored before failure, and
all repository/refund/restore exceptions are combined for target logs. Protocol 14 pre-compensation telemetry
now leaves inventory restoration fields unset until rollback actually runs.

## Territory protocols 17/18 hardening

Protocol 18 now carries the explicit stable kind IDs `data` and `error`; enum ordinals are not wire data.
`DATA` retains the complete-owned/minimal-authorized boundary, while `ERROR` is an empty, detail-free sync
failure signal. The page request ID is the only stale-response authority; the dead global
territory response tracker has been removed.

`TerritoryDataClientApplier` restores every owned and authorized entry before a single commit, so restore
failure cannot publish a partial page. Current ERROR responses and 200-tick timeouts end loading without
clearing previously committed data, and retry allocates a new non-negative request ID. Server query,
capture, response construction, and DATA-send failures attempt exactly one ERROR response.

`TerritoryDataWireCodec` is now the only NBT-free field implementation used by both targets. It writes to a
temporary buffer, checks the 1 MiB raw budget, then copies into the destination, so a rejected encode leaves
the destination unchanged. Stable DATA and ERROR golden fixtures run in both target suites.

Protocol 19 is a common UUID-only C2S message with an exact 16-byte, NBT-free wire codec. The common teleport
service re-reads the authoritative territory, permits only owner or current authorized members, validates the
dimension and exact `backpoint.above()` destination, applies a bounded 20-tick server cooldown, and performs
recall-potion removal/commit/rollback as a one-shot transaction. `RecallPotionReservation.reserve` owns the
remaining-stack write and dirty mark before returning; a reserve failure restores the original stack or reports
ROLLBACK_FAILED with the compensation error suppressed. Both targets separate required server dirty marking from
best-effort client synchronization, so sync failure cannot reverse a confirmed inventory fact. Rollback restores only that potion: it uses the
original slot only while its expected remainder is unchanged, otherwise selects a mergeable stack or empty slot,
and fails without overwriting unrelated items when neither exists. Repository, capture, dimension, preparation
and inventory exceptions become a logged generic failure; rollback exceptions are suppressed onto the primary
failure. Commit is an in-memory, unchecked state transition. Arrival reports ARRIVED/NOT_ARRIVED/UNKNOWN;
UNKNOWN commits the removal without automatic refund and returns TELEPORT_STATE_UNKNOWN to avoid duplication.
Limiters are weakly server-scoped and also reset on tick epoch rollback. Ordinary effect failures remain best
effort and JVM Errors are never swallowed. It uses an expiring `POST_TELEPORT` ticket and never persists
a forced chunk. Supported results are SUCCESS, TERRITORY_NOT_FOUND, NO_PERMISSION, NO_BACKPOINT,
DIMENSION_NOT_FOUND, UNSAFE_DESTINATION, NO_RECALL_POTION, COOLDOWN, TELEPORT_FAILED,
TELEPORT_STATE_UNKNOWN and ROLLBACK_FAILED.
Protocol 20 is complete with the unchanged 32-byte UUID-only request. The invitation store rejects
already-expired/future-created inserts, keeps claim construction and completion package-local, and maps
PERSIST_FAILED to a safe release while consuming STATE_UNKNOWN. Client player-list responses atomically publish
an immutable snapshot with a monotonic local revision, allowing Forge to distinguish a fresh response from stale
cache. Canonical invite resources are strict-parsed for duplicate keys and placeholder parity. Protocol 21
removal is now migrated and hardened as described below; protocol 22 and later territory operations remain
legacy. The current suites contain 266 shared-source, 333 Forge and 349 NeoForge tests.

Protocol 21 uses the loader-neutral `RemoveTerritoryMessage` and the shared 16-byte UUID codec. The service
derives authority from the authenticated sender, applies a bounded 20-tick server-scoped limiter, never refunds,
and exposes SUCCESS, TERRITORY_NOT_FOUND, NO_PERMISSION, RATE_LIMITED, PERSIST_FAILED and STATE_UNKNOWN.
`PERSIST_FAILED` guarantees rollback and safe retry; `STATE_UNKNOWN` does not. Protocol 22+ remains legacy.
Successful removal cleanup receives the immutable server-side removed snapshot, so dependent invitation and
NeoForge resize-session cleanup never re-read deleted state. NeoForge resize preserves primary/owner/SavedData
identity, rebuilds only QuadTree, and refunds an expansion debit only when failure or rollback is proven.

Final protocol-21 integration uses an immutable prepare/commit resize plan. Prepare reads the live Territory,
validates every index and overlap, and derives the charge from live old/new inclusive X/Z areas; the session
difference is preview-only. Commit revalidates identity, owner, old bounds/backpoint, indexes and overlap before
mutation. Equal-area reshapes are free, identical bounds are UNCHANGED, and STATE_UNKNOWN never auto-refunds.
QuadTree verification proves identity count, expected storage path and representative point queries. Removal
repositories carry an explicit NONE/INTEGRITY/PERSISTENCE/UNKNOWN failure kind, and both handlers use one
overworld game-time value for the limiter and cleanup. The request wire remains one UUID (16 bytes); protocol
22+ remains legacy.

## Client file check transaction (protocols 23-25)

Protocols 23-25 are migrated as one atomic transaction. Common owns the three immutable messages, the
legacy-order bounded wire codec, strict schema-1 JSON model, weak server-scoped pending store, fixed-directory
scanner, bounded daemon executor and local comparison model. The only roots are `mods`, `shaderpacks` and
`resourcepacks`; scanning is non-recursive, rejects symlinks and streams SHA-256 without transmitting contents,
paths, timestamps or exception details. The server binds every response to authenticated target plus the exact
pending requester/type key, consumes malformed results as `FAILED/INVALID_RESULT`, and prevents replay.
Protocol 26 and later remain legacy.

Protocols 23-25 are lifecycle-hardened. Common owns the connection-generation task coordinator, exact consent
coordinator, status-specific result controller/layout, and the single server result-routing service. Session
cancellation never becomes a protocol 24 result. Scans cap direct-child enumeration at 4096 and use one
`NOFOLLOW_LINKS` channel for size plus streaming SHA-256. Verified counts: 317 shared-source, 393 Forge, 453
NeoForge. Protocol 26+ remains legacy.
The verified suites contain 303 shared-source, 379 Forge and 439 NeoForge tests.
