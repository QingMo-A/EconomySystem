package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritoryPresenceService.Location;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class TerritoryPresenceServiceTest {
  private static final Location OVERWORLD = new Location("minecraft:overworld", 4, 8);

  @Test
  void tracksTransitionsAndDetectsDimensionChangesAtTheSameCoordinates() {
    TerritoryPresenceService service = new TerritoryPresenceService();
    Owned territory = TerritoryManagementTestFixtures.owned();
    UUID player = UUID.randomUUID();
    AtomicInteger lookups = new AtomicInteger();

    var entered = service.tick(player, 0, OVERWORLD, location -> {
      lookups.incrementAndGet();
      return Optional.of(territory);
    });
    assertEquals(territory, entered.entered().orElseThrow());
    assertEquals(territory, entered.current().orElseThrow());
    assertFalse(entered.applyBuffs());

    service.tick(player, 1, OVERWORLD, location -> {
      lookups.incrementAndGet();
      return Optional.of(territory);
    });
    assertEquals(1, lookups.get());

    var exited = service.tick(
        player,
        TerritoryRuntimePolicy.MOVEMENT_CHECK_INTERVAL_TICKS,
        new Location("minecraft:the_nether", 4, 8),
        location -> Optional.empty());
    assertEquals(territory, exited.exited().orElseThrow());
    assertTrue(exited.current().isEmpty());
  }

  @Test
  void refreshesBuffSnapshotsOnTheSharedTickSchedule() {
    TerritoryPresenceService service = new TerritoryPresenceService();
    Owned territory = TerritoryTestFixtures.owned();
    UUID player = UUID.randomUUID();

    service.tick(player, 0, OVERWORLD, location -> Optional.of(territory));
    var before = service.tick(player, 99, OVERWORLD, location -> Optional.of(territory));
    assertFalse(before.applyBuffs());
    var due = service.tick(player, 100, OVERWORLD, location -> Optional.of(territory));
    assertTrue(due.applyBuffs());
    assertEquals(territory, due.current().orElseThrow());
    assertFalse(service.tick(player, 199, OVERWORLD, location -> Optional.of(territory)).applyBuffs());
    assertTrue(service.tick(player, 200, OVERWORLD, location -> Optional.of(territory)).applyBuffs());
  }

  @Test
  void lookupFailurePreservesKnownStateAndCleanupIsExplicit() {
    TerritoryPresenceService service = new TerritoryPresenceService();
    Owned territory = TerritoryManagementTestFixtures.owned();
    UUID player = UUID.randomUUID();
    service.tick(player, 0, OVERWORLD, location -> Optional.of(territory));

    var failed = service.tick(player, 100, OVERWORLD, location -> {
      throw new IllegalStateException("storage unavailable");
    });
    assertTrue(failed.lookupFailed());
    assertFalse(failed.applyBuffs());
    assertEquals(territory, failed.current().orElseThrow());
    assertEquals(1, service.trackedPlayers());
    assertTrue(service.clear(player));
    assertEquals(0, service.trackedPlayers());
  }

  @Test
  void clockRegressionStartsAVisibleFreshSession() {
    TerritoryPresenceService service = new TerritoryPresenceService();
    Owned territory = TerritoryManagementTestFixtures.owned();
    UUID player = UUID.randomUUID();
    assertTrue(service.tick(player, 100, OVERWORLD, location -> Optional.of(territory)).applyBuffs());

    var reset = service.tick(player, 1, OVERWORLD, location -> Optional.of(territory));
    assertEquals(territory, reset.entered().orElseThrow());
    assertFalse(reset.applyBuffs());
  }
}
