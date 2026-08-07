package com.mo.economy_system.target.neoforge1211.client;

import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;

/** Client-side handoff for the common territory-management Screen shell. */
public final class NeoForge1211SingleTerritoryClientState {
  private static long requestId = -1;
  private static SingleTerritoryDataResponseMessage response;

  private NeoForge1211SingleTerritoryClientState() {}

  public static synchronized void apply(SingleTerritoryDataResponseMessage value) {
    if (value.requestId() < requestId) return;
    requestId = value.requestId();
    response = value;
  }

  public static synchronized Snapshot snapshot() {
    return new Snapshot(requestId, response);
  }

  public record Snapshot(long requestId, SingleTerritoryDataResponseMessage response) {}
}
