package com.mo.economy_system.ui.territory.detail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.PlayerSummary;
import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryDetailViewParityTest {
  @Test
  void commonViewProducesDeterministicNestedSemanticOperations() {
    TerritoryDetailState state = new TerritoryDetailState(
        TerritoryDetailTestFixtures.territory(List.of(
            new Member(TerritoryDetailTestFixtures.ALICE, "alice"))),
        List.of(new PlayerSummary(TerritoryDetailTestFixtures.ALICE, "alice"),
            new PlayerSummary(TerritoryDetailTestFixtures.BOB, "bob")),
        TerritoryDetailViewKind.ACCESS, 0, 5, "", ScreenState.READY, null, -1, 1);
    TerritoryDetailLayout.Layout layout = TerritoryDetailLayout.calculate(640, 360, state);
    RecordingRenderer forge = new RecordingRenderer();
    RecordingRenderer neoForge = new RecordingRenderer();

    TerritoryDetailView.render(forge, state, layout, 0, 0);
    TerritoryDetailView.render(neoForge, state, layout, 0, 0);

    assertEquals(forge.operations, neoForge.operations);
    assertTrue(forge.operations.stream().anyMatch(value -> value.kind.equals("playerHead")));
    assertTrue(forge.operations.stream().anyMatch(value ->
        value.kind.equals("translatedButton")
            && value.value.equals("button.territory.access.remove")));
  }

  @Test
  void commonViewOwnsMainActionsAndErrorRetry() {
    TerritoryDetailState main = new TerritoryDetailState(
        TerritoryDetailTestFixtures.territory(List.of()), List.of(), TerritoryDetailViewKind.MAIN,
        0, 5, "", ScreenState.READY, null, -1, 0);
    TerritoryDetailLayout.Layout mainLayout = TerritoryDetailLayout.calculate(640, 360, main);
    RecordingRenderer renderer = new RecordingRenderer();
    TerritoryDetailView.render(renderer, main, mainLayout, 0, 0);
    assertTrue(renderer.operations.stream().anyMatch(value ->
        value.kind.equals("translatedButton")
            && value.value.equals("message.territory_management.buff")));

    TerritoryDetailState error = new TerritoryDetailState(main.territory(), List.of(),
        TerritoryDetailViewKind.MAIN, 0, 5, "", ScreenState.ERROR,
        "screen.territory.detail.sync_failed", -1, 0);
    TerritoryDetailLayout.Layout errorLayout = TerritoryDetailLayout.calculate(640, 360, error);
    renderer = new RecordingRenderer();
    TerritoryDetailView.render(renderer, error, errorLayout,
        errorLayout.retryButton().x(), errorLayout.retryButton().y());
    assertTrue(renderer.operations.stream().anyMatch(value ->
        value.kind.equals("translatedButton")
            && value.value.equals("screen.territory.detail.retry") && value.enabled));
  }

  private static final class RecordingRenderer implements EconomyUiRenderer {
    private final List<Operation> operations = new ArrayList<>();
    @Override public void fill(UiRect rect, int argb) { add("fill", rect, Integer.toString(argb), true); }
    @Override public void text(String text, int x, int y, int argb) {
      add("text", new UiRect(x, y, 0, 0), text, true);
    }
    @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) {
      add("translatedText", new UiRect(x, y, 0, 0), key + arguments, true);
    }
    @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) {
      add("textInRect", rect, text + alignment, true);
    }
    @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                                               int argb, UiTextAlignment alignment) {
      add("translatedTextInRect", rect, key + arguments + alignment, true);
    }
    @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {
      add("card", rect, style + Boolean.toString(hovered), true);
    }
    @Override public void button(UiRect rect, UiButtonStyle style, String text,
                                 boolean hovered, boolean enabled) {
      add("button", rect, text + style + hovered, enabled);
    }
    @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                           List<String> arguments, boolean hovered, boolean enabled) {
      add("translatedButton", rect, key, enabled);
    }
    @Override public void translatedIconButton(UiRect rect, UiButtonStyle style, UiIcon icon,
                                               String key, List<String> arguments,
                                               boolean hovered, boolean enabled) {
      add("translatedIconButton", rect, key, enabled);
    }
    @Override public void icon(UiIcon icon, UiRect rect) { add("icon", rect, icon.name(), true); }
    @Override public void scaledIconText(UiIcon icon, String text, int originX, int originY,
                                         float scale, int iconSize, int iconAdvance,
                                         int textColor) {
      add("scaledIconText", new UiRect(originX, originY, iconSize, iconSize), text, true);
    }
    @Override public void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans,
                                               int originX, int originY, float scale,
                                               int iconSize, int iconAdvance) {
      add("scaledIconStyledText", new UiRect(originX, originY, iconSize, iconSize),
          spans.toString(), true);
    }
    @Override public UiTextMetrics metrics() { return UiTextMetrics.APPROXIMATE; }
    @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
      add("playerHead", rect, playerId + playerName, true);
    }
    @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {
      add("tooltip", new UiRect(mouseX, mouseY, 0, 0), tooltip.toString(), true);
    }
    @Override public void itemDisplayNameWithSuffix(String itemId, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
    @Override public void translatedTextWithSuffix(String key, List<String> arguments, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
    private void add(String kind, UiRect rect, String value, boolean enabled) {
      operations.add(new Operation(kind, rect, value, enabled));
    }
  }

  private record Operation(String kind, UiRect rect, String value, boolean enabled) {}
}
