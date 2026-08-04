package com.mo.economy_system.common.territory;

import java.util.Objects;

/** One-shot, conflict-safe restoration of exactly one removed recall potion. */
public final class RecallPotionReservation<T> implements TerritoryTeleportService.Reservation {
  public interface Slots<T> {
    int size(); T get(int slot); void set(int slot, T value);
    T copy(T value); boolean equivalent(T left, T right); boolean empty(T value);
    boolean canMerge(T existing, T removed); int count(T value); int maximum(T value);
    T withAddedOne(T existing, T removed); void changed() throws Exception;
  }
  private enum State { RESERVED, COMMITTED, ROLLED_BACK, ROLLBACK_FAILED }
  private final int slot; private final T removed; private final T expectedRemaining; private final Slots<T> slots;
  private State state = State.RESERVED;

  public RecallPotionReservation(int slot, T removed, T expectedRemaining, Slots<T> slots) {
    this.slot = slot; this.removed = Objects.requireNonNull(removed, "removed");
    this.expectedRemaining = Objects.requireNonNull(expectedRemaining, "expectedRemaining");
    this.slots = Objects.requireNonNull(slots, "slots");
    if (slot < 0 || slot >= slots.size()) throw new IllegalArgumentException("invalid slot");
  }
  public int slot() { return slot; }
  public synchronized void commit() {
    if (state == State.COMMITTED) return;
    if (state != State.RESERVED) throw new IllegalStateException("reservation already completed: " + state);
    state = State.COMMITTED;
  }
  public synchronized void rollback() throws Exception {
    if (state != State.RESERVED) throw new IllegalStateException("reservation already completed: " + state);
    int destination = -1;
    T replacement = null;
    T current = slots.get(slot);
    if (slots.equivalent(current, expectedRemaining)) {
      destination = slot; replacement = slots.withAddedOne(current, removed);
    } else {
      for (int index = 0; index < slots.size(); index++) {
        T candidate = slots.get(index);
        if (slots.canMerge(candidate, removed) && slots.count(candidate) < slots.maximum(candidate)) {
          destination = index; replacement = slots.withAddedOne(candidate, removed); break;
        }
      }
      if (destination < 0) for (int index = 0; index < slots.size(); index++) {
        if (slots.empty(slots.get(index))) { destination = index; replacement = slots.copy(removed); break; }
      }
    }
    if (destination < 0) { state = State.ROLLBACK_FAILED; throw new IllegalStateException("no safe recall-potion rollback slot"); }
    try { slots.set(destination, replacement); slots.changed(); state = State.ROLLED_BACK; }
    catch (Exception error) { state = State.ROLLBACK_FAILED; throw error; }
  }
}
