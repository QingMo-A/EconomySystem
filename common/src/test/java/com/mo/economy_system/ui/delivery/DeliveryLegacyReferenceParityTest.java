package com.mo.economy_system.ui.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.testsupport.RecordingEconomyUiRenderer;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Exact geometry/chrome assertions transcribed from the legacy delivery-box screen. */
class DeliveryLegacyReferenceParityTest {
  @Test
  void legacyGridKeepsTwoRowsAndSixEntriesAtReferenceViewport() {
    DeliveryState state = ready(6, 6, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    assertEquals(3, layout.columns());
    assertEquals(2, layout.rows());
    assertEquals(6, layout.pageSize());
    assertEquals(new UiRect(12, 55, 200, 70), layout.cards().get(0).card());
    assertEquals(new UiRect(220, 55, 200, 70), layout.cards().get(1).card());
    assertEquals(new UiRect(428, 133, 200, 70), layout.cards().get(5).card());
    assertEquals(new UiRect(20, 74, 32, 32), layout.cards().get(0).itemIcon());
    assertEquals(new UiRect(144, 99, 60, 18), layout.cards().get(0).claimButton());
  }

  @Test
  void legacyChromeUsesLocalizedTitleNativeItemAndDeliveryActions() {
    DeliveryState state = ready(1, 6, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("inputFrame")
        && op.value().contains("right=-18611")), "orange search frame is required");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("scaledIconTranslatedText")
        && op.value().contains("screen.delivery_box.title")), "title stays localized");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayNameWithSuffix")),
        "item label must use the native item display name");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("button.delivery_box.claim")
        && op.value().contains("accent=" + (EconomyUiTheme.DELIVERY_ACCENT & 0x00FFFFFF))),
        "claim uses the delivery action style");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("card")
        && op.value().contains("accent=" + EconomyUiTheme.SHOP_ACCENT)),
        "legacy delivery cards use the shop-orange card accent");
  }

  @Test
  void deliveryNameAndCountShareOneTruncatedNativeLineAndRetryUsesDeliveryStyle() {
    DeliveryState state = ready(1, 6, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemDisplayNameWithSuffix")
        && op.value().contains(" x2")), "legacy name and count are truncated as one line");
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.delivery_box.item.source") && op.rect().width() == 78),
        "legacy source text excludes the claim button and is truncated to the remaining width");

    DeliveryState error = new DeliveryState(state.rows(), 0, 6, "", ScreenState.ERROR,
        "screen.delivery_box.sync_failed", 1, Set.of(DeliveryAction.RETRY));
    renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, error, layout, layout.message().x(), layout.message().y());
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedButton")
        && op.value().startsWith("screen.delivery_box.retry")
        && op.value().contains(EconomyUiTheme.DELIVERY_CLAIM_BUTTON.toString())),
        "retry stays in the delivery action style family, not the Home navigation style");
  }

  @Test
  void legacyPagingUsesNativeArrowsAndOrangePageStyle() {
    DeliveryState state = ready(7, 6, 1);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, state, layout, 0, 0);
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("icon")
        && op.value().equals("ARROW_LEFT") && op.rect().equals(new UiRect(262, 326, 12, 12))));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("icon")
        && op.value().equals("ARROW_RIGHT") && op.rect().equals(new UiRect(366, 326, 12, 12))));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("button")
        && op.value().contains("accent=" + (EconomyUiTheme.SHOP_ACCENT & 0x00FFFFFF))));
  }

  @Test
  void tooltipRetainsNativeCountAndSourceLines() {
    DeliveryState state = ready(1, 6, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    var tooltip = DeliveryView.tooltipAt(layout, 20, 74).orElseThrow();
    assertEquals(1, tooltip.lines().size());
    assertTrue(tooltip.lines().get(0).getClass().getSimpleName().equals("NativeItem"),
        "legacy delivery icon hover delegates to the native ItemStack tooltip");
  }

  private static DeliveryState ready(int count, int pageSize, int page) {
    List<DeliveryRow> rows = java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> new DeliveryRow(DeliveryBoxTestFixtures.entry(new UUID(4, i + 1), i + 2)))
        .toList();
    return new DeliveryState(rows, page, pageSize, "", ScreenState.READY, null, 1,
        Set.of(DeliveryAction.CLAIM, DeliveryAction.BACK));
  }
}
