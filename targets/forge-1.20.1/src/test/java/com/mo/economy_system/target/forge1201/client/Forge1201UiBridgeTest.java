package com.mo.economy_system.target.forge1201.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.gui.GuiGraphics;
import org.junit.jupiter.api.Test;

class Forge1201UiBridgeTest {
  @Test
  void homeDeliveryAndTerritoryHaveNativeRenderers() {
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.HOME));
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.DELIVERY_BOX));
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.TERRITORY));
    assertTrue(Forge1201UiBridge.INSTANCE.create(EconomyUiRoute.HOME).orElseThrow()
        instanceof Forge1201HomeScreen);
  }

  @Test
  void sharedPagesUseNativeShells() {
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.SHOP));
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.MARKET));
    assertTrue(Forge1201UiBridge.INSTANCE.create(EconomyUiRoute.MARKET).orElseThrow()
        instanceof Forge1201MarketScreen);
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.ABOUT));
    assertTrue(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.BALANCE_LOG));
    assertTrue(Forge1201UiBridge.INSTANCE.create(EconomyUiRoute.ABOUT).orElseThrow()
        instanceof Forge1201AboutScreen);
    assertTrue(Forge1201UiBridge.INSTANCE.create(EconomyUiRoute.BALANCE_LOG).orElseThrow()
        instanceof Forge1201BalanceLogScreen);
  }

  @Test
  void territoryPilotUsesCommonRendererAndControllerShell() throws Exception {
    assertTrue(EconomyUiRenderer.class.isAssignableFrom(Forge1201UiRenderer.class));
    assertTrue(java.util.Arrays.stream(Forge1201TerritoryManageScreen.class.getDeclaredFields())
        .anyMatch(field -> field.getType() == com.mo.economy_system.ui.territory.TerritoryManageController.class));
  }

  @Test
  void aboutAndPurchaseShellsConsumeNavigationAndDisableVanillaBlur() throws Exception {
    assertTrue(Forge1201AboutScreen.class.getDeclaredMethod("tick") != null);
    assertTrue(Forge1201ShopPurchaseScreen.class.getDeclaredMethod("renderBackground",
        GuiGraphics.class) != null);
  }

  @Test
  void marketCreateSyncsControllerQuantityAndUsesMainInventoryCapacityOnly() throws Exception {
    String source = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201MarketCreateScreen.java"));
    assertTrue(source.contains("private boolean syncingInputs"));
    assertTrue(source.contains("syncValue(quantity"));
    assertTrue(source.contains("if (!syncingInputs)"));
    String purchase = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201ShopPurchaseScreen.java"));
    assertTrue(purchase.contains("player.getInventory().items"));
    assertTrue(purchase.contains("item.getMaxStackSize()"));
    assertTrue(purchase.contains("stack.getMaxStackSize()"));
    assertFalse(purchase.contains("getArmorSlots"));
    assertFalse(purchase.contains("offhand"));
  }

  @Test
  void marketInventoryCountUsesNativeDecorationAfterItemInOneRenderer() throws Exception {
    assertTrue(EconomyUiRenderer.class.getDeclaredMethod("itemWithCount",
        String.class, int.class, com.mo.economy_system.ui.geometry.UiRect.class) != null);
    String source = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201UiRenderer.java"));
    assertTrue(source.contains("void itemWithCount"));
    int item = source.indexOf("graphics.renderItem(stack, x, y)");
    int decorations = source.indexOf("graphics.renderItemDecorations(font, stack, x, y, Integer.toString(count))");
    assertTrue(item >= 0 && decorations > item,
        "Forge must paint native item count after the item in one scaled pose");
  }

  @Test
  void marketCreateReturnRefreshesExistingMarketScreenAndCompletionScrolls() throws Exception {
    String market = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201MarketScreen.java"));
    String create = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201MarketCreateScreen.java"));
    assertTrue(market.contains("void refreshData"));
    assertTrue(market.contains("new MarketEvent.Initialize"));
    assertTrue(create.contains("refreshData()"));
    assertTrue(create.contains("mouseScrolled"));
    assertTrue(create.contains("CompletionMoved"));
    assertFalse(create.contains(".limit(MarketCreateLayout.COMPLETION_MAX_ROWS)"));
  }

  @Test
  void activeNativeInputsUseOneSharedStyleAdapter() throws Exception {
    for (String file : new String[]{
        "Forge1201MarketCreateScreen.java", "Forge1201MarketScreen.java",
        "Forge1201ShopScreen.java", "Forge1201ShopPurchaseScreen.java",
        "Forge1201DeliveryBoxScreen.java", "Forge1201BuffManageScreen.java",
        "Forge1201TerritoryDetailScreen.java", "Forge1201TerritoryInviteScreen.java",
        "Forge1201TerritoryListScreen.java"}) {
      String source = read(repositoryRoot().resolve(
          "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/" + file));
      assertTrue(source.contains("Forge1201UiInputAdapter.apply"), file);
    }
  }

  @Test
  void activeNativeInputsUseBottomAlignedTransparentSubclass() throws Exception {
    String widget = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/Forge1201UnderlinedEditBox.java"));
    assertTrue(widget.contains("extends EditBox"));
    assertTrue(widget.contains("height - font.lineHeight - BOTTOM_TEXT_PADDING"));
    for (String file : new String[]{
        "Forge1201MarketCreateScreen.java", "Forge1201MarketScreen.java",
        "Forge1201ShopScreen.java", "Forge1201ShopPurchaseScreen.java",
        "Forge1201DeliveryBoxScreen.java", "Forge1201BuffManageScreen.java",
        "Forge1201TerritoryDetailScreen.java", "Forge1201TerritoryInviteScreen.java",
        "Forge1201TerritoryListScreen.java"}) {
      String source = read(repositoryRoot().resolve(
          "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/" + file));
      assertTrue(source.contains("new Forge1201UnderlinedEditBox"), file);
      assertFalse(source.contains("new EditBox"), file);
    }
    String fileCheck = read(repositoryRoot().resolve(
        "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/network/Forge1201ClientFileCheckScreens.java"));
    assertTrue(fileCheck.contains("new Forge1201UnderlinedEditBox"));
    assertFalse(fileCheck.contains("new EditBox"));
  }

  @Test
  void homeShopMarketAndAboutUseSettledStaticLayouts() throws Exception {
    for (String file : new String[]{"Forge1201HomeScreen.java", "Forge1201ShopScreen.java",
        "Forge1201MarketScreen.java", "Forge1201AboutScreen.java"}) {
      String source = read(repositoryRoot().resolve(
          "targets/forge-1.20.1/src/main/java/com/mo/economy_system/target/forge1201/client/" + file));
      assertFalse(source.contains("OpenAnimation"), file);
      assertFalse(source.contains("animationStartedAtNanos"), file);
      assertFalse(source.contains("animationProgress()"), file);
      assertTrue(source.contains("1.0f"), file);
    }
  }

  private static String read(Path path) throws Exception {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath();
    while (current != null && !Files.exists(current.resolve("settings.gradle"))) current = current.getParent();
    if (current == null) throw new IllegalStateException("repository root not found");
    return current;
  }
}
