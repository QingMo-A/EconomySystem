package com.mo.economy_system.core.playerlevel_system.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.playerlevel_system.overalllevel.PlayerLevelManager;
import com.mo.economy_system.server.playerbiomes.PlayerBiomesDataManager;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 生物群系探索处理器
 * 检测玩家进入新生物群系并记录探索进度
 *
 * 参考原版 BiomeAmbientSoundsHandler 的实现方式
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID)
public class BiomeExplorationHandler {

    // ==================== 配置项 ====================
    /** 原版生物群系经验奖励（minecraft 命名空间） */
    private static final long VANILLA_BIOME_EXPERIENCE = 100L;

    /** 其他模组生物群系经验奖励（非 minecraft 命名空间） */
    private static final long MOD_BIOME_EXPERIENCE = 120L;

    /** 判断原版生物群系的命名空间 */
    private static final String VANILLA_NAMESPACE = "minecraft";

    // =================================================

    /**
     * 玩家生物群系缓存数据
     */
    private static class BiomeCacheEntry {
        Holder<Biome> lastBiome;
        ResourceLocation lastDimension;

        BiomeCacheEntry(Holder<Biome> biome, ResourceLocation dimension) {
            this.lastBiome = biome;
            this.lastDimension = dimension;
        }
    }

    // 使用 ThreadLocal 避免 HashMap 的并发开销，每个服务器线程有独立的缓存
    private static final ThreadLocal<PlayerBiomeCache> cache = ThreadLocal.withInitial(PlayerBiomeCache::new);

    /**
     * 玩家生物群系缓存，使用弱引用避免内存泄漏
     */
    private static class PlayerBiomeCache {
        private final java.util.Map<UUID, BiomeCacheEntry> cache = new java.util.WeakHashMap<>();

        BiomeCacheEntry get(UUID uuid) {
            return cache.get(uuid);
        }

        void put(UUID uuid, BiomeCacheEntry entry) {
            cache.put(uuid, entry);
        }

        void remove(UUID uuid) {
            cache.remove(uuid);
        }
    }

    /**
     * 定期检查玩家是否进入新生物群系（每秒检查一次，即每20 tick）
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;
        if (player.tickCount % 20 != 0) return;

        UUID playerUUID = player.getUUID();
        BlockPos pos = player.blockPosition();

        // 获取当前生物群系和维度
        Holder<Biome> currentBiome = player.level().getBiome(pos);
        ResourceLocation currentDimension = player.level().dimension().location();

        // 获取缓存
        PlayerBiomeCache playerCache = cache.get();
        BiomeCacheEntry lastEntry = playerCache.get(playerUUID);

        // 检查生物群系或维度是否变化
        if (lastEntry != null
                && Objects.equals(lastEntry.lastBiome, currentBiome)
                && Objects.equals(lastEntry.lastDimension, currentDimension)) {
            return; // 还在同一个生物群系中
        }

        // 获取生物群系ID用于记录
        ResourceLocation biomeId = currentBiome.unwrapKey()
                .map(key -> key.location())
                .orElse(null);

        if (biomeId == null) {
            return;
        }

        // 更新缓存
        playerCache.put(playerUUID, new BiomeCacheEntry(currentBiome, currentDimension));

        // 构建生物群系唯一键（包含维度信息）
        String biomeKey = currentDimension + ":" + biomeId;

        // 尝试添加探索记录
        boolean isNewBiome = PlayerBiomesDataManager.addExploredBiome(playerUUID, biomeKey);

        if (isNewBiome) {
            // 新生物群系探索！
            onNewBiomeDiscovered(player, biomeId, biomeKey);
        }
    }

    /**
     * 玩家登出时清理缓存
     */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cache.get().remove(player.getUUID());
        }
    }

    /**
     * 处理玩家发现新生物群系
     */
    private static void onNewBiomeDiscovered(ServerPlayer player, ResourceLocation biomeId, String biomeKey) {
        int totalExplored = PlayerBiomesDataManager.getExploredBiomeCount(player.getUUID());

        // 计算经验奖励
        long expReward = calculateExperienceReward(biomeId);

        // 发送探索消息
        player.sendSystemMessage(Component.literal("§a✦ 发现新生物群系！§e" + biomeId.getPath()));
        player.sendSystemMessage(Component.literal("§7已探索生物群系总数：§f" + totalExplored));
        player.sendSystemMessage(Component.literal("§b+§f" + expReward + " §b经验"));

        // 发放经验奖励
        PlayerLevelManager.addPlayerExperienceServer(player, expReward);

        EconomySystem.LOGGER.info("玩家 {} 发现新生物群系：{}，总计 {} 个，获得 {} 经验",
                player.getScoreboardName(), biomeKey, totalExplored, expReward);
    }

    /**
     * 计算探索新生物群系的经验奖励
     * @param biomeId 生物群系ID
     * @return 经验值
     */
    private static long calculateExperienceReward(ResourceLocation biomeId) {
        // 原版生物群系 vs 模组生物群系
        boolean experienceReward = VANILLA_NAMESPACE.equals(biomeId.getNamespace());
        return experienceReward ? VANILLA_BIOME_EXPERIENCE : MOD_BIOME_EXPERIENCE;
    }

    /**
     * 查询玩家已探索的生物群系列表
     * 可通过命令调用此方法
     */
    public static void showExploredBiomes(ServerPlayer player) {
        Set<String> exploredBiomes = PlayerBiomesDataManager.getExploredBiomes(player.getUUID());
        int count = exploredBiomes.size();

        player.sendSystemMessage(Component.literal("§6========== 生物群系探索进度 =========="));
        player.sendSystemMessage(Component.literal("§e已探索生物群系数量：§f" + count));
        player.sendSystemMessage(Component.literal("§7-------------------------------------"));

        if (count > 0) {
            // 显示最近探索的几个生物群系
            exploredBiomes.stream()
                    .skip(Math.max(0, count - 10))
                    .forEach(biomeKey -> {
                        String[] parts = biomeKey.split(":", 3);
                        if (parts.length >= 3) {
                            String dimension = parts[1];
                            String biome = parts[2];
                            player.sendSystemMessage(Component.literal(
                                String.format("§8[§7%s§8] §f%s", dimension, biome)
                            ));
                        }
                    });
        } else {
            player.sendSystemMessage(Component.literal("§c你还没有探索任何生物群系！"));
        }

        player.sendSystemMessage(Component.literal("§6======================================"));
    }

    /**
     * 获取生物群系的显示名称
     */
    public static String getBiomeDisplayName(ResourceLocation biomeId) {
        // 可以在这里添加生物群系名称的本地化映射
        // 例如：minecraft:plains -> "平原"
        return biomeId.getPath();
    }
}
