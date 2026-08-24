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

  /** Sends the integer unit price. The server derives and validates the authoritative total. */
  void submitSales(int slot, int quantity, int unitPrice);

  /** Sends the integer unit price. The server derives and validates the authoritative total. */
  void submitDemand(String itemId, int quantity, int unitPrice);
}
