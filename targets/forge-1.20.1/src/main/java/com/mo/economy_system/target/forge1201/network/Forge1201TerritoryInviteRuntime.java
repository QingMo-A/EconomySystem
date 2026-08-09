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
import net.minecraft.server.level.ServerLevel;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Server-session invitation state shared by the packet handler and commands. */
public final class Forge1201TerritoryInviteRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final TerritoryInviteStoreRegistry<MinecraftServer> STORES =
      new TerritoryInviteStoreRegistry<>();
  private static final Map<MinecraftServer, TerritoryInviteRateLimiter> LIMITERS =
      Collections.synchronizedMap(new WeakHashMap<>());

  private Forge1201TerritoryInviteRuntime() {}

  public static TerritoryInviteStore store(MinecraftServer server) {
    return STORES.get(server);
  }

  /** Eagerly opens the server-scoped territory persistence used by all adapters. */
  public static void initialize(ServerLevel level) {
    Forge1201TerritorySnapshotStore.get(level);
  }

  public static TerritoryInviteRateLimiter limiter(MinecraftServer server) {
    return LIMITERS.computeIfAbsent(server, ignored -> new TerritoryInviteRateLimiter());
  }

  public static TerritoryInviteDecisionService decisions(MinecraftServer server) {
    return new TerritoryInviteDecisionService(
        store(server),
        (territoryId, expectedOwner, playerId, playerName) ->
            Forge1201TerritorySnapshotStore.get(server.overworld())
                .authorize(territoryId, expectedOwner, playerId, playerName),
        (stage, invite, error) -> LOGGER.error("invite decision stage={} invite={} territory={}",
            stage, invite.inviteId(), invite.territoryId(), error));
  }

  public static long tick(MinecraftServer server) {
    return server.getTickCount();
  }

  public static UUID nextInviteId() {
    return UUID.randomUUID();
  }
}
