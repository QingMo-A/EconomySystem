package com.mo.economy_system.common.client;

import com.mo.economy_system.common.commission.PublicCommission;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseKind;
import com.mo.economy_system.common.network.commission_public.PublicCommissionDataResponseMessage;
import com.mo.economy_system.common.network.commission_public.PublicCommissionSubmitStatus;
import com.mo.economy_system.common.network.commission_public.PublicCommissionActionResponseMessage;
import java.util.List;

/** Latest server-authoritative public commission snapshot shared by target screens. */
public final class ClientPublicCommissionState {
  private static long nextRequestId = 1L;
  private static Snapshot snapshot = new Snapshot(-1L, true, 0L, List.of(), "", null, "");

  private ClientPublicCommissionState() {}

  public static synchronized long nextRequestId() {
    if (nextRequestId == Long.MAX_VALUE) throw new IllegalStateException("public commission request id exhausted");
    long id = nextRequestId++;
    snapshot = snapshot.withRequest(id, true, "");
    return id;
  }

  public static synchronized Snapshot snapshot() { return snapshot; }

  public static synchronized boolean apply(PublicCommissionDataResponseMessage response) {
    if (response.requestId() < snapshot.requestId()) return false;
    if (response.kind() == PublicCommissionDataResponseKind.ERROR) {
      snapshot = new Snapshot(response.requestId(), false, response.serverNowMillis(), List.of(),
          response.errorKey(), null, "");
      return true;
    }
    snapshot = new Snapshot(response.requestId(), false, response.serverNowMillis(),
        response.commissions(), "", snapshot.lastSubmitStatus(), snapshot.actionMessage());
    return true;
  }

  public static synchronized void applyAction(PublicCommissionActionResponseMessage response) {
    if (response.requestId() < snapshot.requestId()) return;
    snapshot = new Snapshot(response.requestId(), false, snapshot.serverNowMillis(),
        snapshot.commissions(), response.status() == PublicCommissionSubmitStatus.REJECTED
            ? "screen.commissions.public.submit_failed" : "", response.status(), response.message());
  }

  public static synchronized void reset() {
    snapshot = new Snapshot(-1L, true, 0L, List.of(), "", null, "");
  }

  public record Snapshot(long requestId, boolean loading, long serverNowMillis,
                         List<PublicCommission> commissions, String errorKey,
                         PublicCommissionSubmitStatus lastSubmitStatus, String actionMessage) {
    public Snapshot {
      commissions = List.copyOf(commissions);
      errorKey = errorKey == null ? "" : errorKey;
      actionMessage = actionMessage == null ? "" : actionMessage;
    }
    private Snapshot withRequest(long id, boolean nextLoading, String message) {
      return new Snapshot(id, nextLoading, serverNowMillis, commissions, message,
          lastSubmitStatus, actionMessage);
    }
  }
}
