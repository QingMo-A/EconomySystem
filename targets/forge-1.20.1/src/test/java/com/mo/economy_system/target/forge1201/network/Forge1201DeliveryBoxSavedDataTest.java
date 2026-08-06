package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.delivery.DeliveryBoxEntryCodec;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class Forge1201DeliveryBoxSavedDataTest {
  @Test
  void versionedEntryRoundTripsAndWritesOnlySchemaOne() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 8);
    CompoundTag root = root(owner, DeliveryBoxEntryCodec.encode(entry));
    Forge1201DeliveryBoxSavedData data = Forge1201DeliveryBoxSavedData.load(root);
    assertEquals(java.util.List.of(entry), data.ledger().list(owner));
    CompoundTag saved = data.save(new CompoundTag());
    CompoundTag stored = saved.getList(owner.toString(), Tag.TAG_COMPOUND).getCompound(0);
    assertEquals(1, stored.getInt("schemaVersion"));
    assertEquals(entry, DeliveryBoxEntryCodec.decode(stored));
  }

  @Test
  void legacyCompactEntryLoadsAndIsUpgradedOnSave() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 2);
    CompoundTag legacy = new CompoundTag();
    legacy.putUUID("dataID", entry.entryId());
    legacy.putString("itemID", entry.item().itemId());
    legacy.put("itemStack", ItemStackSnapshotCodec.encode(entry.item()).orElseThrow());
    legacy.putString("source", entry.source());
    Forge1201DeliveryBoxSavedData data = Forge1201DeliveryBoxSavedData.load(root(owner, legacy));
    CompoundTag stored = data.save(new CompoundTag())
        .getList(owner.toString(), Tag.TAG_COMPOUND).getCompound(0);
    assertTrue(stored.contains("schemaVersion", Tag.TAG_INT));
    assertFalse(stored.contains("dataID"));
  }

  @Test
  void malformedOwnerAndEntryFailClosed() {
    CompoundTag invalidOwner = new CompoundTag();
    invalidOwner.put("not-a-uuid", new ListTag());
    assertThrows(
        IllegalArgumentException.class, () -> Forge1201DeliveryBoxSavedData.load(invalidOwner));

    CompoundTag invalidEntry = new CompoundTag();
    invalidEntry.putString("schemaVersion", "one");
    assertThrows(
        IllegalArgumentException.class,
        () -> Forge1201DeliveryBoxSavedData.load(root(UUID.randomUUID(), invalidEntry)));
  }

  private static CompoundTag root(UUID owner, CompoundTag entry) {
    CompoundTag root = new CompoundTag();
    ListTag entries = new ListTag();
    entries.add(entry);
    root.put(owner.toString(), entries);
    return root;
  }
}
