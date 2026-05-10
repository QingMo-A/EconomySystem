package com.mo.economy_system.enchant;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.enchant.enchants.BountyHunterEnchantment;
import com.mo.economy_system.enchant.enchants.CarefullyEnchantment;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class EconomySystem_Enchants {
    public static final ResourceKey<Enchantment> CAREFULLY = CarefullyEnchantment.KEY;
    public static final ResourceKey<Enchantment> BOUNTY_HUNTER = BountyHunterEnchantment.KEY;

    public static void register(IEventBus eventBus) {
        // Enchantments are data-driven in 1.21.1. Keep this hook so the old
        // 1:1 project structure can stay in place while JSON definitions carry
        // the registrations.
    }

    public static ResourceKey<Enchantment> createKey(String name) {
        return ResourceKey.create(
                Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, name)
        );
    }
}
