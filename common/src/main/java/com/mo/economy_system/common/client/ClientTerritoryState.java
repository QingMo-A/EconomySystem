package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.TerritoryDataResponseKind;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic client cache for the territory-list response.
 *
 * <p>The cache intentionally contains only bounded common snapshots. Target
 * screens poll it and translate the snapshots into their native drawing API;
 * no screen instance is retained by a protocol handler.</p>
 */
public final class ClientTerritoryState {
  private static final AtomicReference<Snapshot> CURRENT = new AtomicReference<>(
      new Snapshot(-1, false, false, List.of(), List.of()));

  private ClientTerritoryState() {}

  public static Snapshot snapshot() {
    return CURRENT.get();
  }

  /** Marks a newly issued request as the only response that may be committed. */
  public static void begin(long requestId) {
    if (requestId < 0) throw new IllegalArgumentException("negative territory request id");
    CURRENT.updateAndGet(previous -> requestId < previous.requestId()
        ? previous
        : new Snapshot(requestId, true, false, previous.owned(), previous.authorized()));
  }

  /** Applies one response, rejecting responses older than the active request. */
  public static boolean apply(TerritoryDataResponseMessage response) {
    Objects.requireNonNull(response, "response");
    while (true) {
      Snapshot previous = CURRENT.get();
      if (response.requestId() < previous.requestId()) return false;
      Snapshot next = response.kind() == TerritoryDataResponseKind.ERROR
          ? new Snapshot(response.requestId(), false, true, previous.owned(), previous.authorized())
          : new Snapshot(response.requestId(), false, false, response.owned(), response.authorized());
      if (CURRENT.compareAndSet(previous, next)) return true;
    }
  }

  public static void reset() {
    CURRENT.set(new Snapshot(-1, false, false, List.of(), List.of()));
  }

  public record Snapshot(long requestId, boolean loading, boolean error,
                         List<Owned> owned, List<Summary> authorized) {
    public Snapshot {
      if (requestId < -1) throw new IllegalArgumentException("invalid territory request id");
      owned = List.copyOf(Objects.requireNonNull(owned, "owned"));
      authorized = List.copyOf(Objects.requireNonNull(authorized, "authorized"));
    }
  }
}
