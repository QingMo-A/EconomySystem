package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import com.mo.economy_system.core.territory_system.Territory;
import java.util.List;
import org.junit.jupiter.api.Test;

class NeoForge1211TerritoryDataClientHandlerTest {
  @Test void handlerAppliesCurrentDataAndErrorButRejectsStaleResponses() {
    Target target = new Target(7);
    assertTrue(NeoForge1211TerritoryDataClientHandler.apply(TerritoryTestFixtures.response(7), target));
    assertEquals(1, target.commits);
    assertFalse(target.failed);
    target.active = 8;
    assertFalse(NeoForge1211TerritoryDataClientHandler.apply(TerritoryDataResponseMessage.error(7), target));
    assertTrue(NeoForge1211TerritoryDataClientHandler.apply(TerritoryDataResponseMessage.error(8), target));
    assertTrue(target.failed);
  }

  private static final class Target
      implements TerritoryDataClientApplier.TerritoryScreenTarget<Territory, Territory> {
    private long active;
    private int commits;
    private boolean failed;
    private Target(long active) { this.active = active; }
    public boolean acceptsRequest(long requestId) { return requestId == active; }
    public void commitTerritoryData(long requestId, List<Territory> owned, List<Territory> authorized) {
      commits++;
      failed = false;
    }
    public void territorySyncFailed(long requestId) { failed = true; }
  }
}
