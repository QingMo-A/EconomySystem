package com.mo.economy_system.ui.market;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exact market confirmation visual assertions from the legacy confirmation dialog. */
class MarketConfirmLegacyReferenceParityTest {
  @Test
  void confirmationUsesNativeItemNameAndCompositeSellerLine() {
    MarketRow row = new MarketRow(MarketTestFixtures.sales(0));
    MarketConfirmState state = new MarketConfirmState(MarketAction.BUY, row, ScreenState.READY,
        null, Set.of(MarketConfirmAction.values()));
    MarketConfirmLayout.Layout layout = MarketConfirmLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    MarketConfirmView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayName")),
        "legacy confirmation resolves the native item hover name");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextWithSuffix")
        && op.value().contains("owner-0")),
        "legacy seller label and owner are one clipped line");
  }
}
