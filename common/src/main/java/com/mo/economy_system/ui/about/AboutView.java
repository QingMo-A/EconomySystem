package com.mo.economy_system.ui.about;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic About-page renderer; target textures remain optional. */
public final class AboutView {
  private AboutView() {}

  public static void render(EconomyUiRenderer renderer, AboutState state,
                            AboutLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.icon(UiIcon.ABOUT, new UiRect(layout.title().x(), layout.title().y(), 12, 12));
    renderer.translatedText("screen.about.title", List.of(), layout.title().x() + 16,
        layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.about.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    renderer.card(layout.panel(), EconomyUiTheme.HOME_CARD, false);
    renderer.translatedText("screen.about.title", List.of(), layout.panel().x() + 12,
        layout.panel().y() + 8, EconomyUiTheme.TEXT_PRIMARY);
    renderer.text(state.modName(), layout.panel().x() + 12, layout.panel().y() + 28,
        EconomyUiTheme.TEXT_SECONDARY);
    renderer.translatedTextInRect("screen.about.author_name", List.of(state.author()),
        new UiRect(layout.panel().x() + 12, layout.panel().y() + 42,
            layout.panel().width() - 24, 14), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.about.github_url", List.of(state.githubUrl()), layout.github(),
        layout.github().contains(mouseX, mouseY) ? EconomyUiTheme.MARKET_ACCENT : EconomyUiTheme.TEXT_PRIMARY,
        UiTextAlignment.LEFT);
    renderer.translatedTextInRect(state.copied() ? "message.about.copy_github_url" : "screen.about.show_github_text",
        List.of(), layout.copyHint(), state.copied() ? EconomyUiTheme.TEXT_SUCCESS : EconomyUiTheme.TEXT_MUTED,
        UiTextAlignment.LEFT);
    renderer.translatedButton(layout.backButton(), EconomyUiTheme.HOME_ABOUT_BUTTON,
        "button.about.back", List.of(), layout.backButton().contains(mouseX, mouseY), true);
    renderer.card(layout.leftQr(), EconomyUiTheme.HOME_CARD, false);
    renderer.card(layout.rightQr(), EconomyUiTheme.HOME_CARD, false);
    renderer.texture("economy_system:textures/gui/vx.png", inset(layout.leftQr(), 6));
    renderer.texture("economy_system:textures/gui/zfb.png", inset(layout.rightQr(), 6));
  }

  private static UiRect inset(UiRect rect, int padding) {
    return new UiRect(rect.x() + padding, rect.y() + padding,
        Math.max(1, rect.width() - padding * 2), Math.max(1, rect.height() - padding * 2));
  }
}
