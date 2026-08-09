package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Shared validation, overlap, unchanged, and pricing policy for territory resize preparation. */
public final class TerritoryResizePlanner {
  private TerritoryResizePlanner() {}

  public static PlanningOutcome prepare(
      UUID territoryId,
      UUID expectedOwner,
      String expectedDimension,
      Position first,
      Position second,
      Position backpoint,
      Owned target,
      List<Owned> allTerritories) {
    if (territoryId == null
        || expectedOwner == null
        || expectedDimension == null
        || expectedDimension.isBlank()
        || first == null
        || second == null
        || backpoint == null
        || first.y() != second.y()
        || !validCandidate(first, second)) {
      return PlanningOutcome.of(PrepareResult.INVALID_BOUNDS);
    }
    if (target == null) return PlanningOutcome.of(PrepareResult.TERRITORY_NOT_FOUND);
    if (!expectedOwner.equals(target.summary().ownerId())) {
      return PlanningOutcome.of(PrepareResult.NO_PERMISSION);
    }
    if (!expectedDimension.equals(target.summary().dimensionId())) {
      return PlanningOutcome.of(PrepareResult.WRONG_DIMENSION);
    }

    try {
      requireConsistentCollection(territoryId, target, allTerritories);
      if (overlapsOther(allTerritories, territoryId, expectedDimension, first, second)) {
        return PlanningOutcome.of(PrepareResult.OVERLAP);
      }
      Position oldFirst = target.summary().pos1();
      Position oldSecond = target.summary().pos2();
      if (oldFirst.equals(first)
          && oldSecond.equals(second)
          && target.backpoint().equals(java.util.Optional.of(backpoint))) {
        return PlanningOutcome.of(PrepareResult.UNCHANGED);
      }
      long oldArea = TerritoryGeometry.area(oldFirst, oldSecond);
      long newArea = TerritoryGeometry.area(first, second);
      long difference = TerritoryPricing.areaDifference(oldArea, newArea);
      int charge = TerritoryPricing.expansionCharge(
          oldArea, newArea, TerritoryPricing.DEFAULT_PRICE_PER_CELL);
      return new PlanningOutcome(
          PrepareResult.READY,
          new Plan(
              territoryId,
              expectedOwner,
              expectedDimension,
              target,
              first,
              second,
              backpoint,
              oldArea,
              newArea,
              difference,
              charge),
          null);
    } catch (ArithmeticException failure) {
      return new PlanningOutcome(PrepareResult.PRICE_OVERFLOW, null, failure);
    } catch (RuntimeException failure) {
      return new PlanningOutcome(PrepareResult.STATE_UNKNOWN, null, failure);
    }
  }

  public static boolean validCandidate(Position first, Position second) {
    return first != null
        && second != null
        && first.y() == second.y()
        && TerritoryGeometry.validCoordinate(first)
        && TerritoryGeometry.validCoordinate(second);
  }

  public static boolean overlapsOther(
      List<Owned> values,
      UUID excludedTerritory,
      String dimension,
      Position first,
      Position second) {
    Objects.requireNonNull(values, "values");
    Objects.requireNonNull(excludedTerritory, "excludedTerritory");
    Objects.requireNonNull(dimension, "dimension");
    TerritoryGeometry.Rectangle candidate = TerritoryGeometry.rectangle(first, second);
    for (Owned value : values) {
      Objects.requireNonNull(value, "territory");
      var summary = value.summary();
      if (summary.territoryId().equals(excludedTerritory)
          || !summary.dimensionId().equals(dimension)) {
        continue;
      }
      if (candidate.intersects(TerritoryGeometry.rectangle(summary.pos1(), summary.pos2()))) {
        return true;
      }
    }
    return false;
  }

  private static void requireConsistentCollection(
      UUID territoryId, Owned target, List<Owned> allTerritories) {
    Objects.requireNonNull(allTerritories, "allTerritories");
    Set<UUID> ids = new HashSet<>();
    Owned matched = null;
    for (Owned value : allTerritories) {
      Objects.requireNonNull(value, "territory");
      UUID id = value.summary().territoryId();
      if (!ids.add(id)) throw new IllegalStateException("duplicate territory id: " + id);
      if (territoryId.equals(id)) matched = value;
    }
    if (matched == null || !matched.equals(target)) {
      throw new IllegalStateException("resize target does not match authoritative collection");
    }
  }

  public record Plan(
      UUID territoryId,
      UUID expectedOwner,
      String dimensionId,
      Owned expectedSnapshot,
      Position first,
      Position second,
      Position backpoint,
      long oldArea,
      long newArea,
      long areaDifference,
      int charge) {
    public Plan {
      Objects.requireNonNull(territoryId, "territoryId");
      Objects.requireNonNull(expectedOwner, "expectedOwner");
      Objects.requireNonNull(dimensionId, "dimensionId");
      Objects.requireNonNull(expectedSnapshot, "expectedSnapshot");
      Objects.requireNonNull(first, "first");
      Objects.requireNonNull(second, "second");
      Objects.requireNonNull(backpoint, "backpoint");
      if (oldArea <= 0 || newArea <= 0 || areaDifference != newArea - oldArea || charge < 0) {
        throw new IllegalArgumentException("invalid resize plan");
      }
    }
  }

  public record PlanningOutcome(PrepareResult result, Plan plan, Throwable failure) {
    public PlanningOutcome {
      Objects.requireNonNull(result, "result");
      if ((result == PrepareResult.READY) != (plan != null)) {
        throw new IllegalArgumentException("planning result/plan mismatch");
      }
      if (failure instanceof Error error) throw error;
    }

    public static PlanningOutcome of(PrepareResult result) {
      return new PlanningOutcome(result, null, null);
    }
  }
}
