# Future Target Skeleton

This directory is the starting point for a new Minecraft/loader target. It is
intentionally not included in `settings.gradle`: a real version target must
declare its own loader plugin and Minecraft dependency before being added to
the build.

The target may implement platform, network, item, registry, event, and screen
shell adapters. It must consume `common/src/main/java` for all business
transactions and UI state, layout, theme, and semantic rendering contracts.
Do not copy `core`, `screen`, `common`, or root `src/main/java` classes into a
new target.

The template source set is deliberately limited to `common/src/main/java` and
the target's own adapter package. A new target should use the same source-set
shape as the Forge and NeoForge targets and add its loader-specific API
dependency in its own Gradle build file.
