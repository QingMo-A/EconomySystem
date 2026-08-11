# Multiversion Target Onboarding Infrastructure

Status: **IMPLEMENTED / HARDENED / VERIFIED**
UI phase: **CLOSED**
Next phase: **TARGET ONBOARDING**

```text
MULTIVERSION_COMMON_UI_PARITY_PHASE = CLOSED

NEXT_PHASE =
MULTIVERSION_TARGET_ONBOARDING_INFRASTRUCTURE
```

This plan records the build and architecture contract after the UI parity phase. It does not reopen
the frozen UI plan and it does not add a third Minecraft target.

## Objective

Make a new Minecraft/loader target an explicit, reviewable adapter exercise rather than a second
hand-maintained Gradle registry. One manifest is the source for project inclusion, target aliases,
aggregate verification, default-run routing, and fail-fast target metadata checks.

## Hardening result

The target manifest lifecycle boundary is now enforced in the Gradle implementation:

- `settings.gradle` is the sole `JsonSlurper` parser and authoritative validator. It retains all
  schema, loader, uniqueness, path-canonicalization, directory, build-file, Java-version, and
  default-target fail-fast checks.
- Settings publishes the normalized immutable-ish `gradle.ext.economyTargetManifest` model. Root
  `build.gradle` consumes that model and fails fast with `Validated economy target manifest is
  unavailable` when the handoff is missing; it does not reopen JSON or duplicate schema logic.
- `runNeoForge1211ClientDev2` is conditional on the manifest's NeoForge target. Removing that target
  removes only the compatibility alias and does not make configuration fail.
- `validateTargetManifest` checks every manifest project for `compileJava`, `processResources`,
  `test`, `build`, `runClient`, `runServer`, and `runData`, then checks every generated per-target
  alias and all four aggregate tasks with target-specific diagnostics.
- `verifyAllTargets` depends on `validateTargetManifest`, `testAllTargets`, and `buildAllTargets`,
  making lifecycle registration a required verification gate.
- `TargetManifestContractTest` locks the sole-parser/model-handoff boundary, optional Dev2 behavior,
  lifecycle set, alias contract, aggregate source wiring, and verify integration. Its former
  future-target map test is now an explicit source contract plus real Gradle dry-run evidence.

## Common, manifest, and target roles

`common/` = runtime/domain/UI source of truth. `gradle/economy-targets.json` = supported-version
source of truth. `targets/<version>/` = Minecraft/loader API adapters.

- `common/` owns business rules, transactions, protocol semantics, UI state/layout/theme contracts,
  and common tests.
- The target manifest describes identity and compatibility metadata; it contains no business behavior
  or UI policy.
- `targets/<version>/` owns registration, network binding, persistence, native item/player/world
  conversion, filesystem access, and rendering/Screen lifecycle.
- `settings.gradle` validates and includes every manifest project, then publishes the normalized
  model. Root `build.gradle` consumes that validated model and creates all lifecycle/aggregate tasks.

## Manifest contract

The manifest uses `schemaVersion: 1`, a `defaultTarget`, and a non-empty `targets` array. Each entry
must expose:

| Field | Contract |
|---|---|
| `id` | non-empty and unique target identity |
| `projectPath` | unique Gradle path below `:targets:`; its directory and `build.gradle` must exist |
| `taskSuffix` | non-empty and unique suffix for root aliases |
| `displayName` | human-readable target name |
| `loader` | allow-listed loader (`forge` or `neoforge`) |
| `minecraftVersion` | non-empty Minecraft version |
| `javaVersion` | positive Java toolchain version |

Validation fails before project configuration for malformed JSON, unsupported schema, empty or
duplicate fields, missing default, unsafe/out-of-tree paths, missing target directories/build files,
unknown loaders, blank Minecraft versions, or non-positive Java versions.

## Future target onboarding (ten steps)

1. Create the target directory under `targets/<version>`.
2. Add its `build.gradle`, toolchain, and native dependencies.
3. Add the target's identity and compatibility entry to `gradle/economy-targets.json`.
4. Implement platform, network, persistence, registry, event, and UI adapters; bind canonical
   common messages without changing IDs, directions, or append-only discriminator order.
5. Add common business code only when the request is an actual new version-independent feature,
   not as a copy of target code.
6. Do not copy common GUI, layout/theme, language, or texture resources into the target.
7. Add and run target-specific compatibility tests plus loader-neutral common contract tests for
   newly discovered behavior; record explicit capability deviations.
8. Run `testAllTargets --no-daemon --rerun-tasks`.
9. Run `buildAllTargets --no-daemon --rerun-tasks` (and the compile/verify aggregates).
10. Audit JAR isolation and target ownership, then update this plan with evidence and commit the
    implementation and documentation separately.

## Capability-deviation record

Every unavoidable version difference is recorded with: target ID; loader/Minecraft/Java versions;
capability or API boundary; NeoForge reference behavior; common semantic contract; exact API
limitation; chosen adapter/fallback; user-visible or wire-visible difference; safety impact;
compatibility test/source gate; owner; status; and a revisit condition/date. Silent target-local
feature removal or redesign is not an accepted deviation.

## Gradle lifecycle and task policy

The manifest owns common lifecycle aliases for every target: compile, processResources, test, build,
run Client, run Server, and run Data. `validateTargetManifest` checks those seven project tasks plus
the generated aliases for every manifest entry and the four global aggregates. It also owns
`compileAllTargets`, `testAllTargets`, `buildAllTargets`, and `verifyAllTargets`; the latter depends
on lifecycle validation, the test aggregate, and the build aggregate.

Legacy root `runClient`, `runServer`, and `runData` resolve `defaultTarget` and dynamically exclude
non-default subproject run selectors. The explicit `runNeoForge1211ClientDev2` compatibility alias is
allowed as a target-exclusive task, but its project path is still resolved from and validated against
the manifest. No target-specific common lifecycle registry or third target is permitted.

## Normally no-edit files for onboarding

Adding a target normally requires no hand edits to the settings target include list, the root
`targetTasks` registry, `compileAllTargets` dependencies, or `buildAllTargets` dependencies. Those
collections are generated from the manifest. An exception must be a documented, target-exclusive
task justified by a loader capability.

## Verification checkpoint

- Execution base: local and origin `bridge` at `ccb44c78d35ccca9fd9552d1ed029225eab0a37f` before
  hardening; no manifest data change (`schemaVersion: 1`).
- Code checkpoint: see git history (the code/test commit precedes this documentation commit).
- Documentation checkpoint: see git history; no self SHA is recorded here.
- Supported IDs: `neoforge-1.21.1`, `forge-1.20.1`.
- Default target: `neoforge-1.21.1`.
- Compatibility aliases: `compile{NeoForge1211,Forge1201}`,
  `process{NeoForge1211,Forge1201}Resources`, `test{NeoForge1211,Forge1201}`,
  `build{NeoForge1211,Forge1201}`, `run{NeoForge1211,Forge1201}{Client,Server,Data}`,
  `runNeoForge1211ClientDev2`, and root `runClient`/`runServer`/`runData`.
- `validateTargetManifest --no-daemon`: passed; required lifecycle and alias contracts present for
  both manifest targets.
- Aggregate task-graph dry-runs (all passed; both target project paths present):
  `compileAllTargets --dry-run`, `testAllTargets --dry-run`, `buildAllTargets --dry-run`, and
  `verifyAllTargets --dry-run`.
- Default alias dry-runs (all passed; NeoForge default only): `runClient --dry-run`,
  `runServer --dry-run`, and `runData --dry-run`.
- Dev2 compatibility dry-run (passed; no client launch):
  `runNeoForge1211ClientDev2 --dry-run`.
- Final target XML totals: Forge 904 tests, 0 failures, 0 errors, 1 skipped; NeoForge 969 tests,
  0 failures, 0 errors, 1 skipped.
- `:targets:forge-1.20.1:test --no-daemon --rerun-tasks`: passed.
- `:targets:neoforge-1.21.1:test --no-daemon --rerun-tasks`: passed.
- `testAllTargets --no-daemon --rerun-tasks`: passed.
- `compileAllTargets --no-daemon --rerun-tasks`: passed.
- `buildAllTargets --no-daemon --rerun-tasks`: passed.
- `verifyAllTargets --no-daemon --rerun-tasks`: passed.
- `tasks --group "economy system"`: lists all per-target aliases and all four aggregates.
- Root `runClient`, `runServer`, and `runData --dry-run`: each selects only
  `:targets:neoforge-1.21.1` plus the root alias.
- Static checks: common production sources contain no Minecraft/Forge/NeoForge imports; target
  registries and aggregate dependencies contain no hardcoded target paths. `build.gradle` contains
  no `JsonSlurper`, `allowedLoaders`, or schema/path validator; the only target ID literal is the
  optional Dev2 lookup.
- JAR/resource checks: Forge JAR contains common classes plus Forge adapter classes and no
  NeoForge/Forge-opposite target/API entries; NeoForge JAR contains common classes plus NeoForge
  adapter classes and no Forge target/API entries. Both contain zero deleted legacy packet entries
  and the expected common `en_us.json`/`zh_cn.json` language resources (2 each).

## Exceptions and operational notes

- Each target has one skipped Windows filesystem test requiring symbolic-link privilege; no failures
  or errors remain in the final XML totals.
- Existing Java/loader deprecation warnings are compiler warnings only and do not change the target
  contract.
- A target-exclusive task is acceptable only when it is explicitly named, justified by an API
  capability, and still resolved through the manifest; common lifecycle behavior must remain shared.
