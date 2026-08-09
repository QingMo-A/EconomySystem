package com.mo.economy_system.ui.balance;

import com.mo.economy_system.core.economy_system.BalanceLogEntry;
import java.util.List;
import java.util.Objects;

public sealed interface BalanceLogEvent
    permits BalanceLogEvent.Initialize, BalanceLogEvent.Retry, BalanceLogEvent.DataLoaded,
        BalanceLogEvent.DataFailed, BalanceLogEvent.CategoryChanged, BalanceLogEvent.ViewportChanged,
        BalanceLogEvent.NextPage, BalanceLogEvent.PreviousPage, BalanceLogEvent.Scroll,
        BalanceLogEvent.ActionClicked, BalanceLogEvent.Tick {
  record Initialize(long nowNanos) implements BalanceLogEvent {}
  record Retry(long nowNanos) implements BalanceLogEvent {}
  record DataLoaded(long requestId, String category, int offset, int limit, int total,
                    List<BalanceLogEntry> entries) implements BalanceLogEvent {
    public DataLoaded {
      Objects.requireNonNull(category, "category");
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
  }
  record DataFailed(long requestId, String errorKey) implements BalanceLogEvent {
    public DataFailed { Objects.requireNonNull(errorKey, "errorKey"); }
  }
  record CategoryChanged(String category) implements BalanceLogEvent {
    public CategoryChanged { Objects.requireNonNull(category, "category"); }
  }
  record ViewportChanged(int visibleRows) implements BalanceLogEvent {}
  record NextPage() implements BalanceLogEvent {}
  record PreviousPage() implements BalanceLogEvent {}
  record Scroll(int steps) implements BalanceLogEvent {}
  record ActionClicked(BalanceLogAction action, long nowNanos) implements BalanceLogEvent {
    public ActionClicked { Objects.requireNonNull(action, "action"); }
  }
  record Tick(long nowNanos) implements BalanceLogEvent {}
}
