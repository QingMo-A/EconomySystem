package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Optional;

/** Semantic delivery-box renderer shared by Forge and NeoForge. */
public final class DeliveryView {
  private DeliveryView() {}

  public static void render(EconomyUiRenderer renderer, DeliveryState state,
                            DeliveryLayout.Layout layout, int mouseX, int mouseY) {
    renderer.inputFrame(layout.searchBackground(), EconomyUiTheme.DELIVERY_SEARCH_FRAME,
        layout.search().contains(mouseX, mouseY));
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(com.mo.economy_system.ui.renderer.UiIcon.DELIVERY, "screen.delivery_box.title", List.of(),
        layout.title().x() + 8,
        layout.title().y() + 5, layout.versionInfoScale(), EconomyUiRenderer.ICON_SIZE,
        EconomyUiRenderer.ICON_ADVANCE,
        EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.delivery_box.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.delivery_box.loading", List.of(), layout.message(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.delivery_box.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.DELIVERY_CLAIM_BUTTON,
          "screen.delivery_box.retry", List.of(), layout.message().contains(mouseX, mouseY),
          state.can(DeliveryAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.delivery_box.empty", List.of(), layout.message(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    }

    for (DeliveryLayout.Card card : layout.cards()) {
      var entry = card.row().entry();
      // The legacy delivery renderer intentionally reuses the shop-orange card accent;
      // the claim action itself remains delivery-green.
      renderer.card(card.card(), EconomyUiTheme.SHOP_CARD, card.card().contains(mouseX, mouseY));
      renderer.item(entry.item().itemId(), card.itemIcon());
      int infoX = card.card().x() + 48;
      int maxTextWidth = Math.max(1, card.card().width() - 48 - 8
          - card.claimButton().width() - 6);
      int lineHeight = Math.max(1, layout.metrics().lineHeight());
      renderer.itemDisplayNameWithSuffix(entry.item().itemId(),
          entry.item().count() > 1 ? " x" + entry.item().count() : "",
          new UiRect(infoX, card.card().y() + 8, maxTextWidth, lineHeight),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.delivery_box.item.source", List.of(entry.source()),
          new UiRect(infoX, card.card().y() + 8 + lineHeight + 2,
              maxTextWidth, lineHeight), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(card.claimButton(), EconomyUiTheme.DELIVERY_CLAIM_BUTTON,
          "button.delivery_box.claim", List.of(), card.claimButton().contains(mouseX, mouseY),
          state.can(DeliveryAction.CLAIM));
    }
    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), previousEnabled ? EconomyUiTheme.DELIVERY_PAGE_BUTTON : EconomyUiTheme.DELIVERY_PAGE_BUTTON_DISABLED, "<",
          layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), nextEnabled ? EconomyUiTheme.DELIVERY_PAGE_BUTTON : EconomyUiTheme.DELIVERY_PAGE_BUTTON_DISABLED, ">",
          layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    }
    tooltipAt(layout, mouseX, mouseY).ifPresent(tooltip -> renderer.tooltip(tooltip, mouseX, mouseY));
  }

  public static Optional<TooltipModel> tooltipAt(DeliveryLayout.Layout layout, int mouseX, int mouseY) {
    for (DeliveryLayout.Card card : layout.cards()) {
      if (card.itemIcon().contains(mouseX, mouseY)) {
        var item = card.row().entry().item();
        return Optional.of(new TooltipModel(List.of(new TooltipLine.NativeItem(item.itemId()))));
      }
    }
    return Optional.empty();
  }
}
