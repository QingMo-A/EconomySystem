package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Aggregated cost used by both the authoritative transaction and client UI. */
public record TerritoryBuffCost(Map<String, Integer> items, int experience, int currency) {
  public TerritoryBuffCost {
    Objects.requireNonNull(items, "items");
    if (experience < 0 || currency < 0) throw new IllegalArgumentException("negative buff cost");
    LinkedHashMap<String, Integer> copy = new LinkedHashMap<>();
    for (Map.Entry<String, Integer> entry : items.entrySet()) {
      String itemId = Objects.requireNonNull(entry.getKey(), "itemId");
      Integer count = Objects.requireNonNull(entry.getValue(), "item count");
      if (itemId.isBlank() || count <= 0) throw new IllegalArgumentException("invalid item cost");
      copy.put(itemId, count);
    }
    items = Collections.unmodifiableMap(copy);
  }

  public static TerritoryBuffCost aggregate(Buff buff) {
    Objects.requireNonNull(buff, "buff");
    long experience = 0;
    long currency = 0;
    LinkedHashMap<String, Integer> items = new LinkedHashMap<>();
    for (BuffUpgradeCost level : buff.upgradeCosts()) {
      experience = Math.addExact(experience, level.experience());
      currency = Math.addExact(currency, level.currency());
      for (ItemRequirement item : level.items()) {
        items.merge(item.itemId(), item.count(), Math::addExact);
      }
    }
    if (experience > Integer.MAX_VALUE || currency > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("territory buff cost overflow");
    }
    return new TerritoryBuffCost(items, (int) experience, (int) currency);
  }

  public static TerritoryBuffCost empty() {
    return new TerritoryBuffCost(Map.of(), 0, 0);
  }
}
