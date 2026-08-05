package com.mo.economy_system.events;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.commands.check_system.Command_Check;
import com.mo.economy_system.commands.check_system.Command_Info;
import com.mo.economy_system.commands.economy_system.Command_Economy;
import com.mo.economy_system.commands.economy_system.Command_RedPacket;
import com.mo.economy_system.commands.territory_system.Command_Territory;
import com.mo.economy_system.commands.territory_system.Command_TerritoryClaim;
import com.mo.economy_system.commands.tpa_system.Command_Tpa;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.enchant.EconomySystem_Enchants;
import com.mo.economy_system.core.economy_system.market.DemandOrder;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.core.economy_system.market.MarketSavedData;
import com.mo.economy_system.core.economy_system.red_packet.RedPacketManager;
import com.mo.economy_system.core.economy_system.reward.RewardManager;
import com.mo.economy_system.core.economy_system.shop.ShopManager;
import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.territory_system.TerritoryManager;
import com.mo.economy_system.core.update_checker_system.UpdateChecker;
import com.mo.economy_system.enchant.enchants.BountyHunterEnchantment;
import com.mo.economy_system.enchant.enchants.CarefullyEnchantment;
import com.mo.economy_system.utils.Util_MessageKeys;
import com.mo.economy_system.target.neoforge1211.protocol.NeoForge1211ClientFileCheckRuntime;
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
import java.util.Random;
import java.util.UUID;

@EventBusSubscriber(modid = EconomySystem.MODID)
public class EconomySystem_EventHandler {

    public static ShopManager shopManager = EconomySystem.SHOP_MANAGER;

    private static final Random RANDOM = new Random();

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // 获取服务器主世界
        ServerLevel overworld = event.getServer().overworld();

        // 注册指令
        Command_Economy.register(event.getServer().getCommands().getDispatcher());
        Command_Tpa.register(event.getServer().getCommands().getDispatcher());
        Command_RedPacket.register(event.getServer().getCommands().getDispatcher());
        Command_TerritoryClaim.register(event.getServer().getCommands().getDispatcher());
        Command_Territory.register(event.getServer().getCommands().getDispatcher());
        Command_Info.register(event.getServer().getCommands().getDispatcher());
        // StarterKitCommand.register(event.getServer().getCommands().getDispatcher());
        Command_Check.register(event.getServer().getCommands().getDispatcher());

        // 初始化 经济系统
        EconomySavedData.getInstance(overworld);

        // 初始化 市场系统
        MarketSavedData marketData = MarketSavedData.getInstance(overworld);

        // 初始化 领地系统
        TerritoryManager.reset();
        TerritoryManager.initialize(overworld);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        NeoForge1211ClientFileCheckRuntime.stop(event.getServer());
        // 服务器停止时，Forge 会自动保存 SavedData
        System.out.println("Saving economy data...");
        MarketSavedData.getInstance(event.getServer().overworld()).setDirty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // 确保只在每个 tick 的开始阶段执行

        ServerLevel overworld = event.getServer().overworld(); // 获取主世界
        long dayTime = overworld.getDayTime() % 24000; // 获取当前世界的时间（一天的 tick 范围 0-23999）

        // 如果是中午（6000 tick）
        if (dayTime == 6000 || dayTime == 18000) {
            if (shopManager != null) {
                shopManager.adjustPrices(); // 调整价格

                // 遍历所有在线玩家并发送消息
                for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                    player.sendSystemMessage(Component.translatable(Util_MessageKeys.SHOP_REFRESH_MESSAGE_KEY));
                }
            }

            EconomySavedData economySavedData = EconomySavedData.getInstance(overworld);
            MarketSavedData marketData = MarketSavedData.getInstance(overworld);
            DeliveryBoxSavedData deliveryBoxSavedData = DeliveryBoxSavedData.getInstance(overworld);
            List<MarketItem> currentMarketItems = MarketManager.getMarketItems();
            marketData.clearMarketItems();
            for (MarketItem item : currentMarketItems) {
                if (item.isExpired()) {
                    ServerPlayer owner = event.getServer().getPlayerList().getPlayer(item.getSellerID());
                    if (item instanceof DemandOrder demandOrder) {
                        if (demandOrder.isDelivered()) {
                            deliveryBoxSavedData.addItem(item.getSellerID(), new DeliveryItem(UUID.randomUUID(), item.getItemID(), item.getItemStack(), "Market"));
                            if (owner != null) {
                                owner.sendSystemMessage(Component.literal("你的求购物品 " + item.getItemStack().getHoverName().getString() + " 已交付并过期确认, 请前往收货箱领取"));
                            } else {
                                String text = Component.literal("你的求购物品 " + item.getItemStack().getHoverName().getString() + " 已交付并过期确认, 请前往收货箱领取").getString();
                                economySavedData.storeOfflineMessage(item.getSellerID(), text);
                            }
                        } else {
                            economySavedData.addBalance(item.getSellerID(), item.getBasePrice(), "市场", "求购单过期退款");
                            if (owner != null) {
                                owner.sendSystemMessage(Component.literal("你的求购单 " + item.getItemStack().getHoverName().getString() + " 已过期, 货币已退回"));
                            } else {
                                String text = Component.literal("你的求购单 " + item.getItemStack().getHoverName().getString() + " 已过期, 货币已退回").getString();
                                economySavedData.storeOfflineMessage(item.getSellerID(), text);
                            }
                        }
                    } else {
                        // 出售单过期时返还商品。
                        deliveryBoxSavedData.addItem(item.getSellerID(), new DeliveryItem(UUID.randomUUID(), item.getItemID(), item.getItemStack(), "Market"));
                        if (owner != null) {
                            owner.sendSystemMessage(Component.literal("你的物品 " + item.getItemStack().getHoverName().getString() + " 已经过期, 请前往收货箱领取"));
                        } else {
                            String text = Component.literal("你的物品 " + item.getItemStack().getHoverName().getString() + " 已经过期, 请前往收货箱领取").getString();
                            economySavedData.storeOfflineMessage(item.getSellerID(), text);
                        }
                    }
                } else {
                    marketData.addMarketItem(item); // 保存当前市场商品
                }
            }
        }

        // 定时检查红包
        if (dayTime % 100 == 0) {
            RedPacketManager.checkAndExpireRedPackets();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            UpdateChecker.checkForUpdates(serverPlayer);


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
            NeoForge1211ClientFileCheckRuntime.discardPlayer(player.getServer(), player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;

        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) return;

        ItemStack weapon = player.getMainHandItem();
        int levelCarefully = CarefullyEnchantment.getLevel(player.serverLevel(), weapon);
        int levelBountyHunter = BountyHunterEnchantment.getLevel(player.serverLevel(), weapon);

        RewardManager.RewardEntry rewardEntry = EconomySystem.REWARD_MANAGER
                .getRewardForEntity(mob.getType().builtInRegistryHolder().key().location())
                .orElse(null);

        if (rewardEntry != null) {
            // 计算掉落奖励
            int reward = calculateReward(levelBountyHunter, levelCarefully, rewardEntry);

            if (reward > 0) {
                EconomySavedData economy = EconomySavedData.getInstance(player.serverLevel());
                economy.addBalance(player.getUUID(), reward, "系统", "击杀奖励: " + event.getEntity().getName().getString());
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MOB_REWARD_MESSAGE_KEY, event.getEntity().getName().getString(), reward));
            }
        }
    }

    /**
     * 计算奖励的方法，包括附魔加成和掉落概率的判断
     *
     * @param levelBountyHunter 赏金猎人附魔的等级
     * @param levelCarefully 精心附魔的等级
     * @param rewardEntry 奖励条目，包含基础掉落概率和最小最大掉落
     * @return 返回最终计算的奖励
     */
    private static int calculateReward(int levelBountyHunter, int levelCarefully, RewardManager.RewardEntry rewardEntry) {
        // 基础掉落概率
        double chance = rewardEntry.dropChance;

        // 如果携带“赏金猎人”附魔 -> 增加掉落概率
        chance = BountyHunterEnchantment.applyDropChanceBonus(chance, levelBountyHunter);

        // 判断是否掉落
        if (RANDOM.nextDouble() < chance) {
            int reward = RANDOM.nextInt(rewardEntry.dropMax - rewardEntry.dropMin + 1) + rewardEntry.dropMin;
            // 如果携带“精心”附魔 -> 增加奖励
            reward = CarefullyEnchantment.applyRewardBonus(reward, levelCarefully);
            return reward;
        }
        return 0; // 如果不掉落，返回 0
    }
}
