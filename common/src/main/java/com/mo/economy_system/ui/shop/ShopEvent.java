package com.mo.economy_system.ui.shop;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import java.util.List;

public sealed interface ShopEvent permits
    ShopEvent.Initialize,
    ShopEvent.DataLoaded,
    ShopEvent.DataFailed,
    ShopEvent.Retry,
    ShopEvent.FilterChanged,
    ShopEvent.ViewportChanged,
    ShopEvent.NextPage,
    ShopEvent.PreviousPage,
    ShopEvent.Scroll,
    ShopEvent.ActionClicked,
    ShopEvent.Tick {
  record Initialize(long nowNanos) implements ShopEvent {}
  record DataLoaded(long requestId, List<ShopItemSnapshot> items) implements ShopEvent {
    public DataLoaded {
      items = List.copyOf(items);
    }
  }
  record DataFailed(long requestId, String errorKey) implements ShopEvent {}
  record Retry(long nowNanos) implements ShopEvent {}
  record FilterChanged(String value) implements ShopEvent {}
  record ViewportChanged(int pageSize) implements ShopEvent {
    public ViewportChanged {
      if (pageSize < 1) throw new IllegalArgumentException("page size must be positive");
    }
  }
  record NextPage() implements ShopEvent {}
  record PreviousPage() implements ShopEvent {}
  record Scroll(int steps) implements ShopEvent {}
  record ActionClicked(ShopAction action, String shopItemId) implements ShopEvent {}
  record Tick(long nowNanos) implements ShopEvent {}
}
