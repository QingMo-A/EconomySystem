package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.platform.EconomyServices;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class TerritoryManager {

  private static TerritorySavedData savedData;
  private static boolean initialized = false;
  public static final File CONFIG_FILE =
      EconomyServices.platform()
          .configDirectory()
          .resolve(EconomySystem.MODID)
          .resolve("territory_buffs.json")
          .toFile();

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
  public static void addTerritory(Territory territory) {
    if (!territoryByID.containsKey(territory.getTerritoryID())) {
      territoryByID.put(territory.getTerritoryID(), territory);
      territoriesByOwner
          .computeIfAbsent(territory.getOwnerUUID(), k -> new ArrayList<>())
          .add(territory);

      // 动态扩展四叉树边界
      if (!quadTree.getBounds().contains(territory.getBounds())) {
        expandQuadTreeBounds(territory.getBounds());
      }

      quadTree.insert(territory); // 插入四叉树
      if (savedData != null) {
        savedData.addTerritory(territory);
      }

      updateTerritoryBuffs(territory);

      autoSave(); // 自动保存
      // ServerMessageUtil.log("Territory added: " + territory.getName());
    }
  }

  private static void updateTerritoryBuffs(Territory territory) {
    System.out.println("------init territory Buff-------");

    // 读取配置文件中的 Buff ID
    Set<String> configBuffs = TerritoryBuffManager.getAllBuffIDs();

    // 获取当前领地已有的 Buff ID（从 List 生成 Set）
    Set<String> existingBuffs = new HashSet<>();
    for (TerritoryBuff buff : territory.getTerritoryBuffs()) {
      existingBuffs.add(buff.getId());
    }

    // **1. 添加新 Buff（配置文件有，领地没有）**
    for (String buffId : configBuffs) {
      if (!existingBuffs.contains(buffId)) {
        System.out.println("add Buff: " + buffId);
        TerritoryBuffConfig buffConfig = TerritoryBuffManager.getBuffConfig(buffId);
        if (buffConfig != null) {
          TerritoryBuff newBuff =
              new TerritoryBuff(
                  buffConfig.getId(),
                  buffConfig.getDisplayText(),
                  buffConfig.getEffectId(),
                  buffConfig.isInitialUnlockState(),
                  buffConfig.getInitialLevel(),
                  buffConfig.getSingleUpgradeLevel(),
                  buffConfig.getMaxLevel(),
                  buffConfig.getUpgradeCost());
          territory.addBuffs(newBuff);
        }
      }
    }

    // **2. 删除已移除 Buff（配置文件没有，领地有）**
    Iterator<TerritoryBuff> iterator = territory.getTerritoryBuffs().iterator();
    while (iterator.hasNext()) {
      TerritoryBuff buff = iterator.next();
      if (!configBuffs.contains(buff.getId())) {
        System.out.println("remove Buff: " + buff.getId());
        iterator.remove(); // 移除 Buff
      }
    }

    // **3. 更新已存在 Buff（同步配置信息）**
    for (TerritoryBuff buff : territory.getTerritoryBuffs()) {
      if (configBuffs.contains(buff.getId())) {
        System.out.println("Update Buff: " + buff.getId());
        TerritoryBuffConfig buffConfig = TerritoryBuffManager.getBuffConfig(buff.getId());

        if (buffConfig != null) {
          // **更新 Buff 的属性**
          boolean changed = false;

          if (!buff.getDisplayText().equals(buffConfig.getDisplayText())) {
            buff.setDisplayText(buffConfig.getDisplayText());
            changed = true;
          }

          if (!buff.getEffectId().equals(buffConfig.getEffectId())) {
            buff.setEffectId(buffConfig.getEffectId());
            changed = true;
          }

          if (buff.getSingleUpgradeLevel() != buffConfig.getSingleUpgradeLevel()) {
            buff.setSingleUpgradeLevel(buffConfig.getSingleUpgradeLevel());
            changed = true;
          }

          if (buff.getMaxLevel() != buffConfig.getMaxLevel()) {
            buff.setMaxLevel(buffConfig.getMaxLevel());
            changed = true;
          }

          if (!buff.getUpgradeCost().equals(buffConfig.getUpgradeCost())) {
            buff.setUpgradeCost(buffConfig.getUpgradeCost());
            changed = true;
          }

          // 确保当前等级不超过最大等级
          if (buff.getLevel() > buffConfig.getMaxLevel()) {
            buff.setLevel(buffConfig.getMaxLevel());
            changed = true;
          }

          // **如果有任何变化，则打印日志**
          if (changed) {
            System.out.println("Buff " + buff.getId() + " 更新完成！");
          }
        }
      }
    }

    autoSave(); // 自动保存
  }

  // 解锁 Buff
  public static boolean unlockBuff(UUID territoryID, String buffID) {
    Territory territory = getTerritoryByID(territoryID);
    if (territory == null) {
      return false; // 领地不存在
    }

    TerritoryBuff buff = territory.getBuff(buffID);
    if (buff == null) {
      return false; // Buff 不存在
    }

    if (!buff.isUnlocked()) {
      buff.setUnlocked(true); // 直接解锁
      markDirty();
      return true; // 成功解锁
    }

    return false; // 已解锁，返回 false
  }

  // 升级 Buff
  public static boolean upgradeBuff(UUID territoryID, String buffID) {
    Territory territory = getTerritoryByID(territoryID);
    if (territory == null) {
      return false; // 领地不存在
    }

    TerritoryBuff buff = territory.getBuff(buffID);
    if (buff == null) {
      return false; // Buff 不存在
    }

    TerritoryBuffConfig config = TerritoryBuffManager.getBuffConfig(buffID);
    if (config == null) {
      return false; // 配置不存在
    }

    if (buff.getLevel() < config.getMaxLevel()) {
      buff.setLevel(
          Math.min(config.getMaxLevel(), buff.getLevel() + config.getSingleUpgradeLevel())); // 直接升级
      markDirty();
      return true; // 成功升级
    }

    return false; // 已达最大等级，返回 false
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
        || newPos1.getY() != newPos2.getY()
        || !validCoordinate(newPos1)
        || !validCoordinate(newPos2))
      return ResizePrepareOutcome.of(ResizePrepareResult.INVALID_BOUNDS);
    Territory territory = territoryByID.get(territoryID);
    if (territory == null) return ResizePrepareOutcome.of(ResizePrepareResult.TERRITORY_NOT_FOUND);
    if (!expectedOwner.equals(territory.getOwnerUUID()))
      return ResizePrepareOutcome.of(ResizePrepareResult.OWNER_MISMATCH);
    IllegalStateException invariant =
        territoryIdentityInvariant(territoryID, expectedOwner, territory);
    if (invariant != null)
      return new ResizePrepareOutcome(ResizePrepareResult.STATE_UNKNOWN, null, invariant);
    if (overlapsOther(territory, Bounds.calculateBounds(newPos1, newPos2)))
      return ResizePrepareOutcome.of(ResizePrepareResult.OVERLAP);
    net.minecraft.core.BlockPos oldPos1 = territory.getPos1();
    net.minecraft.core.BlockPos oldPos2 = territory.getPos2();
    net.minecraft.core.BlockPos oldBackpoint = territory.getBackpoint();
    if (oldPos1.equals(newPos1)
        && oldPos2.equals(newPos2)
        && Objects.equals(oldBackpoint, newBackpoint))
      return ResizePrepareOutcome.of(ResizePrepareResult.UNCHANGED);
    try {
      long oldArea = calculateArea(oldPos1, oldPos2);
      long newArea = calculateArea(newPos1, newPos2);
      long difference = Math.subtractExact(newArea, oldArea);
      long rawCharge = difference <= 0 ? 0 : Math.multiplyExact(difference, 20L);
      if (rawCharge > Integer.MAX_VALUE)
        return ResizePrepareOutcome.of(ResizePrepareResult.PRICE_OVERFLOW);
      return new ResizePrepareOutcome(
          ResizePrepareResult.READY,
          new ResizePlan(
              territoryID,
              expectedOwner,
              territory,
              oldPos1,
              oldPos2,
              oldBackpoint,
              newPos1,
              newPos2,
              newBackpoint,
              oldArea,
              newArea,
              difference,
              (int) rawCharge),
          null);
    } catch (ArithmeticException failure) {
      return new ResizePrepareOutcome(ResizePrepareResult.PRICE_OVERFLOW, null, failure);
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
    if (overlapsOther(current, Bounds.calculateBounds(plan.newPos1(), plan.newPos2())))
      return ResizeOutcome.of(ResizeResult.OVERLAP);
    return resizeTerritoryAuthoritatively(
        plan.territoryId(),
        plan.expectedOwnerId(),
        plan.newPos1(),
        plan.newPos2(),
        plan.newBackpoint());
  }

  private static long calculateArea(
      net.minecraft.core.BlockPos first, net.minecraft.core.BlockPos second) {
    long width = Math.abs((long) first.getX() - second.getX()) + 1L;
    long height = Math.abs((long) first.getZ() - second.getZ()) + 1L;
    return Math.multiplyExact(width, height);
  }

  private static boolean overlapsOther(Territory territory, Bounds candidate) {
    return territoryByID.values().stream()
        .filter(
            other -> other != territory && other.getDimension().equals(territory.getDimension()))
        .anyMatch(
            other -> {
              Bounds existing = other.getBounds();
              return candidate.x <= existing.x + existing.width
                  && candidate.x + candidate.width >= existing.x
                  && candidate.z <= existing.z + existing.height
                  && candidate.z + candidate.height >= existing.z;
            });
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
    if (newPos1.getY() != newPos2.getY()
        || !validCoordinate(newPos1)
        || !validCoordinate(newPos2)) {
      return ResizeOutcome.of(ResizeResult.INVALID_BOUNDS);
    }
    IllegalStateException invariant =
        territoryIdentityInvariant(territoryID, expectedOwner, territory);
    if (invariant != null) return new ResizeOutcome(ResizeResult.STATE_UNKNOWN, invariant);
    Bounds candidate = Bounds.calculateBounds(newPos1, newPos2);
    boolean overlaps = overlapsOther(territory, candidate);
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

  private static boolean validCoordinate(net.minecraft.core.BlockPos pos) {
    return Math.abs((long) pos.getX()) <= 30_000_000L && Math.abs((long) pos.getZ()) <= 30_000_000L;
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

  public static boolean transferTerritory(
      UUID territoryID, UUID newOwnerUUID, String newOwnerName) {
    Territory territory = getTerritoryByID(territoryID);
    if (territory == null
        || newOwnerUUID == null
        || newOwnerName == null
        || newOwnerName.isBlank()) {
      return false;
    }
    UUID oldOwnerUUID = territory.getOwnerUUID();
    if (oldOwnerUUID.equals(newOwnerUUID)) {
      return false;
    }

    territoriesByOwner.getOrDefault(oldOwnerUUID, new ArrayList<>()).remove(territory);
    territory.removeAuthorizedPlayer(newOwnerUUID);
    territory.addAuthorizedPlayer(oldOwnerUUID, territory.getOwnerName());
    territory.setOwner(newOwnerUUID, newOwnerName);
    territoriesByOwner.computeIfAbsent(newOwnerUUID, key -> new ArrayList<>()).add(territory);
    markDirty();
    return true;
  }

  public static boolean setTerritoryPermission(
      UUID territoryID, UUID playerUUID, String playerName, boolean allowed) {
    Territory territory = getTerritoryByID(territoryID);
    if (territory == null
        || playerUUID == null
        || playerName == null
        || playerName.isBlank()
        || territory.isOwner(playerUUID)) {
      return false;
    }
    if (allowed) {
      territory.addAuthorizedPlayer(playerUUID, playerName);
    } else {
      territory.removeAuthorizedPlayer(playerUUID);
    }
    markDirty();
    return true;
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

  public static boolean setTerritoryRule(
      UUID territoryID, TerritoryPermissionAction action, TerritoryPermissionLevel level) {
    Territory territory = getTerritoryByID(territoryID);
    if (territory == null || action == null || level == null) {
      return false;
    }
    territory.setPermissionLevel(action, level);
    markDirty();
    return true;
  }

  // 查询指定位置的领地（X 和 Z 轴）
  public static Territory getTerritoryAtIgnoreY(int x, int z) {
    List<Territory> candidates = quadTree.query(x, z);
    return candidates.stream()
        .filter(territory -> territory.isWithinBoundsIgnoreY(x, z))
        .findFirst()
        .orElse(null);
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

  // 检查玩家是否有权限
  public static boolean isPlayerAuthorized(UUID territoryID, UUID playerUUID) {
    Territory territory = getTerritoryByID(territoryID);
    return territory != null && territory.hasPermission(playerUUID);
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
