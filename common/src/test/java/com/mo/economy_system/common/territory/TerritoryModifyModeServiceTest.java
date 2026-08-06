package com.mo.economy_system.common.territory;

import static com.mo.economy_system.common.territory.TerritoryManagementTestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import com.mo.economy_system.common.network.ModifyTerritoryModeMessage;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TerritoryModifyModeServiceTest {
  @Test
  void startsOnlyForLiveOwnerInCurrentDimension() {
    AtomicReference<UUID> started = new AtomicReference<>();
    assertEquals(
        TerritoryManagementResult.SUCCESS,
        TerritoryModifyModeService.start(
            new ModifyTerritoryModeMessage(TERRITORY),
            OWNER,
            "minecraft:overworld",
            id -> owned().summary(),
            id -> { started.set(id); return true; }));
    assertEquals(TERRITORY, started.get());
    assertEquals(
        TerritoryManagementResult.NOT_OWNER,
        TerritoryModifyModeService.start(
            new ModifyTerritoryModeMessage(TERRITORY),
            MEMBER,
            "minecraft:overworld",
            id -> owned().summary(),
            id -> true));
    assertEquals(
        TerritoryManagementResult.WRONG_DIMENSION,
        TerritoryModifyModeService.start(
            new ModifyTerritoryModeMessage(TERRITORY),
            OWNER,
            "minecraft:the_nether",
            id -> owned().summary(),
            id -> true));
  }

  @Test
  void missingAndAdapterFailureFailClosed() {
    ModifyTerritoryModeMessage message = new ModifyTerritoryModeMessage(TERRITORY);
    assertEquals(
        TerritoryManagementResult.NOT_FOUND,
        TerritoryModifyModeService.start(
            message, OWNER, "minecraft:overworld", id -> null, id -> true));
    assertEquals(
        TerritoryManagementResult.STATE_UNKNOWN,
        TerritoryModifyModeService.start(
            message,
            OWNER,
            "minecraft:overworld",
            id -> owned().summary(),
            id -> { throw new IllegalStateException("session"); }));
  }
}
