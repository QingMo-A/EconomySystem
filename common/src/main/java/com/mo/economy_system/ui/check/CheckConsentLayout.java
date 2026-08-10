package com.mo.economy_system.ui.check;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Virtual-coordinate layout shared by Forge and NeoForge consent shells. */
public final class CheckConsentLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private CheckConsentLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight) {
    UiScale scale = UiScale.fit(
        physicalWidth, physicalHeight, EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth();
    int height = scale.virtualHeight();
    // The pre-Bridge screen was deliberately plain: all text was centered on the
    // viewport and the two native buttons were anchored to the bottom edge.  Keep
    // the card record as the full viewport for compatibility with callers that use
    // it as an interaction bounds container; the view does not render card chrome.
    UiRect card = new UiRect(0, 0, width, height);
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
        new UiRect(0, 35, width, 14),
        new UiRect(0, 57, width, 14),
        new UiRect(0, 73, width, 14),
        new UiRect(0, 89, width, 14),
        new UiRect(0, 111, width, 14),
        new UiRect(0, 127, width, 14),
        allow,
        decline);
  }

  public record Layout(
      UiScale scale,
      UiRect card,
      UiRect title,
      UiRect requester,
      UiRect type,
      UiRect folder,
      UiRect dataNotice,
      UiRect noContentNotice,
      UiRect allow,
      UiRect decline) {}
}
