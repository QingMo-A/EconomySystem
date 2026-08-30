package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.settings.EconomySettings;

/** Shared territory area pricing and overflow policy. */
public final class TerritoryPricing {
  public static final long DEFAULT_PRICE_PER_CELL = 20L;
  public static final long MAX_PRICE_PER_CELL = Integer.MAX_VALUE;

  private TerritoryPricing() {}

  /**
   * Returns the currently configured price.  Tests and early bootstrap code can use this before
   * the target platform initializes the settings store; in that case the historical default is
   * retained. Invalid or out-of-range persisted values also fail closed to the default.
   */
  public static long pricePerCell() {
    try {
      String raw = EconomySettings.get(EconomySettings.TERRITORY_PRICE_PER_CELL);
      long value = Long.parseLong(raw);
      return value >= 0 && value <= MAX_PRICE_PER_CELL ? value : DEFAULT_PRICE_PER_CELL;
    } catch (RuntimeException ignored) {
      return DEFAULT_PRICE_PER_CELL;
    }
  }

  public static long areaDifference(long oldArea, long newArea) {
    if (oldArea < 0 || newArea < 0) throw new IllegalArgumentException("negative area");
    return Math.subtractExact(newArea, oldArea);
  }

  public static long priceForArea(long area, long pricePerCell) {
    if (area < 0 || pricePerCell < 0) throw new IllegalArgumentException("negative price input");
    return Math.multiplyExact(area, pricePerCell);
  }

  /** UI/command preview policy: an unrepresentable price is displayed as the maximum value. */
  public static long saturatingPriceForArea(long area, long pricePerCell) {
    try {
      return priceForArea(area, pricePerCell);
    } catch (ArithmeticException overflow) {
      return Long.MAX_VALUE;
    }
  }

  /** Authoritative account charge for an expansion; shrinking and reshaping are free. */
  public static int expansionCharge(long oldArea, long newArea, long pricePerCell) {
    long difference = areaDifference(oldArea, newArea);
    if (difference <= 0) return 0;
    long charge = priceForArea(difference, pricePerCell);
    if (charge > Integer.MAX_VALUE) throw new ArithmeticException("territory charge overflow");
    return (int) charge;
  }
}
