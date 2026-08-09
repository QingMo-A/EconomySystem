package com.mo.economy_system.enchant.enchants;

import com.mo.economy_system.enchant.EconomySystem_Enchants;
import com.mo.economy_system.common.reward.RewardEnchantmentIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class BountyHunterEnchantment {
    public static final String NAME = RewardEnchantmentIds.BOUNTY_HUNTER;
    public static final ResourceKey<Enchantment> KEY = EconomySystem_Enchants.createKey(NAME);

    private BountyHunterEnchantment() {}

    public static int getLevel(ServerLevel level, ItemStack stack) {
        return level.registryAccess()
                .registryOrThrow(Registries.ENCHANTMENT)
                .getHolder(KEY)
                .map(enchantment -> EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack))
                .orElse(0);
    }

}
