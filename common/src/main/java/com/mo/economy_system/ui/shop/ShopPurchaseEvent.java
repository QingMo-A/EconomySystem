package com.mo.economy_system.ui.shop;

public sealed interface ShopPurchaseEvent permits ShopPurchaseEvent.QuantityChanged, ShopPurchaseEvent.FactsChanged,
    ShopPurchaseEvent.ItemRefreshed, ShopPurchaseEvent.ActionClicked {
  record QuantityChanged(int quantity) implements ShopPurchaseEvent {}
  record FactsChanged(int availableQuantity, int balance) implements ShopPurchaseEvent {}
  record ItemRefreshed(ShopRow row) implements ShopPurchaseEvent {
    public ItemRefreshed {
      if (row == null) throw new IllegalArgumentException("row");
    }
  }
  record ActionClicked(ShopPurchaseAction action) implements ShopPurchaseEvent {}
}
