package com.mo.economy_system.target.neoforge1211.starter;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.starter.StarterKitAccountPort;
import com.mo.economy_system.common.starter.StarterKitPort;
import com.mo.economy_system.common.starter.StarterKitService;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

/** NeoForge persistence and account adapter for the common starter-kit service. */
public final class NeoForge1211StarterKitRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final String TAG_KEY = "ReceivedStarterKit";
  private static final Map<MinecraftServer, StarterKitService> SERVICES = new IdentityHashMap<>();

  private NeoForge1211StarterKitRuntime() {}

  public static synchronized StarterKitService service(MinecraftServer server) {
    Objects.requireNonNull(server, "server");
    return SERVICES.computeIfAbsent(server, NeoForge1211StarterKitRuntime::createService);
  }

  public static void start(MinecraftServer server) {
    service(server);
  }

  public static synchronized void shutdown(MinecraftServer server) {
    SERVICES.remove(server);
  }

  /** Preserve the one-time marker when Minecraft creates a replacement player entity. */
  public static void copyOnClone(PlayerEvent.Clone event) {
    if (!(event.getOriginal() instanceof ServerPlayer original)
        || !(event.getEntity() instanceof ServerPlayer replacement)) return;
    CompoundTag source = original.getPersistentData();
    if (source.getBoolean(TAG_KEY)) {
      replacement.getPersistentData().putBoolean(TAG_KEY, true);
    }
  }

  private static StarterKitService createService(MinecraftServer server) {
    return new StarterKitService(
        new Marker(server),
        new AccountAdapter(server),
        (operation, playerId, primary, secondary) ->
            LOGGER.warn("starter-kit operation={} player={}", operation, playerId, primary));
  }

  private static final class Marker implements StarterKitPort {
    private final MinecraftServer server;

    private Marker(MinecraftServer server) {
      this.server = server;
    }

    @Override
    public boolean claimed(UUID playerId) {
      return player(playerId).getPersistentData().getBoolean(TAG_KEY);
    }

    @Override
    public void markClaimed(UUID playerId) {
      player(playerId).getPersistentData().putBoolean(TAG_KEY, true);
    }

    @Override
    public void unmarkClaimed(UUID playerId) {
      player(playerId).getPersistentData().remove(TAG_KEY);
    }

    private ServerPlayer player(UUID id) {
      ServerPlayer player = server.getPlayerList().getPlayer(id);
      if (player == null) throw new IllegalStateException("player is offline: " + id);
      return player;
    }
  }

  private static final class AccountAdapter implements StarterKitAccountPort {
    private final MinecraftServer server;

    private AccountAdapter(MinecraftServer server) {
      this.server = server;
    }

    @Override
    public com.mo.economy_system.core.economy_system.BalanceMutationResult credit(
        UUID playerId, int amount, String category, String reason) {
      return EconomySavedData.getInstance(requirePlayer(playerId).serverLevel())
          .creditExact(playerId, amount, category, reason);
    }

    @Override
    public com.mo.economy_system.core.economy_system.BalanceMutationResult debit(
        UUID playerId, int amount, String category, String reason) {
      return EconomySavedData.getInstance(requirePlayer(playerId).serverLevel())
          .debitExact(playerId, amount, category, reason);
    }

    private ServerPlayer requirePlayer(UUID id) {
      ServerPlayer player = server.getPlayerList().getPlayer(id);
      if (player == null) throw new IllegalStateException("player is offline: " + id);
      return player;
    }
  }
}
