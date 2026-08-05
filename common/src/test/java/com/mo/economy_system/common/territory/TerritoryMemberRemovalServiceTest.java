package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalServiceTest {
  private final UUID owner = UUID.randomUUID(),
      land = UUID.randomUUID(),
      target = UUID.randomUUID();

  private TerritoryMemberRemovalService.RemovedMember snap() {
    return new TerritoryMemberRemovalService.RemovedMember(
        land, owner, " Land ", target, " Member ");
  }

  @Test
  void successUsesAuthenticatedOwnerAndExactCleanup() {
    List<Object> seen = new ArrayList<>();
    var s =
        new TerritoryMemberRemovalService(
            (l, o, t) -> {
              seen.add(List.of(l, o, t));
              return new TerritoryMemberRemovalService.RepositoryOutcome(
                  TerritoryMemberRemovalService.RepositoryResult.REMOVED, snap());
            },
            new TerritoryMemberRemovalRateLimiter(),
            (t, l, tick) -> {
              seen.add(List.of(t, l, tick));
              return new TerritoryInviteStore.DiscardResult(1, 0);
            },
            (stage, p, l, e) -> {});
    var out = s.remove(owner, land, target, 10);
    assertEquals(TerritoryMemberRemovalService.Result.SUCCESS, out.result());
    assertEquals("Member", out.removedMember().targetPlayerName());
    assertEquals(List.of(land, owner, target), seen.get(0));
    assertEquals(List.of(target, land, 10L), seen.get(1));
  }

  @Test
  void mapsResultsAndRateBoundary() {
    for (var pair :
        Map.of(
                TerritoryMemberRemovalService.RepositoryResult.TERRITORY_NOT_FOUND,
                TerritoryMemberRemovalService.Result.TERRITORY_NOT_FOUND,
                TerritoryMemberRemovalService.RepositoryResult.OWNER_MISMATCH,
                TerritoryMemberRemovalService.Result.NO_PERMISSION,
                TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET,
                TerritoryMemberRemovalService.Result.CANNOT_REMOVE_OWNER,
                TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER,
                TerritoryMemberRemovalService.Result.TARGET_NOT_MEMBER)
            .entrySet()) {
      var s =
          service(
              (l, o, t) ->
                  new TerritoryMemberRemovalService.RepositoryOutcome(pair.getKey(), null));
      assertEquals(pair.getValue(), s.remove(owner, land, target, 0).result());
    }
    var s =
        service(
            (l, o, t) ->
                new TerritoryMemberRemovalService.RepositoryOutcome(
                    TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER, null));
    assertEquals(
        TerritoryMemberRemovalService.Result.TARGET_NOT_MEMBER,
        s.remove(owner, land, target, 0).result());
    assertEquals(
        TerritoryMemberRemovalService.Result.RATE_LIMITED,
        s.remove(owner, land, target, 19).result());
    assertEquals(
        TerritoryMemberRemovalService.Result.TARGET_NOT_MEMBER,
        s.remove(owner, land, target, 20).result());
  }

  @Test
  void failuresFailClosedAndErrorsEscape() {
    assertEquals(
        TerritoryMemberRemovalService.Result.STATE_UNKNOWN,
        service((l, o, t) -> null).remove(owner, land, target, 0).result());
    assertEquals(
        TerritoryMemberRemovalService.Result.STATE_UNKNOWN,
        service(
                (l, o, t) -> {
                  throw new IllegalStateException();
                })
            .remove(owner, land, target, 0)
            .result());
    assertThrows(
        AssertionError.class,
        () ->
            service(
                    (l, o, t) -> {
                      throw new AssertionError();
                    })
                .remove(owner, land, target, 0));
    var mismatch =
        new TerritoryMemberRemovalService.RemovedMember(UUID.randomUUID(), owner, "x", target, "y");
    assertEquals(
        TerritoryMemberRemovalService.Result.STATE_UNKNOWN,
        service(
                (l, o, t) ->
                    new TerritoryMemberRemovalService.RepositoryOutcome(
                        TerritoryMemberRemovalService.RepositoryResult.REMOVED, mismatch))
            .remove(owner, land, target, 0)
            .result());
  }

  private TerritoryMemberRemovalService service(TerritoryMemberRemovalService.Repository r) {
    return new TerritoryMemberRemovalService(
        r,
        new TerritoryMemberRemovalRateLimiter(),
        (t, l, k) -> new TerritoryInviteStore.DiscardResult(0, 0),
        (s, p, l, e) -> {});
  }
}
