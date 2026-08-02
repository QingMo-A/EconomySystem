package com.mo.economy_system.target.forge1201.network;

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
import net.minecraft.world.level.saveddata.SavedData;

/** Read-only 1.20.1 persistence adapter; NBT never crosses the network boundary. */
final class Forge1201TerritorySnapshotStore extends SavedData {
  private static final String DATA_NAME = "territory_data";
  private final List<Owned> territories;

  private Forge1201TerritorySnapshotStore() { this(List.of()); }
  private Forge1201TerritorySnapshotStore(List<Owned> territories) { this.territories = List.copyOf(territories); }

  static Forge1201TerritorySnapshotStore get(ServerLevel level) {
    return level.getDataStorage().computeIfAbsent(
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

  private static Forge1201TerritorySnapshotStore load(CompoundTag root) {
    ListTag values = root.getList("Territories", Tag.TAG_COMPOUND);
    List<Owned> territories = new ArrayList<>(values.size());
    for (Tag value : values) territories.add(capture((CompoundTag) value));
    return new Forge1201TerritorySnapshotStore(territories);
  }

  private static Owned capture(CompoundTag tag) {
    Summary summary = new Summary(tag.getUUID("TerritoryID"), tag.getUUID("OwnerUUID"),
        tag.getString("OwnerName"), tag.getString("Name"),
        new Position(tag.getInt("X1"), tag.getInt("Y1"), tag.getInt("Z1")),
        new Position(tag.getInt("X2"), tag.getInt("Y2"), tag.getInt("Z2")), tag.getString("Dimension"));
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
        String stored = permissions.getString(action.name());
        if (!stored.isEmpty()) levels.put(action, RuleLevel.valueOf(stored));
      }
    }
    List<Rule> rules = levels.entrySet().stream().map(value -> new Rule(value.getKey(), value.getValue())).toList();
    List<Buff> buffs = new ArrayList<>();
    for (Tag value : tag.getList("TerritoryBuffs", Tag.TAG_COMPOUND)) buffs.add(buff((CompoundTag) value));
    return new Owned(summary, members, backpoint, rules, buffs);
  }

  private static Buff buff(CompoundTag tag) {
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
    int maxLevel = tag.getInt("max_Level");
    int level = tag.getInt("level");
    int step = tag.getInt("single_Upgrade_Level");
    return new Buff(tag.getString("id"), tag.getString("displayText"), tag.getString("effectId"),
        tag.getBoolean("initialUnlockState"), Math.min(tag.getInt("initialLevel"), maxLevel),
        step <= 0 ? 1 : step, maxLevel, tag.getBoolean("unlocked"), Math.min(level, maxLevel), costs);
  }

  @Override public CompoundTag save(CompoundTag tag) {
    // This adapter never mutates territory persistence.
    return tag;
  }
}
