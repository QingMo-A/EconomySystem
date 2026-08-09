package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritoryResizePlannerTest {
  private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID TERRITORY = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final String DIMENSION = "minecraft:overworld";

  @Test
  void plansInclusiveAreaAndExpansionChargeOnceForEveryTarget() {
    Owned target = territory(TERRITORY, OWNER, 0, 0, 9, 9, new Position(0, 64, 0));
    var outcome = prepare(target, List.of(target), new Position(0, 64, 0), new Position(19, 64, 9));

    assertEquals(PrepareResult.READY, outcome.result());
    assertEquals(100, outcome.plan().oldArea());
    assertEquals(200, outcome.plan().newArea());
    assertEquals(100, outcome.plan().areaDifference());
    assertEquals(2_000, outcome.plan().charge());
  }

  @Test
  void detectsClosedRectangleOverlapIncludingSharedEdges() {
    Owned target = territory(TERRITORY, OWNER, 0, 0, 9, 9, new Position(0, 64, 0));
    Owned other = territory(UUID.randomUUID(), UUID.randomUUID(), 20, 0, 30, 10,
        new Position(20, 64, 0));

    var outcome = prepare(
        target, List.of(target, other), new Position(10, 64, 0), new Position(20, 64, 9));

    assertEquals(PrepareResult.OVERLAP, outcome.result());
  }

  @Test
  void unchangedIncludesBackpointAndSameAreaReshapesAreFree() {
    Position oldBackpoint = new Position(0, 64, 0);
    Owned target = territory(TERRITORY, OWNER, 0, 0, 9, 9, oldBackpoint);
    assertEquals(PrepareResult.UNCHANGED,
        prepare(target, List.of(target), new Position(0, 64, 0), new Position(9, 64, 9)).result());

    var reshape = TerritoryResizePlanner.prepare(
        TERRITORY, OWNER, DIMENSION,
        new Position(20, 64, 20), new Position(29, 64, 29), new Position(20, 64, 20),
        target, List.of(target));
    assertEquals(PrepareResult.READY, reshape.result());
    assertEquals(0, reshape.plan().areaDifference());
    assertEquals(0, reshape.plan().charge());
  }

  @Test
  void duplicateOrMismatchedAuthoritativeStateFailsClosed() {
    Owned target = territory(TERRITORY, OWNER, 0, 0, 9, 9, new Position(0, 64, 0));
    var outcome = prepare(target, Arrays.asList(target, target),
        new Position(0, 64, 0), new Position(12, 64, 12));

    assertEquals(PrepareResult.STATE_UNKNOWN, outcome.result());
    assertNull(outcome.plan());
  }

  @Test
  void validatesOwnerDimensionAndCoordinatesBeforePricing() {
    Owned target = territory(TERRITORY, OWNER, 0, 0, 9, 9, new Position(0, 64, 0));
    assertEquals(PrepareResult.NO_PERMISSION,
        TerritoryResizePlanner.prepare(TERRITORY, UUID.randomUUID(), DIMENSION,
            new Position(0, 64, 0), new Position(10, 64, 10), new Position(0, 64, 0),
            target, List.of(target)).result());
    assertEquals(PrepareResult.WRONG_DIMENSION,
        TerritoryResizePlanner.prepare(TERRITORY, OWNER, "minecraft:the_nether",
            new Position(0, 64, 0), new Position(10, 64, 10), new Position(0, 64, 0),
            target, List.of(target)).result());
    assertEquals(PrepareResult.INVALID_BOUNDS,
        TerritoryResizePlanner.prepare(TERRITORY, OWNER, DIMENSION,
            new Position(0, 64, 0), new Position(10, 65, 10), new Position(0, 64, 0),
            target, List.of(target)).result());
  }

  private static TerritoryResizePlanner.PlanningOutcome prepare(
      Owned target, List<Owned> all, Position first, Position second) {
    return TerritoryResizePlanner.prepare(
        TERRITORY, OWNER, DIMENSION, first, second, first, target, all);
  }

  private static Owned territory(
      UUID id, UUID owner, int x1, int z1, int x2, int z2, Position backpoint) {
    Summary summary = new Summary(id, owner, "Owner", "Home",
        new Position(x1, 64, z1), new Position(x2, 64, z2), DIMENSION);
    List<Rule> rules = Arrays.stream(RuleAction.values())
        .map(action -> new Rule(action, RuleLevel.MEMBERS))
        .toList();
    return new Owned(summary, List.of(), Optional.ofNullable(backpoint), rules, List.of());
  }
}
