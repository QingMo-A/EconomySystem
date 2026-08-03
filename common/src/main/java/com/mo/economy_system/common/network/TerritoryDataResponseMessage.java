package com.mo.economy_system.common.network;

import com.mo.economy_system.common.territory.TerritoryResponseBudget;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.platform.network.EconomyNetworkMessage;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TerritoryDataResponseMessage(
    TerritoryDataResponseKind kind, long requestId, List<Owned> owned, List<Summary> authorized)
    implements EconomyNetworkMessage {
  public TerritoryDataResponseMessage {
    Objects.requireNonNull(kind, "kind");
    if (requestId < 0) throw new IllegalArgumentException("negative territory request id");
    owned = copy(owned, "owned");
    authorized = copy(authorized, "authorized");
    if (kind == TerritoryDataResponseKind.ERROR) {
      if (!owned.isEmpty() || !authorized.isEmpty()) {
        throw new IllegalArgumentException("error territory response cannot carry data");
      }
    } else {
      if ((long) owned.size() + authorized.size() > EconomyNetworkLimits.MAX_TERRITORIES_PER_RESPONSE) {
        throw new IllegalArgumentException("too many territories");
      }
      Set<UUID> ids = new HashSet<>();
      for (Owned value : owned) if (!ids.add(value.summary().territoryId())) throw new IllegalArgumentException("duplicate owned territory");
      for (Summary value : authorized) if (!ids.add(value.territoryId())) throw new IllegalArgumentException("duplicate or overlapping territory");
      TerritoryResponseBudget.requireWithinBudget(owned, authorized);
    }
  }

  public static TerritoryDataResponseMessage data(
      long requestId, List<Owned> owned, List<Summary> authorized) {
    return new TerritoryDataResponseMessage(TerritoryDataResponseKind.DATA, requestId, owned, authorized);
  }

  public static TerritoryDataResponseMessage error(long requestId) {
    return new TerritoryDataResponseMessage(
        TerritoryDataResponseKind.ERROR, requestId, List.of(), List.of());
  }

  private static <T> List<T> copy(List<T> values, String name) {
    Objects.requireNonNull(values, name);
    if (values.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("null " + name + " entry");
    return List.copyOf(values);
  }
}
