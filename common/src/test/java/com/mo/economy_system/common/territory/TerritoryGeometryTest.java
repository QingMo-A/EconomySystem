package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TerritoryGeometryTest {
  @Test
  void areaIsInclusiveAndIndependentOfPointOrder() {
    assertEquals(1L, TerritoryGeometry.area(4, 8, 4, 8));
    assertEquals(12L, TerritoryGeometry.area(1, 2, 3, 5));
    assertEquals(12L, TerritoryGeometry.area(3, 5, 1, 2));
  }

  @Test
  void rectanglesUseClosedIntervalOverlap() {
    TerritoryGeometry.Rectangle first = TerritoryGeometry.rectangle(0, 0, 2, 2);
    assertTrue(first.contains(2, 2));
    assertTrue(first.intersects(TerritoryGeometry.rectangle(2, 2, 4, 4)));
    assertFalse(first.intersects(TerritoryGeometry.rectangle(3, 0, 4, 1)));
  }

  @Test
  void coordinateAndAreaOverflowPoliciesAreExplicit() {
    assertTrue(TerritoryGeometry.validCoordinate(30_000_000, -30_000_000));
    assertFalse(TerritoryGeometry.validCoordinate(30_000_001, 0));
    assertThrows(
        ArithmeticException.class,
        () ->
            TerritoryGeometry.area(
                Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
  }

  @Test
  void pricingSharesExpansionAndSaturationRules() {
    assertEquals(0, TerritoryPricing.expansionCharge(10, 9, 20));
    assertEquals(40, TerritoryPricing.expansionCharge(10, 12, 20));
    assertEquals(Long.MAX_VALUE, TerritoryPricing.saturatingPriceForArea(Long.MAX_VALUE, 2));
    assertThrows(
        ArithmeticException.class,
        () -> TerritoryPricing.expansionCharge(0, Integer.MAX_VALUE, Long.MAX_VALUE));
  }
}
