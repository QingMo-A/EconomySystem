package com.mo.economy_system.item.items;

import com.mo.economy_system.common.network.EconomyNetworkLimits;
import com.mo.economy_system.common.territory.TerritorySelectionService;
import com.mo.economy_system.target.neoforge1211.territory.NeoForge1211TerritorySelectionRuntime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** Minecraft item shell for the common territory-selection state machine. */
public final class Item_ClaimWand extends Item {
  public record ResizeCleanupResult(
      int clearedSessions, int notifiedPlayers, int notificationFailures) {}

  public Item_ClaimWand(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResult useOn(UseOnContext context) {
    if (!(context.getPlayer() instanceof ServerPlayer player)) {
      return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
    return NeoForge1211TerritorySelectionRuntime.select(player, context.getClickedPos())
        ? InteractionResult.SUCCESS
        : InteractionResult.FAIL;
  }

  public static void startResizing(ServerPlayer player, UUID territoryId) {
    NeoForge1211TerritorySelectionRuntime.startResize(player, territoryId);
  }

  public static void cancelResizing(ServerPlayer player) {
    NeoForge1211TerritorySelectionRuntime.cancelResize(player);
  }

  public static ResizeCleanupResult cancelResizingForTerritory(
      MinecraftServer server, UUID territoryId, String territoryName) {
    if (server == null || !server.isSameThread()) {
      throw new IllegalStateException("resize cleanup must run on server thread");
    }
    if (territoryId == null) throw new IllegalArgumentException("territoryId");
    String normalized = territoryName == null ? "" : territoryName.trim();
    if (normalized.isEmpty() || normalized.length() > EconomyNetworkLimits.MAX_TERRITORY_NAME_LENGTH) {
      throw new IllegalArgumentException("territoryName");
    }
    List<UUID> players = NeoForge1211TerritorySelectionRuntime.clearForTerritoryAndList(server, territoryId);
    int notified = 0;
    int failures = 0;
    for (UUID playerId : players) {
      ServerPlayer player = server.getPlayerList().getPlayer(playerId);
      if (player == null) continue;
      try {
        player.sendSystemMessage(
            Component.translatable("message.territory.remove.resize_cancelled", normalized));
        notified++;
      } catch (RuntimeException failure) {
        failures++;
      }
    }
    return new ResizeCleanupResult(players.size(), notified, failures);
  }

  public static void tick(MinecraftServer server) {
    NeoForge1211TerritorySelectionRuntime.expire(server);
  }

  public static void clearAll() {
    NeoForge1211TerritorySelectionRuntime.clearAll();
  }

  public static boolean isResizing(ServerPlayer player) {
    return NeoForge1211TerritorySelectionRuntime.hasResize(player);
  }

  public static boolean isResizing(UUID playerId) {
    return NeoForge1211TerritorySelectionRuntime.find(playerId, TerritorySelectionService.Mode.RESIZE)
        .isPresent();
  }

  public static UUID getResizingTerritoryID(ServerPlayer player) {
    return NeoForge1211TerritorySelectionRuntime.resizeSession(player)
        .map(TerritorySelectionService.Session::territoryId)
        .orElse(null);
  }

  public static BlockPos getFirstPosition(UUID playerId) {
    return point(NeoForge1211TerritorySelectionRuntime.find(
        playerId, TerritorySelectionService.Mode.CLAIM)
        .flatMap(session -> session.first()));
  }

  public static BlockPos getSecondPosition(UUID playerId) {
    return point(NeoForge1211TerritorySelectionRuntime.find(
        playerId, TerritorySelectionService.Mode.CLAIM)
        .flatMap(session -> session.second()));
  }

  public static BlockPos getFirstModifyPosition(UUID playerId) {
    return point(NeoForge1211TerritorySelectionRuntime.find(
        playerId, TerritorySelectionService.Mode.RESIZE)
        .flatMap(session -> session.first()));
  }

  public static BlockPos getSecondModifyPosition(UUID playerId) {
    return point(NeoForge1211TerritorySelectionRuntime.find(
        playerId, TerritorySelectionService.Mode.RESIZE)
        .flatMap(session -> session.second()));
  }

  public static void clearPositions(UUID playerId) {
    NeoForge1211TerritorySelectionRuntime.clear(playerId);
  }

  private static BlockPos point(Optional<TerritorySelectionService.Point> point) {
    return point.map(value -> new BlockPos(value.x(), value.y(), value.z())).orElse(null);
  }
}
