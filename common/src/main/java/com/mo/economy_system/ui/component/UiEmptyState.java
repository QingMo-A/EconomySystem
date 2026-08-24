package com.mo.economy_system.ui.component;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Consistent centered title + optional description for empty/loading/error content panes. */
public final class UiEmptyState {
  private UiEmptyState() {}

  public static void render(EconomyUiRenderer renderer, UiRect area, String titleKey, String descriptionKey) {
    int lineHeight = Math.max(1, renderer.metrics().lineHeight());
    int gap = EconomyUiTheme.Spacing.SMALL;
    boolean hasDescription = descriptionKey != null && !descriptionKey.isBlank();
    int totalHeight = lineHeight + (hasDescription ? gap + lineHeight : 0);
    int top = area.y() + Math.max(0, (area.height() - totalHeight) / 2);

    UiRect title = new UiRect(area.x() + EconomyUiTheme.Spacing.MEDIUM, top,
        Math.max(1, area.width() - EconomyUiTheme.Spacing.MEDIUM * 2), lineHeight);
    renderer.translatedTextInRect(titleKey, List.of(), title,
        EconomyUiTheme.Text.SECONDARY, UiTextAlignment.CENTER);

    if (hasDescription) {
      UiRect description = new UiRect(title.x(), top + lineHeight + gap, title.width(), lineHeight);
      renderer.translatedTextInRect(descriptionKey, List.of(), description,
          EconomyUiTheme.Text.MUTED, UiTextAlignment.CENTER);
    }
  }
}
