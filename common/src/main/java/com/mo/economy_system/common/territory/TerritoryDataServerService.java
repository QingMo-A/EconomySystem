package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Loader-neutral protocol-17 orchestration including one bounded ERROR fallback. */
public final class TerritoryDataServerService {
  private TerritoryDataServerService() {}

  public static boolean serve(
      TerritoryDataRequestMessage request,
      UUID playerId,
      TerritoryDataQueryService.Repository repository,
      Sender sender,
      FailureSink failures) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(playerId, "playerId");
    Objects.requireNonNull(repository, "repository");
    Objects.requireNonNull(sender, "sender");
    Objects.requireNonNull(failures, "failures");
    List<Owned> owned = null;
    List<Summary> authorized = null;
    String stage = "owned";
    try {
      owned = Objects.requireNonNull(repository.owned(playerId), "owned");
      stage = "authorized";
      authorized = Objects.requireNonNull(repository.authorized(playerId), "authorized");
      List<Owned> capturedOwned = owned;
      List<Summary> capturedAuthorized = authorized;
      stage = "response";
      TerritoryDataResponseMessage response = TerritoryDataQueryService.query(
          request, playerId, new TerritoryDataQueryService.Repository() {
            public List<Owned> owned(UUID ignored) { return capturedOwned; }
            public List<Summary> authorized(UUID ignored) { return capturedAuthorized; }
          });
      stage = "data-send";
      sender.send(response);
      return true;
    } catch (RuntimeException error) {
      failures.record(playerId, request.requestId(), stage,
          owned == null ? -1 : owned.size(), authorized == null ? -1 : authorized.size(), error);
      try {
        sender.send(TerritoryDataResponseMessage.error(request.requestId()));
      } catch (RuntimeException sendError) {
        failures.record(playerId, request.requestId(), "error-send",
            owned == null ? -1 : owned.size(), authorized == null ? -1 : authorized.size(), sendError);
      }
      return false;
    }
  }

  @FunctionalInterface public interface Sender {
    void send(TerritoryDataResponseMessage response);
  }

  @FunctionalInterface public interface FailureSink {
    void record(UUID playerId, long requestId, String stage, int ownedCount, int authorizedCount,
        RuntimeException error);
  }
}
