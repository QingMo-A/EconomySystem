package com.mo.economy_system.common.market;

import java.util.ArrayList;
import java.util.List;

/** Loader-neutral transaction algorithm used by both market inventory adapters. */
public final class SlotInventoryTransactions<T> {
  private final Slots<T> slots;

  public SlotInventoryTransactions(Slots<T> slots) {
    this.slots = slots;
  }

  public long countMatching(T template) {
    long count = 0;
    for (int index = 0; index < slots.size(); index++) {
      T value = slots.get(index);
      if (!slots.isEmpty(value) && slots.matches(value, template)) count += slots.count(value);
    }
    return count;
  }

  public long capacity(T template) {
    long capacity = 0;
    for (int index = 0; index < slots.size(); index++) {
      T value = slots.get(index);
      if (slots.isEmpty(value)) capacity += slots.maxStackSize(template);
      else if (slots.matches(value, template))
        capacity += Math.max(0, slots.maxStackSize(value) - slots.count(value));
    }
    return capacity;
  }

  public InventoryInsertionResult insert(T template, int quantity) {
    List<T> before = snapshot();
    try {
      int remaining = quantity;
      for (int index = 0; index < slots.size() && remaining > 0; index++) {
        T value = slots.get(index);
        if (!slots.isEmpty(value) && slots.matches(value, template)) {
          int add = Math.min(remaining, Math.max(0, slots.maxStackSize(value) - slots.count(value)));
          if (add > 0) { slots.setCount(value, slots.count(value) + add); remaining -= add; }
        }
      }
      for (int index = 0; index < slots.size() && remaining > 0; index++)
        if (slots.isEmpty(slots.get(index))) {
          T inserted = slots.copy(template);
          int add = Math.min(remaining, slots.maxStackSize(inserted));
          slots.setCount(inserted, add); slots.set(index, inserted); remaining -= add;
        }
      if (remaining != 0) return InventoryInsertionResult.failure(restore(before));
      slots.setChanged();
      return InventoryInsertionResult.success(() -> restore(before));
    } catch (RuntimeException error) {
      return InventoryInsertionResult.failure(restore(before));
    }
  }

  public InventoryRemovalResult remove(T template, int quantity) {
    List<T> before = snapshot();
    try {
      int remaining = quantity;
      for (int index = 0; index < slots.size() && remaining > 0; index++) {
        T value = slots.get(index);
        if (!slots.isEmpty(value) && slots.matches(value, template)) {
          int removed = Math.min(remaining, slots.count(value));
          slots.setCount(value, slots.count(value) - removed); remaining -= removed;
        }
      }
      if (remaining != 0) return InventoryRemovalResult.failure(restore(before));
      slots.setChanged();
      return InventoryRemovalResult.success(() -> restore(before));
    } catch (RuntimeException error) {
      return InventoryRemovalResult.failure(restore(before));
    }
  }

  private List<T> snapshot() {
    List<T> result = new ArrayList<>(slots.size());
    for (int index = 0; index < slots.size(); index++) result.add(slots.copy(slots.get(index)));
    return result;
  }

  private boolean restore(List<T> before) {
    boolean restored = true;
    for (int index = 0; index < before.size(); index++)
      try { slots.set(index, slots.copy(before.get(index))); }
      catch (RuntimeException error) { restored = false; slots.rollbackError(index, error); }
    try { slots.setChanged(); }
    catch (RuntimeException error) { restored = false; slots.rollbackError(-1, error); }
    return restored;
  }

  public interface Slots<T> {
    int size();
    T get(int index);
    void set(int index, T value);
    T copy(T value);
    boolean isEmpty(T value);
    boolean matches(T value, T template);
    int count(T value);
    void setCount(T value, int count);
    int maxStackSize(T value);
    void setChanged();
    void rollbackError(int index, RuntimeException error);
  }
}
