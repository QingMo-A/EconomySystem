package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryMemberRemovalRateLimiterTest {
  @Test
  void enforcesTwentyTickBoundaryWithoutRefreshingRejectedAttempts() {
    var limiter = new TerritoryMemberRemovalRateLimiter();
    UUID player = UUID.randomUUID();
    assertTrue(limiter.acquire(player, 0));
    assertFalse(limiter.acquire(player, 19));
    assertTrue(limiter.acquire(player, 20));
    assertTrue(limiter.acquire(UUID.randomUUID(), 20));
    assertThrows(IllegalArgumentException.class, () -> limiter.acquire(player, -1));
  }

  @Test
  void tickRollbackStartsANewEpoch() {
    var limiter = new TerritoryMemberRemovalRateLimiter();
    UUID player = UUID.randomUUID();
    assertTrue(limiter.acquire(player, 100));
    assertTrue(limiter.acquire(player, 1));
  }

  @Test
  void capacityEvictsLeastRecentlyAllowedPlayer() {
    var limiter = new TerritoryMemberRemovalRateLimiter(2);
    UUID first = UUID.randomUUID(), second = UUID.randomUUID(), third = UUID.randomUUID();
    assertTrue(limiter.acquire(first, 0));
    assertTrue(limiter.acquire(second, 0));
    assertTrue(limiter.acquire(first, 20));
    assertTrue(limiter.acquire(third, 20));
    assertFalse(limiter.acquire(first, 21));
    assertTrue(limiter.acquire(second, 21));
  }
}
