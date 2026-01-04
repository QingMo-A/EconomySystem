package com.mo.economy_system.server;

import com.mo.economy_system.EconomySystem;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 关闭玩家自然回血功能的核心类
 */
@Mod.EventBusSubscriber(modid = EconomySystem.MODID,  bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CloseNaturalRegeneration {

    /**
     * 核心方法：关闭所有维度的自然回血（推荐使用，全局生效）
     */
    @SubscribeEvent
    public static void disableAllDimensionsNaturalRegeneration(ServerStartedEvent event) {
        // 先判断服务端实例是否已初始化（避免空指针异常）
        MinecraftServer server = GetServerInstance.SERVER_INSTANCE;
        if (server == null) {
            return;
        }

        // 获取 naturalRegeneration 游戏规则的Key（布尔类型）
        GameRules.Key<GameRules.BooleanValue> regenRuleKey = GameRules.RULE_NATURAL_REGENERATION;

        //遍历所有服务端维度（主世界、下界、末地），确保全局生效
        for (ServerLevel level : server.getAllLevels()) {
            //获取当前维度的游戏规则，并设置为 false（关闭自然回血）
            // 第二个参数 server：用于同步所有客户端，确保客户端状态与服务端一致
            level.getGameRules().getRule(regenRuleKey).set(false, server);
            LogUtils.getLogger().info("已关闭自然回血功能");
        }
    }

    /**
     * 关闭指定维度的自然回血
     * @param targetLevel 目标服务端维度
     */
    public static void disableSpecifiedDimensionNaturalRegeneration(ServerLevel targetLevel) {
        // 非空判断（服务端实例 + 目标维度）
        MinecraftServer server = GetServerInstance.SERVER_INSTANCE;
        if (server == null || targetLevel == null) {
            // LogUtils.getLogger().warn("服务端实例或目标维度为空，无法关闭自然回血！");
            return;
        }

        GameRules.Key<GameRules.BooleanValue> regenRuleKey = GameRules.RULE_NATURAL_REGENERATION;
        targetLevel.getGameRules().getRule(regenRuleKey).set(false, server);

        // 可选：日志输出
        // LogUtils.getLogger().info("维度 " + targetLevel.dimension().location() + " 已关闭自然回血功能");
    }
}