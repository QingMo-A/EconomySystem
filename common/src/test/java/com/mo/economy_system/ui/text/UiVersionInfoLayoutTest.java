package com.mo.economy_system.ui.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.geometry.UiRect;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Dynamic title-card sizing must follow native translated metrics, not fixed widths. */
class UiVersionInfoLayoutTest {
  @Test
  void titleCardScalesToConfiguredContentMaximum() {
    UiTextMetrics metrics = new UiTextMetrics() {
      @Override public int width(String text) { return 8; }
      @Override public int lineHeight() { return 11; }
      @Override public int translatedWidth(String key, List<String> arguments) { return 220; }
    };
    UiVersionInfoLayout.Result result = UiVersionInfoLayout.calculate(metrics,
        "screen.shop.title", List.of(), 12, 348, 120);
    assertEquals(234, result.contentWidth());
    assertEquals(120, result.maxContentWidth());
    assertEquals(120f / 234f, result.contentScale(), 0.00001f);
    assertEquals(new UiRect(12, 327, 136, 21), result.card());
  }

  @Test
  void shortLocalizedTitleRemainsAtNaturalScaleAndUsesLineHeight() {
    UiTextMetrics metrics = new UiTextMetrics() {
      @Override public int width(String text) { return 8; }
      @Override public int lineHeight() { return 9; }
      @Override public int translatedWidth(String key, List<String> arguments) { return 50; }
    };
    UiVersionInfoLayout.Result result = UiVersionInfoLayout.calculate(metrics,
        "screen.delivery_box.title", List.of(), 12, 348, 140);
    assertEquals(64, result.contentWidth());
    assertEquals(1.0f, result.contentScale());
    assertTrue(result.card().equals(new UiRect(12, 329, 80, 19)));
  }
}
