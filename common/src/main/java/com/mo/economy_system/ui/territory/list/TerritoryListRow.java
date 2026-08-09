package com.mo.economy_system.ui.territory.list;

import com.mo.economy_system.common.territory.TerritorySnapshots;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Summary;
import java.util.Objects;
import java.util.Optional;

/** One immutable card in the shared territory list. */
public record TerritoryListRow(Summary summary, Optional<Owned> ownedSnapshot) {
  public TerritoryListRow {
    Objects.requireNonNull(summary, "summary");
    ownedSnapshot = Objects.requireNonNull(ownedSnapshot, "ownedSnapshot");
    if (ownedSnapshot.isPresent()
        && !ownedSnapshot.orElseThrow().summary().territoryId().equals(summary.territoryId())) {
      throw new IllegalArgumentException("owned snapshot does not match summary");
    }
  }

  public static TerritoryListRow owned(Owned value) {
    Objects.requireNonNull(value, "value");
    return new TerritoryListRow(value.summary(), Optional.of(value));
  }

  public static TerritoryListRow authorized(Summary value) {
    return new TerritoryListRow(value, Optional.empty());
  }

  public boolean owned() {
    return ownedSnapshot.isPresent();
  }

  public String dimensionId() {
    return summary.dimensionId();
  }

  public String coordinateText() {
    TerritorySnapshots.Position first = summary.pos1();
    TerritorySnapshots.Position second = summary.pos2();
    return "X: " + first.x() + " ~ " + second.x() + "  Z: "
        + first.z() + " ~ " + second.z();
  }
}
