package com.mo.economy_system.ui.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShopViewParityTest {
  @Test
  void bothTargetsReceiveTheSameCatalogOperations() {
    ShopState state = new ShopState(ShopTestFixtures.items(3).stream().map(ShopRow::new).toList(),
        0, 15, "", ScreenState.READY, null, -1, Set.of(ShopAction.values()));
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer forge = new RecordingEconomyUiRenderer();
    RecordingEconomyUiRenderer neoForge = new RecordingEconomyUiRenderer();

    ShopView.render(forge, state, layout, 0, 0);
    ShopView.render(neoForge, state, layout, 0, 0);

    assertEquals(forge.operations(), neoForge.operations());
    assertEquals(3, forge.operations().stream().filter(operation -> operation.kind().equals("item")).count());
    assertEquals(3, forge.operations().stream()
        .filter(operation -> operation.kind().equals("translatedButton"))
        .filter(operation -> operation.value().startsWith("screen.shop.buy")).count());
    assertTrue(forge.operations().stream().anyMatch(operation ->
        operation.kind().equals("textInRect")
            && operation.value().startsWith("￥")));
  }

  @Test
  void errorViewOwnsReachableRetrySemantics() {
    ShopState state = new ShopState(List.of(), 0, 15, "", ScreenState.ERROR,
        "screen.shop.sync_failed", -1, Set.of(ShopAction.RETRY, ShopAction.BACK));
    ShopLayout.Layout layout = ShopLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    ShopView.render(renderer, state, layout, layout.message().x(), layout.message().y());

    assertTrue(renderer.operations().stream().anyMatch(operation ->
        operation.kind().equals("translatedButton")
            && operation.value().startsWith("screen.shop.retry") && operation.enabled()));
  }
}
