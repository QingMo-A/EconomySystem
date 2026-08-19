package com.mo.economy_system.api;

import com.mo.economy_system.target.neoforge1211.api.NeoForge1211EconomyApiSession;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Stable public entry point for third-party EconomySystem integrations.
 *
 * <p>All target JARs expose this same fully-qualified class and API semantics. Calls are server-side
 * only and must run on the Minecraft server thread.</p>
 */
public final class EconomySystemApi {
  public static final int API_MAJOR = 1;
  public static final int API_MINOR = 0;
  public static final int API_PATCH = 0;
  public static final String API_VERSION = "1.0.0";
  public static final String CURRENCY_NAME = "梦鱼币";

  private EconomySystemApi() {}

  public static boolean isCompatibleMajor(int requiredMajor) {
    return requiredMajor == API_MAJOR;
  }

  public static EconomyApiSession forLevel(ServerLevel level) {
    return new NeoForge1211EconomyApiSession(Objects.requireNonNull(level, "level"));
  }

  public static EconomyApiSession forPlayer(ServerPlayer player) {
    Objects.requireNonNull(player, "player");
    return forLevel(player.serverLevel());
  }

  public static EconomyApiSession forServer(MinecraftServer server) {
    Objects.requireNonNull(server, "server");
    ServerLevel overworld = server.getLevel(Level.OVERWORLD);
    if (overworld == null) throw new IllegalStateException("Overworld is not loaded");
    return forLevel(overworld);
  }
}
