package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.component.UiPanel;
import com.mo.economy_system.ui.component.UiSection;
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

/** Market v2: compact order browser with a persistent inline detail/action pane. */
public final class MarketView {
  private static final DateTimeFormatter EXPIRATION_FORMATTER =
      DateTimeFormatter.ofPattern("MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());

  private MarketView() {}

  public static void renderSearchFrame(
      EconomyUiRenderer renderer, UiRect nativeWidgetRect, boolean focused) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect, EconomyUiTheme.MARKET_SEARCH_FRAME, focused);
  }

  public static void renderSearchFrame(
      EconomyUiRenderer renderer, UiRect nativeWidgetRect, boolean focused, boolean hovered) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect, EconomyUiTheme.MARKET_SEARCH_FRAME,
        focused, hovered);
  }

  /** Compatibility overload for callers/tests that have no selected detail state. */
  public static void render(
      EconomyUiRenderer renderer,
      MarketState state,
      MarketLayout.Layout layout,
      UUID viewerId,
      int mouseX,
      int mouseY) {
    render(renderer, state, layout, null, viewerId, mouseX, mouseY);
  }

  public static void render(
      EconomyUiRenderer renderer,
      MarketState state,
      MarketLayout.Layout layout,
      MarketDetailState detailState,
      UUID viewerId,
      int mouseX,
      int mouseY) {
    UiPanel.render(renderer, layout.catalogArea(), false);
    UiPanel.render(renderer, layout.detailPanel(), false);

    renderer.translatedButton(layout.createSales(), EconomyUiTheme.MARKET_FORM_BUTTON,
        "screen.market.create_sales", List.of(), layout.createSales().contains(mouseX, mouseY),
        state.can(MarketAction.CREATE_SALES));
    renderer.translatedButton(layout.createDemand(), EconomyUiTheme.MARKET_FORM_BUTTON,
        "screen.market.create_demand", List.of(), layout.createDemand().contains(mouseX, mouseY),
        state.can(MarketAction.CREATE_DEMAND));

    renderFilterTabs(renderer, state, layout);
    renderSortTabs(renderer, state, layout);
    renderer.translatedTextInRect("screen.market.esc", List.of(), layout.esc(),
        EconomyUiTheme.Text.MUTED, UiTextAlignment.RIGHT);

    if (state.screenState() == ScreenState.LOADING) {
      renderer.translatedTextInRect("screen.market.loading", List.of(), layout.message(),
          EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);
    } else if (state.screenState() == ScreenState.ERROR) {
      renderer.translatedTextInRect(
          state.errorKey() == null ? "screen.market.sync_failed" : state.errorKey(),
          List.of(), layout.message(), EconomyUiTheme.Text.ERROR, UiTextAlignment.CENTER);
      renderer.translatedButton(layout.retryButton(), EconomyUiTheme.MARKET_FORM_BUTTON,
          "screen.market.retry", List.of(), layout.retryButton().contains(mouseX, mouseY),
          state.can(MarketAction.RETRY));
    } else if (state.screenState() == ScreenState.EMPTY) {
      renderer.translatedTextInRect("screen.market.empty", List.of(), layout.message(),
          EconomyUiTheme.Text.MUTED, UiTextAlignment.CENTER);
    }

    for (MarketLayout.Card card : layout.cards()) {
      var order = card.row().order();
      boolean hovered = card.card().contains(mouseX, mouseY);
      boolean selected = detailState != null
          && detailState.row().order().tradeId().equals(order.tradeId());
      int accent = order.type() == MarketOrderType.SALES
          ? EconomyUiTheme.MARKET_ACCENT : EconomyUiTheme.SHOP_ACCENT;

      UiSection.render(renderer, card.card(), 0, hovered);
      if (selected) UiSection.selectionGlow(renderer, card.card(), accent);

      renderer.item(order.item().itemId(), card.itemIcon());
      String name = card.row().displayName().isBlank()
          ? order.item().itemId() : card.row().displayName();
      renderer.textInRect(name, card.name(), EconomyUiTheme.Text.PRIMARY, UiTextAlignment.LEFT);
      renderer.translatedTextInRect(
          order.type() == MarketOrderType.SALES ? "screen.market.filter.sales" : "screen.market.filter.demand",
          List.of(), card.typeBadge(), accent, UiTextAlignment.RIGHT);
      renderer.translatedTextInRect("screen.market.card.unit_price",
          List.of(MarketInlineDetailView.unitPriceText(order.totalPrice(), order.quantity())),
          card.unitPrice(), EconomyUiTheme.BALANCE_ACCENT, UiTextAlignment.LEFT);
      renderer.translatedTextInRect("screen.market.card.summary",
          List.of(Integer.toString(order.quantity()), UiNumbers.formatInteger(order.totalPrice())),
          card.summary(), EconomyUiTheme.Text.MUTED, UiTextAlignment.LEFT);
    }

    if (state.totalPages() > 1) {
      boolean previousEnabled = state.page() > 0;
      renderer.button(layout.previousButton(), EconomyUiTheme.MARKET_FORM_BUTTON, "<",
          layout.previousButton().contains(mouseX, mouseY), previousEnabled);
      renderer.textInRect((state.page() + 1) + " / " + state.totalPages(), layout.pageText(),
          EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);
      boolean nextEnabled = state.page() + 1 < state.totalPages();
      renderer.button(layout.nextButton(), EconomyUiTheme.MARKET_FORM_BUTTON, ">",
          layout.nextButton().contains(mouseX, mouseY), nextEnabled);
    }

    MarketInlineDetailView.render(renderer, detailState, layout, mouseX, mouseY);
    tooltipAt(state, layout, mouseX, mouseY)
        .ifPresent(tooltip -> renderer.tooltip(tooltip, mouseX, mouseY));
  }

  private static void renderFilterTabs(
      EconomyUiRenderer renderer, MarketState state, MarketLayout.Layout layout) {
    for (MarketLayout.FilterTab tab : layout.filterTabs()) {
      boolean selected = tab.filter() == state.filter();
      renderer.translatedTextInRect(MarketLayout.filterKey(tab.filter()), List.of(), tab.textRect(),
          selected ? EconomyUiTheme.Text.PRIMARY : EconomyUiTheme.Text.MUTED,
          UiTextAlignment.LEFT);
      if (selected) {
        renderer.fill(new UiRect(tab.textRect().x(), tab.textRect().bottom() - 1,
            tab.textRect().width(), 1), EconomyUiTheme.MARKET_ACCENT);
      }
    }
  }

  private static void renderSortTabs(
      EconomyUiRenderer renderer, MarketState state, MarketLayout.Layout layout) {
    for (MarketLayout.SortTab tab : layout.sortTabs()) {
      boolean selected = tab.sort() == state.sort();
      renderer.translatedTextInRect(MarketLayout.sortKey(tab.sort()), List.of(), tab.textRect(),
          selected ? EconomyUiTheme.Text.PRIMARY : EconomyUiTheme.Text.MUTED,
          UiTextAlignment.LEFT);
      if (selected) {
        renderer.fill(new UiRect(tab.textRect().x(), tab.textRect().bottom() - 1,
            tab.textRect().width(), 1), EconomyUiTheme.MARKET_ACCENT);
      }
    }
  }

  public static MarketAction actionFor(
      com.mo.economy_system.common.network.MarketOrderSnapshot order, UUID viewerId) {
    boolean own = viewerId != null && viewerId.equals(order.ownerId());
    if (order.type() == MarketOrderType.SALES) return own ? MarketAction.REMOVE_SALES : MarketAction.BUY;
    if (own) return order.delivered() ? MarketAction.CONFIRM_DEMAND : MarketAction.REMOVE_DEMAND;
    return order.delivered() ? null : MarketAction.DELIVER_DEMAND;
  }

  public static Optional<TooltipModel> tooltipAt(
      MarketState state, MarketLayout.Layout layout, int mouseX, int mouseY) {
    for (MarketLayout.Card card : layout.cards()) {
      if (!card.card().contains(mouseX, mouseY)) continue;
      var order = card.row().order();
      boolean icon = card.itemIcon().contains(mouseX, mouseY);
      List<TooltipLine> lines = new ArrayList<>();
      if (icon) lines.add(new TooltipLine.NativeItem(order.item().itemId()));
      lines.add(new TooltipLine.ColoredLiteral("物品ID: " + order.item().itemId(), 0xFFAAAAAA));
      lines.add(new TooltipLine.ColoredLiteral("交易ID: " + order.tradeId(), 0xFF777777));
      lines.add(new TooltipLine.ColoredLiteral("发布者: " + order.ownerName(), 0xFFAAAAAA));
      lines.add(new TooltipLine.ColoredLiteral("过期: "
          + EXPIRATION_FORMATTER.format(Instant.ofEpochMilli(order.expirationTime())), 0xFFFFAA00));
      long remainingMillis = Math.max(0L, order.expirationTime() - System.currentTimeMillis());
      lines.add(new TooltipLine.ColoredLiteral("剩余: " + formatDuration(remainingMillis), 0xFFFFFF55));
      return Optional.of(new TooltipModel(lines));
    }
    return Optional.empty();
  }

  static String formatDuration(long millis) {
    long totalSeconds = millis / 1000L;
    long days = totalSeconds / 86400L;
    long hours = (totalSeconds % 86400L) / 3600L;
    long minutes = (totalSeconds % 3600L) / 60L;
    if (days > 0) return days + "天 " + hours + "小时";
    if (hours > 0) return hours + "小时 " + minutes + "分钟";
    return minutes + "分钟";
  }
}
