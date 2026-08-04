package com.mo.economy_system.common.territory;

import java.util.*;

/** Bounded server-tick limiter for destructive territory requests. */
public final class TerritoryRemovalRateLimiter {
  private final int capacity; private final Map<UUID,Long> attempts=new LinkedHashMap<>(); private long last=-1;
  public TerritoryRemovalRateLimiter(){this(4096);} public TerritoryRemovalRateLimiter(int capacity){if(capacity<1)throw new IllegalArgumentException("capacity");this.capacity=capacity;}
  public synchronized boolean acquire(UUID player,long tick){Objects.requireNonNull(player);reset(tick);Long prior=attempts.get(player);if(prior!=null&&tick-prior<20)return false;attempts.put(player,tick);while(attempts.size()>capacity)attempts.remove(attempts.keySet().iterator().next());return true;}
  private void reset(long tick){if(tick<0)throw new IllegalArgumentException("tick");if(last>=0&&tick<last)attempts.clear();last=tick;}
}
