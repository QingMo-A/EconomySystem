package com.mo.economy_system.core.territory_system;

import java.util.ArrayList;
import java.util.List;

public class TerritoryBuffConfig {
    private String id;
    private String displayText;
    private String effectId;
    private boolean initialUnlockState;
    private int initialLevel;
    private int singleUpgradeLevel;
    private int maxLevel;
    private List<BuffUpgradeCost> upgradeCost;

    public static class BuffUpgradeCost {
        public List<ItemRequirement> items = new ArrayList<>();
        public int xp;
        // Keep the serialized field name for compatibility with current JSON.
        public int df_coin;

        public static class ItemRequirement {
            public String item;
            public int count;

            public ItemRequirement(String item, int count) {
                this.item = item;
                this.count = count;
            }
        }
    }

    public String getId() {
        return id;
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

    public List<BuffUpgradeCost> getUpgradeCost() {
        return upgradeCost;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDisplayText() {
        return displayText;
    }

    public void setDisplayText(String displayText) {
        this.displayText = displayText;
    }

    public void setEffectId(String effectId) {
        this.effectId = effectId;
    }

    public boolean isInitialUnlockState() {
        return initialUnlockState;
    }

    public void setInitialUnlockState(boolean initialUnlockState) {
        this.initialUnlockState = initialUnlockState;
    }

    public int getInitialLevel() {
        return initialLevel;
    }

    public void setInitialLevel(int initialLevel) {
        this.initialLevel = initialLevel;
    }

    public void setSingleUpgradeLevel(int singleUpgradeLevel) {
        this.singleUpgradeLevel = singleUpgradeLevel;
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public void setUpgradeCost(List<BuffUpgradeCost> upgradeCost) {
        this.upgradeCost = upgradeCost;
    }
}
