package com.mo.economy_system.common.network;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RemoveTerritoryMemberMessageTest {
  @Test
  void contract() {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID();
    var m = new RemoveTerritoryMemberMessage(a, b);
    assertEquals(a, m.territoryId());
    assertEquals(b, m.targetPlayerId());
    assertThrows(NullPointerException.class, () -> new RemoveTerritoryMemberMessage(null, b));
    assertThrows(NullPointerException.class, () -> new RemoveTerritoryMemberMessage(a, null));
  }
}
