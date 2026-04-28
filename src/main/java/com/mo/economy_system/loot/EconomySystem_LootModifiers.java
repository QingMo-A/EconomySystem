package com.mo.economy_system.loot;

import com.mo.economy_system.EconomySystem;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import net.neoforged.neoforge.registries.DeferredHolder;

public class EconomySystem_LootModifiers {
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EconomySystem.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, ? extends MapCodec<? extends IGlobalLootModifier>> BLUEPRINT_LOOT =
        LOOT_MODIFIERS.register("blueprint_loot", () -> BlueprintLootModifier.CODEC);

    public static void register(IEventBus eventBus) {
        LOOT_MODIFIERS.register(eventBus);
    }
}
