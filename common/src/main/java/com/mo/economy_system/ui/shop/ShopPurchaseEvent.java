package com.mo.economy_system.ui.shop;

public sealed interface ShopPurchaseEvent permits ShopPurchaseEvent.QuantityChanged, ShopPurchaseEvent.ActionClicked {
  record QuantityChanged(int quantity) implements ShopPurchaseEvent {}
  record ActionClicked(ShopPurchaseAction action) implements ShopPurchaseEvent {}
}
