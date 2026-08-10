package com.mo.economy_system.ui.transfer;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Shared virtual-coordinate layout for checked-file transfer consent. */
public final class TransferConsentLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private TransferConsentLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    // Legacy transfer consent was a plain native screen.  Retain a full-viewport
    // interaction container for callers while keeping card chrome out of the view.
    UiRect card = new UiRect(0, 0, width, height);
    int contentX = 12;
    int contentWidth = Math.max(1, width - 24);
    List<UiRect> details = List.of(
        new UiRect(contentX, 42, contentWidth, 12),
        new UiRect(contentX, 54, contentWidth, 12),
        new UiRect(contentX, 66, contentWidth, 12),
        new UiRect(contentX, 78, contentWidth, 12),
        new UiRect(contentX, 90, contentWidth, 12));
    UiRect warning = new UiRect(contentX, 102, contentWidth, 14);
    UiRect allow;
    UiRect decline;
    if (width >= 220 && height >= 30) {
      int buttonY = height - 25;
      allow = new UiRect(width / 2 - 105, buttonY, 100, 20);
      decline = new UiRect(width / 2 + 5, buttonY, 100, 20);
    } else if (width >= 110 && height >= 55) {
      allow = new UiRect(width / 2 - 50, height - 50, 100, 20);
      decline = new UiRect(width / 2 - 50, height - 25, 100, 20);
    } else {
      allow = new UiRect(0, 0, 1, 1);
      decline = new UiRect(0, 0, 1, 1);
    }
    return new Layout(
        scale,
        card,
        new UiRect(0, 18, width, 14),
        details,
        warning,
        allow,
        decline);
  }

  public record Layout(
      UiScale scale,
      UiRect card,
      UiRect title,
      List<UiRect> details,
      UiRect warning,
      UiRect allow,
      UiRect decline) {
    public Layout {
      details = List.copyOf(details);
    }
  }
}
