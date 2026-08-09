package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MarketViewParityTest {
  @Test
  void bothTargetsReceiveAllNineCommonOrderCards() {
    MarketState state = new MarketState(MarketTestFixtures.orders(9).stream().map(MarketRow::new).toList(),
        0, MarketController.NETWORK_PAGE_SIZE, 18, 9, 9, MarketOrderFilter.ALL, "",
        ScreenState.READY, null, -1, 1, Set.of(MarketAction.values()));
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer forge = new RecordingEconomyUiRenderer();
    RecordingEconomyUiRenderer neoForge = new RecordingEconomyUiRenderer();

    MarketView.render(forge, state, layout, MarketTestFixtures.VIEWER, 0, 0);
    MarketView.render(neoForge, state, layout, MarketTestFixtures.VIEWER, 0, 0);

    assertEquals(forge.operations(), neoForge.operations());
    assertEquals(9, forge.operations().stream().filter(operation -> operation.kind().equals("item")).count());
    assertEquals(9, forge.operations().stream()
        .filter(operation -> operation.kind().equals("translatedButton"))
        .filter(operation -> operation.value().startsWith("screen.market.buy")).count());
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("textInRect") && operation.value().startsWith("1 / 2")));
  }

  @Test
  void errorViewOwnsReachableRetrySemantics() {
    MarketState state = new MarketState(java.util.List.of(), 0, MarketController.NETWORK_PAGE_SIZE,
        0, 0, 0, MarketOrderFilter.ALL, "", ScreenState.ERROR, "screen.market.sync_failed",
        -1, 1, Set.of(MarketAction.RETRY, MarketAction.BACK));
    MarketLayout.Layout layout = MarketLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketView.render(renderer, state, layout, MarketTestFixtures.VIEWER,
        layout.message().x(), layout.message().y());

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton")
            && operation.value().startsWith("screen.market.retry") && operation.enabled()));
  }
}
