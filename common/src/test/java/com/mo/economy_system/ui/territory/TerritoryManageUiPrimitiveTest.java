package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.ui.text.UiText;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import org.junit.jupiter.api.Test;

class TerritoryManageUiPrimitiveTest {
    private static final UiTextMetrics METRICS = new UiTextMetrics() {
        @Override public int width(String text) { return text.length() * 5; }
        @Override public int lineHeight() { return 9; }
    };

    @Test
    void cardAndActionStylesExposeReferenceAlpha() {
        assertEquals(3, EconomyUiTheme.TERRITORY_CARD.accentWidth());
        assertEquals(0xCC, EconomyUiTheme.TERRITORY_CARD.accentAlpha());
        assertEquals(0xFF, EconomyUiTheme.TERRITORY_CARD.accentAlphaHover());
        assertEquals(0x55, EconomyUiTheme.TERRITORY_BUTTON.backgroundAlpha());
        assertEquals(0x70, EconomyUiTheme.TERRITORY_BUTTON.backgroundAlphaHover());
        assertEquals(4, EconomyUiTheme.TERRITORY_BUTTON.stripeWidth());
        assertEquals(0xCC, EconomyUiTheme.TERRITORY_BUTTON.stripeAlpha());
        assertEquals(0xFF, EconomyUiTheme.TERRITORY_BUTTON.stripeAlphaHover());
        assertEquals(6, EconomyUiTheme.TERRITORY_BUTTON.glowHeight());
        assertEquals(36, EconomyUiTheme.TERRITORY_BUTTON.glowAlphaStart());
        assertEquals(4, EconomyUiTheme.TERRITORY_BUTTON.glowAlphaStep());
    }

    @Test
    void truncationPreservesLeadingUuidPrefix() {
        assertEquals("abcdef...", UiText.truncate(METRICS, "abcdef012345", 45));
        assertEquals("short", UiText.truncate(METRICS, "short", 100));
    }
}
