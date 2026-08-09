package com.mo.economy_system.common.delivery;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.platform.item.ItemStackSnapshotCodec;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeliveryBoxEntryCodecTest {
  @Test
  void schemaOneGoldenStructureRoundTrips() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(
        UUID.fromString("00112233-4455-6677-8899-aabbccddeeff"), 5);
    NbtData.Compound encoded = DeliveryBoxEntryCodec.encode(entry);
    assertEquals(DeliveryBoxEntryCodec.SCHEMA_VERSION, intValue(encoded, "schemaVersion"));
    assertEquals(entry.entryId(), NbtData.readUuid(encoded.get("entryId")));
    assertEquals("market.order", stringValue(encoded, "source"));
    assertEquals(Set.of("schemaVersion", "entryId", "item", "source"), encoded.keys());
    assertEquals(entry, DeliveryBoxEntryCodec.decode(encoded));
  }

  @Test
  void readsLegacyCompactEntryAndAlwaysWritesVersionedSchema() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 2);
    NbtData.Compound legacy = legacy(entry, entry.item().itemId());
    DeliveryBoxEntrySnapshot decoded = DeliveryBoxEntryCodec.decode(legacy);
    assertEquals(entry, decoded);
    assertTrue(DeliveryBoxEntryCodec.encode(decoded).contains("schemaVersion"));
  }

  @Test
  void rejectsUnknownVersionFieldsAndLegacyItemMismatch() {
    NbtData.Compound encoded = DeliveryBoxEntryCodec.encode(
        DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1));
    NbtData.Compound futureVersion = encoded.with("schemaVersion", NbtData.intValue(2));
    assertThrows(IllegalArgumentException.class, () -> DeliveryBoxEntryCodec.decode(futureVersion));

    NbtData.Compound unknownField = encoded.with("future", NbtData.string("x"));
    assertThrows(IllegalArgumentException.class, () -> DeliveryBoxEntryCodec.decode(unknownField));

    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    NbtData.Compound mismatch = legacy(entry, "minecraft:stone");
    assertThrows(IllegalArgumentException.class, () -> DeliveryBoxEntryCodec.decode(mismatch));
  }

  @Test
  void encodedAndDecodedNbtAreImmutable() {
    DeliveryBoxEntrySnapshot entry = DeliveryBoxTestFixtures.entry(UUID.randomUUID(), 1);
    NbtData.Compound encoded = DeliveryBoxEntryCodec.encode(entry);
    DeliveryBoxEntrySnapshot decoded = DeliveryBoxEntryCodec.decode(encoded);
    NbtData.Compound changedItem = ((NbtData.Compound) encoded.get("item"))
        .with("id", NbtData.string("minecraft:dirt"));
    NbtData.Compound changed = encoded.with("item", changedItem);

    assertEquals("minecraft:diamond_sword", decoded.item().itemId());
    assertEquals("minecraft:dirt", stringValue((NbtData.Compound) changed.get("item"), "id"));
    assertThrows(UnsupportedOperationException.class,
        () -> decoded.item().customData().values().put("owner", NbtData.string("changed")));
    assertEquals("snapshot", stringValue(decoded.item().customData(), "owner"));
  }

  private static NbtData.Compound legacy(DeliveryBoxEntrySnapshot entry, String itemId) {
    return NbtData.compoundBuilder()
        .putUuid("dataID", entry.entryId())
        .putString("itemID", itemId)
        .put("itemStack", ItemStackSnapshotCodec.encode(entry.item()).orElseThrow())
        .putString("source", entry.source())
        .build();
  }

  private static int intValue(NbtData.Compound tag, String key) {
    return ((NbtData.IntValue) tag.get(key)).value();
  }

  private static String stringValue(NbtData.Compound tag, String key) {
    return ((NbtData.StringValue) tag.get(key)).value();
  }
}
