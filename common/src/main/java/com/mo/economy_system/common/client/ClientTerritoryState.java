package com.mo.economy_system.common.client;

import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import java.util.Optional;

/** Loader-neutral stale-response gate for clients without an active territory screen adapter. */
public final class ClientTerritoryState {
  private static long activeRequestId = -1;
  private static TerritoryDataResponseMessage current;
  private ClientTerritoryState() {}
  public static synchronized void begin(long requestId) {
    if (requestId < 0 || requestId <= activeRequestId) throw new IllegalArgumentException("request id must increase");
    activeRequestId = requestId;
  }
  public static synchronized boolean apply(TerritoryDataResponseMessage response) {
    if (response.requestId() != activeRequestId) return false;
    current = response;
    return true;
  }
  public static synchronized Optional<TerritoryDataResponseMessage> current() {
    return Optional.ofNullable(current);
  }
  static synchronized void resetForTest() { activeRequestId = -1; current = null; }
}
