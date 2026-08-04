package com.mo.economy_system.item.items;

import com.mo.economy_system.core.territory_system.Territory;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class Item_ClaimWand extends Item {
  public record ResizeCleanupResult(
      int clearedSessions, int notifiedPlayers, int notificationFailures) {}

  private static final Map<UUID, BlockPos> firstPositions = new HashMap<>();
  private static final Map<UUID, BlockPos> secondPositions = new HashMap<>();
  private static final Map<UUID, BlockPos> firstModifyPositions = new HashMap<>();
  private static final Map<UUID, BlockPos> secondModifyPositions = new HashMap<>();
  private static final Map<UUID, Long> modifyVolume = new HashMap<>();
  private static final Map<UUID, ScheduledExecutorService> timeoutTasks =
      new HashMap<>(); // 记录每个玩家的超时任务

  private static final Map<UUID, UUID> playerModify = new HashMap<>(); // 玩家UUID -> 领地ID

  public Item_ClaimWand(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    if (!(context.getPlayer() instanceof ServerPlayer player)) {
      return InteractionResult.FAIL; // 仅支持服务端
    }

    UUID playerUUID = player.getUUID();
    BlockPos clickedPos = context.getClickedPos();

    if (!playerModify.containsKey(playerUUID)) {
      if (!firstPositions.containsKey(playerUUID)) {
        // 玩家未选定第一个点
        firstPositions.put(playerUUID, clickedPos);
        player.sendSystemMessage(
            Component.translatable(
                Util_MessageKeys.CLAIM_WAND_FIRST_POSITION_SET,
                clickedPos.getX(),
                clickedPos.getY(),
                clickedPos.getZ()));
        startTimeoutTask(player); // 开始倒计时任务
      } else if (!secondPositions.containsKey(playerUUID)) {
        // 玩家未选定第二个点
        secondPositions.put(playerUUID, clickedPos);

        // 检查新圈地是否包含已有领地
        BlockPos firstPos = firstPositions.get(playerUUID);
        if (isOverlappingExistingTerritory(player, firstPos, clickedPos)) {
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_OVERLAP_ERROR));
          firstPositions.remove(playerUUID);
          secondPositions.remove(playerUUID);
          return InteractionResult.FAIL;
        }

        BlockPos secondPos = secondPositions.get(playerUUID);
        // 检查两点是否水平
        if (!(firstPos.getY() == secondPos.getY())) {
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_Y_MISMATCH_ERROR));
          firstPositions.remove(playerUUID);
          secondPositions.remove(playerUUID);
          return InteractionResult.FAIL;
        }

        player.sendSystemMessage(
            Component.translatable(
                Util_MessageKeys.CLAIM_WAND_SECOND_POSITION_SET,
                clickedPos.getX(),
                clickedPos.getY(),
                clickedPos.getZ()));

        // 计算范围和价格
        long volume = calculateVolume(firstPos, clickedPos);
        long price = volume * 20L;

        player.sendSystemMessage(
            Component.translatable(Util_MessageKeys.CLAIM_WAND_VOLUME, volume));
        player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_PRICE, price));
        player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_INSTRUCTION));

        // 显示粒子效果（仅显示边缘）
        showParticleEffect((ServerLevel) player.level(), firstPos, clickedPos, player);

      } else {
        // 第三次右键，取消圈地
        firstPositions.remove(playerUUID);
        secondPositions.remove(playerUUID);

        // 停止粒子效果
        stopParticleEffect(playerUUID);
        stopTimeoutTask(playerUUID);

        player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_CANCEL));
      }
    } else if (playerModify.containsKey(playerUUID)) {

      if (!firstPositions.containsKey(playerUUID)) {
        // 玩家未选定第一个点
        firstPositions.put(playerUUID, clickedPos);
        player.sendSystemMessage(
            Component.translatable(
                Util_MessageKeys.CLAIM_WAND_FIRST_POSITION_SET,
                clickedPos.getX(),
                clickedPos.getY(),
                clickedPos.getZ()));
        startTimeoutTask(player); // 开始倒计时任务
      } else if (!secondPositions.containsKey(playerUUID)) {
        // 玩家未选定第二个点
        secondPositions.put(playerUUID, clickedPos);

        // 检查新圈地是否包含已有领地
        BlockPos firstPos = firstPositions.get(playerUUID);
        if (isOverlappingExistingTerritory(player, firstPos, clickedPos)) {
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_OVERLAP_ERROR));
          firstPositions.remove(playerUUID);
          secondPositions.remove(playerUUID);
          return InteractionResult.FAIL;
        }

        BlockPos secondPos = secondPositions.get(playerUUID);
        // 检查两点是否水平
        if (!(firstPos.getY() == secondPos.getY())) {
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_Y_MISMATCH_ERROR));
          firstPositions.remove(playerUUID);
          secondPositions.remove(playerUUID);
          return InteractionResult.FAIL;
        }

        player.sendSystemMessage(
            Component.translatable(
                Util_MessageKeys.CLAIM_WAND_SECOND_POSITION_SET,
                clickedPos.getX(),
                clickedPos.getY(),
                clickedPos.getZ()));

        Territory territory = TerritoryManager.getTerritoryByID(playerModify.get(playerUUID));
        if (territory == null
            || !territory.isOwner(playerUUID)
            || !territory.getDimension().equals(player.serverLevel().dimension())) {
          player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_RESIZE_FAILED));
          clearPositions(playerUUID);
          return InteractionResult.FAIL;
        }

        // 计算范围和价格
        long volume = calculateVolume(firstPos, clickedPos);
        long oldVolume = calculateVolume(territory.getPos1(), territory.getPos2());
        // 计算新旧面积差值
        long areaDiff = volume - oldVolume;

        firstModifyPositions.put(playerUUID, firstPos);
        secondModifyPositions.put(playerUUID, secondPos);
        modifyVolume.put(playerUUID, areaDiff);

        if (areaDiff > 0) {
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_CONFIRM_EXPAND));
          long newPrice = areaDiff * 20L;
          player.sendSystemMessage(
              Component.translatable(
                  Util_MessageKeys.CLAIM_WAND_RESIZE_COST_DETAILS, oldVolume, volume, newPrice));
        } else if (areaDiff < 0) {
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_CONFIRM_SHRINK));
          player.sendSystemMessage(
              Component.translatable(Util_MessageKeys.CLAIM_WAND_VOLUME_CHANGE, oldVolume, volume));
        }

        // 显示粒子效果（仅显示边缘）
        showParticleEffect((ServerLevel) player.level(), firstPos, clickedPos, player);

      } else {
        // 第三次右键，取消圈地
        firstPositions.remove(playerUUID);
        secondPositions.remove(playerUUID);
        firstModifyPositions.remove(playerUUID);
        secondModifyPositions.remove(playerUUID);
        modifyVolume.remove(playerUUID);

        // 停止粒子效果
        stopParticleEffect(playerUUID);
        stopTimeoutTask(playerUUID);
        cancelResizing(player);

        player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_CANCEL));
      }
    }

    return InteractionResult.SUCCESS;
  }

  public static void startResizing(ServerPlayer player, UUID territoryUUID) {
    playerModify.put(player.getUUID(), territoryUUID);
    stopTimeoutTask(player.getUUID());
    player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_ENTER_RESIZE_MODE));
  }

  public static void cancelResizing(ServerPlayer player) {
    playerModify.remove(player.getUUID());
    player.sendSystemMessage(Component.translatable(Util_MessageKeys.CLAIM_WAND_EXIT_RESIZE_MODE));
  }

  public static ResizeCleanupResult cancelResizingForTerritory(
      MinecraftServer server, UUID territoryId, String territoryName) {
    java.util.Objects.requireNonNull(server, "server");
    java.util.Objects.requireNonNull(territoryId, "territoryId");
    territoryName = java.util.Objects.requireNonNull(territoryName, "territoryName").trim();
    if (territoryName.isEmpty() || territoryName.length() > 128)
      throw new IllegalArgumentException("territoryName");
    if (!server.isSameThread())
      throw new IllegalStateException("resize cleanup must run on server thread");
    java.util.List<UUID> players =
        playerModify.entrySet().stream()
            .filter(entry -> territoryId.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .toList();
    int notified = 0;
    int failures = 0;
    for (UUID playerId : players) {
      clearPositions(playerId);
      ServerPlayer player = server.getPlayerList().getPlayer(playerId);
      if (player != null) {
        try {
          player.sendSystemMessage(
              Component.translatable("message.territory.remove.resize_cancelled", territoryName));
          notified++;
        } catch (RuntimeException failure) {
          failures++;
        }
      }
    }
    return new ResizeCleanupResult(players.size(), notified, failures);
  }

  public static boolean isResizing(ServerPlayer player) {
    return playerModify.containsKey(player.getUUID());
  }

  public static boolean isResizing(UUID playerUUID) {
    return playerModify.containsKey(playerUUID);
  }

  public static UUID getResizingTerritoryID(ServerPlayer player) {
    return playerModify.get(player.getUUID());
  }

  private long calculateVolume(BlockPos pos1, BlockPos pos2) {
    long xSize = Math.abs((long) pos2.getX() - pos1.getX()) + 1L;
    long zSize = Math.abs((long) pos2.getZ() - pos1.getZ()) + 1L;
    return xSize * zSize; // 计算体积
  }

  private void showParticleEffect(
      ServerLevel level, BlockPos pos1, BlockPos pos2, ServerPlayer player) {
    spawnNearestBoundaryParticles(level, pos1, pos2, player.blockPosition(), 16);
  }

  private static void stopParticleEffect(UUID playerUUID) {
    // 粒子现在只在服务端主线程按需生成最近边界片段，不再保留后台粒子任务。
  }

  private static void startTimeoutTask(ServerPlayer player) {
    UUID playerUUID = player.getUUID();
    ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    executorService.schedule(
        () ->
            player
                .getServer()
                .execute(
                    () -> {
                      ScheduledExecutorService completed = timeoutTasks.remove(playerUUID);
                      if (completed != executorService) return;
                      completed.shutdown();
                      // 如果玩家的圈地状态仍然有效，则自动取消
                      if (firstPositions.containsKey(playerUUID)
                          || secondPositions.containsKey(playerUUID)) {
                        firstPositions.remove(playerUUID);
                        secondPositions.remove(playerUUID);
                        stopParticleEffect(playerUUID);
                        firstModifyPositions.remove(playerUUID);
                        secondModifyPositions.remove(playerUUID);
                        modifyVolume.remove(playerUUID);
                        playerModify.remove(playerUUID);
                        player.sendSystemMessage(
                            Component.translatable(Util_MessageKeys.CLAIM_WAND_TIMEOUT));
                      }
                    }),
        60,
        TimeUnit.SECONDS); // 60秒后执行

    timeoutTasks.put(playerUUID, executorService);
  }

  private static void stopTimeoutTask(UUID playerUUID) {
    ScheduledExecutorService executorService = timeoutTasks.remove(playerUUID);
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  public static BlockPos getFirstPosition(UUID playerUUID) {
    return firstPositions.get(playerUUID);
  }

  public static BlockPos getSecondPosition(UUID playerUUID) {
    return secondPositions.get(playerUUID);
  }

  public static BlockPos getFirstModifyPosition(UUID playerUUID) {
    return firstModifyPositions.get(playerUUID);
  }

  public static BlockPos getSecondModifyPosition(UUID playerUUID) {
    return secondModifyPositions.get(playerUUID);
  }

  public static long getModifyVolume(UUID playerUUID) {
    return modifyVolume.getOrDefault(playerUUID, 0L);
  }

  public static void clearPositions(UUID playerUUID) {
    firstPositions.remove(playerUUID);
    secondPositions.remove(playerUUID);
    firstModifyPositions.remove(playerUUID);
    secondModifyPositions.remove(playerUUID);
    modifyVolume.remove(playerUUID);
    playerModify.remove(playerUUID);

    // 停止超时任务
    stopTimeoutTask(playerUUID);
  }

  private static void spawnNearestBoundaryParticles(
      ServerLevel level, BlockPos pos1, BlockPos pos2, BlockPos playerPos, int radius) {
    int minX = Math.min(pos1.getX(), pos2.getX());
    int maxX = Math.max(pos1.getX(), pos2.getX());
    int minY = Math.min(pos1.getY(), pos2.getY());
    int maxY = Math.max(pos1.getY(), pos2.getY());
    int minZ = Math.min(pos1.getZ(), pos2.getZ());
    int maxZ = Math.max(pos1.getZ(), pos2.getZ());

    int distanceToWest = Math.abs(playerPos.getX() - minX);
    int distanceToEast = Math.abs(playerPos.getX() - maxX);
    int distanceToNorth = Math.abs(playerPos.getZ() - minZ);
    int distanceToSouth = Math.abs(playerPos.getZ() - maxZ);
    int nearest =
        Math.min(
            Math.min(distanceToWest, distanceToEast), Math.min(distanceToNorth, distanceToSouth));

    if (nearest == distanceToWest || nearest == distanceToEast) {
      int x = nearest == distanceToWest ? minX : maxX;
      int fromZ = Math.max(minZ, playerPos.getZ() - radius);
      int toZ = Math.min(maxZ, playerPos.getZ() + radius);
      for (int z = fromZ; z <= toZ; z++) {
        spawnVerticalParticleColumn(level, x, z, minY, maxY);
      }
    } else {
      int z = nearest == distanceToNorth ? minZ : maxZ;
      int fromX = Math.max(minX, playerPos.getX() - radius);
      int toX = Math.min(maxX, playerPos.getX() + radius);
      for (int x = fromX; x <= toX; x++) {
        spawnVerticalParticleColumn(level, x, z, minY, maxY);
      }
    }
  }

  private static void spawnVerticalParticleColumn(
      ServerLevel level, int x, int z, int minY, int maxY) {
    level.sendParticles(ParticleTypes.END_ROD, x + 0.5, minY + 1.5, z + 0.5, 1, 0, 0, 0, 0);
    if (maxY != minY) {
      level.sendParticles(ParticleTypes.END_ROD, x + 0.5, maxY + 1.5, z + 0.5, 1, 0, 0, 0, 0);
    }
  }

  private boolean isOverlappingExistingTerritory(
      ServerPlayer player, BlockPos pos1, BlockPos pos2) {
    // 计算新领地的范围
    int minX = Math.min(pos1.getX(), pos2.getX());
    int maxX = Math.max(pos1.getX(), pos2.getX());
    int minZ = Math.min(pos1.getZ(), pos2.getZ());
    int maxZ = Math.max(pos1.getZ(), pos2.getZ());

    boolean flag = false;

    // 遍历所有现有领地
    for (Territory territory : TerritoryManager.getAllTerritories()) {

      // 获取现有领地的 X-Z 范围
      int existingMinX = Math.min(territory.getPos1().getX(), territory.getPos2().getX());
      int existingMaxX = Math.max(territory.getPos1().getX(), territory.getPos2().getX());
      int existingMinZ = Math.min(territory.getPos1().getZ(), territory.getPos2().getZ());
      int existingMaxZ = Math.max(territory.getPos1().getZ(), territory.getPos2().getZ());

      // 检测范围是否重叠
      if (maxX >= existingMinX
          && minX <= existingMaxX
          && maxZ >= existingMinZ
          && minZ <= existingMaxZ
          && player.serverLevel().dimension().equals(territory.getDimension())) {
        // t.put(player.getUUID(), territory);
        // 如果玩家处于修改模式
        if (playerModify.containsKey(player.getUUID())) {
          if (!(playerModify.get(player.getUUID()).equals(territory.getTerritoryID()))) {
            flag = true;
            break;
          }
        } else {
          return true;
        }
      }
    }

    return flag; // 没有重叠
  }

  private boolean isContainingTerritoryOwner(ServerPlayer player, BlockPos pos1, BlockPos pos2) {
    // 计算新领地的范围
    int minX = Math.min(pos1.getX(), pos2.getX());
    int maxX = Math.max(pos1.getX(), pos2.getX());
    int minZ = Math.min(pos1.getZ(), pos2.getZ());
    int maxZ = Math.max(pos1.getZ(), pos2.getZ());

    // 遍历所有现有领地
    for (Territory territory : TerritoryManager.getAllTerritories()) {

      // 获取现有领地的 X-Z 范围
      int existingMinX = Math.min(territory.getPos1().getX(), territory.getPos2().getX());
      int existingMaxX = Math.max(territory.getPos1().getX(), territory.getPos2().getX());
      int existingMinZ = Math.min(territory.getPos1().getZ(), territory.getPos2().getZ());
      int existingMaxZ = Math.max(territory.getPos1().getZ(), territory.getPos2().getZ());

      // 检测范围是否重叠
      if (maxX >= existingMinX
          && minX <= existingMaxX
          && maxZ >= existingMinZ
          && minZ <= existingMaxZ
          && player.serverLevel().dimension().equals(territory.getDimension())) {
        if (territory.isOwner(player.getUUID())) {
          return true;
        } else {
          return false;
        }
      }
    }

    return false; // 没有重叠
  }
}
