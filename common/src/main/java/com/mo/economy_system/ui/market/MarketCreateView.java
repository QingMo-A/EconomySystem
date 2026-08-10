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
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconText(UiIcon.MARKET, "Market", layout.title().x() + 8, layout.title().y() + 5,
        1.0f, EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.market.esc", List.of(),
        new UiRect(layout.scale().virtualWidth() - 102, layout.title().y(), 90, layout.title().height()),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    if (state.mode() == MarketCreateMode.SALES) renderer.card(layout.inventoryPanel(), EconomyUiTheme.MARKET_CARD, false);
    renderer.card(layout.formPanel(), state.mode() == MarketCreateMode.SALES ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.SHOP_CARD, false);
    if (state.mode() == MarketCreateMode.SALES) {
      renderer.translatedText("screen.market.create.inventory", List.of(), layout.inventoryPanel().x() + 10, layout.inventoryPanel().y() + 10, EconomyUiTheme.TEXT_PRIMARY);
      for (MarketCreateLayout.Slot slot : layout.slots()) {
        boolean hovered = slot.rect().contains(mouseX, mouseY);
        renderer.card(slot.rect(), slot.item().slot() == state.selectedSlot() ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.DELIVERY_CARD, hovered);
        renderer.item(slot.item().itemId(), new UiRect(slot.rect().x() + 4, slot.rect().y() + 4, 16, 16));
        renderer.text(Integer.toString(slot.item().count()), slot.rect().x() + 3, slot.rect().bottom() - 9, EconomyUiTheme.TEXT_PRIMARY);
      }
      renderer.translatedText("screen.market.create.selected", List.of(), layout.formPanel().x() + 10, layout.formPanel().y() + 10, EconomyUiTheme.TEXT_PRIMARY);
      MarketInventoryItem selected = state.selectedItem();
      if (selected != null) {
        renderer.item(selected.itemId(), new UiRect(layout.formPanel().x() + 12, layout.formPanel().y() + 36, 32, 32));
        renderer.itemDisplayName(selected.itemId(),
            new UiRect(layout.formPanel().x() + 52, layout.formPanel().y() + 38,
                Math.max(1, layout.formPanel().width() - 64), 14),
            EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
        renderer.textInRect("Owned: " + state.availableQuantity(), new UiRect(layout.formPanel().x() + 52, layout.formPanel().y() + 54,
            Math.max(1, layout.formPanel().width() - 64), 14), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      }
    } else {
      renderer.translatedText("screen.market.create.item_id", List.of(), layout.formPanel().x() + 12, layout.itemId().y() + 6, EconomyUiTheme.TEXT_SECONDARY);
    }
    renderer.translatedText("screen.market.create.quantity", List.of(), layout.formPanel().x() + 12, layout.quantity().y() + 6, EconomyUiTheme.TEXT_SECONDARY);
    renderer.translatedText("screen.market.create.price", List.of(), layout.formPanel().x() + 12, layout.price().y() + 6, EconomyUiTheme.TEXT_SECONDARY);
    if (state.mode() == MarketCreateMode.SALES) {
      renderer.button(layout.decrement(), EconomyUiTheme.MARKET_BUTTON, "-1", layout.decrement().contains(mouseX, mouseY), state.can(MarketCreateAction.DECREMENT));
      renderer.button(layout.increment(), EconomyUiTheme.MARKET_BUTTON, "+1", layout.increment().contains(mouseX, mouseY), state.can(MarketCreateAction.INCREMENT));
      renderer.translatedButton(layout.all(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create.all", List.of(), layout.all().contains(mouseX, mouseY), state.can(MarketCreateAction.SELECT_ALL));
    }
    renderer.translatedButton(layout.submit(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create.submit", List.of(), layout.submit().contains(mouseX, mouseY), state.can(MarketCreateAction.SUBMIT));
    renderer.translatedButton(layout.back(), EconomyUiTheme.DISABLED_BUTTON, "screen.market.create.back", List.of(), layout.back().contains(mouseX, mouseY), state.can(MarketCreateAction.BACK));
    if (state.screenState() == ScreenState.ERROR && state.errorKey() != null) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
    }
  }
}
