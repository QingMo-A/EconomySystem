package com.mo.economy_system.ui.shop;

import com.mo.economy_system.ui.component.UiPanel;
import com.mo.economy_system.ui.geometry.UiRect;
import com.mo.economy_system.ui.renderer.EconomyUiRenderer;
import com.mo.economy_system.ui.renderer.UiNativeInputFrame;
import com.mo.economy_system.ui.renderer.UiTextAlignment;
import com.mo.economy_system.ui.text.UiNumbers;
import com.mo.economy_system.ui.theme.EconomyUiTheme;
import java.util.List;

/** Inline purchase details for the rightmost quarter of the shop page. */
public final class ShopInlinePurchaseView {
  private ShopInlinePurchaseView() {}

  public static void render(
      EconomyUiRenderer renderer,
      ShopPurchaseState state,
      ShopLayout.Layout layout,
      int mouseX,
      int mouseY) {
    UiPanel.render(renderer, layout.purchasePanel(), false);
    if (state == null) return;

    renderer.translatedTextInRect("screen.shop.purchase.title", List.of(), layout.purchaseTitle(),
        EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);
    renderer.item(state.row().item().itemId(), layout.purchaseItem());
    renderer.itemDisplayName(state.row().item().itemId(), layout.purchaseName(),
        EconomyUiTheme.Text.PRIMARY, UiTextAlignment.CENTER);

    renderer.translatedTextInRect("screen.shop.purchase.unit_price",
        List.of(UiNumbers.formatInteger(state.row().item().currentPrice())),
        layout.purchaseUnitPrice(), EconomyUiTheme.Text.SECONDARY, UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.shop.purchase.balance",
        List.of(UiNumbers.formatInteger(state.balance())),
        layout.purchaseBalance(), state.affordable() ? EconomyUiTheme.Text.SECONDARY : EconomyUiTheme.Text.ERROR,
        UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.shop.purchase.capacity",
        List.of(Integer.toString(state.availableQuantity())),
        layout.purchaseCapacity(), state.availableQuantity() >= state.quantity()
            ? EconomyUiTheme.Text.MUTED : EconomyUiTheme.Text.ERROR,
        UiTextAlignment.LEFT);
    renderer.translatedTextInRect("screen.shop.purchase.total",
        List.of(state.totalPrice() == Long.MAX_VALUE ? "-" : UiNumbers.formatInteger(state.totalPrice())),
        layout.purchaseTotal(), state.affordable() ? EconomyUiTheme.SHOP_ACCENT : EconomyUiTheme.Text.ERROR,
        UiTextAlignment.LEFT);

    if (state.errorKey() != null) {
      renderer.translatedTextInRect(state.errorKey(), List.of(), layout.purchaseMessage(),
          EconomyUiTheme.Text.ERROR, UiTextAlignment.CENTER);
    }

    renderer.translatedButton(layout.purchaseConfirm(), EconomyUiTheme.SHOP_FORM_BUTTON,
        "screen.shop.purchase.confirm", List.of(), layout.purchaseConfirm().contains(mouseX, mouseY),
        state.can(ShopPurchaseAction.CONFIRM));
  }

  public static void renderQuantityFrame(
      EconomyUiRenderer renderer,
      UiRect nativeWidgetRect,
      boolean focused,
      boolean hovered,
      boolean error) {
    UiNativeInputFrame.render(renderer, nativeWidgetRect,
        error ? EconomyUiTheme.INPUT_ERROR_FRAME : EconomyUiTheme.SHOP_SEARCH_FRAME,
        focused, hovered);
  }
}
