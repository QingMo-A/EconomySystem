package com.mo.economy_system.common.check;

import static org.junit.jupiter.api.Assertions.*;

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
}
