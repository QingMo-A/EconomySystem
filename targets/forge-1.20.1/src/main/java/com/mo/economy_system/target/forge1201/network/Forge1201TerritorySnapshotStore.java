package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.common.territory.TerritoryInviteRequestService;
import com.mo.economy_system.common.territory.TerritoryRemovalService;
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
final class Forge1201TerritorySnapshotStore extends SavedData
    implements TerritoryRemovalService.Repository {
  private static final String DATA_NAME = "territory_data";
  private CompoundTag raw;
  private List<Owned> territories;
  /**
   * The dirty marker is injectable for package-private transaction tests.  Production stores use
   * SavedData#setDirty; tests can make the first or rollback mark fail without mutating NBT.
   */
  private final DirtyMarker dirtyMarker;

  private Forge1201TerritorySnapshotStore() { this(new CompoundTag()); }

  /** Test and adapter constructor retained for callers that already have snapshots. */
  Forge1201TerritorySnapshotStore(List<Owned> territories) {
    this(territories, null);
  }

  Forge1201TerritorySnapshotStore(List<Owned> territories, DirtyMarker dirtyMarker) {
    this.territories = List.copyOf(Objects.requireNonNull(territories, "territories"));
    this.raw = encodeSnapshots(this.territories);
    this.dirtyMarker = dirtyMarker == null ? this::setDirty : dirtyMarker;
  }

  Forge1201TerritorySnapshotStore(CompoundTag root) {
    this(root, null);
  }

  Forge1201TerritorySnapshotStore(CompoundTag root, DirtyMarker dirtyMarker) {
    this.raw = Objects.requireNonNull(root, "root").copy();
    this.territories = parseLenient(this.raw);
    this.dirtyMarker = dirtyMarker == null ? this::setDirty : dirtyMarker;
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
    Objects.requireNonNull(territoryId, "territoryId");
    Tag encodedTerritories = raw.get("Territories");
    if (encodedTerritories == null) {
      return Optional.empty();
    }
    if (!(encodedTerritories instanceof ListTag records)) {
      throw integrity("Territories is not a list");
    }
    CompoundTag target = null;
    int matches = 0;
    for (Tag value : records) {
      if (!(value instanceof CompoundTag record)) throw integrity("territory record is not a compound");
      if (!record.hasUUID("TerritoryID")) continue;
      if (!territoryId.equals(record.getUUID("TerritoryID"))) {
        continue;
      }
      matches++;
      target = record;
    }
    if (matches == 0) return Optional.empty();
    if (matches != 1) throw integrity("duplicate territory UUID " + territoryId);
    if (!target.hasUUID("OwnerUUID")) throw integrity("matching territory has no owner");
    if (!target.contains("Name", Tag.TAG_STRING)) throw integrity("matching territory has no name");
    String territoryName = target.getString("Name").trim();
    if (territoryName.isEmpty()
        || territoryName.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH) {
      throw integrity("matching territory has an invalid name");
    }

    Tag encodedMembers = target.get("AuthorizedPlayers");
    if (!(encodedMembers instanceof ListTag membersTag)) throw integrity("AuthorizedPlayers is not a list");
    UUID ownerId = target.getUUID("OwnerUUID");
    Set<UUID> members = new HashSet<>();
    for (Tag value : membersTag) {
      if (!(value instanceof CompoundTag member) || !member.hasUUID("PlayerUUID")
          || !member.contains("PlayerName", Tag.TAG_STRING)) {
        throw integrity("authorized member is malformed");
      }
      String playerName = member.getString("PlayerName").trim();
      if (playerName.isEmpty()
          || playerName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH) {
        throw integrity("authorized member name is invalid");
      }
      UUID memberId = member.getUUID("PlayerUUID");
      if (!members.add(memberId) || ownerId.equals(memberId)) {
        throw integrity("authorized member UUID is duplicate or owner");
      }
    }
    try {
      return Optional.of(new TerritoryInviteRequestService.Territory(
          territoryId, ownerId, territoryName, members));
    } catch (RuntimeException invalid) {
      if (invalid instanceof TerritorySnapshotIntegrityException integrity) throw integrity;
      throw new TerritorySnapshotIntegrityException("matching territory snapshot is invalid");
    }
  }

  private static TerritorySnapshotIntegrityException integrity(String message) {
    return new TerritorySnapshotIntegrityException(message);
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

  /**
   * Removes exactly one territory from the authoritative raw NBT document.
   *
   * <p>The parsed {@link Owned} list is only a cache.  Removal therefore validates the complete
   * raw document first, identifies the target by UUID, and then performs a copy-on-write update.
   * No snapshot re-encoding is used, which keeps unknown fields, list order, and future schema
   * additions intact.</p>
   */
  @Override public synchronized TerritoryRemovalService.RepositoryOutcome remove(
      UUID territoryId, UUID expectedOwnerId) {
    if (territoryId == null || expectedOwnerId == null) {
      return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
    }

    final StrictRoot source;
    try {
      source = parseStrictRoot(raw);
    } catch (RuntimeException malformed) {
      return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
    }

    int targetIndex = -1;
    Owned targetSnapshot = null;
    for (int index = 0; index < source.snapshots().size(); index++) {
      Owned snapshot = source.snapshots().get(index);
      if (!territoryId.equals(snapshot.summary().territoryId())) continue;
      if (targetIndex >= 0) {
        // parseStrictRoot already rejects duplicates, but keep this guard next to the mutation.
        return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
      }
      targetIndex = index;
      targetSnapshot = snapshot;
    }
    if (targetIndex < 0 || targetSnapshot == null) {
      return repositoryState(TerritoryRemovalService.RepositoryResult.NOT_FOUND);
    }
    if (!expectedOwnerId.equals(targetSnapshot.summary().ownerId())) {
      return repositoryState(TerritoryRemovalService.RepositoryResult.OWNER_MISMATCH);
    }

    final CompoundTag originalRaw = raw;
    final List<Owned> originalSnapshots = territories;
    final CompoundTag candidateRoot;
    final List<Owned> candidateSnapshots;
    try {
      candidateRoot = originalRaw.copy();
      Tag candidateEncoded = candidateRoot.get("Territories");
      if (!(candidateEncoded instanceof ListTag sourceList)) {
        return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
      }
      ListTag candidateList = sourceList.copy();
      if (targetIndex >= candidateList.size()) {
        return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
      }
      candidateList.remove(targetIndex);
      candidateRoot.put("Territories", candidateList);

      // Parse the candidate before publishing it.  This validates every remaining record and
      // proves that the requested UUID is no longer present.
      StrictRoot reparsed = parseStrictRoot(candidateRoot);
      if (reparsed.snapshots().stream()
          .anyMatch(value -> territoryId.equals(value.summary().territoryId()))) {
        return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
      }
      candidateSnapshots = reparsed.snapshots();
    } catch (RuntimeException invalidCandidate) {
      return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
    }

    // Publish both representations together.  A dirty-marker failure rolls both back before the
    // result is exposed to the caller.
    raw = candidateRoot;
    territories = candidateSnapshots;
    try {
      dirtyMarker.markDirty();
    } catch (RuntimeException persistFailure) {
      raw = originalRaw;
      territories = originalSnapshots;
      try {
        dirtyMarker.markDirty();
      } catch (RuntimeException rollbackFailure) {
        persistFailure.addSuppressed(rollbackFailure);
        return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
      }
      try {
        // The original source was strict-parsed above.  Re-parse the restored document as a
        // defensive check so a faulty marker cannot leave a partially restored cache unnoticed.
        StrictRoot restored = parseStrictRoot(raw);
        if (restored.snapshots().size() != originalSnapshots.size()
            || restored.snapshots().stream().noneMatch(
                value -> territoryId.equals(value.summary().territoryId()))) {
          return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
        }
      } catch (RuntimeException restorationFailure) {
        persistFailure.addSuppressed(restorationFailure);
        return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN);
      }
      return repositoryState(TerritoryRemovalService.RepositoryResult.PERSIST_FAILED);
    }

    TerritoryRemovalService.RemovedTerritory removed =
        new TerritoryRemovalService.RemovedTerritory(
            targetSnapshot.summary().territoryId(),
            targetSnapshot.summary().ownerId(),
            targetSnapshot.summary().name());
    return new TerritoryRemovalService.RepositoryOutcome(
        TerritoryRemovalService.RepositoryResult.REMOVED, removed);
  }

  /** Explicitly named alias for adapters that prefer the authoritative operation name. */
  synchronized TerritoryRemovalService.RepositoryOutcome removeTerritory(
      UUID territoryId, UUID expectedOwnerId) {
    return remove(territoryId, expectedOwnerId);
  }

  /** Explicitly named alias used by network/runtime integration code. */
  synchronized TerritoryRemovalService.RepositoryOutcome removeTerritoryAuthoritatively(
      UUID territoryId, UUID expectedOwnerId) {
    return remove(territoryId, expectedOwnerId);
  }

  private static TerritoryRemovalService.RepositoryOutcome repositoryState(
      TerritoryRemovalService.RepositoryResult result) {
    return new TerritoryRemovalService.RepositoryOutcome(result, null);
  }

  /** Returns a deep copy for persistence tests; callers cannot mutate store state. */
  CompoundTag rawCopy() {
    synchronized (this) {
      return raw.copy();
    }
  }

  private static StrictRoot parseStrictRoot(CompoundTag root) {
    if (root == null) throw integrity("root is null");
    Tag encoded = root.get("Territories");
    if (!(encoded instanceof ListTag values)) {
      throw integrity("Territories is not a list");
    }
    List<Owned> parsed = new ArrayList<>(values.size());
    Set<UUID> ids = new HashSet<>();
    for (Tag value : values) {
      if (!(value instanceof CompoundTag record)) {
        throw integrity("territory record is not a compound");
      }
      Owned snapshot = captureStrict(record);
      UUID id = snapshot.summary().territoryId();
      if (!ids.add(id)) throw integrity("duplicate territory UUID " + id);
      parsed.add(snapshot);
    }
    return new StrictRoot(List.copyOf(parsed));
  }

  /**
   * Strictly validates the schema needed to produce an {@link Owned} snapshot.  Optional legacy
   * fields remain optional, but when present they must have the expected NBT type; this prevents
   * CompoundTag#getList's permissive empty-list fallback from hiding corrupt records.
   */
  private static Owned captureStrict(CompoundTag tag) {
    requireUuid(tag, "TerritoryID");
    requireUuid(tag, "OwnerUUID");
    requireString(tag, "OwnerName");
    requireString(tag, "Name");
    requireString(tag, "Dimension");
    for (String coordinate : new String[] {"X1", "Y1", "Z1", "X2", "Y2", "Z2"}) {
      requireType(tag, coordinate, Tag.TAG_INT);
    }

    if (tag.contains("AuthorizedPlayers")) {
      Tag encodedMembers = tag.get("AuthorizedPlayers");
      if (!(encodedMembers instanceof ListTag members)) {
        throw integrity("AuthorizedPlayers is not a list");
      }
      for (Tag value : members) {
        if (!(value instanceof CompoundTag member)) {
          throw integrity("authorized member is not a compound");
        }
        requireUuid(member, "PlayerUUID");
        requireString(member, "PlayerName");
      }
    }

    if (tag.contains("Backpoint")) {
      Tag encodedBackpoint = tag.get("Backpoint");
      if (!(encodedBackpoint instanceof CompoundTag point)) {
        throw integrity("Backpoint is not a compound");
      }
      for (String coordinate : new String[] {"BackX", "BackY", "BackZ"}) {
        requireType(point, coordinate, Tag.TAG_INT);
      }
    }

    if (tag.contains("Permissions")) {
      Tag encodedPermissions = tag.get("Permissions");
      if (!(encodedPermissions instanceof CompoundTag permissions)) {
        throw integrity("Permissions is not a compound");
      }
      for (RuleAction action : RuleAction.values()) {
        if (permissions.contains(action.name())) requireString(permissions, action.name());
      }
    }

    if (tag.contains("TerritoryBuffs")) {
      Tag encodedBuffs = tag.get("TerritoryBuffs");
      if (!(encodedBuffs instanceof ListTag buffs)) {
        throw integrity("TerritoryBuffs is not a list");
      }
      for (Tag value : buffs) {
        if (!(value instanceof CompoundTag buff)) {
          throw integrity("territory buff is not a compound");
        }
        if (buff.contains("upgrade_Cost")) {
          Tag encodedCosts = buff.get("upgrade_Cost");
          if (!(encodedCosts instanceof ListTag costs)) {
            throw integrity("buff upgrade_Cost is not a list");
          }
          for (Tag costValue : costs) {
            if (!(costValue instanceof CompoundTag cost)) {
              throw integrity("buff cost is not a compound");
            }
            if (cost.contains("items")) {
              Tag encodedItems = cost.get("items");
              if (!(encodedItems instanceof ListTag items)) {
                throw integrity("buff cost items is not a list");
              }
              for (Tag itemValue : items) {
                if (!(itemValue instanceof CompoundTag)) {
                  throw integrity("buff cost item is not a compound");
                }
              }
            }
          }
        }
        // Invoke the normal bounded model validation after the structural checks above.
        buff(buff);
      }
    }

    try {
      return capture(tag);
    } catch (RuntimeException invalid) {
      if (invalid instanceof TerritorySnapshotIntegrityException integrity) throw integrity;
      throw new TerritorySnapshotIntegrityException("territory snapshot is invalid");
    }
  }

  private static void requireUuid(CompoundTag tag, String key) {
    if (!tag.hasUUID(key)) throw integrity("missing UUID field " + key);
  }

  private static void requireString(CompoundTag tag, String key) {
    requireType(tag, key, Tag.TAG_STRING);
  }

  private static void requireType(CompoundTag tag, String key, int type) {
    if (!tag.contains(key, type)) throw integrity("invalid field " + key);
  }

  private record StrictRoot(List<Owned> snapshots) {
    private StrictRoot {
      snapshots = List.copyOf(Objects.requireNonNull(snapshots, "snapshots"));
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

  static Forge1201TerritorySnapshotStore load(CompoundTag root, DirtyMarker dirtyMarker) {
    return new Forge1201TerritorySnapshotStore(root, dirtyMarker);
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

/** Package-private dirty hook used to inject persistence failures in transaction tests. */
@FunctionalInterface
interface DirtyMarker {
  void markDirty();
}
