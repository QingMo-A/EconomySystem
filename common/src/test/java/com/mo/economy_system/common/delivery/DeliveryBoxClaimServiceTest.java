package com.mo.economy_system.common.delivery;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.market.InventoryInsertionResult;
import com.mo.economy_system.common.market.TransactionalInventory;
import com.mo.economy_system.common.network.DeliveryBoxClaimMessage;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeliveryBoxClaimServiceTest {
  @Test
  void successfulClaimInsertsThenRemovesAndRejectsReplay() {
    Fixture fixture = new Fixture();
    AtomicBoolean inserted = new AtomicBoolean();
    fixture.inventory.insert = (template, quantity) -> {
      assertEquals(4, quantity);
      inserted.set(true);
      return InventoryInsertionResult.success(() -> { inserted.set(false); return true; });
    };
    assertEquals(DeliveryBoxClaimResult.SUCCESS, fixture.claim());
    assertTrue(inserted.get());
    assertTrue(fixture.ledger.list(fixture.owner).isEmpty());
    assertEquals(DeliveryBoxClaimResult.NOT_FOUND, fixture.claim());
  }

  @Test
  void capacityAndInsertFailureLeaveEntryUntouched() {
    Fixture full = new Fixture();
    full.inventory.accepts = false;
    assertEquals(DeliveryBoxClaimResult.INVENTORY_FULL, full.claim());
    assertEquals(1, full.ledger.list(full.owner).size());

    Fixture failed = new Fixture();
    failed.inventory.insert = (template, quantity) -> InventoryInsertionResult.failure(true);
    assertEquals(DeliveryBoxClaimResult.INVENTORY_FAILED, failed.claim());
    assertEquals(1, failed.ledger.list(failed.owner).size());

    Fixture unknown = new Fixture();
    unknown.inventory.insert = (template, quantity) -> InventoryInsertionResult.failure(false);
    assertEquals(DeliveryBoxClaimResult.ROLLBACK_FAILED, unknown.claim());
    assertEquals(1, unknown.ledger.list(unknown.owner).size());
  }

  @Test
  void persistenceFailureRollsBackInventoryAndKeepsEntry() {
    Fixture fixture = new Fixture();
    AtomicBoolean inserted = new AtomicBoolean();
    fixture.inventory.insert = (template, quantity) -> {
      inserted.set(true);
      return InventoryInsertionResult.success(() -> { inserted.set(false); return true; });
    };
    fixture.dirtyFails = true;
    assertEquals(DeliveryBoxClaimResult.PERSIST_FAILED, fixture.claim());
    assertFalse(inserted.get());
    assertEquals(1, fixture.ledger.list(fixture.owner).size());
  }

  @Test
  void failedCompensationIsExplicitAndReservationIsReleased() {
    Fixture fixture = new Fixture();
    fixture.inventory.insert =
        (template, quantity) -> InventoryInsertionResult.success(() -> false);
    fixture.dirtyFails = true;
    assertEquals(DeliveryBoxClaimResult.ROLLBACK_FAILED, fixture.claim());
    assertNotNull(fixture.ledger.reserve(fixture.owner, fixture.entry.entryId()));
  }

  @Test
  void restoreAndRepositoryExceptionsAreBounded() {
    Fixture restore = new Fixture();
    restore.materializerFails = true;
    assertEquals(DeliveryBoxClaimResult.ITEM_RESTORE_FAILED, restore.claim());
    assertEquals(1, restore.reports.get());

    DeliveryBoxRepository repository = new DeliveryBoxRepository() {
      public java.util.List<DeliveryBoxEntrySnapshot> list(UUID owner) { return java.util.List.of(); }
      public Reservation reserve(UUID owner, UUID entry) { throw new IllegalStateException("boom"); }
    };
    TestInventory inventory = new TestInventory(restore.owner);
    var context = new DeliveryBoxClaimService.Context(
        restore.owner,
        repository,
        entry -> new Object(),
        inventory,
        () -> {},
        DeliveryBoxClaimService.FailureReporter.noop());
    assertEquals(
        DeliveryBoxClaimResult.STATE_UNKNOWN,
        DeliveryBoxClaimService.claim(
            new DeliveryBoxClaimMessage(UUID.randomUUID(), 0), context));
  }

  private static final class Fixture {
    final UUID owner = UUID.randomUUID();
    final DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 4);
    final DeliveryBoxLedger ledger = new DeliveryBoxLedger();
    final TestInventory inventory = new TestInventory(owner);
    final AtomicInteger reports = new AtomicInteger();
    boolean dirtyFails;
    boolean materializerFails;

    Fixture() {
      ledger.add(owner, entry, () -> {});
    }

    DeliveryBoxClaimResult claim() {
      return DeliveryBoxClaimService.claim(
          new DeliveryBoxClaimMessage(entry.entryId(), 0),
          new DeliveryBoxClaimService.Context(
              owner,
              ledger,
              value -> {
                if (materializerFails) throw new IllegalStateException("restore");
                return value.item();
              },
              inventory,
              () -> {
                if (dirtyFails) throw new IllegalStateException("dirty");
              },
              (ownerId, entryId, stage, result, error) -> reports.incrementAndGet()));
    }
  }

  private static final class TestInventory implements TransactionalInventory {
    final UUID owner;
    boolean accepts = true;
    Inserter insert = (template, quantity) -> InventoryInsertionResult.success(() -> true);

    TestInventory(UUID owner) {
      this.owner = owner;
    }

    public UUID ownerId() { return owner; }
    public boolean canAccept(Object template, int quantity) { return accepts; }
    public InventoryInsertionResult insert(Object template, int quantity) {
      return insert.insert(template, quantity);
    }
  }

  @FunctionalInterface
  private interface Inserter {
    InventoryInsertionResult insert(Object template, int quantity);
  }
}
