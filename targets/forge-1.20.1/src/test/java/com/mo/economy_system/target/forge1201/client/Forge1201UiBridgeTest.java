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
    assertNotNull(Forge1201UiBridge.INSTANCE.create(EconomyUiRoute.HOME).orElseThrow());
  }

  @Test
  void unportedPagesFailClosed() {
    assertFalse(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.SHOP));
    assertFalse(Forge1201UiBridge.INSTANCE.create(EconomyUiRoute.MARKET).isPresent());
    assertFalse(Forge1201UiBridge.INSTANCE.supports(EconomyUiRoute.ABOUT));
  }

  @Test
  void territoryPilotUsesCommonRendererAndControllerShell() throws Exception {
    assertTrue(EconomyUiRenderer.class.isAssignableFrom(Forge1201UiRenderer.class));
    assertTrue(java.util.Arrays.stream(Forge1201TerritoryManageScreen.class.getDeclaredFields())
        .anyMatch(field -> field.getType() == com.mo.economy_system.ui.territory.TerritoryManageController.class));
  }
}
