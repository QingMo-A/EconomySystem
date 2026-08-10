package com.mo.economy_system.ui.about;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.geometry.UiScale;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.theme.EconomyUiTheme;

/** Pure layout for the About page and its optional target textures. */
public final class AboutLayout {
  public static final int BACKGROUND_COLOR = 0xB0000000;
  private AboutLayout() {}

  public static Layout calculate(int physicalWidth, int physicalHeight) {
    return calculate(physicalWidth, physicalHeight, UiTextMetrics.APPROXIMATE, 1.0f);
  }

  public static Layout calculate(int physicalWidth, int physicalHeight, float animationProgress) {
    return calculate(physicalWidth, physicalHeight, UiTextMetrics.APPROXIMATE, animationProgress);
  }

  /** Calculates reference geometry using target-native metrics and opening offsets. */
  public static Layout calculate(int physicalWidth, int physicalHeight, UiTextMetrics metrics,
                                 float animationProgress) {
    if (metrics == null) metrics = UiTextMetrics.APPROXIMATE;
    UiScale scale = UiScale.fit(physicalWidth, physicalHeight,
        EconomyUiTheme.BASE_WIDTH, EconomyUiTheme.BASE_HEIGHT);
    int width = scale.virtualWidth(), height = scale.virtualHeight();
    int panelWidth = Math.min(420, Math.max(180, width - EconomyUiTheme.PANEL_PADDING * 2));
    int panelHeight = Math.min(170, Math.max(120, height - EconomyUiTheme.PANEL_PADDING * 2 - 120));
    int panelX = Math.max(0, (width - panelWidth) / 2);
    int panelY = EconomyUiTheme.PANEL_PADDING + 16;
    float progress = Math.max(0.0f, Math.min(1.0f, animationProgress));
    int leftOffset = AboutOpenAnimation.leftOffset(progress);
    int rightOffset = AboutOpenAnimation.rightOffset(progress);
    int panelOffset = leftOffset * -1;
    UiRect panel = new UiRect(panelX, panelY - panelOffset, panelWidth, panelHeight);
    // CardRenderer.drawVersionInfo receives the card's top edge (title text is drawn inside it).
    UiRect title = new UiRect(EconomyUiTheme.PANEL_PADDING + leftOffset,
        EconomyUiTheme.PANEL_PADDING, Math.max(1, Math.min(220, 16 + metrics.width("About") + 14)),
        Math.max(1, metrics.lineHeight() + 10));
    UiRect esc = new UiRect(Math.max(EconomyUiTheme.PANEL_PADDING, width - EconomyUiTheme.PANEL_PADDING - 90) + rightOffset,
        EconomyUiTheme.PANEL_PADDING + 6, 90, Math.max(1, metrics.lineHeight()));
    int lineHeight = Math.max(1, metrics.lineHeight());
    UiRect github = new UiRect(panelX + EconomyUiTheme.PANEL_PADDING, panel.y() + lineHeight * 4 + 13,
        Math.max(1, panelWidth - EconomyUiTheme.PANEL_PADDING * 2), 18);
    UiRect copyHint = new UiRect(panelX + EconomyUiTheme.PANEL_PADDING, panel.y() + lineHeight * 5 + 15,
        Math.max(1, panelWidth - EconomyUiTheme.PANEL_PADDING * 2), 14);
    int buttonWidth = Math.min(110, Math.max(90, panelWidth - EconomyUiTheme.PANEL_PADDING * 2));
    UiRect back = new UiRect(panelX + (panelWidth - buttonWidth) / 2,
        panel.y() + panelHeight - 34, buttonWidth, 22);
    int qrSize = Math.min(110, Math.max(60, (height - EconomyUiTheme.PANEL_PADDING * 2) / 3));
    int qrY = height - EconomyUiTheme.PANEL_PADDING - qrSize;
    UiRect leftQr = new UiRect(EconomyUiTheme.PANEL_PADDING + leftOffset, qrY, qrSize, qrSize);
    UiRect rightQr = new UiRect(width - EconomyUiTheme.PANEL_PADDING - qrSize + rightOffset, qrY, qrSize, qrSize);
    return new Layout(scale, metrics, panel, title, esc, github, copyHint, back, leftQr, rightQr,
        progress);
  }

  public record Layout(UiScale scale, UiTextMetrics metrics, UiRect panel, UiRect title,
                       UiRect esc, UiRect github, UiRect copyHint, UiRect backButton,
                       UiRect leftQr, UiRect rightQr, float animationProgress) {}
}
