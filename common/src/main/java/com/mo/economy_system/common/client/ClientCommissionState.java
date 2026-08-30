package com.mo.economy_system.common.client;

import com.mo.economy_system.common.commission.CommissionInstance;
import com.mo.economy_system.common.network.CommissionDataResponseMessage;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Client cache for the server-authoritative personal commission center. */
public final class ClientCommissionState {
  private static final AtomicLong IDS = new AtomicLong();
  private static Snapshot current = empty();

  private ClientCommissionState() {}

  private static Snapshot empty() {
    return new Snapshot(0L, 0L, 1, List.of(), -1L, false, false, "");
  }

  public static synchronized long nextRequestId() {
    long id = IDS.incrementAndGet();
    if (id < 0) throw new IllegalStateException("commission request id exhausted");
    current = current.withRequest(id, true, false, "");
    return id;
  }

  public static synchronized Snapshot snapshot() { return current; }

  public static synchronized boolean apply(CommissionDataResponseMessage message) {
    if (message.requestId() != current.requestId()) return false;
    if (message.kind() == com.mo.economy_system.common.network.CommissionDataResponseKind.ERROR) {
      current = current.withRequest(current.requestId(), false, true, message.errorKey());
      return true;
    }
    current = new Snapshot(message.nextRefreshAt(), message.serverNowMillis(),
        message.maxActivePersonalCommissions(), message.commissions(), message.requestId(),
        false, false, "");
    return true;
  }

  public static synchronized void applyActionError(long requestId, String error) {
    if (requestId != current.requestId()) return;
    current = current.withRequest(requestId, false, true, error);
  }

  public static synchronized void reset() { current = empty(); }

  public record Snapshot(long nextRefreshAt, long serverNowMillis, int maxActivePersonalCommissions,
                         List<CommissionInstance> commissions, long requestId, boolean loading,
                         boolean error, String errorMessage) {
    public Snapshot {
      commissions = List.copyOf(commissions);
      errorMessage = errorMessage == null ? "" : errorMessage;
    }

    private Snapshot withRequest(long id, boolean nextLoading, boolean nextError, String message) {
      return new Snapshot(nextRefreshAt, serverNowMillis, maxActivePersonalCommissions,
          commissions, id, nextLoading, nextError, message);
    }
  }
}
