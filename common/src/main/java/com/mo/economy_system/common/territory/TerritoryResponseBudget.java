package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class TerritoryResponseBudget {
  private TerritoryResponseBudget() {}

  public static int estimate(List<Owned> owned, List<Summary> authorized) {
    long bytes = 16;
    for (Owned value : owned) {
      bytes += summary(value.summary()) + 8;
      for (Member member : value.authorizedMembers()) bytes += 16 + text(member.playerName());
      if (value.backpoint().isPresent()) bytes += 13;
      for (Rule rule : value.rules()) bytes += text(rule.action().id()) + text(rule.level().id());
      for (Buff buff : value.buffs()) {
        bytes += text(buff.id()) + text(buff.displayText()) + text(buff.effectId()) + 25;
        for (BuffUpgradeCost cost : buff.upgradeCosts()) {
          bytes += 12;
          for (ItemRequirement item : cost.items()) bytes += text(item.itemId()) + 4;
        }
      }
      if (bytes > Integer.MAX_VALUE) return Integer.MAX_VALUE;
    }
    for (Summary value : authorized) bytes += summary(value);
    return (int) Math.min(bytes, Integer.MAX_VALUE);
  }

  public static void requireWithinBudget(List<Owned> owned, List<Summary> authorized) {
    if (estimate(owned, authorized) > EconomyNetworkLimits.MAX_TERRITORY_RESPONSE_ESTIMATED_BYTES) {
      throw new IllegalArgumentException("territory response exceeds estimated budget");
    }
  }

  private static int summary(Summary value) {
    return 68 + text(value.ownerName()) + text(value.name()) + text(value.dimensionId());
  }

  private static int text(String value) {
    return 4 + value.getBytes(StandardCharsets.UTF_8).length;
  }
}
