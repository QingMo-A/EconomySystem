package com.mo.economy_system.common.client;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.territory.TerritoryTestFixtures;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TerritoryDataClientApplierTest {
  @Test void currentDataRestoresCompletelyThenCommitsOnce() {
    Target target = new Target(7);
    boolean applied = TerritoryDataClientApplier.apply(TerritoryTestFixtures.response(7), target,
        value -> "owned:" + value.summary().name(), value -> "authorized:" + value.name(), failNever());
    assertTrue(applied);
    assertEquals(1, target.commits);
    assertEquals(1, target.owned.size());
    assertTrue(target.owned.get(0).startsWith("owned:"));
    assertEquals(1, target.authorized.size());
    assertFalse(target.failed);
  }

  @Test void staleDataAndErrorDoNothing() {
    Target target = new Target(8);
    assertFalse(TerritoryDataClientApplier.apply(TerritoryTestFixtures.response(7), target,
        value -> "owned", value -> "authorized", failNever()));
    assertFalse(TerritoryDataClientApplier.apply(TerritoryDataResponseMessage.error(7), target,
        value -> "owned", value -> "authorized", failNever()));
    assertEquals(0, target.commits);
    assertFalse(target.failed);
  }

  @Test void currentErrorFailsWithoutClearingOldData() {
    Target target = new Target(7);
    target.owned = new ArrayList<>(List.of("old"));
    assertTrue(TerritoryDataClientApplier.apply(TerritoryDataResponseMessage.error(7), target,
        value -> "owned", value -> "authorized", failNever()));
    assertTrue(target.failed);
    assertEquals(List.of("old"), target.owned);
    assertEquals(0, target.commits);
  }

  @Test void authorizedRestoreFailureNeverCommitsOwned() {
    Target target = new Target(7);
    target.owned = new ArrayList<>(List.of("old"));
    List<String> failures = new ArrayList<>();
    assertTrue(TerritoryDataClientApplier.apply(TerritoryTestFixtures.response(7), target,
        value -> "new-owned", value -> { throw new IllegalStateException("bad summary"); },
        (request, owned, authorized, error) -> failures.add(request + ":" + owned + ":" + authorized)));
    assertEquals(List.of("old"), target.owned);
    assertEquals(0, target.commits);
    assertTrue(target.failed);
    assertEquals(List.of("7:1:1"), failures);
  }

  private static TerritoryDataClientApplier.RestoreFailure failNever() {
    return (request, owned, authorized, error) -> fail("unexpected restore failure", error);
  }

  private static final class Target implements TerritoryDataClientApplier.TerritoryScreenTarget<String, String> {
    private final long active;
    private int commits;
    private boolean failed;
    private List<String> owned = new ArrayList<>();
    private List<String> authorized = new ArrayList<>();
    private Target(long active) { this.active = active; }
    public boolean acceptsRequest(long requestId) { return requestId == active; }
    public void commitTerritoryData(long requestId, List<String> owned, List<String> authorized) {
      commits++;
      this.owned = new ArrayList<>(owned);
      this.authorized = new ArrayList<>(authorized);
      failed = false;
    }
    public void territorySyncFailed(long requestId) { failed = true; }
  }
}
