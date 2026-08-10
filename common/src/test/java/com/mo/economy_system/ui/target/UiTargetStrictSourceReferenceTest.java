package com.mo.economy_system.ui.target;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.text.UiSearchPolicy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Source-level gates for target-owned native widgets and one-page hitbox policy. */
class UiTargetStrictSourceReferenceTest {
  private static final String POLICY = "com.mo.economy_system.ui.text.UiSearchPolicy.SEARCH_MAX_LENGTH";

  @Test
  void nativeSearchFieldsUseCommonMaxLengthAndRealHints() throws IOException {
    assertTrue(UiSearchPolicy.SEARCH_MAX_LENGTH == 50);
    for (TargetSearch target : targets()) {
      String source = read(target.path());
      assertTrue(source.contains("setMaxLength(" + POLICY + ")"), target.path());
      assertTrue(source.contains("setHint(Component.translatable(\"" + target.hintKey() + "\"))"), target.path());
      assertTrue(source.contains("setFocused(false)"), target.path());
      assertFalse(source.contains("setMaxLength(64)"), target.path());
    }
  }

  @Test
  void onePageNativeClickHandlersRequireVisiblePagination() throws IOException {
    for (String path : List.of(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201ShopScreen.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201MarketScreen.java",
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201DeliveryBoxScreen.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211ShopScreen.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211MarketScreen.java",
        "targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211DeliveryBoxScreen.java")) {
      String source = read(path);
      assertTrue(source.contains("controller.state().totalPages() > 1 && layout.previousButton().contains"), path);
      assertTrue(source.contains("controller.state().totalPages() > 1 && layout.nextButton().contains"), path);
    }
  }

  @Test
  void commonViewsOwnFrameOnlyAndGatePageDrawing() throws IOException {
    String shop = read("common/src/main/java/com/mo/economy_system/ui/shop/ShopView.java");
    String market = read("common/src/main/java/com/mo/economy_system/ui/market/MarketView.java");
    String delivery = read("common/src/main/java/com/mo/economy_system/ui/delivery/DeliveryView.java");
    assertFalse(shop.contains("translatedTextInRect(\"screen.shop.search\""));
    assertFalse(market.contains("translatedTextInRect(\"screen.market.search\""));
    assertFalse(delivery.contains("translatedTextInRect(\"screen.delivery_box.search\""));
    assertTrue(shop.contains("if (state.totalPages() > 1)"));
    assertTrue(market.contains("if (state.totalPages() > 1)"));
    assertTrue(delivery.contains("if (state.totalPages() > 1)"));
  }

  private static List<TargetSearch> targets() {
    return List.of(
        new TargetSearch("targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201ShopScreen.java", "text.shop.search_hint"),
        new TargetSearch("targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201MarketScreen.java", "screen.market.search_hint"),
        new TargetSearch("targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201DeliveryBoxScreen.java", "text.delivery_box.hint"),
        new TargetSearch("targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211ShopScreen.java", "text.shop.search_hint"),
        new TargetSearch("targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211MarketScreen.java", "screen.market.search_hint"),
        new TargetSearch("targets/neoforge-1.21.1/src/main/java/com/mo/economy_system/target/neoforge1211/client/NeoForge1211DeliveryBoxScreen.java", "text.delivery_box.hint"));
  }

  private static String read(String path) throws IOException {
    Path cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath();
    while (cursor != null) {
      Path candidate = cursor.resolve(path);
      if (Files.exists(candidate)) return Files.readString(candidate);
      cursor = cursor.getParent();
    }
    throw new java.nio.file.NoSuchFileException(path);
  }

  private record TargetSearch(String path, String hintKey) {}
}
