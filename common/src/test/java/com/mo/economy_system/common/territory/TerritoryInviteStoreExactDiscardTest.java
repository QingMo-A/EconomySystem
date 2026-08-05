package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryInviteStoreExactDiscardTest {
  @Test
  void exactDiscardIsIdempotentAndLeavesUnrelatedInvites() {
    TerritoryInviteStore store = new TerritoryInviteStore();
    UUID target = UUID.randomUUID(), territory = UUID.randomUUID();
    TerritoryInvite exact = invite(territory, target);
    TerritoryInvite otherTarget = invite(territory, UUID.randomUUID());
    TerritoryInvite otherTerritory = invite(UUID.randomUUID(), target);
    store.put(exact, 0);
    store.put(otherTarget, 0);
    store.put(otherTerritory, 0);

    assertEquals(new TerritoryInviteStore.DiscardResult(1, 0), store.discardPending(target, territory, 1));
    assertEquals(new TerritoryInviteStore.DiscardResult(0, 0), store.discardPending(target, territory, 1));
    assertTrue(store.find(otherTarget.inviteId(), 1).isPresent());
    assertTrue(store.find(otherTerritory.inviteId(), 1).isPresent());
    assertTrue(store.consistent());
  }

  @Test
  void processingInviteIsSkippedWithoutChangingClaim() {
    TerritoryInviteStore store = new TerritoryInviteStore();
    TerritoryInvite invite = invite(UUID.randomUUID(), UUID.randomUUID());
    store.put(invite, 0);
    TerritoryInviteStore.Claim claim = store.claim(invite.inviteId(), invite.targetPlayerId(), 1).claim();

    assertEquals(new TerritoryInviteStore.DiscardResult(0, 1), store.discardPending(invite.targetPlayerId(), invite.territoryId(), 1));
    assertEquals(TerritoryInviteStore.ReleaseResult.RELEASED, store.release(claim));
    assertTrue(store.consistent());
  }

  @Test
  void tickRollbackClearsTheEpochBeforeDiscard() {
    TerritoryInviteStore store = new TerritoryInviteStore();
    TerritoryInvite invite = invite(UUID.randomUUID(), UUID.randomUUID());
    store.put(invite, 5);
    assertEquals(new TerritoryInviteStore.DiscardResult(0, 0), store.discardPending(invite.targetPlayerId(), invite.territoryId(), 0));
    assertEquals(0, store.size(0));
    assertTrue(store.consistent());
  }

  @Test
  void missingKeyFailsClosed() throws Exception {
    TerritoryInviteStore store = new TerritoryInviteStore();
    TerritoryInvite invite = invite(UUID.randomUUID(), UUID.randomUUID());
    store.put(invite, 0);
    keys(store).remove(new TerritoryInviteStore.Key(invite.targetPlayerId(), invite.territoryId()));
    assertThrows(IllegalStateException.class, () -> store.discardPending(invite.targetPlayerId(), invite.territoryId(), 1));
  }

  @Test
  void danglingAndMismatchedKeysFailClosed() throws Exception {
    TerritoryInviteStore dangling = new TerritoryInviteStore();
    UUID target = UUID.randomUUID(), territory = UUID.randomUUID();
    keys(dangling).put(new TerritoryInviteStore.Key(target, territory), UUID.randomUUID());
    assertThrows(IllegalStateException.class, () -> dangling.discardPending(target, territory, 0));

    TerritoryInviteStore mismatched = new TerritoryInviteStore();
    TerritoryInvite first = invite(territory, target);
    TerritoryInvite second = invite(UUID.randomUUID(), UUID.randomUUID());
    mismatched.put(first, 0);
    mismatched.put(second, 0);
    keys(mismatched).put(new TerritoryInviteStore.Key(target, territory), second.inviteId());
    assertThrows(IllegalStateException.class, () -> mismatched.discardPending(target, territory, 1));
  }

  @SuppressWarnings("unchecked")
  private static Map<TerritoryInviteStore.Key, UUID> keys(TerritoryInviteStore store) throws Exception {
    Field field = TerritoryInviteStore.class.getDeclaredField("keys");
    field.setAccessible(true);
    return (Map<TerritoryInviteStore.Key, UUID>) field.get(store);
  }

  private static TerritoryInvite invite(UUID territory, UUID target) {
    UUID owner = UUID.randomUUID();
    return new TerritoryInvite(UUID.randomUUID(), territory, owner, owner, target, "land", "owner", "target", 0, 20);
  }
}
