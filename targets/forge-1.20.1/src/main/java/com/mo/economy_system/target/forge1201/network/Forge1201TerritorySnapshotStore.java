package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.common.territory.TerritoryInviteRequestService;
import com.mo.economy_system.common.territory.TerritoryGeometry;
import com.mo.economy_system.common.territory.TerritoryMemberRemovalService;
import com.mo.economy_system.common.territory.TerritoryRemovalService;
import com.mo.economy_system.common.territory.TerritoryResizePlanner;
import com.mo.economy_system.common.territory.TerritoryResizeTransactionService;
import com.mo.economy_system.common.territory.TerritoryAdministrationService;
import com.mo.economy_system.common.territory.TerritoryBackpointService;
import com.mo.economy_system.common.territory.TerritoryBuffCatalogPolicy;
import com.mo.economy_system.common.territory.TerritoryBuffTransactionService;
import com.mo.economy_system.common.territory.TerritoryClaimService;
import com.mo.economy_system.common.territory.TerritoryClaimCreationPolicy;
import com.mo.economy_system.common.territory.TerritorySnapshots.*;
import com.mo.economy_system.common.territory.TerritoryTeleportTarget;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

/** Read-only 1.20.1 persistence adapter; NBT never crosses the network boundary. */
public final class Forge1201TerritorySnapshotStore extends SavedData
    implements TerritoryRemovalService.Repository, TerritoryBackpointService.Repository {
  private static final String DATA_NAME = "territory_data";
  private static volatile List<TerritoryBuffCatalogPolicy.Definition> buffCatalog = List.of();
  private CompoundTag raw;
  private List<Owned> territories;

  /**
   * The dirty marker is injectable for package-private transaction tests. Production stores use
   * SavedData#setDirty; tests can make the first or rollback mark fail without mutating NBT.
   */
  private final DirtyMarker dirtyMarker;

  private Forge1201TerritorySnapshotStore() {
    this(emptyRoot());
  }

  private static CompoundTag emptyRoot() {
    CompoundTag root = new CompoundTag();
    root.put("Territories", new ListTag());
    return root;
  }

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

  public static Forge1201TerritorySnapshotStore get(ServerLevel level) {
    ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("overworld is unavailable");
    return overworld
        .getDataStorage()
        .computeIfAbsent(
            Forge1201TerritorySnapshotStore::load, Forge1201TerritorySnapshotStore::new, DATA_NAME);
  }

  /** Installs the common catalog used when creating and synchronizing Forge snapshots. */
  public static void configureBuffCatalog(
      List<TerritoryBuffCatalogPolicy.Definition> definitions) {
    buffCatalog = List.copyOf(Objects.requireNonNull(definitions, "definitions"));
  }

  public static void clearBuffCatalog() {
    buffCatalog = List.of();
  }

  /**
   * Applies the common catalog policy to every persisted territory in one copy-on-write update.
   * Unknown territory fields remain untouched.
   */
  public synchronized BuffCatalogSyncResult synchronizeBuffCatalog(
      List<TerritoryBuffCatalogPolicy.Definition> definitions) {
    Objects.requireNonNull(definitions, "definitions");
    final StrictRoot source;
    try {
      source = parseStrictRoot(raw);
      if (!source.snapshots().equals(territories)) return BuffCatalogSyncResult.STATE_UNKNOWN;
    } catch (RuntimeException failure) {
      return BuffCatalogSyncResult.STATE_UNKNOWN;
    }

    CompoundTag candidateRoot = raw.copy();
    ListTag records = candidateRoot.getList("Territories", Tag.TAG_COMPOUND).copy();
    boolean changed = false;
    try {
      for (int index = 0; index < source.snapshots().size(); index++) {
        Owned current = source.snapshots().get(index);
        TerritoryBuffCatalogPolicy.Synchronization sync =
            TerritoryBuffCatalogPolicy.synchronize(current.buffs(), definitions);
        if (!sync.changed()) continue;
        CompoundTag record = records.getCompound(index).copy();
        record.put("TerritoryBuffs", encodeBuffs(sync.buffs()));
        records.set(index, record);
        changed = true;
      }
      if (!changed) return BuffCatalogSyncResult.UNCHANGED;
      candidateRoot.put("Territories", records);
      List<Owned> candidateCache = parseStrictRoot(candidateRoot).snapshots();
      CompoundTag originalRaw = raw;
      List<Owned> originalCache = territories;
      CompoundTag originalCopy = originalRaw.copy();
      raw = candidateRoot;
      territories = candidateCache;
      try {
        dirtyMarker.markDirty();
        verifyManagementPublished(candidateRoot, candidateRoot.copy(), candidateCache);
        return BuffCatalogSyncResult.UPDATED;
      } catch (RuntimeException failure) {
        return rollbackBuffCatalog(
            originalRaw, originalCache, originalCopy, failure);
      }
    } catch (RuntimeException failure) {
      return BuffCatalogSyncResult.STATE_UNKNOWN;
    }
  }

  private BuffCatalogSyncResult rollbackBuffCatalog(
      CompoundTag originalRaw,
      List<Owned> originalCache,
      CompoundTag originalCopy,
      RuntimeException primary) {
    raw = originalRaw;
    territories = originalCache;
    try {
      dirtyMarker.markDirty();
      if (raw != originalRaw || territories != originalCache || !raw.equals(originalCopy)
          || !parseStrictRoot(raw).snapshots().equals(originalCache)) {
        throw new IllegalStateException("buff catalog rollback changed state");
      }
      return BuffCatalogSyncResult.PERSIST_FAILED;
    } catch (RuntimeException rollback) {
      primary.addSuppressed(rollback);
      return BuffCatalogSyncResult.STATE_UNKNOWN;
    }
  }

  List<Owned> owned(UUID requester) {
    return territories.stream()
        .filter(value -> value.summary().ownerId().equals(requester))
        .toList();
  }

  /** Returns an immutable strict snapshot for command-side inspection and package-local adapters. */
  synchronized List<Owned> allSnapshots() {
    return parseStrictRoot(raw).snapshots();
  }

  /** Resolves the territory containing an X/Z point in the requested dimension. */
  public synchronized Optional<Owned> at(String dimensionId, int x, int z) {
    if (dimensionId == null || dimensionId.isBlank()) return Optional.empty();
    return allSnapshots().stream()
        .filter(value -> value.summary().dimensionId().equals(dimensionId))
        .filter(value -> contains(value, x, z))
        .findFirst();
  }

  @Override
  public synchronized Owned findAt(String dimensionId, int x, int z) {
    return at(dimensionId, x, z).orElse(null);
  }

  synchronized boolean overlaps(TerritoryClaimService.Request request) {
    Objects.requireNonNull(request, "request");
    TerritoryGeometry.Rectangle candidate =
        TerritoryGeometry.rectangle(request.first(), request.second());
    for (Owned value : parseStrictRoot(raw).snapshots()) {
      if (!value.summary().dimensionId().equals(request.dimensionId())) continue;
      if (candidate.intersects(TerritoryGeometry.rectangle(
          value.summary().pos1(), value.summary().pos2()))) return true;
    }
    return false;
  }

  synchronized TerritoryClaimService.RepositoryResult create(
      TerritoryClaimService.Request request, long area, int price) {
    Objects.requireNonNull(request, "request");
    final StrictRoot source;
    try {
      source = parseStrictRoot(raw);
      if (!source.snapshots().equals(territories)) {
        return TerritoryClaimService.RepositoryResult.STATE_UNKNOWN;
      }
      if (overlaps(request)) return TerritoryClaimService.RepositoryResult.OVERLAP;
    } catch (RuntimeException failure) {
      return TerritoryClaimService.RepositoryResult.STATE_UNKNOWN;
    }

    CompoundTag originalRaw = raw;
    List<Owned> originalCache = territories;
    try {
      CompoundTag candidateRoot = originalRaw.copy();
      ListTag encoded = candidateRoot.contains("Territories", Tag.TAG_LIST)
          ? candidateRoot.getList("Territories", Tag.TAG_COMPOUND).copy()
          : new ListTag();
      Owned created = TerritoryClaimCreationPolicy.create(request, buffCatalog);
      CompoundTag record = encodeSnapshot(created);
      encoded.add(record);
      candidateRoot.put("Territories", encoded);
      List<Owned> candidateCache = parseStrictRoot(candidateRoot).snapshots();
      raw = candidateRoot;
      territories = candidateCache;
      dirtyMarker.markDirty();
      return TerritoryClaimService.RepositoryResult.CREATED;
    } catch (RuntimeException failure) {
      raw = originalRaw;
      territories = originalCache;
      try {
        dirtyMarker.markDirty();
      } catch (RuntimeException rollbackFailure) {
        failure.addSuppressed(rollbackFailure);
        return TerritoryClaimService.RepositoryResult.STATE_UNKNOWN;
      }
      return TerritoryClaimService.RepositoryResult.PERSIST_FAILED;
    }
  }

  @Override
  public synchronized TerritoryBackpointService.RepositoryResult setBackpoint(
      UUID territoryId,
      UUID expectedOwner,
      Optional<Position> expectedBackpoint,
      Position point) {
    if (territoryId == null || expectedOwner == null || expectedBackpoint == null || point == null) {
      return TerritoryBackpointService.RepositoryResult.STATE_UNKNOWN;
    }
    return mutateRaw(
        territoryId,
        target -> {
          Owned current = target.snapshot();
          if (!expectedOwner.equals(current.summary().ownerId())) {
            return TerritoryBackpointService.RepositoryResult.OWNER_CHANGED;
          }
          if (!expectedBackpoint.equals(current.backpoint())) {
            return TerritoryBackpointService.RepositoryResult.SNAPSHOT_CHANGED;
          }
          if (current.backpoint().equals(Optional.of(point))) {
            return TerritoryBackpointService.RepositoryResult.UNCHANGED;
          }
          CompoundTag backpoint = new CompoundTag();
          backpoint.putInt("BackX", point.x());
          backpoint.putInt("BackY", point.y());
          backpoint.putInt("BackZ", point.z());
          target.tag().put("Backpoint", backpoint);
          return TerritoryBackpointService.RepositoryResult.UPDATED;
        },
        result -> result == TerritoryBackpointService.RepositoryResult.UPDATED,
        TerritoryBackpointService.RepositoryResult.PERSIST_FAILED,
        TerritoryBackpointService.RepositoryResult.STATE_UNKNOWN,
        TerritoryBackpointService.RepositoryResult.NOT_FOUND);
  }

  private static boolean contains(Owned value, int x, int z) {
    return TerritoryGeometry.rectangle(value.summary().pos1(), value.summary().pos2()).contains(x, z);
  }

  synchronized void mutateRawForTest(Consumer<CompoundTag> mutation) {
    Objects.requireNonNull(mutation, "mutation").accept(raw);
  }

  synchronized void replaceCacheForTest(List<Owned> replacement) {
    territories = List.copyOf(Objects.requireNonNull(replacement, "replacement"));
  }

  List<Summary> authorized(UUID requester) {
    return territories.stream()
        .filter(
            value ->
                !value.summary().ownerId().equals(requester)
                    && value.authorizedMembers().stream()
                        .anyMatch(member -> member.playerId().equals(requester)))
        .map(Owned::summary)
        .toList();
  }

  /** Resolves a complete teleport target without exposing the mutable NBT model. */
  Optional<TerritoryTeleportTarget> find(UUID territoryId) {
    if (territoryId == null) return Optional.empty();
    return territories.stream()
        .filter(value -> value.summary().territoryId().equals(territoryId))
        .findFirst()
        .map(Forge1201TerritorySnapshotStore::teleportTarget);
  }

  synchronized Owned findOwned(UUID territoryId) {
    Objects.requireNonNull(territoryId, "territoryId");
    StrictRoot parsed = parseStrictRoot(raw);
    return parsed.snapshots().stream()
        .filter(value -> value.summary().territoryId().equals(territoryId))
        .findFirst()
        .orElse(null);
  }

  /** Applies a complete common administration replacement with a raw-NBT compare-and-set. */
  synchronized TerritoryAdministrationService.RepositoryResult applyAdministration(
      Owned expected, Owned replacement) {
    if (!TerritoryAdministrationService.isValidReplacement(expected, replacement)) {
      return TerritoryAdministrationService.RepositoryResult.INVALID_TARGET;
    }
    UUID territoryId = expected.summary().territoryId();
    return mutateRaw(
        territoryId,
        target -> {
          Owned current = target.snapshot();
          if (!current.summary().ownerId().equals(expected.summary().ownerId())) {
            return TerritoryAdministrationService.RepositoryResult.OWNER_CHANGED;
          }
          if (!current.equals(expected)) {
            return TerritoryAdministrationService.RepositoryResult.STATE_UNKNOWN;
          }
          if (current.equals(replacement)) {
            return TerritoryAdministrationService.RepositoryResult.NO_CHANGE;
          }

          CompoundTag record = target.tag();
          Summary summary = replacement.summary();
          record.putUUID("OwnerUUID", summary.ownerId());
          record.putString("OwnerName", summary.ownerName());

          ListTag members = new ListTag();
          for (Member member : replacement.authorizedMembers()) {
            CompoundTag encoded = new CompoundTag();
            encoded.putUUID("PlayerUUID", member.playerId());
            encoded.putString("PlayerName", member.playerName());
            members.add(encoded);
          }
          record.put("AuthorizedPlayers", members);

          CompoundTag permissions = record.contains("Permissions", Tag.TAG_COMPOUND)
              ? record.getCompound("Permissions")
              : new CompoundTag();
          for (Rule rule : replacement.rules()) {
            permissions.putString(rule.action().name(), rule.level().name());
          }
          record.put("Permissions", permissions);
          return TerritoryAdministrationService.RepositoryResult.SUCCESS;
        },
        result -> result == TerritoryAdministrationService.RepositoryResult.SUCCESS,
        TerritoryAdministrationService.RepositoryResult.PERSIST_FAILED,
        TerritoryAdministrationService.RepositoryResult.STATE_UNKNOWN,
        TerritoryAdministrationService.RepositoryResult.NOT_FOUND);
  }

  synchronized TerritoryBuffTransactionService.RepositoryResult mutateBuff(
      UUID territoryId,
      UUID expectedOwner,
      String buffId,
      boolean expectedUnlocked,
      int expectedLevel,
      boolean newUnlocked,
      int newLevel) {
    if (territoryId == null || expectedOwner == null || buffId == null) {
      return TerritoryBuffTransactionService.RepositoryResult.STATE_UNKNOWN;
    }
    return mutateRaw(
        territoryId,
        target -> {
          if (!target.snapshot().summary().ownerId().equals(expectedOwner)) {
            return TerritoryBuffTransactionService.RepositoryResult.OWNER_CHANGED;
          }
          ListTag buffs = target.tag().getList("TerritoryBuffs", Tag.TAG_COMPOUND);
          CompoundTag encoded = null;
          for (Tag value : buffs) {
            CompoundTag current = (CompoundTag) value;
            if (buffId.equals(current.getString("id"))) {
              if (encoded != null) throw integrity("duplicate territory buff id");
              encoded = current;
            }
          }
          if (encoded == null) return TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED;
          if (encoded.getBoolean("unlocked") != expectedUnlocked
              || encoded.getInt("level") != expectedLevel) {
            return TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED;
          }
          if (newUnlocked == expectedUnlocked && newLevel == expectedLevel) {
            return TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED;
          }
          encoded.putBoolean("unlocked", newUnlocked);
          encoded.putInt("level", newLevel);
          return TerritoryBuffTransactionService.RepositoryResult.SUCCESS;
        },
        result -> result == TerritoryBuffTransactionService.RepositoryResult.SUCCESS,
        TerritoryBuffTransactionService.RepositoryResult.PERSIST_FAILED,
        TerritoryBuffTransactionService.RepositoryResult.STATE_UNKNOWN,
        TerritoryBuffTransactionService.RepositoryResult.NOT_FOUND);
  }

  enum ResizePrepareResult {
    READY,
    UNCHANGED,
    NOT_FOUND,
    NOT_OWNER,
    WRONG_DIMENSION,
    INVALID_BOUNDS,
    OVERLAP,
    PRICE_OVERFLOW,
    STATE_UNKNOWN
  }

  enum ResizeCommitResult {
    SUCCESS,
    NOT_FOUND,
    CHANGED,
    OVERLAP,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  record ResizePlan(
      UUID territoryId,
      UUID expectedOwner,
      String dimensionId,
      Owned expectedSnapshot,
      Position first,
      Position second,
      Position backpoint,
      long oldArea,
      long newArea,
      int charge) {
    ResizePlan {
      Objects.requireNonNull(territoryId, "territoryId");
      Objects.requireNonNull(expectedOwner, "expectedOwner");
      dimensionId = canonicalDimension(dimensionId);
      Objects.requireNonNull(expectedSnapshot, "expectedSnapshot");
      Objects.requireNonNull(first, "first");
      Objects.requireNonNull(second, "second");
      Objects.requireNonNull(backpoint, "backpoint");
      if (oldArea <= 0 || newArea <= 0 || charge < 0) {
        throw new IllegalArgumentException("invalid resize plan");
      }
    }
  }

  record ResizePrepareOutcome(ResizePrepareResult result, ResizePlan plan, Throwable failure) {
    ResizePrepareOutcome {
      Objects.requireNonNull(result, "result");
      if ((result == ResizePrepareResult.READY) != (plan != null)) {
        throw new IllegalArgumentException("resize prepare result/plan mismatch");
      }
      if (failure instanceof Error error) throw error;
    }

    static ResizePrepareOutcome of(ResizePrepareResult result) {
      return new ResizePrepareOutcome(result, null, null);
    }
  }

  synchronized ResizePrepareOutcome prepareResize(
      UUID territoryId,
      UUID expectedOwner,
      String expectedDimension,
      Position first,
      Position second) {
    if (territoryId == null
        || expectedOwner == null
        || expectedDimension == null
        || first == null
        || second == null
        || !TerritoryResizePlanner.validCandidate(first, second)) {
      return ResizePrepareOutcome.of(ResizePrepareResult.INVALID_BOUNDS);
    }
    final String dimension;
    final StrictRoot source;
    try {
      dimension = canonicalDimension(expectedDimension);
      source = parseStrictRoot(raw);
      if (!source.snapshots().equals(territories)) {
        throw integrity("resize raw/cache mismatch");
      }
    } catch (RuntimeException failure) {
      return new ResizePrepareOutcome(ResizePrepareResult.STATE_UNKNOWN, null, failure);
    }
    Owned target = source.snapshots().stream()
        .filter(value -> territoryId.equals(value.summary().territoryId()))
        .findFirst()
        .orElse(null);
    Position backpoint = first;
    TerritoryResizePlanner.PlanningOutcome planning = TerritoryResizePlanner.prepare(
        territoryId, expectedOwner, dimension, first, second, backpoint, target, source.snapshots());
    if (planning.result() != TerritoryResizeTransactionService.PrepareResult.READY) {
      return new ResizePrepareOutcome(mapPlanningResult(planning.result()), null, planning.failure());
    }
    TerritoryResizePlanner.Plan plan = planning.plan();
    return new ResizePrepareOutcome(
        ResizePrepareResult.READY,
        new ResizePlan(
            plan.territoryId(),
            plan.expectedOwner(),
            plan.dimensionId(),
            plan.expectedSnapshot(),
            plan.first(),
            plan.second(),
            plan.backpoint(),
            plan.oldArea(),
            plan.newArea(),
            plan.charge()),
        null);
  }

  synchronized ResizeCommitResult commitResize(ResizePlan plan) {
    Objects.requireNonNull(plan, "plan");
    final StrictRoot source;
    try {
      source = parseStrictRoot(raw);
      if (!source.snapshots().equals(territories)) return ResizeCommitResult.STATE_UNKNOWN;
    } catch (RuntimeException failure) {
      return ResizeCommitResult.STATE_UNKNOWN;
    }
    Owned current = source.snapshots().stream()
        .filter(value -> plan.territoryId().equals(value.summary().territoryId()))
        .findFirst()
        .orElse(null);
    if (current == null) return ResizeCommitResult.NOT_FOUND;
    if (!current.equals(plan.expectedSnapshot())
        || !plan.expectedOwner().equals(current.summary().ownerId())
        || !plan.dimensionId().equals(current.summary().dimensionId())) {
      return ResizeCommitResult.CHANGED;
    }
    if (TerritoryResizePlanner.overlapsOther(
        source.snapshots(),
        plan.territoryId(),
        plan.dimensionId(),
        plan.first(),
        plan.second())) {
      return ResizeCommitResult.OVERLAP;
    }
    return mutateRaw(
        plan.territoryId(),
        target -> {
          if (!target.snapshot().equals(plan.expectedSnapshot())) {
            return ResizeCommitResult.CHANGED;
          }
          CompoundTag tag = target.tag();
          tag.putInt("X1", plan.first().x());
          tag.putInt("Y1", plan.first().y());
          tag.putInt("Z1", plan.first().z());
          tag.putInt("X2", plan.second().x());
          tag.putInt("Y2", plan.second().y());
          tag.putInt("Z2", plan.second().z());
          CompoundTag backpoint = new CompoundTag();
          backpoint.putInt("BackX", plan.backpoint().x());
          backpoint.putInt("BackY", plan.backpoint().y());
          backpoint.putInt("BackZ", plan.backpoint().z());
          tag.put("Backpoint", backpoint);
          return ResizeCommitResult.SUCCESS;
        },
        result -> result == ResizeCommitResult.SUCCESS,
        ResizeCommitResult.PERSIST_FAILED,
        ResizeCommitResult.STATE_UNKNOWN,
        ResizeCommitResult.NOT_FOUND);
  }

  private static ResizePrepareResult mapPlanningResult(
      TerritoryResizeTransactionService.PrepareResult result) {
    return switch (result) {
      case READY -> ResizePrepareResult.READY;
      case UNCHANGED -> ResizePrepareResult.UNCHANGED;
      case TERRITORY_NOT_FOUND -> ResizePrepareResult.NOT_FOUND;
      case NO_PERMISSION -> ResizePrepareResult.NOT_OWNER;
      case WRONG_DIMENSION -> ResizePrepareResult.WRONG_DIMENSION;
      case INVALID_BOUNDS -> ResizePrepareResult.INVALID_BOUNDS;
      case OVERLAP -> ResizePrepareResult.OVERLAP;
      case PRICE_OVERFLOW -> ResizePrepareResult.PRICE_OVERFLOW;
      case STATE_UNKNOWN -> ResizePrepareResult.STATE_UNKNOWN;
    };
  }

  private synchronized <R> R mutateRaw(
      UUID territoryId,
      Function<RawTarget, R> mutation,
      Predicate<R> publishes,
      R persistFailed,
      R stateUnknown,
      R notFound) {
    Objects.requireNonNull(territoryId, "territoryId");
    try {
      StrictRoot source = parseStrictRoot(raw);
      if (!source.snapshots().equals(territories)) return stateUnknown;
      int index = -1;
      for (int current = 0; current < source.snapshots().size(); current++) {
        if (source.snapshots().get(current).summary().territoryId().equals(territoryId)) {
          index = current;
          break;
        }
      }
      if (index < 0) return notFound;
      CompoundTag candidate = raw.copy();
      ListTag records = candidate.getList("Territories", Tag.TAG_COMPOUND);
      R result = mutation.apply(new RawTarget(source.snapshots().get(index), records.getCompound(index)));
      if (!publishes.test(result)) return result;
      StrictRoot parsed = parseStrictRoot(candidate);
      CompoundTag originalRaw = raw;
      List<Owned> originalCache = territories;
      CompoundTag originalRawCopy = originalRaw.copy();
      List<Owned> originalParsed = source.snapshots();
      CompoundTag candidateCopy = candidate.copy();
      List<Owned> candidateCache = parsed.snapshots();
      raw = candidate;
      territories = candidateCache;
      try {
        dirtyMarker.markDirty();
        verifyManagementPublished(candidate, candidateCopy, candidateCache);
        return result;
      } catch (RuntimeException failure) {
        return rollbackManagement(
            originalRaw,
            originalCache,
            originalRawCopy,
            originalParsed,
            persistFailed,
            stateUnknown,
            failure);
      } catch (Error failure) {
        try {
          rollbackManagement(
              originalRaw,
              originalCache,
              originalRawCopy,
              originalParsed,
              persistFailed,
              stateUnknown,
              new IllegalStateException("management dirty error", failure));
        } catch (RuntimeException rollback) {
          failure.addSuppressed(rollback);
        }
        throw failure;
      }
    } catch (RuntimeException failure) {
      return stateUnknown;
    }
  }

  private void verifyManagementPublished(
      CompoundTag expectedRaw, CompoundTag expectedCopy, List<Owned> expectedCache) {
    if (raw != expectedRaw || territories != expectedCache) {
      throw new IllegalStateException("management publication identity changed");
    }
    if (!raw.equals(expectedCopy)
        || !parseStrictRoot(raw).snapshots().equals(expectedCache)) {
      throw new IllegalStateException("management publication content changed");
    }
  }

  private <R> R rollbackManagement(
      CompoundTag originalRaw,
      List<Owned> originalCache,
      CompoundTag originalRawCopy,
      List<Owned> originalParsed,
      R persistFailed,
      R stateUnknown,
      RuntimeException primary) {
    raw = originalRaw;
    territories = originalCache;
    try {
      dirtyMarker.markDirty();
      if (raw != originalRaw || territories != originalCache) {
        throw new IllegalStateException("management rollback identity changed");
      }
      if (!raw.equals(originalRawCopy)
          || !parseStrictRoot(raw).snapshots().equals(originalParsed)
          || !originalCache.equals(originalParsed)) {
        throw new IllegalStateException("management rollback content changed");
      }
      return persistFailed;
    } catch (RuntimeException rollback) {
      primary.addSuppressed(rollback);
      return stateUnknown;
    }
  }

  private record RawTarget(Owned snapshot, CompoundTag tag) {}

  /**
   * Reads the invitation view directly from the raw NBT record. The parsed cache is deliberately
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
      if (!(value instanceof CompoundTag record))
        throw integrity("territory record is not a compound");
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
    if (!(encodedMembers instanceof ListTag membersTag))
      throw integrity("AuthorizedPlayers is not a list");
    UUID ownerId = target.getUUID("OwnerUUID");
    Set<UUID> members = new HashSet<>();
    for (Tag value : membersTag) {
      if (!(value instanceof CompoundTag member)
          || !member.hasUUID("PlayerUUID")
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
      return Optional.of(
          new TerritoryInviteRequestService.Territory(
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
   * Adds one member using a copy-on-write raw NBT transaction. The expected owner is checked
   * against the current raw record, rather than the previously parsed snapshot, so an owner
   * transfer racing an invite cannot be overwritten. All fields except AuthorizedPlayers are
   * retained byte-for-byte by the defensive copy.
   */
  synchronized TerritoryInviteDecisionService.WriteResult authorize(
      UUID territoryId, UUID expectedOwner, UUID playerId, String playerName) {
    if (territoryId == null
        || expectedOwner == null
        || playerId == null
        || playerName == null
        || playerName.trim().isEmpty()
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
    ListTag candidateMembers =
        candidateTarget.getList("AuthorizedPlayers", Tag.TAG_COMPOUND).copy();
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
   * <p>The parsed {@link Owned} list is only a cache. Removal therefore validates the complete raw
   * document first, identifies the target by UUID, and then performs a copy-on-write update. No
   * snapshot re-encoding is used, which keeps unknown fields, list order, and future schema
   * additions intact.
   */
  @Override
  public synchronized TerritoryRemovalService.RepositoryOutcome remove(
      UUID territoryId, UUID expectedOwnerId) {
    if (territoryId == null || expectedOwnerId == null) {
      return repositoryState(
          TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
          new IllegalStateException("territory removal input is null"));
    }

    final StrictRoot source;
    try {
      source = parseStrictRoot(raw);
    } catch (RuntimeException malformed) {
      return repositoryState(TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN, malformed);
    }

    int targetIndex = -1;
    Owned targetSnapshot = null;
    for (int index = 0; index < source.snapshots().size(); index++) {
      Owned snapshot = source.snapshots().get(index);
      if (!territoryId.equals(snapshot.summary().territoryId())) continue;
      if (targetIndex >= 0) {
        // parseStrictRoot already rejects duplicates, but keep this guard next to the mutation.
        return repositoryState(
            TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
            new IllegalStateException("duplicate territory UUID"));
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
        return repositoryState(
            TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
            new IllegalStateException("candidate Territories is malformed"));
      }
      ListTag candidateList = sourceList.copy();
      if (targetIndex >= candidateList.size()) {
        return repositoryState(
            TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
            new IllegalStateException("candidate target index mismatch"));
      }
      candidateList.remove(targetIndex);
      candidateRoot.put("Territories", candidateList);

      // Parse the candidate before publishing it.  This validates every remaining record and
      // proves that the requested UUID is no longer present.
      StrictRoot reparsed = parseStrictRoot(candidateRoot);
      if (reparsed.snapshots().stream()
          .anyMatch(value -> territoryId.equals(value.summary().territoryId()))) {
        return repositoryState(
            TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
            new IllegalStateException("candidate still contains removed territory"));
      }
      candidateSnapshots = reparsed.snapshots();
    } catch (RuntimeException invalidCandidate) {
      return repositoryState(
          TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN, invalidCandidate);
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
        return repositoryUnknown(persistFailure);
      }
      try {
        // The original source was strict-parsed above.  Re-parse the restored document as a
        // defensive check so a faulty marker cannot leave a partially restored cache unnoticed.
        StrictRoot restored = parseStrictRoot(raw);
        if (restored.snapshots().size() != originalSnapshots.size()
            || restored.snapshots().stream()
                .noneMatch(value -> territoryId.equals(value.summary().territoryId()))) {
          return repositoryUnknown(persistFailure);
        }
      } catch (RuntimeException restorationFailure) {
        persistFailure.addSuppressed(restorationFailure);
        return repositoryUnknown(persistFailure);
      }
      return repositoryState(
          TerritoryRemovalService.RepositoryResult.PERSIST_FAILED, persistFailure);
    }

    TerritoryRemovalService.RemovedTerritory removed =
        new TerritoryRemovalService.RemovedTerritory(
            targetSnapshot.summary().territoryId(),
            targetSnapshot.summary().ownerId(),
            targetSnapshot.summary().name());
    return new TerritoryRemovalService.RepositoryOutcome(
        TerritoryRemovalService.RepositoryResult.REMOVED, removed);
  }

  private static TerritoryRemovalService.RepositoryOutcome repositoryState(
      TerritoryRemovalService.RepositoryResult result) {
    return new TerritoryRemovalService.RepositoryOutcome(result, null);
  }

  synchronized TerritoryMemberRemovalService.RepositoryOutcome removeMember(
      UUID territoryId, UUID expectedOwnerId, UUID targetPlayerId) {
    if (territoryId == null || expectedOwnerId == null || targetPlayerId == null)
      return memberUnknown(new IllegalStateException("member removal input is null"));
    try {
      parseStrictRoot(raw);
    } catch (RuntimeException malformed) {
      return memberIntegrity(malformed);
    }
    ListTag sourceTerritories = raw.getList("Territories", Tag.TAG_COMPOUND);
    int territoryIndex = -1;
    CompoundTag sourceTarget = null;
    for (int i = 0; i < sourceTerritories.size(); i++) {
      CompoundTag candidate = sourceTerritories.getCompound(i);
      if (territoryId.equals(candidate.getUUID("TerritoryID"))) {
        if (territoryIndex >= 0) return memberIntegrity(integrity("duplicate territory UUID"));
        territoryIndex = i;
        sourceTarget = candidate;
      }
    }
    if (territoryIndex < 0)
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.TERRITORY_NOT_FOUND, null);
    UUID ownerId = sourceTarget.getUUID("OwnerUUID");
    if (!expectedOwnerId.equals(ownerId))
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.OWNER_MISMATCH, null);
    if (targetPlayerId.equals(ownerId))
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.OWNER_TARGET, null);
    ListTag sourceMembers = sourceTarget.getList("AuthorizedPlayers", Tag.TAG_COMPOUND);
    int memberIndex = -1;
    String memberName = null;
    for (int i = 0; i < sourceMembers.size(); i++) {
      CompoundTag member = sourceMembers.getCompound(i);
      if (targetPlayerId.equals(member.getUUID("PlayerUUID"))) {
        if (memberIndex >= 0) return memberIntegrity(integrity("duplicate member UUID"));
        memberIndex = i;
        memberName = member.getString("PlayerName");
      }
    }
    if (memberIndex < 0)
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.TARGET_NOT_MEMBER, null);
    CompoundTag originalRaw = raw;
    CompoundTag originalRawDeepCopy = raw.copy();
    List<Owned> originalCache = territories;
    CompoundTag candidateRoot = raw.copy();
    ListTag candidateTerritories = candidateRoot.getList("Territories", Tag.TAG_COMPOUND).copy();
    CompoundTag candidateTarget = candidateTerritories.getCompound(territoryIndex).copy();
    ListTag candidateMembers =
        candidateTarget.getList("AuthorizedPlayers", Tag.TAG_COMPOUND).copy();
    candidateMembers.remove(memberIndex);
    candidateTarget.put("AuthorizedPlayers", candidateMembers);
    candidateTerritories.set(territoryIndex, candidateTarget);
    candidateRoot.put("Territories", candidateTerritories);
    List<Owned> candidateCache;
    CompoundTag candidateRawDeepCopy;
    try {
      candidateCache = parseStrictRoot(candidateRoot).snapshots();
      ListTag verifiedMembers =
          candidateRoot
              .getList("Territories", Tag.TAG_COMPOUND)
              .getCompound(territoryIndex)
              .getList("AuthorizedPlayers", Tag.TAG_COMPOUND);
      if (verifiedMembers.stream()
          .map(CompoundTag.class::cast)
          .anyMatch(member -> targetPlayerId.equals(member.getUUID("PlayerUUID")))) {
        throw integrity("candidate still contains target member");
      }
      if (verifiedMembers.size() != sourceMembers.size() - 1)
        throw integrity("candidate member count mismatch");
      for (int sourceIndex = 0, candidateIndex = 0;
          sourceIndex < sourceMembers.size();
          sourceIndex++) {
        if (sourceIndex == memberIndex) continue;
        if (!sourceMembers
            .getCompound(sourceIndex)
            .equals(verifiedMembers.getCompound(candidateIndex++)))
          throw integrity("candidate changed a remaining member record");
      }
      if (!parseStrictRoot(candidateRoot).snapshots().equals(candidateCache))
        throw integrity("candidate raw/cache mismatch");
      candidateRawDeepCopy = candidateRoot.copy();
    } catch (RuntimeException malformed) {
      return memberIntegrity(malformed);
    }
    RuntimeException primary;
    try {
      raw = candidateRoot;
      territories = candidateCache;
      dirtyMarker.markDirty();
      verifyPublishedMemberRemoval(
          raw,
          territories,
          candidateRawDeepCopy,
          candidateCache,
          territoryIndex,
          targetPlayerId,
          sourceMembers,
          memberIndex);
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.REMOVED,
          new TerritoryMemberRemovalService.RemovedMember(
              territoryId, ownerId, sourceTarget.getString("Name"), targetPlayerId, memberName));
    } catch (RuntimeException failure) {
      primary = failure;
    }
    try {
      raw = originalRaw;
      territories = originalCache;
      dirtyMarker.markDirty();
      if (raw != originalRaw || territories != originalCache)
        throw new IllegalStateException("rollback identity mismatch");
      StrictRoot restored = parseStrictRoot(raw);
      if (!raw.equals(originalRawDeepCopy)
          || !territories.equals(originalCache)
          || !restored.snapshots().equals(originalCache))
        throw new IllegalStateException("rollback content mismatch");
      ListTag restoredMembers =
          raw.getList("Territories", Tag.TAG_COMPOUND)
              .getCompound(territoryIndex)
              .getList("AuthorizedPlayers", Tag.TAG_COMPOUND);
      long restoredTargets =
          restoredMembers.stream()
              .map(CompoundTag.class::cast)
              .filter(member -> targetPlayerId.equals(member.getUUID("PlayerUUID")))
              .count();
      if (restoredTargets != 1
          || !memberName.equals(restoredMembers.getCompound(memberIndex).getString("PlayerName")))
        throw new IllegalStateException("rollback target mismatch");
      return new TerritoryMemberRemovalService.RepositoryOutcome(
          TerritoryMemberRemovalService.RepositoryResult.PERSIST_FAILED,
          null,
          TerritoryMemberRemovalService.RepositoryFailureKind.PERSISTENCE,
          primary);
    } catch (RuntimeException rollback) {
      primary.addSuppressed(rollback);
      return memberUnknown(primary);
    }
  }

  private static void verifyPublishedMemberRemoval(
      CompoundTag publishedRaw,
      List<Owned> publishedCache,
      CompoundTag candidateRawDeepCopy,
      List<Owned> candidateCache,
      int territoryIndex,
      UUID targetPlayerId,
      ListTag originalMembers,
      int removedIndex) {
    if (!publishedRaw.equals(candidateRawDeepCopy) || !publishedCache.equals(candidateCache))
      throw new IllegalStateException("post-dirty candidate changed");
    StrictRoot reparsed = parseStrictRoot(publishedRaw);
    if (!reparsed.snapshots().equals(candidateCache))
      throw new IllegalStateException("post-dirty raw/cache mismatch");
    ListTag members =
        publishedRaw
            .getList("Territories", Tag.TAG_COMPOUND)
            .getCompound(territoryIndex)
            .getList("AuthorizedPlayers", Tag.TAG_COMPOUND);
    if (members.size() != originalMembers.size() - 1
        || members.stream()
            .map(CompoundTag.class::cast)
            .anyMatch(member -> targetPlayerId.equals(member.getUUID("PlayerUUID"))))
      throw new IllegalStateException("post-dirty target removal mismatch");
    for (int sourceIndex = 0, candidateIndex = 0;
        sourceIndex < originalMembers.size();
        sourceIndex++) {
      if (sourceIndex == removedIndex) continue;
      if (!originalMembers.getCompound(sourceIndex).equals(members.getCompound(candidateIndex++)))
        throw new IllegalStateException("post-dirty member order/content mismatch");
    }
  }

  private static TerritoryMemberRemovalService.RepositoryOutcome memberIntegrity(
      Throwable failure) {
    return new TerritoryMemberRemovalService.RepositoryOutcome(
        TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN,
        null,
        TerritoryMemberRemovalService.RepositoryFailureKind.INTEGRITY,
        failure);
  }

  private static TerritoryMemberRemovalService.RepositoryOutcome memberUnknown(Throwable failure) {
    return new TerritoryMemberRemovalService.RepositoryOutcome(
        TerritoryMemberRemovalService.RepositoryResult.STATE_UNKNOWN,
        null,
        TerritoryMemberRemovalService.RepositoryFailureKind.UNKNOWN,
        failure);
  }

  private static TerritoryRemovalService.RepositoryOutcome repositoryState(
      TerritoryRemovalService.RepositoryResult result, Throwable failure) {
    TerritoryRemovalService.RepositoryFailureKind kind =
        result == TerritoryRemovalService.RepositoryResult.PERSIST_FAILED
            ? TerritoryRemovalService.RepositoryFailureKind.PERSISTENCE
            : TerritoryRemovalService.RepositoryFailureKind.INTEGRITY;
    return new TerritoryRemovalService.RepositoryOutcome(result, null, kind, failure);
  }

  private static TerritoryRemovalService.RepositoryOutcome repositoryUnknown(Throwable failure) {
    return new TerritoryRemovalService.RepositoryOutcome(
        TerritoryRemovalService.RepositoryResult.STATE_UNKNOWN,
        null,
        TerritoryRemovalService.RepositoryFailureKind.UNKNOWN,
        failure);
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
   * Strictly validates the schema needed to produce an {@link Owned} snapshot. Optional legacy
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
    value
        .backpoint()
        .ifPresent(
            point -> {
              CompoundTag backpoint = new CompoundTag();
              backpoint.putInt("BackX", point.x());
              backpoint.putInt("BackY", point.y());
              backpoint.putInt("BackZ", point.z());
              tag.put("Backpoint", backpoint);
            });
    CompoundTag permissions = new CompoundTag();
    for (Rule rule : value.rules())
      permissions.putString(rule.action().name(), rule.level().name());
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

  private static ListTag encodeBuffs(List<Buff> buffs) {
    ListTag encoded = new ListTag();
    for (Buff buff : buffs) encoded.add(encodeBuff(buff));
    return encoded;
  }

  private static TerritoryTeleportTarget teleportTarget(Owned value) {
    return new TerritoryTeleportTarget(
        value.summary().territoryId(),
        value.summary().name(),
        value.summary().ownerId(),
        value.authorizedMembers().stream()
            .map(Member::playerId)
            .collect(java.util.stream.Collectors.toUnmodifiableSet()),
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
    Summary summary =
        new Summary(
            tag.getUUID("TerritoryID"),
            tag.getUUID("OwnerUUID"),
            tag.getString("OwnerName"),
            tag.getString("Name"),
            new Position(tag.getInt("X1"), tag.getInt("Y1"), tag.getInt("Z1")),
            new Position(tag.getInt("X2"), tag.getInt("Y2"), tag.getInt("Z2")),
            dimension);
    List<Member> members = new ArrayList<>();
    for (Tag value : tag.getList("AuthorizedPlayers", Tag.TAG_COMPOUND)) {
      CompoundTag member = (CompoundTag) value;
      members.add(new Member(member.getUUID("PlayerUUID"), member.getString("PlayerName")));
    }
    Optional<Position> backpoint = Optional.empty();
    if (tag.contains("Backpoint", Tag.TAG_COMPOUND)) {
      CompoundTag point = tag.getCompound("Backpoint");
      backpoint =
          Optional.of(
              new Position(point.getInt("BackX"), point.getInt("BackY"), point.getInt("BackZ")));
    }
    Map<RuleAction, RuleLevel> levels = new EnumMap<>(RuleAction.class);
    for (RuleAction action : RuleAction.values()) levels.put(action, RuleLevel.MEMBERS);
    if (tag.contains("Permissions", Tag.TAG_COMPOUND)) {
      CompoundTag permissions = tag.getCompound("Permissions");
      for (RuleAction action : RuleAction.values()) {
        levels.put(action, permission(permissions.getString(action.name())));
      }
    }
    List<Rule> rules =
        levels.entrySet().stream()
            .map(value -> new Rule(value.getKey(), value.getValue()))
            .toList();
    List<Buff> buffs = new ArrayList<>();
    for (Tag value : tag.getList("TerritoryBuffs", Tag.TAG_COMPOUND))
      buffs.add(buff((CompoundTag) value));
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
    return new Buff(
        tag.getString("id"),
        tag.getString("displayText"),
        tag.getString("effectId"),
        tag.getBoolean("initialUnlockState"),
        tag.getInt("initialLevel"),
        tag.getInt("single_Upgrade_Level"),
        tag.getInt("max_Level"),
        tag.getBoolean("unlocked"),
        tag.getInt("level"),
        costs);
  }

  static RuleLevel permission(String stored) {
    return switch (stored) {
      case "OWNER_ONLY" -> RuleLevel.OWNER_ONLY;
      case "EVERYONE" -> RuleLevel.EVERYONE;
      case "MEMBERS" -> RuleLevel.MEMBERS;
      default -> RuleLevel.MEMBERS;
    };
  }

  public enum BuffCatalogSyncResult {
    UPDATED,
    UNCHANGED,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  static String canonicalDimension(String value) {
    if (value == null
        || value.isEmpty()
        || value.length() > EconomyNetworkLimits.MAX_ITEM_RESOURCE_ID_LENGTH) {
      throw new IllegalArgumentException("invalid territory dimension");
    }
    ResourceLocation parsed = ResourceLocation.tryParse(value);
    if (parsed == null || !parsed.toString().equals(value)) {
      throw new IllegalArgumentException("invalid territory dimension: " + value);
    }
    return value;
  }

  @Override
  public synchronized CompoundTag save(CompoundTag tag) {
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
