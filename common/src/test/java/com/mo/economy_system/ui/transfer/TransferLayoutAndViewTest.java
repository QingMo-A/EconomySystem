package com.mo.economy_system.ui.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TransferLayoutAndViewTest {
  @Test
  void layoutsKeepAllActionRectsInsideTheCard() {
    for (int[] viewport : new int[][] {{1, 1}, {240, 160}, {640, 360}, {1600, 900}}) {
      TransferConsentLayout.Layout consent = TransferConsentLayout.calculate(viewport[0], viewport[1]);
      assertTrue(consent.card().contains(consent.allow()));
      assertTrue(consent.card().contains(consent.decline()));

      TransferResultLayout.Layout artifact = TransferResultLayout.calculate(viewport[0], viewport[1], artifact());
      assertTrue(artifact.card().contains(artifact.primary()));
      assertTrue(artifact.card().contains(artifact.secondary()));
      TransferResultLayout.Layout terminal = TransferResultLayout.calculate(
          viewport[0], viewport[1], TransferResultState.terminal("message.transfer.status.failed", "message.transfer.expired"));
      assertTrue(terminal.card().contains(terminal.close()));
    }
  }

  @Test
  void viewsEmitSharedSemanticButtons() {
    RecordingRenderer renderer = new RecordingRenderer();
    TransferConsentLayout.Layout consent = TransferConsentLayout.calculate(640, 360);
    TransferConsentView.render(
        renderer,
        new TransferConsentState(
            "requester", "mods", "example.jar", 12, "a".repeat(64),
            com.mo.economy_system.ui.core.ScreenState.READY,
            java.util.Set.of(TransferConsentAction.ALLOW, TransferConsentAction.DECLINE)),
        consent,
        0,
        0);
    assertEquals(List.of("button.transfer.allow", "button.transfer.decline"), renderer.buttons);

    renderer.buttons.clear();
    TransferResultLayout.Layout result = TransferResultLayout.calculate(640, 360, artifact());
    TransferResultView.render(renderer, artifact(), result, 0, 0);
    assertEquals(List.of("button.transfer.save", "button.transfer.discard"), renderer.buttons);
  }

  private static TransferResultState artifact() {
    return TransferResultState.artifact(
        "target", "mods", "example.jar", 12, "a".repeat(64), "message.transfer.state.pending");
  }

  private static final class RecordingRenderer implements EconomyUiRenderer {
    private final List<String> buttons = new ArrayList<>();
    @Override public void fill(UiRect rect, int argb) {}
    @Override public void text(String text, int x, int y, int argb) {}
    @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) {}
    @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {}
    @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect, int argb, UiTextAlignment alignment) {}
    @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {}
    @Override public void button(UiRect rect, UiButtonStyle style, String text, boolean hovered, boolean enabled) {}
    @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key, List<String> arguments, boolean hovered, boolean enabled) { buttons.add(key); }
    @Override public void icon(UiIcon icon, UiRect rect) {}
    @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {}
    @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {}
  }
}
