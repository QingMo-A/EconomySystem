package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.CheckedFileTransferChunkRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlRequestMessage;
import com.mo.economy_system.common.network.CheckedFileTransferControlResponseMessage;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CheckedFileTransferRoutingServiceTest {
  @Test
  void wrongAuthenticatedTargetCannotDiscardAnotherTransfer() throws Exception {
    Fixture fixture = new Fixture(new byte[] {1});
    assertEquals(CheckedFileTransferStore.Result.WRONG_TARGET,
        CheckedFileTransferRoutingService.control(fixture.ready(), UUID.randomUUID(), 2,
            fixture.store, ignored -> new Object(), (player, message) -> {}));
    assertEquals(1, fixture.store.size(2));
  }

  @Test
  void readySendFailureConsumesAndReplayIsNotFound() throws Exception {
    Fixture fixture = new Fixture(new byte[] {1});
    assertEquals(CheckedFileTransferStore.Result.REQUESTER_OFFLINE,
        CheckedFileTransferRoutingService.control(fixture.ready(), fixture.target, 2,
            fixture.store, ignored -> new Object(), (player, message) -> {
              throw new IllegalStateException("send");
            }));
    assertEquals(CheckedFileTransferStore.Result.NOT_FOUND,
        CheckedFileTransferRoutingService.control(fixture.ready(), fixture.target, 3,
            fixture.store, ignored -> new Object(), (player, message) -> {}));
  }

  @Test
  void readyErrorEscapesAfterConsumption() throws Exception {
    Fixture fixture = new Fixture(new byte[] {1});
    assertThrows(AssertionError.class,
        () -> CheckedFileTransferRoutingService.control(fixture.ready(), fixture.target, 2,
            fixture.store, ignored -> new Object(), (player, message) -> {
              throw new AssertionError("fatal");
            }));
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void chunkFailureCannotAdvanceOrReplay() throws Exception {
    Fixture fixture = new Fixture(new byte[] {1});
    CheckedFileTransferRoutingService.control(fixture.ready(), fixture.target, 2,
        fixture.store, ignored -> new Object(), (player, message) -> {});
    var chunk = fixture.chunk(0, 1, new byte[] {1});
    assertEquals(CheckedFileTransferStore.Result.REQUESTER_OFFLINE,
        CheckedFileTransferRoutingService.chunk(chunk, fixture.target, 3, fixture.store,
            ignored -> new Object(), (player, message) -> { throw new IllegalStateException(); }));
    assertEquals(CheckedFileTransferStore.Result.NOT_FOUND,
        CheckedFileTransferRoutingService.chunk(chunk, fixture.target, 4, fixture.store,
            ignored -> new Object(), (player, message) -> {}));
  }

  @Test
  void finalChunkForwardsChunkThenServerGeneratedComplete() throws Exception {
    Fixture fixture = new Fixture(new byte[] {1});
    AtomicInteger sent = new AtomicInteger();
    CheckedFileTransferRoutingService.control(fixture.ready(), fixture.target, 2,
        fixture.store, ignored -> new Object(), (player, message) -> {});
    assertEquals(CheckedFileTransferStore.Result.COMPLETE,
        CheckedFileTransferRoutingService.chunk(fixture.chunk(0, 1, new byte[] {1}),
            fixture.target, 3, fixture.store, ignored -> new Object(),
            (player, message) -> sent.incrementAndGet()));
    assertEquals(2, sent.get());
    assertEquals(0, fixture.store.size(3));
  }

  @Test
  void clientCompleteIsRejectedAndConsumed() throws Exception {
    Fixture fixture = new Fixture(new byte[0]);
    var complete = new CheckedFileTransferControlRequestMessage("Target", fixture.target,
        "Requester", fixture.requester, ClientFileCheckType.MODS, "a.jar",
        CheckedFileTransferControlJsonCodec.encode(
            CheckedFileTransferControl.complete(fixture.transfer, 0, fixture.hash)));
    assertEquals(CheckedFileTransferStore.Result.INVALID_METADATA,
        CheckedFileTransferRoutingService.control(complete, fixture.target, 2, fixture.store,
            ignored -> new Object(), (player, message) -> {}));
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void invalidChunkConsumesThenSendsAuthoritativeFailureAndReplayIsNotFound() throws Exception {
    Fixture fixture = new Fixture(new byte[] {1});
    CheckedFileTransferRoutingService.control(fixture.ready(), fixture.target, 2,
        fixture.store, ignored -> new Object(), (player, message) -> {});
    List<Object> sent = new ArrayList<>();
    var invalid = fixture.chunk(0, 1, new byte[0]);
    assertEquals(CheckedFileTransferStore.Result.INVALID_CHUNK,
        CheckedFileTransferRoutingService.chunk(invalid, fixture.target, 3, fixture.store,
            ignored -> new Object(), (player, message) -> sent.add(message)));
    assertEquals(1, sent.size());
    var response = (CheckedFileTransferControlResponseMessage) sent.get(0);
    var control = CheckedFileTransferControlJsonCodec.decode(response.controlPayload());
    assertEquals(CheckedFileTransferControlStatus.FAILED, control.status());
    assertEquals("INVALID_CHUNK", control.errorCode());
    assertEquals("Target", response.targetPlayerName());
    assertEquals(CheckedFileTransferStore.Result.NOT_FOUND,
        CheckedFileTransferRoutingService.chunk(invalid, fixture.target, 4, fixture.store,
            ignored -> new Object(), (player, message) -> {}));
  }

  private static final class Fixture {
    final UUID target = UUID.randomUUID();
    final UUID requester = UUID.randomUUID();
    final UUID transfer = UUID.randomUUID();
    final byte[] content;
    final String hash;
    final CheckedFileTransferStore store = new CheckedFileTransferStore();

    Fixture(byte[] content) throws Exception {
      this.content = content;
      this.hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
      var key = new CheckedFileTransferStore.Key(target, requester, ClientFileCheckType.MODS, "a.jar");
      assertEquals(CheckedFileTransferStore.Result.CREATED,
          store.create(new CheckedFileTransferStore.Pending(
              key, "Target", "Requester", content.length, hash, 1, 100), 1));
    }

    CheckedFileTransferControlRequestMessage ready() {
      return new CheckedFileTransferControlRequestMessage("Target", target, "Requester", requester,
          ClientFileCheckType.MODS, "a.jar", CheckedFileTransferControlJsonCodec.encode(
              CheckedFileTransferControl.ready(transfer, content.length, hash)));
    }

    CheckedFileTransferChunkRequestMessage chunk(int index, int total, byte[] raw) {
      return new CheckedFileTransferChunkRequestMessage("Target", target, "Requester", requester,
          ClientFileCheckType.MODS, "a.jar", transfer, index, total,
          Base64.getEncoder().encodeToString(raw));
    }
  }
}
