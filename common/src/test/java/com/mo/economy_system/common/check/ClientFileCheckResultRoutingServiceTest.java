package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ClientFileCheckResultRequestMessage;
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

  private static String validJson() {
    return ClientFileCheckResultJsonCodec.encode(
        ClientFileCheckResult.declined(ClientFileCheckType.MODS));
  }

  private static final class Fixture {
    final UUID target = UUID.randomUUID();
    final UUID requester = UUID.randomUUID();
    final ClientFileCheckRequestStore store = new ClientFileCheckRequestStore();

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
  }
}
