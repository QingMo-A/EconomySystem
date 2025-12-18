package com.mo.economy_system.sound;

import com.mo.economy_system.EconomySystem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EconomySystem_Sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, EconomySystem.MODID);

    // 丧尸游荡音效
    public static final RegistryObject<SoundEvent> HIVE_ZOMBIE_AMBIENT = registerSound("entity.hive_zombie.ambient");
    // 丧尸死亡音效
    public static final RegistryObject<SoundEvent> HIVE_ZOMBIE_DEATH = registerSound("entity.hive_zombie.death");
    // 丧尸蜂巢效应发动时音效
    public static final RegistryObject<SoundEvent> HIVE_CALL = registerSound("entity.hive_zombie.hive_call");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        ResourceLocation location = new ResourceLocation(EconomySystem.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
