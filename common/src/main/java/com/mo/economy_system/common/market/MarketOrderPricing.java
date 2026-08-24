package com.mo.economy_system.common.market;

import java.util.OptionalInt;

/** Shared integer-price rules for legacy and v2 market orders. */
public final class MarketOrderPricing {
  private MarketOrderPricing() {}

  /**
   * Returns the exact integer unit price when the persisted total is divisible by quantity.
   * Non-divisible legacy orders remain valid but must be traded as whole orders.
   */
  public static OptionalInt exactUnitPrice(MarketOrder order) {
    if (order == null || order.quantity() <= 0 || order.totalPrice() <= 0) return OptionalInt.empty();
    if (order.totalPrice() % order.quantity() != 0) return OptionalInt.empty();
    int unit = order.totalPrice() / order.quantity();
    return unit > 0 ? OptionalInt.of(unit) : OptionalInt.empty();
  }

  public static boolean supportsPartialFill(MarketOrder order) {
    return exactUnitPrice(order).isPresent();
  }

  public static int fillAmount(MarketOrder order, int fillQuantity) {
    if (fillQuantity <= 0 || order == null || fillQuantity > order.quantity()) {
      throw new IllegalArgumentException("invalid fill quantity");
    }
    OptionalInt unit = exactUnitPrice(order);
    if (unit.isEmpty()) {
      if (fillQuantity != order.quantity()) {
        throw new IllegalArgumentException("non-divisible legacy order only supports whole-order fill");
      }
      return order.totalPrice();
    }
    return Math.multiplyExact(unit.getAsInt(), fillQuantity);
  }
}
