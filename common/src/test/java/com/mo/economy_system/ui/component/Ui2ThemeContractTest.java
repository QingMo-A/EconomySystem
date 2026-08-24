package com.mo.economy_system.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import org.junit.jupiter.api.Test;

class Ui2ThemeContractTest {
  @Test
  void spacingScaleIsMonotonicAndAnchoredToFourPixels() {
    assertEquals(4, EconomyUiTheme.Spacing.MICRO);
    assertTrue(EconomyUiTheme.Spacing.MICRO < EconomyUiTheme.Spacing.SMALL);
    assertTrue(EconomyUiTheme.Spacing.SMALL < EconomyUiTheme.Spacing.COMPACT);
    assertTrue(EconomyUiTheme.Spacing.COMPACT < EconomyUiTheme.Spacing.MEDIUM);
    assertTrue(EconomyUiTheme.Spacing.MEDIUM < EconomyUiTheme.Spacing.SECTION);
    assertTrue(EconomyUiTheme.Spacing.SECTION < EconomyUiTheme.Spacing.PAGE);
  }

  @Test
  void semanticStateColorsKeepWarningsRedAndOtherStatesDistinct() {
    assertEquals(EconomyUiTheme.TEXT_ERROR, EconomyUiTheme.State.WARNING);
    assertNotEquals(EconomyUiTheme.State.SUCCESS, EconomyUiTheme.State.WARNING);
    assertNotEquals(EconomyUiTheme.State.INFO, EconomyUiTheme.State.DANGER);
    assertNotEquals(EconomyUiTheme.State.NEUTRAL, EconomyUiTheme.State.SUCCESS);
  }

  @Test
  void itemSlotExposesTheRequiredSharedStates() {
    assertEquals(6, UiItemSlot.State.values().length);
    assertEquals(UiItemSlot.State.CLAIMED, UiItemSlot.State.valueOf("CLAIMED"));
    assertEquals(UiItemSlot.State.SELECTED, UiItemSlot.State.valueOf("SELECTED"));
  }

  @Test
  void selectedItemSlotKeepsTheNormalSurfaceAndUsesBorderGlowOnly() {
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    UiRect slot = new UiRect(10, 20, 22, 22);

    UiItemSlot.render(renderer, slot, UiItemSlot.State.SELECTED, EconomyUiTheme.MARKET_ACCENT);

    assertEquals(EconomyUiTheme.Surface.ITEM_SLOT, renderer.paints().get(0).argb());
    assertFalse(renderer.paints().stream().anyMatch(paint ->
        paint.rect().width() == 3 && paint.rect().height() == slot.height()),
        "selection must not reintroduce a solid left accent stripe");
    assertTrue(renderer.paints().stream().anyMatch(paint -> paint.argb() == EconomyUiTheme.MARKET_ACCENT),
        "selection must render the bright accent edge");
  }

  @Test
  void motionTokensStaySubtle() {
    assertTrue(EconomyUiTheme.Motion.HOVER_MILLIS <= 150);
    assertTrue(EconomyUiTheme.Motion.SELECTION_MILLIS <= 180);
    assertTrue(EconomyUiTheme.Motion.PANEL_ENTER_MILLIS <= 220);
  }
}
