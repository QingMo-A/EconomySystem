package com.mo.economy_system.ui.territory;

import java.util.List;
import java.util.UUID;

public sealed interface TerritoryManageEvent permits
        TerritoryManageEvent.Initialize,
        TerritoryManageEvent.DataLoaded,
        TerritoryManageEvent.DataFailed,
        TerritoryManageEvent.Retry,
        TerritoryManageEvent.FilterChanged,
        TerritoryManageEvent.ViewportChanged,
        TerritoryManageEvent.Scroll,
        TerritoryManageEvent.NextPage,
        TerritoryManageEvent.PreviousPage,
        TerritoryManageEvent.ActionClicked,
        TerritoryManageEvent.Tick {
    record Initialize(long nowNanos) implements TerritoryManageEvent {}
    record DataLoaded(long requestId, List<MemberRow> members) implements TerritoryManageEvent {}
    record DataFailed(long requestId, String errorKey) implements TerritoryManageEvent {}
    record Retry(long nowNanos) implements TerritoryManageEvent {}
    record FilterChanged(String value) implements TerritoryManageEvent {}
    record ViewportChanged(int pageSize) implements TerritoryManageEvent {
        public ViewportChanged {
            if (pageSize < 1) throw new IllegalArgumentException("pageSize must be positive");
        }
    }
    record Scroll(int steps) implements TerritoryManageEvent {}
    record NextPage() implements TerritoryManageEvent {}
    record PreviousPage() implements TerritoryManageEvent {}
    record ActionClicked(TerritoryManageAction action, UUID targetPlayerId) implements TerritoryManageEvent {}
    record Tick(long nowNanos) implements TerritoryManageEvent {}
}
