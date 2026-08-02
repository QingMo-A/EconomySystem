package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class TransactionalInventoryContractTest {
  @Test void insertionFillsExistingStacksThenEmptySlotsAndRollsBack() {
    Slots slots = new Slots(stack("a", 60), stack("", 0), stack("", 0));
    InventoryInsertionResult result = tx(slots).insert(stack("a", 1), 70);
    assertTrue(result.succeeded()); assertEquals(List.of(64, 64, 2), slots.counts());
    assertTrue(result.rollback().rollback()); assertEquals(List.of(60, 0, 0), slots.counts());
  }

  @Test void insufficientInsertionRestoresEverySlot() {
    Slots slots = new Slots(stack("a", 63), stack("b", 64));
    InventoryInsertionResult result = tx(slots).insert(stack("a", 1), 2);
    assertFalse(result.succeeded()); assertTrue(result.failureRestored()); assertEquals(List.of(63, 64), slots.counts());
  }

  @Test void removalAggregatesStacksAndSuccessfulRollbackRestoresAll() {
    Slots slots = new Slots(stack("a", 2), stack("b", 8), stack("a", 5));
    InventoryRemovalResult result = tx(slots).remove(stack("a", 1), 6);
    assertTrue(result.succeeded()); assertEquals(List.of(0, 8, 1), slots.counts());
    assertTrue(result.rollback().orElseThrow().rollback()); assertEquals(List.of(2, 8, 5), slots.counts());
  }

  @Test void insufficientRemovalRestoresAllSlots() {
    Slots slots = new Slots(stack("a", 2), stack("a", 3));
    InventoryRemovalResult result = tx(slots).remove(stack("a", 1), 6);
    assertFalse(result.succeeded()); assertTrue(result.failureRestored()); assertEquals(List.of(2, 3), slots.counts());
  }

  @Test void countUsesLongWithoutOverflow() {
    Slots slots = new Slots(stack("a", Integer.MAX_VALUE), stack("a", Integer.MAX_VALUE));
    assertEquals(2L * Integer.MAX_VALUE, tx(slots).countMatching(stack("a", 1)));
  }

  @Test void rollbackContinuesAfterSlotAndSetChangedFailures() {
    Slots slots = new Slots(stack("a", 3), stack("a", 4)); InventoryRemovalResult result = tx(slots).remove(stack("a", 1), 2);
    slots.failSet.add(0); slots.failChanged = true;
    assertFalse(result.rollback().orElseThrow().rollback()); assertEquals(4, slots.values.get(1).count); assertEquals(2, slots.errors);
  }

  private static SlotInventoryTransactions<Stack> tx(Slots slots) { return new SlotInventoryTransactions<>(slots); }
  private static Stack stack(String id, int count) { return new Stack(id, count); }

  private static final class Stack { final String id; int count; Stack(String id, int count) { this.id = id; this.count = count; } }
  private static final class Slots implements SlotInventoryTransactions.Slots<Stack> {
    final List<Stack> values; final Set<Integer> failSet = new HashSet<>(); boolean failChanged; int errors;
    Slots(Stack... values) { this.values = new ArrayList<>(Arrays.asList(values)); }
    public int size() { return values.size(); } public Stack get(int i) { return values.get(i); }
    public void set(int i, Stack value) { if (failSet.contains(i)) throw new IllegalStateException(); values.set(i, value); }
    public Stack copy(Stack value) { return new Stack(value.id, value.count); }
    public boolean isEmpty(Stack value) { return value.count == 0; }
    public boolean matches(Stack value, Stack template) { return value.id.equals(template.id); }
    public int count(Stack value) { return value.count; } public void setCount(Stack value, int count) { value.count = count; }
    public int maxStackSize(Stack value) { return 64; }
    public void setChanged() { if (failChanged) throw new IllegalStateException(); }
    public void rollbackError(int index, RuntimeException error) { errors++; }
    List<Integer> counts() { return values.stream().map(value -> value.count).toList(); }
  }
}
