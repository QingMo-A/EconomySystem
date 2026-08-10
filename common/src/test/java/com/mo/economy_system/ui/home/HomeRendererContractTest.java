package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Source-level contract checks for the pixel-sensitive Home target adapters. */
class HomeRendererContractTest {
  @Test
  void iconButtonGeometryIsTargetExplicitAndUsesCommonDimensions() throws IOException {
    assertEqualsReferenceGeometry();
    String contract = read(repositoryRoot().resolve(
        "common/src/main/java/com/mo/economy_system/ui/renderer/EconomyUiRenderer.java"));
    assertFalse(contract.contains("default void translatedIconButton"));
    assertFalse(contract.contains("default void scaledIconText"));
    assertFalse(contract.contains("default void scaledIconStyledText"));
    assertFalse(contract.contains("default UiTextMetrics metrics"));

    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201UiRenderer.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211UiRenderer.java")) {
      String source = read(repositoryRoot().resolve(relative));
      assertTrue(source.contains("@Override public void translatedIconButton"), relative);
      assertTrue(source.contains("EconomyUiRenderer.ICON_SIZE"), relative);
      assertTrue(source.contains("EconomyUiRenderer.ICON_ADVANCE"), relative);
      assertTrue(source.contains("textY - 1"), relative);
      assertTrue(source.contains("rect.x() + style.padding()"), relative);
      assertTrue(source.contains("font.lineHeight"), relative);
    }
  }

  @Test
  void physicalBackgroundContractIsSeparatedFromVirtualHomeView() throws IOException {
    String view = read(repositoryRoot().resolve(
        "common/src/main/java/com/mo/economy_system/ui/home/HomeView.java"));
    assertFalse(view.contains("renderer.fill(\n        new UiRect(0, 0"));
    for (String relative : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201HomeScreen.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211HomeScreen.java")) {
      String source = read(repositoryRoot().resolve(relative));
      assertTrue(source.contains("fillPhysicalBackground(width, height, HomeLayout.BACKGROUND_COLOR)"),
          relative);
      assertFalse(source.contains("UiTextMetrics.APPROXIMATE"), relative);
    }
  }

  private static void assertEqualsReferenceGeometry() {
    org.junit.jupiter.api.Assertions.assertEquals(10, EconomyUiRenderer.ICON_SIZE);
    org.junit.jupiter.api.Assertions.assertEquals(14, EconomyUiRenderer.ICON_ADVANCE);
  }

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
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
