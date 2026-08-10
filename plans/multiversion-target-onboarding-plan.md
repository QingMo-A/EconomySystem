# Multiversion Target Onboarding Infrastructure

Status: **IMPLEMENTED / VERIFIED**
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

## Common, manifest, and target roles

`common/` = runtime/domain/UI source of truth. `gradle/economy-targets.json` = supported-version
source of truth. `targets/<version>/` = Minecraft/loader API adapters.

- `common/` owns business rules, transactions, protocol semantics, UI state/layout/theme contracts,
  and common tests.
- The target manifest describes identity and compatibility metadata; it contains no business behavior
  or UI policy.
- `targets/<version>/` owns registration, network binding, persistence, native item/player/world
  conversion, filesystem access, and rendering/Screen lifecycle.
- `settings.gradle` validates and includes every manifest project. Root `build.gradle` validates the
  same contract and creates all lifecycle/aggregate tasks from the parsed collection.

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
run Client, run Server, and run Data. It also owns `compileAllTargets`, `testAllTargets`,
`buildAllTargets`, and `verifyAllTargets` (the latter depends on the test and build aggregates).

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

- Base/preflight checkpoint: local and origin `bridge` at `0cf37b63a3cd5a2efefb650524502bc2b8a11ae0`.
- Code checkpoint: `0cc23b0653106dbe5e962eb60a5f2fa76ffd83ce`.
- Documentation checkpoint: see git history; no self SHA is recorded here.
- Supported IDs: `neoforge-1.21.1`, `forge-1.20.1`.
- Default target: `neoforge-1.21.1`.
- Compatibility aliases: `compile{NeoForge1211,Forge1201}`,
  `process{NeoForge1211,Forge1201}Resources`, `test{NeoForge1211,Forge1201}`,
  `build{NeoForge1211,Forge1201}`, `run{NeoForge1211,Forge1201}{Client,Server,Data}`,
  `runNeoForge1211ClientDev2`, and root `runClient`/`runServer`/`runData`.
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
  registries and aggregate dependencies contain no hardcoded target paths.
- JAR checks: both built JARs contain common classes and their own adapter classes, contain zero
  opposite-loader target/API entries, and contain zero deleted legacy packet entries.

## Exceptions and operational notes

- Each target has one skipped Windows filesystem test requiring symbolic-link privilege; no failures
  or errors remain in the final XML totals.
- Existing Java/loader deprecation warnings are compiler warnings only and do not change the target
  contract.
- A target-exclusive task is acceptable only when it is explicitly named, justified by an API
  capability, and still resolved through the manifest; common lifecycle behavior must remain shared.
