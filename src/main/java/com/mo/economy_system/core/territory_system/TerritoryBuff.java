package com.mo.economy_system.core.territory_system;

import com.mo.economy_system.utils.Util_Message;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class TerritoryBuff {
    private String id;
    private String displayText;
    private String effectId;
    private boolean initialUnlockState;
    private int initialLevel;
    private int singleUpgradeLevel;
    private int maxLevel;
    private List<TerritoryBuffConfig.BuffUpgradeCost> upgradeCost;

    private boolean unlocked; // 是否解锁
    private int level; // 当前等级

    public TerritoryBuff(String id, String displayText, String effectId, boolean initialUnlockState, int initialLevel, int singleUpgradeLevel, int maxLevel, List<TerritoryBuffConfig.BuffUpgradeCost> upgradeCost) {
        this.id = id;
        this.displayText = displayText;
        this.effectId = effectId;
        this.initialUnlockState = initialUnlockState;
        this.initialLevel = initialLevel;
        this.singleUpgradeLevel = singleUpgradeLevel;
        this.maxLevel = maxLevel;
        this.upgradeCost = upgradeCost;

        // 设置默认状态
        this.unlocked = initialUnlockState;
        this.level = initialLevel;
    }

    public String getId() {
        return id;
    }

    public String getDisplayText() {
        return displayText;
    }

    public String getEffectId() {
        return effectId;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public int getSingleUpgradeLevel() {
        return singleUpgradeLevel;
    }

    public boolean isUnlocked() {
        return unlocked;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public int getInitialLevel() {
        return initialLevel;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUpgradeCost(List<TerritoryBuffConfig.BuffUpgradeCost> upgradeCost) {
        this.upgradeCost = upgradeCost;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public void setSingleUpgradeLevel(int singleUpgradeLevel) {
        this.singleUpgradeLevel = singleUpgradeLevel;
    }

    public void setInitialLevel(int initialLevel) {
        this.initialLevel = initialLevel;
    }

    public void setInitialUnlockState(boolean initialUnlockState) {
        this.initialUnlockState = initialUnlockState;
    }

    public void setEffectId(String effectId) {
        this.effectId = effectId;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public boolean isInitialUnlockState() {
        return initialUnlockState;
    }

    public List<TerritoryBuffConfig.BuffUpgradeCost> getUpgradeCost() {
        return upgradeCost;
    }

    public void unlock() {
        this.unlocked = true;
    }

    public boolean upgrade() {
        if (!unlocked || level >= maxLevel) {
            return false;
        }
        level += singleUpgradeLevel;
        return true;
    }

    /**
     * 保存 Buff 状态到 NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("displayText", displayText);
        tag.putString("effectId", effectId);
        tag.putBoolean("unlocked", unlocked);
        tag.putInt("level", level);
        tag.putInt("single_Upgrade_Level", singleUpgradeLevel);
        tag.putInt("max_Level", maxLevel);

        // **✅ 添加升级费用**
        ListTag costList = new ListTag();
        if (upgradeCost != null) {
            for (TerritoryBuffConfig.BuffUpgradeCost cost : upgradeCost) {
                CompoundTag costTag = new CompoundTag();

                // **✅ 保存多个物品**
                ListTag itemListTag = new ListTag();
                for (TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement itemCost : cost.items) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putString("item", itemCost.item);
                    itemTag.putInt("count", itemCost.count);
                    itemListTag.add(itemTag);
                }
                costTag.put("items", itemListTag);

                // **✅ 经验 & 货币**
                costTag.putInt("xp", cost.xp);
                costTag.putInt("df_coin", cost.df_coin);

                costList.add(costTag);
            }
        }
        tag.put("upgrade_Cost", costList);

        return tag;
    }


    /**
     * 从 NBT 读取 Buff 状态
     */
    public static TerritoryBuff fromNBT(CompoundTag tag) {
        String id = tag.getString("id");
        String displayText = tag.getString("displayText");
        String effectId = tag.getString("effectId");
        int maxLevel = tag.getInt("max_Level");
        int singleUpgradeLevel = tag.getInt("single_Upgrade_Level");
        boolean initialUnlockState = tag.getBoolean("initialUnlockState");
        int initialLevel = tag.getInt("initialLevel");
        boolean unlocked = tag.getBoolean("unlocked");
        int level = tag.getInt("level");

        // **✅ 读取升级消耗**
        List<TerritoryBuffConfig.BuffUpgradeCost> upgradeCost = new ArrayList<>();
        ListTag costListTag = tag.getList("upgrade_Cost", Tag.TAG_COMPOUND);
        for (int i = 0; i < costListTag.size(); i++) {
            CompoundTag costTag = costListTag.getCompound(i);
            TerritoryBuffConfig.BuffUpgradeCost cost = new TerritoryBuffConfig.BuffUpgradeCost();

            // **✅ 读取多个物品**
            List<TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement> items = new ArrayList<>();
            ListTag itemListTag = costTag.getList("items", Tag.TAG_COMPOUND);
            for (int j = 0; j < itemListTag.size(); j++) {
                CompoundTag itemTag = itemListTag.getCompound(j);
                String item = itemTag.getString("item");
                int count = itemTag.getInt("count");
                items.add(new TerritoryBuffConfig.BuffUpgradeCost.ItemRequirement(item, count));
            }
            cost.items = items;

            // **✅ 读取经验 & 货币**
            cost.xp = costTag.getInt("xp");
            cost.df_coin = costTag.getInt("df_coin");

            upgradeCost.add(cost);
        }

        // 创建并返回 Buff
        TerritoryBuff buff = new TerritoryBuff(id, displayText, effectId, initialUnlockState, initialLevel, singleUpgradeLevel, maxLevel, upgradeCost);
        buff.unlocked = unlocked;
        buff.level = level;
        return buff;
    }
}
