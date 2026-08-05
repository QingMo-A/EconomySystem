package com.mo.economy_system.common.check;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ClientFileCheckRequestStore {
  public enum PutResult {
    CREATED,
    ALREADY_PENDING,
    FULL,
    RATE_LIMITED
  }

  public enum ClaimStatus {
    CLAIMED,
    NOT_FOUND,
    BUSY
  }

  public record Key(UUID targetPlayerId, UUID requesterPlayerId, ClientFileCheckType checkType) {
    public Key {
      Objects.requireNonNull(targetPlayerId);
      Objects.requireNonNull(requesterPlayerId);
      Objects.requireNonNull(checkType);
    }
  }

  public record Pending(
      UUID targetPlayerId,
      String targetPlayerName,
      UUID requesterPlayerId,
      String requesterPlayerName,
      ClientFileCheckType checkType,
      long createdTick,
      long expiresTick) {
    public Pending {
      Objects.requireNonNull(targetPlayerId);
      targetPlayerName = ClientFileCheckValidation.playerName(targetPlayerName);
      Objects.requireNonNull(requesterPlayerId);
      requesterPlayerName = ClientFileCheckValidation.playerName(requesterPlayerName);
      Objects.requireNonNull(checkType);
      if (createdTick < 0 || expiresTick <= createdTick)
        throw new IllegalArgumentException("ticks");
    }

    public Key key() {
      return new Key(targetPlayerId, requesterPlayerId, checkType);
    }
  }

  public record Claim(long token, Pending pending) {
    public Claim {
      if (token <= 0) throw new IllegalArgumentException("token");
      Objects.requireNonNull(pending);
    }
  }

  public record ClaimResult(ClaimStatus status, Claim claim) {
    public ClaimResult {
      Objects.requireNonNull(status, "status");
      if ((status == ClaimStatus.CLAIMED) != (claim != null))
        throw new IllegalArgumentException("claim invariant");
    }
  }

  private final int capacity;
  private final Map<Key, Pending> pending = new LinkedHashMap<>();
  private final Map<Key, Long> processing = new HashMap<>();
  private final Map<UUID, Long> requesterCooldown = new LinkedHashMap<>();
  private final Map<UUID, Long> targetCooldown = new LinkedHashMap<>();
  private long lastTick = -1;
  private long nextToken = 1;

  public ClientFileCheckRequestStore() {
    this(EconomyNetworkLimits.MAX_PENDING_CHECKS);
  }

  public ClientFileCheckRequestStore(int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("capacity");
    this.capacity = capacity;
  }

  public synchronized PutResult put(Pending value, long tick) {
    Objects.requireNonNull(value, "pending");
    cleanup(tick);
    if (value.createdTick() != tick
        || value.expiresTick() - tick > EconomyNetworkLimits.CHECK_REQUEST_TTL_TICKS)
      throw new IllegalArgumentException("pending tick");
    if (pending.containsKey(value.key())) return PutResult.ALREADY_PENDING;
    if (!cooldownAllows(value.requesterPlayerId(), value.targetPlayerId(), tick))
      return PutResult.RATE_LIMITED;
    if (pending.size() >= capacity) return PutResult.FULL;
    pending.put(value.key(), value);
    requesterCooldown.put(value.requesterPlayerId(), tick);
    targetCooldown.put(value.targetPlayerId(), tick);
    trimCooldowns();
    return PutResult.CREATED;
  }

  public synchronized Optional<Pending> find(Key key, long tick) {
    cleanup(tick);
    return Optional.ofNullable(pending.get(Objects.requireNonNull(key)));
  }

  public synchronized ClaimResult claim(Key key, long tick) {
    cleanup(tick);
    Pending value = pending.get(Objects.requireNonNull(key));
    if (value == null) return new ClaimResult(ClaimStatus.NOT_FOUND, null);
    if (processing.containsKey(key)) return new ClaimResult(ClaimStatus.BUSY, null);
    long token = nextToken++;
    if (nextToken <= 0) nextToken = 1;
    processing.put(key, token);
    return new ClaimResult(ClaimStatus.CLAIMED, new Claim(token, value));
  }

  public synchronized boolean complete(Claim claim) {
    if (!valid(claim)) return false;
    pending.remove(claim.pending().key());
    processing.remove(claim.pending().key());
    return true;
  }

  public synchronized boolean release(Claim claim) {
    if (!valid(claim)) return false;
    processing.remove(claim.pending().key());
    return true;
  }

  public synchronized boolean discard(Key key, long tick) {
    cleanup(tick);
    if (processing.containsKey(key)) return false;
    return pending.remove(key) != null;
  }

  public synchronized boolean rollbackCreated(Pending value, long tick) {
    Objects.requireNonNull(value, "pending");
    cleanup(tick);
    if (processing.containsKey(value.key()) || !Objects.equals(pending.get(value.key()), value))
      return false;
    pending.remove(value.key());
    requesterCooldown.computeIfPresent(
        value.requesterPlayerId(),
        (ignored, recorded) -> recorded == value.createdTick() ? null : recorded);
    targetCooldown.computeIfPresent(
        value.targetPlayerId(),
        (ignored, recorded) -> recorded == value.createdTick() ? null : recorded);
    return true;
  }

  public synchronized int size(long tick) {
    cleanup(tick);
    return pending.size();
  }

  private boolean valid(Claim claim) {
    return claim != null
        && Objects.equals(processing.get(claim.pending().key()), claim.token())
        && Objects.equals(pending.get(claim.pending().key()), claim.pending());
  }

  private boolean cooldownAllows(UUID requester, UUID target, long tick) {
    Long requesterTick = requesterCooldown.get(requester);
    Long targetTick = targetCooldown.get(target);
    long cooldown = EconomyNetworkLimits.CHECK_REQUEST_COOLDOWN_TICKS;
    return (requesterTick == null || tick - requesterTick >= cooldown)
        && (targetTick == null || tick - targetTick >= cooldown);
  }

  private void cleanup(long tick) {
    if (tick < 0) throw new IllegalArgumentException("tick");
    if (lastTick >= 0 && tick < lastTick) {
      pending.clear();
      processing.clear();
      requesterCooldown.clear();
      targetCooldown.clear();
    }
    lastTick = tick;
    for (Pending value : new ArrayList<>(pending.values()))
      if (tick >= value.expiresTick()) {
        pending.remove(value.key());
        processing.remove(value.key());
      }
  }

  private void trimCooldowns() {
    while (requesterCooldown.size() > capacity)
      requesterCooldown.remove(requesterCooldown.keySet().iterator().next());
    while (targetCooldown.size() > capacity)
      targetCooldown.remove(targetCooldown.keySet().iterator().next());
  }
}
