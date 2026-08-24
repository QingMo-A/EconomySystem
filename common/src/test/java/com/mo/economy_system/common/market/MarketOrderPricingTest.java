package com.mo.economy_system.common.market;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class MarketOrderPricingTest {
  @Test
  void exactIntegerUnitPriceSupportsPartialFill() {
    MarketOrder order = order(64, 1_280);
    assertEquals(20, MarketOrderPricing.exactUnitPrice(order).orElseThrow());
    assertTrue(MarketOrderPricing.supportsPartialFill(order));
    assertEquals(200, MarketOrderPricing.fillAmount(order, 10));
  }

  @Test
  void nonDivisibleLegacyOrderIsWholeOnly() {
    MarketOrder order = order(3, 10);
    assertTrue(MarketOrderPricing.exactUnitPrice(order).isEmpty());
    assertFalse(MarketOrderPricing.supportsPartialFill(order));
    assertThrows(IllegalArgumentException.class, () -> MarketOrderPricing.fillAmount(order, 1));
    assertEquals(10, MarketOrderPricing.fillAmount(order, 3));
  }

  @Test
  void invalidFillQuantitiesAreRejected() {
    MarketOrder order = order(5, 100);
    assertThrows(IllegalArgumentException.class, () -> MarketOrderPricing.fillAmount(order, 0));
    assertThrows(IllegalArgumentException.class, () -> MarketOrderPricing.fillAmount(order, 6));
  }

  private static MarketOrder order(int quantity, int totalPrice) {
    return new MarketOrder(MarketOrderType.SALES, UUID.randomUUID(), MarketOrderCodecTest.item(),
        quantity, totalPrice, "seller", UUID.randomUUID(), 1, 2, false);
  }
}
