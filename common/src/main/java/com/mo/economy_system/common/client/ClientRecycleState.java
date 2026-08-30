package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.RecycleDataResponseMessage;
import java.util.List;

/** Target-neutral latest recycle data; target packet handlers only publish snapshots here. */
public final class ClientRecycleState {
  private static long nextRequestId = 1L;
  private static Snapshot snapshot = new Snapshot(-1L, true, 0L, 0L, List.of(), "");
  private ClientRecycleState() {}
  public static synchronized long nextRequestId() {
    if (nextRequestId == Long.MAX_VALUE) throw new IllegalStateException("recycle request id exhausted");
    return nextRequestId++;
  }
  public static synchronized void begin(long requestId) { snapshot = new Snapshot(requestId, true, 0L, 0L, List.of(), ""); }
  public static synchronized void apply(RecycleDataResponseMessage response) {
    if (response.requestId() < snapshot.requestId()) return;
    snapshot = new Snapshot(response.requestId(), false, response.serverNowMillis(), response.cycleEndsAt(), response.offers(), response.errorKey());
  }
  public static synchronized Snapshot snapshot() { return snapshot; }
  public record Snapshot(long requestId, boolean loading, long serverNowMillis, long cycleEndsAt,
                         List<com.mo.economy_system.common.network.RecycleOfferSnapshot> offers,
                         String errorKey) {
    public Snapshot { offers = List.copyOf(offers); errorKey = errorKey == null ? "" : errorKey; }
  }
}
