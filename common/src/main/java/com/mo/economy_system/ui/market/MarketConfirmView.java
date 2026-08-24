package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiIcon;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Semantic market confirmation renderer shared by Forge and NeoForge. */
public final class MarketConfirmView {
  private MarketConfirmView() {}

  public static void render(EconomyUiRenderer renderer, MarketConfirmState state,
                            MarketConfirmLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.card(), EconomyUiTheme.MARKET_CARD, false);
    renderer.scaledIconText(UiIcon.MARKET, "Economy", layout.card().x() + 12, layout.card().y() + 10,
        1.0f, EconomyUiRenderer.ICON_SIZE, EconomyUiRenderer.ICON_ADVANCE, EconomyUiTheme.TEXT_PRIMARY);
    renderer.translatedTextInRect(titleKey(state.action()), List.of(),
        new UiRect(layout.card().x() + 74, layout.card().y() + 12, layout.card().width() - 150, 18), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.item(state.row().order().item().itemId(), layout.item());
    renderer.itemDisplayName(state.row().order().item().itemId(),
        new UiRect(layout.details().x(), layout.details().y(), layout.details().width(), 16),
        EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.textInRect("￥" + UiNumbers.formatInteger(state.row().order().totalPrice()),
        new UiRect(layout.details().x(), layout.details().y() + 16, layout.details().width(), 16),
        EconomyUiTheme.MARKET_ACCENT, UiTextAlignment.CENTER);
    String ownerKey = state.row().order().type() == MarketOrderType.SALES ? "screen.market.seller" : "screen.market.requester";
    renderer.translatedTextWithSuffix(ownerKey, List.of(), ": " + state.row().order().ownerName(),
        new UiRect(layout.details().x(), layout.details().y() + 32, layout.details().width(), 16),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.CENTER);
    renderer.translatedTextInRect(state.row().order().type() == MarketOrderType.SALES ? "screen.market.confirm.sales_warning" : "screen.market.confirm.demand_warning", List.of(), new UiRect(layout.card().x() + 18, layout.card().y() + 128, layout.card().width() - 36, 20), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
    renderer.translatedButton(layout.confirm(), EconomyUiTheme.MARKET_BUTTON, "screen.market.confirm.confirm", List.of(), layout.confirm().contains(mouseX, mouseY), state.can(MarketConfirmAction.CONFIRM));
    renderer.translatedButton(layout.cancel(), EconomyUiTheme.DISABLED_BUTTON, "screen.market.confirm.cancel", List.of(), layout.cancel().contains(mouseX, mouseY), state.can(MarketConfirmAction.CANCEL));
  }

  private static String titleKey(MarketAction action) {
    return switch (action) {
      case BUY -> "screen.market.confirm.buy_title";
      case REMOVE_SALES, ADMIN_REMOVE_SALES -> "screen.market.confirm.remove_sales_title";
      case REMOVE_DEMAND -> "screen.market.confirm.remove_demand_title";
      case DELIVER_DEMAND -> "screen.market.confirm.deliver_title";
      case CONFIRM_DEMAND -> "screen.market.confirm.confirm_title";
      default -> "screen.market.confirm.title";
    };
  }
}
