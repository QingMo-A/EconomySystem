package com.mo.economy_system.ui.component;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Quiet UI 2.0 surface with optional semantic accent; intentionally avoids decorative full borders. */
public final class UiPanel {
  private UiPanel() {}

  public static void render(EconomyUiRenderer renderer, UiRect rect, boolean hovered) {
    render(renderer, rect, 0, 0, hovered);
  }

  public static void render(EconomyUiRenderer renderer, UiRect rect, int accent, int accentWidth,
                            boolean hovered) {
    renderer.fill(rect, hovered ? EconomyUiTheme.Surface.PANEL_HOVER : EconomyUiTheme.Surface.PANEL);
    if (accentWidth > 0) {
      renderer.fill(new UiRect(rect.x(), rect.y(), Math.min(rect.width(), accentWidth), rect.height()), accent);
    }
  }
}
