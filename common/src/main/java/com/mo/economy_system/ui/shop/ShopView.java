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
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconText(UiIcon.SHOP, "Shop", layout.title().x() + 8, layout.title().y() + 5,
        1.0f, EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE,
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
      String displayName = item.description().isBlank() ? item.itemId() : item.description();
      renderer.textInRect(displayName, new UiRect(card.card().x() + 6, card.card().y() + 6,
          card.card().width() - 12, 14), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      renderer.item(item.itemId(), card.itemIcon());
      renderer.translatedTextInRect("screen.shop.price", List.of(Integer.toString(item.currentPrice())),
          new UiRect(card.card().x() + 6, card.card().y() + 60, card.card().width() - 12, 14),
          EconomyUiTheme.SHOP_ACCENT, UiTextAlignment.CENTER);
      int change = item.currentPrice() - item.lastPrice();
      if (change != 0) {
        renderer.text((change > 0 ? "+" : "") + change,
            card.card().right() - 6 - (Math.abs(change) >= 100 ? 18 : 12),
            card.card().y() + 6,
            change > 0 ? EconomyUiTheme.TEXT_ERROR : EconomyUiTheme.TEXT_SUCCESS);
      }
      // Keep the common action contract explicit; target shells use the full card hitbox and
      // render this one-pixel semantic marker without introducing a second visual button.
      renderer.translatedButton(card.buyButton(), EconomyUiTheme.SHOP_BUTTON, "screen.shop.buy", List.of(),
          card.card().contains(mouseX, mouseY), state.can(ShopAction.BUY));
    }
    boolean previousEnabled = state.page() > 0;
    renderer.button(layout.previousButton(), previousEnabled ? EconomyUiTheme.PAGE_BUTTON : EconomyUiTheme.PAGE_BUTTON_DISABLED,
        "", layout.previousButton().contains(mouseX, mouseY), previousEnabled);
    renderer.icon(UiIcon.ARROW_LEFT, new UiRect(layout.previousButton().x() + 19,
        layout.previousButton().y() + 7, 10, 10));
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    boolean nextEnabled = state.page() + 1 < state.totalPages();
    renderer.button(layout.nextButton(), nextEnabled ? EconomyUiTheme.PAGE_BUTTON : EconomyUiTheme.PAGE_BUTTON_DISABLED,
        "", layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    renderer.icon(UiIcon.ARROW_RIGHT, new UiRect(layout.nextButton().x() + 20,
        layout.nextButton().y() + 7, 10, 10));
  }
}
