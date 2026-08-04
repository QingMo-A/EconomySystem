package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.common.territory.TerritoryInviteRequestService;
import com.mo.economy_system.common.territory.TerritoryTeleportTarget;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
  private CompoundTag raw;
  private List<Owned> territories;

  private Forge1201TerritorySnapshotStore() { this(new CompoundTag()); }

  /** Test and adapter constructor retained for callers that already have snapshots. */
  Forge1201TerritorySnapshotStore(List<Owned> territories) {
    this.territories = List.copyOf(Objects.requireNonNull(territories, "territories"));
    this.raw = encodeSnapshots(this.territories);
  }

  private Forge1201TerritorySnapshotStore(CompoundTag root) {
    this.raw = Objects.requireNonNull(root, "root").copy();
    this.territories = parseLenient(this.raw);
  }

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

  /**
   * Reads the invitation view directly from the raw NBT record.  The parsed cache is deliberately
   * not used here: a stale cache must never grant an invitation after an owner/member change.
   * Duplicate territory IDs or malformed authorization entries make the lookup fail closed.
   */
  synchronized Optional<TerritoryInviteRequestService.Territory> inviteTerritory(UUID territoryId) {
    if (territoryId == null || !raw.contains("Territories", Tag.TAG_LIST)) {
      return Optional.empty();
    }
    ListTag records = raw.getList("Territories", Tag.TAG_COMPOUND);
    CompoundTag target = null;
    int matches = 0;
    for (Tag value : records) {
      if (!(value instanceof CompoundTag record) || !record.hasUUID("TerritoryID")) {
        continue;
      }
      if (!territoryId.equals(record.getUUID("TerritoryID"))) {
        continue;
      }
      matches++;
      target = record;
    }
    if (matches != 1 || target == null || !target.hasUUID("OwnerUUID")) {
      return Optional.empty();
    }

    Tag encodedMembers = target.get("AuthorizedPlayers");
    if (!(encodedMembers instanceof ListTag membersTag)) {
      return Optional.empty();
    }
    UUID ownerId = target.getUUID("OwnerUUID");
    Set<UUID> members = new HashSet<>();
    for (Tag value : membersTag) {
      if (!(value instanceof CompoundTag member) || !member.hasUUID("PlayerUUID")
          || !member.contains("PlayerName", Tag.TAG_STRING)) {
        return Optional.empty();
      }
      String playerName = member.getString("PlayerName").trim();
      if (playerName.isEmpty()
          || playerName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH) {
        return Optional.empty();
      }
      UUID memberId = member.getUUID("PlayerUUID");
      if (!members.add(memberId) || ownerId.equals(memberId)) {
        return Optional.empty();
      }
    }
    try {
      return Optional.of(new TerritoryInviteRequestService.Territory(
          territoryId, ownerId, target.getString("Name"), members));
    } catch (RuntimeException invalid) {
      return Optional.empty();
    }
  }

  /**
   * Adds one member using a copy-on-write raw NBT transaction.  The expected owner is checked
   * against the current raw record, rather than the previously parsed snapshot, so an owner
   * transfer racing an invite cannot be overwritten.  All fields except AuthorizedPlayers are
   * retained byte-for-byte by the defensive copy.
   */
  synchronized TerritoryInviteDecisionService.WriteResult authorize(
      UUID territoryId, UUID expectedOwner, UUID playerId, String playerName) {
    if (territoryId == null || expectedOwner == null || playerId == null
        || playerName == null || playerName.trim().isEmpty()
        || playerName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH) {
      return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
    }
    if (!raw.contains("Territories", Tag.TAG_LIST)) {
      return TerritoryInviteDecisionService.WriteResult.TERRITORY_NOT_FOUND;
    }

    ListTag sourceTerritories = raw.getList("Territories", Tag.TAG_COMPOUND);
    int targetIndex = -1;
    CompoundTag sourceTarget = null;
    for (int index = 0; index < sourceTerritories.size(); index++) {
      CompoundTag candidate = sourceTerritories.getCompound(index);
      if (!candidate.hasUUID("TerritoryID")) continue;
      if (!territoryId.equals(candidate.getUUID("TerritoryID"))) continue;
      if (targetIndex >= 0) return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
      targetIndex = index;
      sourceTarget = candidate;
    }
    if (targetIndex < 0 || sourceTarget == null) {
      return TerritoryInviteDecisionService.WriteResult.TERRITORY_NOT_FOUND;
    }
    if (!sourceTarget.hasUUID("OwnerUUID")) {
      return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
    }
    if (!expectedOwner.equals(sourceTarget.getUUID("OwnerUUID"))) {
      return TerritoryInviteDecisionService.WriteResult.OWNER_CHANGED;
    }
    if (playerId.equals(expectedOwner)) {
      return TerritoryInviteDecisionService.WriteResult.ALREADY_MEMBER;
    }
    if (!sourceTarget.contains("AuthorizedPlayers", Tag.TAG_LIST)) {
      return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
    }

    ListTag sourceMembers = sourceTarget.getList("AuthorizedPlayers", Tag.TAG_COMPOUND);
    Set<UUID> memberIds = new HashSet<>();
    for (Tag value : sourceMembers) {
      if (!(value instanceof CompoundTag member) || !member.hasUUID("PlayerUUID")) {
        return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
      }
      UUID memberId = member.getUUID("PlayerUUID");
      if (!memberIds.add(memberId) || memberId.equals(expectedOwner)) {
        return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
      }
      if (memberId.equals(playerId)) {
        return TerritoryInviteDecisionService.WriteResult.ALREADY_MEMBER;
      }
    }

    CompoundTag candidateRoot = raw.copy();
    ListTag candidateTerritories = candidateRoot.getList("Territories", Tag.TAG_COMPOUND).copy();
    CompoundTag candidateTarget = candidateTerritories.getCompound(targetIndex).copy();
    ListTag candidateMembers = candidateTarget.getList("AuthorizedPlayers", Tag.TAG_COMPOUND).copy();
    CompoundTag added = new CompoundTag();
    added.putUUID("PlayerUUID", playerId);
    added.putString("PlayerName", playerName.trim());
    candidateMembers.add(added);
    candidateTarget.put("AuthorizedPlayers", candidateMembers);
    candidateTerritories.set(targetIndex, candidateTarget);
    candidateRoot.put("Territories", candidateTerritories);

    List<Owned> reparsed;
    try {
      reparsed = parseStrict(candidateRoot);
    } catch (RuntimeException invalid) {
      return TerritoryInviteDecisionService.WriteResult.STATE_UNKNOWN;
    }
    this.raw = candidateRoot;
    this.territories = reparsed;
    setDirty();
    return TerritoryInviteDecisionService.WriteResult.ADDED;
  }

  /** Returns a deep copy for persistence tests; callers cannot mutate store state. */
  CompoundTag rawCopy() {
    synchronized (this) {
      return raw.copy();
    }
  }

  private static List<Owned> parseLenient(CompoundTag root) {
    if (!root.contains("Territories", Tag.TAG_LIST)) return List.of();
    ListTag values = root.getList("Territories", Tag.TAG_COMPOUND);
    List<Owned> parsed = new ArrayList<>(values.size());
    for (Tag value : values) {
      try {
        parsed.add(capture((CompoundTag) value));
      } catch (RuntimeException invalid) {
        // A malformed record is never exposed to a client or mutation path.
      }
    }
    return List.copyOf(parsed);
  }

  private static List<Owned> parseStrict(CompoundTag root) {
    if (!root.contains("Territories", Tag.TAG_LIST)) return List.of();
    ListTag values = root.getList("Territories", Tag.TAG_COMPOUND);
    List<Owned> parsed = new ArrayList<>(values.size());
    for (Tag value : values) {
      if (!(value instanceof CompoundTag compound)) {
        throw new IllegalArgumentException("territory record is not a compound");
      }
      parsed.add(capture(compound));
    }
    return List.copyOf(parsed);
  }

  private static CompoundTag encodeSnapshots(List<Owned> values) {
    CompoundTag root = new CompoundTag();
    ListTag list = new ListTag();
    for (Owned value : values) list.add(encodeSnapshot(value));
    root.put("Territories", list);
    return root;
  }

  private static CompoundTag encodeSnapshot(Owned value) {
    Summary summary = value.summary();
    CompoundTag tag = new CompoundTag();
    tag.putUUID("TerritoryID", summary.territoryId());
    tag.putUUID("OwnerUUID", summary.ownerId());
    tag.putString("OwnerName", summary.ownerName());
    tag.putString("Name", summary.name());
    tag.putString("Dimension", summary.dimensionId());
    tag.putInt("X1", summary.pos1().x());
    tag.putInt("Y1", summary.pos1().y());
    tag.putInt("Z1", summary.pos1().z());
    tag.putInt("X2", summary.pos2().x());
    tag.putInt("Y2", summary.pos2().y());
    tag.putInt("Z2", summary.pos2().z());

    ListTag members = new ListTag();
    for (Member member : value.authorizedMembers()) {
      CompoundTag encoded = new CompoundTag();
      encoded.putUUID("PlayerUUID", member.playerId());
      encoded.putString("PlayerName", member.playerName());
      members.add(encoded);
    }
    tag.put("AuthorizedPlayers", members);
    value.backpoint().ifPresent(point -> {
      CompoundTag backpoint = new CompoundTag();
      backpoint.putInt("BackX", point.x());
      backpoint.putInt("BackY", point.y());
      backpoint.putInt("BackZ", point.z());
      tag.put("Backpoint", backpoint);
    });
    CompoundTag permissions = new CompoundTag();
    for (Rule rule : value.rules()) permissions.putString(rule.action().name(), rule.level().name());
    tag.put("Permissions", permissions);
    ListTag buffs = new ListTag();
    for (Buff buff : value.buffs()) buffs.add(encodeBuff(buff));
    tag.put("TerritoryBuffs", buffs);
    return tag;
  }

  private static CompoundTag encodeBuff(Buff buff) {
    CompoundTag tag = new CompoundTag();
    tag.putString("id", buff.id());
    tag.putString("displayText", buff.displayText());
    tag.putString("effectId", buff.effectId());
    tag.putBoolean("initialUnlockState", buff.initialUnlocked());
    tag.putInt("initialLevel", buff.initialLevel());
    tag.putInt("single_Upgrade_Level", buff.singleUpgradeLevel());
    tag.putInt("max_Level", buff.maxLevel());
    tag.putBoolean("unlocked", buff.unlocked());
    tag.putInt("level", buff.level());
    ListTag costs = new ListTag();
    for (BuffUpgradeCost cost : buff.upgradeCosts()) {
      CompoundTag encodedCost = new CompoundTag();
      encodedCost.putInt("xp", cost.experience());
      encodedCost.putInt("df_coin", cost.currency());
      ListTag items = new ListTag();
      for (ItemRequirement item : cost.items()) {
        CompoundTag encodedItem = new CompoundTag();
        encodedItem.putString("item", item.itemId());
        encodedItem.putInt("count", item.count());
        items.add(encodedItem);
      }
      encodedCost.put("items", items);
      costs.add(encodedCost);
    }
    tag.put("upgrade_Cost", costs);
    return tag;
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

  static Forge1201TerritorySnapshotStore load(CompoundTag root) {
    return new Forge1201TerritorySnapshotStore(root);
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

  @Override public synchronized CompoundTag save(CompoundTag tag) {
    // Merge a defensive copy so unknown fields and future schema additions survive unchanged.
    tag.merge(raw.copy());
    return tag;
  }
}
