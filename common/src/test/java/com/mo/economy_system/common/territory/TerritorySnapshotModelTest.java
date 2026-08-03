package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.TerritoryDataRequestMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseMessage;
import com.mo.economy_system.common.network.TerritoryDataResponseKind;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritorySnapshotModelTest {
  @Test void requestIdsAreNonNegative() {
    assertEquals(0, new TerritoryDataRequestMessage(0).requestId());
    assertEquals(9, new TerritoryDataRequestMessage(9).requestId());
    assertThrows(IllegalArgumentException.class, () -> new TerritoryDataRequestMessage(-1));
  }

  @Test void summaryRejectsInvalidTextAndNulls() {
    Summary valid = TerritoryTestFixtures.owned().summary();
    assertThrows(NullPointerException.class, () -> new Summary(null, valid.ownerId(), "a", "b",
        valid.pos1(), valid.pos2(), valid.dimensionId()));
    assertThrows(IllegalArgumentException.class, () -> new Summary(valid.territoryId(), valid.ownerId(),
        "a", " ", valid.pos1(), valid.pos2(), valid.dimensionId()));
    assertThrows(IllegalArgumentException.class, () -> new Summary(valid.territoryId(), valid.ownerId(),
        "a", "x".repeat(129), valid.pos1(), valid.pos2(), valid.dimensionId()));
  }

  @Test void ownedEnforcesMembersRulesBuffsAndLevels() {
    Owned valid = TerritoryTestFixtures.owned();
    assertThrows(IllegalArgumentException.class, () -> new Owned(valid.summary(),
        List.of(new Member(valid.summary().ownerId(), "owner")), Optional.empty(), valid.rules(), valid.buffs()));
    assertThrows(IllegalArgumentException.class, () -> new Owned(valid.summary(), valid.authorizedMembers(),
        Optional.empty(), valid.rules().subList(0, 4), valid.buffs()));
    assertThrows(IllegalArgumentException.class, () -> new Owned(valid.summary(), valid.authorizedMembers(),
        Optional.empty(), valid.rules(), List.of(valid.buffs().get(0), valid.buffs().get(0))));
    assertThrows(IllegalArgumentException.class, () -> new Buff("x", "x", "x", false, 2, 0, 1,
        false, 0, List.of()));
    assertThrows(IllegalArgumentException.class, () -> new ItemRequirement("minecraft:stone", 0));
    assertThrows(IllegalArgumentException.class, () -> new BuffUpgradeCost(List.of(), -1, 0));
  }

  @Test void responseIsDeeplyImmutableAndRejectsConflicts() {
    Owned owned = TerritoryTestFixtures.owned();
    ArrayList<Owned> source = new ArrayList<>(List.of(owned));
    TerritoryDataResponseMessage response = TerritoryDataResponseMessage.data(1, source, List.of());
    source.clear();
    assertEquals(1, response.owned().size());
    assertThrows(UnsupportedOperationException.class, () -> response.owned().clear());
    assertThrows(IllegalArgumentException.class, () -> TerritoryDataResponseMessage.data(1,
        List.of(owned, owned), List.of()));
    assertThrows(IllegalArgumentException.class, () -> TerritoryDataResponseMessage.data(1,
        List.of(owned), List.of(owned.summary())));
  }

  @Test void responseKindsUseStableIdsAndErrorCarriesNoData() {
    assertEquals("data", TerritoryDataResponseKind.DATA.id());
    assertEquals("error", TerritoryDataResponseKind.ERROR.id());
    assertEquals(TerritoryDataResponseKind.DATA, TerritoryDataResponseKind.fromId("data"));
    assertThrows(IllegalArgumentException.class, () -> TerritoryDataResponseKind.fromId("future"));
    TerritoryDataResponseMessage error = TerritoryDataResponseMessage.error(3);
    assertEquals(TerritoryDataResponseKind.ERROR, error.kind());
    assertTrue(error.owned().isEmpty());
    assertTrue(error.authorized().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> new TerritoryDataResponseMessage(
        TerritoryDataResponseKind.ERROR, 3, List.of(TerritoryTestFixtures.owned()), List.of()));
    assertThrows(NullPointerException.class, () -> new TerritoryDataResponseMessage(
        null, 3, List.of(), List.of()));
  }

  @Test void nestedContentContributesToBudget() {
    TerritoryDataResponseMessage response = TerritoryTestFixtures.response(1);
    assertTrue(TerritoryResponseBudget.estimate(response.owned(), response.authorized()) > 200);
  }

  @Test void queryUsesRequesterFiltersDeduplicatesAndSorts() {
    UUID requester = TerritoryTestFixtures.OWNER;
    Owned owned = TerritoryTestFixtures.owned();
    Summary authorized = TerritoryTestFixtures.summary(UUID.randomUUID(), UUID.randomUUID(), "Alpha");
    TerritoryDataResponseMessage result = TerritoryDataQueryService.query(
        new TerritoryDataRequestMessage(42), requester, new TerritoryDataQueryService.Repository() {
          public List<Owned> owned(UUID id) { assertEquals(requester, id); return List.of(owned, owned); }
          public List<Summary> authorized(UUID id) { return List.of(owned.summary(), authorized, authorized); }
        });
    assertEquals(42, result.requestId());
    assertEquals(List.of(owned), result.owned());
    assertEquals(List.of(authorized), result.authorized());
  }
}
