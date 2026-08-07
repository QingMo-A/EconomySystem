package com.mo.economy_system.target.neoforge1211.client;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiRoute;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import org.junit.jupiter.api.Test;

class NeoForge1211UiBridgeTest {
    @Test
    void baselineProvidesEverySharedRoute() {
        for (EconomyUiRoute route : EconomyUiRoute.values()) {
            assertTrue(NeoForge1211UiBridge.INSTANCE.supports(route), route.name());
        }
    }

    @Test
    void territoryPilotUsesCommonRendererAndControllerShell() {
        assertTrue(EconomyUiRenderer.class.isAssignableFrom(NeoForge1211UiRenderer.class));
        assertTrue(java.util.Arrays.stream(NeoForge1211TerritoryManageScreen.class.getDeclaredFields())
                .anyMatch(field -> field.getType() == com.mo.economy_system.ui.territory.TerritoryManageController.class));
    }
}
