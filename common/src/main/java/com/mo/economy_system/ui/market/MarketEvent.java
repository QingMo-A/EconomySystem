package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.network.MarketOrderFilter;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import java.util.List;

public sealed interface MarketEvent permits
    MarketEvent.Initialize,
    MarketEvent.DataLoaded,
    MarketEvent.DataFailed,
    MarketEvent.Retry,
    MarketEvent.FilterChanged,
    MarketEvent.QueryChanged,
    MarketEvent.ViewportChanged,
    MarketEvent.NextPage,
    MarketEvent.PreviousPage,
    MarketEvent.Scroll,
    MarketEvent.ActionClicked,
    MarketEvent.Tick {
  record Initialize(long nowNanos) implements MarketEvent {}
  record DataLoaded(long requestId, long revision, int offset, int totalMatched,
                    int totalSales, int totalDemand, List<MarketOrderSnapshot> orders)
      implements MarketEvent {
    public DataLoaded {
      orders = List.copyOf(orders);
    }
  }
  record DataFailed(long requestId, String errorKey) implements MarketEvent {}
  record Retry(long nowNanos) implements MarketEvent {}
  record FilterChanged(MarketOrderFilter filter) implements MarketEvent {}
  record QueryChanged(String query) implements MarketEvent {}
  record ViewportChanged(int pageSize) implements MarketEvent {
    public ViewportChanged {
      if (pageSize < 1) throw new IllegalArgumentException("page size must be positive");
    }
  }
  record NextPage() implements MarketEvent {}
  record PreviousPage() implements MarketEvent {}
  record Scroll(int steps) implements MarketEvent {}
  record ActionClicked(MarketAction action, java.util.UUID tradeId) implements MarketEvent {}
  record Tick(long nowNanos) implements MarketEvent {}
}
