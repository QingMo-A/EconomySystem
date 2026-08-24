package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiNativeInputFrame;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;
import java.util.Locale;

/** Rendering for the selected order in the right-side Market v2 detail pane. */
public final class MarketInlineDetailView {
  private MarketInlineDetailView() {}

  public static void render(
      EconomyUiRenderer renderer,
      MarketDetailState state,
      MarketLayout.Layout layout,
      int mouseX,
      int mouseY) {
    if (state == null) return;
    var order = state.row().order();
    int accent = order.type() == MarketOrderType.SALES
        ? EconomyUiTheme.MARKET_ACCENT : EconomyUiTheme.SHOP_ACCENT;

    renderer.translatedTextInRect("screen.market.detail.title", List.of(), layout.detailTitle(),
        EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);
    renderer.item(order.item().itemId(), layout.detailItem());
    String name = state.row().displayName().isBlank() ? order.item().itemId() : state.row().displayName();
    renderer.textInRect(name, layout.detailName(), EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);
    renderer.translatedTextInRect(
        order.type() == MarketOrderType.SALES ? "screen.market.sales" : "screen.market.demand",
        List.of(), layout.detailType(), accent, UiTextAlignment.CENTER);

    renderer.translatedTextInRect("screen.market.detail.remaining",
        List.of(Integer.toString(order.quantity())), layout.detailRemaining(),
        EconomyUiTheme.Text.SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.market.detail.unit_price",
        List.of(unitPriceText(order.totalPrice(), order.quantity())), layout.detailUnitPrice(),
        accent, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.market.detail.order_total",
        List.of(UiNumbers.formatInteger(order.totalPrice())), layout.detailOrderTotal(),
        EconomyUiTheme.Text.SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect(
        order.type() == MarketOrderType.SALES ? "screen.market.detail.seller" : "screen.market.detail.requester",
        List.of(order.ownerName()), layout.detailOwner(), EconomyUiTheme.Text.MUTED, UiTextAlignment.LEFT);

    if (state.primaryAction() == MarketAction.BUY) {
      renderer.translatedTextInRect("screen.market.detail.buy_facts",
          List.of(UiNumbers.formatInteger(state.balance()), Integer.toString(state.inventoryCapacity())),
          layout.detailFacts(), EconomyUiTheme.Text.MUTED, UiTextAlignment.LEFT);
    } else if (state.primaryAction() == MarketAction.DELIVER_DEMAND) {
      renderer.translatedTextInRect("screen.market.detail.deliver_facts",
          List.of(Integer.toString(state.matchingItems()), UiNumbers.formatInteger(state.receivableHeadroom())),
          layout.detailFacts(), EconomyUiTheme.Text.MUTED, UiTextAlignment.LEFT);
    } else {
      renderer.translatedTextInRect("screen.market.detail.trade_id",
          List.of(order.tradeId().toString()), layout.detailFacts(), EconomyUiTheme.Text.MUTED,
          UiTextAlignment.LEFT);
    }

    if (state.quantityMode()) {
      if (state.partialSupported()) {
        renderer.button(layout.decrement(), EconomyUiTheme.MARKET_FORM_BUTTON, "-1",
            layout.decrement().contains(mouseX, mouseY), state.can(MarketDetailAction.DECREMENT));
        renderer.button(layout.increment(), EconomyUiTheme.MARKET_FORM_BUTTON, "+1",
            layout.increment().contains(mouseX, mouseY), state.can(MarketDetailAction.INCREMENT));
        renderer.translatedButton(layout.all(), EconomyUiTheme.MARKET_FORM_BUTTON,
            "screen.market.detail.all", List.of(), layout.all().contains(mouseX, mouseY),
            state.can(MarketDetailAction.SELECT_ALL));
      } else {
        renderer.translatedTextInRect("screen.market.detail.whole_order_only", List.of(),
            new UiRect(layout.decrement().x(), layout.decrement().y(),
                layout.all().right() - layout.decrement().x(), layout.decrement().height()),
            EconomyUiTheme.Text.MUTED, UiTextAlignment.CENTER);
      }
      String amountKey = state.primaryAction() == MarketAction.BUY
          ? "screen.market.detail.trade_total" : "screen.market.detail.expected_income";
      renderer.translatedTextInRect(amountKey,
          List.of(UiNumbers.formatInteger(state.amount())), layout.detailAmount(),
          state.errorKey() == null ? accent : EconomyUiTheme.Text.ERROR, UiTextAlignment.LEFT);
    }

    if (state.errorKey() != null) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.detailError(),
          EconomyUiTheme.Text.ERROR, UiTextAlignment.CENTER);
    }

    if (state.primaryAction() != null) {
      renderer.translatedButton(layout.primaryAction(), EconomyUiTheme.MARKET_FORM_BUTTON,
          actionKey(state.primaryAction()), List.of(), layout.primaryAction().contains(mouseX, mouseY),
          state.can(MarketDetailAction.SUBMIT_PRIMARY));
    }
    if (state.secondaryAction() != null) {
      renderer.translatedButton(layout.secondaryAction(), EconomyUiTheme.NEUTRAL_FORM_BUTTON,
          actionKey(state.secondaryAction()), List.of(), layout.secondaryAction().contains(mouseX, mouseY),
          state.can(MarketDetailAction.SUBMIT_SECONDARY));
    }
  }

  public static void renderQuantityFrame(
      EconomyUiRenderer renderer,
      UiRect nativeWidgetRect,
      boolean focused,
      boolean hovered,
      boolean error) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect,
        error ? EconomyUiTheme.INPUT_ERROR_FRAME : EconomyUiTheme.MARKET_SEARCH_FRAME,
        focused, hovered);
  }

  public static String actionKey(MarketAction action) {
    return switch (action) {
      case BUY -> "screen.market.buy";
      case REMOVE_SALES -> "screen.market.remove_sales";
      case ADMIN_REMOVE_SALES -> "screen.market.admin_remove";
      case DELIVER_DEMAND -> "screen.market.deliver";
      case CONFIRM_DEMAND -> "screen.market.confirm";
      case REMOVE_DEMAND -> "screen.market.remove_demand";
      default -> "screen.market.done";
    };
  }

  public static String unitPriceText(int totalPrice, int quantity) {
    if (quantity <= 0) return "-";
    if (totalPrice % quantity == 0) return UiNumbers.formatInteger(totalPrice / quantity);
    return String.format(Locale.ROOT, "%.2f", totalPrice / (double) quantity);
  }
}
