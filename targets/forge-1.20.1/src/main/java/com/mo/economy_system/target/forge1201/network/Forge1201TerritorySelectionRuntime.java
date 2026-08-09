package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.territory.TerritorySelectionService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;

/** Forge-owned server registry for the common claim-wand state machine. */
final class Forge1201TerritorySelectionRuntime {
  private static final Map<MinecraftServer, TerritorySelectionService> BY_SERVER =
      new WeakHashMap<>();

  private Forge1201TerritorySelectionRuntime() {}

  static TerritorySelectionService state(MinecraftServer server) {
    synchronized (BY_SERVER) {
      return BY_SERVER.computeIfAbsent(server, ignored -> new TerritorySelectionService());
    }
  }

  static Optional<TerritorySelectionService.Session> session(
      MinecraftServer server, UUID playerId, TerritorySelectionService.Mode mode) {
    return state(server).session(playerId, mode, tick(server));
  }

  static List<TerritorySelectionService.Session> expire(MinecraftServer server) {
    return state(server).expire(tick(server));
  }

  static void clear(MinecraftServer server, UUID playerId) {
    state(server).clear(playerId);
  }

  static void stop(MinecraftServer server) {
    synchronized (BY_SERVER) {
      TerritorySelectionService removed = BY_SERVER.remove(server);
      if (removed != null) removed.clearAll();
    }
  }

  static long tick(MinecraftServer server) {
    return server.overworld().getGameTime();
  }
}
