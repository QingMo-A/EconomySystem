package com.mo.economy_system.ui.renderer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiInputFrameStyle;
import com.mo.economy_system.ui.theme.UiInputStyle;
import org.junit.jupiter.api.Test;

class UiInputStyleTest {
  @Test
  void sharedStyleDefinesReadableTextAndNativeWidgetChromePolicy() {
    UiInputStyle style = EconomyUiTheme.INPUT_STYLE;
    assertEquals(EconomyUiTheme.TEXT_PRIMARY, style.textColor());
    assertEquals(EconomyUiTheme.TEXT_LOCKED, style.disabledTextColor());
    assertEquals(EconomyUiTheme.TEXT_MUTED, style.placeholderColor());
    assertFalse(style.textShadow());
    assertTrue(style.hideNativeBorder());
  }

  @Test
  void globalInputChromeIsTransparentWithOnlyAnUnderline() {
    UiInputFrameStyle style = EconomyUiTheme.MARKET_SEARCH_FRAME;
    assertEquals(0, style.background());
    assertEquals(0, style.top(false));
    assertEquals(0, style.left(false));
    assertEquals(0, style.right(false));

    UiRect rect = new UiRect(20, 30, 160, 18);
    var idle = UiInputFramePlan.frame(rect, style, false).commands();
    var focused = UiInputFramePlan.frame(rect, style, true).commands();
    assertEquals(1, idle.size());
    assertEquals(new UiRect(20, 47, 160, 1), idle.get(0).rect());
    assertEquals(EconomyUiTheme.CARD_BORDER, idle.get(0).argb());
    assertEquals(EconomyUiTheme.MARKET_ACCENT, focused.get(0).argb());
  }
}
