package com.mo.economy_system.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Loader-neutral repository contract for adding a Minecraft/loader target.
 *
 * <p>This deliberately reads the manifest and Gradle source as text.  The target test suites
 * already provide the JUnit runtime, while keeping this gate independent of either loader API.
 */
class TargetManifestContractTest {
  private static final Pattern SCHEMA = Pattern.compile("\\\"schemaVersion\\\"\\s*:\\s*(\\d+)");
  private static final Pattern DEFAULT = Pattern.compile("\\\"defaultTarget\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
  private static final Pattern TARGET_OBJECT = Pattern.compile("\\{\\s*\\\"id\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"(?s:.*?)\\}");
  private static final Pattern FIELD = Pattern.compile(
      "\\\"([A-Za-z][A-Za-z0-9]*)\\\"\\s*:\\s*(?:\\\"([^\\\"]*)\\\"|(\\d+))");

  @Test
  void manifestDefinesTheSupportedTargetsAndDefault() throws Exception {
    String manifest = read(repositoryRoot().resolve("gradle/economy-targets.json"));
    assertEquals("1", firstGroup(SCHEMA, manifest));
    assertEquals("neoforge-1.21.1", firstGroup(DEFAULT, manifest));

    List<Map<String, String>> targets = targetObjects(manifest);
    assertTrue(targets.stream().map(target -> target.get("id"))
        .collect(Collectors.toSet()).containsAll(List.of("neoforge-1.21.1", "forge-1.20.1")));
    assertUnique(targets, "id");
    assertUnique(targets, "projectPath");
    assertUnique(targets, "taskSuffix");
    Set<String> allowedLoaders = Set.of("forge", "neoforge");
    Set<String> ids = targets.stream().map(target -> target.get("id")).collect(Collectors.toSet());
    assertTrue(ids.contains(firstGroup(DEFAULT, manifest)), "default target must be present");
    for (Map<String, String> target : targets) {
      assertTrue(target.get("projectPath").startsWith(":targets:"), target.get("id"));
      assertTrue(allowedLoaders.contains(target.get("loader")), target.get("id"));
      assertFalse(target.get("minecraftVersion").isBlank(), target.get("id"));
      assertTrue(Integer.parseInt(target.get("javaVersion")) > 0, target.get("id"));
    }

    Map<String, Map<String, String>> byId = targets.stream()
        .collect(Collectors.toMap(target -> target.get("id"), target -> target, (left, right) -> left,
            LinkedHashMap::new));
    assertTarget(byId.get("neoforge-1.21.1"), ":targets:neoforge-1.21.1", "NeoForge1211",
        "NeoForge 1.21.1", "neoforge", "1.21.1", "21");
    assertTarget(byId.get("forge-1.20.1"), ":targets:forge-1.20.1", "Forge1201",
        "Forge 1.20.1", "forge", "1.20.1", "17");
  }

  @Test
  void targetDirectoriesAndSourceSetContractsMatchManifest() throws Exception {
    Path root = repositoryRoot();
    List<Map<String, String>> targets = targetObjects(read(root.resolve("gradle/economy-targets.json")));
    for (Map<String, String> target : targets) {
      String id = target.get("id");
      Path directory = root.resolve(target.get("projectPath").substring(1).replace(':', '/'));
      assertTrue(Files.isDirectory(directory), id + " target directory");
      assertTrue(Files.isRegularFile(directory.resolve("build.gradle")), id + " build.gradle");
      assertTrue(Files.isDirectory(directory.resolve("src/main/java")), id + " source tree");

      String build = read(directory.resolve("build.gradle"));
      assertTrue(build.contains("rootProject.file('common/src/main/java')"), id);
      assertTrue(build.contains("rootProject.file('common/src/test/java')"), id);
      assertTrue(build.contains("rootProject.file('common/src/main/resources')"), id);
      assertTrue(build.contains("java.exclude('com/mo/economy_system/screen/**')"), id);
      assertTrue(build.contains("java.toolchain.languageVersion = JavaLanguageVersion.of(" + target.get("javaVersion") + ")"), id);
    }
  }

  @Test
  void settingsAndRootLifecycleAreManifestDriven() throws Exception {
    Path root = repositoryRoot();
    String settings = read(root.resolve("settings.gradle"));
    String build = read(root.resolve("build.gradle"));

    assertTrue(settings.contains("economy-targets.json"), "settings must load the target manifest");
    assertTrue(settings.contains("JsonSlurper"), "settings must be the manifest parser");
    assertFalse(settings.contains("include('targets:neoforge-1.21.1')"));
    assertFalse(settings.contains("include('targets:forge-1.20.1')"));
    assertTrue(settings.contains("normalizedTargets"), "settings must expose normalized targets");
    assertTrue(settings.contains("normalizedTargets.each"), "settings must include every manifest target");
    assertTrue(settings.contains("schemaVersion"), "settings must validate the schema version");
    assertTrue(settings.contains("allowedLoaders"), "settings must validate loader names");
    assertTrue(settings.contains("projectPath.startsWith"), "settings must constrain target paths");
    assertTrue(settings.contains("defaultTarget"), "settings must validate the default target");
    assertTrue(settings.contains("javaVersion"), "settings must validate Java versions");

    assertTrue(build.contains("economyTargetManifest"), "root build must consume the settings model");
    assertFalse(build.contains("JsonSlurper"), "root build must not parse the target manifest");
    assertFalse(build.contains("economy-targets.json"), "root build must not reopen the manifest");
    assertFalse(build.contains("allowedLoaders"), "root build must not duplicate schema validation");
    assertFalse(build.contains("schemaVersion"), "root build must not duplicate schema validation");
    assertFalse(build.contains("projectPath.startsWith"), "root build must not duplicate path validation");
    assertFalse(build.contains("def targetTasks = ["), "target registry cannot be duplicated in root build");
    assertFalse(build.contains("dependsOn(':targets:"), "aggregate dependencies must not hardcode target paths");
    assertFalse(build.contains("dependsOn(\":targets:"), "aggregate dependencies must not hardcode target paths");
    for (String aggregate : List.of("compileAllTargets", "testAllTargets", "buildAllTargets", "verifyAllTargets")) {
      assertTrue(build.contains("tasks.register('" + aggregate + "'"), aggregate);
    }
    assertTrue(build.contains("targetProjects.collect"), "aggregate dependencies must be collected from targets");
    assertTrue(build.contains("defaultTarget"), "legacy aliases must resolve the manifest default");
    assertTrue(build.contains("excludedTaskNames"), "default aliases must exclude non-default run selectors");
    assertTrue(build.contains("runNeoForge1211ClientDev2"), "Dev2 compatibility alias must remain available");
    assertTrue(build.contains("neoForgeTarget != null"), "Dev2 compatibility target must be optional");
    assertFalse(build.contains("required compatibility target"), "Dev2 alias must not be a registry requirement");
    assertTrue(build.contains("validateTargetManifest"), "manifest lifecycle gate must be registered");
    assertRequiredLifecycleContract(build);
    assertTrue(build.contains("requiredTargetLifecycleTasks.each"),
        "validator must iterate the required lifecycle contract");
    for (String alias : List.of(
        "compile", "process", "test", "build", "run", "compileAllTargets", "testAllTargets",
        "buildAllTargets", "verifyAllTargets")) {
      assertTrue(build.contains(alias), "required root alias contract: " + alias);
    }
    assertTrue(build.contains("dependsOn('validateTargetManifest'"),
        "verifyAllTargets must validate the manifest lifecycle first");
  }

  @Test
  void aggregateDependenciesAndIncludesAreManifestDrivenSourceContracts() throws Exception {
    Path root = repositoryRoot();
    String settings = read(root.resolve("settings.gradle"));
    String build = read(root.resolve("build.gradle"));

    assertTrue(settings.contains("normalizedTargets"), "settings must include normalized manifest targets");
    assertTrue(settings.contains("normalizedTargets.each"), "settings includes must come from normalized targets");
    assertTrue(build.contains("targetProjects.collect"), "root aggregate dependencies must collect targets");
    assertTrue(build.contains("targetDependencies"), "root aggregate dependency helper must be manifest-driven");
    assertAggregateDependsOn(build, "compileAllTargets", "compileJava");
    assertAggregateDependsOn(build, "testAllTargets", "test");
    assertAggregateDependsOn(build, "buildAllTargets", "build");
    assertFalse(build.contains(":targets:forge-1.20.1"), "aggregate graph must not hardcode Forge");
    assertFalse(build.contains(":targets:neoforge-1.21.1"), "aggregate graph must not hardcode NeoForge");
  }

  private static void assertAggregateDependsOn(String build, String aggregate, String lifecycle) {
    int start = build.indexOf("tasks.register('" + aggregate + "'");
    assertTrue(start >= 0, aggregate + " task registration missing");
    int next = build.indexOf("tasks.register('", start + 1);
    String declaration = build.substring(start, next < 0 ? build.length() : next);
    assertTrue(declaration.contains("dependsOn(targetDependencies('" + lifecycle + "'))"),
        aggregate + " must depend on targetDependencies('" + lifecycle + "')");
  }

  private static void assertRequiredLifecycleContract(String build) {
    int start = build.indexOf("def requiredTargetLifecycleTasks");
    assertTrue(start >= 0, "validator must declare one lifecycle contract");
    int end = build.indexOf("tasks.register('validateTargetManifest'", start);
    assertTrue(end > start, "lifecycle contract must be declared before its validator");
    String declaration = build.substring(start, end);
    Matcher matcher = Pattern.compile("'([^']+)'").matcher(declaration);
    Set<String> lifecycleTasks = new java.util.LinkedHashSet<>();
    while (matcher.find()) lifecycleTasks.add(matcher.group(1));
    assertEquals(Set.of("compileJava", "processResources", "test", "build", "runClient", "runServer", "runData"),
        lifecycleTasks, "required lifecycle task set changed");
  }

  private static void assertTarget(
      Map<String, String> target,
      String projectPath,
      String taskSuffix,
      String displayName,
      String loader,
      String minecraftVersion,
      String javaVersion) {
    assertNotNull(target);
    assertEquals(projectPath, target.get("projectPath"));
    assertEquals(taskSuffix, target.get("taskSuffix"));
    assertEquals(displayName, target.get("displayName"));
    assertEquals(loader, target.get("loader"));
    assertEquals(minecraftVersion, target.get("minecraftVersion"));
    assertEquals(javaVersion, target.get("javaVersion"));
  }

  private static void assertUnique(List<Map<String, String>> targets, String field) {
    Set<String> values = targets.stream().map(target -> target.get(field)).collect(Collectors.toSet());
    assertEquals(targets.size(), values.size(), "duplicate manifest " + field);
    assertFalse(values.contains(null), "manifest " + field + " must be nonempty");
    assertTrue(values.stream().noneMatch(String::isBlank), "manifest " + field + " must be nonempty");
  }

  private static List<Map<String, String>> targetObjects(String manifest) {
    int targetsStart = manifest.indexOf("\"targets\"");
    int arrayStart = manifest.indexOf('[', targetsStart);
    int arrayEnd = manifest.indexOf(']', arrayStart);
    assertTrue(targetsStart >= 0 && arrayStart >= 0 && arrayEnd > arrayStart, "manifest targets array missing");
    String array = manifest.substring(arrayStart + 1, arrayEnd);
    List<Map<String, String>> result = new ArrayList<>();
    Matcher matcher = TARGET_OBJECT.matcher(array);
    while (matcher.find()) {
      Map<String, String> fields = new LinkedHashMap<>();
      Matcher fieldMatcher = FIELD.matcher(matcher.group());
      while (fieldMatcher.find()) {
        fields.put(fieldMatcher.group(1), fieldMatcher.group(2) != null
            ? fieldMatcher.group(2) : fieldMatcher.group(3));
      }
      result.add(fields);
    }
    return result;
  }

  private static String firstGroup(Pattern pattern, String source) {
    Matcher matcher = pattern.matcher(source);
    assertTrue(matcher.find(), "manifest field missing: " + pattern);
    return matcher.group(1);
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}
