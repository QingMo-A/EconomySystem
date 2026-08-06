package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;

/** Client-only immutable protocol-40 state. */
public final class Forge1201SingleTerritoryClientState {
  private static long requestId = -1;
  private static Owned territory;
  private static SingleTerritoryDataResponseKind kind = SingleTerritoryDataResponseKind.ERROR;

  private Forge1201SingleTerritoryClientState() {}

  static synchronized void apply(SingleTerritoryDataResponseMessage message) {
    if (message.requestId() < requestId) return;
    requestId = message.requestId();
    kind = message.kind();
    territory = message.territory().orElse(null);
  }

  public static synchronized Snapshot snapshot() {
    return new Snapshot(requestId, kind, territory);
  }

  public record Snapshot(
      long requestId, SingleTerritoryDataResponseKind kind, Owned territory) {}
}
