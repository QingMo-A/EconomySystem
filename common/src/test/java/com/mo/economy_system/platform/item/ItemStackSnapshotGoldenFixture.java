package com.mo.economy_system.platform.item;

import com.mo.economy_system.platform.nbt.NbtData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/** One canonical schema-v1 fixture shared by both target test suites. */
public final class ItemStackSnapshotGoldenFixture {
    private ItemStackSnapshotGoldenFixture() {}

    public static ItemStackSnapshot snapshot() {
        LinkedHashMap<String, Integer> enchantments = new LinkedHashMap<>();
        enchantments.put("minecraft:protection", 4);
        enchantments.put("minecraft:unbreaking", 3);
        NbtData.Compound customData = NbtData.compoundBuilder()
                .putString("economyOwner", "alice")
                .build();
        return ItemStackSnapshot.create("minecraft:leather_chestplate", 1,
                Optional.of("{\"text\":\"Golden Coat\"}"),
                List.of("{\"text\":\"First line\"}", "{\"text\":\"Second line\"}"),
                enchantments, Map.of("minecraft:mending", 1), false, false, 7, 4,
                true, false, OptionalInt.of(0x336699), false, OptionalInt.of(42), customData).orElseThrow();
    }

    public static NbtData.Compound schema() {
        return ItemStackSnapshotCodec.encode(snapshot()).orElseThrow();
    }
}
