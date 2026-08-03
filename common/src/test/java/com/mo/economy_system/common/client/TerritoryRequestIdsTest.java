package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class TerritoryRequestIdsTest {
  @Test void idsAreNonNegativeAndMonotonic() {
    AtomicLong sequence = new AtomicLong();
    assertEquals(0, TerritoryRequestIds.next(sequence));
    assertEquals(1, TerritoryRequestIds.next(sequence));
  }

  @Test void maximumAndCorruptNegativeStateFailExplicitly() {
    assertThrows(IllegalStateException.class,
        () -> TerritoryRequestIds.next(new AtomicLong(Long.MAX_VALUE)));
    assertThrows(IllegalStateException.class,
        () -> TerritoryRequestIds.next(new AtomicLong(-1)));
    AtomicLong sequence = new AtomicLong(Long.MAX_VALUE - 1);
    assertEquals(Long.MAX_VALUE - 1, TerritoryRequestIds.next(sequence));
    assertThrows(IllegalStateException.class, () -> TerritoryRequestIds.next(sequence));
  }
}
