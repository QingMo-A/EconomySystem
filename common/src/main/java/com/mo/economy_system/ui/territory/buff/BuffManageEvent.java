package com.mo.economy_system.ui.territory.buff;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import java.util.List;

public sealed interface BuffManageEvent permits BuffManageEvent.Initialize, BuffManageEvent.DataLoaded,
        BuffManageEvent.DataFailed, BuffManageEvent.Retry, BuffManageEvent.FilterChanged,
        BuffManageEvent.ViewportChanged, BuffManageEvent.NextPage, BuffManageEvent.PreviousPage,
        BuffManageEvent.Scroll, BuffManageEvent.ActionClicked, BuffManageEvent.Tick {
    record Initialize(long nowNanos) implements BuffManageEvent {}
    record DataLoaded(long requestId, List<Buff> buffs) implements BuffManageEvent {}
    record DataFailed(long requestId, String errorKey) implements BuffManageEvent {}
    record Retry(long nowNanos) implements BuffManageEvent {}
    record FilterChanged(String value) implements BuffManageEvent {}
    record ViewportChanged(int pageSize) implements BuffManageEvent {
        public ViewportChanged { if (pageSize < 1) throw new IllegalArgumentException("pageSize"); }
    }
    record NextPage() implements BuffManageEvent {}
    record PreviousPage() implements BuffManageEvent {}
    record Scroll(int steps) implements BuffManageEvent {}
    record ActionClicked(BuffAction action, String buffId, long nowNanos) implements BuffManageEvent {}
    record Tick(long nowNanos) implements BuffManageEvent {}
}
