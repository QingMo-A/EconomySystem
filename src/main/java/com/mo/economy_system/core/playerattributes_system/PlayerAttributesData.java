package com.mo.economy_system.core.playerattributes_system;

import com.mo.economy_system.EconomySystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

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

    //血量系统
    private double maxHealth; //最大血量

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

        this.maxHealth = calculateMaxHealthByLevel(level);

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

        this.maxHealth = calculateMaxHealthByLevel(level);
        this.syncMaxHealthToPlayer(player);

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

        this.maxHealth = calculateMaxHealthByLevel(level);

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

    /**
     * 最大血量计算
     * @param level 玩家等级
     * @return 对应等级的最大血量
     */
    public double calculateMaxHealthByLevel(int level) {
        int caculateHealth = 20 + (int)(level / 5) * 2;
        if (caculateHealth > 40) {
            caculateHealth = 40;
        }
        return caculateHealth;
    }

    // ========== 等级更新（同步更新所有属性最大值） ==========
    public void setLevel(int level, ServerPlayer player) {  // 新增ServerPlayer参数
        this.level = level;

        // 更新各属性最大值（包含最大血量）
        this.maxStrength = calculateMaxStrengthByLevel(level);
        this.maxSan = calculateMaxSanByLevel(level);
        this.maxCourage = calculateMaxCourageByLevel(level);
        this.maxHealth = calculateMaxHealthByLevel(level);

        // 防止当前属性超过新最大值
        this.currentStrength = Math.min(this.currentStrength, maxStrength);
        this.currentSan = Math.min(this.currentSan, maxSan);
        this.currentCourage = Math.min(this.currentCourage, maxCourage);

        // ========== 同步生命值到玩家实体 ==========
        // 同步生命值到玩家实体（使用自定义方法）
        if (player != null) {
            syncMaxHealthToPlayer(player);  // 调用自定义同步方法
        }

        EconomySystem.LOGGER.info("玩家 {} 等级更新为{}，属性最大值同步完成", playerName, level);
    }

    public void syncMaxHealthToPlayer(ServerPlayer player) {
        if (player == null) return;

        // 获取玩家的最大生命值属性
        AttributeInstance maxHealthAttribute = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttribute == null) return;

        // 设置最大生命值的基础值（清除所有modifiers，仅保留基础值）
        maxHealthAttribute.setBaseValue(this.maxHealth);

        // 确保当前生命值不超过新的最大值
        if (player.getHealth() > this.maxHealth) {
            player.setHealth((float) this.maxHealth);
        }

        // 同步生命值到客户端（避免显示异常）
        player.setHealth(player.getHealth());
    }

    /**
     * 自定义血量恢复、
     * @param player 服务端玩家实例
     * @param healAmount 回血数值（正数，药品配置的回血值）
     * @return 是否恢复成功（false：已达最大血量，无需恢复）
     */
    public boolean restoreCustomHealth(ServerPlayer player, double healAmount) {
        // 非空校验
        if (player == null || healAmount <= 0) {
            return false;
        }
        // 获取当前血量（从玩家实体同步，避免数据不一致）
        double currentHealth = player.getHealth();
        // 边界判断：已达最大血量，无需恢复
        if (currentHealth >= this.maxHealth) {
            return false;
        }
        // 计算新血量（不超过最大血量）
        double newHealth = Math.min(currentHealth + healAmount, this.maxHealth);
        // 同步血量到玩家实体
        player.setHealth((float) newHealth);
        // 同步客户端显示（防止血量显示异常）
        player.setHealth(player.getHealth());
        EconomySystem.LOGGER.info("玩家 {} 使用自定义药品回血：{} → {}（最大血量：{}）",
                this.playerName, currentHealth, newHealth, this.maxHealth);
        return true;
    }

    /**
     * 强制设置当前血量（用于特殊场景：如药品副作用、受伤扣血）
     * @param player 服务端玩家实例
     * @param newHealth 目标血量
     */
    public void setCustomHealth(ServerPlayer player, double newHealth) {
        if (player == null) {
            return;
        }
        // 边界控制：不低于0，不超过最大血量
        double finalHealth = Math.max(0, Math.min(newHealth, this.maxHealth));
        player.setHealth((float) finalHealth);
        player.setHealth(player.getHealth()); // 同步客户端
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

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
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