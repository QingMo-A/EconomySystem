package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientFileCheckRequestStoreTest {
  private static ClientFileCheckRequestStore.Pending pending(
      UUID target, UUID requester, ClientFileCheckType type, long tick) {
    return new ClientFileCheckRequestStore.Pending(
        target, "Target", requester, "Requester", type, tick, tick + 1200);
  }

  @Test
  void exactOneShotClaimReleaseAndComplete() {
    var store = new ClientFileCheckRequestStore();
    UUID target = UUID.randomUUID(), requester = UUID.randomUUID();
    var value = pending(target, requester, ClientFileCheckType.MODS, 10);
    assertEquals(ClientFileCheckRequestStore.PutResult.CREATED, store.put(value, 10));
    assertEquals(ClientFileCheckRequestStore.PutResult.ALREADY_PENDING, store.put(value, 10));
    var claim = store.claim(value.key(), 11);
    assertEquals(ClientFileCheckRequestStore.ClaimStatus.CLAIMED, claim.status());
    assertEquals(
        ClientFileCheckRequestStore.ClaimStatus.BUSY, store.claim(value.key(), 11).status());
    assertTrue(store.release(claim.claim()));
    var second = store.claim(value.key(), 12);
    assertTrue(store.complete(second.claim()));
    assertEquals(
        ClientFileCheckRequestStore.ClaimStatus.NOT_FOUND, store.claim(value.key(), 12).status());
  }

  @Test
  void enforcesCapacityCooldownExpiryAndRollback() {
    var store = new ClientFileCheckRequestStore(1);
    UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
    assertEquals(
        ClientFileCheckRequestStore.PutResult.CREATED,
        store.put(pending(a, b, ClientFileCheckType.MODS, 100), 100));
    assertEquals(
        ClientFileCheckRequestStore.PutResult.RATE_LIMITED,
        store.put(pending(a, c, ClientFileCheckType.MODS, 101), 101));
    assertEquals(
        ClientFileCheckRequestStore.PutResult.FULL,
        store.put(pending(c, a, ClientFileCheckType.MODS, 101), 101));
    assertEquals(0, store.size(1300));
    assertEquals(
        ClientFileCheckRequestStore.PutResult.CREATED,
        store.put(pending(c, a, ClientFileCheckType.MODS, 1300), 1300));
    assertEquals(0, store.size(1));
  }

  @Test
  void weakRegistrySeparatesServers() {
    var registry = new ClientFileCheckRequestStoreRegistry<Object>();
    assertNotSame(registry.get(new Object()), registry.get(new Object()));
  }

  @Test
  void processingExpiryRemovesTokenAndInvalidatesClaimWithoutTouchingOthers() {
    var store = new ClientFileCheckRequestStore();
    UUID target = UUID.randomUUID(), requester = UUID.randomUUID();
    var expiring =
        new ClientFileCheckRequestStore.Pending(
            target, "Target", requester, "Requester", ClientFileCheckType.MODS, 1, 3);
    var other = pending(UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS, 1);
    assertEquals(ClientFileCheckRequestStore.PutResult.CREATED, store.put(expiring, 1));
    assertEquals(ClientFileCheckRequestStore.PutResult.CREATED, store.put(other, 1));
    var claim = store.claim(expiring.key(), 2).claim();
    assertEquals(1, store.size(3));
    assertFalse(store.complete(claim));
    assertFalse(store.release(claim));
    assertTrue(store.find(other.key(), 3).isPresent());
  }

  @Test
  void rollbackCreatedRemovesExactPendingAndItsCooldowns() {
    var store = new ClientFileCheckRequestStore();
    UUID target = UUID.randomUUID(), requester = UUID.randomUUID();
    var value = pending(target, requester, ClientFileCheckType.MODS, 10);
    assertEquals(ClientFileCheckRequestStore.PutResult.CREATED, store.put(value, 10));
    assertTrue(store.rollbackCreated(value, 10));
    assertEquals(
        ClientFileCheckRequestStore.PutResult.CREATED,
        store.put(pending(target, requester, ClientFileCheckType.SHADERPACKS, 11), 11));
  }

  @Test
  void rollbackRejectsProcessingAndMismatchedPending() {
    var store = new ClientFileCheckRequestStore();
    var value = pending(UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS, 10);
    store.put(value, 10);
    var mismatch =
        new ClientFileCheckRequestStore.Pending(
            value.targetPlayerId(),
            "Other",
            value.requesterPlayerId(),
            value.requesterPlayerName(),
            value.checkType(),
            10,
            1210);
    assertFalse(store.rollbackCreated(mismatch, 10));
    store.claim(value.key(), 11);
    assertFalse(store.rollbackCreated(value, 11));
  }

  @Test
  void claimResultInvariantRejectsImpossibleStates() {
    var value = pending(UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS, 1);
    var claim = new ClientFileCheckRequestStore.Claim(1, value);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ClientFileCheckRequestStore.ClaimResult(
                ClientFileCheckRequestStore.ClaimStatus.CLAIMED, null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ClientFileCheckRequestStore.ClaimResult(
                ClientFileCheckRequestStore.ClaimStatus.BUSY, claim));
  }

  @Test
  void tokenOverflowStaysPositiveAndTickRollbackClearsAllState() throws Exception {
    var store = new ClientFileCheckRequestStore();
    Field field = ClientFileCheckRequestStore.class.getDeclaredField("nextToken");
    field.setAccessible(true);
    field.setLong(store, Long.MAX_VALUE);
    var first = pending(UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS, 10);
    var second = pending(UUID.randomUUID(), UUID.randomUUID(), ClientFileCheckType.MODS, 10);
    store.put(first, 10);
    store.put(second, 10);
    assertEquals(Long.MAX_VALUE, store.claim(first.key(), 10).claim().token());
    assertEquals(1, store.claim(second.key(), 10).claim().token());
    assertEquals(0, store.size(1));
    assertEquals(
        ClientFileCheckRequestStore.PutResult.CREATED,
        store.put(
            pending(first.targetPlayerId(), first.requesterPlayerId(), ClientFileCheckType.MODS, 1),
            1));
  }
}
