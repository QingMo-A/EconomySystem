package com.mo.economy_system.platform.item;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackSnapshotCodecTest {
    @Test
    void goldenSchemaV1ContainsStableFieldsAndAllSupportedComponents() {
        CompoundTag encoded = ItemStackSnapshotCodec.encode(fullSnapshot()).orElseThrow();
        assertEquals("{components:{customData:{economyOwner:\"alice\"},customModelData:42,customName:'{\"text\":\"Ledger Blade\"}',damage:7,dyedColor:{rgb:3368601,showInTooltip:1b},enchantments:{entries:[{id:\"minecraft:sharpness\",level:5},{id:\"minecraft:unbreaking\",level:3}],showInTooltip:1b},lore:['{\"text\":\"Line one\"}','{\"text\":\"Line two\"}'],repairCost:4,storedEnchantments:{entries:[{id:\"minecraft:mending\",level:1}],showInTooltip:0b},unbreakable:{showInTooltip:0b}},count:2,id:\"minecraft:diamond_sword\",schemaVersion:1}", encoded.toString());
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(encoded).orElseThrow();
        assertSnapshotEquals(fullSnapshot(), decoded);
    }

    @Test
    void supportsOrdinaryAndMultiCountSnapshots() {
        ItemStackSnapshot ordinary = empty("minecraft:stone", 1, new CompoundTag());
        assertSnapshotEquals(ordinary, ItemStackSnapshotCodec.decode(ItemStackSnapshotCodec.encode(ordinary).orElseThrow()).orElseThrow());
        ItemStackSnapshot multiple = empty("minecraft:stone", 32, new CompoundTag());
        assertEquals(32, ItemStackSnapshotCodec.decode(ItemStackSnapshotCodec.encode(multiple).orElseThrow()).orElseThrow().count());
    }

    @Test
    void readsLegacyCompactButAlwaysWritesV1() {
        CompoundTag legacy = new CompoundTag();
        legacy.putString("id", "minecraft:stone");
        legacy.putInt("count", 3);
        CompoundTag data = new CompoundTag();
        data.putString("owner", "alice");
        legacy.put("customData", data);
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(legacy).orElseThrow();
        assertEquals("alice", decoded.customData().getString("owner"));
        CompoundTag rewritten = ItemStackSnapshotCodec.encode(decoded).orElseThrow();
        assertEquals(1, rewritten.getInt("schemaVersion"));
        assertTrue(rewritten.getCompound("components").contains("customData"));
    }

    @Test
    void rejectsUnknownVersionUnknownComponentAndMalformedFields() {
        CompoundTag future = ItemStackSnapshotCodec.encode(empty("minecraft:stone", 1, new CompoundTag())).orElseThrow();
        future.putInt("schemaVersion", 99);
        assertError(future, ItemStackSnapshotError.UNSUPPORTED_SCHEMA_VERSION);

        CompoundTag unknown = ItemStackSnapshotCodec.encode(empty("minecraft:stone", 1, new CompoundTag())).orElseThrow();
        unknown.getCompound("components").putBoolean("mystery", true);
        assertError(unknown, ItemStackSnapshotError.UNSUPPORTED_COMPONENT);

        CompoundTag invalid = ItemStackSnapshotCodec.encode(empty("minecraft:stone", 1, new CompoundTag())).orElseThrow();
        invalid.putString("count", "many");
        assertError(invalid, ItemStackSnapshotError.INVALID_COUNT);

        CompoundTag zero = ItemStackSnapshotCodec.encode(empty("minecraft:stone", 1, new CompoundTag())).orElseThrow();
        zero.putInt("count", 0);
        assertError(zero, ItemStackSnapshotError.INVALID_COUNT);

        CompoundTag negative = ItemStackSnapshotCodec.encode(empty("minecraft:stone", 1, new CompoundTag())).orElseThrow();
        negative.putInt("count", -2);
        assertError(negative, ItemStackSnapshotError.INVALID_COUNT);
    }

    @Test
    void snapshotAndEncodedNbtAreDefensivelyCopied() {
        CompoundTag source = new CompoundTag();
        source.putInt("value", 1);
        java.util.ArrayList<String> lore = new java.util.ArrayList<>(List.of("{\"text\":\"safe\"}"));
        LinkedHashMap<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:sharpness", 1);
        ItemStackSnapshot snapshot = ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), lore,
                enchantments, Map.of(), true, true, 0, 0, false, true,
                OptionalInt.empty(), true, OptionalInt.empty(), source).orElseThrow();
        source.putInt("value", 2);
        lore.clear();
        enchantments.clear();
        assertEquals(1, snapshot.customData().getInt("value"));
        assertEquals(1, snapshot.loreJson().size());
        assertEquals(1, snapshot.enchantments().size());
        CompoundTag leaked = snapshot.customData();
        leaked.putInt("value", 3);
        assertEquals(1, snapshot.customData().getInt("value"));

        CompoundTag encoded = ItemStackSnapshotCodec.encode(snapshot).orElseThrow();
        CompoundTag postEncodeView = snapshot.customData();
        postEncodeView.putInt("value", 99);
        assertEquals(1, encoded.getCompound("components").getCompound("customData").getInt("value"));
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(encoded).orElseThrow();
        encoded.getCompound("components").getCompound("customData").putInt("value", 4);
        assertEquals(1, decoded.customData().getInt("value"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.loreJson().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.enchantments().put("x", 2));
    }

    @Test
    void invalidItemIdRemainsTargetValidationResponsibility() {
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(
                ItemStackSnapshotCodec.encode(empty("missing_mod:not_here", 1, new CompoundTag())).orElseThrow()).orElseThrow();
        assertEquals("missing_mod:not_here", decoded.itemId());
    }

    private static ItemStackSnapshot fullSnapshot() {
        LinkedHashMap<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:sharpness", 5);
        enchantments.put("minecraft:unbreaking", 3);
        CompoundTag data = new CompoundTag();
        data.putString("economyOwner", "alice");
        return ItemStackSnapshot.create("minecraft:diamond_sword", 2, Optional.of("{\"text\":\"Ledger Blade\"}"),
                List.of("{\"text\":\"Line one\"}", "{\"text\":\"Line two\"}"), enchantments,
                Map.of("minecraft:mending", 1), true, false, 7, 4, true, false,
                OptionalInt.of(0x336699), true, OptionalInt.of(42), data).orElseThrow();
    }

    private static ItemStackSnapshot empty(String id, int count, CompoundTag data) {
        return ItemStackSnapshot.create(id, count, Optional.empty(), List.of(), Map.of(), Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), data).orElseThrow();
    }

    private static void assertError(CompoundTag tag, ItemStackSnapshotError error) {
        ItemStackSnapshotResult<ItemStackSnapshot> result = ItemStackSnapshotCodec.decode(tag);
        assertFalse(result.isSuccess());
        assertEquals(error, result.error().orElseThrow());
    }

    private static void assertSnapshotEquals(ItemStackSnapshot expected, ItemStackSnapshot actual) {
        assertEquals(expected.itemId(), actual.itemId());
        assertEquals(expected.count(), actual.count());
        assertEquals(expected.customNameJson(), actual.customNameJson());
        assertEquals(expected.loreJson(), actual.loreJson());
        assertEquals(expected.enchantments(), actual.enchantments());
        assertEquals(expected.storedEnchantments(), actual.storedEnchantments());
        assertEquals(expected.enchantmentsShown(), actual.enchantmentsShown());
        assertEquals(expected.storedEnchantmentsShown(), actual.storedEnchantmentsShown());
        assertEquals(expected.damage(), actual.damage());
        assertEquals(expected.repairCost(), actual.repairCost());
        assertEquals(expected.unbreakable(), actual.unbreakable());
        assertEquals(expected.unbreakableShown(), actual.unbreakableShown());
        assertEquals(expected.dyedColor(), actual.dyedColor());
        assertEquals(expected.dyedColorShown(), actual.dyedColorShown());
        assertEquals(expected.customModelData(), actual.customModelData());
        assertEquals(expected.customData(), actual.customData());
    }
}
