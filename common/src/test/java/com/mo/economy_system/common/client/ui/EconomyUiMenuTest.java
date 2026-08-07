package com.mo.economy_system.common.client.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EconomyUiMenuTest {
    @Test
    void defaultMenuHasStableOrderAndRoutes() {
        List<EconomyUiMenu.Entry> entries = EconomyUiMenu.defaultEntries();

        assertEquals(List.of(
                EconomyUiRoute.SHOP,
                EconomyUiRoute.MARKET,
                EconomyUiRoute.DELIVERY_BOX,
                EconomyUiRoute.TERRITORY,
                EconomyUiRoute.ABOUT),
                entries.stream().map(EconomyUiMenu.Entry::route).toList());
        assertEquals("button.home.market", entries.get(1).labelKey());
    }

    @Test
    void menuEntryRejectsInvalidLabels() {
        assertThrows(IllegalArgumentException.class,
                () -> new EconomyUiMenu.Entry(EconomyUiRoute.HOME, " "));
    }
}
