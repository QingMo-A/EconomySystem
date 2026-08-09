package com.mo.economy_system.common.client;

import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.network.DeliveryBoxDataResponseMessage;
import com.mo.economy_system.common.network.DeliveryBoxResponseKind;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Immutable client cache for the latest delivery-box response. */
public final class ClientDeliveryBoxState {
  private static final AtomicReference<Snapshot> CURRENT =
      new AtomicReference<>(new Snapshot(0, -1, DeliveryBoxResponseKind.ERROR, List.of()));

  private ClientDeliveryBoxState() {}

  public static Snapshot snapshot() {
    return CURRENT.get();
  }

  /** Publishes only responses for the newest request, rejecting stale packets. */
  public static boolean update(DeliveryBoxDataResponseMessage message) {
    Objects.requireNonNull(message, "message");
    while (true) {
      Snapshot previous = CURRENT.get();
      if (message.requestId() < previous.requestId()) return false;
      if (CURRENT.compareAndSet(previous,
          new Snapshot(nextRevision(previous.revision()), message.requestId(),
              message.kind(), message.entries()))) return true;
    }
  }

  private static long nextRevision(long revision) {
    if (revision == Long.MAX_VALUE) throw new IllegalStateException("delivery revision exhausted");
    return revision + 1;
  }

  public record Snapshot(long revision, long requestId, DeliveryBoxResponseKind kind,
                         List<DeliveryBoxEntrySnapshot> entries) {
    public Snapshot {
      if (revision < 0 || requestId < -1) throw new IllegalArgumentException("invalid delivery state");
      Objects.requireNonNull(kind, "kind");
      entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
    }

    public boolean failed() {
      return kind == DeliveryBoxResponseKind.ERROR;
    }
  }
}
