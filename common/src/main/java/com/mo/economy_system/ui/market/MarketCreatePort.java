package com.mo.economy_system.ui.market;

import java.util.List;

/** Target adapter for item lookup and market order submission. */
public interface MarketCreatePort {
  boolean isKnownItem(String itemId);

  /**
   * Returns a bounded, deterministic list of registry ids matching the current input.  The
   * common form owns the dropdown geometry and rendering; targets only adapt their native item
   * registry into this loader-neutral list.
   */
  List<String> itemIdSuggestions(String prefix);

  int maxStackSize(String itemId);

  void submitSales(int slot, int quantity, int totalPrice);

  void submitDemand(String itemId, int quantity, int totalPrice);
}
