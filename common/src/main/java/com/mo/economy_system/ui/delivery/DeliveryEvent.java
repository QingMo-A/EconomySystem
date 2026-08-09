package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public sealed interface DeliveryEvent
    permits DeliveryEvent.Initialize, DeliveryEvent.Retry, DeliveryEvent.DataLoaded,
        DeliveryEvent.DataFailed, DeliveryEvent.FilterChanged, DeliveryEvent.ViewportChanged,
        DeliveryEvent.NextPage, DeliveryEvent.PreviousPage, DeliveryEvent.Scroll,
        DeliveryEvent.ActionClicked, DeliveryEvent.Tick {
  record Initialize(long nowNanos) implements DeliveryEvent {}
  record Retry(long nowNanos) implements DeliveryEvent {}
  record DataLoaded(long requestId, List<DeliveryBoxEntrySnapshot> entries) implements DeliveryEvent {
    public DataLoaded {
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }
  }
  record DataFailed(long requestId, String errorKey) implements DeliveryEvent {
    public DataFailed {
      Objects.requireNonNull(errorKey, "errorKey");
    }
  }
  record FilterChanged(String value) implements DeliveryEvent {}
  record ViewportChanged(int pageSize) implements DeliveryEvent {}
  record NextPage() implements DeliveryEvent {}
  record PreviousPage() implements DeliveryEvent {}
  record Scroll(int steps) implements DeliveryEvent {}
  record ActionClicked(DeliveryAction action, UUID entryId, long nowNanos) implements DeliveryEvent {
    public ActionClicked {
      Objects.requireNonNull(action, "action");
    }
  }
  record Tick(long nowNanos) implements DeliveryEvent {}
}
