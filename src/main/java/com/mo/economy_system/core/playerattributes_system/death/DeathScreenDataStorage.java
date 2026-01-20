package com.mo.economy_system.core.playerattributes_system.death;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;

/**
 * 死亡屏幕数据存储
 * 用于在数据包到达和屏幕打开之间存储数据
 */
public class DeathScreenDataStorage {

    private static DeathScreenData storedData = null;

    /**
     * 设置待显示的死亡屏幕数据
     * 如果死亡屏幕已经打开，通知其刷新
     */
    public static void setData(float respawnPoint, float normalCost, float keepInventoryCost, boolean isInfected, Component deathMessage) {
        storedData = new DeathScreenData(respawnPoint, normalCost, keepInventoryCost, isInfected, deathMessage);

        // 如果当前正在显示死亡屏幕，需要刷新按钮状态
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DeathScreen) {
            // 标记需要重新初始化按钮
            setNeedsReinit(true);
        }
    }

    /**
     * 获取存储的数据
     * 如果没有数据，返回默认数据（用于重连后数据包还未到达的情况）
     */
    public static DeathScreenData getData() {
        if (storedData == null) {
            // 返回默认数据，确保始终显示自定义界面
            return new DeathScreenData(100.0f, 5.0f, 35.0f, false, Component.literal("您 died"));
        }
        return storedData;
    }

    /**
     * 清除存储的数据
     */
    public static void clearData() {
        storedData = null;
        setNeedsReinit(false);
    }

    /**
     * 是否需要重新初始化按钮
     */
    private static boolean needsReinit = false;

    public static boolean needsReinit() {
        return needsReinit;
    }

    public static void setNeedsReinit(boolean value) {
        needsReinit = value;
    }

    /**
     * 死亡屏幕数据
     */
    public record DeathScreenData(
            float respawnPoint,
            float normalCost,
            float keepInventoryCost,
            boolean isInfected,
            Component deathMessage
    ) {}
}