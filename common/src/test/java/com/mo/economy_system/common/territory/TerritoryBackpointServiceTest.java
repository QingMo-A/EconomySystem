package com.mo.economy_system.common.territory;

import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.OWNER;
import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.TERRITORY;
import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.owned;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TerritoryBackpointServiceTest {
  @Test
  void ownerInsideTerritoryUsesCompareAndSetRepository() {
    FakeRepository repository = new FakeRepository(owned());
    Position next = new Position(4, 70, 4);

    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryBackpointService.execute(
            OWNER, "minecraft:overworld", next, repository, TerritoryBackpointService.Diagnostics.noop()));
    assertEquals(TERRITORY, repository.territoryId.get());
    assertEquals(Optional.of(new Position(1, 65, 1)), repository.expected.get());
    assertEquals(next, repository.next.get());
    assertEquals(1, repository.writes.get());
  }

  @Test
  void missingOrNonOwnerCannotWrite() {
    FakeRepository missing = new FakeRepository(null);
    assertEquals(
        TerritoryManagementResult.NOT_FOUND,
        TerritoryBackpointService.execute(
            OWNER, "minecraft:overworld", new Position(2, 70, 2), missing,
            TerritoryBackpointService.Diagnostics.noop()));
    FakeRepository other = new FakeRepository(owned());
    assertEquals(
        TerritoryManagementResult.NOT_OWNER,
        TerritoryBackpointService.execute(
            UUID.randomUUID(), "minecraft:overworld", new Position(2, 70, 2), other,
            TerritoryBackpointService.Diagnostics.noop()));
    assertEquals(0, other.writes.get());
  }

  @Test
  void persistenceAndLookupFailuresAreVisible() {
    FakeRepository repository = new FakeRepository(owned());
    repository.result = TerritoryBackpointService.RepositoryResult.PERSIST_FAILED;
    assertEquals(
        TerritoryManagementResult.PERSIST_FAILED,
        TerritoryBackpointService.execute(
            OWNER, "minecraft:overworld", new Position(2, 70, 2), repository,
            TerritoryBackpointService.Diagnostics.noop()));
    repository.throwOnLookup = true;
    assertEquals(
        TerritoryManagementResult.STATE_UNKNOWN,
        TerritoryBackpointService.execute(
            OWNER, "minecraft:overworld", new Position(2, 70, 2), repository,
            TerritoryBackpointService.Diagnostics.noop()));
  }

  private static final class FakeRepository implements TerritoryBackpointService.Repository {
    final Owned value;
    final AtomicInteger writes = new AtomicInteger();
    final AtomicReference<UUID> territoryId = new AtomicReference<>();
    final AtomicReference<Optional<Position>> expected = new AtomicReference<>();
    final AtomicReference<Position> next = new AtomicReference<>();
    TerritoryBackpointService.RepositoryResult result =
        TerritoryBackpointService.RepositoryResult.UPDATED;
    boolean throwOnLookup;

    FakeRepository(Owned value) { this.value = value; }

    public Owned findAt(String dimensionId, int x, int z) {
      if (throwOnLookup) throw new IllegalStateException("lookup");
      return value;
    }

    public TerritoryBackpointService.RepositoryResult setBackpoint(
        UUID id, UUID owner, Optional<Position> old, Position point) {
      writes.incrementAndGet();
      territoryId.set(id);
      expected.set(old);
      next.set(point);
      return result;
    }
  }
}
