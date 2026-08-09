package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.TerritoryBuffCatalogPolicy;
import com.mo.economy_system.common.territory.TerritoryClaimService;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class Forge1201TerritoryBuffCatalogTest {
  private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @AfterEach
  void clearCatalog() {
    Forge1201TerritorySnapshotStore.clearBuffCatalog();
  }

  @Test
  void synchronizationAddsConfiguredBuffsAndPreservesUnknownNbt() {
    CompoundTag territory = validTerritory();
    territory.putString("FutureField", "keep");
    CompoundTag root = new CompoundTag();
    ListTag records = new ListTag();
    records.add(territory);
    root.put("Territories", records);
    AtomicInteger dirtyCalls = new AtomicInteger();
    Forge1201TerritorySnapshotStore store = Forge1201TerritorySnapshotStore.load(
        root, dirtyCalls::incrementAndGet);

    var definition = definition("speed", "Speed", "minecraft:speed", 0, 3);
    assertEquals(
        Forge1201TerritorySnapshotStore.BuffCatalogSyncResult.UPDATED,
        store.synchronizeBuffCatalog(List.of(definition)));

    CompoundTag saved = store.rawCopy();
    CompoundTag savedTerritory = saved.getList("Territories", Tag.TAG_COMPOUND).getCompound(0);
    assertEquals("keep", savedTerritory.getString("FutureField"));
    assertEquals("speed", savedTerritory.getList("TerritoryBuffs", Tag.TAG_COMPOUND)
        .getCompound(0).getString("id"));
    assertEquals(1, dirtyCalls.get());
  }

  @Test
  void newlyCreatedTerritoriesReceiveTheConfiguredCatalog() {
    Forge1201TerritorySnapshotStore.configureBuffCatalog(List.of(
        definition("speed", "Speed", "minecraft:speed", 0, 3)));
    Forge1201TerritorySnapshotStore store = new Forge1201TerritorySnapshotStore(
        List.of(), () -> {});
    TerritoryClaimService.Request request = new TerritoryClaimService.Request(
        OWNER, "Owner", "Home", "minecraft:overworld",
        new Position(0, 64, 0), new Position(2, 64, 2));

    assertEquals(
        TerritoryClaimService.RepositoryResult.CREATED,
        store.create(request, 9, 90));
    assertEquals(1, store.allSnapshots().get(0).buffs().size());
    assertTrue(store.allSnapshots().get(0).buffs().get(0).level() == 0);
  }

  private static TerritoryBuffCatalogPolicy.Definition definition(
      String id, String text, String effect, int initialLevel, int maxLevel) {
    return new TerritoryBuffCatalogPolicy.Definition(
        id, text, effect, false, initialLevel, 1, maxLevel,
        List.of(new BuffUpgradeCost(List.of(), 0, 0)));
  }

  private static CompoundTag validTerritory() {
    CompoundTag tag = new CompoundTag();
    tag.putUUID("TerritoryID", UUID.fromString("10000000-0000-0000-0000-000000000001"));
    tag.putUUID("OwnerUUID", OWNER);
    tag.putString("OwnerName", "Owner");
    tag.putString("Name", "Home");
    tag.putString("Dimension", "minecraft:overworld");
    tag.putInt("X1", 0);
    tag.putInt("Y1", 64);
    tag.putInt("Z1", 0);
    tag.putInt("X2", 10);
    tag.putInt("Y2", 80);
    tag.putInt("Z2", 10);
    tag.put("AuthorizedPlayers", new ListTag());
    return tag;
  }
}
