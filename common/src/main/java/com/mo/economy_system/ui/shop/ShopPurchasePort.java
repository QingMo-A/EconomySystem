package com.mo.economy_system.ui.shop;

/** Target adapter for inventory capacity facts and the final purchase intent. */
public interface ShopPurchasePort {
  /**
   * Returns the exact main-inventory capacity for the selected item.  A target must count only
   * free space in the ordinary inventory list: remaining room in matching stacks plus empty
   * slots multiplied by that item's real max stack size.  Armor/offhand slots are not ordinary
   * delivery capacity and must not be included.
   */
  int availableQuantity(ShopRow row);
  void submit(ShopRow row, int quantity);
}
