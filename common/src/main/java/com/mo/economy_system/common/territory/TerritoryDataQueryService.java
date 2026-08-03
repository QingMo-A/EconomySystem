package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TerritoryDataQueryService {
  private TerritoryDataQueryService() {}

  public static TerritoryDataResponseMessage query(
      TerritoryDataRequestMessage request, UUID requesterId, Repository repository) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(requesterId, "requesterId");
    Objects.requireNonNull(repository, "repository");
    List<Owned> rawOwned = Objects.requireNonNull(repository.owned(requesterId), "owned");
    List<Summary> rawAuthorized = Objects.requireNonNull(repository.authorized(requesterId), "authorized");
    Map<UUID, Owned> owned = new LinkedHashMap<>();
    for (Owned value : rawOwned) {
      if (!value.summary().ownerId().equals(requesterId)) throw new IllegalArgumentException("owned territory owner mismatch");
      owned.putIfAbsent(value.summary().territoryId(), value);
    }
    Map<UUID, Summary> authorized = new LinkedHashMap<>();
    for (Summary value : rawAuthorized) {
      if (!value.ownerId().equals(requesterId) && !owned.containsKey(value.territoryId())) {
        authorized.putIfAbsent(value.territoryId(), value);
      }
    }
    Comparator<Summary> order = Comparator.comparing(Summary::name, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Summary::territoryId);
    List<Owned> sortedOwned = new ArrayList<>(owned.values());
    sortedOwned.sort(Comparator.comparing(Owned::summary, order));
    List<Summary> sortedAuthorized = new ArrayList<>(authorized.values());
    sortedAuthorized.sort(order);
    return TerritoryDataResponseMessage.data(request.requestId(), sortedOwned, sortedAuthorized);
  }

  public interface Repository {
    List<Owned> owned(UUID requesterId);
    List<Summary> authorized(UUID requesterId);
  }
}
