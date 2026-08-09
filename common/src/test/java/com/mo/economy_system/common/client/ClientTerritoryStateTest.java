package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClientTerritoryStateTest {
  @AfterEach void reset() { ClientTerritoryState.reset(); }

  @Test
  void commitsOnlyCurrentOrNewerResponseAndPreservesDataOnError() {
    UUID id = new UUID(0, 1);
    Summary summary = new Summary(id, new UUID(0, 2), "owner", "home",
        new Position(0, 64, 0), new Position(1, 64, 1), "minecraft:overworld");
    ClientTerritoryState.begin(2);
    assertFalse(ClientTerritoryState.apply(TerritoryDataResponseMessage.data(1, List.of(), List.of(summary))));
    assertTrue(ClientTerritoryState.apply(TerritoryDataResponseMessage.data(2, List.of(), List.of(summary))));
    assertTrue(ClientTerritoryState.apply(TerritoryDataResponseMessage.error(3)));
    assertTrue(ClientTerritoryState.snapshot().error());
    assertTrue(ClientTerritoryState.snapshot().authorized().contains(summary));
  }
}
