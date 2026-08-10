package com.mo.economy_system.ui.territory.buff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import com.mo.economy_system.ui.theme.UiButtonStyle;
import com.mo.economy_system.ui.theme.UiCardStyle;
import com.mo.economy_system.ui.text.UiTextMetrics;
import com.mo.economy_system.ui.text.UiTextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuffManageViewParityTest {
  @Test
  void commonViewProducesDeterministicBaselineSemanticOperations() {
    BuffRow upgrade = BuffRow.inspect(
        BuffManageTestFixtures.buff("speed", true, 1, 3, 2, 3, 4),
        BuffManageTestFixtures.resources(8, 8));
    BuffRow locked = BuffRow.inspect(
        BuffManageTestFixtures.buff("strength", false, 0, 3, 2, 3, 4),
        BuffManageTestFixtures.resources(0, 0));
    BuffManageState state = state(List.of(upgrade, locked), ScreenState.READY, null);
    BuffManageLayout.Layout layout = BuffManageLayout.calculate(640, 360, state);
    RecordingRenderer forge = new RecordingRenderer();
    RecordingRenderer neoForge = new RecordingRenderer();

    BuffManageView.render(forge, state, layout, 0, 0);
    BuffManageView.render(neoForge, state, layout, 0, 0);

    assertEquals(forge.operations, neoForge.operations);
    assertTrue(forge.operations.stream().anyMatch(operation ->
        operation.kind.equals("icon") && operation.value.equals(UiIcon.BUFF)));
    assertTrue(forge.operations.stream().anyMatch(operation ->
        operation.kind.equals("translatedButton")
            && operation.key.equals("button.territory.buff.upgrade")
            && operation.buttonStyle.equals(EconomyUiTheme.TERRITORY_BUFF_UPGRADE_BUTTON)
            && operation.enabled));
    assertTrue(forge.operations.stream().anyMatch(operation ->
        operation.kind.equals("translatedButton")
            && operation.key.equals("button.territory.buff.unlock")
            && operation.buttonStyle.equals(EconomyUiTheme.DISABLED_BUTTON)
            && !operation.enabled));
    assertTrue(forge.operations.stream().anyMatch(operation ->
        operation.kind.equals("card")
            && operation.cardStyle.equals(EconomyUiTheme.TERRITORY_LOCKED_CARD)));
  }

  @Test
  void errorRetryAndTooltipsAreReachableCommonModels() {
    BuffRow row = BuffRow.inspect(
        BuffManageTestFixtures.buff("speed", true, 1, 3, 2, 3, 4),
        BuffManageTestFixtures.resources(1, 2));
    BuffManageState ready = state(List.of(row), ScreenState.READY, null);
    BuffManageLayout.Layout readyLayout = BuffManageLayout.calculate(640, 360, ready);
    var card = readyLayout.cards().get(0);

    TooltipModel iconTooltip = BuffManageView.tooltipAt(ready, readyLayout,
        card.icon().x(), card.icon().y()).orElseThrow();
    assertTrue(iconTooltip.lines().stream().anyMatch(line ->
        line instanceof TooltipLine.Translated translated
            && translated.key().equals("screen.territory.buff.tooltip.id")));
    TooltipModel costTooltip = BuffManageView.tooltipAt(ready, readyLayout,
        card.cost().x(), card.cost().y()).orElseThrow();
    assertTrue(costTooltip.lines().stream().anyMatch(TooltipLine.Item.class::isInstance));

    BuffManageState error = state(List.of(), ScreenState.ERROR,
        "screen.territory.buff.sync_failed");
    BuffManageLayout.Layout errorLayout = BuffManageLayout.calculate(640, 360, error);
    RecordingRenderer renderer = new RecordingRenderer();
    BuffManageView.render(renderer, error, errorLayout,
        errorLayout.retryButton().x(), errorLayout.retryButton().y());
    assertTrue(renderer.operations.stream().anyMatch(operation ->
        operation.kind.equals("translatedButton")
            && operation.key.equals("screen.territory.buff.retry")
            && operation.rect.equals(errorLayout.retryButton())
            && operation.enabled));
  }

  private static BuffManageState state(List<BuffRow> rows, ScreenState screenState,
                                       String errorKey) {
    return new BuffManageState(new UUID(0, 1), "home", rows, 0, 6, 0, "",
        screenState, errorKey, -1);
  }

  private static final class RecordingRenderer implements EconomyUiRenderer {
    private final List<Operation> operations = new ArrayList<>();

    @Override public void fill(UiRect rect, int argb) {
      operations.add(new Operation("fill", rect, null, null, null, null, true));
    }
    @Override public void text(String text, int x, int y, int argb) {
      operations.add(new Operation("text", new UiRect(x, y, 0, 0), text,
          null, null, null, true));
    }
    @Override public void translatedText(String key, List<String> arguments,
                                         int x, int y, int argb) {
      operations.add(new Operation("translatedText", new UiRect(x, y, 0, 0), arguments,
          key, null, null, true));
    }
    @Override public void textInRect(String text, UiRect rect, int argb,
                                     UiTextAlignment alignment) {
      operations.add(new Operation("textInRect", rect, text + alignment,
          null, null, null, true));
    }
    @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect,
                                               int argb, UiTextAlignment alignment) {
      operations.add(new Operation("translatedTextInRect", rect, arguments + alignment.name(),
          key, null, null, true));
    }
    @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) {
      operations.add(new Operation("card", rect, hovered, null, style, null, true));
    }
    @Override public void button(UiRect rect, UiButtonStyle style, String text,
                                 boolean hovered, boolean enabled) {
      operations.add(new Operation("button", rect, text + hovered, null, null, style, enabled));
    }
    @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key,
                                           List<String> arguments, boolean hovered,
                                           boolean enabled) {
      operations.add(new Operation("translatedButton", rect, arguments + Boolean.toString(hovered),
          key, null, style, enabled));
    }
    @Override public void translatedIconButton(UiRect rect, UiButtonStyle style, UiIcon icon,
                                               String key, List<String> arguments,
                                               boolean hovered, boolean enabled) {
      operations.add(new Operation("translatedIconButton", rect,
          arguments + Boolean.toString(hovered), key, null, style, enabled));
    }
    @Override public void icon(UiIcon icon, UiRect rect) {
      operations.add(new Operation("icon", rect, icon, null, null, null, true));
    }
    @Override public void scaledIconText(UiIcon icon, String text, int originX, int originY,
                                         float scale, int iconSize, int iconAdvance,
                                         int textColor) {
      operations.add(new Operation("scaledIconText", new UiRect(originX, originY, iconSize,
          iconSize), icon + ":" + text, null, null, null, true));
    }
    @Override public void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans,
                                               int originX, int originY, float scale,
                                               int iconSize, int iconAdvance) {
      operations.add(new Operation("scaledIconStyledText", new UiRect(originX, originY,
          iconSize, iconSize), icon + ":" + spans, null, null, null, true));
    }
    @Override public UiTextMetrics metrics() { return UiTextMetrics.APPROXIMATE; }
    @Override public void playerHead(UUID playerId, String playerName, UiRect rect) {
      operations.add(new Operation("playerHead", rect, playerId + playerName,
          null, null, null, true));
    }
    @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) {
      operations.add(new Operation("tooltip", new UiRect(mouseX, mouseY, 0, 0), tooltip,
          null, null, null, true));
    }
    @Override public void itemDisplayNameWithSuffix(String itemId, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
    @Override public void translatedTextWithSuffix(String key, List<String> arguments, String suffix, UiRect rect, int color, UiTextAlignment alignment) {}
  }

  private record Operation(String kind, UiRect rect, Object value, String key,
                           UiCardStyle cardStyle, UiButtonStyle buttonStyle,
                           boolean enabled) {}
}
