package com.mo.economy_system.common.tpa;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Bounded, server-scoped request store keyed by the accepting target. */
public final class TpaRequestStore {
  public enum CreateResult {
    CREATED,
    SELF,
    TARGET_BUSY,
    SENDER_BUSY,
    CAPACITY,
    INVALID_TICK
  }

  public record Claim(TpaRequest request) {
    public Claim {
      Objects.requireNonNull(request, "request");
    }
  }

  private static final int DEFAULT_CAPACITY = 4096;
  private static final long DEFAULT_TTL_TICKS = 1_200L;

  private final int capacity;
  private final long ttlTicks;
  private final Map<UUID, TpaRequest> byTarget = new LinkedHashMap<>();
  private final Map<UUID, UUID> targetBySender = new LinkedHashMap<>();

  public TpaRequestStore() {
    this(DEFAULT_CAPACITY, DEFAULT_TTL_TICKS);
  }

  public TpaRequestStore(int capacity, long ttlTicks) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity");
    if (ttlTicks <= 0) throw new IllegalArgumentException("ttlTicks");
    this.capacity = capacity;
    this.ttlTicks = ttlTicks;
  }

  public synchronized CreateResult create(UUID senderId, UUID targetId, long serverTick) {
    Objects.requireNonNull(senderId, "senderId");
    Objects.requireNonNull(targetId, "targetId");
    if (serverTick < 0) return CreateResult.INVALID_TICK;
    expire(serverTick);
    if (senderId.equals(targetId)) return CreateResult.SELF;
    if (byTarget.containsKey(targetId)) return CreateResult.TARGET_BUSY;
    if (targetBySender.containsKey(senderId)) return CreateResult.SENDER_BUSY;
    if (byTarget.size() >= capacity) return CreateResult.CAPACITY;
    long expiresTick;
    try {
      expiresTick = Math.addExact(serverTick, ttlTicks);
    } catch (ArithmeticException error) {
      return CreateResult.INVALID_TICK;
    }
    TpaRequest request = new TpaRequest(senderId, targetId, serverTick, expiresTick);
    byTarget.put(targetId, request);
    targetBySender.put(senderId, targetId);
    return CreateResult.CREATED;
  }

  /** Removes one current request so accept/deny cannot be replayed. */
  public synchronized Optional<Claim> claim(UUID targetId, long serverTick) {
    Objects.requireNonNull(targetId, "targetId");
    if (serverTick < 0) return Optional.empty();
    expire(serverTick);
    TpaRequest request = byTarget.remove(targetId);
    if (request == null) return Optional.empty();
    targetBySender.remove(request.senderId(), targetId);
    return Optional.of(new Claim(request));
  }

  /** Reinstalls a claimed request only when it is still current and both indexes are free. */
  public synchronized boolean release(Claim claim, long serverTick) {
    Objects.requireNonNull(claim, "claim");
    TpaRequest request = claim.request();
    if (serverTick < 0 || request.isExpired(serverTick)) return false;
    if (byTarget.containsKey(request.targetId()) || targetBySender.containsKey(request.senderId())) {
      return false;
    }
    byTarget.put(request.targetId(), request);
    targetBySender.put(request.senderId(), request.targetId());
    return true;
  }

  public synchronized List<TpaRequest> expire(long serverTick) {
    if (serverTick < 0) return List.of();
    List<TpaRequest> expired = new ArrayList<>();
    for (TpaRequest request : new ArrayList<>(byTarget.values())) {
      if (!request.isExpired(serverTick)) continue;
      byTarget.remove(request.targetId(), request);
      targetBySender.remove(request.senderId(), request.targetId());
      expired.add(request);
    }
    return List.copyOf(expired);
  }

  public synchronized int size() {
    return byTarget.size();
  }

  public synchronized void clear() {
    byTarget.clear();
    targetBySender.clear();
  }
}
