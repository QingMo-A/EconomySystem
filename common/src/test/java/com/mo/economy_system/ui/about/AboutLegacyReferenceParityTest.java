package com.mo.economy_system.ui.about;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.geometry.UiRect;
import org.junit.jupiter.api.Test;

/** Exact title/panel/texture assertions transcribed from the legacy About screen. */
class AboutLegacyReferenceParityTest {
  @Test
  void legacyAboutUsesLocalizedTitleAndAboutGrayCards() {
    AboutState state = new AboutState("EconomySystem", "QingMo HanHanYu",
        "https://github.com/QingMo-A/EconoeySystem", false);
    AboutLayout.Layout layout = AboutLayout.calculate(640, 360, 1.0f);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    AboutView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("scaledIconTranslatedText")
        && op.value().contains("screen.about.title")), "About title is translated");
    assertTrue(renderer.operations().stream().noneMatch(op -> op.kind().equals("scaledIconText")
        && op.value().contains(":About:")), "raw English About title is not rendered");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("card")
        && op.value().contains("accent=" + EconomyUiTheme.ABOUT_ACCENT)),
        "info and QR cards use the legacy gray About accent");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("texture")
        && op.value().contains("textures/gui/vx.png")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("texture")
        && op.value().contains("textures/gui/zfb.png")));
  }

  @Test
  void legacyAboutLocksPanelQrResourceGeometryAndFooterControls() {
    AboutState state = new AboutState("EconomySystem", "QingMo HanHanYu",
        "https://github.com/QingMo-A/EconoeySystem", false);
    AboutLayout.Layout layout = AboutLayout.calculate(640, 360, 1.0f);
    assertEquals(new UiRect(110, 28, 420, 170), layout.panel());
    assertEquals(new UiRect(12, 238, 110, 110), layout.leftQr());
    assertEquals(new UiRect(518, 238, 110, 110), layout.rightQr());
    assertEquals(new UiRect(18, 244, 98, 98), textureRect(layout.leftQr()));
    assertEquals(new UiRect(524, 244, 98, 98), textureRect(layout.rightQr()));
    assertEquals(12, layout.title().x());
    assertEquals(12, layout.title().y());
    assertEquals(538, layout.esc().x());
    assertEquals(18, layout.esc().y());
    assertEquals(new UiRect(265, 164, 110, 22), layout.backButton());

    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    AboutView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("texture")
        && op.rect().equals(textureRect(layout.leftQr()))));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("texture")
        && op.rect().equals(textureRect(layout.rightQr()))));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("button.about.back")));
  }

  @Test
  void githubHoverAndClipboardConfirmationUseLegacyColorAndLocalizedKeys() {
    AboutState state = new AboutState("EconomySystem", "QingMo HanHanYu",
        "https://github.com/QingMo-A/EconoeySystem", false);
    AboutLayout.Layout layout = AboutLayout.calculate(640, 360, 1.0f);
    RecordingEconomyUiRenderer normal = new RecordingEconomyUiRenderer();
    AboutView.render(normal, state, layout, 0, 0);
    assertTrue(normal.paints().stream().anyMatch(p -> p.kind().equals("translatedTextInRect")
        && p.rect().equals(layout.github()) && p.argb() == EconomyUiTheme.TEXT_PRIMARY));

    RecordingEconomyUiRenderer hovered = new RecordingEconomyUiRenderer();
    AboutView.render(hovered, state, layout, layout.github().x() + 1, layout.github().y() + 1);
    assertTrue(hovered.paints().stream().anyMatch(p -> p.kind().equals("translatedTextInRect")
        && p.rect().equals(layout.github()) && p.argb() == EconomyUiTheme.MARKET_ACCENT));

    RecordingEconomyUiRenderer copied = new RecordingEconomyUiRenderer();
    AboutView.render(copied, new AboutState(state.modName(), state.author(), state.githubUrl(), true), layout, 0, 0);
    assertTrue(copied.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("message.about.copy_github_url")));
    assertTrue(copied.paints().stream().anyMatch(p -> p.kind().equals("translatedTextInRect")
        && p.rect().equals(layout.copyHint()) && p.argb() == EconomyUiTheme.TEXT_SUCCESS));
  }

  private static UiRect textureRect(UiRect card) {
    return new UiRect(card.x() + 6, card.y() + 6, card.width() - 12, card.height() - 12);
  }
}
