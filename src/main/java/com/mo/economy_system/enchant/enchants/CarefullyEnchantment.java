package com.mo.economy_system.enchant.enchants;

import com.mo.economy_system.enchant.EconomySystem_Enchants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class CarefullyEnchantment {
    public static final String NAME = "carefully";
    public static final ResourceKey<Enchantment> KEY = EconomySystem_Enchants.createKey(NAME);
    private static final double REWARD_MULTIPLIER_PER_LEVEL = 0.3D;

    private CarefullyEnchantment() {}

    public static int getLevel(ServerLevel level, ItemStack stack) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(KEY)
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack))
                .orElse(0);
    }

    public static int applyRewardBonus(int reward, int enchantmentLevel) {
        if (enchantmentLevel <= 0) {
            return reward;
        }
        return (int) Math.round(reward * (REWARD_MULTIPLIER_PER_LEVEL * enchantmentLevel + 1.0D));
    }
}
