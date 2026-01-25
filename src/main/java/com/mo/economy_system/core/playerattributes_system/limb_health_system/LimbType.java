package com.mo.economy_system.core.playerattributes_system.limb_health_system;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * 肢体部位枚举
 * 用于判断玩家受伤的具体部位，并应用不同的伤害倍率
 */
public enum LimbType {
    /**
     * 头部 - 伤害倍率 1.2
     */
    HEAD("头部", EquipmentSlot.HEAD, 1.2F),

    /**
     * 胸部/身体 - 伤害倍率 1.0（默认）
     */
    CHEST("胸部", EquipmentSlot.CHEST, 1.0F),

    /**
     * 腿部 - 伤害倍率 0.9
     */
    LEGS("腿部", EquipmentSlot.LEGS, 0.9F),

    /**
     * 脚部 - 伤害倍率 0.9
     */
    FEET("脚部", EquipmentSlot.FEET, 0.9F);

    private final String displayName;
    private final EquipmentSlot equipmentSlot;
    private final float damageMultiplier;

    LimbType(String displayName, EquipmentSlot equipmentSlot, float damageMultiplier) {
        this.displayName = displayName;
        this.equipmentSlot = equipmentSlot;
        this.damageMultiplier = damageMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public float getDamageMultiplier() {
        return damageMultiplier;
    }

    /**
     * 获取该部位的显示名称（带伤害倍率）
     */
    public String getDisplayNameWithMultiplier() {
        if (damageMultiplier >= 1.0F) {
            return displayName + " (×" + damageMultiplier + ")";
        } else {
            return displayName + " (×" + damageMultiplier + ")";
        }
    }
}
