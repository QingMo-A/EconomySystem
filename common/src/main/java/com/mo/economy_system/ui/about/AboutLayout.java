package com.mo.economy_system.ui.about;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Pure layout for the About page and its optional target textures. */
public final class AboutLayout {
  private AboutLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight) {
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panelWidth = Math.min(420, Math.max(180, width - EconomyUiTheme.PANEL_PADDING * 2));
    int panelHeight = Math.min(170, Math.max(120, height - EconomyUiTheme.PANEL_PADDING * 2 - 120));
    int panelX = Math.max(0, (width - panelWidth) / 2);
    int panelY = EconomyUiTheme.PANEL_PADDING + 16;
    UiRect panel = new UiRect(panelX, panelY, panelWidth, panelHeight);
    UiRect title = new UiRect(EconomyUiTheme.PANEL_PADDING, EconomyUiTheme.PANEL_PADDING + 22, 220, 14);
    UiRect esc = new UiRect(Math.max(EconomyUiTheme.PANEL_PADDING, width - EconomyUiTheme.PANEL_PADDING - 90),
        EconomyUiTheme.PANEL_PADDING + 6, 90, 14);
    UiRect github = new UiRect(panelX + EconomyUiTheme.PANEL_PADDING, panelY + 58,
        Math.max(1, panelWidth - EconomyUiTheme.PANEL_PADDING * 2), 18);
    UiRect copyHint = new UiRect(panelX + EconomyUiTheme.PANEL_PADDING, panelY + 80,
        Math.max(1, panelWidth - EconomyUiTheme.PANEL_PADDING * 2), 14);
    int buttonWidth = Math.min(110, Math.max(90, panelWidth - EconomyUiTheme.PANEL_PADDING * 2));
    UiRect back = new UiRect(panelX + (panelWidth - buttonWidth) / 2,
        panelY + panelHeight - 34, buttonWidth, 22);
    int qrSize = Math.min(110, Math.max(60, (height - EconomyUiTheme.PANEL_PADDING * 2) / 3));
    int qrY = height - EconomyUiTheme.PANEL_PADDING - qrSize;
    UiRect leftQr = new UiRect(EconomyUiTheme.PANEL_PADDING, qrY, qrSize, qrSize);
    UiRect rightQr = new UiRect(width - EconomyUiTheme.PANEL_PADDING - qrSize, qrY, qrSize, qrSize);
    return new Layout(scale, panel, title, esc, github, copyHint, back, leftQr, rightQr);
  }

  public record Layout(UiScale scale, UiRect panel, UiRect title, UiRect esc, UiRect github,
                       UiRect copyHint, UiRect backButton, UiRect leftQr, UiRect rightQr) {}
}
