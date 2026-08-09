package com.mo.economy_system.ui.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.UiChromePlan;
import com.mo.economy_system.ui.renderer.UiFillCommand;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.text.UiText;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryManageUiPrimitiveTest {
    private static final UiTextMetrics METRICS = new UiTextMetrics() {
        @Override public int width(String text) { return text.length() * 5; }
        @Override public int lineHeight() { return 9; }
    };

    @Test
    void cardChromeMatchesLegacy1211ReferenceExactly() {
        UiRect rect = new UiRect(10, 20, 100, 40);
        assertEquals(List.of(
                fill(rect, 0x801A1A2A),
                fill(new UiRect(10, 20, 100, 1), 0xFF4A5568),
                fill(new UiRect(10, 59, 100, 1), 0xFF4A5568),
                fill(new UiRect(109, 20, 1, 40), 0xFF4A5568),
                fill(new UiRect(10, 20, 3, 40), 0xCC9B59B6)),
                UiChromePlan.cardChrome(rect, EconomyUiTheme.TERRITORY_CARD, false));
        assertEquals(List.of(
                fill(rect, 0x901A1A2A),
                fill(new UiRect(10, 20, 100, 1), 0xFF6A7588),
                fill(new UiRect(10, 59, 100, 1), 0xFF6A7588),
                fill(new UiRect(109, 20, 1, 40), 0xFF6A7588),
                fill(new UiRect(10, 20, 3, 40), 0xFF9B59B6)),
                UiChromePlan.cardChrome(rect, EconomyUiTheme.TERRITORY_CARD, true));
    }

    @Test
    void actionButtonChromeMatchesLegacy1211ReferenceExactly() {
        UiRect rect = new UiRect(10, 20, 100, 22);
        assertEquals(List.of(
                fill(rect, 0x55000000),
                fill(new UiRect(10, 20, 4, 22), 0xCC9B59B6),
                fill(new UiRect(109, 20, 1, 22), 0x25FFFFFF),
                fill(new UiRect(14, 41, 96, 1), 0x25FFFFFF)),
                UiChromePlan.buttonChrome(rect, EconomyUiTheme.TERRITORY_BUTTON, false, true));
        assertEquals(List.of(
                fill(rect, 0x70000000),
                fill(new UiRect(10, 20, 4, 22), 0xFF9B59B6),
                fill(new UiRect(14, 20, 96, 1), 0x249B59B6),
                fill(new UiRect(14, 21, 96, 1), 0x209B59B6),
                fill(new UiRect(14, 22, 96, 1), 0x1C9B59B6),
                fill(new UiRect(14, 23, 96, 1), 0x189B59B6),
                fill(new UiRect(14, 24, 96, 1), 0x149B59B6),
                fill(new UiRect(14, 25, 96, 1), 0x109B59B6),
                fill(new UiRect(109, 20, 1, 22), 0x40FFFFFF),
                fill(new UiRect(14, 41, 96, 1), 0x40FFFFFF)),
                UiChromePlan.buttonChrome(rect, EconomyUiTheme.TERRITORY_BUTTON, true, true));
    }

    @Test
    void pageButtonChromeHasFourBordersAndEnabledHighlight() {
        UiRect rect = new UiRect(10, 20, 100, 24);
        assertEquals(List.of(
                fill(rect, 0xB03A7ABF),
                fill(new UiRect(10, 20, 100, 1), 0xFF4A8ACF),
                fill(new UiRect(10, 43, 100, 1), 0xFF4A8ACF),
                fill(new UiRect(10, 20, 1, 24), 0xFF4A8ACF),
                fill(new UiRect(109, 20, 1, 24), 0xFF4A8ACF),
                fill(new UiRect(12, 21, 96, 1), 0x60FFFFFF)),
                UiChromePlan.buttonChrome(rect, EconomyUiTheme.PAGE_BUTTON, false, true));
        assertEquals(List.of(
                fill(rect, 0x602A2A3A),
                fill(new UiRect(10, 20, 100, 1), 0xFF3A3A4A),
                fill(new UiRect(10, 43, 100, 1), 0xFF3A3A4A),
                fill(new UiRect(10, 20, 1, 24), 0xFF3A3A4A),
                fill(new UiRect(109, 20, 1, 24), 0xFF3A3A4A)),
                UiChromePlan.buttonChrome(rect, EconomyUiTheme.PAGE_BUTTON_DISABLED, false, false));
    }

    @Test
    void everySemanticIconMapsToACommon64PixelTexture() {
        for (UiIcon icon : UiIcon.values()) {
            assertEquals(64, icon.sourceWidth(), icon.name());
            assertEquals(64, icon.sourceHeight(), icon.name());
            assertNotNull(getClass().getClassLoader().getResource(
                    "assets/economy_system/textures/gui/icons/" + icon.fileName() + ".png"),
                    icon.name());
        }
    }

    @Test
    void truncationPreservesLeadingUuidPrefix() {
        assertEquals("abcdef...", UiText.truncate(METRICS, "abcdef012345", 45));
        assertEquals("short", UiText.truncate(METRICS, "short", 100));
    }

    private static UiFillCommand fill(UiRect rect, int argb) {
        return new UiFillCommand(rect, argb);
    }
}
