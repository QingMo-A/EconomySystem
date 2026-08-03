package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseKind;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryDataServerServiceTest {
  @Test void successSendsOnlyData() {
    List<TerritoryDataResponseMessage> sent = new ArrayList<>();
    assertTrue(TerritoryDataServerService.serve(new TerritoryDataRequestMessage(4),
        TerritoryTestFixtures.OWNER, repository(), sent::add, failNever()));
    assertEquals(1, sent.size());
    assertEquals(TerritoryDataResponseKind.DATA, sent.get(0).kind());
  }

  @Test void repositoryFailureSendsExactlyOneError() {
    List<TerritoryDataResponseMessage> sent = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    var repository = new TerritoryDataQueryService.Repository() {
      public List<TerritorySnapshots.Owned> owned(UUID requester) { throw new IllegalStateException("boom"); }
      public List<TerritorySnapshots.Summary> authorized(UUID requester) { return List.of(); }
    };
    assertFalse(TerritoryDataServerService.serve(new TerritoryDataRequestMessage(5),
        TerritoryTestFixtures.OWNER, repository, sent::add,
        (player, request, stage, owned, authorized, error) ->
            failures.add(player + ":" + request + ":" + stage + ":" + owned + ":" + authorized)));
    assertEquals(List.of(TerritoryDataResponseMessage.error(5)), sent);
    assertEquals(1, failures.size());
    assertTrue(failures.get(0).endsWith(":5:owned:-1:-1"));
  }

  @Test void dataSendFailureAttemptsErrorOnceAndSuppressesItsFailure() {
    List<String> failures = new ArrayList<>();
    int[] sends = {0};
    assertFalse(TerritoryDataServerService.serve(new TerritoryDataRequestMessage(6),
        TerritoryTestFixtures.OWNER, repository(), response -> {
          sends[0]++;
          throw new IllegalStateException(response.kind().id());
        }, (player, request, stage, owned, authorized, error) -> failures.add(stage + ":" + error.getMessage())));
    assertEquals(2, sends[0]);
    assertEquals(List.of("data-send:data", "error-send:error"), failures);
  }

  @Test void authorizedFailureReportsKnownOwnedCountAndSendsNoPartialData() {
    List<TerritoryDataResponseMessage> sent = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    var repository = new TerritoryDataQueryService.Repository() {
      public List<TerritorySnapshots.Owned> owned(UUID requester) {
        return List.of(TerritoryTestFixtures.owned());
      }
      public List<TerritorySnapshots.Summary> authorized(UUID requester) {
        throw new IllegalStateException("authorized failed");
      }
    };
    assertFalse(TerritoryDataServerService.serve(new TerritoryDataRequestMessage(8),
        TerritoryTestFixtures.OWNER, repository, sent::add,
        (player, request, stage, owned, authorized, error) ->
            failures.add(stage + ":" + owned + ":" + authorized)));
    assertEquals(List.of(TerritoryDataResponseMessage.error(8)), sent);
    assertEquals(List.of("authorized:1:-1"), failures);
  }

  private static TerritoryDataQueryService.Repository repository() {
    return new TerritoryDataQueryService.Repository() {
      public List<TerritorySnapshots.Owned> owned(UUID requester) {
        return List.of(TerritoryTestFixtures.owned());
      }
      public List<TerritorySnapshots.Summary> authorized(UUID requester) { return List.of(); }
    };
  }

  private static TerritoryDataServerService.FailureSink failNever() {
    return (player, request, stage, owned, authorized, error) -> fail("unexpected failure", error);
  }
}
