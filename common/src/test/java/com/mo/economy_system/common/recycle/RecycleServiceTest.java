package com.mo.economy_system.common.recycle;

import com.mo.economy_system.core.economy_system.BalanceMutationResult;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RecycleServiceTest {
  private static final ItemStackSnapshot KELP = item("minecraft:kelp");
  private static final UUID PLAYER = UUID.randomUUID();

  @Test
  void highPriceConsumesQuotaAndThenFallsBack() {
    FakeInventory inventory = new FakeInventory(20);
    FakeEconomy economy = new FakeEconomy();
    RecycleService service = new RecycleService(
        new RecycleConfig(Duration.ofHours(1), new RecycleOffer("minecraft:kelp", 1, 3, 5, true)),
        inventory, economy);

    RecycleResult first = service.recycle(PLAYER, KELP, 4, UUID.randomUUID(), 0);
    assertEquals(RecycleResult.Status.SUCCESS, first.status());
    assertEquals(4, first.acceptedAmount());
    assertEquals(12, first.payout());
    assertEquals(1, first.highQuotaRemaining());
    RecycleResult second = service.recycle(PLAYER, KELP, 4, UUID.randomUUID(), 0);
    assertEquals(4, second.acceptedAmount());
    assertEquals(6, second.payout()); // one remaining high-price slot, then base price
    assertEquals(0, second.highQuotaRemaining());
    assertEquals(18, economy.balance);
    assertEquals(12, inventory.count);
  }

  @Test
  void duplicateSubmissionDoesNotRemoveOrPayTwice() {
    FakeInventory inventory = new FakeInventory(10);
    FakeEconomy economy = new FakeEconomy();
    RecycleService service = new RecycleService(new RecycleConfig(Duration.ofHours(1), new RecycleOffer("minecraft:kelp", 2)), inventory, economy);
    UUID submission = UUID.randomUUID();
    assertTrue(service.recycle(PLAYER, KELP, 3, submission, 0).success());
    RecycleResult duplicate = service.recycle(PLAYER, KELP, 3, submission, 0);
    assertEquals(RecycleResult.Status.DUPLICATE_SUBMISSION, duplicate.status());
    assertEquals(7, inventory.count);
    assertEquals(6, economy.balance);
  }

  @Test
  void failedCreditRestoresItems() {
    FakeInventory inventory = new FakeInventory(10);
    FakeEconomy economy = new FakeEconomy();
    economy.result = BalanceMutationResult.BALANCE_LIMIT;
    RecycleService service = new RecycleService(new RecycleConfig(Duration.ofHours(1), new RecycleOffer("minecraft:kelp", 2)), inventory, economy);
    RecycleResult result = service.recycle(PLAYER, KELP, 3, UUID.randomUUID(), 0);
    assertEquals(RecycleResult.Status.BALANCE_LIMIT, result.status());
    assertEquals(10, inventory.count);
    assertEquals(0, economy.balance);
  }

  @Test
  void stopPolicyNeverDowngradesExcessToBasePrice() {
    FakeInventory inventory = new FakeInventory(10);
    FakeEconomy economy = new FakeEconomy();
    RecycleService service = new RecycleService(
        new RecycleConfig(Duration.ofHours(1), new RecycleOffer("minecraft:kelp", 1, 4, 2, false)),
        inventory, economy);
    RecycleResult partial = service.recycle(PLAYER, KELP, 3, UUID.randomUUID(), 0);
    assertTrue(partial.success());
    assertEquals(2, partial.acceptedAmount());
    assertEquals(8, partial.payout());
    RecycleResult exhausted = service.recycle(PLAYER, KELP, 1, UUID.randomUUID(), 0);
    assertEquals(RecycleResult.Status.HIGH_QUOTA_EXHAUSTED, exhausted.status());
    assertEquals(8, inventory.count);
  }

  private static ItemStackSnapshot item(String id) {
    return ItemStackSnapshot.create(id, 1, Optional.empty(), java.util.List.of(), Map.of(), Map.of(), true, true,
        0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), NbtData.emptyCompound()).orElseThrow();
  }

  private static final class FakeInventory implements RecycleService.InventoryPort {
    int count;
    FakeInventory(int count) { this.count = count; }
    public int count(UUID playerId, ItemStackSnapshot item) { return count; }
    public boolean remove(UUID playerId, ItemStackSnapshot item, int amount) {
      if (count < amount) return false;
      count -= amount;
      return true;
    }
    public void restore(UUID playerId, ItemStackSnapshot item, int amount) { count += amount; }
  }

  private static final class FakeEconomy implements RecycleService.EconomyPort {
    int balance;
    BalanceMutationResult result = BalanceMutationResult.SUCCESS;
    public BalanceMutationResult creditExact(UUID playerId, int amount, String category, String reason) {
      if (result == BalanceMutationResult.SUCCESS) balance += amount;
      return result;
    }
  }
}
