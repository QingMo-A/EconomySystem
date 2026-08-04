package com.mo.economy_system.common.territory;
import java.util.*;
public final class TerritoryRemovalRateLimiterRegistry<K>{private final Map<K,TerritoryRemovalRateLimiter> values=new WeakHashMap<>();public synchronized TerritoryRemovalRateLimiter get(K server){return values.computeIfAbsent(Objects.requireNonNull(server),ignored->new TerritoryRemovalRateLimiter());}public synchronized int serverCount(){return values.size();}}
