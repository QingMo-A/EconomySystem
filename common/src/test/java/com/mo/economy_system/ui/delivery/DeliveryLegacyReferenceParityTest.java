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

/** Mailbox interaction/geometry contract replacing the old delivery-grid parity assertions. */
class DeliveryLegacyReferenceParityTest {
  @Test
  void mailboxUsesThreePanesAtReferenceViewport() {
    DeliveryState state = ready(4, 4, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);

    assertEquals(new UiRect(12, 52, 96, 266), layout.categoryPanel());
    assertEquals(new UiRect(116, 52, 190, 48), layout.cards().get(0).card());
    assertEquals(new UiRect(314, 52, 314, 266), layout.detailPanel());
    assertEquals(1, layout.columns());
    assertEquals(4, layout.rows());
    assertEquals(4, layout.pageSize());
    assertTrue(layout.attachmentCard().contains(layout.detailItemIcon()));
    assertEquals(new UiRect(layout.attachmentCard().x(), layout.attachmentCard().y(), 28, 28),
        layout.attachmentCard());
  }

  @Test
  void mailboxRendersCategoriesMailPreviewDetailAndAttachmentAction() {
    DeliveryState state = ready(1, 4, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, state, layout, 0, 0);

    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("scaledIconTranslatedText")
        && op.value().contains("screen.delivery_box.title")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextWithSuffix")
        && op.value().startsWith("screen.mailbox.category.all")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.mailbox.sender.market")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.mailbox.subject.market_return")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("translatedTextInRect")
        && op.value().startsWith("screen.mailbox.attachments")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemWithCount")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("fill")
        && op.rect().equals(layout.attachmentCard())));
  }

  @Test
  void selectedMailUsesDeliveryAccentAndAttachmentKeepsItemAccent() {
    DeliveryState state = ready(2, 4, 0);
    UUID second = state.rows().get(1).entryId();
    state = new DeliveryState(state.rows(), 0, 4, "", DeliveryCategory.ALL, second,
        ScreenState.READY, null, 1, Set.of(DeliveryAction.CLAIM, DeliveryAction.BACK));
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, state, layout, 0, 0);

    UiRect selectedRect = layout.cards().get(1).card();
    assertTrue(renderer.paints().stream().anyMatch(op -> op.kind().equals("fill")
        && op.rect().equals(new UiRect(selectedRect.x(), selectedRect.y(), 2, selectedRect.height()))
        && op.argb() == EconomyUiTheme.DELIVERY_ACCENT));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("itemWithCount")
        && op.rect().equals(layout.detailItemIcon())));
  }

  @Test
  void marketCategoryContainsLegacyMarketDeliveriesWhileOtherCategoriesCanBeEmpty() {
    DeliveryState state = ready(2, 4, 0);
    assertEquals(2, state.count(DeliveryCategory.MARKET));
    assertEquals(0, state.count(DeliveryCategory.SYSTEM));
    DeliveryState system = new DeliveryState(state.rows(), 0, 4, "", DeliveryCategory.SYSTEM, null,
        ScreenState.READY, null, 1, state.actions());
    assertTrue(system.filteredRows().isEmpty());
  }

  @Test
  void mailboxPagingUsesTextualAnglesAndListLocalControls() {
    DeliveryState state = ready(5, 4, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    RecordingEconomyUiRenderer renderer = new RecordingEconomyUiRenderer();
    DeliveryView.render(renderer, state, layout, 0, 0);

    assertEquals(2, state.totalPages());
    assertEquals(116, layout.previousButton().x());
    assertEquals(264, layout.nextButton().x());
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("button")
        && op.value().startsWith("<:")));
    assertTrue(renderer.operations().stream().anyMatch(op -> op.kind().equals("button")
        && op.value().startsWith(">:")));
  }

  @Test
  void listAndDetailItemHoverUseNativeItemTooltip() {
    DeliveryState state = ready(1, 4, 0);
    DeliveryLayout.Layout layout = DeliveryLayout.calculate(640, 360, state);
    UiRect listIcon = layout.cards().get(0).itemIcon();
    var listTooltip = DeliveryView.tooltipAt(layout, listIcon.x(), listIcon.y()).orElseThrow();
    var detailTooltip = DeliveryView.tooltipAt(state, layout,
        layout.detailItemIcon().x(), layout.detailItemIcon().y()).orElseThrow();
    assertEquals(1, listTooltip.lines().size());
    assertEquals(1, detailTooltip.lines().size());
    assertEquals("NativeItem", listTooltip.lines().get(0).getClass().getSimpleName());
    assertEquals("NativeItem", detailTooltip.lines().get(0).getClass().getSimpleName());
  }

  private static DeliveryState ready(int count, int pageSize, int page) {
    List<DeliveryRow> rows = java.util.stream.IntStream.range(0, count)
        .mapToObj(i -> new DeliveryRow(DeliveryBoxTestFixtures.entry(new UUID(4, i + 1), i + 2)))
        .toList();
    UUID selected = rows.isEmpty() ? null : rows.get(Math.min(page * pageSize, rows.size() - 1)).entryId();
    return new DeliveryState(rows, page, pageSize, "", DeliveryCategory.ALL, selected,
        ScreenState.READY, null, 1, Set.of(DeliveryAction.CLAIM, DeliveryAction.BACK));
  }
}
