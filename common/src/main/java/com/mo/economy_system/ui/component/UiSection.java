package com.mo.economy_system.ui.component;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Lightweight section surface for grouping content without adding another full outlined card. */
public final class UiSection {
  private UiSection() {}

  public static void render(EconomyUiRenderer renderer, UiRect rect, boolean hovered) {
    render(renderer, rect, 0, hovered);
  }

  public static void render(EconomyUiRenderer renderer, UiRect rect, int accent, boolean hovered) {
    renderer.fill(rect, hovered ? EconomyUiTheme.Surface.SECTION_HOVER : EconomyUiTheme.Surface.SECTION);
    if (accent != 0) {
      renderer.fill(new UiRect(rect.x(), rect.y(), Math.min(2, rect.width()), rect.height()), accent);
    }
  }

  public static void selectionGlow(EconomyUiRenderer renderer, UiRect rect, int accent) {
    if (rect.width() <= 2 || rect.height() <= 2) return;
    border(renderer, new UiRect(rect.x() - 1, rect.y() - 1, rect.width() + 2, rect.height() + 2),
        withAlpha(accent, 0x36));
    border(renderer, rect, accent);
    border(renderer, new UiRect(rect.x() + 1, rect.y() + 1, rect.width() - 2, rect.height() - 2),
        withAlpha(accent, 0x55));
  }

  public static void divider(EconomyUiRenderer renderer, int x, int y, int width) {
    if (width <= 0) return;
    renderer.fill(new UiRect(x, y, width, 1), EconomyUiTheme.Surface.HAIRLINE);
  }

  private static void border(EconomyUiRenderer renderer, UiRect rect, int color) {
    renderer.fill(new UiRect(rect.x(), rect.y(), rect.width(), 1), color);
    renderer.fill(new UiRect(rect.x(), rect.bottom() - 1, rect.width(), 1), color);
    renderer.fill(new UiRect(rect.x(), rect.y(), 1, rect.height()), color);
    renderer.fill(new UiRect(rect.right() - 1, rect.y(), 1, rect.height()), color);
  }

  private static int withAlpha(int argbOrRgb, int alpha) {
    return ((alpha & 0xFF) << 24) | (argbOrRgb & 0x00FFFFFF);
  }
}
