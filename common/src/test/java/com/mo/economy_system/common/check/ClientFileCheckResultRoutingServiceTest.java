package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
import com.mo.economy_system.common.transfer.ClientFileCheckManifestAuthorizationStore;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientFileCheckResultRoutingServiceTest {
  @Test
  void deliversOnceAndRejectsReplay() {
    Fixture fixture = new Fixture();
    var outcome = fixture.route(fixture.message(validJson()), (requester, response) -> {});
    assertEquals(ClientFileCheckResultRoutingService.Outcome.DELIVERED, outcome);
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.NOT_FOUND,
        fixture.route(fixture.message(validJson()), (requester, response) -> {}));
  }

  @Test
  void malformedResultIsConvertedAndConsumed() {
    Fixture fixture = new Fixture();
    var outcome =
        fixture.route(
            fixture.message("{}"),
            (requester, response) ->
                assertEquals(
                    ClientFileCheckStatus.FAILED,
                    ClientFileCheckResultJsonCodec.decode(response.resultJson()).status()));
    assertEquals(ClientFileCheckResultRoutingService.Outcome.DELIVERED, outcome);
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void malformedResultClearsPreviousAuthorizationScope() {
    Fixture fixture = new Fixture();
    fixture.installAuthorization();
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.DELIVERED,
        fixture.routeWithAuthorizations(fixture.message("{}"), ignored -> new Object(),
            (requester, response) -> {}));
    fixture.assertAuthorizationMissing();
  }

  @Test
  void requesterOfflineClearsPreviousAuthorizationScope() {
    Fixture fixture = new Fixture();
    fixture.installAuthorization();
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.REQUESTER_OFFLINE,
        fixture.routeWithAuthorizations(fixture.message(validJson()), ignored -> null,
            (requester, response) -> fail()));
    fixture.assertAuthorizationMissing();
  }

  @Test
  void senderRuntimeFailureClearsPreviousAuthorizationScope() {
    Fixture fixture = new Fixture();
    fixture.installAuthorization();
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.DELIVERY_FAILED,
        fixture.routeWithAuthorizations(fixture.message(validJson()), ignored -> new Object(),
            (requester, response) -> {
              throw new IllegalStateException("network");
            }));
    fixture.assertAuthorizationMissing();
  }

  @Test
  void senderErrorClearsPreviousAuthorizationScopeBeforeRethrow() {
    Fixture fixture = new Fixture();
    fixture.installAuthorization();
    assertThrows(
        AssertionError.class,
        () -> fixture.routeWithAuthorizations(
            fixture.message(validJson()), ignored -> new Object(),
            (requester, response) -> {
              throw new AssertionError("fatal");
            }));
    fixture.assertAuthorizationMissing();
  }

  @Test
  void deliveryRuntimeFailureStillConsumesResult() {
    Fixture fixture = new Fixture();
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.DELIVERY_FAILED,
        fixture.route(
            fixture.message(validJson()),
            (requester, response) -> {
              throw new IllegalStateException("network");
            }));
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void deliveryErrorEscapesAfterClaimCleanup() {
    Fixture fixture = new Fixture();
    assertThrows(
        AssertionError.class,
        () ->
            fixture.route(
                fixture.message(validJson()),
                (requester, response) -> {
                  throw new AssertionError("fatal");
                }));
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void wrongTargetMissingAndBusyAreDistinct() {
    Fixture fixture = new Fixture();
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.WRONG_TARGET,
        ClientFileCheckResultRoutingService.route(
            fixture.message(validJson()),
            UUID.randomUUID(),
            2,
            fixture.store,
            ignored -> new Object(),
            (requester, response) -> {},
            (stage, target, requester, failure) -> {}));
    var claim =
        fixture.store.claim(
            new ClientFileCheckRequestStore.Key(
                fixture.target, fixture.requester, ClientFileCheckType.MODS),
            2);
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.BUSY,
        fixture.route(fixture.message(validJson()), (requester, response) -> {}));
    fixture.store.complete(claim.claim());
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.NOT_FOUND,
        fixture.route(fixture.message(validJson()), (requester, response) -> {}));
  }

  @Test
  void everyMetadataMismatchConsumesAndRejectsReplay() {
    assertMetadataMismatch(new Fixture(), "Other", null, null, null);
    assertMetadataMismatch(new Fixture(), null, "Other", null, null);
    Fixture wrongKey = new Fixture();
    var wrongRequester =
        new ClientFileCheckResultRequestMessage(
            "Target",
            wrongKey.target,
            "Requester",
            UUID.randomUUID(),
            ClientFileCheckType.MODS,
            validJson());
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.NOT_FOUND,
        wrongKey.route(wrongRequester, (requester, response) -> fail()));
    var wrongType =
        new ClientFileCheckResultRequestMessage(
            "Target",
            wrongKey.target,
            "Requester",
            wrongKey.requester,
            ClientFileCheckType.SHADERPACKS,
            ClientFileCheckResultJsonCodec.encode(
                ClientFileCheckResult.declined(ClientFileCheckType.SHADERPACKS)));
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.NOT_FOUND,
        wrongKey.route(wrongType, (requester, response) -> fail()));
  }

  @Test
  void requesterOfflineConsumesAndDiagnosticsRuntimeCannotReopen() {
    Fixture fixture = new Fixture();
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.REQUESTER_OFFLINE,
        ClientFileCheckResultRoutingService.route(
            fixture.message(validJson()),
            fixture.target,
            2,
            fixture.store,
            ignored -> null,
            (requester, response) -> fail(),
            (stage, target, requester, failure) -> {
              throw new IllegalStateException("log");
            }));
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void lookupRuntimeAndErrorEscapeAfterConsumption() {
    Fixture runtime = new Fixture();
    assertThrows(
        IllegalStateException.class,
        () ->
            ClientFileCheckResultRoutingService.route(
                runtime.message(validJson()),
                runtime.target,
                2,
                runtime.store,
                ignored -> {
                  throw new IllegalStateException("lookup");
                },
                (requester, response) -> {},
                (stage, target, requester, failure) -> {}));
    assertEquals(0, runtime.store.size(2));
    Fixture error = new Fixture();
    assertThrows(
        AssertionError.class,
        () ->
            ClientFileCheckResultRoutingService.route(
                error.message(validJson()),
                error.target,
                2,
                error.store,
                ignored -> {
                  throw new AssertionError("lookup");
                },
                (requester, response) -> {},
                (stage, target, requester, failure) -> {}));
    assertEquals(0, error.store.size(2));
  }

  @Test
  void diagnosticsErrorEscapesAfterOfflineResultIsConsumed() {
    Fixture fixture = new Fixture();
    assertThrows(
        AssertionError.class,
        () ->
            ClientFileCheckResultRoutingService.route(
                fixture.message(validJson()),
                fixture.target,
                2,
                fixture.store,
                ignored -> null,
                (requester, response) -> {},
                (stage, target, requester, failure) -> {
                  throw new AssertionError("diagnostics");
                }));
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void allTerminalStatusesAreDeliveredWithoutMutation() {
    for (ClientFileCheckResult result :
        java.util.List.of(
            ClientFileCheckResult.declined(ClientFileCheckType.MODS),
            ClientFileCheckResult.failed(ClientFileCheckType.MODS, "SCAN_FAILED"),
            new ClientFileCheckResult(
                1,
                ClientFileCheckStatus.SUCCESS,
                ClientFileCheckType.MODS,
                java.util.List.of(),
                java.util.List.of(),
                null),
            new ClientFileCheckResult(
                1,
                ClientFileCheckStatus.TRUNCATED,
                ClientFileCheckType.MODS,
                java.util.List.of(),
                java.util.List.of(),
                "FILE_LIMIT"))) {
      Fixture fixture = new Fixture();
      fixture.route(
          fixture.message(ClientFileCheckResultJsonCodec.encode(result)),
          (requester, response) ->
              assertEquals(result, ClientFileCheckResultJsonCodec.decode(response.resultJson())));
    }
  }

  private static void assertMetadataMismatch(
      Fixture fixture,
      String targetName,
      String requesterName,
      UUID requesterId,
      ClientFileCheckType type) {
    var message =
        new ClientFileCheckResultRequestMessage(
            targetName == null ? "Target" : targetName,
            fixture.target,
            requesterName == null ? "Requester" : requesterName,
            requesterId == null ? fixture.requester : requesterId,
            type == null ? ClientFileCheckType.MODS : type,
            validJson());
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.METADATA_MISMATCH,
        fixture.route(message, (requester, response) -> fail()));
    assertEquals(
        ClientFileCheckResultRoutingService.Outcome.NOT_FOUND,
        fixture.route(fixture.message(validJson()), (requester, response) -> {}));
  }

  private static String validJson() {
    return ClientFileCheckResultJsonCodec.encode(
        ClientFileCheckResult.declined(ClientFileCheckType.MODS));
  }

  private static final class Fixture {
    final UUID target = UUID.randomUUID();
    final UUID requester = UUID.randomUUID();
    final ClientFileCheckRequestStore store = new ClientFileCheckRequestStore();
    final ClientFileCheckManifestAuthorizationStore authorizations =
        new ClientFileCheckManifestAuthorizationStore();

    Fixture() {
      assertEquals(
          ClientFileCheckRequestStore.PutResult.CREATED,
          store.put(
              new ClientFileCheckRequestStore.Pending(
                  target, "Target", requester, "Requester", ClientFileCheckType.MODS, 1, 100),
              1));
    }

    ClientFileCheckResultRequestMessage message(String json) {
      return new ClientFileCheckResultRequestMessage(
          "Target", target, "Requester", requester, ClientFileCheckType.MODS, json);
    }

    ClientFileCheckResultRoutingService.Outcome route(
        ClientFileCheckResultRequestMessage message,
        ClientFileCheckResultRoutingService.ResponseSender sender) {
      return ClientFileCheckResultRoutingService.route(
          message,
          target,
          2,
          store,
          ignored -> new Object(),
          sender,
          (stage, targetId, requesterId, failure) -> {});
    }

    ClientFileCheckResultRoutingService.Outcome routeWithAuthorizations(
        ClientFileCheckResultRequestMessage message,
        ClientFileCheckResultRoutingService.RequesterLookup lookup,
        ClientFileCheckResultRoutingService.ResponseSender sender) {
      return ClientFileCheckResultRoutingService.route(
          message,
          target,
          2,
          store,
          authorizations,
          lookup,
          sender,
          (stage, targetId, requesterId, failure) -> {});
    }

    void installAuthorization() {
      ClientFileCheckResult result =
          new ClientFileCheckResult(
              1,
              ClientFileCheckStatus.SUCCESS,
              ClientFileCheckType.MODS,
              List.of(new ClientFileCheckEntry("old.jar", 1, "0".repeat(64))),
              List.of(),
              null);
      assertEquals(
          ClientFileCheckManifestAuthorizationStore.ReplaceResult.INSTALLED,
          authorizations.replace(target, requester, result, 1));
    }

    void assertAuthorizationMissing() {
      var key =
          new ClientFileCheckManifestAuthorizationStore.Key(
              target, requester, ClientFileCheckType.MODS, "old.jar");
      assertTrue(authorizations.find(key, 2).isEmpty());
    }
  }
}
