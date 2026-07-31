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
  transfers (`4`), system-shop catalog synchronization and purchasing (`5-7`), and server
  player lists (`34/35`). Both targets have real codecs and handlers for these
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
  new snapshot write uses schema v1. Stage A is complete. The next migration
  slice is protocol `8` (create sale order), which is not part of Stage A.
- The verified Stage A build runs 35 Forge 1.20.1 tests and 36 NeoForge
  1.21.1 tests with no failures.
