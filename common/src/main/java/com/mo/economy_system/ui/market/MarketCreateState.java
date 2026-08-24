package com.mo.economy_system.ui.market;

import com.mo.economy_system.ui.core.ScreenState;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable common state for market create-sales/create-demand forms. */
public record MarketCreateState(
    MarketCreateMode mode,
    List<MarketInventoryItem> inventory,
    int selectedSlot,
    String itemId,
    int quantity,
    int totalPrice,
    ScreenState screenState,
    String errorKey,
    Set<MarketCreateAction> actions) {
  public MarketCreateState {
    mode = Objects.requireNonNull(mode, "mode");
    inventory = List.copyOf(Objects.requireNonNull(inventory, "inventory"));
    if (selectedSlot < -1 || quantity < 0 || totalPrice < 0) {
      throw new IllegalArgumentException("invalid market form state");
    }
    itemId = Objects.requireNonNullElse(itemId, "");
    screenState = Objects.requireNonNull(screenState, "screenState");
    actions = Set.copyOf(Objects.requireNonNull(actions, "actions"));
  }

  public MarketInventoryItem selectedItem() {
    return inventory.stream().filter(item -> item.slot() == selectedSlot).findFirst().orElse(null);
  }

  public int availableQuantity() {
    MarketInventoryItem selected = selectedItem();
    if (selected == null) return 0;
    return inventory.stream().filter(item -> item.itemId().equals(selected.itemId()))
        .mapToInt(MarketInventoryItem::count).sum();
  }

  /**
   * Market v2 reinterprets the existing price-input field as unit price while keeping the record
   * component name for source compatibility with the current target screens/protocol adapters.
   */
  public int unitPrice() {
    return totalPrice;
  }

  public long computedOrderTotalPrice() {
    return (long) quantity * unitPrice();
  }

  public boolean computedOrderTotalFitsInt() {
    long total = computedOrderTotalPrice();
    return total > 0 && total <= Integer.MAX_VALUE;
  }

  public boolean can(MarketCreateAction action) {
    return actions.contains(action);
  }
}
