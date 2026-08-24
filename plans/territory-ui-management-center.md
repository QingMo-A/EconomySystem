# Territory UI Management Center

## Goal

Replace the duplicated `TerritoryManageScreen -> TerritoryDetailScreen` navigation flow with one primary territory management center while retaining the legacy management screen as a compatibility shell.

## Status

`B1 BUFF COMMON = IMPLEMENTED / VERIFIED`

`B2 NESTED TERRITORY COMMON = IMPLEMENTED / VERIFIED`

The implementation base is `817da72cdcdac64f78453964e85fa4597f610d1e`; code checkpoints and this documentation checkpoint are recorded in Git history.

## Phase 1 implementation

- Territory list `Manage` opens `TerritoryDetailScreen` directly on Forge 1.20.1 and NeoForge 1.21.1.
- Territory detail becomes the primary management shell with fixed navigation:
  - Overview
  - Members
  - Permissions
  - Buffs
  - Settings
- Overview becomes an actual status dashboard rather than a stack of navigation buttons.
- Members shows authorized members only, supports removal, and exposes an explicit invite action.
- Permissions uses one current-state button per rule; clicking cycles `Owner only -> Members -> Everyone -> Owner only`, and the button label always reflects the active level.
- Bulk `Private / Standard / Open` presets are not shown in the UI; the simpler per-rule cycle interaction is the preferred player-facing behavior.
- Settings groups ID copy, resize, transfer and deletion; transfer/delete use danger semantics.
- Mutation refreshes keep the last snapshot visible while synchronizing instead of blanking the page.
- Existing OP bypass and server-authoritative permission evaluation remain unchanged.

## Compatibility

- Keep `TerritoryManageScreen` and its controller for legacy entry points during this pass.
- Keep `ACCESS`, `TRANSFER`, and the old cycle-rule event path internally where useful so old target/tests are not broken unnecessarily.
- No network wire format changes are required for the UI-only restructuring.

## B1 Buff common closure

- `BuffManageController` owns loading, success, empty, explicit error, timeout, retry and stale/unknown request handling.
- Search, paging, scroll boundaries, maximum level, unavailable resources and target availability are common policies.
- Unlock/upgrade submission is one-shot while a refresh is in flight.
- Layout coverage includes canonical, fractional, ultra-narrow and ultra-short viewports.
- Forge and NeoForge shells retain only lifecycle, native widgets, rendering adapters, network send and fallback navigation.

## B2 common model mapping

The repository already had the more complete unified `TerritoryDetail*` management-center model when B2 began. It is the canonical implementation of the planned `TerritoryAction*` family, avoiding a second competing state machine:

- `TerritoryDetailViewKind` maps `MAIN / ACCESS / RULES / SETTINGS / TRANSFER`.
- `TerritoryDetailState/Event/Controller/Layout/View` own overview, members, permissions, rules, transfer and settings semantics.
- `TerritoryInviteState/Event/Controller/Layout/View` own loading/error/timeout/retry, player-list revision checks, search, paging, selection and one-shot invite submission.
- `TerritoryConfirmationState/Event/Controller/Layout/View` own remove-member/remove-territory confirmation and the cancel/confirm one-shot gate.
- All collection state uses immutable copies; common code contains no Minecraft or loader types.

The active Forge 1.20.1 and NeoForge 1.21.1 shells route management, Buffs, invite, member removal, transfer and deletion through these common contracts. Legacy screens remain compatibility fallbacks and are no longer the default management path.

## Capability deviation

None recorded. Forge 1.20.1 and NeoForge 1.21.1 currently expose equivalent common territory semantics. Existing wire IDs/codecs and server-authoritative territory transactions were not changed by this UI commonization.

## Verification (2026-08-23)

- Forge 1.20.1 full suite: 1002 tests, 0 failures, 1 skipped.
- NeoForge 1.21.1 full suite: 1067 tests, 0 failures, 1 skipped.
- `buildAllTargets --no-daemon --rerun-tasks`: successful, 19 tasks executed.
- Common production loader scan: no `net.minecraftforge` or `net.neoforged` references.
- B1/B2 targeted controller, layout, renderer/parity and target bridge suites: passed on both targets.
- Production gameplay semantics changed: NO.
- Territory network/wire format changed: NO.
