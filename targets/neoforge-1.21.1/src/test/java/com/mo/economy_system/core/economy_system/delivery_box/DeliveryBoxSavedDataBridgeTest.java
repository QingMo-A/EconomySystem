package com.mo.economy_system.core.economy_system.delivery_box;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.delivery.DeliveryBoxEntryCodec;
import com.mo.economy_system.common.delivery.DeliveryBoxEntrySnapshot;
import com.mo.economy_system.common.delivery.DeliveryBoxTestFixtures;
import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.target.neoforge1211.item.NeoForge1211NbtAdapter;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;

class DeliveryBoxSavedDataBridgeTest {
  @Test
  void schemaOneAndLegacyCompactBothRoundTripToVersionedStorage() {
    UUID owner = UUID.randomUUID();
    DeliveryBoxEntrySnapshot versioned = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 3);
    DeliveryBoxEntrySnapshot legacyEntry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 4);
    CompoundTag legacy = new CompoundTag();
    legacy.putUUID("dataID", legacyEntry.entryId());
    legacy.putString("itemID", legacyEntry.item().itemId());
    legacy.put("itemStack", NeoForge1211NbtAdapter.toNative(
        ItemStackSnapshotCodec.encode(legacyEntry.item()).orElseThrow()));
    legacy.putString("source", legacyEntry.source());
    ListTag entries = new ListTag();
    entries.add(NeoForge1211NbtAdapter.toNative(DeliveryBoxEntryCodec.encode(versioned)));
    entries.add(legacy);
    CompoundTag root = new CompoundTag();
    root.put(owner.toString(), entries);

    DeliveryBoxSavedData data = DeliveryBoxSavedData.load(root, null);
    assertEquals(java.util.List.of(versioned, legacyEntry), data.ledger().list(owner));
    CompoundTag saved = data.save(new CompoundTag(), null);
    ListTag stored = saved.getList(owner.toString(), Tag.TAG_COMPOUND);
    assertEquals(2, stored.size());
    assertEquals(1, stored.getCompound(0).getInt("schemaVersion"));
    assertEquals(1, stored.getCompound(1).getInt("schemaVersion"));
  }

  @Test
  void malformedRootFailsClosedWithoutPartialLedger() {
    UUID owner = UUID.randomUUID();
    ListTag entries = new ListTag();
    entries.add(NeoForge1211NbtAdapter.toNative(DeliveryBoxEntryCodec.encode(
        DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1))));
    CompoundTag malformed = new CompoundTag();
    malformed.putString("schemaVersion", "wrong");
    entries.add(malformed);
    CompoundTag root = new CompoundTag();
    root.put(owner.toString(), entries);
    assertThrows(IllegalArgumentException.class, () -> DeliveryBoxSavedData.load(root, null));
  }
}
