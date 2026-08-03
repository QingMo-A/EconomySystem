package com.mo.economy_system.common.territory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class TerritoryTeleportRateLimiter {
  private final long cooldownTicks; private final int maximumEntries;
  private final LinkedHashMap<UUID, Long> accepted = new LinkedHashMap<>();
  public TerritoryTeleportRateLimiter() { this(20, 4096); }
  public TerritoryTeleportRateLimiter(long cooldownTicks, int maximumEntries) {
    if (cooldownTicks < 1 || maximumEntries < 1) throw new IllegalArgumentException();
    this.cooldownTicks = cooldownTicks; this.maximumEntries = maximumEntries;
  }
  public synchronized boolean tryAcquire(UUID playerId, long tick) {
    Long previous = accepted.get(playerId);
    if (previous != null && tick >= previous && tick - previous < cooldownTicks) return false;
    accepted.put(playerId, tick);
    Iterator<Map.Entry<UUID, Long>> iterator = accepted.entrySet().iterator();
    while (iterator.hasNext()) {
      Map.Entry<UUID, Long> entry = iterator.next();
      if (accepted.size() > maximumEntries || (tick >= entry.getValue() && tick - entry.getValue() >= cooldownTicks * 4)) iterator.remove();
      else break;
    }
    return true;
  }
  public synchronized int size() { return accepted.size(); }
}
