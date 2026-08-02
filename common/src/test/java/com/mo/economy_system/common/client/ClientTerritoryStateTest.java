package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientTerritoryStateTest {
  @BeforeEach void reset() { ClientTerritoryState.resetForTest(); }
  @Test void acceptsOnlyActiveRequestAndNewRequestsMustIncrease() {
    ClientTerritoryState.begin(1);
    assertFalse(ClientTerritoryState.apply(TerritoryTestFixtures.response(0)));
    assertTrue(ClientTerritoryState.apply(TerritoryTestFixtures.response(1)));
    ClientTerritoryState.begin(2);
    assertFalse(ClientTerritoryState.apply(TerritoryTestFixtures.response(1)));
    assertThrows(IllegalArgumentException.class, () -> ClientTerritoryState.begin(2));
  }
}
