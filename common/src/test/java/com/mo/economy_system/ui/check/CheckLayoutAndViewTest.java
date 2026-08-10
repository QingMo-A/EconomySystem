package com.mo.economy_system.ui.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckResultController;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckLayoutAndViewTest {
  @Test
  void layoutsKeepInteractiveRectsStableAcrossViewportSizes() {
    for (int[] viewport : new int[][] {{1, 1}, {240, 160}, {640, 360}, {1600, 900}}) {
      CheckConsentLayout.Layout consent = CheckConsentLayout.calculate(viewport[0], viewport[1]);
      assertTrue(consent.allow().width() > 0);
      assertTrue(consent.decline().width() > 0);
      assertTrue(consent.card().contains(consent.allow()));
      assertTrue(consent.card().contains(consent.decline()));

      CheckResultLayout.Layout result = CheckResultLayout.calculate(viewport[0], viewport[1], state());
      assertTrue(result.visibleRows() >= 1);
      assertTrue(result.rows().height() >= 1);
      assertTrue(result.retry().width() > 0);
      assertTrue(result.back().width() > 0);
    }
  }

  @Test
  void commonViewsEmitTheExpectedSemanticControls() {
    RecordingRenderer renderer = new RecordingRenderer();
    CheckConsentLayout.Layout consent = CheckConsentLayout.calculate(640, 360);
    CheckConsentView.render(
        renderer,
        new CheckConsentState("requester", "mods", ScreenState.READY,
            Set.of(CheckConsentAction.ALLOW, CheckConsentAction.DECLINE)),
        consent,
        consent.allow().x(),
        consent.allow().y());
    assertTrue(renderer.buttons.contains("button.check_consent.allow"));
    assertTrue(renderer.buttons.contains("button.check_consent.decline"));

    renderer.buttons.clear();
    CheckResultLayout.Layout result = CheckResultLayout.calculate(640, 360, state());
    CheckResultView.render(renderer, state(), result, 0, 0);
    assertEquals(List.of("button.check_result.retry", "button.transfer.close"), renderer.buttons);
  }

  private static CheckResultState state() {
    return new CheckResultState(
        "target",
        "mods",
        ClientFileCheckStatus.SUCCESS,
        1,
        0,
        null,
        ClientFileCheckResultController.LocalState.FAILED,
        "SCAN_FAILED",
        false,
        List.of(new CheckResultRow("a.jar", "screen.check_result.only_remote", false)),
        "",
        0,
        3,
        ScreenState.ERROR,
        Set.of(CheckResultAction.RETRY, CheckResultAction.BACK));
  }

  private static final class RecordingRenderer implements EconomyUiRenderer {
    private final List<String> buttons = new ArrayList<>();

    @Override public void fill(UiRect rect, int argb) {}
    @Override public void text(String text, int x, int y, int argb) {}
    @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) {}
    @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {}
    @Override public void translatedTextInRect(
        String key, List<String> arguments, UiRect rect, int argb, UiTextAlignment alignment) {}
    @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {}
    @Override public void button(
        UiRect rect, UiButtonStyle style, String text, boolean hovered, boolean enabled) {}
    @Override public void translatedButton(
        UiRect rect,
        UiButtonStyle style,
        String key,
        List<String> arguments,
        boolean hovered,
        boolean enabled) {
      buttons.add(key);
    }
    @Override public void translatedIconButton(
        UiRect rect, UiButtonStyle style, UiIcon icon, String key, List<String> arguments,
        boolean hovered, boolean enabled) {}
    @Override public void scaledIconText(
        UiIcon icon, String text, int originX, int originY, float scale, int iconSize,
        int iconAdvance, int textColor) {}
    @Override public void scaledIconStyledText(
        UiIcon icon, List<UiTextSpan> spans, int originX, int originY, float scale,
        int iconSize, int iconAdvance) {}
    @Override public UiTextMetrics metrics() { return UiTextMetrics.APPROXIMATE; }
    @Override public void icon(UiIcon icon, UiRect rect) {}
    @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {}
    @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {}
    @Override public void itemDisplayNameWithSuffix(String itemId, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
    @Override public void translatedTextWithSuffix(String key, List<String> arguments, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
  }
}
