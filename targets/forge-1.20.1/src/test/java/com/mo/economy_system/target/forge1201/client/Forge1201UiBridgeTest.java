package com.mo.economy_system.target.forge1201.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
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
}
