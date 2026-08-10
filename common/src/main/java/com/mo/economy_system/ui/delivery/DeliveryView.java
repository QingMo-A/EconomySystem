package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic delivery-box renderer shared by Forge and NeoForge. */
public final class DeliveryView {
  private DeliveryView() {}

  public static void render(EconomyUiRenderer renderer, DeliveryState state,
                            DeliveryLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), DeliveryLayout.BACKGROUND_COLOR);
    renderer.card(layout.searchBackground(), EconomyUiTheme.DELIVERY_CARD,
        layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.delivery_box.search", List.of(), layout.search(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconText(UiIcon.DELIVERY, "Delivery Box", layout.title().x() + 8,
        layout.title().y() + 5, 1.0f, EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE,
        EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.delivery_box.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.delivery_box.loading", List.of(), layout.message(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.delivery_box.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.HOME_DELIVERY_BUTTON,
          "screen.delivery_box.retry", List.of(), layout.message().contains(mouseX, mouseY),
          state.can(DeliveryAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.delivery_box.empty", List.of(), layout.message(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    }

    for (DeliveryLayout.Card card : layout.cards()) {
      var entry = card.row().entry();
      renderer.card(card.card(), EconomyUiTheme.SHOP_CARD, card.card().contains(mouseX, mouseY));
      renderer.item(entry.item().itemId(), card.itemIcon());
      String itemName = entry.item().itemId() + (entry.item().count() > 1 ? " x" + entry.item().count() : "");
      renderer.textInRect(itemName,
          new UiRect(card.card().x() + 48, card.card().y() + 8,
              card.card().width() - 48 - 8 - 60 - 6, 14), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.delivery_box.item.source", List.of(entry.source()),
          new UiRect(card.card().x() + 48, card.card().y() + 25,
              card.card().width() - 48 - 8, 14), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedButton(card.claimButton(), EconomyUiTheme.HOME_DELIVERY_BUTTON,
          "button.delivery_box.claim", List.of(), card.claimButton().contains(mouseX, mouseY),
          state.can(DeliveryAction.CLAIM));
    }
    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), previousEnabled ? EconomyUiTheme.PAGE_BUTTON : EconomyUiTheme.PAGE_BUTTON_DISABLED, "<",
          layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), nextEnabled ? EconomyUiTheme.PAGE_BUTTON : EconomyUiTheme.PAGE_BUTTON_DISABLED, ">",
          layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    }
  }
}
