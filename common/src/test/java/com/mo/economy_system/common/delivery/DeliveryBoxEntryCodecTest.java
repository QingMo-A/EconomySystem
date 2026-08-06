package com.mo.economy_system.common.delivery;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class DeliveryBoxEntryCodecTest {
  @Test
  void schemaOneGoldenStructureRoundTrips() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(
        UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), 5);
    CompoundTag encoded = DeliveryBoxEntryCodec.encode(entry);
    assertEquals(DeliveryBoxEntryCodec.SCHEMA_VERSION, encoded.getInt("schemaVersion"));
    assertEquals(entry.entryId(), encoded.getUUID("entryId"));
    assertEquals("market.order", encoded.getString("source"));
    assertEquals(
        java.util.Set.of("schemaVersion", "entryId", "item", "source"),
        encoded.getAllKeys());
    assertEquals(entry, DeliveryBoxEntryCodec.decode(encoded));
  }

  @Test
  void readsLegacyCompactEntryAndAlwaysWritesVersionedSchema() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 2);
    CompoundTag legacy = new CompoundTag();
    legacy.putUUID("dataID", entry.entryId());
    legacy.putString("itemID", entry.item().itemId());
    legacy.put("itemStack", ItemStackSnapshotCodec.encode(entry.item()).orElseThrow());
    legacy.putString("source", entry.source());
    DeliveryBoxEntrySnapshot decoded = DeliveryBoxEntryCodec.decode(legacy);
    assertEquals(entry, decoded);
    assertTrue(DeliveryBoxEntryCodec.encode(decoded).contains("schemaVersion"));
  }

  @Test
  void rejectsUnknownVersionFieldsAndLegacyItemMismatch() {
    CompoundTag futureVersion = DeliveryBoxEntryCodec.encode(
        DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1));
    futureVersion.putInt("schemaVersion", 2);
    assertThrows(
        IllegalArgumentException.class, () -> DeliveryBoxEntryCodec.decode(futureVersion));

    CompoundTag unknownField = DeliveryBoxEntryCodec.encode(
        DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1));
    unknownField.putString("future", "x");
    assertThrows(
        IllegalArgumentException.class, () -> DeliveryBoxEntryCodec.decode(unknownField));

    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    CompoundTag legacy = new CompoundTag();
    legacy.putUUID("dataID", entry.entryId());
    legacy.putString("itemID", "minecraft:stone");
    legacy.put("itemStack", ItemStackSnapshotCodec.encode(entry.item()).orElseThrow());
    legacy.putString("source", entry.source());
    assertThrows(IllegalArgumentException.class, () -> DeliveryBoxEntryCodec.decode(legacy));
  }

  @Test
  void inputAndOutputNbtAreDefensivelyCopied() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    CompoundTag encoded = DeliveryBoxEntryCodec.encode(entry);
    DeliveryBoxEntrySnapshot decoded = DeliveryBoxEntryCodec.decode(encoded);
    encoded.getCompound("item").putString("id", "minecraft:dirt");
    assertEquals("minecraft:diamond_sword", decoded.item().itemId());
    CompoundTag first = decoded.item().customData();
    first.putString("owner", "changed");
    assertEquals("snapshot", decoded.item().customData().getString("owner"));
  }
}
