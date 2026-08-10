package com.mo.economy_system.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.UiChromePlan;
import com.mo.economy_system.ui.renderer.UiFillCommand;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Exact glow/style gates for the strict UI reference closure. */
class UiStrictChromeReferenceTest {
  @Test
  void topButtonUsesLegacyStripedGlowParameters() {
    UiButtonStyle style = EconomyUiTheme.MARKET_TOP_SALES_BUTTON;
    assertEquals(4, style.stripeWidth());
    assertEquals(0xCC, style.stripeAlpha());
    assertEquals(0xFF, style.stripeAlphaHover());
    assertEquals(7, style.glowHeight());
    assertEquals(36, style.glowAlphaStart());
    assertEquals(4, style.glowAlphaStep());
    assertEquals(List.of(36, 32, 28, 24, 20, 16, 12), glowRows(new UiRect(10, 20, 84, 24), style));
  }

  @Test
  void compactActionAndPageButtonsUseFourRowGlow() {
    for (UiButtonStyle style : List.of(
        EconomyUiTheme.MARKET_ACTION_BUY,
        EconomyUiTheme.MARKET_ACTION_REMOVE,
        EconomyUiTheme.MARKET_ACTION_DELIVER,
        EconomyUiTheme.MARKET_ACTION_CONFIRM,
        EconomyUiTheme.MARKET_ACTION_CANCEL,
        EconomyUiTheme.DELIVERY_CLAIM_BUTTON,
        EconomyUiTheme.SHOP_PAGE_BUTTON,
        EconomyUiTheme.MARKET_PAGE_BUTTON,
        EconomyUiTheme.DELIVERY_PAGE_BUTTON,
        EconomyUiTheme.TERRITORY_PAGE_BUTTON,
        EconomyUiTheme.MARKET_BUTTON)) {
      assertEquals(3, style.stripeWidth());
      assertEquals(0xCC, style.stripeAlpha());
      assertEquals(0xFF, style.stripeAlphaHover());
      assertEquals(4, style.glowHeight());
      assertEquals(36, style.glowAlphaStart());
      assertEquals(4, style.glowAlphaStep());
      assertEquals(List.of(36, 32, 28, 24), glowRows(new UiRect(10, 20, 80, 24), style));
    }
  }

  @Test
  void territoryActionRemainsSixRowRegression() {
    UiButtonStyle style = EconomyUiTheme.TERRITORY_BUTTON;
    assertEquals(4, style.stripeWidth());
    assertEquals(6, style.glowHeight());
    assertEquals(List.of(36, 32, 28, 24, 20, 16), glowRows(new UiRect(10, 20, 100, 24), style));
  }

  @Test
  void disabledPageStyleRetainsLegacyValues() {
    UiButtonStyle style = EconomyUiTheme.SHOP_PAGE_BUTTON_DISABLED;
    assertEquals(0x6F7F8C, style.accent());
    assertEquals(EconomyUiTheme.TEXT_LOCKED, style.textColor());
    assertEquals(0x30, style.backgroundAlpha());
    assertEquals(0x30, style.backgroundAlphaHover());
    assertEquals(3, style.stripeWidth());
    assertEquals(0x50, style.stripeAlpha());
    assertEquals(0x50, style.stripeAlphaHover());
    assertEquals(0, style.glowHeight());
    assertEquals(0x20, style.borderAlpha());
    assertEquals(0x20, style.borderAlphaHover());
    assertEquals(6, style.padding());
    assertFalse(style.textShadow());
  }

  private static List<Integer> glowRows(UiRect rect, UiButtonStyle style) {
    return UiChromePlan.buttonChrome(rect, style, true, true).stream()
        .filter(command -> command.rect().x() == rect.x() + style.stripeWidth()
            && command.rect().width() == rect.width() - style.stripeWidth()
            && command.rect().height() == 1
            && command.rect().y() < rect.y() + style.glowHeight())
        .map(UiFillCommand::argb)
        .map(argb -> (argb >>> 24) & 0xFF)
        .toList();
  }
}
