package com.mo.economy_system.ui.about;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
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
}
