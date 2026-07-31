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
