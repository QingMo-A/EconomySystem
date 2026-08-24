package com.mo.economy_system.ui.market;

/** Events consumed by the inline market detail state machine. */
public sealed interface MarketDetailEvent permits
    MarketDetailEvent.QuantityChanged,
    MarketDetailEvent.FactsChanged,
    MarketDetailEvent.RowRefreshed,
    MarketDetailEvent.OrderInvalidated,
    MarketDetailEvent.ActionClicked {

  record QuantityChanged(int quantity) implements MarketDetailEvent {}

  /** Client-side preflight facts only; the server remains authoritative. */
  record FactsChanged(int balance, int inventoryCapacity, int matchingItems, int receivableHeadroom)
      implements MarketDetailEvent {
    public FactsChanged {
      if (balance < 0 || inventoryCapacity < 0 || matchingItems < 0 || receivableHeadroom < 0) {
        throw new IllegalArgumentException("negative market detail facts");
      }
    }
  }

  record RowRefreshed(MarketRow row) implements MarketDetailEvent {
    public RowRefreshed {
      if (row == null) throw new IllegalArgumentException("row");
    }
  }

  /** The selected trade was not present in the authoritative focused refresh result. */
  record OrderInvalidated() implements MarketDetailEvent {}

  record ActionClicked(MarketDetailAction action) implements MarketDetailEvent {}
}
