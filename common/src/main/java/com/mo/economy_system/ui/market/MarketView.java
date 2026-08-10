package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.TooltipLine;
import com.mo.economy_system.ui.renderer.TooltipModel;
import com.mo.economy_system.ui.renderer.UiNativeInputFrame;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Semantic order-card renderer for both target shells. */
public final class MarketView {
  private static final DateTimeFormatter EXPIRATION_FORMATTER =
      DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  private MarketView() {}

  /** Draws the native search field frame in physical target pixels. */
  public static void renderSearchFrame(EconomyUiRenderer renderer, UiRect nativeWidgetRect,
                                       boolean focused) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect, EconomyUiTheme.MARKET_SEARCH_FRAME, focused);
  }

  public static void render(EconomyUiRenderer renderer, MarketState state, MarketLayout.Layout layout,
                            UUID viewerId, int mouseX, int mouseY) {
    renderer.translatedButton(layout.createSales(), EconomyUiTheme.MARKET_TOP_SALES_BUTTON,
        "screen.market.create_sales", List.of(), layout.createSales().contains(mouseX, mouseY),
        state.can(MarketAction.CREATE_SALES));
    renderer.translatedButton(layout.createDemand(), EconomyUiTheme.MARKET_TOP_DEMAND_BUTTON,
        "screen.market.create_demand", List.of(), layout.createDemand().contains(mouseX, mouseY),
        state.can(MarketAction.CREATE_DEMAND));
    for (MarketLayout.FilterTab tab : layout.filterTabs()) {
      boolean selected = tab.filter() == state.filter();
      renderer.translatedTextInRect(filterKey(tab.filter()), List.of(), tab.textRect(),
          selected ? EconomyUiTheme.TEXT_PRIMARY : EconomyUiTheme.TEXT_MUTED,
          UiTextAlignment.LEFT);
      if (selected) {
        renderer.fill(new UiRect(tab.textRect().x(), tab.textRect().bottom() - 1,
            Math.max(1, layout.metrics().translatedWidth(filterKey(tab.filter()), List.of())), 1),
            EconomyUiTheme.MARKET_ACCENT);
      }
    }
    renderer.translatedTextInRect("screen.market.esc", List.of(), layout.esc(),
        EconomyUiTheme.TEXT_MUTED, UiTextAlignment.RIGHT);
    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.market.loading", List.of(), layout.message(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(state.errorKey() == null ? "screen.market.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.message(), EconomyUiTheme.MARKET_BUTTON,
          "screen.market.retry", List.of(), layout.message().contains(mouseX, mouseY),
          state.can(MarketAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.market.empty", List.of(), layout.message(),
          EconomyUiTheme.TEXT_MUTED, UiTextAlignment.CENTER);
    }
    for (MarketLayout.Card card : layout.cards()) {
      var order = card.row().order();
      boolean own = viewerId != null && viewerId.equals(order.ownerId());
      renderer.card(card.card(), own ? EconomyUiTheme.DELIVERY_CARD
          : order.type() == MarketOrderType.SALES ? EconomyUiTheme.MARKET_CARD
          : EconomyUiTheme.SHOP_CARD, card.card().contains(mouseX, mouseY));
      renderer.item(order.item().itemId(), card.itemIcon());
      int left = card.card().x() + 8;
      int right = card.card().right() - 8;
      int lineHeight = Math.max(1, layout.metrics().lineHeight());
      renderer.translatedTextInRect(own ? "screen.market.filter.mine"
          : order.type() == MarketOrderType.SALES ? "screen.market.sales" : "screen.market.demand", List.of(),
          new UiRect(left, card.card().y() + 6, 104, 14),
          own ? EconomyUiTheme.DELIVERY_ACCENT
              : order.type() == MarketOrderType.SALES ? EconomyUiTheme.MARKET_ACCENT
              : EconomyUiTheme.SHOP_ACCENT,
          UiTextAlignment.LEFT);
      renderer.textInRect("\uFFE5" + UiNumbers.formatLegacyMarketNumber(order.totalPrice()),
          new UiRect(left + 104, card.card().y() + 6,
              Math.max(1, right - left - 104), 14), EconomyUiTheme.BALANCE_ACCENT,
          UiTextAlignment.RIGHT);
      UiRect itemNameRect = new UiRect(left, card.card().y() + 6 + lineHeight + 2,
          Math.max(1, right - left), lineHeight);
      renderer.itemDisplayNameWithSuffix(order.item().itemId(), " x" + order.quantity(), itemNameRect,
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.LEFT);
      String ownerKey = order.type() == MarketOrderType.SALES
          ? "screen.market.seller" : "screen.market.requester";
      int ownerWidth = Math.max(1, right - left - MarketLayout.ACTION_BUTTON_WIDTH
          - MarketLayout.ACTION_BUTTON_GAP);
      renderer.translatedTextWithSuffix(ownerKey, List.of(), ": " + order.ownerName(),
          new UiRect(left, card.card().bottom() - 12, ownerWidth, lineHeight),
          EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
      if (!own && order.type() == MarketOrderType.SALES
          && state.can(MarketAction.ADMIN_REMOVE_SALES)) {
        renderer.translatedButton(card.adminActionButton(), EconomyUiTheme.MARKET_ACTION_REMOVE,
            actionKey(MarketAction.ADMIN_REMOVE_SALES), List.of(),
            card.adminActionButton().contains(mouseX, mouseY), true);
      }
      MarketAction action = actionFor(order, viewerId);
      renderer.translatedButton(card.actionButton(), styleForAction(action), actionKey(action),
          List.of(), card.actionButton().contains(mouseX, mouseY),
          action != null && state.can(action));
    }
    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), previousEnabled ? EconomyUiTheme.MARKET_PAGE_BUTTON
          : EconomyUiTheme.MARKET_PAGE_BUTTON_DISABLED, "<",
          layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), nextEnabled ? EconomyUiTheme.MARKET_PAGE_BUTTON
          : EconomyUiTheme.MARKET_PAGE_BUTTON_DISABLED, ">",
          layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    }
    tooltipAt(state, layout, mouseX, mouseY)
        .ifPresent(tooltip -> renderer.tooltip(tooltip, mouseX, mouseY));
  }

  public static MarketAction actionFor(com.mo.economy_system.common.network.MarketOrderSnapshot order,
                                       UUID viewerId) {
    boolean own = viewerId != null && viewerId.equals(order.ownerId());
    if (order.type() == MarketOrderType.SALES) return own ? MarketAction.REMOVE_SALES : MarketAction.BUY;
    if (own) return order.delivered() ? MarketAction.CONFIRM_DEMAND : MarketAction.REMOVE_DEMAND;
    return order.delivered() ? null : MarketAction.DELIVER_DEMAND;
  }

  /** Legacy market icon tooltip plus metadata-only card hover. */
  public static Optional<TooltipModel> tooltipAt(MarketState state, MarketLayout.Layout layout,
                                                 int mouseX, int mouseY) {
    for (MarketLayout.Card card : layout.cards()) {
      if (!card.card().contains(mouseX, mouseY)) continue;
      var order = card.row().order();
      boolean icon = card.itemIcon().contains(mouseX, mouseY);
      List<TooltipLine> lines = new ArrayList<>();
      if (icon) lines.add(new TooltipLine.NativeItem(order.item().itemId()));
      lines.add(new TooltipLine.ColoredLiteral("\u7269\u54C1ID: " + order.item().itemId(), 0xFFAAAAAA));
      lines.add(new TooltipLine.ColoredLiteral("\u5546\u54C1ID: " + order.tradeId(), 0xFF555555));
      lines.add(new TooltipLine.ColoredLiteral("\u8FC7\u671F\u65F6\u95F4: "
          + EXPIRATION_FORMATTER.format(Instant.ofEpochMilli(order.expirationTime())), 0xFFFFAA00));
      long remainingMillis = Math.max(0L, order.expirationTime() - System.currentTimeMillis());
      lines.add(new TooltipLine.ColoredLiteral("\u5269\u4F59\u65F6\u95F4: " + formatDuration(remainingMillis), 0xFFFFFF55));
      return Optional.of(new TooltipModel(lines));
    }
    return Optional.empty();
  }

  static String formatDuration(long millis) {
    long totalSeconds = millis / 1000L;
    long days = totalSeconds / 86400L;
    long hours = (totalSeconds % 86400L) / 3600L;
    long minutes = (totalSeconds % 3600L) / 60L;
    if (days > 0) return days + "\u5929 " + hours + "\u5C0F\u65F6";
    if (hours > 0) return hours + "\u5C0F\u65F6 " + minutes + "\u5206\u949F";
    return minutes + "\u5206\u949F";
  }

  private static String actionKey(MarketAction action) {
    if (action == null) return "screen.market.done";
    return switch (action) {
      case BUY -> "screen.market.buy";
      case REMOVE_SALES, ADMIN_REMOVE_SALES -> "screen.market.remove_sales";
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
      case REMOVE_SALES, ADMIN_REMOVE_SALES -> EconomyUiTheme.MARKET_ACTION_REMOVE;
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
