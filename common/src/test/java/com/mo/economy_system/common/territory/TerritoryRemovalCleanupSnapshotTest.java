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
                    TerritoryRemovalService.RepositoryResult.PERSIST_FAILED, null, failure),
            new TerritoryRemovalRateLimiter(),
            (snapshot, tick) -> {},
            (snapshot, tick) -> {},
            (stage, player, territory, error) -> stages.add(stage));
    assertEquals(
        TerritoryRemovalService.Result.PERSIST_FAILED,
        service.remove(PLAYER, TERRITORY, 0).result());
    assertEquals(List.of("repository-persist-failed"), stages);
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
