package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic renderer for market create-sales/create-demand forms. */
public final class MarketCreateView {
  private MarketCreateView() {}

  public static void render(EconomyUiRenderer renderer, MarketCreateState state,
                            MarketCreateLayout.Layout layout, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.icon(UiIcon.MARKET, new UiRect(layout.title().x(), layout.title().y() + 2, 14, 14));
    String title = state.mode() == MarketCreateMode.SALES ? "screen.market.create.sales_title" : "screen.market.create.demand_title";
    renderer.translatedText(title, List.of(), layout.title().x() + 18, layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
    renderer.card(layout.inventoryPanel(), EconomyUiTheme.MARKET_CARD, false);
    renderer.card(layout.formPanel(), EconomyUiTheme.DELIVERY_CARD, false);
    if (state.mode() == MarketCreateMode.SALES) {
      renderer.translatedText("screen.market.create.inventory", List.of(), layout.inventoryPanel().x() + 10, layout.inventoryPanel().y() + 10, EconomyUiTheme.TEXT_PRIMARY);
      for (MarketCreateLayout.Slot slot : layout.slots()) {
        boolean hovered = slot.rect().contains(mouseX, mouseY);
        renderer.card(slot.rect(), slot.item().slot() == state.selectedSlot() ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.DELIVERY_CARD, hovered);
        renderer.item(slot.item().itemId(), new UiRect(slot.rect().x() + 4, slot.rect().y() + 4, 16, 16));
        renderer.text(Integer.toString(slot.item().count()), slot.rect().x() + 3, slot.rect().bottom() - 9, EconomyUiTheme.TEXT_PRIMARY);
      }
      renderer.translatedText("screen.market.create.selected", List.of(), layout.formPanel().x() + 10, layout.formPanel().y() + 10, EconomyUiTheme.TEXT_PRIMARY);
    } else {
      renderer.translatedText("screen.market.create.item_id", List.of(), layout.formPanel().x() + 10, layout.formPanel().y() + 58, EconomyUiTheme.TEXT_SECONDARY);
    }
    renderer.translatedText("screen.market.create.quantity", List.of(), layout.formPanel().x() + 10, layout.formPanel().y() + 90, EconomyUiTheme.TEXT_SECONDARY);
    renderer.translatedText("screen.market.create.price", List.of(), layout.formPanel().x() + 10, layout.formPanel().y() + 122, EconomyUiTheme.TEXT_SECONDARY);
    renderer.button(layout.decrement(), EconomyUiTheme.MARKET_BUTTON, "-", layout.decrement().contains(mouseX, mouseY), state.can(MarketCreateAction.DECREMENT));
    renderer.button(layout.increment(), EconomyUiTheme.MARKET_BUTTON, "+", layout.increment().contains(mouseX, mouseY), state.can(MarketCreateAction.INCREMENT));
    renderer.translatedButton(layout.all(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create.all", List.of(), layout.all().contains(mouseX, mouseY), state.can(MarketCreateAction.SELECT_ALL));
    renderer.translatedButton(layout.submit(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create.submit", List.of(), layout.submit().contains(mouseX, mouseY), state.can(MarketCreateAction.SUBMIT));
    renderer.translatedButton(layout.back(), EconomyUiTheme.DISABLED_BUTTON, "screen.market.create.back", List.of(), layout.back().contains(mouseX, mouseY), state.can(MarketCreateAction.BACK));
    if (state.screenState() == ScreenState.ERROR && state.errorKey() != null) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
    }
  }
}
