package com.mo.economy_system.platform.item;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.platform.nbt.NbtData;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ItemStackSnapshotCodecTest {
    @Test
    void goldenSchemaV1ContainsStableFieldsAndAllSupportedComponents() {
        NbtData.Compound encoded = ItemStackSnapshotCodec.encode(fullSnapshot()).orElseThrow();
        NbtData.Compound expected = NbtData.compoundBuilder()
                .putInt("schemaVersion", 1)
                .putString("id", "minecraft:diamond_sword")
                .putInt("count", 2)
                .put("components", NbtData.compoundBuilder()
                        .putString("customName", "{\"text\":\"Ledger Blade\"}")
                        .put("lore", NbtData.list(List.of(
                                NbtData.string("{\"text\":\"Line one\"}"),
                                NbtData.string("{\"text\":\"Line two\"}"))))
                        .put("enchantments", enchantments(true,
                                enchantment("minecraft:sharpness", 5),
                                enchantment("minecraft:unbreaking", 3)))
                        .put("storedEnchantments", enchantments(false,
                                enchantment("minecraft:mending", 1)))
                        .putInt("damage", 7)
                        .putInt("repairCost", 4)
                        .put("unbreakable", NbtData.compoundBuilder()
                                .putBoolean("showInTooltip", false).build())
                        .put("dyedColor", NbtData.compoundBuilder()
                                .putInt("rgb", 0x336699)
                                .putBoolean("showInTooltip", true).build())
                        .putInt("customModelData", 42)
                        .put("customData", NbtData.compoundBuilder()
                                .putString("economyOwner", "alice").build())
                        .build())
                .build();
        assertEquals(expected, encoded);
        assertSnapshotEquals(fullSnapshot(), ItemStackSnapshotCodec.decode(encoded).orElseThrow());
    }

    @Test
    void supportsOrdinaryAndMultiCountSnapshots() {
        ItemStackSnapshot ordinary = empty("minecraft:stone", 1, NbtData.emptyCompound());
        assertSnapshotEquals(ordinary,
                ItemStackSnapshotCodec.decode(ItemStackSnapshotCodec.encode(ordinary).orElseThrow()).orElseThrow());
        ItemStackSnapshot multiple = empty("minecraft:stone", 32, NbtData.emptyCompound());
        assertEquals(32, ItemStackSnapshotCodec.decode(
                ItemStackSnapshotCodec.encode(multiple).orElseThrow()).orElseThrow().count());
    }

    @Test
    void readsLegacyCompactButAlwaysWritesV1() {
        NbtData.Compound legacy = NbtData.compoundBuilder()
                .putString("id", "minecraft:stone")
                .putInt("count", 3)
                .put("customData", NbtData.compoundBuilder().putString("owner", "alice").build())
                .build();
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(legacy).orElseThrow();
        assertEquals("alice", stringValue(decoded.customData(), "owner"));
        NbtData.Compound rewritten = ItemStackSnapshotCodec.encode(decoded).orElseThrow();
        assertEquals(1, intValue(rewritten, "schemaVersion"));
        assertTrue(compoundValue(rewritten, "components").contains("customData"));
    }

    @Test
    void rejectsUnknownVersionUnknownComponentAndMalformedFields() {
        NbtData.Compound base = ItemStackSnapshotCodec.encode(
                empty("minecraft:stone", 1, NbtData.emptyCompound())).orElseThrow();
        assertError(base.with("schemaVersion", NbtData.intValue(99)),
                ItemStackSnapshotError.UNSUPPORTED_SCHEMA_VERSION);

        NbtData.Compound unknownComponents = compoundValue(base, "components")
                .with("mystery", NbtData.byteValue((byte) 1));
        assertError(base.with("components", unknownComponents),
                ItemStackSnapshotError.UNSUPPORTED_COMPONENT);
        assertError(base.with("count", NbtData.string("many")), ItemStackSnapshotError.INVALID_COUNT);
        assertError(base.with("count", NbtData.intValue(0)), ItemStackSnapshotError.INVALID_COUNT);
        assertError(base.with("count", NbtData.intValue(-2)), ItemStackSnapshotError.INVALID_COUNT);
    }

    @Test
    void snapshotCollectionsAndNbtTreesAreImmutable() {
        NbtData.Compound source = NbtData.compoundBuilder().putInt("value", 1).build();
        ArrayList<String> lore = new ArrayList<>(List.of("{\"text\":\"safe\"}"));
        LinkedHashMap<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:sharpness", 1);
        ItemStackSnapshot snapshot = ItemStackSnapshot.create(
                "minecraft:stone", 1, Optional.empty(), lore, enchantments, Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), source).orElseThrow();
        lore.clear();
        enchantments.clear();

        assertEquals(1, intValue(snapshot.customData(), "value"));
        assertEquals(1, snapshot.loreJson().size());
        assertEquals(1, snapshot.enchantments().size());
        assertThrows(UnsupportedOperationException.class,
                () -> snapshot.customData().values().put("value", NbtData.intValue(3)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.loreJson().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.enchantments().put("x", 2));

        NbtData.Compound encoded = ItemStackSnapshotCodec.encode(snapshot).orElseThrow();
        NbtData.Compound components = compoundValue(encoded, "components");
        NbtData.Compound changed = encoded.with("components", components.with(
                "customData", compoundValue(components, "customData").with("value", NbtData.intValue(4))));
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(encoded).orElseThrow();
        assertEquals(1, intValue(decoded.customData(), "value"));
        assertEquals(4, intValue(
                compoundValue(compoundValue(changed, "components"), "customData"), "value"));
    }

    @Test
    void invalidItemIdRemainsTargetValidationResponsibility() {
        ItemStackSnapshot decoded = ItemStackSnapshotCodec.decode(ItemStackSnapshotCodec.encode(
                empty("missing_mod:not_here", 1, NbtData.emptyCompound())).orElseThrow()).orElseThrow();
        assertEquals("missing_mod:not_here", decoded.itemId());
    }

    private static ItemStackSnapshot fullSnapshot() {
        LinkedHashMap<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:sharpness", 5);
        enchantments.put("minecraft:unbreaking", 3);
        NbtData.Compound data = NbtData.compoundBuilder().putString("economyOwner", "alice").build();
        return ItemStackSnapshot.create(
                "minecraft:diamond_sword", 2, Optional.of("{\"text\":\"Ledger Blade\"}"),
                List.of("{\"text\":\"Line one\"}", "{\"text\":\"Line two\"}"), enchantments,
                Map.of("minecraft:mending", 1), true, false, 7, 4, true, false,
                OptionalInt.of(0x336699), true, OptionalInt.of(42), data).orElseThrow();
    }

    private static ItemStackSnapshot empty(String id, int count, NbtData.Compound data) {
        return ItemStackSnapshot.create(
                id, count, Optional.empty(), List.of(), Map.of(), Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), data).orElseThrow();
    }

    private static NbtData.Compound enchantment(String id, int level) {
        return NbtData.compoundBuilder().putString("id", id).putInt("level", level).build();
    }

    private static NbtData.Compound enchantments(boolean shown, NbtData.Compound... entries) {
        return NbtData.compoundBuilder()
                .putBoolean("showInTooltip", shown)
                .put("entries", NbtData.list(List.of(entries)))
                .build();
    }

    private static int intValue(NbtData.Compound tag, String key) {
        return ((NbtData.IntValue) tag.get(key)).value();
    }

    private static String stringValue(NbtData.Compound tag, String key) {
        return ((NbtData.StringValue) tag.get(key)).value();
    }

    private static NbtData.Compound compoundValue(NbtData.Compound tag, String key) {
        return (NbtData.Compound) tag.get(key);
    }

    private static void assertError(NbtData.Compound tag, ItemStackSnapshotError error) {
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
