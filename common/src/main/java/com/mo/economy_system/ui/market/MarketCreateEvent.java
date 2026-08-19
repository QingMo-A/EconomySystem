package com.mo.economy_system.ui.market;

import java.util.List;

public sealed interface MarketCreateEvent permits
    MarketCreateEvent.InventoryChanged,
    MarketCreateEvent.SlotSelected,
    MarketCreateEvent.ItemIdChanged,
    MarketCreateEvent.CompletionMoved,
    MarketCreateEvent.CompletionAccepted,
    MarketCreateEvent.CompletionDismissed,
    MarketCreateEvent.QuantityChanged,
    MarketCreateEvent.PriceChanged,
    MarketCreateEvent.ActionClicked {
  record InventoryChanged(List<MarketInventoryItem> inventory) implements MarketCreateEvent {
    public InventoryChanged {
      inventory = List.copyOf(inventory);
    }
  }

  record SlotSelected(int slot) implements MarketCreateEvent {}

  record ItemIdChanged(String itemId) implements MarketCreateEvent {}

  /** Moves the highlighted registry-id suggestion by a signed row delta. */
  record CompletionMoved(int delta) implements MarketCreateEvent {}

  /** Accepts the highlighted row without submitting the order. */
  record CompletionAccepted(int index) implements MarketCreateEvent {}

  /** Hides the dropdown while retaining the current text. */
  record CompletionDismissed() implements MarketCreateEvent {}

  record QuantityChanged(int quantity) implements MarketCreateEvent {}

  record PriceChanged(int totalPrice) implements MarketCreateEvent {}

  record ActionClicked(MarketCreateAction action) implements MarketCreateEvent {}
}
