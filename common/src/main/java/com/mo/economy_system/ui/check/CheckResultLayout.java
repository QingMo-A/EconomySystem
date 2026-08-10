package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Stable virtual layout and row capacity for checked-file result pages. */
public final class CheckResultLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private CheckResultLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight, CheckResultState state) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    // Match the old native screen: centered title, native search field at y=62,
    // metadata/status at the top-left, and a twelve-pixel plain row list.
    UiRect title = new UiRect(0, 18, width, 14);
    int searchWidth = Math.max(1, Math.min(220, width - 24));
    UiRect search = new UiRect(12, 62, searchWidth, 18);
    UiRect searchCard = new UiRect(Math.max(0, search.x() - 4), Math.max(0, search.y() - 3),
        search.width() + 8, search.height() + 6);
    UiRect status = new UiRect(12, 38, Math.max(1, width - 24), 42);
    UiRect rows = new UiRect(12, 92, Math.max(1, width - 24), Math.max(1, height - 92));
    // Keep controller actions available to the target shells without introducing
    // modern visible buttons absent from the reference screen.
    UiRect retry = new UiRect(0, 0, 1, 1);
    UiRect back = new UiRect(0, 0, 1, 1);
    int visibleRows = Math.max(1, (height - 100) / 12);
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
