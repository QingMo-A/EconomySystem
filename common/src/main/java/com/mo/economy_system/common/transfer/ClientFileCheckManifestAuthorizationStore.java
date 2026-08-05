package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckEntry;
import com.mo.economy_system.common.check.ClientFileCheckResult;
import com.mo.economy_system.common.check.ClientFileCheckStatus;
import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Server-scoped authorizations installed only after protocol 25 delivery succeeds. */
public final class ClientFileCheckManifestAuthorizationStore {
  public enum ReplaceResult { INSTALLED, CLEARED_NO_ENTRIES, CAPACITY_REJECTED }

  public record Scope(UUID targetPlayerId, UUID requesterPlayerId, ClientFileCheckType checkType) {
    public Scope {
      Objects.requireNonNull(targetPlayerId);
      Objects.requireNonNull(requesterPlayerId);
      Objects.requireNonNull(checkType);
    }
  }

  public record Key(UUID targetPlayerId, UUID requesterPlayerId, ClientFileCheckType checkType,
                    String fileName) {
    public Key {
      Objects.requireNonNull(targetPlayerId);
      Objects.requireNonNull(requesterPlayerId);
      Objects.requireNonNull(checkType);
      fileName = CheckedFileTransferValidation.fileName(fileName);
    }

    public Scope scope() { return new Scope(targetPlayerId, requesterPlayerId, checkType); }
  }

  public record Authorization(long expectedByteLength, String expectedSha256,
                              long createdTick, long expiresTick) {
    public Authorization {
      if (expectedByteLength < 0
          || expectedByteLength > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES
          || createdTick < 0
          || expiresTick <= createdTick
          || expiresTick - createdTick > EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS) {
        throw new IllegalArgumentException("authorization");
      }
      expectedSha256 = CheckedFileTransferValidation.sha256(expectedSha256);
    }
  }

  private final int capacity;
  private final int perTargetCapacity;
  private final int perRequesterCapacity;
  private final int perScopeCapacity;
  private final LinkedHashMap<Key, Authorization> entries = new LinkedHashMap<>();
  private long lastTick = -1;

  public ClientFileCheckManifestAuthorizationStore() {
    this(EconomyNetworkLimits.MAX_PENDING_FILE_TRANSFERS * 4);
  }

  public ClientFileCheckManifestAuthorizationStore(int capacity) {
    this(capacity, capacity, capacity, capacity);
  }

  public ClientFileCheckManifestAuthorizationStore(
      int capacity, int perTargetCapacity, int perRequesterCapacity, int perScopeCapacity) {
    if (capacity < 1 || perTargetCapacity < 1 || perRequesterCapacity < 1 || perScopeCapacity < 1) {
      throw new IllegalArgumentException("capacity");
    }
    this.capacity = capacity;
    this.perTargetCapacity = perTargetCapacity;
    this.perRequesterCapacity = perRequesterCapacity;
    this.perScopeCapacity = perScopeCapacity;
  }

  public synchronized ReplaceResult replace(
      UUID target, UUID requester, ClientFileCheckResult result, long tick) {
    Objects.requireNonNull(result);
    cleanup(tick);
    Scope scope = new Scope(target, requester, result.checkType());
    removeScopeWithoutCleanup(scope);
    if (result.status() != ClientFileCheckStatus.SUCCESS
        && result.status() != ClientFileCheckStatus.TRUNCATED) {
      return ReplaceResult.CLEARED_NO_ENTRIES;
    }

    List<java.util.Map.Entry<Key, Authorization>> replacement = new ArrayList<>();
    for (ClientFileCheckEntry entry : result.files()) {
      if (entry.size() > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES) continue;
      Key key = new Key(target, requester, result.checkType(), entry.fileName());
      Authorization value = new Authorization(
          entry.size(), entry.sha256(), tick, tick + EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS);
      replacement.add(java.util.Map.entry(key, value));
    }
    if (!hasCapacity(scope, replacement.size())) return ReplaceResult.CAPACITY_REJECTED;
    replacement.forEach(entry -> entries.put(entry.getKey(), entry.getValue()));
    return replacement.isEmpty() ? ReplaceResult.CLEARED_NO_ENTRIES : ReplaceResult.INSTALLED;
  }

  public synchronized Optional<Authorization> find(Key key, long tick) {
    cleanup(tick);
    return Optional.ofNullable(entries.get(Objects.requireNonNull(key)));
  }

  public synchronized void removeScope(Scope scope, long tick) {
    cleanup(tick);
    removeScopeWithoutCleanup(Objects.requireNonNull(scope));
  }

  public synchronized int size(long tick) { cleanup(tick); return entries.size(); }
  public synchronized void clear() { entries.clear(); lastTick = -1; }

  private boolean hasCapacity(Scope scope, int added) {
    if (added > perScopeCapacity || entries.size() + added > capacity) return false;
    long target = entries.keySet().stream()
        .filter(key -> key.targetPlayerId().equals(scope.targetPlayerId())).count();
    long requester = entries.keySet().stream()
        .filter(key -> key.requesterPlayerId().equals(scope.requesterPlayerId())).count();
    return target + added <= perTargetCapacity && requester + added <= perRequesterCapacity;
  }

  private void removeScopeWithoutCleanup(Scope scope) {
    entries.keySet().removeIf(key -> key.scope().equals(scope));
  }

  private void cleanup(long tick) {
    if (tick < 0) throw new IllegalArgumentException("tick");
    if (lastTick >= 0 && tick < lastTick) entries.clear();
    lastTick = tick;
    entries.entrySet().removeIf(entry -> tick >= entry.getValue().expiresTick());
  }
}
