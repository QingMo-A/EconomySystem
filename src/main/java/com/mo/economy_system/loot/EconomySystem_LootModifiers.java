package com.mo.economy_system.loot;

import com.mo.economy_system.EconomySystem;
import com.mojang.serialization.Codec;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EconomySystem_LootModifiers {
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EconomySystem.MODID);

    public static final RegistryObject<Codec<? extends IGlobalLootModifier>> BLUEPRINT_LOOT =
        LOOT_MODIFIERS.register("blueprint_loot", () -> BlueprintLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}
