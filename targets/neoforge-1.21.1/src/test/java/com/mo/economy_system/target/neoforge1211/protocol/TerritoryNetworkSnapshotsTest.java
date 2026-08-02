package com.mo.economy_system.target.neoforge1211.protocol;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryNetworkSnapshots;
import org.junit.jupiter.api.Test;

class TerritoryNetworkSnapshotsTest {
  @Test void ownedDomainSnapshotRoundTripPreservesAllNetworkData() {
    var snapshot = TerritoryTestFixtures.owned();
    Territory restored = TerritoryNetworkSnapshots.restoreOwned(snapshot);
    assertEquals(snapshot, TerritoryNetworkSnapshots.owned(restored));
  }

  @Test void summaryRestoreIsMinimalAndInvalidDimensionFailsClosed() {
    Summary snapshot = TerritoryTestFixtures.response(1).authorized().get(0);
    Territory restored = TerritoryNetworkSnapshots.restoreSummary(snapshot);
    assertEquals(snapshot, TerritoryNetworkSnapshots.summary(restored));
    assertTrue(restored.getAuthorizedPlayers().isEmpty());
    assertTrue(restored.getTerritoryBuffs().isEmpty());
    Summary invalid = new Summary(snapshot.territoryId(), snapshot.ownerId(), snapshot.ownerName(),
        snapshot.name(), snapshot.pos1(), snapshot.pos2(), "not a resource id");
    assertThrows(IllegalArgumentException.class, () -> TerritoryNetworkSnapshots.restoreSummary(invalid));
  }
}
