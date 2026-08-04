package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class TerritoryInviteClickDebounceTest {
    @Test void enforcesCooldownBoundary() {
        TerritoryInviteClickDebounce debounce = new TerritoryInviteClickDebounce(10);
        assertTrue(debounce.available(0));
        assertTrue(debounce.tryAcquire(0));
        assertFalse(debounce.tryAcquire(0));
        assertFalse(debounce.available(9));
        assertTrue(debounce.available(10));
        assertTrue(debounce.tryAcquire(10));
    }

    @Test void rejectsInvalidTicks() {
        TerritoryInviteClickDebounce debounce = new TerritoryInviteClickDebounce(10);
        assertThrows(IllegalArgumentException.class, () -> debounce.tryAcquire(-1));
        assertThrows(IllegalArgumentException.class, () -> debounce.available(-1));
    }
}
