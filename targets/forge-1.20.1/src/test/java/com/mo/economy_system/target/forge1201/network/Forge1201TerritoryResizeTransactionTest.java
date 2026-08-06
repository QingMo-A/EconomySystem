package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class Forge1201TerritoryResizeTransactionTest {
  private static final UUID TERRITORY =
      UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID OWNER =
      UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void successfulExpansionChargesAuthoritativeAreaDifference() {
    EconomySavedData accounts = new EconomySavedData();
    accounts.setBalance(OWNER, 2_000);
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(root());
    var outcome = execute(accounts, store, new Position(0, 64, 0), new Position(12, 64, 12));
    assertEquals(Forge1201TerritoryResizeTransaction.Result.SUCCESS, outcome.result());
    assertEquals(1_040, accounts.getBalance(OWNER));
    assertEquals(12, store.rawCopy().getList("Territories", 10).getCompound(0).getInt("X2"));
  }

  @Test
  void persistenceFailureRefundsBecauseRollbackWasProven() {
    EconomySavedData accounts = new EconomySavedData();
    accounts.setBalance(OWNER, 2_000);
    AtomicInteger marks = new AtomicInteger();
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(
        root(),
        () -> {
          if (marks.getAndIncrement() == 0) throw new IllegalStateException("dirty");
        });
    var outcome = execute(accounts, store, new Position(0, 64, 0), new Position(12, 64, 12));
    assertEquals(Forge1201TerritoryResizeTransaction.Result.PERSIST_FAILED, outcome.result());
    assertEquals(2_000, accounts.getBalance(OWNER));
    assertEquals(2, marks.get());
  }

  @Test
  void uncertainMutationDoesNotBlindlyRefund() {
    EconomySavedData accounts = new EconomySavedData();
    accounts.setBalance(OWNER, 2_000);
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(
        root(),
        () -> {
          throw new IllegalStateException("dirty");
        });
    var outcome = execute(accounts, store, new Position(0, 64, 0), new Position(12, 64, 12));
    assertEquals(Forge1201TerritoryResizeTransaction.Result.STATE_UNKNOWN, outcome.result());
    assertEquals(1_040, accounts.getBalance(OWNER));
  }

  @Test
  void insufficientFundsNeverMutateTerritory() {
    EconomySavedData accounts = new EconomySavedData();
    accounts.setBalance(OWNER, 10);
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(root());
    CompoundTag before = store.rawCopy();
    var outcome = execute(accounts, store, new Position(0, 64, 0), new Position(12, 64, 12));
    assertEquals(
        Forge1201TerritoryResizeTransaction.Result.INSUFFICIENT_FUNDS, outcome.result());
    assertEquals(10, accounts.getBalance(OWNER));
    assertEquals(before, store.rawCopy());
  }

  private static Forge1201TerritoryResizeTransaction.Outcome execute(
      EconomySavedData accounts,
      Forge1201TerritorySnapshotStore store,
      Position first,
      Position second) {
    return Forge1201TerritoryResizeTransaction.execute(
        accounts,
        store,
        OWNER,
        TERRITORY,
        "minecraft:overworld",
        first,
        second,
        (stage, player, territory, failure) -> {});
  }

  private static CompoundTag root() {
    CompoundTag territory = new CompoundTag();
    territory.putUUID("TerritoryID", TERRITORY);
    territory.putUUID("OwnerUUID", OWNER);
    territory.putString("OwnerName", "Owner");
    territory.putString("Name", "Home");
    territory.putString("Dimension", "minecraft:overworld");
    territory.putInt("X1", 0);
    territory.putInt("Y1", 64);
    territory.putInt("Z1", 0);
    territory.putInt("X2", 10);
    territory.putInt("Y2", 64);
    territory.putInt("Z2", 10);
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(territory);
    root.put("Territories", records);
    return root;
  }
}
