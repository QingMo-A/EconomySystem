package com.mo.economy_system.ui.territory.list;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.List;

public sealed interface TerritoryListEvent permits
    TerritoryListEvent.Initialize,
    TerritoryListEvent.DataLoaded,
    TerritoryListEvent.DataFailed,
    TerritoryListEvent.Retry,
    TerritoryListEvent.FilterChanged,
    TerritoryListEvent.ViewportChanged,
    TerritoryListEvent.NextPage,
    TerritoryListEvent.PreviousPage,
    TerritoryListEvent.Scroll,
    TerritoryListEvent.ActionClicked,
    TerritoryListEvent.Tick {
  record Initialize(long nowNanos) implements TerritoryListEvent {}

  record DataLoaded(long requestId, List<Owned> owned, List<Summary> authorized)
      implements TerritoryListEvent {
    public DataLoaded {
      owned = List.copyOf(owned);
      authorized = List.copyOf(authorized);
    }
  }

  record DataFailed(long requestId, String errorKey) implements TerritoryListEvent {}

  record Retry(long nowNanos) implements TerritoryListEvent {}

  record FilterChanged(String value) implements TerritoryListEvent {}

  record ViewportChanged(int pageSize) implements TerritoryListEvent {
    public ViewportChanged {
      if (pageSize < 1) throw new IllegalArgumentException("page size must be positive");
    }
  }

  record NextPage() implements TerritoryListEvent {}

  record PreviousPage() implements TerritoryListEvent {}

  record Scroll(int steps) implements TerritoryListEvent {}

  record ActionClicked(TerritoryListAction action, java.util.UUID territoryId)
      implements TerritoryListEvent {}

  record Tick(long nowNanos) implements TerritoryListEvent {}
}
