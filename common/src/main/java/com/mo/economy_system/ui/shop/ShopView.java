package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Optional;

/** Semantic shop card view shared by both targets. */
public final class ShopView {
  private ShopView() {}

  public static void render(EconomyUiRenderer renderer, ShopState state, ShopLayout.Layout layout,
                            int mouseX, int mouseY) {
    renderer.inputFrame(layout.searchBackground(), EconomyUiTheme.SHOP_SEARCH_FRAME,
        layout.search().contains(mouseX, mouseY));
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(UiIcon.SHOP, "screen.shop.title", List.of(),
        layout.title().x() + 8, layout.title().y() + 5,
        layout.versionInfoScale(), EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE,
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
      int lineHeight = Math.max(1, layout.metrics().lineHeight());
      renderer.itemDisplayName(item.itemId(), new UiRect(card.card().x() + 6, card.card().y() + 6,
          card.card().width() - 12, lineHeight), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.item(item.itemId(), card.itemIcon());
      String formattedPrice = "\uffe5" + UiNumbers.formatInteger(item.currentPrice());
      renderer.textInRect(formattedPrice, new UiRect(card.card().x() + 6, card.card().y() + 6,
          card.card().width() - 12, lineHeight), EconomyUiTheme.BALANCE_ACCENT, UiTextAlignment.RIGHT);
    }
    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), previousEnabled ? EconomyUiTheme.SHOP_PAGE_BUTTON : EconomyUiTheme.SHOP_PAGE_BUTTON_DISABLED,
          "", layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.icon(UiIcon.ARROW_LEFT, new UiRect(layout.previousButton().x() + 19,
          layout.previousButton().y() + 6, 12, 12));
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), nextEnabled ? EconomyUiTheme.SHOP_PAGE_BUTTON : EconomyUiTheme.SHOP_PAGE_BUTTON_DISABLED,
          "", layout.nextButton().contains(mouseX, mouseY), nextEnabled);
      renderer.icon(UiIcon.ARROW_RIGHT, new UiRect(layout.nextButton().x() + 19,
          layout.nextButton().y() + 6, 12, 12));
    }
    tooltipAt(state, layout, mouseX, mouseY).ifPresent(tooltip -> renderer.tooltip(tooltip, mouseX, mouseY));
  }

  /** Legacy card tooltip: native item information followed by exact price metadata. */
  public static Optional<TooltipModel> tooltipAt(ShopState state, ShopLayout.Layout layout,
                                                 int mouseX, int mouseY) {
    for (ShopLayout.Card card : layout.cards()) {
      if (!card.card().contains(mouseX, mouseY)) continue;
      var item = card.row().item();
      int change = item.currentPrice() - item.lastPrice();
      String changeText = change > 0 ? "+" + change : Integer.toString(change);
      int changeColor = change > 0 ? EconomyUiTheme.TOOLTIP_PRICE_UP
          : change < 0 ? EconomyUiTheme.TOOLTIP_PRICE_DOWN : EconomyUiTheme.TOOLTIP_PRICE_SAME;
      return Optional.of(new TooltipModel(List.of(
          new TooltipLine.NativeItem(item.itemId()),
          new TooltipLine.Literal("-=-=-=-=-=-"),
          new TooltipLine.ColoredTranslated("screen.shop.item.change_price", List.of(changeText), changeColor),
          new TooltipLine.Translated("screen.shop.item.basic_price", List.of(UiNumbers.formatInteger(item.basePrice()))),
          new TooltipLine.Translated("screen.shop.item.current_price", List.of(UiNumbers.formatInteger(item.currentPrice()))),
          new TooltipLine.Translated("screen.shop.item.fluctuation_factor", List.of(Double.toString(item.fluctuationFactor()))),
          new TooltipLine.Translated("screen.shop.item.id", List.of(item.itemId())))));
    }
    return Optional.empty();
  }
}
