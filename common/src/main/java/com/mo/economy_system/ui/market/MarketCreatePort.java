package com.mo.economy_system.ui.market;

/** Target adapter for item lookup and market order submission. */
public interface MarketCreatePort {
  boolean isKnownItem(String itemId);

  int maxStackSize(String itemId);

  void submitSales(int slot, int quantity, int totalPrice);

  void submitDemand(String itemId, int quantity, int totalPrice);
}
