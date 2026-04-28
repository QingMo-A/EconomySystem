package com.mo.economy_system.sound;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mo.economy_system.EconomySystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.neoforged.neoforge.registries.DeferredHolder;

public class EconomySystem_Sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, EconomySystem.MODID);

    // 丧尸游荡音效
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> HIVE_ZOMBIE_AMBIENT = registerSound("entity.hive_zombie.ambient");
    // 丧尸受伤音效
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> HIVE_ZOMBIE_HURT = registerSound("entity.hive_zombie.hurt");
    // 丧尸死亡音效
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> HIVE_ZOMBIE_DEATH = registerSound("entity.hive_zombie.death");
    // 丧尸蜂巢效应发动时音效
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> HIVE_CALL = registerSound("entity.hive_zombie.hive_call");

    private static DeferredHolder<SoundEvent, ? extends SoundEvent> registerSound(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
