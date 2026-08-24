package com.mo.economy_system.common.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Language resources are shared by both loader targets.  Keep this test rooted in the common
 * source tree so a target cannot silently ship a second, divergent language authority.
 */
class StrictLanguageResourceParityTest {
  private static final Path COMMON_EN =
      Path.of("common/src/main/resources/assets/economy_system/lang/en_us.json");
  private static final Path COMMON_ZH =
      Path.of("common/src/main/resources/assets/economy_system/lang/zh_cn.json");
  private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z]");
  private static final Pattern TRANSLATION_KEY =
      Pattern.compile("\\\"([a-z]+(?:\\.[a-zA-Z0-9_]+)+)\\\"");

  // Values which are intentionally language-neutral identifiers/brand names.
  private static final Set<String> LANGUAGE_NEUTRAL_KEYS =
      Set.of(
          "item.economy_system.hanhanyu_doll_hat",
          "item.economy_system.player_351987654321_doll_hat",
          "item.economy_system.poxiaojin_doll_hat",
          "itemGroup.economy_system.tab",
          "screen.home.version",
          "button.territory.delete_short",
          "screen.territory.detail.settings.copy_id.description",
          "screen.territory.buff.tooltip.cost.item_unknown");

  @Test
  void commonResourcesAreValidCompleteAndLoaderNeutral() throws Exception {
    Path root = repositoryRoot();
    JsonObject en = read(root.resolve(COMMON_EN));
    JsonObject zh = read(root.resolve(COMMON_ZH));

    assertEquals(en.keySet(), zh.keySet(), "zh_cn must cover every en_us key");
    assertTrue(en.keySet().size() >= 820, "active + legacy language key set unexpectedly shrank");

    for (String key : new TreeSet<>(en.keySet())) {
      String english = en.get(key).getAsString();
      String chinese = zh.get(key).getAsString();
      assertFalse(english.isBlank(), key + " has blank en_us text");
      assertFalse(chinese.isBlank(), key + " has blank zh_cn text");
      assertEquals(signature(english), signature(chinese), key + " placeholder signature differs");
      if (!LANGUAGE_NEUTRAL_KEYS.contains(key)) {
        assertFalse(english.equals(chinese), key + " silently falls back to English");
      }
    }

    Set<String> active = activeUiKeys(root.resolve("common/src/main/java"));
    assertTrue(en.keySet().containsAll(active), () -> "missing active UI keys: " + difference(active, en.keySet()));

    // These legacy terms are stable user-facing anchors.  They also catch accidental use of the
    // newer generated language providers instead of the f334e640 reference resources.
    assertEquals("Search item ID or order creator", en.get("screen.market.search_hint").getAsString());
    assertEquals("搜索物品 ID 或订单创建者", zh.get("screen.market.search_hint").getAsString());
    // No target-local language authority is permitted.  Both target JARs consume common resources.
    for (Path targetLanguage : targetLanguagePaths(root)) {
      assertFalse(Files.exists(targetLanguage), "target-local language copy: " + targetLanguage);
    }
  }

  @Test
  void legacyOverlapValuesRemainExact() throws Exception {
    Path root = repositoryRoot();
    JsonObject en = read(root.resolve(COMMON_EN));
    JsonObject zh = read(root.resolve(COMMON_ZH));
    Map<String, JsonObject> legacy = legacyResources(root);
    assertEquals(396, legacy.get("en_us").keySet().size(), "legacy en_us contract changed");
    assertEquals(396, legacy.get("zh_cn").keySet().size(), "legacy zh_cn contract changed");
    for (String key : legacy.get("en_us").keySet()) {
      assertTrue(en.has(key), "common en_us missing legacy key " + key);
      assertTrue(zh.has(key), "common zh_cn missing legacy key " + key);
      assertEquals(legacy.get("en_us").get(key), en.get(key), "legacy en_us value changed: " + key);
      assertEquals(legacy.get("zh_cn").get(key), zh.get(key), "legacy zh_cn value changed: " + key);
    }
  }

  private static JsonObject read(Path path) throws Exception {
    String content = Files.readString(path, StandardCharsets.UTF_8);
    assertNoDuplicateKeys(path.toString(), content);
    return JsonParser.parseString(content).getAsJsonObject();
  }

  private static void assertNoDuplicateKeys(String path, String content) throws Exception {
    Set<String> names = new HashSet<>();
    try (JsonReader reader = new JsonReader(new StringReader(content))) {
      reader.beginObject();
      while (reader.hasNext()) {
        String name = reader.nextName();
        assertTrue(names.add(name), path + " duplicate key " + name);
        reader.skipValue();
      }
      reader.endObject();
    }
  }

  private static String signature(String text) {
    Matcher matcher = PLACEHOLDER.matcher(text);
    int count = 0;
    while (matcher.find()) count++;
    return Integer.toString(count);
  }

  private static Set<String> activeUiKeys(Path sourceRoot) throws Exception {
    Set<String> keys = new HashSet<>();
    try (var paths = Files.walk(sourceRoot)) {
      paths.filter(path -> path.toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  Matcher matcher = TRANSLATION_KEY.matcher(Files.readString(path, StandardCharsets.UTF_8));
                  while (matcher.find()) {
                    String key = matcher.group(1);
                    if (key.startsWith("screen.")
                        || key.startsWith("button.")
                        || key.startsWith("message.")
                        || key.startsWith("text.")
                        || key.startsWith("tooltip.")
                        || key.startsWith("item.")
                        || key.startsWith("key.")
                        || key.startsWith("enchantment.")
                        || key.startsWith("itemGroup.")) keys.add(key);
                  }
                } catch (Exception exception) {
                  throw new IllegalStateException("failed to scan " + path, exception);
                }
              });
    }
    return keys;
  }

  private static Set<String> difference(Set<String> left, Set<String> right) {
    Set<String> result = new TreeSet<>(left);
    result.removeAll(right);
    return result;
  }

  private static List<Path> targetLanguagePaths(Path root) {
    return List.of(
        root.resolve("targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/en_us.json"),
        root.resolve("targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/zh_cn.json"),
        root.resolve("targets/neoforge-1.21.1/src/generated/resources/assets/economy_system/lang/en_us.json"),
        root.resolve("targets/neoforge-1.21.1/src/generated/resources/assets/economy_system/lang/zh_cn.json"));
  }

  private static Map<String, JsonObject> legacyResources(Path root) throws Exception {
    Map<String, JsonObject> result = new LinkedHashMap<>();
    String revision = "f334e640ca1e24157511b7e06f1f76efba90152b";
    for (String language : List.of("en_us", "zh_cn")) {
      String path =
          "targets/forge-1.20.1/src/main/resources/assets/economy_system/lang/"
              + language
              + ".json";
      Process process =
          new ProcessBuilder("git", "show", revision + ":" + path)
              .directory(root.toFile())
              .redirectErrorStream(true)
              .start();
      String content = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      assertEquals(0, process.waitFor(), "unable to read legacy " + language + " resource");
      result.put(language, JsonParser.parseString(content).getAsJsonObject());
    }
    return result;
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    return Objects.requireNonNull(current, "workspace root");
  }
}
