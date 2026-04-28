package com.mo.economy_system.client;

/**
 * 客户端死亡点数据存储
 * 用于在标题界面显示玩家的死亡点数信息
 */
public class DeathPointDataStorage {

    private static float respawnPoint = 100.0f;
    private static boolean isInfected = false;
    private static boolean hasData = false;

    /**
     * 设置死亡点数据
     */
    public static void setData(float respawnPoint, boolean isInfected) {
        DeathPointDataStorage.respawnPoint = respawnPoint;
        DeathPointDataStorage.isInfected = isInfected;
        DeathPointDataStorage.hasData = true;
    }

    /**
     * 获取复活点数
     */
    public static float getRespawnPoint() {
        return respawnPoint;
    }

    /**
     * 是否感染
     */
    public static boolean isInfected() {
        return isInfected;
    }

    /**
     * 是否有数据（已连接到服务器并收到数据）
     */
    public static boolean hasData() {
        return hasData;
    }

    /**
     * 清除数据（断开连接时调用）
     */
    public static void clearData() {
        respawnPoint = 100.0f;
        isInfected = false;
        hasData = false;
    }

    /**
     * 格式化复活点数显示
     */
    public static String formatRespawnPoint() {
        return String.format("%.1f", respawnPoint);
    }
}
