package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TerritoryRemovalServiceTest {
  private static final UUID PLAYER = UUID.randomUUID();
  private static final UUID LAND = UUID.randomUUID();

  private static TerritoryRemovalService.RemovedTerritory removed() {
    return new TerritoryRemovalService.RemovedTerritory(LAND, PLAYER, "Home");
  }

  private static TerritoryRemovalService service(
      TerritoryRemovalService.Repository repository,
      TerritoryRemovalRateLimiter limiter,
      AtomicInteger cleanup,
      AtomicReference<String> stage) {
    return new TerritoryRemovalService(
        repository,
        limiter,
        (snapshot, tick) -> cleanup.incrementAndGet(),
        (snapshot, tick) -> cleanup.incrementAndGet(),
        (name, player, id, error) -> stage.set(name));
  }

  @Test
  void successUsesAuthenticatedOwnerAndCleansAfterRemoval() {
    AtomicReference<UUID> owner = new AtomicReference<>();
    AtomicInteger cleanup = new AtomicInteger();
    var service =
        service(
            (id, expectedOwner) -> {
              owner.set(expectedOwner);
              return new TerritoryRemovalService.RepositoryOutcome(
                  TerritoryRemovalService.RepositoryResult.REMOVED, removed());
            },
            new TerritoryRemovalRateLimiter(),
            cleanup,
            new AtomicReference<>());
    assertEquals(TerritoryRemovalService.Result.SUCCESS, service.remove(PLAYER, LAND, 0).result());
    assertEquals(PLAYER, owner.get());
    assertEquals(2, cleanup.get());
  }

  @Test
  void mapsRepositoryResultsAndLimitsAttempts() {
    Map<TerritoryRemovalService.RepositoryResult, TerritoryRemovalService.Result> expected =
        Map.of(
            TerritoryRemovalService.RepositoryResult.NOT_FOUND,
            TerritoryRemovalService.Result.TERRITORY_NOT_FOUND,
            TerritoryRemovalService.RepositoryResult.OWNER_MISMATCH,
            TerritoryRemovalService.Result.NO_PERMISSION,
            TerritoryRemovalService.RepositoryResult.PERSIST_FAILED,
            TerritoryRemovalService.Result.PERSIST_FAILED,
            TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
            TerritoryRemovalService.Result.STATE_UNKNOWN);
    for (var entry : expected.entrySet()) {
      AtomicInteger cleanup = new AtomicInteger();
      var service =
          service(
              (id, owner) -> repositoryOutcome(entry.getKey()),
              new TerritoryRemovalRateLimiter(),
              cleanup,
              new AtomicReference<>());
      assertEquals(entry.getValue(), service.remove(PLAYER, LAND, 0).result());
      assertEquals(
          TerritoryRemovalService.Result.RATE_LIMITED,
          service.remove(PLAYER, UUID.randomUUID(), 19).result());
      assertEquals(0, cleanup.get());
    }
  }

  @Test
  void nullExceptionAndContractViolationsAreUnknown() {
    AtomicReference<String> stage = new AtomicReference<>();
    var nullService =
        service((id, owner) -> null, new TerritoryRemovalRateLimiter(), new AtomicInteger(), stage);
    assertEquals(
        TerritoryRemovalService.Result.STATE_UNKNOWN, nullService.remove(PLAYER, LAND, 0).result());
    assertEquals("repository", stage.get());
    var mismatch =
        service(
            (id, owner) ->
                new TerritoryRemovalService.RepositoryOutcome(
                    TerritoryRemovalService.RepositoryResult.REMOVED,
                    new TerritoryRemovalService.RemovedTerritory(UUID.randomUUID(), PLAYER, "x")),
            new TerritoryRemovalRateLimiter(),
            new AtomicInteger(),
            stage);
    assertEquals(
        TerritoryRemovalService.Result.STATE_UNKNOWN, mismatch.remove(PLAYER, LAND, 0).result());
    assertEquals("repository-contract", stage.get());
    var thrown =
        service(
            (id, owner) -> {
              throw new IllegalStateException();
            },
            new TerritoryRemovalRateLimiter(),
            new AtomicInteger(),
            stage);
    assertEquals(
        TerritoryRemovalService.Result.STATE_UNKNOWN, thrown.remove(PLAYER, LAND, 0).result());
    assertThrows(
        AssertionError.class,
        () ->
            service(
                    (id, owner) -> {
                      throw new AssertionError();
                    },
                    new TerritoryRemovalRateLimiter(),
                    new AtomicInteger(),
                    stage)
                .remove(PLAYER, LAND, 0));
  }

  @Test
  void cleanupAndDiagnosticsFailuresDoNotChangeSuccess() {
    var service =
        new TerritoryRemovalService(
            (id, owner) ->
                new TerritoryRemovalService.RepositoryOutcome(
                    TerritoryRemovalService.RepositoryResult.REMOVED, removed()),
            new TerritoryRemovalRateLimiter(),
            (snapshot, tick) -> {
              throw new IllegalStateException();
            },
            (snapshot, tick) -> {
              throw new IllegalStateException();
            },
            (stage, player, id, error) -> {
              throw new IllegalStateException();
            });
    assertEquals(TerritoryRemovalService.Result.SUCCESS, service.remove(PLAYER, LAND, 0).result());
  }

  @Test
  void limiterBoundaryCapacityAndEpochReset() {
    var limiter = new TerritoryRemovalRateLimiter(1);
    assertTrue(limiter.acquire(PLAYER, 0));
    assertFalse(limiter.acquire(PLAYER, 19));
    assertTrue(limiter.acquire(PLAYER, 20));
    assertTrue(limiter.acquire(UUID.randomUUID(), 21));
    assertTrue(limiter.acquire(PLAYER, 22));
    assertTrue(limiter.acquire(PLAYER, 1));
    assertThrows(IllegalArgumentException.class, () -> limiter.acquire(PLAYER, -1));
  }

  private static TerritoryRemovalService.RepositoryOutcome repositoryOutcome(
      TerritoryRemovalService.RepositoryResult result) {
    if (result == TerritoryRemovalService.RepositoryResult.PERSIST_FAILED)
      return new TerritoryRemovalService.RepositoryOutcome(
          result,
          null,
          TerritoryRemovalService.RepositoryFailureKind.PERSISTENCE,
          new IllegalStateException("persistence"));
    if (result == TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN)
      return new TerritoryRemovalService.RepositoryOutcome(
          result,
          null,
          TerritoryRemovalService.RepositoryFailureKind.UNKNOWN,
          new IllegalStateException("unknown"));
    return new TerritoryRemovalService.RepositoryOutcome(result, null);
  }
}
