package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Synchronizes the configured territory buff catalog with a persisted territory snapshot.
 *
 * <p>The catalog describes metadata and initial state. Runtime state (unlock and level) is kept
 * for buffs that already exist, while removed definitions are discarded and new definitions are
 * initialized from their configured defaults.
 */
public final class TerritoryBuffCatalogPolicy {
  private TerritoryBuffCatalogPolicy() {}

  public static Synchronization synchronize(
      List<Buff> existing, List<Definition> configured) {
    Objects.requireNonNull(existing, "existing");
    Objects.requireNonNull(configured, "configured");

    Map<String, Buff> currentById = new HashMap<>();
    for (Buff value : existing) {
      Objects.requireNonNull(value, "existing buff");
      if (currentById.put(value.id(), value) != null) {
        throw new IllegalArgumentException("duplicate existing buff id: " + value.id());
      }
    }

    Set<String> configuredIds = new HashSet<>();
    List<Buff> result = new ArrayList<>(configured.size());
    for (Definition definition : configured) {
      Objects.requireNonNull(definition, "configured definition");
      if (!configuredIds.add(definition.id())) {
        throw new IllegalArgumentException("duplicate configured buff id: " + definition.id());
      }

      Buff current = currentById.get(definition.id());
      if (current == null) {
        result.add(definition.initialBuff());
        continue;
      }

      // Metadata follows the current server configuration. Unlock and level are player progress;
      // only a reduced max level can clamp the persisted level.
      int level = Math.min(current.level(), definition.maxLevel());
      result.add(definition.withRuntimeState(current.unlocked(), level));
    }

    List<Buff> immutable = List.copyOf(result);
    return new Synchronization(immutable, !immutable.equals(List.copyOf(existing)));
  }

  public record Synchronization(List<Buff> buffs, boolean changed) {
    public Synchronization {
      buffs = List.copyOf(Objects.requireNonNull(buffs, "buffs"));
    }
  }

  /** Immutable, loader-neutral representation of one configured buff. */
  public record Definition(
      String id,
      String displayText,
      String effectId,
      boolean initialUnlocked,
      int initialLevel,
      int singleUpgradeLevel,
      int maxLevel,
      List<BuffUpgradeCost> upgradeCosts) {
    public Definition {
      id = requireText(id, "id");
      displayText = requireText(displayText, "displayText");
      effectId = requireText(effectId, "effectId");
      if (initialLevel < 0 || singleUpgradeLevel <= 0 || maxLevel < 0
          || initialLevel > maxLevel) {
        throw new IllegalArgumentException("invalid configured buff levels");
      }
      upgradeCosts = List.copyOf(Objects.requireNonNull(upgradeCosts, "upgradeCosts"));
      if (upgradeCosts.stream().anyMatch(Objects::isNull)) {
        throw new IllegalArgumentException("null upgrade cost");
      }
    }

    public Buff initialBuff() {
      return new Buff(
          id,
          displayText,
          effectId,
          initialUnlocked,
          initialLevel,
          singleUpgradeLevel,
          maxLevel,
          initialUnlocked,
          initialLevel,
          upgradeCosts);
    }

    public Buff withRuntimeState(boolean unlocked, int level) {
      return new Buff(
          id,
          displayText,
          effectId,
          initialUnlocked,
          initialLevel,
          singleUpgradeLevel,
          maxLevel,
          unlocked,
          Math.max(0, Math.min(level, maxLevel)),
          upgradeCosts);
    }

    private static String requireText(String value, String name) {
      Objects.requireNonNull(value, name);
      if (value.isBlank()) throw new IllegalArgumentException("blank " + name);
      return value;
    }
  }
}
