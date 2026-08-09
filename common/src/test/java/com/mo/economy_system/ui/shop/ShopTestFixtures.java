package com.mo.economy_system.ui.shop;

import com.mo.economy_system.common.network.ShopItemSnapshot;
import java.util.List;
import java.util.stream.IntStream;

final class ShopTestFixtures {
  private ShopTestFixtures() {}

  static ShopItemSnapshot item(int index) {
    return new ShopItemSnapshot("shop-" + index, "minecraft:stone", 25, 20 + index, 19 + index,
        "stone item " + index, 0.1, "", "", 0, 64, 64);
  }

  static List<ShopItemSnapshot> items(int count) {
    return IntStream.range(0, count).mapToObj(ShopTestFixtures::item).toList();
  }
}
