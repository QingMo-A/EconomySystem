package com.mo.economy_system.common.territory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mo.economy_system.common.territory.RecallPotionUseService.Arrival;
import com.mo.economy_system.common.territory.RecallPotionUseService.Lookup;
import com.mo.economy_system.common.territory.RecallPotionUseService.Result;
import com.mo.economy_system.common.territory.RecallPotionUseService.Target;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecallPotionUseServiceTest {
  @Test
  void usesDefaultSpawnOnlyWhenRespawnIsNotConfigured() {
    Fixture fixture = new Fixture();
    fixture.respawn = Lookup.notConfigured();

    Result result = fixture.execute();

    assertEquals(Result.SUCCESS, result);
    assertEquals(List.of("respawn", "default", "prepare:default", "source", "teleport:default",
        "arrival:default", "effects:default"), fixture.operations);
    assertTrue(result.consumesItem());
  }

  @Test
  void missingConfiguredDimensionDoesNotSilentlyFallBackOrConsume() {
    Fixture fixture = new Fixture();
    fixture.respawn = Lookup.dimensionNotFound();

    Result result = fixture.execute();

    assertEquals(Result.DIMENSION_NOT_FOUND, result);
    assertEquals(List.of("respawn"), fixture.operations);
    assertFalse(result.consumesItem());
  }

  @Test
  void failedArrivalDoesNotConsumePotionOrPlayDestinationEffects() {
    Fixture fixture = new Fixture();
    fixture.arrival = Arrival.NOT_ARRIVED;

    Result result = fixture.execute();

    assertEquals(Result.TELEPORT_FAILED, result);
    assertFalse(result.consumesItem());
    assertFalse(fixture.operations.contains("effects:respawn"));
    assertTrue(fixture.warnings.contains("teleport"));
  }

  @Test
  void unknownArrivalConsumesPotionToAvoidDuplicatingAfterAnUncertainTeleport() {
    Fixture fixture = new Fixture();
    fixture.arrival = Arrival.UNKNOWN;

    Result result = fixture.execute();

    assertEquals(Result.TELEPORT_STATE_UNKNOWN, result);
    assertTrue(result.consumesItem());
    assertFalse(fixture.operations.contains("effects:respawn"));
  }

  @Test
  void thrownTeleportStillSucceedsWhenArrivalIsAuthoritativelyVerified() {
    Fixture fixture = new Fixture();
    fixture.teleportThrows = true;

    assertEquals(Result.SUCCESS, fixture.execute());
    assertTrue(fixture.warnings.contains("teleport-arrived"));
    assertTrue(fixture.operations.contains("effects:respawn"));
  }

  @Test
  void visualAndDiagnosticFailuresCannotChangeSuccessfulTeleport() {
    Fixture fixture = new Fixture();
    fixture.sourceThrows = true;
    fixture.effectsThrow = true;
    fixture.diagnosticsThrow = true;

    assertEquals(Result.SUCCESS, fixture.execute());
  }

  private static final class Fixture
      implements RecallPotionUseService.Port<String>, RecallPotionUseService.Diagnostics {
    private final List<String> operations = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private Lookup<String> respawn = Lookup.found(target("respawn"));
    private Lookup<String> defaultTarget = Lookup.found(target("default"));
    private Arrival arrival = Arrival.ARRIVED;
    private boolean teleportThrows;
    private boolean sourceThrows;
    private boolean effectsThrow;
    private boolean diagnosticsThrow;

    private Result execute() {
      return RecallPotionUseService.execute(this, this);
    }

    @Override
    public Lookup<String> respawnTarget() {
      operations.add("respawn");
      return respawn;
    }

    @Override
    public Lookup<String> defaultTarget() {
      operations.add("default");
      return defaultTarget;
    }

    @Override
    public void prepare(Target<String> target) {
      operations.add("prepare:" + target.dimension());
    }

    @Override
    public void sourceEffect() {
      operations.add("source");
      if (sourceThrows) throw new IllegalStateException("source");
    }

    @Override
    public void teleport(Target<String> target) {
      operations.add("teleport:" + target.dimension());
      if (teleportThrows) throw new IllegalStateException("teleport");
    }

    @Override
    public Arrival arrival(Target<String> target) {
      operations.add("arrival:" + target.dimension());
      return arrival;
    }

    @Override
    public void destinationEffects(Target<String> target) {
      operations.add("effects:" + target.dimension());
      if (effectsThrow) throw new IllegalStateException("effects");
    }

    @Override
    public void warning(String stage, Throwable primary, Throwable secondary) {
      warnings.add(stage);
      if (diagnosticsThrow) throw new IllegalStateException("diagnostics");
    }

    private static Target<String> target(String dimension) {
      return new Target<>(dimension, new Position(10, 64, 20));
    }
  }
}
