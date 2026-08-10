package com.mo.economy_system.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Final cross-loader UI audit gate.  This deliberately reads production source
 * rather than asserting only RecordingRenderer output, so a target cannot regain
 * visual authority or bypass the measured common layout on one loader.
 */
class StrictUiForensicAuditTest {
  private static final Pattern DIRECT_DRAW = Pattern.compile(
      "\\.(?:fill|fillGradient|drawString|drawCenteredString|blit|renderItem|"
          + "renderFakeItem|renderItemDecorations|renderTooltip|renderComponentTooltip)\\s*\\(");
  private static final Pattern APPROXIMATE_CONVENIENCE = Pattern.compile(
      "(?:Home|About|Delivery|Shop|Market|TerritoryManage)Layout\\.calculate\\s*\\(\\s*"
          + "width\\s*,\\s*height\\s*,\\s*(?:controller\\.state\\(\\)|state)\\s*\\)");

  @Test
  void activeScreensUseMeasuredCommonViewsForRenderAndInput() throws Exception {
    Path root = repositoryRoot();
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java",
        "targets/neoforge-1.21.1/src/main/java")) {
      Path sourceRoot = root.resolve(relative);
      List<Path> screens = activeScreenFiles(sourceRoot);
      int screenClasses = screens.stream().mapToInt(file -> {
        try {
          return countMatches(read(file), "\\bextends\\s+Screen\\b");
        } catch (IOException failure) {
          throw new IllegalStateException(failure);
        }
      }).sum();
      assertTrue(screenClasses >= 19, relative + " active screen inventory unexpectedly shrank");
      for (Path file : screens) {
        String source = read(file);
        String name = relative + ":" + sourceRoot.relativize(file).toString();
        assertFalse(source.contains("UiTextMetrics.APPROXIMATE"),
            name + " must use target font metrics for production layout/hitboxes");
        assertFalse(APPROXIMATE_CONVENIENCE.matcher(source).find(),
            name + " must not call an approximate convenience layout overload");
        assertFalse(DIRECT_DRAW.matcher(source).find(),
            name + " must delegate rendering to a common semantic View");
        assertTrue(source.contains("com.mo.economy_system.ui."),
            name + " must depend on a common UI contract");
        if (source.contains("mouseClicked") || source.contains("mouseReleased")
            || source.contains("mouseScrolled") || source.contains("keyPressed")) {
          assertTrue(source.contains("Layout."), name + " input/hitbox path must derive common layout");
        }
      }
    }
  }

  @Test
  void commonViewsDoNotSynthesizeSemanticIconsFromText() throws Exception {
    Path root = repositoryRoot().resolve("common/src/main/java/com/mo/economy_system/ui");
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        String source = read(file);
        assertFalse(source.contains("substring(0, 1)"), file.toString());
        assertFalse(source.contains("substring(0,1)"), file.toString());
        assertFalse(source.contains("icon.name()"), file.toString());
      }
    }
  }

  @Test
  void sharedGuiResourcesHaveOneCommonSourceAuthority() throws Exception {
    Path root = repositoryRoot();
    Path icons = root.resolve("common/src/main/resources/assets/economy_system/textures/gui/icons");
    assertTrue(Files.isDirectory(icons), "common GUI icon authority is missing");
    try (Stream<Path> files = Files.list(icons)) {
      assertTrue(files.filter(path -> path.getFileName().toString().endsWith(".png")).count() >= 18,
          "common GUI icon authority unexpectedly incomplete");
    }
    for (String target : List.of("targets/forge-1.20.1", "targets/neoforge-1.21.1")) {
      Path targetGui = root.resolve(target).resolve(
          "src/main/resources/assets/economy_system/textures/gui");
      assertFalse(Files.exists(targetGui), target + " must not duplicate shared GUI textures");
    }
    String renderer = read(root.resolve(
        "common/src/main/java/com/mo/economy_system/ui/renderer/EconomyUiRenderer.java"));
    assertTrue(renderer.contains("void translatedIconButton"));
    assertTrue(renderer.contains("void icon"));
    assertTrue(renderer.contains("void itemDisplayName") || renderer.contains("itemDisplayName("));
  }

  private static List<Path> activeScreenFiles(Path sourceRoot) throws IOException {
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      return files.filter(path -> path.toString().endsWith(".java"))
          .filter(path -> !sourceRoot.relativize(path).toString().replace('\\', '/')
              .startsWith("com/mo/economy_system/screen/"))
          .filter(path -> {
            try {
              return read(path).contains("extends Screen");
            } catch (IOException failure) {
              throw new IllegalStateException(failure);
            }
          })
          .toList();
    }
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static int countMatches(String source, String regex) {
    int count = 0;
    var matcher = Pattern.compile(regex).matcher(source);
    while (matcher.find()) count++;
    return count;
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) {
      current = current.getParent();
    }
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}
