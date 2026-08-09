package com.mo.economy_system.ui.shop;

/** Target adapter for inventory capacity facts and the final purchase intent. */
public interface ShopPurchasePort {
  int availableQuantity(ShopRow row);
  void submit(ShopRow row, int quantity);
}
