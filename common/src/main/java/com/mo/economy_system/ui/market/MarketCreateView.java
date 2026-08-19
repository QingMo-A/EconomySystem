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
    String footerKey = state.mode() == MarketCreateMode.SALES
        ? "screen.market.create.sales_title" : "screen.market.create.demand_title";
    renderer.card(layout.title(), EconomyUiTheme.VERSION_CARD, false);
    renderer.scaledIconTranslatedText(UiIcon.MARKET, footerKey, List.of(),
        layout.title().x() + 8, layout.title().y() + 5, layout.versionInfoScale(),
        EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.market.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    if (state.mode() == MarketCreateMode.SALES) renderer.card(layout.inventoryPanel(), EconomyUiTheme.MARKET_CARD, false);
    renderer.card(layout.formPanel(), state.mode() == MarketCreateMode.SALES ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.SHOP_CARD, false);
    if (state.mode() == MarketCreateMode.SALES) {
      renderer.translatedText("screen.market.create.inventory", List.of(), layout.inventoryPanel().x() + 10, layout.inventoryPanel().y() + 10, EconomyUiTheme.TEXT_PRIMARY);
      for (MarketCreateLayout.Slot slot : layout.slots()) {
        boolean hovered = slot.rect().contains(mouseX, mouseY);
        renderer.card(slot.rect(), slot.item().slot() == state.selectedSlot() ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.DELIVERY_CARD, hovered);
        UiRect itemRect = new UiRect(slot.rect().x() + 4, slot.rect().y() + 4, 16, 16);
        // Keep item and stack count in one target-native semantic operation.  This ensures the
        // count is painted with the native decoration pass above the item model in both loaders.
        renderer.itemWithCount(slot.item().itemId(), slot.item().count(), itemRect);
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
    }
    if (state.mode() == MarketCreateMode.SALES) {
      renderer.button(layout.decrement(), EconomyUiTheme.MARKET_BUTTON, "-1", layout.decrement().contains(mouseX, mouseY), state.can(MarketCreateAction.DECREMENT));
      renderer.button(layout.increment(), EconomyUiTheme.MARKET_BUTTON, "+1", layout.increment().contains(mouseX, mouseY), state.can(MarketCreateAction.INCREMENT));
      renderer.translatedButton(layout.all(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create.all", List.of(), layout.all().contains(mouseX, mouseY), state.can(MarketCreateAction.SELECT_ALL));
    }
    renderer.translatedButton(layout.submit(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create.submit", List.of(), layout.submit().contains(mouseX, mouseY), state.can(MarketCreateAction.SUBMIT));
    if (state.screenState() == ScreenState.ERROR && state.errorKey() != null) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
    }
  }

  /** Paints shared focus chrome beneath the target-owned native EditBoxes. */
  public static void renderInputFrames(EconomyUiRenderer renderer, MarketCreateState state,
                                       MarketCreateLayout.Layout layout, boolean itemFocused,
                                       boolean quantityFocused, boolean priceFocused) {
    var frame = state.screenState() == ScreenState.ERROR
        ? EconomyUiTheme.INPUT_ERROR_FRAME : EconomyUiTheme.MARKET_SEARCH_FRAME;
    if (state.mode() == MarketCreateMode.DEMAND) {
      renderer.inputFrame(layout.itemId(), frame, itemFocused);
    }
    renderer.inputFrame(layout.quantity(), frame, quantityFocused);
    renderer.inputFrame(layout.price(), frame, priceFocused);
  }

  /**
   * Draws the registry-id completion overlay.  Target shells call this after their native
   * widgets have rendered so the popup is never hidden by an EditBox.
   */
  public static void renderCompletionOverlay(EconomyUiRenderer renderer, MarketCreateState state,
                                             MarketCreateLayout.Layout layout, int mouseX, int mouseY,
                                             List<String> itemSuggestions, int completionSelection) {
    if (state.mode() != MarketCreateMode.DEMAND || itemSuggestions == null
        || itemSuggestions.isEmpty() || layout.completionDropdown().width() <= 0) return;
    int start = MarketCreateLayout.completionWindowStart(itemSuggestions.size(), completionSelection);
    int count = Math.min(MarketCreateLayout.COMPLETION_MAX_ROWS, itemSuggestions.size() - start);
    UiRect dropdown = new UiRect(layout.completionDropdown().x(), layout.completionDropdown().y(),
        layout.completionDropdown().width(), count * MarketCreateLayout.COMPLETION_ROW_HEIGHT);
    renderer.card(dropdown, EconomyUiTheme.MARKET_CARD, false);
    for (int i = 0; i < count; i++) {
      int suggestionIndex = start + i;
      UiRect row = new UiRect(dropdown.x(), dropdown.y() + i * MarketCreateLayout.COMPLETION_ROW_HEIGHT,
          dropdown.width(), MarketCreateLayout.COMPLETION_ROW_HEIGHT);
      boolean hovered = row.contains(mouseX, mouseY);
      UiRect textRect = new UiRect(row.x() + 8, row.y(), Math.max(1, row.width() - 12), row.height());
      renderer.textInRect(itemSuggestions.get(suggestionIndex), textRect,
          hovered || suggestionIndex == completionSelection ? EconomyUiTheme.MARKET_ACCENT
              : EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
    }
  }
}
