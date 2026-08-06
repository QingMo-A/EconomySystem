package com.mo.economy_system.common.transfer;

import com.mo.economy_system.common.check.ClientFileCheckType;
import com.mo.economy_system.common.check.ClientFileCheckValidation;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Server-scoped ordered streaming state. File content is never accumulated. */
public final class CheckedFileTransferStore {
  interface DigestSupport {
    MessageDigest create();
    MessageDigest copy(MessageDigest source);
  }

  private static final DigestSupport DEFAULT_DIGEST_SUPPORT = new DigestSupport() {
    @Override public MessageDigest create() { return digest(); }
    @Override public MessageDigest copy(MessageDigest source) { return cloneDigest(source); }
  };
  public enum State {
    AWAITING_TARGET_RESPONSE, READY_FORWARDING, STREAMING, CHUNK_FORWARDING, FINALIZING
  }

  public enum Result {
    CREATED, ALREADY_PENDING, TARGET_BUSY, REQUESTER_BUSY, BUSY, FULL, RATE_LIMITED,
    NOT_FOUND, WRONG_TARGET, INVALID_METADATA, INVALID_CHUNK, CHUNK_OUT_OF_ORDER,
    HASH_MISMATCH, SIZE_MISMATCH, REQUESTER_OFFLINE, TARGET_OFFLINE,
    READY, FORWARD, COMPLETE
  }

  public record Key(UUID targetPlayerId, UUID requesterPlayerId,
                    ClientFileCheckType checkType, String fileName) {
    public Key {
      Objects.requireNonNull(targetPlayerId);
      Objects.requireNonNull(requesterPlayerId);
      Objects.requireNonNull(checkType);
      fileName = CheckedFileTransferValidation.fileName(fileName);
    }
  }

  public record Pending(Key key, String targetName, String requesterName, long size,
                        String sha256, long createdTick, long expiresTick) {
    public Pending {
      Objects.requireNonNull(key);
      targetName = ClientFileCheckValidation.playerName(targetName);
      requesterName = ClientFileCheckValidation.playerName(requesterName);
      sha256 = CheckedFileTransferValidation.sha256(sha256);
      if (size < 0 || size > EconomyNetworkLimits.MAX_TRANSFER_FILE_BYTES
          || createdTick < 0 || expiresTick <= createdTick
          || expiresTick - createdTick > EconomyNetworkLimits.FILE_TRANSFER_TTL_TICKS) {
        throw new IllegalArgumentException("pending");
      }
    }
  }

  public record ReadyClaim(long token, Key key, CheckedFileTransferControl control) {}
  public record ChunkClaim(long token, Key key, UUID transferId, int index, int total,
                           byte[] raw, boolean terminal, long byteLength, String sha256) {
    public ChunkClaim { raw = Arrays.copyOf(raw, raw.length); }
    @Override public byte[] raw() { return Arrays.copyOf(raw, raw.length); }
  }
  public record PrepareReady(
      Result result, ReadyClaim claim, Pending failurePending, String failureCode) {
    PrepareReady(Result result, ReadyClaim claim) { this(result, claim, null, null); }
  }
  public record PrepareChunk(Result result, ChunkClaim claim, Pending failurePending) {
    PrepareChunk(Result result, ChunkClaim claim) { this(result, claim, null); }
  }
  public record CommitResult(Result result, Pending failurePending) {}
  public record ChunkResult(Result result, boolean consumed, long byteLength, String sha256) {}

  private static final class Active {
    final Pending pending;
    State state = State.AWAITING_TARGET_RESPONSE;
    UUID transferId;
    int total;
    int next;
    long received;
    MessageDigest digest;
    long claimToken;
    Active(Pending pending) { this.pending = pending; }
  }

  private final int capacity;
  private final DigestSupport digestSupport;
  private final LinkedHashMap<Key, Active> active = new LinkedHashMap<>();
  private final Map<UUID, Key> transferIds = new HashMap<>();
  private final LinkedHashMap<UUID, Long> targetCooldown = new LinkedHashMap<>();
  private final LinkedHashMap<UUID, Long> requesterCooldown = new LinkedHashMap<>();
  private long lastTick = -1;
  private long nextClaimToken = 1;

  public CheckedFileTransferStore() { this(EconomyNetworkLimits.MAX_PENDING_FILE_TRANSFERS); }
  public CheckedFileTransferStore(int capacity) {
    this(capacity, DEFAULT_DIGEST_SUPPORT, 1);
  }
  CheckedFileTransferStore(int capacity, DigestSupport digestSupport, long initialClaimToken) {
    if (capacity < 1) throw new IllegalArgumentException("capacity");
    if (initialClaimToken < 1) throw new IllegalArgumentException("claim token");
    this.capacity = Math.min(capacity, EconomyNetworkLimits.MAX_ACTIVE_FILE_TRANSFERS);
    this.digestSupport = Objects.requireNonNull(digestSupport, "digest support");
    this.nextClaimToken = initialClaimToken;
  }

  public synchronized Result create(Pending pending, long tick) {
    Objects.requireNonNull(pending);
    cleanup(tick);
    if (pending.createdTick() != tick) return Result.INVALID_METADATA;
    if (active.containsKey(pending.key())) return Result.ALREADY_PENDING;
    if (hasTarget(pending.key().targetPlayerId())) return Result.TARGET_BUSY;
    if (hasRequester(pending.key().requesterPlayerId())) return Result.REQUESTER_BUSY;
    if (isCooling(targetCooldown, pending.key().targetPlayerId(), tick)
        || isCooling(requesterCooldown, pending.key().requesterPlayerId(), tick)) {
      return Result.RATE_LIMITED;
    }
    if (active.size() >= capacity) return Result.FULL;
    active.put(pending.key(), new Active(pending));
    targetCooldown.put(pending.key().targetPlayerId(), tick);
    requesterCooldown.put(pending.key().requesterPlayerId(), tick);
    trimCooldowns();
    return Result.CREATED;
  }

  public synchronized boolean rollback(Pending pending, long tick) {
    cleanup(tick);
    Active current = active.get(pending.key());
    if (current == null || current.pending != pending) return false;
    consume(pending.key());
    removeCooldownIfCreatedBy(pending);
    return true;
  }

  public synchronized PrepareReady prepareReady(
      Key key, UUID authenticatedTarget, CheckedFileTransferControl control, long tick) {
    cleanup(tick);
    Active current = active.get(key);
    if (current == null) return new PrepareReady(Result.NOT_FOUND, null);
    if (!key.targetPlayerId().equals(authenticatedTarget)) {
      return new PrepareReady(Result.WRONG_TARGET, null);
    }
    String failureCode = null;
    if (current.state != State.AWAITING_TARGET_RESPONSE) failureCode = "READY_REPLAY";
    else if (transferIds.containsKey(control.transferId())) failureCode = "TRANSFER_ID_REUSE";
    else if (control.status() != CheckedFileTransferControlStatus.READY
        || control.byteLength() != current.pending.size()
        || !control.sha256().equals(current.pending.sha256())
        || control.rawChunkBytes() != EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES
        || control.totalChunks() != CheckedFileTransferValidation.totalChunks(
            current.pending.size(), EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES)) {
      failureCode = "INVALID_METADATA";
    }
    if (failureCode != null) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareReady(Result.INVALID_METADATA, null, failurePending, failureCode);
    }
    current.state = State.READY_FORWARDING;
    current.claimToken = claimToken();
    return new PrepareReady(Result.READY,
        new ReadyClaim(current.claimToken, key, control));
  }

  public synchronized Result commitReady(ReadyClaim claim, long tick) {
    return commitReadyDetailed(claim, tick).result();
  }

  public synchronized CommitResult commitReadyDetailed(ReadyClaim claim, long tick) {
    cleanup(tick);
    Active current = claimed(claim.key(), claim.token(), State.READY_FORWARDING);
    if (current == null) return new CommitResult(Result.NOT_FOUND, null);
    CheckedFileTransferControl control = claim.control();
    current.transferId = control.transferId();
    current.total = control.totalChunks();
    try {
      current.digest = digestSupport.create();
    } catch (RuntimeException failure) {
      Pending failurePending = current.pending;
      consume(claim.key());
      return new CommitResult(Result.INVALID_METADATA, failurePending);
    } catch (Error failure) {
      consume(claim.key());
      throw failure;
    }
    current.state = current.total == 0 ? State.FINALIZING : State.STREAMING;
    transferIds.put(current.transferId, claim.key());
    return new CommitResult(current.total == 0 ? Result.COMPLETE : Result.READY, null);
  }

  public synchronized PrepareChunk prepareChunk(
      Key key, UUID authenticatedTarget, UUID transferId, int index, int total,
      byte[] raw, long tick) {
    cleanup(tick);
    Active current = active.get(key);
    if (current == null) return new PrepareChunk(Result.NOT_FOUND, null);
    if (!key.targetPlayerId().equals(authenticatedTarget)) {
      return new PrepareChunk(Result.WRONG_TARGET, null);
    }
    if (current.state != State.STREAMING || !Objects.equals(current.transferId, transferId)
        || !Objects.equals(transferIds.get(transferId), key) || total != current.total) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareChunk(Result.INVALID_CHUNK, null, failurePending);
    }
    if (index != current.next) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareChunk(Result.CHUNK_OUT_OF_ORDER, null, failurePending);
    }
    long remaining = current.pending.size() - current.received;
    int expected = (int) Math.min(EconomyNetworkLimits.TRANSFER_RAW_CHUNK_BYTES, remaining);
    if (raw == null || raw.length != expected) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareChunk(Result.INVALID_CHUNK, null, failurePending);
    }
    boolean terminal = index + 1 == total;
    MessageDigest candidate;
    try {
      candidate = digestSupport.copy(current.digest);
    } catch (RuntimeException failure) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareChunk(Result.INVALID_CHUNK, null, failurePending);
    } catch (Error failure) {
      consume(key);
      throw failure;
    }
    candidate.update(raw);
    long byteLength = current.received + raw.length;
    String hash = terminal ? HexFormat.of().formatHex(candidate.digest()) : null;
    if (terminal && byteLength != current.pending.size()) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareChunk(Result.SIZE_MISMATCH, null, failurePending);
    }
    if (terminal && !hash.equals(current.pending.sha256())) {
      Pending failurePending = current.pending;
      consume(key);
      return new PrepareChunk(Result.HASH_MISMATCH, null, failurePending);
    }
    current.state = State.CHUNK_FORWARDING;
    current.claimToken = claimToken();
    return new PrepareChunk(terminal ? Result.COMPLETE : Result.FORWARD,
        new ChunkClaim(current.claimToken, key, transferId, index, total, raw,
            terminal, byteLength, hash));
  }

  public synchronized Result commitChunk(ChunkClaim claim, long tick) {
    return commitChunkDetailed(claim, tick).result();
  }

  public synchronized CommitResult commitChunkDetailed(ChunkClaim claim, long tick) {
    cleanup(tick);
    Active current = claimed(claim.key(), claim.token(), State.CHUNK_FORWARDING);
    if (current == null) return new CommitResult(Result.NOT_FOUND, null);
    current.digest.update(claim.raw());
    current.received += claim.raw().length;
    current.next++;
    if (claim.terminal()) {
      current.state = State.FINALIZING;
      return new CommitResult(Result.COMPLETE, null);
    }
    current.state = State.STREAMING;
    return new CommitResult(Result.FORWARD, null);
  }

  /** Irreversibly consumes an authenticated exact key while retaining authoritative metadata. */
  public synchronized Pending consumePending(Key key, long tick) {
    cleanup(tick);
    Active removed = consume(key);
    return removed == null ? null : removed.pending;
  }

  public synchronized boolean consumeClaim(ReadyClaim claim) { return consumeClaim(claim.key(), claim.token()); }
  public synchronized boolean consumeClaim(ChunkClaim claim) { return consumeClaim(claim.key(), claim.token()); }
  public synchronized boolean complete(Key key, long tick) { cleanup(tick); return consume(key) != null; }

  /** Compatibility entry point retained for existing callers and tests. */
  public synchronized Result ready(Key key, UUID target, CheckedFileTransferControl control, long tick) {
    PrepareReady prepared = prepareReady(key, target, control, tick);
    if (prepared.claim() == null) return prepared.result();
    return commitReady(prepared.claim(), tick);
  }

  /** Compatibility entry point retained for existing callers and tests. */
  public synchronized ChunkResult chunk(Key key, UUID target, UUID transferId, int index,
                                        int total, byte[] raw, long tick) {
    PrepareChunk prepared = prepareChunk(key, target, transferId, index, total, raw, tick);
    if (prepared.claim() == null) return new ChunkResult(prepared.result(), true, 0, null);
    Result result = commitChunk(prepared.claim(), tick);
    if (result == Result.COMPLETE) {
      complete(key, tick);
      return new ChunkResult(result, true, prepared.claim().byteLength(), prepared.claim().sha256());
    }
    return new ChunkResult(result, false, 0, null);
  }

  public synchronized boolean discard(Key key, long tick) { cleanup(tick); return consume(key) != null; }
  public synchronized int size(long tick) { cleanup(tick); return active.size(); }
  public synchronized boolean contains(Key key, long tick) {
    cleanup(tick);
    return active.containsKey(key);
  }
  public synchronized boolean metadataMatches(Key key, String targetName, String requesterName,
                                              long tick) {
    cleanup(tick);
    Active current = active.get(key);
    return current != null && current.pending.targetName().equals(targetName)
        && current.pending.requesterName().equals(requesterName);
  }
  public synchronized void discardTarget(UUID targetId) {
    var removed = active.keySet().stream()
        .filter(key -> key.targetPlayerId().equals(targetId)).toList();
    for (Key key : removed) {
      consume(key);
      requesterCooldown.remove(key.requesterPlayerId());
    }
    targetCooldown.remove(targetId);
  }
  public synchronized void discardRequester(UUID requesterId) {
    var removed = active.keySet().stream()
        .filter(key -> key.requesterPlayerId().equals(requesterId)).toList();
    for (Key key : removed) {
      consume(key);
      targetCooldown.remove(key.targetPlayerId());
    }
    requesterCooldown.remove(requesterId);
  }
  public synchronized void clear() {
    active.clear(); transferIds.clear(); targetCooldown.clear(); requesterCooldown.clear(); lastTick = -1;
  }

  private Active claimed(Key key, long token, State state) {
    Active current = active.get(key);
    return current != null && current.state == state && current.claimToken == token ? current : null;
  }
  private boolean consumeClaim(Key key, long token) {
    Active current = active.get(key);
    if (current == null || current.claimToken != token) return false;
    consume(key);
    return true;
  }
  private Active consume(Key key) {
    Active removed = active.remove(key);
    removeTransferIndex(removed);
    return removed;
  }
  private void removeTransferIndex(Active active) {
    if (active != null && active.transferId != null) transferIds.remove(active.transferId);
  }
  private boolean hasTarget(UUID id) { return active.keySet().stream().anyMatch(k -> k.targetPlayerId().equals(id)); }
  private boolean hasRequester(UUID id) { return active.keySet().stream().anyMatch(k -> k.requesterPlayerId().equals(id)); }
  private static boolean isCooling(Map<UUID, Long> cooldowns, UUID id, long tick) {
    Long previous = cooldowns.get(id);
    return previous != null && tick - previous < EconomyNetworkLimits.FILE_TRANSFER_COOLDOWN_TICKS;
  }
  private void cleanup(long tick) {
    if (tick < 0) throw new IllegalArgumentException("tick");
    if (lastTick >= 0 && tick < lastTick) { clear(); lastTick = tick; return; }
    lastTick = tick;
    active.entrySet().removeIf(entry -> {
      boolean expired = tick >= entry.getValue().pending.expiresTick();
      if (expired) removeTransferIndex(entry.getValue());
      return expired;
    });
  }
  private void removeCooldownIfCreatedBy(Pending pending) {
    targetCooldown.computeIfPresent(pending.key().targetPlayerId(),
        (key, value) -> value == pending.createdTick() ? null : value);
    requesterCooldown.computeIfPresent(pending.key().requesterPlayerId(),
        (key, value) -> value == pending.createdTick() ? null : value);
  }
  private void trimCooldowns() {
    int limit = Math.max(capacity * 4, 16);
    while (targetCooldown.size() > limit) targetCooldown.remove(targetCooldown.keySet().iterator().next());
    while (requesterCooldown.size() > limit) requesterCooldown.remove(requesterCooldown.keySet().iterator().next());
  }
  private long claimToken() {
    for (int attempts = 0; attempts <= active.size(); attempts++) {
      long candidate = nextClaimToken;
      nextClaimToken = nextClaimToken == Long.MAX_VALUE ? 1 : nextClaimToken + 1;
      boolean inUse = active.values().stream().anyMatch(value -> value.claimToken == candidate);
      if (!inUse) return candidate;
    }
    throw new IllegalStateException("claim token space exhausted");
  }
  private static MessageDigest digest() {
    try { return MessageDigest.getInstance("SHA-256"); }
    catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
  }
  private static MessageDigest cloneDigest(MessageDigest source) {
    try { return (MessageDigest) source.clone(); }
    catch (CloneNotSupportedException exception) { throw new IllegalStateException(exception); }
  }
}
