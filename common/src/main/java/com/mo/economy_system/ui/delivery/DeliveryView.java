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
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.card(layout.searchBackground(), EconomyUiTheme.DELIVERY_CARD,
        layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.delivery_box.search", List.of(), layout.search(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.icon(UiIcon.DELIVERY, new UiRect(layout.title().x(), layout.title().y(), 12, 12));
    renderer.translatedText("screen.delivery_box.title", List.of(), layout.title().x() + 16,
        layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
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
      renderer.card(card.card(), EconomyUiTheme.DELIVERY_CARD, card.card().contains(mouseX, mouseY));
      renderer.item(entry.item().itemId(), card.itemIcon());
      renderer.textInRect(entry.item().itemId(),
          new UiRect(card.card().x() + 46, card.card().y() + 8,
              card.card().width() - 118, 14), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.delivery_box.item.source", List.of(entry.source()),
          new UiRect(card.card().x() + 46, card.card().y() + 25,
              card.card().width() - 54, 14), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.delivery_box.item.name_and_count",
          List.of(entry.item().itemId(), Integer.toString(entry.item().count())),
          new UiRect(card.card().x() + 8, card.card().y() + 51, 90, 14),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
      renderer.translatedButton(card.claimButton(), EconomyUiTheme.HOME_DELIVERY_BUTTON,
          "button.delivery_box.claim", List.of(), card.claimButton().contains(mouseX, mouseY),
          state.can(DeliveryAction.CLAIM));
    }
    renderer.button(layout.previousButton(), EconomyUiTheme.HOME_DELIVERY_BUTTON, "<",
        layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.button(layout.nextButton(), EconomyUiTheme.HOME_DELIVERY_BUTTON, ">",
        layout.nextButton().contains(mouseX, mouseY), state.page() + 1 < state.totalPages());
  }
}
