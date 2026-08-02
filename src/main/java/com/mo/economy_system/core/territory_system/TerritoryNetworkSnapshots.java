package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/** Explicit domain/network mapping. Territory NBT remains persistence-only. */
public final class TerritoryNetworkSnapshots {
  private TerritoryNetworkSnapshots() {}

  public static Summary summary(Territory territory) {
    return new Summary(
        territory.getTerritoryID(), territory.getOwnerUUID(), territory.getOwnerName(),
        territory.getName(), position(territory.getPos1()), position(territory.getPos2()),
        territory.getDimension().location().toString());
  }

  public static Owned owned(Territory territory) {
    List<Member> members = territory.getAuthorizedPlayers().stream()
        .map(value -> new Member(value.getUuid(), value.getName()))
        .sorted(Comparator.comparing(Member::playerId))
        .toList();
    List<Rule> rules = new ArrayList<>();
    for (TerritoryPermissionAction action : TerritoryPermissionAction.values()) {
      rules.add(new Rule(action(action), level(territory.getPermissionLevel(action))));
    }
    List<Buff> buffs = territory.getTerritoryBuffs().stream().map(TerritoryNetworkSnapshots::buff).toList();
    return new Owned(summary(territory), members,
        Optional.ofNullable(territory.getBackpoint()).map(TerritoryNetworkSnapshots::position),
        rules, buffs);
  }

  public static Territory restoreOwned(Owned snapshot) {
    Territory territory = restoreSummary(snapshot.summary());
    for (Member member : snapshot.authorizedMembers()) {
      territory.addAuthorizedPlayer(member.playerId(), member.playerName());
    }
    snapshot.backpoint().ifPresent(value -> territory.setBackpoint(blockPos(value)));
    for (Rule rule : snapshot.rules()) {
      territory.setPermissionLevel(action(rule.action()), level(rule.level()));
    }
    for (Buff value : snapshot.buffs()) territory.addBuffs(buff(value));
    return territory;
  }

  public static Territory restoreSummary(Summary snapshot) {
    ResourceLocation location = ResourceLocation.tryParse(snapshot.dimensionId());
    if (location == null || !location.toString().equals(snapshot.dimensionId())) {
      throw new IllegalArgumentException("invalid territory dimension: " + snapshot.dimensionId());
    }
    ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, location);
    return new Territory(snapshot.territoryId(), snapshot.name(), snapshot.ownerId(),
        snapshot.ownerName(), snapshot.pos1().x(), snapshot.pos1().y(), snapshot.pos1().z(),
        snapshot.pos2().x(), snapshot.pos2().y(), snapshot.pos2().z(), null, dimension);
  }

  private static Position position(BlockPos value) { return new Position(value.getX(), value.getY(), value.getZ()); }
  private static BlockPos blockPos(Position value) { return new BlockPos(value.x(), value.y(), value.z()); }

  private static Buff buff(TerritoryBuff value) {
    List<BuffUpgradeCost> costs = value.getUpgradeCost().stream().map(cost ->
        new BuffUpgradeCost(cost.items.stream()
            .map(item -> new ItemRequirement(item.item, item.count)).toList(), cost.xp, cost.df_coin))
        .toList();
    return new Buff(value.getId(), value.getDisplayText(), value.getEffectId(),
        value.isInitialUnlockState(), value.getInitialLevel(), value.getSingleUpgradeLevel(),
        value.getMaxLevel(), value.isUnlocked(), value.getLevel(), costs);
  }

  private static TerritoryBuff buff(Buff value) {
    List<TerritoryBuffConfig.BuffUpgradeCost> costs = value.upgradeCosts().stream().map(cost -> {
      TerritoryBuffConfig.BuffUpgradeCost restored = new TerritoryBuffConfig.BuffUpgradeCost();
      restored.items = cost.items().stream()
          .map(item -> new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement(item.itemId(), item.count()))
          .toList();
      restored.xp = cost.experience();
      restored.df_coin = cost.currency();
      return restored;
    }).toList();
    TerritoryBuff restored = new TerritoryBuff(value.id(), value.displayText(), value.effectId(),
        value.initialUnlocked(), value.initialLevel(), value.singleUpgradeLevel(), value.maxLevel(), costs);
    restored.setUnlocked(value.unlocked());
    restored.setLevel(value.level());
    return restored;
  }

  private static RuleAction action(TerritoryPermissionAction value) {
    return switch (value) {
      case PLACE_BLOCK -> RuleAction.PLACE_BLOCK;
      case BREAK_BLOCK -> RuleAction.BREAK_BLOCK;
      case USE_ITEM -> RuleAction.USE_ITEM;
      case INTERACT_BLOCK -> RuleAction.INTERACT_BLOCK;
      case OPEN_CONTAINER -> RuleAction.OPEN_CONTAINER;
    };
  }
  private static TerritoryPermissionAction action(RuleAction value) {
    return switch (value) {
      case PLACE_BLOCK -> TerritoryPermissionAction.PLACE_BLOCK;
      case BREAK_BLOCK -> TerritoryPermissionAction.BREAK_BLOCK;
      case USE_ITEM -> TerritoryPermissionAction.USE_ITEM;
      case INTERACT_BLOCK -> TerritoryPermissionAction.INTERACT_BLOCK;
      case OPEN_CONTAINER -> TerritoryPermissionAction.OPEN_CONTAINER;
    };
  }
  private static RuleLevel level(TerritoryPermissionLevel value) {
    return switch (value) {
      case OWNER_ONLY -> RuleLevel.OWNER_ONLY;
      case MEMBERS -> RuleLevel.MEMBERS;
      case EVERYONE -> RuleLevel.EVERYONE;
    };
  }
  private static TerritoryPermissionLevel level(RuleLevel value) {
    return switch (value) {
      case OWNER_ONLY -> TerritoryPermissionLevel.OWNER_ONLY;
      case MEMBERS -> TerritoryPermissionLevel.MEMBERS;
      case EVERYONE -> TerritoryPermissionLevel.EVERYONE;
    };
  }
}
