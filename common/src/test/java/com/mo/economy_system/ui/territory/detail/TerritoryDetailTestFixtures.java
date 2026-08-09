package com.mo.economy_system.ui.territory.detail;

import com.mo.economy_system.common.territory.TerritorySnapshots.Member;
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

final class TerritoryDetailTestFixtures {
  static final UUID TERRITORY = new UUID(0, 1);
  static final UUID OWNER = new UUID(0, 2);
  static final UUID ALICE = new UUID(0, 3);
  static final UUID BOB = new UUID(0, 4);

  private TerritoryDetailTestFixtures() {}

  static Owned territory(List<Member> members) {
    return new Owned(new Summary(TERRITORY, OWNER, "owner", "spawn",
        new Position(0, 64, 0), new Position(10, 70, 10), "minecraft:overworld"),
        members, Optional.empty(),
        Arrays.stream(RuleAction.values()).map(action ->
            new Rule(action, RuleLevel.OWNER_ONLY)).toList(), List.of());
  }
}
