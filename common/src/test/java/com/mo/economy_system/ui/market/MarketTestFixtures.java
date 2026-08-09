package com.mo.economy_system.ui.market;

import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.MarketOrderSnapshot;
import com.mo.economy_system.platform.item.ItemStackSnapshot;
import com.mo.economy_system.platform.nbt.NbtData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.stream.IntStream;

final class MarketTestFixtures {
  static final UUID VIEWER = new UUID(1, 1);

  private MarketTestFixtures() {}

  static List<MarketOrderSnapshot> orders(int count) {
    return IntStream.range(0, count).mapToObj(MarketTestFixtures::sales).toList();
  }

  static MarketOrderSnapshot sales(int index) {
    return order(index, MarketOrderType.SALES, new UUID(2, index + 1), false);
  }

  static MarketOrderSnapshot order(int index, MarketOrderType type, UUID ownerId, boolean delivered) {
    return new MarketOrderSnapshot(type, new UUID(3, index + 1), item(index), index + 1,
        (index + 1) * 20, "owner-" + index, ownerId, 1, 2, delivered);
  }

  private static ItemStackSnapshot item(int index) {
    return ItemStackSnapshot.create("minecraft:stone", 1, Optional.empty(), List.of(), Map.of(), Map.of(),
        true, true, 0, 0, false, true, OptionalInt.empty(), true, OptionalInt.empty(), NbtData.emptyCompound())
        .orElseThrow();
  }
}
