package com.mo.economy_system.target.forge1201.reward;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.EconomyConstants;
import com.mo.economy_system.common.reward.RewardAccountPort;
import com.mo.economy_system.common.reward.RewardCalculator;
import com.mo.economy_system.common.reward.RewardConfiguration;
import com.mo.economy_system.common.reward.RewardRandom;
import com.mo.economy_system.common.reward.RewardService;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.platform.EconomyServices;
import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

/** Forge API adapter for the common mob-reward service. */
public final class Forge1201RewardRuntime {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final Map<MinecraftServer, RewardService> SERVICES = new IdentityHashMap<>();
  private static RewardConfiguration configuration;

  private Forge1201RewardRuntime() {}

  public static synchronized RewardService service(MinecraftServer server) {
    Objects.requireNonNull(server, "server");
    ensureConfiguration();
    return SERVICES.computeIfAbsent(server, Forge1201RewardRuntime::createService);
  }

  public static void start(MinecraftServer server) {
    service(server);
  }

  public static RewardService.Outcome award(
      MinecraftServer server,
      UUID playerId,
      String entityType,
      String entityDisplayName,
      int bountyHunterLevel,
      int carefullyLevel) {
    return service(server)
        .award(playerId, entityType, entityDisplayName, bountyHunterLevel, carefullyLevel);
  }

  public static synchronized void shutdown(MinecraftServer server) {
    SERVICES.remove(server);
    if (SERVICES.isEmpty() && configuration != null) {
      configuration.close();
      configuration = null;
    }
  }

  private static void ensureConfiguration() {
    if (configuration != null) return;
    Path file =
        EconomyServices.platform()
            .configDirectory()
            .resolve(EconomyConstants.MOD_ID)
            .resolve("economy_rewards.json");
    configuration =
        new RewardConfiguration(
            file,
            (operation, detail, error) -> {
              if (error == null) {
                LOGGER.warn("reward config operation={} detail={}", operation, detail);
              } else {
                LOGGER.warn("reward config operation={} detail={}", operation, detail, error);
              }
            });
    configuration.start();
  }

  private static RewardService createService(MinecraftServer server) {
    EconomySavedData economy = EconomySavedData.getInstance(server.overworld());
    RewardAccountPort accounts = economy::creditExact;
    Random random = new Random();
    RewardRandom randomAdapter =
        new RewardRandom() {
          @Override
          public double nextDouble() {
            return random.nextDouble();
          }

          @Override
          public int nextInt(int bound) {
            return random.nextInt(bound);
          }
        };
    return new RewardService(
        configuration.catalog(),
        new RewardCalculator(randomAdapter),
        accounts,
        (operation, playerId, entityType, error) ->
            LOGGER.error(
                "mob reward operation={} player={} entity={}",
                operation,
                playerId,
                entityType,
                error));
  }
}
