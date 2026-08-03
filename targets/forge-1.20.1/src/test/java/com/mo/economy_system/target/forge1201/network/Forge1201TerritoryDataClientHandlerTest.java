package com.mo.economy_system.target.forge1201.network;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.client.TerritoryDataClientApplier;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import java.util.List;
import org.junit.jupiter.api.Test;

class Forge1201TerritoryDataClientHandlerTest {
  @Test void handlerAppliesCurrentDataAndErrorButRejectsStaleResponses() {
    Target target = new Target(7);
    assertTrue(Forge1201TerritoryDataClientHandler.apply(TerritoryTestFixtures.response(7), target));
    assertEquals(1, target.commits);
    assertFalse(target.failed);
    target.active = 8;
    assertFalse(Forge1201TerritoryDataClientHandler.apply(TerritoryDataResponseMessage.error(7), target));
    assertFalse(target.failed);
    assertTrue(Forge1201TerritoryDataClientHandler.apply(TerritoryDataResponseMessage.error(8), target));
    assertTrue(target.failed);
  }

  private static final class Target
      implements TerritoryDataClientApplier.TerritoryScreenTarget<Owned, Summary> {
    private long active;
    private int commits;
    private boolean failed;
    private Target(long active) { this.active = active; }
    public boolean acceptsRequest(long requestId) { return requestId == active; }
    public void commitTerritoryData(long requestId, List<Owned> owned, List<Summary> authorized) {
      commits++;
      failed = false;
    }
    public void territorySyncFailed(long requestId) { failed = true; }
  }
}
