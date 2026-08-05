package com.mo.economy_system.core.territory_system;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class BoundsBoundaryTest {
  @Test
  void containsUsesClosedIntegerIntervals() {
    Bounds bounds = new Bounds(0, 0, 9, 9);

    assertTrue(bounds.contains(0, 0));
    assertTrue(bounds.contains(9, 9));
    assertTrue(bounds.contains(9, 0));
    assertTrue(bounds.contains(0, 9));
    assertFalse(bounds.contains(10, 9));
    assertFalse(bounds.contains(9, 10));
  }

  @Test
  void containsBoundsIncludesIdenticalAndBoundaryBounds() {
    Bounds outer = new Bounds(0, 0, 9, 9);

    assertTrue(outer.contains(new Bounds(0, 0, 9, 9)));
    assertTrue(outer.contains(new Bounds(9, 9, 0, 0)));
    assertTrue(outer.contains(new Bounds(1, 1, 0, 0)));
    assertFalse(outer.contains(new Bounds(-1, 0, 1, 1)));
    assertFalse(outer.contains(new Bounds(9, 9, 1, 0)));
  }

  @Test
  void intersectsItselfAndASharedEdge() {
    Bounds first = new Bounds(0, 0, 9, 9);
    Bounds separated = new Bounds(10, 0, 9, 9);
    Bounds touching = new Bounds(9, 0, 10, 9);

    assertTrue(first.intersects(first));
    assertFalse(first.intersects(separated));
    assertFalse(separated.intersects(first));
    assertTrue(first.intersects(touching));
    assertTrue(touching.intersects(first));
  }

  @Test
  void zeroWidthIsAValidClosedColumn() {
    Bounds column = new Bounds(5, 0, 0, 9);

    assertTrue(column.contains(5, 0));
    assertTrue(column.contains(5, 9));
    assertFalse(column.contains(4, 5));
    assertTrue(column.intersects(new Bounds(5, 3, 0, 0)));
    assertFalse(column.intersects(new Bounds(4, 3, 0, 0)));
  }

  @Test
  void zeroHeightIsAValidClosedRow() {
    Bounds row = new Bounds(0, 5, 9, 0);

    assertTrue(row.contains(0, 5));
    assertTrue(row.contains(9, 5));
    assertFalse(row.contains(5, 4));
    assertTrue(row.intersects(new Bounds(3, 5, 0, 0)));
    assertFalse(row.intersects(new Bounds(3, 4, 0, 0)));
  }

  @Test
  void zeroWidthAndHeightIsAValidSingleCell() {
    Bounds cell = new Bounds(7, -3, 0, 0);

    assertTrue(cell.contains(7, -3));
    assertFalse(cell.contains(8, -3));
    assertTrue(cell.intersects(cell));
    assertTrue(cell.intersects(new Bounds(7, -3, 0, 0)));
  }

  @Test
  void negativeCoordinatesRemainClosed() {
    Bounds bounds = new Bounds(-10, -20, 10, 20);

    assertTrue(bounds.contains(-10, -20));
    assertTrue(bounds.contains(0, 0));
    assertTrue(bounds.contains(-5, -10));
    assertFalse(bounds.contains(1, 0));
    assertFalse(bounds.contains(0, 1));
    assertTrue(bounds.intersects(new Bounds(0, 0, 0, 0)));
  }

  @Test
  void endpointArithmeticUsesLongNearIntegerExtremes() {
    Bounds high = new Bounds(Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2, 2, 2);
    Bounds low = new Bounds(Integer.MIN_VALUE, Integer.MIN_VALUE, 2, 2);

    assertTrue(high.contains(Integer.MAX_VALUE, Integer.MAX_VALUE));
    assertTrue(high.contains(new Bounds(Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0)));
    assertTrue(low.contains(Integer.MIN_VALUE, Integer.MIN_VALUE));
    assertTrue(low.contains(Integer.MIN_VALUE + 2, Integer.MIN_VALUE + 2));
    assertTrue(low.intersects(new Bounds(Integer.MIN_VALUE + 2, Integer.MIN_VALUE + 2, 0, 0)));
  }

  @Test
  void calculateBoundsUsesInclusiveEndpointDifference() {
    Bounds bounds =
        Bounds.calculateBounds(new BlockPos(-3, 64, 9), new BlockPos(5, 70, -2));

    assertEquals(-3, bounds.x);
    assertEquals(-2, bounds.z);
    assertEquals(8, bounds.width);
    assertEquals(11, bounds.height);
    assertTrue(bounds.contains(-3, -2));
    assertTrue(bounds.contains(5, 9));
  }

  @Test
  void constructorRejectsNegativeDimensions() {
    assertThrows(IllegalArgumentException.class, () -> new Bounds(0, 0, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new Bounds(0, 0, 0, -1));
    assertThrows(IllegalArgumentException.class, () -> new Bounds(0, 0, -1, -1));
  }

  @Test
  void nullOtherBoundsAreRejected() {
    Bounds bounds = new Bounds(0, 0, 1, 1);

    assertThrows(NullPointerException.class, () -> bounds.contains((Bounds) null));
    assertThrows(NullPointerException.class, () -> bounds.intersects(null));
  }
}
