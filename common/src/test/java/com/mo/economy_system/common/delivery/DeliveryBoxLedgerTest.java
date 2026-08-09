package com.mo.economy_system.common.delivery;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DeliveryBoxLedgerTest {
  @Test
  void snapshotsAreImmutableAndReservationsAreExclusive() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 3);
    DeliveryBoxLedger ledger = new DeliveryBoxLedger();
    ledger.add(owner, entry, () -> {});
    assertThrows(UnsupportedOperationException.class, () -> ledger.list(owner).add(entry));
    Map<UUID, List<DeliveryBoxEntrySnapshot>> snapshot = ledger.snapshot();
    assertThrows(UnsupportedOperationException.class, () -> snapshot.put(owner, List.of()));
    DeliveryBoxRepository.Reservation first = ledger.reserve(owner, entry.entryId());
    assertNotNull(first);
    assertNull(ledger.reserve(owner, entry.entryId()));
    first.release();
    assertNotNull(ledger.reserve(owner, entry.entryId()));
  }

  @Test
  void commitRemovesOnceAndReplayCannotClaimAgain() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    DeliveryBoxLedger ledger = new DeliveryBoxLedger();
    ledger.add(owner, entry, () -> {});
    DeliveryBoxRepository.Reservation reservation = ledger.reserve(owner, entry.entryId());
    assertEquals(
        DeliveryBoxRepository.CommitResult.REMOVED, reservation.commit(() -> {}));
    assertEquals(
        DeliveryBoxRepository.CommitResult.STATE_UNKNOWN, reservation.commit(() -> {}));
    assertNull(ledger.reserve(owner, entry.entryId()));
    assertTrue(ledger.list(owner).isEmpty());
  }

  @Test
  void dirtyFailureRestoresOriginalOrderAndReleasesReservation() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot first = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    DeliveryBoxEntrySnapshot second = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    DeliveryBoxLedger ledger = new DeliveryBoxLedger();
    ledger.add(owner, first, () -> {});
    ledger.add(owner, second, () -> {});
    DeliveryBoxRepository.Reservation reservation = ledger.reserve(owner, first.entryId());
    assertEquals(
        DeliveryBoxRepository.CommitResult.PERSIST_FAILED,
        reservation.commit(() -> { throw new IllegalStateException("dirty"); }));
    assertEquals(List.of(first, second), ledger.list(owner));
    assertNotNull(ledger.reserve(owner, first.entryId()));
  }

  @Test
  void addDirtyFailureAndRestoreRejectPartialOrDuplicateState() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    DeliveryBoxLedger ledger = new DeliveryBoxLedger();
    assertThrows(
        IllegalStateException.class,
        () -> ledger.add(owner, entry, () -> { throw new IllegalStateException("dirty"); }));
    assertTrue(ledger.list(owner).isEmpty());

    assertThrows(
        IllegalArgumentException.class,
        () -> ledger.restore(Map.of(owner, List.of(entry), UUID.randomUUID(), List.of(entry))));
    assertTrue(ledger.snapshot().isEmpty());
  }

  @Test
  void batchAddIsAtomicAndMarksPersistenceOnce() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot first = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    DeliveryBoxEntrySnapshot second = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 2);
    DeliveryBoxLedger ledger = new DeliveryBoxLedger();
    AtomicInteger dirty = new AtomicInteger();

    ledger.addAll(owner, List.of(first, second), dirty::incrementAndGet);

    assertEquals(List.of(first, second), ledger.list(owner));
    assertEquals(1, dirty.get());

    DeliveryBoxLedger failing = new DeliveryBoxLedger();
    assertThrows(
        IllegalStateException.class,
        () -> failing.addAll(owner, List.of(first, second), () -> {
          throw new IllegalStateException("dirty");
        }));
    assertTrue(failing.list(owner).isEmpty());
  }
}
