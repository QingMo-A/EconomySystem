package com.mo.economy_system.common.delivery;

import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class DeliveryBoxTestFixtures {
  private DeliveryBoxTestFixtures() {}

  public static ItemStackSnapshot item(int count) {
    NbtData.Compound customData = NbtData.compoundBuilder()
        .putString("owner", "snapshot")
        .build();
    return ItemStackSnapshot.create(
            "minecraft:diamond_sword",
            count,
            Optional.of("{\"text\":\"Bridge Blade\"}"),
            List.of("{\"text\":\"line one\"}", "{\"text\":\"line two\"}"),
            Map.of("minecraft:sharpness", 3),
            Map.of(),
            true,
            true,
            7,
            2,
            true,
            true,
            OptionalInt.empty(),
            true,
            OptionalInt.of(42),
            customData)
        .orElseThrow();
  }

  public static DeliveryBoxEntrySnapshot entry(UUID id, int count) {
    return new DeliveryBoxEntrySnapshot(id, item(count), "market.order");
  }
}
