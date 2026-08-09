package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic shop card view shared by both targets. */
public final class ShopView {
  private ShopView() {}

  public static void render(EconomyUiRenderer renderer, ShopState state, ShopLayout.Layout layout,
                            int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.card(layout.searchBackground(), EconomyUiTheme.SHOP_CARD, layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.shop.search", List.of(), layout.search(), EconomyUiTheme.TEXT_MUTED,
        UiTextAlignment.LEFT);
    renderer.icon(UiIcon.SHOP, new UiRect(layout.title().x(), layout.title().y(), 12, 12));
    renderer.translatedText("screen.shop.title", List.of(), layout.title().x() + 16, layout.title().y(),
        EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.shop.esc", List.of(), layout.esc(), EconomyUiTheme.TEXT_MUTED,
        UiTextAlignment.RIGHT);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.shop.loading", List.of(), layout.message(), EconomyUiTheme.TEXT_PRIMARY,
          UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.shop.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.SHOP_BUTTON, "screen.shop.retry", List.of(),
          layout.message().contains(mouseX, mouseY), state.can(ShopAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.shop.empty", List.of(), layout.message(), EconomyUiTheme.TEXT_MUTED,
          UiTextAlignment.CENTER);
    }

    for (ShopLayout.Card card : layout.cards()) {
      var item = card.row().item();
      renderer.card(card.card(), EconomyUiTheme.SHOP_CARD, card.card().contains(mouseX, mouseY));
      renderer.textInRect(item.itemId(), new UiRect(card.card().x() + 6, card.card().y() + 6,
          card.card().width() - 12, 14), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      renderer.item(item.itemId(), card.itemIcon());
      renderer.translatedTextInRect("screen.shop.price", List.of(Integer.toString(item.currentPrice())),
          new UiRect(card.card().x() + 6, card.card().y() + 55, card.card().width() - 12, 14),
          EconomyUiTheme.SHOP_ACCENT, UiTextAlignment.CENTER);
      renderer.translatedButton(card.buyButton(), EconomyUiTheme.SHOP_BUTTON, "screen.shop.buy", List.of(),
          card.buyButton().contains(mouseX, mouseY), state.can(ShopAction.BUY));
    }
    renderer.button(layout.previousButton(), EconomyUiTheme.SHOP_BUTTON, "<",
        layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.button(layout.nextButton(), EconomyUiTheme.SHOP_BUTTON, ">",
        layout.nextButton().contains(mouseX, mouseY), state.page() + 1 < state.totalPages());
  }
}
