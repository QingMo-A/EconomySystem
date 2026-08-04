package com.mo.economy_system.common.territory;
import java.util.Objects;
import java.util.WeakHashMap;

/** Weak server-scoped limiter ownership prevents cooldown leakage across server lifetimes. */
public final class TerritoryTeleportLimiterRegistry<K> {
  private final WeakHashMap<K,TerritoryTeleportRateLimiter> values=new WeakHashMap<>();
  public synchronized TerritoryTeleportRateLimiter forServer(K server){return values.computeIfAbsent(Objects.requireNonNull(server,"server"),ignored->new TerritoryTeleportRateLimiter());}
  synchronized int size(){return values.size();}
}
