package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketCreateCompletionTest {
  @Test
  void keyboardSelectionAcceptsAnIdWithoutSubmitting() {
    FakePort port = new FakePort();
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.DEMAND,
        List.of(), port);

    controller.handle(new MarketCreateEvent.ItemIdChanged("dia"));
    assertTrue(controller.completionOpen());
    assertEquals(List.of("minecraft:diamond", "minecraft:diamond_block"),
        controller.completionSuggestions());

    controller.handle(new MarketCreateEvent.CompletionMoved(1));
    controller.handle(new MarketCreateEvent.CompletionAccepted(controller.completionSelection()));
    assertEquals("minecraft:diamond_block", controller.state().itemId());
    assertFalse(controller.completionOpen());
    assertEquals(0, port.submissions);
  }

  @Test
  void dismissHidesSuggestionsButRetainsTypedText() {
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.DEMAND,
        List.of(), new FakePort());
    controller.handle(new MarketCreateEvent.ItemIdChanged("sto"));
    controller.handle(new MarketCreateEvent.CompletionDismissed());
    assertEquals("sto", controller.state().itemId());
    assertFalse(controller.completionOpen());
  }

  @Test
  void keyboardAndOverlayTraverseAllCandidatesWithSelectionScrolledIntoView() {
    FakePort port = new FakePort();
    MarketCreateController controller = new MarketCreateController(MarketCreateMode.DEMAND,
        List.of(), port);

    controller.handle(new MarketCreateEvent.ItemIdChanged("many"));
    assertEquals(8, controller.completionSuggestions().size());
    for (int i = 0; i < 7; i++) controller.handle(new MarketCreateEvent.CompletionMoved(1));
    assertEquals(7, controller.completionSelection());

    MarketCreateState state = controller.state();
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer overlay = new RecordingEconomyUiRenderer();
    MarketCreateView.renderCompletionOverlay(overlay, state, layout, 0, 0,
        controller.completionSuggestions(), controller.completionSelection());
    assertTrue(overlay.operations().stream().anyMatch(operation ->
        operation.value().contains("minecraft:item_7")),
        "selected candidate must be brought into the visible completion window");

    controller.handle(new MarketCreateEvent.CompletionAccepted(controller.completionSelection()));
    assertEquals("minecraft:item_7", controller.state().itemId());
    assertEquals(0, port.submissions);
  }

  @Test
  void completionIsAnExplicitPostWidgetOverlay() {
    MarketCreateState state = new MarketCreateState(MarketCreateMode.DEMAND, List.of(), -1,
        "dia", 1, 1, ScreenState.READY, null, Set.of(MarketCreateAction.values()));
    MarketCreateLayout.Layout layout = MarketCreateLayout.calculate(640, 360, state);
    List<String> suggestions = List.of("minecraft:diamond", "minecraft:diamond_block");

    RecordingEconomyUiRenderer base = new RecordingEconomyUiRenderer();
    MarketCreateView.render(base, state, layout, 0, 0);
    assertTrue(base.operations().stream().noneMatch(operation ->
        operation.value().contains("minecraft:diamond")));

    RecordingEconomyUiRenderer overlay = new RecordingEconomyUiRenderer();
    MarketCreateView.renderCompletionOverlay(overlay, state, layout, 0, 0, suggestions, 0);
    assertTrue(overlay.operations().stream().anyMatch(operation -> operation.kind().equals("card")));
    assertTrue(overlay.operations().stream().anyMatch(operation ->
        operation.value().contains("minecraft:diamond")));
  }

  private static final class FakePort implements MarketCreatePort {
    private int submissions;

    @Override public List<String> itemIdSuggestions(String prefix) {
      return prefix.equals("dia")
          ? List.of("minecraft:diamond", "minecraft:diamond_block")
          : prefix.equals("many") ? java.util.stream.IntStream.range(0, 8)
              .mapToObj(index -> "minecraft:item_" + index).toList()
          : prefix.equals("sto") ? List.of("minecraft:stone") : List.of();
    }
    @Override public boolean isKnownItem(String itemId) { return true; }
    @Override public int maxStackSize(String itemId) { return 64; }
    @Override public void submitSales(int slot, int quantity, int totalPrice) { submissions++; }
    @Override public void submitDemand(String itemId, int quantity, int totalPrice) { submissions++; }
  }
}
