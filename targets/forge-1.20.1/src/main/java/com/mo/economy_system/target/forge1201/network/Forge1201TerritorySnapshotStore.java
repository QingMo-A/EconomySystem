package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryTeleportTarget;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.Level;

/** Read-only 1.20.1 persistence adapter; NBT never crosses the network boundary. */
final class Forge1201TerritorySnapshotStore extends SavedData {
  private static final String DATA_NAME = "territory_data";
  private final List<Owned> territories;

  private Forge1201TerritorySnapshotStore() { this(List.of()); }
  Forge1201TerritorySnapshotStore(List<Owned> territories) { this.territories = List.copyOf(territories); }

  static Forge1201TerritorySnapshotStore get(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("overworld is unavailable");
    return overworld.getDataStorage().computeIfAbsent(
        Forge1201TerritorySnapshotStore::load, Forge1201TerritorySnapshotStore::new, DATA_NAME);
  }

  List<Owned> owned(UUID requester) {
    return territories.stream().filter(value -> value.summary().ownerId().equals(requester)).toList();
  }

  List<Summary> authorized(UUID requester) {
    return territories.stream().filter(value -> !value.summary().ownerId().equals(requester)
        && value.authorizedMembers().stream().anyMatch(member -> member.playerId().equals(requester)))
        .map(Owned::summary).toList();
  }

  /** Resolves a complete teleport target without exposing the mutable NBT model. */
  Optional<TerritoryTeleportTarget> find(UUID territoryId) {
    if (territoryId == null) return Optional.empty();
    return territories.stream()
        .filter(value -> value.summary().territoryId().equals(territoryId))
        .findFirst()
        .map(Forge1201TerritorySnapshotStore::teleportTarget);
  }

  private static TerritoryTeleportTarget teleportTarget(Owned value) {
    return new TerritoryTeleportTarget(
        value.summary().territoryId(),
        value.summary().name(),
        value.summary().ownerId(),
        value.authorizedMembers().stream().map(Member::playerId).collect(java.util.stream.Collectors.toUnmodifiableSet()),
        value.summary().dimensionId(),
        value.backpoint());
  }

  private static Forge1201TerritorySnapshotStore load(CompoundTag root) {
    ListTag values = root.getList("Territories", Tag.TAG_COMPOUND);
    List<Owned> territories = new ArrayList<>(values.size());
    for (Tag value : values) territories.add(capture((CompoundTag) value));
    return new Forge1201TerritorySnapshotStore(territories);
  }

  static Owned capture(CompoundTag tag) {
    String dimension = canonicalDimension(tag.getString("Dimension"));
    Summary summary = new Summary(tag.getUUID("TerritoryID"), tag.getUUID("OwnerUUID"),
        tag.getString("OwnerName"), tag.getString("Name"),
        new Position(tag.getInt("X1"), tag.getInt("Y1"), tag.getInt("Z1")),
        new Position(tag.getInt("X2"), tag.getInt("Y2"), tag.getInt("Z2")), dimension);
    List<Member> members = new ArrayList<>();
    for (Tag value : tag.getList("AuthorizedPlayers", Tag.TAG_COMPOUND)) {
      CompoundTag member = (CompoundTag) value;
      members.add(new Member(member.getUUID("PlayerUUID"), member.getString("PlayerName")));
    }
    Optional<Position> backpoint = Optional.empty();
    if (tag.contains("Backpoint", Tag.TAG_COMPOUND)) {
      CompoundTag point = tag.getCompound("Backpoint");
      backpoint = Optional.of(new Position(point.getInt("BackX"), point.getInt("BackY"), point.getInt("BackZ")));
    }
    Map<RuleAction, RuleLevel> levels = new EnumMap<>(RuleAction.class);
    for (RuleAction action : RuleAction.values()) levels.put(action, RuleLevel.MEMBERS);
    if (tag.contains("Permissions", Tag.TAG_COMPOUND)) {
      CompoundTag permissions = tag.getCompound("Permissions");
      for (RuleAction action : RuleAction.values()) {
        levels.put(action, permission(permissions.getString(action.name())));
      }
    }
    List<Rule> rules = levels.entrySet().stream().map(value -> new Rule(value.getKey(), value.getValue())).toList();
    List<Buff> buffs = new ArrayList<>();
    for (Tag value : tag.getList("TerritoryBuffs", Tag.TAG_COMPOUND)) buffs.add(buff((CompoundTag) value));
    return new Owned(summary, members, backpoint, rules, buffs);
  }

  static Buff buff(CompoundTag tag) {
    List<BuffUpgradeCost> costs = new ArrayList<>();
    for (Tag value : tag.getList("upgrade_Cost", Tag.TAG_COMPOUND)) {
      CompoundTag cost = (CompoundTag) value;
      List<ItemRequirement> items = new ArrayList<>();
      for (Tag itemValue : cost.getList("items", Tag.TAG_COMPOUND)) {
        CompoundTag item = (CompoundTag) itemValue;
        items.add(new ItemRequirement(item.getString("item"), item.getInt("count")));
      }
      costs.add(new BuffUpgradeCost(items, cost.getInt("xp"), cost.getInt("df_coin")));
    }
    return new Buff(tag.getString("id"), tag.getString("displayText"), tag.getString("effectId"),
        tag.getBoolean("initialUnlockState"), tag.getInt("initialLevel"),
        tag.getInt("single_Upgrade_Level"), tag.getInt("max_Level"),
        tag.getBoolean("unlocked"), tag.getInt("level"), costs);
  }

  static RuleLevel permission(String stored) {
    return switch (stored) {
      case "OWNER_ONLY" -> RuleLevel.OWNER_ONLY;
      case "EVERYONE" -> RuleLevel.EVERYONE;
      case "MEMBERS" -> RuleLevel.MEMBERS;
      default -> RuleLevel.MEMBERS;
    };
  }

  static String canonicalDimension(String value) {
    if (value == null || value.isEmpty()
        || value.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH) {
      throw new IllegalArgumentException("invalid territory dimension");
    }
    ResourceLocation parsed = ResourceLocation.tryParse(value);
    if (parsed == null || !parsed.toString().equals(value)) {
      throw new IllegalArgumentException("invalid territory dimension: " + value);
    }
    return value;
  }

  @Override public CompoundTag save(CompoundTag tag) {
    // This adapter never mutates territory persistence.
    return tag;
  }
}
