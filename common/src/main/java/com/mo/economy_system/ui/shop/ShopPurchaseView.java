package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.core.ScreenState;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Target-neutral rendering contract for the shop purchase dialog. */
public final class ShopPurchaseView {
  private ShopPurchaseView() {}
  public static void render(EconomyUiRenderer renderer, ShopPurchaseState state, ShopPurchaseLayout.Layout layout, int mouseX, int mouseY) {
    renderer.card(layout.card(), EconomyUiTheme.SHOP_PURCHASE_PANEL, false);
    renderer.translatedTextInRect("screen.shop.purchase.title", List.of(), new UiRect(layout.card().x()+12, layout.card().y()+10, layout.card().width()-24, 16), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.item(state.row().item().itemId(), layout.item());
    renderer.itemDisplayName(state.row().item().itemId(), new UiRect(layout.card().x()+12, layout.card().y()+16,
        layout.card().width()-24, 14), EconomyUiTheme.TEXT_PRIMARY, UiTextAlignment.CENTER);
    renderer.translatedTextInRect("screen.shop.purchase.unit_price", List.of(Integer.toString(state.row().item().currentPrice())),
        new UiRect(layout.card().x()+12, layout.card().y()+56, layout.card().width()-24, 16), EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.CENTER);
    renderer.translatedTextInRect("screen.shop.purchase.total", List.of(Long.toString(state.totalPrice())),
        new UiRect(layout.card().x()+12, layout.card().y()+69, layout.card().width()-24, 16), EconomyUiTheme.SHOP_ACCENT, UiTextAlignment.CENTER);
    renderer.translatedTextInRect("screen.shop.purchase.quantity", List.of(), layout.quantityLabel(),
        EconomyUiTheme.TEXT_SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedButton(layout.confirm(), EconomyUiTheme.SHOP_BUTTON, "screen.shop.purchase.confirm", List.of(), layout.confirm().contains(mouseX, mouseY), state.can(ShopPurchaseAction.CONFIRM));
    if (state.screenState() == ScreenState.ERROR && state.errorKey() != null) renderer.translatedTextInRect(state.errorKey(), List.of(), layout.message(), EconomyUiTheme.TEXT_ERROR, UiTextAlignment.CENTER);
  }
}
