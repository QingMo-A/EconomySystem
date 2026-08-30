package com.mo.economy_system.events;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.commands.check_system.Command_Check;
import com.mo.economy_system.commands.check_system.Command_Info;
import com.mo.economy_system.commands.economy_system.Command_Economy;
import com.mo.economy_system.commands.economy_system.Command_RedPacket;
import com.mo.economy_system.commands.economy_system.Command_StarterKit;
import com.mo.economy_system.commands.territory_system.Command_Territory;
import com.mo.economy_system.commands.territory_system.Command_TerritoryClaim;
import com.mo.economy_system.commands.tpa_system.Command_Tpa;
import com.mo.economy_system.common.economy.ShopPriceRefreshSchedule;
import com.mo.economy_system.common.market.MarketExpirationSchedule;
import com.mo.economy_system.common.network.ShopDataResponseMessage;
import com.mo.economy_system.common.reward.RewardFeedback;
import com.mo.economy_system.common.reward.RewardService;
import com.mo.economy_system.platform.EconomyServices;
import com.mo.economy_system.enchant.EconomySystem_Enchants;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.events.territory_system.EventHandler_Player;
import com.mo.economy_system.enchant.enchants.BountyHunterEnchantment;
import com.mo.economy_system.enchant.enchants.CarefullyEnchantment;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211ClientFileCheckRuntime;
import com.mo.economy_system.target.neoforge1211.redpacket.NeoForge1211RedPacketRuntime;
import com.mo.economy_system.target.neoforge1211.reward.NeoForge1211RewardRuntime;
import com.mo.economy_system.target.neoforge1211.starter.NeoForge1211StarterKitRuntime;
import com.mo.economy_system.target.neoforge1211.tpa.NeoForge1211TpaRuntime;
import com.mo.economy_system.target.neoforge1211.update.NeoForge1211UpdateRuntime;
import com.mo.economy_system.target.neoforge1211.market.NeoForge1211MarketExpirationRuntime;
import com.mo.economy_system.target.neoforge1211.territory.NeoForge1211TerritorySelectionRuntime;
import com.mo.economy_system.target.neoforge1211.commission.NeoForge1211CommissionRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.UUID;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class EconomySystem_EventHandler {

    private static final ShopPriceRefreshSchedule SHOP_REFRESH_SCHEDULE = new ShopPriceRefreshSchedule();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        SHOP_REFRESH_SCHEDULE.reset();
        // 获取服务器主世界
        ServerLevel overworld = event.getServer().overworld();

        // 注册指令
        Command_Economy.register(event.getServer().getCommands().getDispatcher());
        com.mo.economy_system.commands.economy_system.Command_Mailbox.register(event.getServer().getCommands().getDispatcher());
        Command_Tpa.register(event.getServer().getCommands().getDispatcher());
        Command_RedPacket.register(event.getServer().getCommands().getDispatcher());
        Command_TerritoryClaim.register(event.getServer().getCommands().getDispatcher());
        Command_Territory.register(event.getServer().getCommands().getDispatcher());
        Command_Info.register(event.getServer().getCommands().getDispatcher());
        Command_StarterKit.register(event.getServer().getCommands().getDispatcher());
        Command_Check.register(event.getServer().getCommands().getDispatcher());

        // 初始化 经济系统
        EconomySavedData.getInstance(overworld);
        NeoForge1211CommissionRuntime.initialize(event.getServer());
        NeoForge1211RedPacketRuntime.service(event.getServer());
        NeoForge1211RewardRuntime.start(event.getServer());
        NeoForge1211StarterKitRuntime.start(event.getServer());
        NeoForge1211UpdateRuntime.start(event.getServer());

        // 初始化 市场系统
        MarketSavedData marketData = MarketSavedData.getInstance(overworld);

        // 初始化 领地系统
        EventHandler_Player.stop(event.getServer());
        TerritoryManager.reset();
        TerritoryManager.initialize(overworld);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NeoForge1211CommissionRuntime.clear(event.getServer());
        SHOP_REFRESH_SCHEDULE.reset();
        EventHandler_Player.stop(event.getServer());
        NeoForge1211TerritorySelectionRuntime.clearAll();
        NeoForge1211RedPacketRuntime.shutdown(event.getServer());
        NeoForge1211RewardRuntime.shutdown(event.getServer());
        NeoForge1211StarterKitRuntime.shutdown(event.getServer());
        NeoForge1211UpdateRuntime.shutdown(event.getServer());
        NeoForge1211TpaRuntime.shutdown(event.getServer());
        NeoForge1211ClientFileCheckRuntime.stop(event.getServer());
        // 服务器停止时，Forge 会自动保存 SavedData
        System.out.println("Saving economy data...");
        MarketSavedData.getInstance(event.getServer().overworld()).setDirty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        NeoForge1211TerritorySelectionRuntime.expire(event.getServer());
        if (event.getServer().getTickCount() % 100 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                try { NeoForge1211CommissionRuntime.refresh(player); } catch (RuntimeException ignored) { }
            }
        }
        // 确保只在每个 tick 的开始阶段执行

        ServerLevel overworld = event.getServer().overworld(); // 获取主世界
        long dayTime = overworld.getDayTime();
        if (SHOP_REFRESH_SCHEDULE.shouldRefresh(dayTime)
                && EconomyServices.platform().shopCatalog().refreshPrices()) {
            ShopDataResponseMessage refreshedCatalog = new ShopDataResponseMessage(
                EconomyServices.platform().shopCatalog().snapshot());
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                try {
                    EconomyServices.platform().network().sendToPlayer(player.getUUID(), refreshedCatalog);
                } catch (RuntimeException syncFailure) {
                    EconomySystem.LOGGER.warn("Shop live catalog sync failed player={}",
                        player.getUUID(), syncFailure);
                }
                player.sendSystemMessage(Component.translatable(ShopPriceRefreshSchedule.REFRESH_MESSAGE_KEY));
            }
        }
        if (MarketExpirationSchedule.shouldRun(event.getServer().getTickCount())) {
            NeoForge1211MarketExpirationRuntime.expire(event.getServer());
        }

        // 定时检查红包
        if (event.getServer().getTickCount() % 100 == 0) {
            NeoForge1211RedPacketRuntime.expire(event.getServer());
        }
        if (event.getServer().getTickCount() % 20 == 0) {
            Command_Tpa.expire(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            NeoForge1211UpdateRuntime.checkForUpdates(
                serverPlayer.getServer(), serverPlayer.getUUID());
            try { NeoForge1211CommissionRuntime.refresh(serverPlayer); } catch (RuntimeException ignored) { }


            ServerLevel serverLevel = serverPlayer.serverLevel();
            EconomySavedData savedData = EconomySavedData.getInstance(serverLevel);

            // 获取离线消息并发送给玩家
            List<String> offlineMessages = savedData.getOfflineMessages(serverPlayer.getUUID());
            for (String message : offlineMessages) {
                serverPlayer.sendSystemMessage(Component.literal(message));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            EventHandler_Player.clear(player.getServer(), player.getUUID());
            NeoForge1211TerritorySelectionRuntime.clear(player.getServer(), player.getUUID());
            NeoForge1211ClientFileCheckRuntime.discardPlayer(player.getServer(), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        NeoForge1211StarterKitRuntime.copyOnClone(event);
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;
        NeoForge1211CommissionRuntime.onKill(player, mob);

        ItemStack weapon = player.getMainHandItem();
        int levelCarefully = CarefullyEnchantment.getLevel(player.serverLevel(), weapon);
        int levelBountyHunter = BountyHunterEnchantment.getLevel(player.serverLevel(), weapon);

        String entityType = mob.getType().builtInRegistryHolder().key().location().toString();
        RewardService.Outcome outcome =
            NeoForge1211RewardRuntime.award(
                player.getServer(),
                player.getUUID(),
                entityType,
                mob.getName().getString(),
                levelBountyHunter,
                levelCarefully);
        switch (outcome.result()) {
            case SUCCESS -> player.sendSystemMessage(
                Component.translatable(RewardFeedback.SUCCESS, mob.getName().getString(), outcome.amount()));
            case BALANCE_LIMIT -> player.sendSystemMessage(Component.translatable(RewardFeedback.BALANCE_LIMIT));
            case PERSIST_FAILED -> player.sendSystemMessage(Component.translatable(RewardFeedback.TRANSACTION_FAILED));
            case STATE_UNKNOWN -> player.sendSystemMessage(Component.translatable(RewardFeedback.STATE_UNKNOWN));
            case UNCONFIGURED, NO_DROP -> {
                // No configured reward was paid.
            }
        }
    }
}
