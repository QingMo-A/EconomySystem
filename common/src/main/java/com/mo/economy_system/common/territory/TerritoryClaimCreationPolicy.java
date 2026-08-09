package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Materializes the common initial state of a newly claimed territory.
 *
 * <p>Targets receive this immutable snapshot and only translate it to their native persistence
 * representation. Keeping UUID generation, default rules, the initial backpoint and the buff
 * catalog here prevents a version adapter from silently creating a different territory.
 */
public final class TerritoryClaimCreationPolicy {
  private TerritoryClaimCreationPolicy() {}

  /** Creates a new territory with a fresh, common-generated identity. */
  public static Owned create(
      TerritoryClaimService.Request request,
      List<TerritoryBuffCatalogPolicy.Definition> buffCatalog) {
    return create(request, UUID.randomUUID(), buffCatalog);
  }

  /** Deterministic overload used by adapters and tests that need to control the identity. */
  public static Owned create(
      TerritoryClaimService.Request request,
      UUID territoryId,
      List<TerritoryBuffCatalogPolicy.Definition> buffCatalog) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(territoryId, "territoryId");
    Objects.requireNonNull(buffCatalog, "buffCatalog");
    if (request.first().y() != request.second().y()
        || !TerritoryGeometry.validCoordinate(request.first())
        || !TerritoryGeometry.validCoordinate(request.second())) {
      throw new IllegalArgumentException("invalid territory bounds");
    }

    List<Rule> rules = java.util.Arrays.stream(RuleAction.values())
        .map(action -> new Rule(action, RuleLevel.MEMBERS))
        .toList();
    List<Buff> buffs = buffCatalog.stream()
        .map(Objects::requireNonNull)
        .map(TerritoryBuffCatalogPolicy.Definition::initialBuff)
        .toList();
    return new Owned(
        new TerritorySnapshots.Summary(
            territoryId,
            request.ownerId(),
            request.ownerName(),
            request.territoryName(),
            request.first(),
            request.second(),
            request.dimensionId()),
        List.of(),
        Optional.of(request.first()),
        rules,
        buffs);
  }
}
