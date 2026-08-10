package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.UUID;

/** Semantic order-card renderer for both target shells. */
public final class MarketView {
  private MarketView() {}

  public static void render(EconomyUiRenderer renderer, MarketState state, MarketLayout.Layout layout,
                            UUID viewerId, int mouseX, int mouseY) {
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), MarketLayout.BACKGROUND_COLOR);
    renderer.inputFrame(new UiRect(layout.search().x() - 4, layout.search().y() - 2,
            layout.search().width() + 8, layout.search().height() + 4),
        EconomyUiTheme.MARKET_SEARCH_FRAME, layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.market.search", List.of(), layout.search(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.translatedButton(layout.createSales(), EconomyUiTheme.MARKET_TOP_SALES_BUTTON,
        "screen.market.create_sales", List.of(), layout.createSales().contains(mouseX, mouseY), state.can(MarketAction.CREATE_SALES));
    renderer.translatedButton(layout.createDemand(), EconomyUiTheme.MARKET_TOP_DEMAND_BUTTON,
        "screen.market.create_demand", List.of(), layout.createDemand().contains(mouseX, mouseY), state.can(MarketAction.CREATE_DEMAND));
    for (MarketLayout.FilterTab tab : layout.filterTabs()) {
      boolean selected = tab.filter() == state.filter();
      renderer.translatedTextInRect(filterKey(tab.filter()), List.of(), tab.rect(),
          selected ? EconomyUiTheme.TEXT_PRIMARY : EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
      if (selected) {
        renderer.fill(new UiRect(tab.rect().x(), tab.rect().bottom() - 1,
            Math.max(1, layout.metrics().translatedWidth(filterKey(tab.filter()), List.of())), 1), EconomyUiTheme.MARKET_ACCENT);
      }
    }
    renderer.translatedTextInRect("screen.market.esc", List.of(), layout.esc(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    if (state.screenState() == ScreenState.LOADING) renderer.translatedTextInRect("screen.market.loading", List.of(), layout.message(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.market.sync_failed" : state.errorKey(), List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.MARKET_BUTTON, "screen.market.retry", List.of(), layout.message().contains(mouseX, mouseY), state.can(MarketAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) renderer.translatedTextInRect("screen.market.empty", List.of(), layout.message(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    for (MarketLayout.Card card : layout.cards()) {
      var order = card.row().order();
      boolean own = viewerId != null && viewerId.equals(order.ownerId());
      renderer.card(card.card(), own ? EconomyUiTheme.DELIVERY_CARD
          : order.type() == MarketOrderType.SALES ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.SHOP_CARD,
          card.card().contains(mouseX, mouseY));
      renderer.item(order.item().itemId(), card.itemIcon());
      int left = card.card().x() + 8;
      int right = card.card().right() - 8;
      renderer.translatedTextInRect(own ? "screen.market.filter.mine"
          : order.type() == MarketOrderType.SALES ? "screen.market.sales" : "screen.market.demand", List.of(),
          new UiRect(left, card.card().y() + 6, 104, 14), own ? EconomyUiTheme.DELIVERY_ACCENT : EconomyUiTheme.TEXT_SECONDARY,
          UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.market.price", List.of(Integer.toString(order.totalPrice())),
          new UiRect(left + 104, card.card().y() + 6, Math.max(1, right - left - 104), 14), EconomyUiTheme.MARKET_ACCENT,
          UiTextAlignment.RIGHT);
      UiRect itemNameRect = new UiRect(left, card.card().y() + 22, right - left - 34, 14);
      renderer.itemDisplayName(order.item().itemId(), itemNameRect,
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.textInRect(" x" + order.quantity(),
          new UiRect(itemNameRect.right(), itemNameRect.y(), 34, itemNameRect.height()),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      String ownerKey = order.type() == MarketOrderType.SALES ? "screen.market.seller" : "screen.market.requester";
      int ownerLabelWidth = Math.min(80, Math.max(1, layout.metrics().translatedWidth(ownerKey, List.of())));
      renderer.translatedTextInRect(ownerKey, List.of(), new UiRect(left, card.card().bottom() - 20, ownerLabelWidth, 14),
          EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.textInRect(": " + order.ownerName(), new UiRect(left + ownerLabelWidth, card.card().bottom() - 20,
          Math.max(1, right - left - ownerLabelWidth - 70), 14), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      if (!own && order.type() == MarketOrderType.SALES
          && state.can(MarketAction.ADMIN_REMOVE_SALES)) {
        renderer.translatedButton(card.adminActionButton(), EconomyUiTheme.MARKET_ACTION_REMOVE,
            actionKey(MarketAction.ADMIN_REMOVE_SALES), List.of(),
            card.adminActionButton().contains(mouseX, mouseY), true);
      }
      MarketAction action = actionFor(order, viewerId);
      renderer.translatedButton(card.actionButton(), styleForAction(action),
          actionKey(action), List.of(), card.actionButton().contains(mouseX, mouseY), action != null && state.can(action));
    }
    renderer.button(layout.previousButton(), state.page() > 0 ? EconomyUiTheme.SHOP_PAGE_BUTTON : EconomyUiTheme.SHOP_PAGE_BUTTON_DISABLED,
        "", layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
    renderer.icon(UiIcon.ARROW_LEFT, new UiRect(layout.previousButton().x() + 19,
        layout.previousButton().y() + 6, 12, 12));
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.button(layout.nextButton(), state.page() + 1 < state.totalPages() ? EconomyUiTheme.SHOP_PAGE_BUTTON : EconomyUiTheme.SHOP_PAGE_BUTTON_DISABLED,
        "", layout.nextButton().contains(mouseX, mouseY), state.page() + 1 < state.totalPages());
    renderer.icon(UiIcon.ARROW_RIGHT, new UiRect(layout.nextButton().x() + 19,
        layout.nextButton().y() + 6, 12, 12));
  }

  public static MarketAction actionFor(com.mo.economy_system.common.network.MarketOrderSnapshot order, UUID viewerId) {
    boolean own = viewerId != null && viewerId.equals(order.ownerId());
    if (order.type() == MarketOrderType.SALES) return own ? MarketAction.REMOVE_SALES : MarketAction.BUY;
    if (own) return order.delivered() ? MarketAction.CONFIRM_DEMAND : MarketAction.REMOVE_DEMAND;
    return order.delivered() ? null : MarketAction.DELIVER_DEMAND;
  }
  private static String actionKey(MarketAction action) {
    if (action == null) return "screen.market.done";
    return switch (action) {
      case BUY -> "screen.market.buy";
      case REMOVE_SALES -> "screen.market.remove_sales";
      case ADMIN_REMOVE_SALES -> "screen.market.remove_sales";
      case DELIVER_DEMAND -> "screen.market.deliver";
      case CONFIRM_DEMAND -> "screen.market.confirm";
      case REMOVE_DEMAND -> "screen.market.remove_demand";
      default -> "screen.market.done";
    };
  }

  private static com.mo.economy_system.ui.theme.UiButtonStyle styleForAction(MarketAction action) {
    if (action == null) return EconomyUiTheme.MARKET_ACTION_DISABLED;
    return switch (action) {
      case BUY -> EconomyUiTheme.MARKET_ACTION_BUY;
      case REMOVE_SALES -> EconomyUiTheme.MARKET_ACTION_REMOVE;
      case ADMIN_REMOVE_SALES -> EconomyUiTheme.MARKET_ACTION_REMOVE;
      case DELIVER_DEMAND -> EconomyUiTheme.MARKET_ACTION_DELIVER;
      case CONFIRM_DEMAND -> EconomyUiTheme.MARKET_ACTION_CONFIRM;
      case REMOVE_DEMAND -> EconomyUiTheme.MARKET_ACTION_CANCEL;
      default -> EconomyUiTheme.MARKET_ACTION_DISABLED;
    };
  }
  private static String filterKey(com.mo.economy_system.common.network.MarketOrderFilter filter) {
    return switch (filter) {
      case ALL -> "screen.market.filter.all";
      case MINE -> "screen.market.filter.mine";
      case SALES -> "screen.market.filter.sales";
      case DEMAND -> "screen.market.filter.demand";
    };
  }
}
