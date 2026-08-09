package com.mo.economy_system.sound;

import com.mo.economy_system.EconomySystem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class EconomySystem_Sounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, EconomySystem.MODID);

    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_C = registerSound("note.c");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_DM = registerSound("note.dm");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_EM = registerSound("note.em");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_F = registerSound("note.f");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_G = registerSound("note.g");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_AM = registerSound("note.am");
    public static final DeferredHolder<SoundEvent, ? extends SoundEvent> NOTE_BM = registerSound("note.bm");

    private static DeferredHolder<SoundEvent, ? extends SoundEvent> registerSound(String name) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(location));
    }
}
