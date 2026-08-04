package com.mo.economy_system.screen.territory_system;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TerritoryTeleportClickDebounceTest {
  @Test void duplicateClickIsRejectedUntilDurationExpires() {
    var debounce=new TerritoryTeleportClickDebounce(2);
    assertTrue(debounce.tryAcquire());assertFalse(debounce.tryAcquire());
    debounce.tick();assertFalse(debounce.tryAcquire());
    debounce.tick();assertTrue(debounce.tryAcquire());
  }
}
