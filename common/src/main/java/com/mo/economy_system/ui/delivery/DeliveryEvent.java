package com.mo.economy_system.ui.delivery;

import com.mo.economy_system.common.mail.MailSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public sealed interface DeliveryEvent
    permits DeliveryEvent.Initialize, DeliveryEvent.Retry, DeliveryEvent.DataLoaded,
        DeliveryEvent.DataFailed, DeliveryEvent.FilterChanged, DeliveryEvent.CategoryChanged,
        DeliveryEvent.MailSelected, DeliveryEvent.ViewportChanged,
        DeliveryEvent.NextPage, DeliveryEvent.PreviousPage, DeliveryEvent.Scroll,
        DeliveryEvent.ActionClicked, DeliveryEvent.Tick {
  record Initialize(long nowNanos) implements DeliveryEvent {}
  record Retry(long nowNanos) implements DeliveryEvent {}
  record DataLoaded(long requestId, List<MailSnapshot> mails) implements DeliveryEvent {
    public DataLoaded {
      mails = List.copyOf(Objects.requireNonNull(mails, "mails"));
    }
  }
  record DataFailed(long requestId, String errorKey) implements DeliveryEvent {
    public DataFailed {
      Objects.requireNonNull(errorKey, "errorKey");
    }
  }
  record FilterChanged(String value) implements DeliveryEvent {}
  record CategoryChanged(DeliveryCategory category) implements DeliveryEvent {
    public CategoryChanged {
      Objects.requireNonNull(category, "category");
    }
  }
  record MailSelected(UUID entryId) implements DeliveryEvent {
    public MailSelected {
      Objects.requireNonNull(entryId, "entryId");
    }
  }
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
