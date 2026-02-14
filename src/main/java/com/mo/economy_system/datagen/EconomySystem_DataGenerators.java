package com.mo.economy_system.datagen;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.datagen.lang.EnUsLanguageProvider;
import com.mo.economy_system.datagen.lang.ZhCnLanguageProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = EconomySystem.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class EconomySystem_DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator dataGenerator = event.getGenerator();
        PackOutput packOutput = dataGenerator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> providerCompletableFuture = event.getLookupProvider();

        dataGenerator.addProvider(event.includeServer(), new ModItemModelProvider(packOutput, existingFileHelper));
        dataGenerator.addProvider(event.includeServer(), new ZhCnLanguageProvider(dataGenerator, EconomySystem.MODID));
        dataGenerator.addProvider(event.includeServer(), new EnUsLanguageProvider(dataGenerator, EconomySystem.MODID));
    }
}
