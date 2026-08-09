package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import com.mo.economy_system.common.territory.TerritorySnapshots.Rule;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction;
import com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Shared territory protection, buff and boundary-display semantics. */
public final class TerritoryRuntimePolicy {
  public static final long MOVEMENT_CHECK_INTERVAL_TICKS = 4L;
  public static final long BUFF_REAPPLY_INTERVAL_TICKS = 100L;
  public static final int BUFF_DURATION_TICKS = 200;
  public static final int BOUNDARY_PARTICLE_RADIUS = 16;
  public static final double TERRITORY_BOUNDARY_Y_OFFSET = 2.5D;
  public static final double SELECTION_BOUNDARY_Y_OFFSET = 1.5D;
  public static final int TITLE_FADE_IN_TICKS = 10;
  public static final int TITLE_STAY_TICKS = 70;
  public static final int TITLE_FADE_OUT_TICKS = 20;

  private TerritoryRuntimePolicy() {}

  /** Operators bypass protection; otherwise the configured rule is authoritative. */
  public static boolean allows(
      Owned territory, RuleAction action, UUID playerId, boolean operatorBypass) {
    Objects.requireNonNull(territory, "territory");
    Objects.requireNonNull(action, "action");
    Objects.requireNonNull(playerId, "playerId");
    if (operatorBypass || territory.summary().ownerId().equals(playerId)) return true;

    RuleLevel level = territory.rules().stream()
        .filter(rule -> rule.action() == action)
        .map(Rule::level)
        .findFirst()
        .orElse(RuleLevel.MEMBERS);
    return switch (level) {
      case OWNER_ONLY -> false;
      case MEMBERS -> territory.authorizedMembers().stream()
          .anyMatch(member -> member.playerId().equals(playerId));
      case EVERYONE -> true;
    };
  }

  public static String denialMessageKey(RuleAction action) {
    Objects.requireNonNull(action, "action");
    return "message.territory.runtime.denied." + action.id();
  }

  /** Converts user-facing effect levels to Minecraft's zero-based amplifier. */
  public static int effectAmplifier(int configuredLevel) {
    if (configuredLevel < 0) throw new IllegalArgumentException("configuredLevel");
    return configuredLevel <= 1 ? 0 : configuredLevel - 1;
  }

  public static List<EffectApplication> activeEffects(Owned territory) {
    Objects.requireNonNull(territory, "territory");
    List<EffectApplication> effects = new ArrayList<>();
    for (Buff buff : territory.buffs()) {
      if (buff.unlocked()) {
        effects.add(new EffectApplication(
            buff.effectId(), BUFF_DURATION_TICKS, effectAmplifier(buff.level())));
      }
    }
    return List.copyOf(effects);
  }

  /** Returns the nearest visible edge segment, preserving the 1.21.1 tie-breaking rule. */
  public static List<BoundaryColumn> nearestBoundaryColumns(
      Position first, Position second, int playerX, int playerZ, int radius) {
    Objects.requireNonNull(first, "first");
    Objects.requireNonNull(second, "second");
    if (radius < 0) throw new IllegalArgumentException("radius");

    int minX = Math.min(first.x(), second.x());
    int maxX = Math.max(first.x(), second.x());
    int minY = Math.min(first.y(), second.y());
    int maxY = Math.max(first.y(), second.y());
    int minZ = Math.min(first.z(), second.z());
    int maxZ = Math.max(first.z(), second.z());
    int west = distance(playerX, minX);
    int east = distance(playerX, maxX);
    int north = distance(playerZ, minZ);
    int south = distance(playerZ, maxZ);
    int nearest = Math.min(Math.min(west, east), Math.min(north, south));

    List<BoundaryColumn> columns = new ArrayList<>();
    if (nearest == west || nearest == east) {
      int x = nearest == west ? minX : maxX;
      int fromZ = Math.max(minZ, subtractSaturated(playerZ, radius));
      int toZ = Math.min(maxZ, addSaturated(playerZ, radius));
      for (int z = fromZ; z <= toZ; z++) {
        columns.add(new BoundaryColumn(x, z, minY, maxY));
        if (z == Integer.MAX_VALUE) break;
      }
    } else {
      int z = nearest == north ? minZ : maxZ;
      int fromX = Math.max(minX, subtractSaturated(playerX, radius));
      int toX = Math.min(maxX, addSaturated(playerX, radius));
      for (int x = fromX; x <= toX; x++) {
        columns.add(new BoundaryColumn(x, z, minY, maxY));
        if (x == Integer.MAX_VALUE) break;
      }
    }
    return List.copyOf(columns);
  }

  private static int distance(int left, int right) {
    long value = Math.abs((long) left - right);
    return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
  }

  private static int subtractSaturated(int value, int amount) {
    long result = (long) value - amount;
    return result < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) result;
  }

  private static int addSaturated(int value, int amount) {
    long result = (long) value + amount;
    return result > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) result;
  }

  public record EffectApplication(String effectId, int durationTicks, int amplifier) {
    public EffectApplication {
      Objects.requireNonNull(effectId, "effectId");
      if (effectId.isBlank() || durationTicks <= 0 || amplifier < 0) {
        throw new IllegalArgumentException("invalid effect application");
      }
    }
  }

  public record BoundaryColumn(int x, int z, int minY, int maxY) {
    public BoundaryColumn {
      if (minY > maxY) throw new IllegalArgumentException("inverted boundary column");
    }
  }
}
