package com.mo.economy_system.core.playerattributes_system;

import com.mo.economy_system.EconomySystem;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * 玩家属性数据实体类（体力、SAN值、勇气值、感染值）
 * 所有属性最大值与玩家等级关联，提供属性消耗/恢复的边界检查
 */
public class PlayerAttributesData {
    // 基础字段
    private UUID playerUUID;
    private String playerName;
    private int level; // 关联等级

    // 体力属性
    private int maxStrength;
    private int currentStrength;

    // SAN值
    private int maxSan;
    private int currentSan;

    // 勇气值
    private int maxCourage;
    private int currentCourage;

    // 感染值（0-100，100为完全感染）
    private int currentInfection;

    // 防重复提示标记（各属性不足时避免刷屏）
    private boolean strengthWarned;
    private boolean sanWarned;
    private boolean courageWarned;
    private boolean infectionWarned;

    /**
     * 无参构造（Gson反序列化必须）
     */
    public PlayerAttributesData() {
        this.playerUUID = UUID.randomUUID();
        this.playerName = "";
        this.level = 1;

        // 初始化属性（默认等级1的最大值）
        this.maxStrength = calculateMaxStrengthByLevel(level);
        this.currentStrength = maxStrength;

        this.maxSan = calculateMaxSanByLevel(level);
        this.currentSan = maxSan;

        this.maxCourage = calculateMaxCourageByLevel(level);
        this.currentCourage = maxCourage;

        this.currentInfection = 0;

        // 初始化提示标记
        this.strengthWarned = false;
        this.sanWarned = false;
        this.courageWarned = false;
        this.infectionWarned = false;
    }

    /**
     * 从ServerPlayer初始化（新玩家）
     */
    public PlayerAttributesData(ServerPlayer player) {
        this.playerUUID = player.getUUID();
        this.playerName = player.getScoreboardName();
        this.level = 1; // 默认初始等级1

        // 等级关联初始化属性最大值
        this.maxStrength = calculateMaxStrengthByLevel(level);
        this.currentStrength = maxStrength;

        this.maxSan = calculateMaxSanByLevel(level);
        this.currentSan = maxSan;

        this.maxCourage = calculateMaxCourageByLevel(level);
        this.currentCourage = maxCourage;

        this.currentInfection = 0;

        // 初始化提示标记
        this.strengthWarned = false;
        this.sanWarned = false;
        this.courageWarned = false;
        this.infectionWarned = false;

        EconomySystem.LOGGER.info("玩家 {} 属性数据初始化完成（等级1）", player.getScoreboardName());
    }

    /**
     * 自定义初始化（指定UUID、名称、等级）
     */
    public PlayerAttributesData(UUID playerUUID, String playerName, int level) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.level = level;

        // 等级关联初始化属性最大值
        this.maxStrength = calculateMaxStrengthByLevel(level);
        this.currentStrength = maxStrength;

        this.maxSan = calculateMaxSanByLevel(level);
        this.currentSan = maxSan;

        this.maxCourage = calculateMaxCourageByLevel(level);
        this.currentCourage = maxCourage;

        this.currentInfection = 0;

        // 初始化提示标记
        this.strengthWarned = false;
        this.sanWarned = false;
        this.courageWarned = false;
        this.infectionWarned = false;
    }

    /**
     * 体力最大值：基础100，每级+5
     */
    public int calculateMaxStrengthByLevel(int level) {
        return 700 + (level - 1) * 60;
    }

    /**
     * SAN值最大值：基础100，每级+3
     */
    public int calculateMaxSanByLevel(int level) {
        return 100 + (level - 1) * 3;
    }

    /**
     * 勇气值最大值：基础100，每级+4
     */
    public int calculateMaxCourageByLevel(int level) {
        return 100 + (level - 1) * 4;
    }

    // ========== 等级更新（同步更新所有属性最大值） ==========
    public void setLevel(int level) {
        this.level = level;

        // 更新各属性最大值
        this.maxStrength = calculateMaxStrengthByLevel(level);
        this.maxSan = calculateMaxSanByLevel(level);
        this.maxCourage = calculateMaxCourageByLevel(level);

        // 防止当前属性超过新最大值
        this.currentStrength = Math.min(this.currentStrength, maxStrength);
        this.currentSan = Math.min(this.currentSan, maxSan);
        this.currentCourage = Math.min(this.currentCourage, maxCourage);

        EconomySystem.LOGGER.info("玩家 {} 等级更新为{}，属性最大值同步完成", playerName, level);
    }

    // 体力消耗（返回是否消耗成功）
    public boolean consumeStrength(int amount) {
        if (currentStrength >= amount) {
            currentStrength -= amount;
            return true;
        }
        return false;
    }

    // 体力恢复（带上限）
    public void restoreStrength(int amount) {
        currentStrength = Math.min(currentStrength + amount, maxStrength);
        // 恢复后重置提示标记
        if (currentStrength > maxStrength * 0.2) {
            this.strengthWarned = false;
        }
    }

    // SAN值消耗
    public boolean consumeSan(int amount) {
        if (currentSan >= amount) {
            currentSan -= amount;
            return true;
        }
        return false;
    }

    // SAN值恢复
    public void restoreSan(int amount) {
        currentSan = Math.min(currentSan + amount, maxSan);
        if (currentSan > maxSan * 0.2) {
            this.sanWarned = false;
        }
    }

    // 勇气值消耗
    public boolean consumeCourage(int amount) {
        if (currentCourage >= amount) {
            currentCourage -= amount;
            return true;
        }
        return false;
    }

    // 勇气值恢复
    public void restoreCourage(int amount) {
        currentCourage = Math.min(currentCourage + amount, maxCourage);
        if (currentCourage > maxCourage * 0.2) {
            this.courageWarned = false;
        }
    }

    // 感染值增加（上限100）
    public void addInfection(int amount) {
        currentInfection = Math.min(currentInfection + amount, 100);
    }

    // 感染值减少（下限0）
    public void reduceInfection(int amount) {
        currentInfection = Math.max(currentInfection - amount, 0);
        if (currentInfection < 80) { // 感染值低于80重置提示
            this.infectionWarned = false;
        }
    }

    // ========== 属性不足/超标判断（提示触发依据） ==========
    public boolean isStrengthLow() {
        return currentStrength <= maxStrength * 0.2; // 体力低于20%
    }

    public boolean isSanLow() {
        return currentSan <= maxSan * 0.2; // SAN值低于20%
    }

    public boolean isCourageLow() {
        return currentCourage <= maxCourage * 0.2; // 勇气值低于20%
    }

    public boolean isInfectionHigh() {
        return currentInfection >= 80; // 感染值高于80%
    }

    // ========== Getter/Setter（Gson序列化+外部调用） ==========
    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public int getLevel() {
        return level;
    }

    public int getMaxStrength() {
        return maxStrength;
    }

    public int getCurrentStrength() {
        return currentStrength;
    }

    public void setCurrentStrength(int currentStrength) {
        this.currentStrength = currentStrength;
    }

    public int getMaxSan() {
        return maxSan;
    }

    public int getCurrentSan() {
        return currentSan;
    }

    public void setCurrentSan(int currentSan) {
        this.currentSan = currentSan;
    }

    public int getMaxCourage() {
        return maxCourage;
    }

    public int getCurrentCourage() {
        return currentCourage;
    }

    public void setCurrentCourage(int currentCourage) {
        this.currentCourage = currentCourage;
    }

    public int getCurrentInfection() {
        return currentInfection;
    }

    public void setCurrentInfection(int currentInfection) {
        this.currentInfection = currentInfection;
    }

    public boolean isStrengthWarned() {
        return strengthWarned;
    }

    public void setStrengthWarned(boolean strengthWarned) {
        this.strengthWarned = strengthWarned;
    }

    public boolean isSanWarned() {
        return sanWarned;
    }

    public void setSanWarned(boolean sanWarned) {
        this.sanWarned = sanWarned;
    }

    public boolean isCourageWarned() {
        return courageWarned;
    }

    public void setCourageWarned(boolean courageWarned) {
        this.courageWarned = courageWarned;
    }

    public boolean isInfectionWarned() {
        return infectionWarned;
    }

    public void setInfectionWarned(boolean infectionWarned) {
        this.infectionWarned = infectionWarned;
    }
}