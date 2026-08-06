package com.mo.economy_system.common.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.mo.economy_system.common.check.ClientFileCheckType;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CheckedFileTransferStoreTest {
  @Test
  void streamsInOrderWithoutAccumulatorAndConsumesFinalChunk() throws Exception {
    byte[] bytes = new byte[18_001];
    Arrays.fill(bytes, (byte) 7);
    Fixture fixture = new Fixture(new CheckedFileTransferStore(), bytes, 1, 100);
    UUID transferId = UUID.randomUUID();
    assertEquals(CheckedFileTransferStore.Result.READY,
        fixture.store.ready(fixture.key, fixture.target,
            CheckedFileTransferControl.ready(transferId, bytes.length, fixture.hash), 2));
    assertEquals(CheckedFileTransferStore.Result.FORWARD,
        fixture.store.chunk(fixture.key, fixture.target, transferId, 0, 2,
            Arrays.copyOf(bytes, 18_000), 3).result());
    assertEquals(CheckedFileTransferStore.Result.COMPLETE,
        fixture.store.chunk(fixture.key, fixture.target, transferId, 1, 2,
            new byte[] {7}, 4).result());
    assertEquals(0, fixture.store.size(4));
  }

  @Test
  void enforcesTargetAndRequesterSingleActiveOwnership() throws Exception {
    CheckedFileTransferStore store = new CheckedFileTransferStore();
    Fixture first = new Fixture(store, new byte[] {1}, 1, 100);
    var sameTarget = pending(first.target, UUID.randomUUID(), "b.jar", 2, 100, new byte[] {2});
    var sameRequester = pending(UUID.randomUUID(), first.requester, "c.jar", 2, 100, new byte[] {3});
    assertEquals(CheckedFileTransferStore.Result.TARGET_BUSY, store.create(sameTarget, 2));
    assertEquals(CheckedFileTransferStore.Result.REQUESTER_BUSY, store.create(sameRequester, 2));
  }

  @Test
  void discardTargetClearsRequesterOwnershipCooldownAndTransferIndex() throws Exception {
    CheckedFileTransferStore store = new CheckedFileTransferStore();
    Fixture first = new Fixture(store, new byte[] {1}, 1, 100);
    UUID reusedTransferId = UUID.randomUUID();
    assertEquals(CheckedFileTransferStore.Result.READY,
        store.ready(first.key, first.target,
            CheckedFileTransferControl.ready(reusedTransferId, 1, first.hash), 2));
    store.discardTarget(first.target);

    var replacement = pending(UUID.randomUUID(), first.requester, "b.jar", 3, 100, new byte[] {2});
    assertEquals(CheckedFileTransferStore.Result.CREATED, store.create(replacement, 3));
    assertEquals(CheckedFileTransferStore.Result.READY,
        store.ready(replacement.key(), replacement.key().targetPlayerId(),
            CheckedFileTransferControl.ready(reusedTransferId, 1, hash(new byte[] {2})), 4));
  }

  @Test
  void discardRequesterClearsTargetOwnershipAndCooldown() throws Exception {
    CheckedFileTransferStore store = new CheckedFileTransferStore();
    Fixture first = new Fixture(store, new byte[] {1}, 1, 100);
    store.discardRequester(first.requester);
    var replacement = pending(first.target, UUID.randomUUID(), "b.jar", 2, 100, new byte[] {2});
    assertEquals(CheckedFileTransferStore.Result.CREATED, store.create(replacement, 2));
  }

  @Test
  void readyReplayAndChunkReplayConsumeExactTransfer() throws Exception {
    Fixture readyReplay = new Fixture(new CheckedFileTransferStore(), new byte[] {1}, 1, 100);
    UUID id = UUID.randomUUID();
    var ready = CheckedFileTransferControl.ready(id, 1, readyReplay.hash);
    assertEquals(CheckedFileTransferStore.Result.READY,
        readyReplay.store.ready(readyReplay.key, readyReplay.target, ready, 2));
    assertEquals(CheckedFileTransferStore.Result.INVALID_METADATA,
        readyReplay.store.ready(readyReplay.key, readyReplay.target, ready, 3));
    assertEquals(0, readyReplay.store.size(3));

    Fixture chunkReplay = new Fixture(new CheckedFileTransferStore(), new byte[] {1, 2}, 1, 100);
    UUID chunkId = UUID.randomUUID();
    chunkReplay.store.ready(chunkReplay.key, chunkReplay.target,
        CheckedFileTransferControl.ready(chunkId, 2, chunkReplay.hash), 2);
    assertEquals(CheckedFileTransferStore.Result.CHUNK_OUT_OF_ORDER,
        chunkReplay.store.chunk(chunkReplay.key, chunkReplay.target, chunkId, 1, 1,
            new byte[] {1, 2}, 3).result());
    assertEquals(CheckedFileTransferStore.Result.NOT_FOUND,
        chunkReplay.store.chunk(chunkReplay.key, chunkReplay.target, chunkId, 0, 1,
            new byte[] {1, 2}, 4).result());
  }

  @Test
  void everyForwardingStateExpiresAndTickRollbackClears() throws Exception {
    Fixture readyForwarding = new Fixture(new CheckedFileTransferStore(), new byte[] {1}, 1, 3);
    var prepared = readyForwarding.store.prepareReady(readyForwarding.key, readyForwarding.target,
        CheckedFileTransferControl.ready(UUID.randomUUID(), 1, readyForwarding.hash), 2);
    assertEquals(CheckedFileTransferStore.Result.READY, prepared.result());
    assertEquals(0, readyForwarding.store.size(3));

    Fixture streaming = new Fixture(new CheckedFileTransferStore(), new byte[] {1}, 10, 100);
    streaming.store.ready(streaming.key, streaming.target,
        CheckedFileTransferControl.ready(UUID.randomUUID(), 1, streaming.hash), 11);
    assertEquals(0, streaming.store.size(9));
  }

  @Test
  void zeroByteTransferCompletesWithoutChunkAndCanBeConsumed() throws Exception {
    Fixture fixture = new Fixture(new CheckedFileTransferStore(), new byte[0], 1, 100);
    UUID id = UUID.randomUUID();
    var prepared = fixture.store.prepareReady(fixture.key, fixture.target,
        CheckedFileTransferControl.ready(id, 0, fixture.hash), 2);
    assertEquals(CheckedFileTransferStore.Result.COMPLETE,
        fixture.store.commitReady(prepared.claim(), 2));
    assertEquals(1, fixture.store.size(2));
    fixture.store.complete(fixture.key, 2);
    assertEquals(0, fixture.store.size(2));
  }

  @Test
  void digestCloneRuntimeFailureConsumesTransfer() throws Exception {
    var support = new CheckedFileTransferStore.DigestSupport() {
      @Override public MessageDigest create() { return sha256(); }
      @Override public MessageDigest copy(MessageDigest source) {
        throw new IllegalStateException("clone failed");
      }
    };
    CheckedFileTransferStore store = new CheckedFileTransferStore(4, support, 1);
    Fixture fixture = new Fixture(store, new byte[] {1}, 1, 100);
    UUID id = UUID.randomUUID();
    store.ready(fixture.key, fixture.target,
        CheckedFileTransferControl.ready(id, 1, fixture.hash), 2);
    assertEquals(CheckedFileTransferStore.Result.INVALID_CHUNK,
        store.prepareChunk(fixture.key, fixture.target, id, 0, 1, new byte[] {1}, 3).result());
    assertEquals(0, store.size(3));
  }

  @Test
  void digestCloneErrorEscapesAfterConsumption() throws Exception {
    var support = new CheckedFileTransferStore.DigestSupport() {
      @Override public MessageDigest create() { return sha256(); }
      @Override public MessageDigest copy(MessageDigest source) { throw new AssertionError("fatal"); }
    };
    CheckedFileTransferStore store = new CheckedFileTransferStore(4, support, 1);
    Fixture fixture = new Fixture(store, new byte[] {1}, 1, 100);
    UUID id = UUID.randomUUID();
    store.ready(fixture.key, fixture.target,
        CheckedFileTransferControl.ready(id, 1, fixture.hash), 2);
    assertThrows(AssertionError.class,
        () -> store.prepareChunk(fixture.key, fixture.target, id, 0, 1, new byte[] {1}, 3));
    assertEquals(0, store.size(3));
  }

  @Test
  void claimTokenRolloverSkipsActiveToken() throws Exception {
    CheckedFileTransferStore store = new CheckedFileTransferStore(4,
        new CheckedFileTransferStore.DigestSupport() {
          @Override public MessageDigest create() { return sha256(); }
          @Override public MessageDigest copy(MessageDigest source) {
            try { return (MessageDigest) source.clone(); }
            catch (CloneNotSupportedException exception) { throw new AssertionError(exception); }
          }
        }, Long.MAX_VALUE);
    Fixture first = new Fixture(store, new byte[] {1}, 1, 100);
    var firstClaim = store.prepareReady(first.key, first.target,
        CheckedFileTransferControl.ready(UUID.randomUUID(), 1, first.hash), 2).claim();
    Field sequence = CheckedFileTransferStore.class.getDeclaredField("nextClaimToken");
    sequence.setAccessible(true);
    sequence.setLong(store, Long.MAX_VALUE);
    var secondPending = pending(UUID.randomUUID(), UUID.randomUUID(), "b.jar", 3, 100,
        new byte[] {2});
    assertEquals(CheckedFileTransferStore.Result.CREATED, store.create(secondPending, 3));
    var secondClaim = store.prepareReady(secondPending.key(), secondPending.key().targetPlayerId(),
        CheckedFileTransferControl.ready(UUID.randomUUID(), 1, hash(new byte[] {2})), 4).claim();
    assertNotEquals(firstClaim.token(), secondClaim.token());
    assertEquals(1, secondClaim.token());
  }

  private static CheckedFileTransferStore.Pending pending(
      UUID target, UUID requester, String file, long created, long expires, byte[] bytes)
      throws Exception {
    var key = new CheckedFileTransferStore.Key(target, requester, ClientFileCheckType.MODS, file);
    return new CheckedFileTransferStore.Pending(
        key, "target", "requester", bytes.length, hash(bytes), created, expires);
  }

  private static String hash(byte[] bytes) throws Exception {
    return HexFormat.of().formatHex(sha256().digest(bytes));
  }

  private static MessageDigest sha256() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (Exception exception) { throw new AssertionError(exception); }
  }

  private static final class Fixture {
    final CheckedFileTransferStore store;
    final UUID target = UUID.randomUUID();
    final UUID requester = UUID.randomUUID();
    final CheckedFileTransferStore.Key key;
    final String hash;

    Fixture(CheckedFileTransferStore store, byte[] bytes, long created, long expires)
        throws Exception {
      this.store = store;
      hash = CheckedFileTransferStoreTest.hash(bytes);
      key = new CheckedFileTransferStore.Key(target, requester, ClientFileCheckType.MODS, "a.jar");
      assertEquals(CheckedFileTransferStore.Result.CREATED,
          store.create(new CheckedFileTransferStore.Pending(
              key, "target", "requester", bytes.length, hash, created, expires), created));
    }
  }
}
