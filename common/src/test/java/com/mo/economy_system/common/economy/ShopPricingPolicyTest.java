package com.mo.economy_system.common.economy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.economy.ShopPricingPolicy.Config;
import com.mo.economy_system.common.economy.ShopPricingPolicy.Mode;
import com.mo.economy_system.common.network.ShopItemSnapshot;
import org.junit.jupiter.api.Test;

class ShopPricingPolicyTest {
  @Test
  void configurationAndInitialStockAreBoundedInCommon() {
    Config config = new Config(Double.NaN, -1, 2, -1, 2, -1, 0, -10, 2, -1);
    assertEquals(0.01D, config.minPriceMultiplier());
    assertEquals(0.01D, config.maxPriceMultiplier());
    assertEquals(1.0D, config.maxCycleChangeRate());
    assertEquals(1, config.defaultMaxStock());
    assertEquals(1, config.minMaxStock());

    ShopItemSnapshot initialized = ShopPricingPolicy.initialize(item(10, 10, 0, 0, 0), Config.defaults());
    assertEquals(512, initialized.maxVirtualStock());
    assertEquals(512, initialized.virtualStock());
  }

  @Test
  void purchaseStatisticsSaturateAndNeverUnderflowStock() {
    ShopItemSnapshot state = item(10, 10, Integer.MAX_VALUE - 1, 3, 512);
    ShopItemSnapshot purchased = ShopPricingPolicy.recordPurchase(state, 10, Config.defaults());
    assertEquals(Integer.MAX_VALUE, purchased.recentDemand());
    assertEquals(0, purchased.virtualStock());
  }

  @Test
  void demandAndStockModesShareCycleLimitsAndDecay() {
    Config config = Config.defaults();
    ShopItemSnapshot demand = ShopPricingPolicy.adjust(item(100, 100, 100, 512, 512), config, Mode.DEMAND);
    assertEquals(100, demand.lastPrice());
    assertEquals(108, demand.currentPrice());
    assertEquals(0.08D, demand.fluctuationFactor());
    assertEquals(65, demand.recentDemand());

    ShopItemSnapshot stock = ShopPricingPolicy.adjust(item(100, 100, 0, 1, 512), config, Mode.STOCK);
    assertEquals(108, stock.currentPrice());
    assertTrue(stock.virtualStock() > 1);
    assertTrue(stock.virtualStock() <= stock.maxVirtualStock());
    assertEquals(Mode.STOCK, Mode.parse(" STOCK ".trim()));
    assertEquals(Mode.DEMAND, Mode.parse("unknown"));
  }

  private static ShopItemSnapshot item(int basePrice, int currentPrice, int demand, int stock, int maxStock) {
    return new ShopItemSnapshot("shop", "minecraft:stone", basePrice, currentPrice, currentPrice,
        "stone", 0, "", "", demand, stock, maxStock);
  }
}
