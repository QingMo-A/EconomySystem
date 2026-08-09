package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.Objects;

/** Loader-neutral return-to-spawn transaction used by both recall-potion items. */
public final class RecallPotionUseService {
  private RecallPotionUseService() {}

  public static <D> Result execute(Port<D> port, Diagnostics diagnostics) {
    Objects.requireNonNull(port, "port");
    Objects.requireNonNull(diagnostics, "diagnostics");

    Lookup<D> lookup;
    try {
      lookup = Objects.requireNonNull(port.respawnTarget(), "respawn target lookup");
      if (lookup.status() == LookupStatus.NOT_CONFIGURED) {
        lookup = Objects.requireNonNull(port.defaultTarget(), "default target lookup");
      }
    } catch (RuntimeException failure) {
      warn(diagnostics, "target-lookup", failure, null);
      return Result.TELEPORT_FAILED;
    }
    if (lookup.status() != LookupStatus.FOUND) return Result.DIMENSION_NOT_FOUND;

    Target<D> target = lookup.target();
    try {
      port.prepare(target);
    } catch (RuntimeException failure) {
      warn(diagnostics, "prepare", failure, null);
      return Result.TELEPORT_FAILED;
    }
    try {
      port.sourceEffect();
    } catch (RuntimeException failure) {
      warn(diagnostics, "source-effect", failure, null);
    }

    RuntimeException teleportFailure = null;
    try {
      port.teleport(target);
    } catch (RuntimeException failure) {
      teleportFailure = failure;
    }

    Arrival arrival;
    try {
      arrival = Objects.requireNonNull(port.arrival(target), "arrival");
    } catch (RuntimeException failure) {
      if (teleportFailure != null && teleportFailure != failure) {
        failure.addSuppressed(teleportFailure);
      }
      warn(diagnostics, "arrival-unknown", failure, teleportFailure);
      return Result.TELEPORT_STATE_UNKNOWN;
    }
    if (arrival == Arrival.UNKNOWN) {
      IllegalStateException failure = new IllegalStateException("adapter reported unknown arrival");
      if (teleportFailure != null) failure.addSuppressed(teleportFailure);
      warn(diagnostics, "arrival-unknown", failure, teleportFailure);
      return Result.TELEPORT_STATE_UNKNOWN;
    }
    if (arrival == Arrival.NOT_ARRIVED) {
      if (teleportFailure == null) {
        teleportFailure = new IllegalStateException("player did not arrive at recall target");
      }
      warn(diagnostics, "teleport", teleportFailure, null);
      return Result.TELEPORT_FAILED;
    }
    if (teleportFailure != null) {
      warn(diagnostics, "teleport-arrived", teleportFailure, null);
    }

    try {
      port.destinationEffects(target);
    } catch (RuntimeException failure) {
      warn(diagnostics, "destination-effects", failure, null);
    }
    return Result.SUCCESS;
  }

  private static void warn(
      Diagnostics diagnostics, String stage, Throwable primary, Throwable secondary) {
    try {
      diagnostics.warning(stage, primary, secondary);
    } catch (RuntimeException ignored) {
      // Diagnostics cannot alter item-consumption or teleport semantics.
    }
  }

  public enum Result {
    SUCCESS(true),
    DIMENSION_NOT_FOUND(false),
    TELEPORT_FAILED(false),
    TELEPORT_STATE_UNKNOWN(true);

    private final boolean consumesItem;

    Result(boolean consumesItem) {
      this.consumesItem = consumesItem;
    }

    public boolean consumesItem() {
      return consumesItem;
    }
  }

  public enum Arrival {
    ARRIVED,
    NOT_ARRIVED,
    UNKNOWN
  }

  public enum LookupStatus {
    FOUND,
    NOT_CONFIGURED,
    DIMENSION_NOT_FOUND
  }

  public record Target<D>(D dimension, Position position) {
    public Target {
      Objects.requireNonNull(dimension, "dimension");
      Objects.requireNonNull(position, "position");
    }
  }

  public record Lookup<D>(LookupStatus status, Target<D> target) {
    public Lookup {
      Objects.requireNonNull(status, "status");
      if ((status == LookupStatus.FOUND) != (target != null)) {
        throw new IllegalArgumentException("lookup status/target mismatch");
      }
    }

    public static <D> Lookup<D> found(Target<D> target) {
      return new Lookup<>(LookupStatus.FOUND, Objects.requireNonNull(target, "target"));
    }

    public static <D> Lookup<D> notConfigured() {
      return new Lookup<>(LookupStatus.NOT_CONFIGURED, null);
    }

    public static <D> Lookup<D> dimensionNotFound() {
      return new Lookup<>(LookupStatus.DIMENSION_NOT_FOUND, null);
    }
  }

  public interface Port<D> {
    Lookup<D> respawnTarget();

    Lookup<D> defaultTarget();

    void prepare(Target<D> target);

    void sourceEffect();

    void teleport(Target<D> target);

    Arrival arrival(Target<D> target);

    void destinationEffects(Target<D> target);
  }

  @FunctionalInterface
  public interface Diagnostics {
    void warning(String stage, Throwable primary, Throwable secondary);
  }
}
