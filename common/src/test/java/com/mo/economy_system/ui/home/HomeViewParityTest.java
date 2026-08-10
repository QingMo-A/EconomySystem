package com.mo.economy_system.ui.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.client.ui.EconomyUiMenu;
import com.mo.economy_system.common.network.AccountBalance;
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

class HomeViewParityTest {
  @Test
  void commonViewProducesDeterministicSemanticOperations() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 42,
        List.of(new AccountBalance("alice", 42), new AccountBalance("bob", 7)), 2, 3,
        0, 5, ScreenState.READY, null, -1, 1, 1);
    HomeLayout.Layout layout = HomeLayout.calculate(640, 360, state);
    RecordingRenderer first = new RecordingRenderer();
    RecordingRenderer second = new RecordingRenderer();
    HomeView.render(first, state, layout, 0, 0);
    HomeView.render(second, state, layout, 0, 0);
    assertEquals(first.operations, second.operations);
    assertTrue(first.operations.stream().anyMatch(op -> op.kind.equals("icon") && op.value.equals("BALANCE")));
    assertTrue(first.operations.stream().anyMatch(op -> op.kind.equals("translatedTextInRect")
        && op.value.equals("screen.home.leaderboard")));
  }

  @Test
  void readyHomeEmitsCanonicalSemanticCardsAndNavigation() {
    HomeState state = new HomeState("alice", EconomyUiMenu.defaultEntries(), 4200,
        List.of(new AccountBalance("alice", 4200), new AccountBalance("bob", 7)), 2, 3,
        0, 10, ScreenState.READY, null, -1, 1, 1);
    HomeLayout.Layout layout = HomeLayout.calculate(640, 360, state);
    RecordingRenderer renderer = new RecordingRenderer();
    HomeView.render(renderer, state, layout, 0, 0);

    assertEquals(5, renderer.operations.stream()
        .filter(operation -> operation.kind.equals("translatedIconButton")).count());
    for (UiIcon icon : List.of(UiIcon.SHOP, UiIcon.MARKET, UiIcon.DELIVERY,
        UiIcon.TERRITORY, UiIcon.ABOUT)) {
      assertTrue(renderer.operations.stream().anyMatch(operation ->
          operation.kind.equals("translatedIconButton") && operation.value.endsWith(icon.name())));
    }
    assertTrue(renderer.operations.stream().filter(operation -> operation.kind.equals("card"))
        .count() >= 4);
    assertTrue(renderer.operations.stream().anyMatch(operation ->
        operation.kind.equals("icon") && operation.value.equals("TRADE")));
    assertTrue(renderer.operations.stream().anyMatch(operation ->
        operation.kind.equals("scaledIconStyledText")));
  }

  private static final class RecordingRenderer implements EconomyUiRenderer {
    private final List<Operation> operations = new ArrayList<>();
    @Override public void fill(UiRect rect, int argb) { add("fill", rect, Integer.toString(argb), true); }
    @Override public void text(String text, int x, int y, int argb) { add("text", new UiRect(x, y, 0, 0), text, true); }
    @Override public void translatedText(String key, List<String> arguments, int x, int y, int argb) { add("translatedText", new UiRect(x, y, 0, 0), key, true); }
    @Override public void textInRect(String text, UiRect rect, int argb, UiTextAlignment alignment) { add("textInRect", rect, text, true); }
    @Override public void translatedTextInRect(String key, List<String> arguments, UiRect rect, int argb, UiTextAlignment alignment) { add("translatedTextInRect", rect, key, true); }
    @Override public void card(UiRect rect, UiCardStyle style, boolean hovered) { add("card", rect, style.toString(), true); }
    @Override public void button(UiRect rect, UiButtonStyle style, String text, boolean hovered, boolean enabled) { add("button", rect, text, enabled); }
    @Override public void translatedButton(UiRect rect, UiButtonStyle style, String key, List<String> arguments, boolean hovered, boolean enabled) { add("translatedButton", rect, key, enabled); }
    @Override public void translatedIconButton(UiRect rect, UiButtonStyle style, UiIcon icon, String key, List<String> arguments, boolean hovered, boolean enabled) { add("translatedIconButton", rect, key + ":" + icon, enabled); }
    @Override public void icon(UiIcon icon, UiRect rect) { add("icon", rect, icon.name(), true); }
    @Override public void scaledIconText(UiIcon icon, String text, int originX, int originY, float scale, int iconSize, int iconAdvance, int textColor) { add("scaledIconText", new UiRect(originX, originY, iconSize, iconSize), text, true); }
    @Override public void scaledIconStyledText(UiIcon icon, List<UiTextSpan> spans, int originX, int originY, float scale, int iconSize, int iconAdvance) { add("scaledIconStyledText", new UiRect(originX, originY, iconSize, iconSize), spans.toString(), true); }
    @Override public UiTextMetrics metrics() { return UiTextMetrics.APPROXIMATE; }
    @Override public void playerHead(UUID playerId, String playerName, UiRect rect) { add("playerHead", rect, playerName, true); }
    @Override public void tooltip(TooltipModel tooltip, int mouseX, int mouseY) { add("tooltip", new UiRect(mouseX, mouseY, 0, 0), tooltip.toString(), true); }
    private void add(String kind, UiRect rect, String value, boolean enabled) { operations.add(new Operation(kind, rect, value, enabled)); }
  }

  private record Operation(String kind, UiRect rect, String value, boolean enabled) {}
}
