package com.mo.economy_system.ui.market;

import java.util.List;

public sealed interface MarketCreateEvent permits
    MarketCreateEvent.InventoryChanged,
    MarketCreateEvent.SlotSelected,
    MarketCreateEvent.ItemIdChanged,
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

  record QuantityChanged(int quantity) implements MarketCreateEvent {}

  record PriceChanged(int totalPrice) implements MarketCreateEvent {}

  record ActionClicked(MarketCreateAction action) implements MarketCreateEvent {}
}
