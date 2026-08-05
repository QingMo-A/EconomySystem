package com.mo.economy_system.common.territory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TerritoryMemberRemovalRateLimiter {
  private final int capacity;
  private final Map<UUID, Long> attempts = new LinkedHashMap<>();
  private long lastTick = -1;

  public TerritoryMemberRemovalRateLimiter() {
    this(4096);
  }

  public TerritoryMemberRemovalRateLimiter(int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("capacity");
    this.capacity = capacity;
  }

  public synchronized boolean acquire(UUID playerId, long tick) {
    Objects.requireNonNull(playerId, "playerId");
    if (tick < 0) throw new IllegalArgumentException("tick");
    if (lastTick >= 0 && tick < lastTick) attempts.clear();
    lastTick = tick;
    Long prior = attempts.get(playerId);
    if (prior != null && tick - prior < 20) return false;
    attempts.remove(playerId);
    attempts.put(playerId, tick);
    while (attempts.size() > capacity) attempts.remove(attempts.keySet().iterator().next());
    return true;
  }
}
