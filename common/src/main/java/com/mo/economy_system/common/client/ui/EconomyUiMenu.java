package com.mo.economy_system.common.client.ui;

import java.util.List;
import java.util.Objects;

/**
 * Shared menu definition. Targets render this model with their own Screen and
 * widget APIs; no Minecraft or loader type is allowed to cross this boundary.
 */
public final class EconomyUiMenu {
    private static final List<Entry> DEFAULT_ENTRIES = List.of(
            new Entry(EconomyUiRoute.SHOP, "button.home.shop"),
            new Entry(EconomyUiRoute.MARKET, "button.home.market"),
            new Entry(EconomyUiRoute.COMMISSIONS, "button.home.commissions"),
            new Entry(EconomyUiRoute.RECYCLE, "button.home.recycle"),
            new Entry(EconomyUiRoute.DELIVERY_BOX, "button.home.delivery_box"),
            new Entry(EconomyUiRoute.TERRITORY, "button.home.territory"),
            new Entry(EconomyUiRoute.ABOUT, "button.home.about"));

    private EconomyUiMenu() {
    }

    public static List<Entry> defaultEntries() {
        return DEFAULT_ENTRIES;
    }

    public record Entry(EconomyUiRoute route, String labelKey) {
        public Entry {
            Objects.requireNonNull(route, "route");
            if (labelKey == null || labelKey.isBlank()) {
                throw new IllegalArgumentException("labelKey cannot be blank");
            }
        }
    }
}
