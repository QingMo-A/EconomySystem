package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TerritorySelectionServiceTest {
  private static final TerritorySelectionService.Point FIRST =
      new TerritorySelectionService.Point(0, 64, 0);
  private static final TerritorySelectionService.Point SECOND =
      new TerritorySelectionService.Point(4, 64, 4);

  @Test
  void claimSelectionRequiresMatchingDimensionAndHeight() {
    TerritorySelectionService service = new TerritorySelectionService(10);
    UUID player = UUID.randomUUID();

    assertEquals(
        TerritorySelectionService.Result.NO_SESSION,
        service.selectResize(
                player,
                UUID.randomUUID(),
                "minecraft:overworld",
                FIRST,
                0,
                (a, b, excluded) -> false)
            .result());
    service.startResize(player, UUID.randomUUID(), "minecraft:overworld", 0);
    assertEquals(
        TerritorySelectionService.Result.DIMENSION_MISMATCH,
        service.selectResize(
                player,
                service.session(player, TerritorySelectionService.Mode.RESIZE, 0).orElseThrow()
                    .territoryId(),
                "minecraft:the_nether",
                FIRST,
                1,
                (a, b, excluded) -> false)
            .result());

    // The resize mismatch cleared the session; start a fresh claim flow.
    service.selectClaim(player, "minecraft:overworld", FIRST, 2, (a, b, excluded) -> false);
    assertEquals(
        TerritorySelectionService.Result.Y_MISMATCH,
        service.selectClaim(
                player,
                "minecraft:overworld",
                new TerritorySelectionService.Point(4, 65, 4),
                3,
                (a, b, excluded) -> false)
            .result());
  }

  @Test
  void overlapAndThirdClickAreHandledByTheCommonStateMachine() {
    TerritorySelectionService service = new TerritorySelectionService();
    UUID player = UUID.randomUUID();
    service.startResize(player, UUID.randomUUID(), "minecraft:overworld", 0);
    assertEquals(
        TerritorySelectionService.Result.FIRST_SELECTED,
        service.selectResize(
                player,
                service.session(player, TerritorySelectionService.Mode.RESIZE, 0).orElseThrow()
                    .territoryId(),
                "minecraft:overworld",
                FIRST,
                1,
                (a, b, excluded) -> false)
            .result());
    assertEquals(
        TerritorySelectionService.Result.OVERLAP,
        service.selectResize(
                player,
                service.session(player, TerritorySelectionService.Mode.RESIZE, 1).orElseThrow()
                    .territoryId(),
                "minecraft:overworld",
                SECOND,
                2,
                (a, b, excluded) -> true)
            .result());
    assertTrue(service.has(player, TerritorySelectionService.Mode.RESIZE, 2));
    assertEquals(
        TerritorySelectionService.Result.FIRST_SELECTED,
        service.selectResize(
                player,
                service.session(player, TerritorySelectionService.Mode.RESIZE, 2).orElseThrow()
                    .territoryId(),
                "minecraft:overworld",
                FIRST,
                3,
                (a, b, excluded) -> false)
            .result());
    assertEquals(
        TerritorySelectionService.Result.SECOND_SELECTED,
        service.selectResize(
                player,
                service.session(player, TerritorySelectionService.Mode.RESIZE, 3).orElseThrow()
                    .territoryId(),
                "minecraft:overworld",
                SECOND,
                4,
                (a, b, excluded) -> false)
            .result());
    assertEquals(
        TerritorySelectionService.Result.CANCELLED,
        service.selectResize(
                player,
                service.session(player, TerritorySelectionService.Mode.RESIZE, 4).orElseThrow()
                    .territoryId(),
                "minecraft:overworld",
                SECOND,
                5,
                (a, b, excluded) -> false)
            .result());
    assertFalse(service.has(player, TerritorySelectionService.Mode.RESIZE, 5));
  }

  @Test
  void expiryAndOverflowDoNotLeaveStaleSessions() {
    TerritorySelectionService service = new TerritorySelectionService(2);
    UUID player = UUID.randomUUID();
    service.startResize(player, UUID.randomUUID(), "minecraft:overworld", Long.MAX_VALUE);
    assertTrue(service.has(player, TerritorySelectionService.Mode.RESIZE, Long.MAX_VALUE));
    service.clearAll();
    service.startResize(player, UUID.randomUUID(), "minecraft:overworld", 0);
    assertEquals(1, service.expire(3).size());
    assertFalse(service.has(player, TerritorySelectionService.Mode.RESIZE, 3));
  }
}
