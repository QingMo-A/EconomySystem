package com.mo.economy_system.common.delivery;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Thread-safe authoritative delivery state with one-shot claim reservations. */
public final class DeliveryBoxLedger implements DeliveryBoxRepository {
  private final Map<UUID, List<DeliveryBoxEntrySnapshot>> boxes = new LinkedHashMap<>();
  private final Set<ClaimKey> reserved = new HashSet<>();

  public synchronized List<DeliveryBoxEntrySnapshot> list(UUID ownerId) {
    Objects.requireNonNull(ownerId, "ownerId");
    return List.copyOf(boxes.getOrDefault(ownerId, List.of()));
  }

  public synchronized void add(UUID ownerId, DeliveryBoxEntrySnapshot entry, DirtyMarker dirty) {
    addAll(ownerId, List.of(entry), dirty);
  }

  /** Adds a group of entries as one persisted mutation, or none of them on failure. */
  public synchronized void addAll(
      UUID ownerId, List<DeliveryBoxEntrySnapshot> entries, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(entries, "entries");
    Objects.requireNonNull(dirty, "dirty");
    if (entries.isEmpty()) throw new IllegalArgumentException("entries must not be empty");
    List<DeliveryBoxEntrySnapshot> additions = List.copyOf(entries);
    List<DeliveryBoxEntrySnapshot> values = boxes.computeIfAbsent(ownerId, ignored -> new ArrayList<>());
    if (additions.size() > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES - values.size()) {
      throw new IllegalStateException("delivery box is full");
    }
    Set<UUID> additionIds = new HashSet<>();
    for (DeliveryBoxEntrySnapshot entry : additions) {
      if (!additionIds.add(entry.entryId()) || find(entry.entryId()) != null) {
        throw new IllegalArgumentException("duplicate delivery entry id");
      }
    }
    values.addAll(additions);
    try {
      dirty.markDirty();
    } catch (RuntimeException failure) {
      values.subList(values.size() - additions.size(), values.size()).clear();
      if (values.isEmpty()) boxes.remove(ownerId);
      throw failure;
    }
  }

  /** Removes a newly-added unreserved group, primarily for cross-ledger transaction rollback. */
  public synchronized boolean removeUnclaimedBatch(UUID ownerId, Set<UUID> entryIds, DirtyMarker dirty) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(entryIds, "entryIds");
    Objects.requireNonNull(dirty, "dirty");
    if (entryIds.isEmpty()) return true;
    List<DeliveryBoxEntrySnapshot> values = boxes.get(ownerId);
    if (values == null) return false;
    for (UUID entryId : entryIds) {
      if (reserved.contains(new ClaimKey(ownerId, entryId))) return false;
      if (values.stream().noneMatch(entry -> entry.entryId().equals(entryId))) return false;
    }
    List<DeliveryBoxEntrySnapshot> before = List.copyOf(values);
    values.removeIf(entry -> entryIds.contains(entry.entryId()));
    if (values.isEmpty()) boxes.remove(ownerId);
    try {
      dirty.markDirty();
      return true;
    } catch (RuntimeException failure) {
      boxes.put(ownerId, new ArrayList<>(before));
      throw failure;
    }
  }

  public synchronized Reservation reserve(UUID ownerId, UUID entryId) {
    Objects.requireNonNull(ownerId, "ownerId");
    Objects.requireNonNull(entryId, "entryId");
    ClaimKey key = new ClaimKey(ownerId, entryId);
    if (reserved.contains(key)) return null;
    List<DeliveryBoxEntrySnapshot> values = boxes.get(ownerId);
    if (values == null) return null;
    for (int index = 0; index < values.size(); index++) {
      DeliveryBoxEntrySnapshot entry = values.get(index);
      if (entry.entryId().equals(entryId)) {
        reserved.add(key);
        return new LedgerReservation(key, entry, index);
      }
    }
    return null;
  }

  public synchronized Map<UUID, List<DeliveryBoxEntrySnapshot>> snapshot() {
    Map<UUID, List<DeliveryBoxEntrySnapshot>> copy = new LinkedHashMap<>();
    boxes.forEach((owner, values) -> copy.put(owner, List.copyOf(values)));
    return Map.copyOf(copy);
  }

  public synchronized void restore(Map<UUID, List<DeliveryBoxEntrySnapshot>> snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    Map<UUID, List<DeliveryBoxEntrySnapshot>> validated = new HashMap<>();
    Set<UUID> ids = new HashSet<>();
    for (Map.Entry<UUID, List<DeliveryBoxEntrySnapshot>> box : snapshot.entrySet()) {
      Objects.requireNonNull(box.getKey(), "ownerId");
      List<DeliveryBoxEntrySnapshot> values = List.copyOf(box.getValue());
      if (values.size() > EconomyNetworkLimits.MAX_DELIVERY_BOX_ENTRIES) {
        throw new IllegalArgumentException("delivery box exceeds entry limit");
      }
      for (DeliveryBoxEntrySnapshot entry : values) {
        Objects.requireNonNull(entry, "entry");
        if (!ids.add(entry.entryId())) throw new IllegalArgumentException("duplicate delivery entry id");
      }
      if (!values.isEmpty()) validated.put(box.getKey(), values);
    }
    boxes.clear();
    validated.forEach((owner, values) -> boxes.put(owner, new ArrayList<>(values)));
    reserved.clear();
  }

  private DeliveryBoxEntrySnapshot find(UUID entryId) {
    for (List<DeliveryBoxEntrySnapshot> values : boxes.values()) {
      for (DeliveryBoxEntrySnapshot entry : values) if (entry.entryId().equals(entryId)) return entry;
    }
    return null;
  }

  private final class LedgerReservation implements Reservation {
    private final ClaimKey key;
    private final DeliveryBoxEntrySnapshot entry;
    private final int expectedIndex;
    private boolean closed;

    private LedgerReservation(ClaimKey key, DeliveryBoxEntrySnapshot entry, int expectedIndex) {
      this.key = key;
      this.entry = entry;
      this.expectedIndex = expectedIndex;
    }

    public DeliveryBoxEntrySnapshot entry() {
      return entry;
    }

    public CommitResult commit(DirtyMarker dirty) {
      Objects.requireNonNull(dirty, "dirty");
      synchronized (DeliveryBoxLedger.this) {
        if (closed || !reserved.contains(key)) return CommitResult.STATE_UNKNOWN;
        List<DeliveryBoxEntrySnapshot> values = boxes.get(key.ownerId());
        if (values == null || expectedIndex >= values.size() || values.get(expectedIndex) != entry) {
          closed = true;
          reserved.remove(key);
          return CommitResult.STATE_UNKNOWN;
        }
        values.remove(expectedIndex);
        if (values.isEmpty()) boxes.remove(key.ownerId());
        try {
          dirty.markDirty();
          closed = true;
          reserved.remove(key);
          return CommitResult.REMOVED;
        } catch (RuntimeException failure) {
          List<DeliveryBoxEntrySnapshot> restored =
              boxes.computeIfAbsent(key.ownerId(), ignored -> new ArrayList<>());
          if (expectedIndex <= restored.size()) restored.add(expectedIndex, entry);
          else restored.add(entry);
          closed = true;
          reserved.remove(key);
          return CommitResult.PERSIST_FAILED;
        }
      }
    }

    public void release() {
      synchronized (DeliveryBoxLedger.this) {
        if (!closed) {
          closed = true;
          reserved.remove(key);
        }
      }
    }
  }

  private record ClaimKey(UUID ownerId, UUID entryId) {}

  @FunctionalInterface
  public interface DirtyMarker {
    void markDirty();
  }
}
