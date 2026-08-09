package com.mo.economy_system.common.tpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TpaRequestStoreTest {
  private static final UUID SENDER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TARGET = UUID.fromString("00000000-0000-0000-0000-000000000002");
  private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-000000000003");

  @Test
  void requestStoresBothIdentitiesAndExpiresAtExactTick() {
    TpaRequestStore store = new TpaRequestStore(2, 10);

    assertEquals(TpaRequestStore.CreateResult.CREATED, store.create(SENDER, TARGET, 5));
    TpaRequest request = store.claim(TARGET, 14).orElseThrow().request();
    assertEquals(SENDER, request.senderId());
    assertEquals(TARGET, request.targetId());
    assertEquals(15, request.expiresTick());
    assertTrue(store.claim(TARGET, 15).isEmpty());
  }

  @Test
  void targetAndSenderAreBothBoundedUntilClaimed() {
    TpaRequestStore store = new TpaRequestStore();

    assertEquals(TpaRequestStore.CreateResult.CREATED, store.create(SENDER, TARGET, 0));
    assertEquals(TpaRequestStore.CreateResult.TARGET_BUSY, store.create(OTHER, TARGET, 1));
    assertEquals(TpaRequestStore.CreateResult.SENDER_BUSY, store.create(SENDER, OTHER, 1));
    TpaRequestStore.Claim claim = store.claim(TARGET, 1).orElseThrow();
    assertTrue(store.release(claim, 1));
    assertEquals(TpaRequestStore.CreateResult.TARGET_BUSY, store.create(OTHER, TARGET, 2));
    assertTrue(store.claim(TARGET, 2).isPresent());
    assertEquals(TpaRequestStore.CreateResult.CREATED, store.create(SENDER, OTHER, 2));
  }

  @Test
  void capacityAndInvalidTickFailClosed() {
    TpaRequestStore store = new TpaRequestStore(1, 10);
    assertEquals(TpaRequestStore.CreateResult.INVALID_TICK, store.create(SENDER, TARGET, -1));
    assertEquals(TpaRequestStore.CreateResult.CREATED, store.create(SENDER, TARGET, 0));
    assertEquals(TpaRequestStore.CreateResult.CAPACITY, store.create(OTHER, UUID.randomUUID(), 0));
  }
}
