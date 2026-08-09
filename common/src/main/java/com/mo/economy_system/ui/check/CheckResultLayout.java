package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Stable virtual layout and row capacity for checked-file result pages. */
public final class CheckResultLayout {
  private CheckResultLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, CheckResultState state) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    int pad = EconomyUiTheme.PANEL_PADDING;
    UiRect title = new UiRect(pad, pad, width - pad * 2, 18);
    UiRect search = new UiRect(pad + 8, 44, Math.min(240, width - pad * 2 - 16), 20);
    UiRect searchCard = new UiRect(search.x() - 4, search.y() - 3, search.width() + 8, search.height() + 6);
    UiRect status = new UiRect(pad, 76, width - pad * 2, 82);
    int buttonY = Math.max(170, height - pad - 22);
    UiRect retry = new UiRect(Math.max(pad, width - pad - 152), buttonY, 70, 22);
    UiRect back = new UiRect(Math.max(pad, width - pad - 76), buttonY, 64, 22);
    UiRect rows = new UiRect(pad, 166, width - pad * 2, Math.max(1, buttonY - 174));
    int visibleRows = Math.max(1, rows.height() / 20);
    return new Layout(scale, title, search, searchCard, status, rows, retry, back, visibleRows);
  }

  public record Layout(
      UiScale scale,
      UiRect title,
      UiRect search,
      UiRect searchCard,
      UiRect status,
      UiRect rows,
      UiRect retry,
      UiRect back,
      int visibleRows) {}
}
