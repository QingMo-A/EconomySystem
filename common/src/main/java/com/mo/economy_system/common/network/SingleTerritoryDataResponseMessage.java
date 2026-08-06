package com.mo.economy_system.common.network;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.Objects;
import java.util.Optional;

public record SingleTerritoryDataResponseMessage(
    SingleTerritoryDataResponseKind kind, long requestId, Optional<Owned> territory)
    implements EconomyNetworkMessage {
  public SingleTerritoryDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(territory, "territory");
    if (requestId < 0) throw new IllegalArgumentException("negative single territory request id");
    if ((kind == SingleTerritoryDataResponseKind.DATA) != territory.isPresent()) {
      throw new IllegalArgumentException("single territory response payload mismatch");
    }
  }

  public static SingleTerritoryDataResponseMessage data(long requestId, Owned territory) {
    return new SingleTerritoryDataResponseMessage(
        SingleTerritoryDataResponseKind.DATA, requestId, Optional.of(territory));
  }

  public static SingleTerritoryDataResponseMessage empty(
      SingleTerritoryDataResponseKind kind, long requestId) {
    if (kind == SingleTerritoryDataResponseKind.DATA) throw new IllegalArgumentException("data requires territory");
    return new SingleTerritoryDataResponseMessage(kind, requestId, Optional.empty());
  }
}
