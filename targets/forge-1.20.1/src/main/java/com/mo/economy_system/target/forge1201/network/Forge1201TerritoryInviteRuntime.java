package com.mo.economy_system.target.forge1201.network;

import com.mo.economy_system.common.territory.TerritoryInviteDecisionService;
import com.mo.economy_system.common.territory.TerritoryInviteRateLimiter;
import com.mo.economy_system.common.territory.TerritoryInviteStore;
import com.mo.economy_system.common.territory.TerritoryInviteStoreRegistry;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;

/** Server-session invitation state shared by the packet handler and commands. */
public final class Forge1201TerritoryInviteRuntime {
  private static final TerritoryInviteStoreRegistry<MinecraftServer> STORES =
      new TerritoryInviteStoreRegistry<>();
  private static final Map<MinecraftServer, TerritoryInviteRateLimiter> LIMITERS =
      Collections.synchronizedMap(new WeakHashMap<>());

  private Forge1201TerritoryInviteRuntime() {}

  public static TerritoryInviteStore store(MinecraftServer server) {
    return STORES.get(server);
  }

  public static TerritoryInviteRateLimiter limiter(MinecraftServer server) {
    return LIMITERS.computeIfAbsent(server, ignored -> new TerritoryInviteRateLimiter());
  }

  public static TerritoryInviteDecisionService decisions(MinecraftServer server) {
    return new TerritoryInviteDecisionService(
        store(server),
        (territoryId, expectedOwner, playerId, playerName) ->
            Forge1201TerritorySnapshotStore.get(server.overworld())
                .authorize(territoryId, expectedOwner, playerId, playerName));
  }

  public static long tick(MinecraftServer server) {
    return server.getTickCount();
  }

  public static UUID nextInviteId() {
    return UUID.randomUUID();
  }
}
