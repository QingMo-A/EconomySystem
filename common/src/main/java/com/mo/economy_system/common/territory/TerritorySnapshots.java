package com.mo.economy_system.common.territory;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Loader-neutral, bounded territory network models. */
public final class TerritorySnapshots {
  private TerritorySnapshots() {}

  public record Position(int x, int y, int z) {}

  public record Summary(
      UUID territoryId,
      UUID ownerId,
      String ownerName,
      String name,
      Position pos1,
      Position pos2,
      String dimensionId) {
    public Summary {
      Objects.requireNonNull(territoryId, "territoryId");
      Objects.requireNonNull(ownerId, "ownerId");
      ownerName = text(ownerName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH, "ownerName");
      name = text(name, EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH, "name");
      Objects.requireNonNull(pos1, "pos1");
      Objects.requireNonNull(pos2, "pos2");
      dimensionId = text(
          dimensionId, EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH, "dimensionId");
    }
  }

  public record Member(UUID playerId, String playerName) {
    public Member {
      Objects.requireNonNull(playerId, "playerId");
      playerName = text(
          playerName, EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH, "playerName");
    }
  }

  public enum RuleAction {
    PLACE_BLOCK("place_block"), BREAK_BLOCK("break_block"), USE_ITEM("use_item"),
    INTERACT_BLOCK("interact_block"), OPEN_CONTAINER("open_container");
    private final String id;
    RuleAction(String id) { this.id = id; }
    public String id() { return id; }
    public static RuleAction fromId(String id) {
      for (RuleAction value : values()) if (value.id.equals(id)) return value;
      throw new IllegalArgumentException("unknown territory rule action: " + id);
    }
  }

  public enum RuleLevel {
    OWNER_ONLY("owner_only"), MEMBERS("members"), EVERYONE("everyone");
    private final String id;
    RuleLevel(String id) { this.id = id; }
    public String id() { return id; }
    public static RuleLevel fromId(String id) {
      for (RuleLevel value : values()) if (value.id.equals(id)) return value;
      throw new IllegalArgumentException("unknown territory rule level: " + id);
    }
  }

  public record Rule(RuleAction action, RuleLevel level) {
    public Rule {
      Objects.requireNonNull(action, "action");
      Objects.requireNonNull(level, "level");
    }
  }

  public record ItemRequirement(String itemId, int count) {
    public ItemRequirement {
      itemId = text(itemId, EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH, "itemId");
      if (count <= 0) throw new IllegalArgumentException("item count must be positive");
    }
  }

  public record BuffUpgradeCost(List<ItemRequirement> items, int experience, int currency) {
    public BuffUpgradeCost {
      items = copy(items, EconomyNetworkLimits.MAX_TERRITORY_COST_ITEMS, "cost items");
      if (experience < 0 || currency < 0) throw new IllegalArgumentException("negative buff cost");
    }
  }

  public record Buff(
      String id,
      String displayText,
      String effectId,
      boolean initialUnlocked,
      int initialLevel,
      int singleUpgradeLevel,
      int maxLevel,
      boolean unlocked,
      int level,
      List<BuffUpgradeCost> upgradeCosts) {
    public Buff {
      id = text(id, EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH, "buff id");
      displayText = text(displayText, EconomyNetworkLimits.MAX_TERRITORY_TEXT_LENGTH, "displayText");
      effectId = text(effectId, EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH, "effectId");
      if (initialLevel < 0 || singleUpgradeLevel <= 0 || maxLevel < 0 || level < 0
          || initialLevel > maxLevel || level > maxLevel) {
        throw new IllegalArgumentException("invalid buff levels");
      }
      upgradeCosts = copy(
          upgradeCosts, EconomyNetworkLimits.MAX_TERRITORY_BUFF_COST_LEVELS, "upgrade costs");
    }
  }

  public record Owned(
      Summary summary,
      List<Member> authorizedMembers,
      Optional<Position> backpoint,
      List<Rule> rules,
      List<Buff> buffs) {
    public Owned {
      Objects.requireNonNull(summary, "summary");
      authorizedMembers = copy(
          authorizedMembers, EconomyNetworkLimits.MAX_TERRITORY_MEMBERS, "members");
      backpoint = Objects.requireNonNull(backpoint, "backpoint");
      rules = copy(rules, EconomyNetworkLimits.MAX_TERRITORY_RULES, "rules");
      buffs = copy(buffs, EconomyNetworkLimits.MAX_TERRITORY_BUFFS, "buffs");
      uniqueMembers(authorizedMembers, summary.ownerId());
      uniqueRules(rules);
      uniqueBuffs(buffs);
    }
  }

  private static String text(String value, int max, String name) {
    Objects.requireNonNull(value, name);
    if (value.isBlank() || value.length() > max) throw new IllegalArgumentException("invalid " + name);
    return value;
  }

  private static <T> List<T> copy(List<T> values, int max, String name) {
    Objects.requireNonNull(values, name);
    if (values.size() > max || values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("invalid " + name);
    }
    return List.copyOf(values);
  }

  private static void uniqueMembers(List<Member> values, UUID owner) {
    Set<UUID> ids = new HashSet<>();
    for (Member value : values) {
      if (value.playerId().equals(owner) || !ids.add(value.playerId())) {
        throw new IllegalArgumentException("duplicate or owner territory member");
      }
    }
  }

  private static void uniqueRules(List<Rule> values) {
    Set<RuleAction> actions = new HashSet<>();
    for (Rule value : values) if (!actions.add(value.action())) throw new IllegalArgumentException("duplicate rule");
    if (actions.size() != RuleAction.values().length) throw new IllegalArgumentException("missing rule");
  }

  private static void uniqueBuffs(List<Buff> values) {
    Set<String> ids = new HashSet<>();
    for (Buff value : values) if (!ids.add(value.id())) throw new IllegalArgumentException("duplicate buff");
  }
}
