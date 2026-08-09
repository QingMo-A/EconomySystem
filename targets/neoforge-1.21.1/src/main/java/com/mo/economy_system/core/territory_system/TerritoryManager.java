package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritoryBuffCatalogPolicy;
import com.mo.economy_system.common.territory.TerritoryResizePlanner;
import com.mo.economy_system.common.territory.TerritorySnapshots.Buff;
import com.mo.economy_system.common.territory.TerritorySnapshots.BuffUpgradeCost;
import com.mo.economy_system.common.territory.TerritorySnapshots.ItemRequirement;
import com.mo.economy_system.common.territory.TerritorySnapshots.Owned;
import com.mo.economy_system.common.territory.TerritorySnapshots.Position;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class TerritoryManager {

  private static TerritorySavedData savedData;
  private static boolean initialized = false;

  private static final Map<UUID, Territory> territoryByID =
      Collections.synchronizedMap(new HashMap<>());
  private static final Map<UUID, List<Territory>> territoriesByOwner = new ConcurrentHashMap<>();

  // 初始化一个有限边界的四叉树
  public static QuadTree quadTree = new QuadTree(0, new Bounds(-30000, -30000, 60000, 60000));

  // 自动保存间隔（60秒）
  private static final long AUTO_SAVE_INTERVAL = 60000L;
  private static long lastSaveTime = System.currentTimeMillis();

  // 初始化领地管理器
  public static void initialize(ServerLevel level) {
    if (initialized) return;

    TerritoryBuffManager.initConfig(); // 初始化 Buff 配置

    quadTree.clear(); // 清空四叉树
    savedData = TerritorySavedData.getInstance(level);

    // ServerMessageUtil.log("Initializing TerritoryManager...");

    for (Territory territory : savedData.getAllTerritories()) {

      addTerritory(territory);
    }

    initialized = true;
    // ServerMessageUtil.log("TerritoryManager initialized with " + territoryByID.size() + "
    // territories.");
  }

  // 添加领地
  public static synchronized void addTerritory(Territory territory) {
    Objects.requireNonNull(territory, "territory");
    UUID territoryId = territory.getTerritoryID();
    if (territoryByID.containsKey(territoryId)) return;

    Territory savedInstance = savedData == null ? null : savedData.getTerritoryByID(territoryId);
    if (savedInstance != null && savedInstance != territory) {
      throw new IllegalStateException("territory persistence identity conflict");
    }

    List<Territory> ownerBucket =
        territoriesByOwner.computeIfAbsent(territory.getOwnerUUID(), ignored -> new ArrayList<>());
    boolean primaryAdded = false;
    boolean ownerAdded = false;
    boolean savedAdded = false;
    try {
      territoryByID.put(territoryId, territory);
      primaryAdded = true;
      ownerBucket.add(territory);
      ownerAdded = true;

      // QuadTree.insert may rebuild the index when the territory is outside its current bounds.
      if (!quadTree.getBounds().contains(territory.getBounds())) {
        expandQuadTreeBounds(territory.getBounds());
      }
      quadTree.insert(territory);

      if (savedData != null && savedInstance == null) {
        savedData.addTerritory(territory);
        savedAdded = true;
      }

      synchronizeTerritoryBuffs(territory);
      autoSave();
    } catch (RuntimeException failure) {
      // Remove every identity inserted by this attempt. The expanded tree bounds are harmless
      // and are intentionally retained; rebuilding them here could fail again and hide the cause.
      try {
        while (quadTree.countIdentity(territory) > 0) {
          if (!quadTree.remove(territory)) {
            throw new IllegalStateException("failed to remove territory from QuadTree");
          }
        }
      } catch (RuntimeException rollbackFailure) {
        failure.addSuppressed(rollbackFailure);
      }
      if (savedAdded && savedData != null) {
        try {
          if (savedData.getTerritoryByID(territoryId) == territory) {
            savedData.removeTerritory(territoryId);
          }
        } catch (RuntimeException rollbackFailure) {
          failure.addSuppressed(rollbackFailure);
        }
      }
      if (ownerAdded) {
        try {
          ownerBucket.removeIf(value -> value == territory);
          if (ownerBucket.isEmpty()) {
            territoriesByOwner.remove(territory.getOwnerUUID(), ownerBucket);
          }
        } catch (RuntimeException rollbackFailure) {
          failure.addSuppressed(rollbackFailure);
        }
      }
      if (primaryAdded) territoryByID.remove(territoryId, territory);
      throw failure;
    }
  }

  private static void synchronizeTerritoryBuffs(Territory territory) {
    if (!TerritoryBuffManager.isCatalogAvailable()) return;
    List<Buff> existing = territory.getTerritoryBuffs().stream()
        .map(TerritoryManager::snapshotBuff)
        .toList();
    var synchronizedBuffs = TerritoryBuffCatalogPolicy.synchronize(
        existing, TerritoryBuffManager.catalog());
    if (!synchronizedBuffs.changed()) return;
    territory.getTerritoryBuffs().clear();
    synchronizedBuffs.buffs().forEach(buff -> territory.addBuffs(nativeBuff(buff)));
    autoSave();
  }

  private static Buff snapshotBuff(TerritoryBuff buff) {
    List<BuffUpgradeCost> costs = new ArrayList<>();
    List<TerritoryBuffConfig.BuffUpgradeCost> configured = buff.getUpgradeCost();
    if (configured != null) {
      for (TerritoryBuffConfig.BuffUpgradeCost cost : configured) {
        if (cost == null) throw new IllegalArgumentException("null territory buff cost");
        List<ItemRequirement> items = new ArrayList<>();
        if (cost.items != null) {
          for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement item : cost.items) {
            if (item == null) throw new IllegalArgumentException("null territory buff item");
            items.add(new ItemRequirement(item.item, item.count));
          }
        }
        costs.add(new BuffUpgradeCost(items, cost.xp, cost.df_coin));
      }
    }
    return new Buff(
        buff.getId(), buff.getDisplayText(), buff.getEffectId(),
        buff.isInitialUnlockState(), buff.getInitialLevel(), buff.getSingleUpgradeLevel(),
        buff.getMaxLevel(), buff.isUnlocked(), buff.getLevel(), costs);
  }

  private static TerritoryBuff nativeBuff(Buff buff) {
    List<TerritoryBuffConfig.BuffUpgradeCost> costs = new ArrayList<>();
    for (BuffUpgradeCost cost : buff.upgradeCosts()) {
      TerritoryBuffConfig.BuffUpgradeCost nativeCost = new TerritoryBuffConfig.BuffUpgradeCost();
      nativeCost.xp = cost.experience();
      nativeCost.df_coin = cost.currency();
      nativeCost.items = cost.items().stream()
          .map(item -> new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement(
              item.itemId(), item.count()))
          .toList();
      costs.add(nativeCost);
    }
    TerritoryBuff result = new TerritoryBuff(
        buff.id(), buff.displayText(), buff.effectId(), buff.initialUnlocked(),
        buff.initialLevel(), buff.singleUpgradeLevel(), buff.maxLevel(), costs);
    result.setUnlocked(buff.unlocked());
    result.setLevel(buff.level());
    return result;
  }

  // 移除领地
  public static synchronized com.mo.economy_system.common.territory.TerritoryRemovalService
          .RepositoryOutcome
      removeTerritoryAuthoritatively(UUID territoryID, UUID expectedOwner) {
    if (territoryID == null || expectedOwner == null || savedData == null)
      return new com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult
              .STATE_UNKNOWN,
          null,
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryFailureKind
              .UNKNOWN,
          new IllegalStateException("SavedData unavailable or removal input invalid"));
    Territory territory = territoryByID.get(territoryID);
    if (territory == null)
      return new com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult.NOT_FOUND,
          null);
    if (!expectedOwner.equals(territory.getOwnerUUID()))
      return new com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult
              .OWNER_MISMATCH,
          null);
    List<Territory> ownerBucket = territoriesByOwner.get(expectedOwner);
    long ownerMatches =
        ownerBucket == null
            ? 0
            : ownerBucket.stream().filter(t -> territoryID.equals(t.getTerritoryID())).count();
    boolean wrongBucket =
        territoriesByOwner.entrySet().stream()
            .anyMatch(
                e ->
                    !e.getKey().equals(expectedOwner)
                        && e.getValue().stream()
                            .anyMatch(t -> territoryID.equals(t.getTerritoryID())));
    if (ownerMatches != 1
        || wrongBucket
        || savedData.getTerritoryByID(territoryID) != territory
        || quadTree.countIdentity(territory) != 1
        || quadTree.countTerritory(territoryID) != 1
        || !quadTree.isIndexedCorrectly(territory))
      return new com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult
              .STATE_UNKNOWN,
          null,
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryFailureKind
              .INTEGRITY,
          new IllegalStateException("territory removal invariant mismatch"));
    var snapshot =
        new com.mo.economy_system.common.territory.TerritoryRemovalService.RemovedTerritory(
            territoryID, expectedOwner, territory.getName());
    net.minecraft.core.BlockPos oldPos1 = territory.getPos1();
    net.minecraft.core.BlockPos oldPos2 = territory.getPos2();
    boolean primaryRemoved = false, ownerRemoved = false, treeRemoved = false, savedRemoved = false;
    try {
      primaryRemoved = territoryByID.remove(territoryID, territory);
      ownerRemoved = ownerBucket.remove(territory);
      if (ownerBucket.isEmpty()) territoriesByOwner.remove(expectedOwner, ownerBucket);
      treeRemoved = quadTree.remove(territory);
      savedRemoved = savedData.removeTerritory(territoryID) == territory;
      boolean ownerAbsent =
          territoriesByOwner.values().stream()
              .noneMatch(
                  list -> list.stream().anyMatch(t -> territoryID.equals(t.getTerritoryID())));
      if (!primaryRemoved
          || !ownerRemoved
          || !treeRemoved
          || !savedRemoved
          || territoryByID.containsKey(territoryID)
          || !ownerAbsent
          || quadTree.countIdentity(territory) != 0
          || quadTree.countTerritory(territoryID) != 0
          || queryContainsAnyRepresentativePoint(territory, oldPos1, oldPos2)
          || savedData.getTerritoryByID(territoryID) != null)
        throw new IllegalStateException("territory removal mutation failed");
      return new com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult.REMOVED,
          snapshot);
    } catch (RuntimeException failure) {
      boolean restored = true;
      try {
        if (territoryByID.putIfAbsent(territoryID, territory) != null
            && territoryByID.get(territoryID) != territory)
          throw new IllegalStateException("primary replacement conflict");
      } catch (RuntimeException e) {
        restored = false;
        failure.addSuppressed(e);
      }
      try {
        territoriesByOwner.computeIfAbsent(expectedOwner, k -> new ArrayList<>());
        if (!territoriesByOwner.get(expectedOwner).contains(territory))
          territoriesByOwner.get(expectedOwner).add(territory);
      } catch (RuntimeException e) {
        restored = false;
        failure.addSuppressed(e);
      }
      try {
        while (quadTree.countIdentity(territory) > 0) {
          if (!quadTree.remove(territory))
            throw new IllegalStateException("failed to remove stale territory identity");
        }
        if (quadTree.countTerritory(territoryID) != 0)
          throw new IllegalStateException("duplicate UUID with different identity in QuadTree");
        quadTree.insert(territory);
      } catch (RuntimeException e) {
        restored = false;
        failure.addSuppressed(e);
      }
      try {
        if (savedData.getTerritoryByID(territoryID) == null) savedData.restoreTerritory(territory);
      } catch (RuntimeException e) {
        restored = false;
        failure.addSuppressed(e);
      }
      long restoredOwnerMatches =
          territoriesByOwner.getOrDefault(expectedOwner, List.of()).stream()
              .filter(t -> territoryID.equals(t.getTerritoryID()))
              .count();
      boolean wrongRestoredBucket =
          territoriesByOwner.entrySet().stream()
              .anyMatch(
                  e ->
                      !e.getKey().equals(expectedOwner)
                          && e.getValue().stream()
                              .anyMatch(t -> territoryID.equals(t.getTerritoryID())));
      boolean verified =
          restored
              && territoryByID.get(territoryID) == territory
              && savedData.getTerritoryByID(territoryID) == territory
              && quadTree.countIdentity(territory) == 1
              && quadTree.countTerritory(territoryID) == 1
              && quadTree.isIndexedCorrectly(territory)
              && queryContainsAllRepresentativePoints(territory, oldPos1, oldPos2)
              && restoredOwnerMatches == 1
              && !wrongRestoredBucket;
      return new com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryOutcome(
          verified
              ? com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult
                  .PERSIST_FAILED
              : com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryResult
                  .STATE_UNKNOWN,
          null,
          verified
              ? com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryFailureKind
                  .PERSISTENCE
              : com.mo.economy_system.common.territory.TerritoryRemovalService.RepositoryFailureKind
                  .UNKNOWN,
          failure);
    }
  }

  public enum ResizeResult {
    RESIZED,
    UNCHANGED,
    CHANGED,
    TERRITORY_NOT_FOUND,
    OWNER_MISMATCH,
    INVALID_BOUNDS,
    OVERLAP,
    PERSIST_FAILED,
    STATE_UNKNOWN
  }

  public enum ResizePrepareResult {
    READY,
    UNCHANGED,
    TERRITORY_NOT_FOUND,
    OWNER_MISMATCH,
    INVALID_BOUNDS,
    OVERLAP,
    PRICE_OVERFLOW,
    STATE_UNKNOWN
  }

  public record ResizePlan(
      UUID territoryId,
      UUID expectedOwnerId,
      Territory expectedInstance,
      net.minecraft.core.BlockPos oldPos1,
      net.minecraft.core.BlockPos oldPos2,
      net.minecraft.core.BlockPos oldBackpoint,
      net.minecraft.core.BlockPos newPos1,
      net.minecraft.core.BlockPos newPos2,
      net.minecraft.core.BlockPos newBackpoint,
      long oldArea,
      long newArea,
      long areaDifference,
      int charge) {}

  public record ResizePrepareOutcome(
      ResizePrepareResult result, ResizePlan plan, Throwable failure) {
    public ResizePrepareOutcome {
      Objects.requireNonNull(result);
      if ((result == ResizePrepareResult.READY) != (plan != null))
        throw new IllegalArgumentException("prepare result/plan");
      if (failure instanceof Error error) throw error;
    }

    static ResizePrepareOutcome of(ResizePrepareResult result) {
      return new ResizePrepareOutcome(result, null, null);
    }
  }

  public record ResizeOutcome(ResizeResult result, Throwable failure) {
    public ResizeOutcome {
      Objects.requireNonNull(result);
      if (failure instanceof Error error) throw error;
    }

    static ResizeOutcome of(ResizeResult result) {
      return new ResizeOutcome(result, null);
    }
  }

  static synchronized ResizePrepareOutcome prepareTerritoryResize(
      UUID territoryID,
      UUID expectedOwner,
      net.minecraft.core.BlockPos newPos1,
      net.minecraft.core.BlockPos newPos2,
      net.minecraft.core.BlockPos newBackpoint) {
    if (territoryID == null
        || expectedOwner == null
        || newPos1 == null
        || newPos2 == null
        || newBackpoint == null
        || !TerritoryResizePlanner.validCandidate(position(newPos1), position(newPos2)))
      return ResizePrepareOutcome.of(ResizePrepareResult.INVALID_BOUNDS);
    Territory territory = territoryByID.get(territoryID);
    if (territory == null) return ResizePrepareOutcome.of(ResizePrepareResult.TERRITORY_NOT_FOUND);
    if (!expectedOwner.equals(territory.getOwnerUUID())) {
      return ResizePrepareOutcome.of(ResizePrepareResult.OWNER_MISMATCH);
    }
    IllegalStateException invariant =
        territoryIdentityInvariant(territoryID, expectedOwner, territory);
    if (invariant != null)
      return new ResizePrepareOutcome(ResizePrepareResult.STATE_UNKNOWN, null, invariant);
    try {
      Owned target = TerritoryNetworkSnapshots.owned(territory);
      List<Owned> all = territoryByID.values().stream()
          .map(TerritoryNetworkSnapshots::owned)
          .toList();
      String dimension = territory.getDimension().location().toString();
      TerritoryResizePlanner.PlanningOutcome planning = TerritoryResizePlanner.prepare(
          territoryID,
          expectedOwner,
          dimension,
          position(newPos1),
          position(newPos2),
          position(newBackpoint),
          target,
          all);
      if (planning.result()
          != com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult.READY) {
        return new ResizePrepareOutcome(
            mapPlanningResult(planning.result()), null, planning.failure());
      }
      TerritoryResizePlanner.Plan commonPlan = planning.plan();
      return new ResizePrepareOutcome(
          ResizePrepareResult.READY,
          new ResizePlan(
              territoryID,
              expectedOwner,
              territory,
              territory.getPos1(),
              territory.getPos2(),
              territory.getBackpoint(),
              newPos1,
              newPos2,
              newBackpoint,
              commonPlan.oldArea(),
              commonPlan.newArea(),
              commonPlan.areaDifference(),
              commonPlan.charge()),
          null);
    } catch (RuntimeException failure) {
      return new ResizePrepareOutcome(ResizePrepareResult.STATE_UNKNOWN, null, failure);
    }
  }

  static synchronized ResizeOutcome commitTerritoryResize(ResizePlan plan) {
    Objects.requireNonNull(plan);
    Territory current = territoryByID.get(plan.territoryId());
    if (current == null) return ResizeOutcome.of(ResizeResult.TERRITORY_NOT_FOUND);
    if (current != plan.expectedInstance()
        || !plan.expectedOwnerId().equals(current.getOwnerUUID())
        || !plan.oldPos1().equals(current.getPos1())
        || !plan.oldPos2().equals(current.getPos2())
        || !Objects.equals(plan.oldBackpoint(), current.getBackpoint()))
      return ResizeOutcome.of(ResizeResult.CHANGED);
    IllegalStateException invariant =
        territoryIdentityInvariant(plan.territoryId(), plan.expectedOwnerId(), current);
    if (invariant != null) return new ResizeOutcome(ResizeResult.STATE_UNKNOWN, invariant);
    if (overlapsOther(current, plan.newPos1(), plan.newPos2()))
      return ResizeOutcome.of(ResizeResult.OVERLAP);
    return resizeTerritoryAuthoritatively(
        plan.territoryId(),
        plan.expectedOwnerId(),
        plan.newPos1(),
        plan.newPos2(),
        plan.newBackpoint());
  }

  private static boolean overlapsOther(
      Territory territory,
      net.minecraft.core.BlockPos first,
      net.minecraft.core.BlockPos second) {
    List<Owned> all = territoryByID.values().stream()
        .map(TerritoryNetworkSnapshots::owned)
        .toList();
    return TerritoryResizePlanner.overlapsOther(
        all,
        territory.getTerritoryID(),
        territory.getDimension().location().toString(),
        position(first),
        position(second));
  }

  static synchronized ResizeOutcome resizeTerritoryAuthoritatively(
      UUID territoryID,
      UUID expectedOwner,
      net.minecraft.core.BlockPos newPos1,
      net.minecraft.core.BlockPos newPos2,
      net.minecraft.core.BlockPos newBackpoint) {
    if (territoryID == null
        || expectedOwner == null
        || newPos1 == null
        || newPos2 == null
        || newBackpoint == null) {
      return ResizeOutcome.of(ResizeResult.INVALID_BOUNDS);
    }
    Territory territory = territoryByID.get(territoryID);
    if (territory == null) return ResizeOutcome.of(ResizeResult.TERRITORY_NOT_FOUND);
    if (!expectedOwner.equals(territory.getOwnerUUID()))
      return ResizeOutcome.of(ResizeResult.OWNER_MISMATCH);
    if (!TerritoryResizePlanner.validCandidate(position(newPos1), position(newPos2))) {
      return ResizeOutcome.of(ResizeResult.INVALID_BOUNDS);
    }
    IllegalStateException invariant =
        territoryIdentityInvariant(territoryID, expectedOwner, territory);
    if (invariant != null) return new ResizeOutcome(ResizeResult.STATE_UNKNOWN, invariant);
    boolean overlaps = overlapsOther(territory, newPos1, newPos2);
    if (overlaps) return ResizeOutcome.of(ResizeResult.OVERLAP);

    net.minecraft.core.BlockPos oldPos1 = territory.getPos1();
    net.minecraft.core.BlockPos oldPos2 = territory.getPos2();
    net.minecraft.core.BlockPos oldBackpoint = territory.getBackpoint();
    RuntimeException failure = null;
    try {
      if (!quadTree.remove(territory)
          || quadTree.countIdentity(territory) != 0
          || quadTree.countTerritory(territoryID) != 0) {
        throw new IllegalStateException("failed to remove old QuadTree entry");
      }
      applyBounds(territory, newPos1, newPos2, newBackpoint);
      quadTree.insert(territory);
      if (!spatiallyVerified(territory, oldPos1, oldPos2))
        throw new IllegalStateException("new QuadTree spatial verification failed");
      IllegalStateException finalInvariant =
          territoryIdentityInvariant(territoryID, expectedOwner, territory);
      if (finalInvariant != null) throw finalInvariant;
      savedData.setDirty();
      return ResizeOutcome.of(ResizeResult.RESIZED);
    } catch (RuntimeException mutationFailure) {
      failure = mutationFailure;
    }
    boolean restored = true;
    try {
      while (quadTree.countIdentity(territory) > 0) {
        if (!quadTree.remove(territory))
          throw new IllegalStateException("failed to remove stale territory identity");
      }
      if (quadTree.countTerritory(territoryID) != 0)
        throw new IllegalStateException("duplicate UUID with different identity in QuadTree");
    } catch (RuntimeException compensation) {
      restored = false;
      failure.addSuppressed(compensation);
    }
    try {
      applyBounds(territory, oldPos1, oldPos2, oldBackpoint);
    } catch (RuntimeException compensation) {
      restored = false;
      failure.addSuppressed(compensation);
    }
    try {
      if (quadTree.countIdentity(territory) == 0) quadTree.insert(territory);
    } catch (RuntimeException compensation) {
      restored = false;
      failure.addSuppressed(compensation);
    }
    try {
      savedData.setDirty();
    } catch (RuntimeException compensation) {
      restored = false;
      failure.addSuppressed(compensation);
    }
    IllegalStateException restoredInvariant =
        territoryIdentityInvariant(territoryID, expectedOwner, territory);
    boolean oldState =
        oldPos1.equals(territory.getPos1())
            && oldPos2.equals(territory.getPos2())
            && Objects.equals(oldBackpoint, territory.getBackpoint())
            && spatiallyVerified(territory, newPos1, newPos2);
    if (!restored || restoredInvariant != null || !oldState) {
      if (restoredInvariant != null) failure.addSuppressed(restoredInvariant);
      return new ResizeOutcome(ResizeResult.STATE_UNKNOWN, failure);
    }
    return new ResizeOutcome(ResizeResult.PERSIST_FAILED, failure);
  }

  private static Position position(net.minecraft.core.BlockPos value) {
    return new Position(value.getX(), value.getY(), value.getZ());
  }

  private static ResizePrepareResult mapPlanningResult(
      com.mo.economy_system.common.territory.TerritoryResizeTransactionService.PrepareResult result) {
    return switch (result) {
      case READY -> ResizePrepareResult.READY;
      case UNCHANGED -> ResizePrepareResult.UNCHANGED;
      case TERRITORY_NOT_FOUND -> ResizePrepareResult.TERRITORY_NOT_FOUND;
      case NO_PERMISSION -> ResizePrepareResult.OWNER_MISMATCH;
      case WRONG_DIMENSION -> ResizePrepareResult.STATE_UNKNOWN;
      case INVALID_BOUNDS -> ResizePrepareResult.INVALID_BOUNDS;
      case OVERLAP -> ResizePrepareResult.OVERLAP;
      case PRICE_OVERFLOW -> ResizePrepareResult.PRICE_OVERFLOW;
      case STATE_UNKNOWN -> ResizePrepareResult.STATE_UNKNOWN;
    };
  }

  private static boolean spatiallyVerified(
      Territory territory,
      net.minecraft.core.BlockPos excludedFirst,
      net.minecraft.core.BlockPos excludedSecond) {
    if (quadTree.countTerritory(territory.getTerritoryID()) != 1
        || !quadTree.isIndexedCorrectly(territory)) return false;
    Bounds bounds = territory.getBounds();
    int maxX = bounds.x + bounds.width;
    int maxZ = bounds.z + bounds.height;
    if (!queryContains(territory, bounds.x + bounds.width / 2, bounds.z + bounds.height / 2)
        || !queryContains(territory, bounds.x, bounds.z)
        || !queryContains(territory, maxX, maxZ)) return false;
    for (net.minecraft.core.BlockPos point : List.of(excludedFirst, excludedSecond)) {
      if (!territory.isWithinBoundsIgnoreY(point.getX(), point.getZ())
          && queryContains(territory, point.getX(), point.getZ())) return false;
    }
    return true;
  }

  private static boolean queryContains(Territory territory, int x, int z) {
    return quadTree.query(x, z).stream().anyMatch(candidate -> candidate == territory);
  }

  private static boolean queryContainsAnyRepresentativePoint(
      Territory territory, net.minecraft.core.BlockPos first, net.minecraft.core.BlockPos second) {
    Bounds bounds = Bounds.calculateBounds(first, second);
    return queryContains(territory, bounds.x, bounds.z)
        || queryContains(territory, bounds.x + bounds.width / 2, bounds.z + bounds.height / 2)
        || queryContains(territory, bounds.x + bounds.width, bounds.z + bounds.height);
  }

  private static boolean queryContainsAllRepresentativePoints(
      Territory territory, net.minecraft.core.BlockPos first, net.minecraft.core.BlockPos second) {
    Bounds bounds = Bounds.calculateBounds(first, second);
    return queryContains(territory, bounds.x, bounds.z)
        && queryContains(territory, bounds.x + bounds.width / 2, bounds.z + bounds.height / 2)
        && queryContains(territory, bounds.x + bounds.width, bounds.z + bounds.height);
  }

  private static void applyBounds(
      Territory territory,
      net.minecraft.core.BlockPos first,
      net.minecraft.core.BlockPos second,
      net.minecraft.core.BlockPos backpoint) {
    territory.setX1(first.getX());
    territory.setY1(first.getY());
    territory.setZ1(first.getZ());
    territory.setX2(second.getX());
    territory.setY2(second.getY());
    territory.setZ2(second.getZ());
    territory.setBackpoint(backpoint);
  }

  private static IllegalStateException territoryIdentityInvariant(
      UUID territoryID, UUID owner, Territory territory) {
    try {
      List<Territory> correct = territoriesByOwner.get(owner);
      long correctCount =
          correct == null
              ? 0
              : correct.stream()
                  .filter(value -> value != null && territoryID.equals(value.getTerritoryID()))
                  .count();
      boolean wrongBucket =
          territoriesByOwner.entrySet().stream()
              .anyMatch(
                  entry ->
                      !Objects.equals(entry.getKey(), owner)
                          && entry.getValue() != null
                          && entry.getValue().stream()
                              .anyMatch(
                                  value ->
                                      value != null && territoryID.equals(value.getTerritoryID())));
      if (territoryByID.get(territoryID) != territory)
        return new IllegalStateException("primary map instance mismatch");
      if (correctCount != 1 || wrongBucket)
        return new IllegalStateException("owner index mismatch");
      if (savedData == null || savedData.getTerritoryByID(territoryID) != territory)
        return new IllegalStateException("SavedData instance mismatch");
      if (quadTree.countIdentity(territory) != 1
          || quadTree.countTerritory(territoryID) != 1
          || !quadTree.isIndexedCorrectly(territory))
        return new IllegalStateException("QuadTree spatial index mismatch");
      return null;
    } catch (RuntimeException failure) {
      return new IllegalStateException("territory identity invariant inspection failed", failure);
    }
  }

  public static synchronized com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome
      removeTerritoryMemberAuthoritatively(
          UUID territoryID, UUID expectedOwner, UUID targetPlayerID) {
    if (territoryID == null || expectedOwner == null || targetPlayerID == null || savedData == null)
      return new com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
              .STATE_UNKNOWN,
          null,
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryFailureKind
              .UNKNOWN,
          new IllegalStateException("invalid member removal state"));
    Territory territory = territoryByID.get(territoryID);
    if (territory == null)
      return new com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
              .TERRITORY_NOT_FOUND,
          null);
    if (!expectedOwner.equals(territory.getOwnerUUID()))
      return new com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
              .OWNER_MISMATCH,
          null);
    if (expectedOwner.equals(targetPlayerID))
      return new com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
              .OWNER_TARGET,
          null);
    IllegalStateException invariant =
        territoryIdentityInvariant(territoryID, expectedOwner, territory);
    if (invariant != null)
      return new com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome(
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
              .STATE_UNKNOWN,
          null,
          com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryFailureKind
              .INTEGRITY,
          invariant);
    java.util.Map<UUID, String> before;
    try {
      before = TerritoryMemberRemovalMutation.snapshot(territory);
    } catch (RuntimeException failure) {
      return memberIntegrityOutcome(failure);
    }
    var outcome =
        TerritoryMemberRemovalMutation.remove(
            territory, expectedOwner, targetPlayerID, savedData::setDirty);
    IllegalStateException after = territoryIdentityInvariant(territoryID, expectedOwner, territory);
    if (after != null) return memberIntegrityOutcome(after);
    try {
      java.util.Map<UUID, String> current = TerritoryMemberRemovalMutation.snapshot(territory);
      boolean validState =
          switch (outcome.result()) {
            case REMOVED -> {
              java.util.Map<UUID, String> expected = new java.util.LinkedHashMap<>(before);
              expected.remove(targetPlayerID);
              var removed = outcome.removedMember();
              yield current.equals(expected)
                  && !territory.hasPermission(targetPlayerID)
                  && removed.territoryId().equals(territoryID)
                  && removed.ownerId().equals(expectedOwner)
                  && removed.targetPlayerId().equals(targetPlayerID)
                  && java.util.Objects.equals(
                      removed.targetPlayerName(), before.get(targetPlayerID))
                  && java.util.Objects.equals(removed.territoryName(), territory.getName());
            }
            case PERSIST_FAILED -> current.equals(before);
            case STATE_UNKNOWN -> true;
            case TARGET_NOT_MEMBER -> !before.containsKey(targetPlayerID) && current.equals(before);
            case OWNER_MISMATCH, OWNER_TARGET, TERRITORY_NOT_FOUND -> current.equals(before);
          };
      if (!validState)
        return memberIntegrityOutcome(
            new IllegalStateException("member repository outcome conflicts with final state"));
    } catch (RuntimeException failure) {
      return memberIntegrityOutcome(failure);
    }
    return outcome;
  }

  private static com.mo.economy_system.common.territory.TerritoryMemberRemovalService
          .RepositoryOutcome
      memberIntegrityOutcome(Throwable failure) {
    return new com.mo.economy_system.common.territory.TerritoryMemberRemovalService
        .RepositoryOutcome(
        com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryResult
            .STATE_UNKNOWN,
        null,
        com.mo.economy_system.common.territory.TerritoryMemberRemovalService.RepositoryFailureKind
            .INTEGRITY,
        failure);
  }

  /** Authoritative invite acceptance mutation with an owner compare-and-set guard. */
  public static synchronized com.mo.economy_system.common.territory.TerritoryInviteDecisionService
          .WriteResult
      authorizeInvitedPlayer(
          UUID territoryID, UUID expectedOwner, UUID playerUUID, String playerName) {
    if (territoryID == null
        || expectedOwner == null
        || playerUUID == null
        || playerName == null
        || playerName.isBlank()
        || playerName.length() > EconomyNetworkLimits.MAX_PLAYER_NAME_LENGTH)
      return com.mo.economy_system.common.territory.TerritoryInviteDecisionService.WriteResult
          .PERSIST_FAILED;
    if (savedData == null)
      return com.mo.economy_system.common.territory.TerritoryInviteDecisionService.WriteResult
          .PERSIST_FAILED;
    Territory territory = getTerritoryByID(territoryID);
    if (territory == null)
      return com.mo.economy_system.common.territory.TerritoryInviteDecisionService.WriteResult
          .TERRITORY_NOT_FOUND;
    if (!territory.getOwnerUUID().equals(expectedOwner))
      return com.mo.economy_system.common.territory.TerritoryInviteDecisionService.WriteResult
          .OWNER_CHANGED;
    return TerritoryInviteMembershipMutation.mutate(
        territory, expectedOwner, playerUUID, playerName, savedData::setDirty);
  }

  /** Applies a complete common administration replacement to the live native territory. */
  public static synchronized com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
      applyTerritoryAdministrationAuthoritatively(
          com.mo.economy_system.common.territory.TerritorySnapshots.Owned expected,
          com.mo.economy_system.common.territory.TerritorySnapshots.Owned replacement) {
    if (!com.mo.economy_system.common.territory.TerritoryAdministrationService
        .isValidReplacement(expected, replacement)) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .INVALID_TARGET;
    }
    if (savedData == null) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .STATE_UNKNOWN;
    }
    UUID territoryID = expected.summary().territoryId();
    Territory territory = territoryByID.get(territoryID);
    if (territory == null) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .NOT_FOUND;
    }
    if (!territory.isOwner(expected.summary().ownerId())) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .OWNER_CHANGED;
    }

    com.mo.economy_system.common.territory.TerritorySnapshots.Owned before;
    try {
      before = TerritoryNetworkSnapshots.owned(territory);
    } catch (RuntimeException failure) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .STATE_UNKNOWN;
    }
    if (!before.equals(expected)) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .STATE_UNKNOWN;
    }
    if (before.equals(replacement)) {
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .NO_CHANGE;
    }

    Map<UUID, List<Territory>> ownerIndexBefore = copyOwnerIndex();
    UUID oldOwner = before.summary().ownerId();
    UUID newOwner = replacement.summary().ownerId();
    try {
      IllegalStateException beforeInvariant = territoryIdentityInvariant(territoryID, oldOwner, territory);
      if (beforeInvariant != null) throw beforeInvariant;
      if (!oldOwner.equals(newOwner)) {
        List<Territory> oldBucket = territoriesByOwner.get(oldOwner);
        if (oldBucket == null || !oldBucket.remove(territory)) {
          throw new IllegalStateException("old owner index mismatch");
        }
        if (oldBucket.isEmpty()) territoriesByOwner.remove(oldOwner, oldBucket);
        List<Territory> newBucket = territoriesByOwner.get(newOwner);
        if (newBucket != null
            && newBucket.stream().anyMatch(value -> territoryID.equals(value.getTerritoryID()))) {
          throw new IllegalStateException("new owner already indexes territory");
        }
      }

      applyAdministrationMutableState(territory, replacement);
      if (!oldOwner.equals(newOwner)) {
        territoriesByOwner.computeIfAbsent(newOwner, ignored -> new ArrayList<>()).add(territory);
      }
      IllegalStateException afterInvariant = territoryIdentityInvariant(territoryID, newOwner, territory);
      if (afterInvariant != null) throw afterInvariant;
      if (!TerritoryNetworkSnapshots.owned(territory).equals(replacement)) {
        throw new IllegalStateException("territory administration replacement did not apply");
      }
      savedData.setDirty();
      verifyAdministrationPublished(territory, newOwner, replacement);
      return com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
          .SUCCESS;
    } catch (RuntimeException failure) {
      boolean restored = restoreMutableState(territory, before);
      restored &= restoreOwnerIndex(ownerIndexBefore);
      try {
        savedData.setDirty();
      } catch (RuntimeException compensation) {
        restored = false;
        failure.addSuppressed(compensation);
      }
      IllegalStateException invariant = territoryIdentityInvariant(territoryID, oldOwner, territory);
      restored &= invariant == null && TerritoryNetworkSnapshots.owned(territory).equals(before);
      return restored
          ? com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
              .PERSIST_FAILED
          : com.mo.economy_system.common.territory.TerritoryAdministrationService.RepositoryResult
              .STATE_UNKNOWN;
    }
  }

  private static void applyAdministrationMutableState(
      Territory territory,
      com.mo.economy_system.common.territory.TerritorySnapshots.Owned replacement) {
    territory.setOwner(replacement.summary().ownerId(), replacement.summary().ownerName());
    territory.getAuthorizedPlayers().clear();
    for (com.mo.economy_system.common.territory.TerritorySnapshots.Member member
        : replacement.authorizedMembers()) {
      territory.addAuthorizedPlayer(member.playerId(), member.playerName());
    }
    for (com.mo.economy_system.common.territory.TerritorySnapshots.Rule rule : replacement.rules()) {
      territory.setPermissionLevel(nativeAction(rule.action()), nativeLevel(rule.level()));
    }
  }

  private static Map<UUID, List<Territory>> copyOwnerIndex() {
    Map<UUID, List<Territory>> copy = new HashMap<>();
    for (Map.Entry<UUID, List<Territory>> entry : territoriesByOwner.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return copy;
  }

  private static boolean restoreOwnerIndex(Map<UUID, List<Territory>> snapshot) {
    try {
      for (Map.Entry<UUID, List<Territory>> entry : snapshot.entrySet()) {
        List<Territory> current = territoriesByOwner.computeIfAbsent(
            entry.getKey(), ignored -> new ArrayList<>());
        current.clear();
        current.addAll(entry.getValue());
      }
      territoriesByOwner.keySet().removeIf(key -> !snapshot.containsKey(key));
      return true;
    } catch (RuntimeException failure) {
      return false;
    }
  }

  /** CAS-protected native persistence adapter for the common backpoint command policy. */
  public static synchronized com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
      setTerritoryBackpointAuthoritatively(
          UUID territoryID,
          UUID expectedOwner,
          Optional<Position> expectedBackpoint,
          Position newBackpoint) {
    if (territoryID == null
        || expectedOwner == null
        || expectedBackpoint == null
        || newBackpoint == null
        || savedData == null) {
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .STATE_UNKNOWN;
    }
    Territory territory = territoryByID.get(territoryID);
    if (territory == null) {
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .NOT_FOUND;
    }
    if (!territory.isOwner(expectedOwner)) {
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .OWNER_CHANGED;
    }
    com.mo.economy_system.common.territory.TerritorySnapshots.Owned before;
    try {
      before = TerritoryNetworkSnapshots.owned(territory);
    } catch (RuntimeException failure) {
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .STATE_UNKNOWN;
    }
    if (!expectedBackpoint.equals(before.backpoint())) {
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .SNAPSHOT_CHANGED;
    }
    if (before.backpoint().equals(Optional.of(newBackpoint))) {
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .UNCHANGED;
    }
    net.minecraft.core.BlockPos oldBackpoint = territory.getBackpoint();
    try {
      territory.setBackpoint(new net.minecraft.core.BlockPos(
          newBackpoint.x(), newBackpoint.y(), newBackpoint.z()));
      com.mo.economy_system.common.territory.TerritorySnapshots.Owned expected =
          TerritoryNetworkSnapshots.owned(territory);
      savedData.setDirty();
      verifyAdministrationPublished(territory, expectedOwner, expected);
      return com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
          .UPDATED;
    } catch (RuntimeException failure) {
      boolean restored = true;
      try {
        territory.setBackpoint(oldBackpoint);
        savedData.setDirty();
      } catch (RuntimeException compensation) {
        restored = false;
        failure.addSuppressed(compensation);
      }
      try {
        restored &= TerritoryNetworkSnapshots.owned(territory).equals(before);
      } catch (RuntimeException verification) {
        restored = false;
        failure.addSuppressed(verification);
      }
      return restored
          ? com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
              .PERSIST_FAILED
          : com.mo.economy_system.common.territory.TerritoryBackpointService.RepositoryResult
              .STATE_UNKNOWN;
    }
  }

  public static synchronized com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult
      mutateTerritoryBuffAuthoritatively(
          UUID territoryID,
          UUID expectedOwner,
          String buffID,
          boolean expectedUnlocked,
          int expectedLevel,
          boolean newUnlocked,
          int newLevel) {
    if (territoryID == null || expectedOwner == null || buffID == null || savedData == null) {
      return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.STATE_UNKNOWN;
    }
    Territory territory = territoryByID.get(territoryID);
    if (territory == null) {
      return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.NOT_FOUND;
    }
    if (!territory.isOwner(expectedOwner)) {
      return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.OWNER_CHANGED;
    }
    TerritoryBuff buff = territory.getBuff(buffID);
    if (buff == null || buff.isUnlocked() != expectedUnlocked || buff.getLevel() != expectedLevel) {
      return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED;
    }
    com.mo.economy_system.common.territory.TerritorySnapshots.Owned before;
    try {
      before = TerritoryNetworkSnapshots.owned(territory);
    } catch (RuntimeException failure) {
      return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.STATE_UNKNOWN;
    }
    try {
      if (newUnlocked == expectedUnlocked && newLevel == expectedLevel) {
        return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.BUFF_CHANGED;
      }
      buff.setUnlocked(newUnlocked);
      buff.setLevel(newLevel);
      com.mo.economy_system.common.territory.TerritorySnapshots.Owned expected =
          TerritoryNetworkSnapshots.owned(territory);
      savedData.setDirty();
      verifyAdministrationPublished(territory, expectedOwner, expected);
      return com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.SUCCESS;
    } catch (RuntimeException failure) {
      boolean restored = restoreMutableState(territory, before);
      try {
        savedData.setDirty();
      } catch (RuntimeException compensation) {
        restored = false;
        failure.addSuppressed(compensation);
      }
      restored &= TerritoryNetworkSnapshots.owned(territory).equals(before);
      return restored
          ? com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.PERSIST_FAILED
          : com.mo.economy_system.common.territory.TerritoryBuffTransactionService.RepositoryResult.STATE_UNKNOWN;
    }
  }

  private static void verifyAdministrationPublished(
      Territory territory,
      UUID expectedOwner,
      com.mo.economy_system.common.territory.TerritorySnapshots.Owned expected) {
    IllegalStateException invariant =
        territoryIdentityInvariant(territory.getTerritoryID(), expectedOwner, territory);
    if (invariant != null) throw invariant;
    if (!TerritoryNetworkSnapshots.owned(territory).equals(expected)) {
      throw new IllegalStateException("territory management state changed during persistence");
    }
  }

  private static boolean restoreMutableState(
      Territory territory,
      com.mo.economy_system.common.territory.TerritorySnapshots.Owned snapshot) {
    try {
      Territory restored = TerritoryNetworkSnapshots.restoreOwned(snapshot);
      territory.setOwner(restored.getOwnerUUID(), restored.getOwnerName());
      territory.getAuthorizedPlayers().clear();
      for (PlayerInfo member : restored.getAuthorizedPlayers()) {
        territory.addAuthorizedPlayer(member.getUuid(), member.getName());
      }
      for (TerritoryPermissionAction action : TerritoryPermissionAction.values()) {
        territory.setPermissionLevel(action, restored.getPermissionLevel(action));
      }
      territory.getTerritoryBuffs().clear();
      territory.getTerritoryBuffs().addAll(restored.getTerritoryBuffs());
      return true;
    } catch (RuntimeException failure) {
      return false;
    }
  }

  private static TerritoryPermissionAction nativeAction(
      com.mo.economy_system.common.territory.TerritorySnapshots.RuleAction action) {
    return switch (action) {
      case PLACE_BLOCK -> TerritoryPermissionAction.PLACE_BLOCK;
      case BREAK_BLOCK -> TerritoryPermissionAction.BREAK_BLOCK;
      case USE_ITEM -> TerritoryPermissionAction.USE_ITEM;
      case INTERACT_BLOCK -> TerritoryPermissionAction.INTERACT_BLOCK;
      case OPEN_CONTAINER -> TerritoryPermissionAction.OPEN_CONTAINER;
    };
  }

  private static TerritoryPermissionLevel nativeLevel(
      com.mo.economy_system.common.territory.TerritorySnapshots.RuleLevel level) {
    return switch (level) {
      case OWNER_ONLY -> TerritoryPermissionLevel.OWNER_ONLY;
      case MEMBERS -> TerritoryPermissionLevel.MEMBERS;
      case EVERYONE -> TerritoryPermissionLevel.EVERYONE;
    };
  }

  // 查询指定维度和 X/Z 位置的领地。领地保护必须带维度判断，否则不同维度同坐标会互相误判。
  public static Territory getTerritoryAtIgnoreY(ResourceKey<Level> dimension, int x, int z) {
    List<Territory> candidates = quadTree.query(x, z);
    return candidates.stream()
        .filter(territory -> territory.getDimension().equals(dimension))
        .filter(territory -> territory.isWithinBoundsIgnoreY(x, z))
        .findFirst()
        .orElse(null);
  }

  // 获取玩家拥有的所有领地
  public static List<Territory> getTerritoriesByOwner(UUID ownerUUID) {
    return territoriesByOwner.getOrDefault(ownerUUID, new ArrayList<>());
  }

  // 获取玩家有权限的领地（排除自己拥有的领地）
  public static List<Territory> getAuthorizedTerritories(UUID playerUUID) {
    List<Territory> authorizedTerritories = new ArrayList<>();
    synchronized (territoryByID) {
      for (Territory territory : territoryByID.values()) {
        if (!territory.isOwner(playerUUID) && territory.hasPermission(playerUUID)) {
          authorizedTerritories.add(territory);
        }
      }
    }
    return authorizedTerritories;
  }

  // 根据 ID 获取领地
  public static Territory getTerritoryByID(UUID territoryID) {
    return territoryByID.get(territoryID);
  }

  // 获取所有领地
  public static List<Territory> getAllTerritories() {
    return new ArrayList<>(territoryByID.values());
  }

  // 扩展四叉树边界
  private static void expandQuadTreeBounds(Bounds newBounds) {
    Bounds currentBounds = quadTree.getBounds();
    int newMinX = Math.min(currentBounds.x, newBounds.x);
    int newMinZ = Math.min(currentBounds.z, newBounds.z);
    int newMaxX = Math.max(currentBounds.x + currentBounds.width, newBounds.x + newBounds.width);
    int newMaxZ = Math.max(currentBounds.z + currentBounds.height, newBounds.z + newBounds.height);

    Bounds expandedBounds = new Bounds(newMinX, newMinZ, newMaxX - newMinX, newMaxZ - newMinZ);

    QuadTree newQuadTree = new QuadTree(0, expandedBounds);
    synchronized (territoryByID) {
      for (Territory territory : territoryByID.values()) {
        newQuadTree.insert(territory);
      }
    }
    quadTree.clear();
    quadTree.copyFrom(newQuadTree);
    // ServerMessageUtil.log("QuadTree bounds expanded: " + expandedBounds);
  }

  // 自动保存
  private static void autoSave() {
    long currentTime = System.currentTimeMillis();
    if (currentTime - lastSaveTime >= AUTO_SAVE_INTERVAL) {
      markDirty();
      lastSaveTime = currentTime;
      // ServerMessageUtil.log("Territory data saved.");
    }
  }

  public static void markDirty() {
    if (savedData != null) {
      savedData.setDirty();
    }
  }

  // 清空领地管理器
  public static void reset() {
    savedData = null;
    initialized = false;
    territoryByID.clear();
    territoriesByOwner.clear();
    quadTree.clear();
  }
}
