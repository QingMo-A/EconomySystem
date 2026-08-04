package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryRemovalCleanupSnapshotTest {
  private static final UUID PLAYER = UUID.randomUUID();
  private static final UUID TERRITORY = UUID.randomUUID();

  @Test
  void cleanupReceivesServerSnapshotInOrder() {
    TerritoryRemovalService.RemovedTerritory removed =
        new TerritoryRemovalService.RemovedTerritory(TERRITORY, PLAYER, "Server Home");
    List<String> calls = new ArrayList<>();
    TerritoryRemovalService service =
        new TerritoryRemovalService(
            (id, owner) ->
                new TerritoryRemovalService.RepositoryOutcome(
                    TerritoryRemovalService.RepositoryResult.REMOVED, removed),
            new TerritoryRemovalRateLimiter(),
            (snapshot, tick) -> calls.add("invite:" + snapshot.territoryName()),
            (snapshot, tick) -> calls.add("resize:" + snapshot.territoryName()),
            (stage, player, territory, error) -> fail(stage));

    assertEquals(
        TerritoryRemovalService.Result.SUCCESS, service.remove(PLAYER, TERRITORY, 10).result());
    assertEquals(List.of("invite:Server Home", "resize:Server Home"), calls);
  }

  @Test
  void inviteFailureStillRunsResizeAndErrorsEscape() {
    List<String> calls = new ArrayList<>();
    TerritoryRemovalService service =
        successful(
            (snapshot, tick) -> {
              calls.add("invite");
              throw new IllegalStateException("invite");
            },
            (snapshot, tick) -> calls.add("resize"));
    assertEquals(
        TerritoryRemovalService.Result.SUCCESS, service.remove(PLAYER, TERRITORY, 0).result());
    assertEquals(List.of("invite", "resize"), calls);

    TerritoryRemovalService fatal =
        successful(
            (snapshot, tick) -> {
              throw new AssertionError("fatal");
            },
            (snapshot, tick) -> fail("must not continue"));
    assertThrows(AssertionError.class, () -> fatal.remove(PLAYER, TERRITORY, 0));
  }

  @Test
  void repositoryFailureIsReportedWithSpecificStage() {
    List<String> stages = new ArrayList<>();
    RuntimeException failure = new IllegalStateException("dirty");
    TerritoryRemovalService service =
        new TerritoryRemovalService(
            (id, owner) ->
                new TerritoryRemovalService.RepositoryOutcome(
                    TerritoryRemovalService.RepositoryResult.PERSIST_FAILED,
                    null,
                    TerritoryRemovalService.RepositoryFailureKind.PERSISTENCE,
                    failure),
            new TerritoryRemovalRateLimiter(),
            (snapshot, tick) -> {},
            (snapshot, tick) -> {},
            (stage, player, territory, error) -> stages.add(stage));
    assertEquals(
        TerritoryRemovalService.Result.PERSIST_FAILED,
        service.remove(PLAYER, TERRITORY, 0).result());
    assertEquals(List.of("repository-persist-failed"), stages);
  }

  @Test
  void explicitFailureKindsSelectStableStages() {
    for (var entry :
        java.util.Map.of(
                TerritoryRemovalService.RepositoryFailureKind.INTEGRITY,
                "repository-integrity",
                TerritoryRemovalService.RepositoryFailureKind.UNKNOWN,
                "repository-state-unknown")
            .entrySet()) {
      List<String> stages = new ArrayList<>();
      TerritoryRemovalService service =
          new TerritoryRemovalService(
              (id, owner) ->
                  new TerritoryRemovalService.RepositoryOutcome(
                      TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
                      null,
                      entry.getKey(),
                      new IllegalStateException("message is irrelevant")),
              new TerritoryRemovalRateLimiter(),
              (snapshot, tick) -> {},
              (snapshot, tick) -> {},
              (stage, player, territory, error) -> stages.add(stage));
      assertEquals(
          TerritoryRemovalService.Result.STATE_UNKNOWN,
          service.remove(PLAYER, TERRITORY, 0).result());
      assertEquals(List.of(entry.getValue()), stages);
    }
  }

  @Test
  void invalidFailureKindCombinationsAndJvmErrorsAreRejected() {
    RuntimeException failure = new IllegalStateException("failure");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TerritoryRemovalService.RepositoryOutcome(
                TerritoryRemovalService.RepositoryResult.PERSIST_FAILED,
                null,
                TerritoryRemovalService.RepositoryFailureKind.UNKNOWN,
                failure));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TerritoryRemovalService.RepositoryOutcome(
                TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
                null,
                TerritoryRemovalService.RepositoryFailureKind.PERSISTENCE,
                failure));
    assertThrows(
        AssertionError.class,
        () ->
            new TerritoryRemovalService.RepositoryOutcome(
                TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
                null,
                TerritoryRemovalService.RepositoryFailureKind.UNKNOWN,
                new AssertionError("fatal")));
  }

  private static TerritoryRemovalService successful(
      TerritoryRemovalService.Cleanup invite, TerritoryRemovalService.Cleanup resize) {
    TerritoryRemovalService.RemovedTerritory removed =
        new TerritoryRemovalService.RemovedTerritory(TERRITORY, PLAYER, "Home");
    return new TerritoryRemovalService(
        (id, owner) ->
            new TerritoryRemovalService.RepositoryOutcome(
                TerritoryRemovalService.RepositoryResult.REMOVED, removed),
        new TerritoryRemovalRateLimiter(),
        invite,
        resize,
        (stage, player, territory, error) -> {});
  }
}
