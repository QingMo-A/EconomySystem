package com.mo.economy_system.core.playerattributes_system.strength;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 仅在客户端注册（保留核心注解，移除绘制相关事件订阅）
@Mod.EventBusSubscriber(modid = EconomySystem.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class StrengthBarRenderer {
    // ===================== 核心：客户端体力缓存（保留，供数据存储与获取） =====================
    private static final Map<UUID, Integer> CURRENT_STRENGTH_CACHE = new HashMap<>();
    private static final Map<UUID, Integer> MAX_STRENGTH_CACHE = new HashMap<>();

    // ===================== 必要方法：设置当前体力缓存（两个重载，保留） =====================
    public static void setCurrentStrength(Player player, int currentStrength) {
        if (player == null) return;
        CURRENT_STRENGTH_CACHE.put(player.getUUID(), Math.max(0, currentStrength));
    }

    public static void setCurrentStrength(int currentStrength) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        setCurrentStrength(player, currentStrength);
    }

    // ===================== 必要方法：设置最大体力缓存（两个重载，保留） =====================
    public static void setMaxStrength(Player player, int maxStrength) {
        if (player == null) return;
        MAX_STRENGTH_CACHE.put(player.getUUID(), Math.max(1, maxStrength)); // 最小为1，避免除以0
    }

    public static void setMaxStrength(int maxStrength) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        setMaxStrength(player, maxStrength);
    }

    // ===================== 必要方法：获取客户端当前体力（保留，供其他UI调用） =====================
    public static int getCurrentStrengthClient(Player player) {
        if (player == null) return 0;
        return CURRENT_STRENGTH_CACHE.getOrDefault(player.getUUID(), 50); // 默认50，兼容初始状态
    }

    // ===================== 必要方法：获取客户端最大体力（保留，供其他UI调用） =====================
    public static int getMaxStrengthClient(Player player) {
        if (player == null) return 0;
        return MAX_STRENGTH_CACHE.getOrDefault(player.getUUID(), 100); // 默认100，兼容初始状态
    }
}