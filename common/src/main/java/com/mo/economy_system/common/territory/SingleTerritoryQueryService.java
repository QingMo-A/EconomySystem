package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.SingleTerritoryDataRequestMessage;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseKind;
import com.mo.economy_system.common.network.SingleTerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import java.util.Objects;
import java.util.UUID;

/** Protocol 39 query; full territory details are visible only to the current owner. */
public final class SingleTerritoryQueryService {
  private SingleTerritoryQueryService() {}

  public static SingleTerritoryDataResponseMessage query(
      SingleTerritoryDataRequestMessage request, UUID requesterId, Repository repository) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(requesterId, "requesterId");
    Objects.requireNonNull(repository, "repository");
    try {
      Owned territory = repository.find(request.territoryId());
      if (territory == null) {
        return SingleTerritoryDataResponseMessage.empty(
            SingleTerritoryDataResponseKind.NOT_FOUND, request.requestId());
      }
      if (!territory.summary().ownerId().equals(requesterId)) {
        return SingleTerritoryDataResponseMessage.empty(
            SingleTerritoryDataResponseKind.UNAUTHORIZED, request.requestId());
      }
      return SingleTerritoryDataResponseMessage.data(request.requestId(), territory);
    } catch (RuntimeException failure) {
      return SingleTerritoryDataResponseMessage.empty(
          SingleTerritoryDataResponseKind.ERROR, request.requestId());
    }
  }

  @FunctionalInterface
  public interface Repository {
    Owned find(UUID territoryId);
  }
}
