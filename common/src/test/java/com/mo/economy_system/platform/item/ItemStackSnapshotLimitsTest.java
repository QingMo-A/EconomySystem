package com.mo.economy_system.platform.item;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ItemStackSnapshotLimitsTest {
    @Test
    void rejectsOversizedNameAndLoreWithLimitError() {
        assertLimit(create(Optional.of("x".repeat(ItemStackSnapshotLimits.MAX_CUSTOM_NAME_JSON_LENGTH + 1)), List.of(), Map.of(), new CompoundTag()));
        assertLimit(create(Optional.empty(), IntStream.range(0, ItemStackSnapshotLimits.MAX_LORE_LINES + 1).mapToObj(i -> "x").toList(), Map.of(), new CompoundTag()));
        assertLimit(create(Optional.empty(), List.of("x".repeat(ItemStackSnapshotLimits.MAX_LORE_LINE_JSON_LENGTH + 1)), Map.of(), new CompoundTag()));
        List<String> totalLore = IntStream.range(0, 5).mapToObj(i -> "x".repeat(7_000)).toList();
        assertLimit(create(Optional.empty(), totalLore, Map.of(), new CompoundTag()));
    }

    @Test
    void rejectsOversizedEnchantmentsAndIdsWithLimitError() {
        Map<String, Integer> tooMany = new LinkedHashMap<>();
        IntStream.range(0, ItemStackSnapshotLimits.MAX_ENCHANTMENTS + 1)
                .forEach(i -> tooMany.put("test:enchantment_" + i, 1));
        assertLimit(create(Optional.empty(), List.of(), tooMany, new CompoundTag()));
        assertLimit(create(Optional.empty(), List.of(), Map.of("x".repeat(ItemStackSnapshotLimits.MAX_ENCHANTMENT_ID_LENGTH + 1), 1), new CompoundTag()));
    }

    @Test
    void rejectsOversizedOrOvernestedCustomDataWithLimitError() {
        CompoundTag oversized = new CompoundTag();
        oversized.putString("payload", "x".repeat(ItemStackSnapshotLimits.MAX_CUSTOM_DATA_BYTES + 1));
        assertLimit(create(Optional.empty(), List.of(), Map.of(), oversized));

        CompoundTag root = new CompoundTag();
        CompoundTag cursor = root;
        for (int i = 1; i <= ItemStackSnapshotLimits.MAX_CUSTOM_DATA_DEPTH; i++) {
            CompoundTag child = new CompoundTag();
            cursor.put("child", child);
            cursor = child;
        }
        assertLimit(create(Optional.empty(), List.of(), Map.of(), root));
    }

    @Test
    void strictEncodeRejectsInvalidOrOversizedSnapshots() {
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, ItemStackSnapshotCodec.encode(null).error().orElseThrow());
        ItemStackSnapshotResult<ItemStackSnapshot> largeButIndividuallyValid = ItemStackSnapshot.create(
                "minecraft:stone", 1, Optional.of("x".repeat(8_000)),
                IntStream.range(0, 4).mapToObj(i -> "x".repeat(ItemStackSnapshotLimits.MAX_LORE_LINE_JSON_LENGTH)).toList(), Map.of(), Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), customData(25_000));
        assertLimit(largeButIndividuallyValid);
    }

    @Test
    void factoryRejectsStructuralInvalidity() {
        assertEquals(ItemStackSnapshotError.INVALID_COUNT, base("minecraft:stone", 0, Map.of(), 0, 0, OptionalInt.empty()).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, base("", 1, Map.of(), 0, 0, OptionalInt.empty()).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, base("minecraft:stone", 1, Map.of("", 1), 0, 0, OptionalInt.empty()).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, base("minecraft:stone", 1, Map.of("minecraft:sharpness", 0), 0, 0, OptionalInt.empty()).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, base("minecraft:stone", 1, Map.of(), -1, 0, OptionalInt.empty()).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, base("minecraft:stone", 1, Map.of(), 0, -1, OptionalInt.empty()).error().orElseThrow());
        assertEquals(ItemStackSnapshotError.INVALID_SCHEMA, base("minecraft:stone", 1, Map.of(), 0, 0, OptionalInt.of(0x1000000)).error().orElseThrow());
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> create(Optional<String> name, List<String> lore,
                                                                      Map<String, Integer> enchantments, CompoundTag data) {
        return ItemStackSnapshot.create("minecraft:stone", 1, name, lore, enchantments, Map.of(), true, true,
                0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), data);
    }

    private static ItemStackSnapshotResult<ItemStackSnapshot> base(String id, int count, Map<String, Integer> enchantments,
                                                                    int damage, int repair, OptionalInt color) {
        return ItemStackSnapshot.create(id, count, Optional.empty(), List.of(), enchantments, Map.of(), true, true,
                damage, repair, false, true, color, true, OptionalInt.empty(), new CompoundTag());
    }

    private static CompoundTag customData(int length) {
        CompoundTag tag = new CompoundTag();
        tag.putString("payload", "x".repeat(length));
        return tag;
    }

    private static void assertLimit(ItemStackSnapshotResult<?> result) {
        assertFalse(result.isSuccess());
        assertEquals(ItemStackSnapshotError.DATA_LIMIT_EXCEEDED, result.error().orElseThrow());
    }
}
