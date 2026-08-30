package com.mo.economy_system.target.forge1201;

import com.mojang.logging.LogUtils;
import com.mo.economy_system.common.economy.ShopPriceRefreshSchedule;
import com.mo.economy_system.common.market.MarketExpirationSchedule;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.core.territory_system.TerritoryBuffManager;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.target.forge1201.redpacket.Forge1201RedPacketRuntime;
import com.mo.economy_system.target.forge1201.reward.Forge1201RewardRuntime;
import com.mo.economy_system.target.forge1201.starter.Forge1201StarterKitRuntime;
import com.mo.economy_system.target.forge1201.update.Forge1201UpdateRuntime;
import com.mo.economy_system.target.forge1201.commission.Forge1201CommissionRuntime;
import com.mo.economy_system.target.forge1201.recycle.Forge1201RecyclerAdapter;
import com.mo.economy_system.target.forge1201.network.Forge1201MarketExpirationRuntime;
import com.mo.economy_system.target.forge1201.network.Forge1201ClientFileCheckRuntime;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritoryInviteRuntime;
import com.mo.economy_system.target.forge1201.network.Forge1201TerritorySnapshotStore;
import com.mo.economy_system.target.forge1201.tpa.Forge1201TpaCommands;
import com.mo.economy_system.target.forge1201.tpa.Forge1201TpaRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/** Forge lifecycle adapter for common server-side schedules. */
@Mod.EventBusSubscriber(modid = EconomySystemForge1201.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Forge1201ServerEvents {
  private static final Logger LOGGER = LogUtils.getLogger();
  private static final ShopPriceRefreshSchedule SHOP_REFRESH_SCHEDULE = new ShopPriceRefreshSchedule();

  private Forge1201ServerEvents() {}

  @SubscribeEvent
  public static void onServerStarting(ServerStartingEvent event) {
    SHOP_REFRESH_SCHEDULE.reset();
    ServerLevel overworld = event.getServer().overworld();
    EconomySavedData.getInstance(overworld);
    MarketSavedData.getInstance(overworld);
    Forge1201CommissionRuntime.initialize(overworld);
    Forge1201RecyclerAdapter.initialize(event.getServer());
    if (TerritoryBuffManager.initConfig()) {
      var buffCatalog = TerritoryBuffManager.catalog();
      Forge1201TerritorySnapshotStore.configureBuffCatalog(buffCatalog);
      var syncResult = Forge1201TerritorySnapshotStore.get(overworld)
          .synchronizeBuffCatalog(buffCatalog);
      if (syncResult == Forge1201TerritorySnapshotStore.BuffCatalogSyncResult.PERSIST_FAILED
          || syncResult == Forge1201TerritorySnapshotStore.BuffCatalogSyncResult.STATE_UNKNOWN) {
        LOGGER.error("Unable to synchronize the territory buff catalog: {}", syncResult);
      }
    } else {
      Forge1201TerritorySnapshotStore.clearBuffCatalog();
      LOGGER.error("Territory buff catalog is unavailable; persisted buffs were left unchanged");
    }
    Forge1201TerritoryInviteRuntime.initialize(overworld);
    Forge1201RedPacketRuntime.service(event.getServer());
    Forge1201RewardRuntime.start(event.getServer());
    Forge1201StarterKitRuntime.start(event.getServer());
    Forge1201UpdateRuntime.start(event.getServer());
  }

  @SubscribeEvent
  public static void onServerStopping(ServerStoppingEvent event) {
    SHOP_REFRESH_SCHEDULE.reset();
    Forge1201TerritorySnapshotStore.clearBuffCatalog();
    Forge1201RedPacketRuntime.shutdown(event.getServer());
    Forge1201RewardRuntime.shutdown(event.getServer());
    Forge1201StarterKitRuntime.shutdown(event.getServer());
    Forge1201UpdateRuntime.shutdown(event.getServer());
    Forge1201TpaRuntime.shutdown(event.getServer());
    Forge1201ClientFileCheckRuntime.stop(event.getServer());
    Forge1201CommissionRuntime.shutdown(event.getServer());
    Forge1201RecyclerAdapter.shutdown(event.getServer());
  }

  @SubscribeEvent
  public static void onServerTick(TickEvent.ServerTickEvent event) {
    if (event.phase != TickEvent.Phase.END) return;
    if (event.getServer().getTickCount() % 100 == 0) {
      Forge1201RedPacketRuntime.expire(event.getServer());
    }
    if (event.getServer().getTickCount() % 20 == 0) {
      Forge1201TpaCommands.expire(event.getServer());
      Forge1201CommissionRuntime.refreshOnlinePlayers(event.getServer());
    }
    if (MarketExpirationSchedule.shouldRun(event.getServer().getTickCount())) {
      Forge1201MarketExpirationRuntime.expire(event.getServer());
    }
    ServerLevel overworld = event.getServer().overworld();
    if (!SHOP_REFRESH_SCHEDULE.shouldRefresh(overworld.getDayTime())) return;
    if (!EconomyServices.platform().shopCatalog().refreshPrices()) return;
    ShopDataResponseMessage refreshedCatalog = new ShopDataResponseMessage(
        EconomyServices.platform().shopCatalog().snapshot());
    event.getServer().getPlayerList().getPlayers().forEach(player -> {
      try {
        EconomyServices.platform().network().sendToPlayer(player.getUUID(), refreshedCatalog);
      } catch (RuntimeException syncFailure) {
        LOGGER.warn("Shop live catalog sync failed player={}", player.getUUID(), syncFailure);
      }
      player.sendSystemMessage(Component.translatable(ShopPriceRefreshSchedule.REFRESH_MESSAGE_KEY));
    });
  }

  @SubscribeEvent
  public static void onPlayerClone(PlayerEvent.Clone event) {
    Forge1201StarterKitRuntime.copyOnClone(event);
  }

  @SubscribeEvent
  public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
      Forge1201UpdateRuntime.checkForUpdates(player.getServer(), player.getUUID());
      EconomySavedData economy = EconomySavedData.getInstance(player.serverLevel());
      for (String message : economy.getOfflineMessages(player.getUUID())) {
        player.sendSystemMessage(Component.literal(message));
      }
      Forge1201CommissionRuntime.onLogin(player);
    }
  }

  @SubscribeEvent
  public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
    if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
      Forge1201ClientFileCheckRuntime.discardPlayer(player.getServer(), player.getUUID());
    }
  }
}
