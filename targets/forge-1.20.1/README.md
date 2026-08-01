# Forge 1.20.1 Target Status

This target now validates the Gradle/Java 17/Forge toolchain, shared platform
services, the Forge SavedData shell, and six end-to-end protocol slices:

- balance request/response (`0/1`);
- balance-history request/response (`2/3`);
- atomic online-player transfer (`4`);
- system-shop catalog request/response (`5/6`);
- server-authoritative system-shop purchase (`7`);
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
read/legacy-API compatibility path only. Protocol `8` is the next planned
migration and has not been implemented here.

The hardened bridge applies the common snapshot limits at creation, schema
decode/encode, capture, and restore. Nonzero `Damage` on an item with no
durability now fails during capture instead of producing a snapshot that later
fails restoration. A shared schema-v1 golden fixture covers native names,
lore, normal/stored enchantments, tooltip flags, damage, repair cost,
unbreakable, dye, model data, and custom data.

The current full target suite contains 43 passing tests. The paired NeoForge
1.21.1 target contains 44 passing tests; `buildAllTargets --rerun-tasks` passes.
