package com.mo.economy_system.common.territory;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

public final class TerritoryMemberRemovalRateLimiterRegistry<K> {
  private final Map<K, TerritoryMemberRemovalRateLimiter> values = new WeakHashMap<>();

  public synchronized TerritoryMemberRemovalRateLimiter get(K server) {
    return values.computeIfAbsent(
        Objects.requireNonNull(server, "server"),
        ignored -> new TerritoryMemberRemovalRateLimiter());
  }

  public synchronized int serverCount() {
    return values.size();
  }
}
