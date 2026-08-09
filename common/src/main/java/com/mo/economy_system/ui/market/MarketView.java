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
    renderer.fill(new UiRect(0, 0, layout.scale().virtualWidth(), layout.scale().virtualHeight()), 0xB0000000);
    renderer.card(layout.search(), EconomyUiTheme.MARKET_CARD, layout.search().contains(mouseX, mouseY));
    renderer.translatedTextInRect("screen.market.search", List.of(), layout.search(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(filterKey(state.filter()), List.of(), layout.filter(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.translatedButton(layout.createSales(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create_sales", List.of(), layout.createSales().contains(mouseX, mouseY), state.can(MarketAction.CREATE_SALES));
    renderer.translatedButton(layout.createDemand(), EconomyUiTheme.MARKET_BUTTON, "screen.market.create_demand", List.of(), layout.createDemand().contains(mouseX, mouseY), state.can(MarketAction.CREATE_DEMAND));
    renderer.icon(UiIcon.MARKET, new UiRect(layout.title().x(), layout.title().y(), 12, 12));
    renderer.translatedText("screen.market.title", List.of(), layout.title().x() + 16, layout.title().y(), EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect("screen.market.esc", List.of(), layout.esc(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    if (state.screenState() == ScreenState.LOADING) renderer.translatedTextInRect("screen.market.loading", List.of(), layout.message(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.market.sync_failed" : state.errorKey(), List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.MARKET_BUTTON, "screen.market.retry", List.of(), layout.message().contains(mouseX, mouseY), state.can(MarketAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) renderer.translatedTextInRect("screen.market.empty", List.of(), layout.message(), EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    for (MarketLayout.Card card : layout.cards()) {
      var order = card.row().order();
      renderer.card(card.card(), order.type() == MarketOrderType.SALES ? EconomyUiTheme.MARKET_CARD : EconomyUiTheme.DELIVERY_CARD, card.card().contains(mouseX, mouseY));
      renderer.item(order.item().itemId(), card.itemIcon());
      renderer.textInRect(order.ownerName(), new UiRect(card.card().x() + 46, card.card().y() + 10, card.card().width() - 114, 14), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect(order.type() == MarketOrderType.SALES ? "screen.market.sales" : "screen.market.demand", List.of(), new UiRect(card.card().x() + 46, card.card().y() + 25, 80, 14), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.market.price", List.of(Integer.toString(order.totalPrice())), new UiRect(card.card().x() + 46, card.card().y() + 42, 78, 14), EconomyUiTheme.MARKET_ACCENT, UiTextAlignment.LEFT);
      MarketAction action = actionFor(order, viewerId);
      renderer.translatedButton(card.actionButton(), action == null ? EconomyUiTheme.DISABLED_BUTTON : EconomyUiTheme.MARKET_BUTTON,
          actionKey(action), List.of(), card.actionButton().contains(mouseX, mouseY), action != null && state.can(action));
    }
    renderer.button(layout.previousButton(), EconomyUiTheme.MARKET_BUTTON, "<", layout.previousButton().contains(mouseX, mouseY), state.page() > 0);
    renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.button(layout.nextButton(), EconomyUiTheme.MARKET_BUTTON, ">", layout.nextButton().contains(mouseX, mouseY), state.page() + 1 < state.totalPages());
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
      case DELIVER_DEMAND -> "screen.market.deliver";
      case CONFIRM_DEMAND -> "screen.market.confirm";
      case REMOVE_DEMAND -> "screen.market.remove_demand";
      default -> "screen.market.done";
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
