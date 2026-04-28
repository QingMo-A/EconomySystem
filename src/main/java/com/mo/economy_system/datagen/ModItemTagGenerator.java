package com.mo.economy_system.datagen;

import com.mo.economy_system.EconomySystem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagGenerator extends ItemTagsProvider {
    public ModItemTagGenerator(PackOutput p_275343_, CompletableFuture<HolderLookup.Provider> p_275729_, CompletableFuture<TagLookup<Block>> p_275322_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_275343_, p_275729_, p_275322_, EconomySystem.MODID, existingFileHelper);
    }

    public ModItemTagGenerator(PackOutput p_275204_, CompletableFuture<HolderLookup.Provider> p_275194_, CompletableFuture<TagLookup<Item>> p_275207_, CompletableFuture<TagLookup<Block>> p_275634_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_275204_, p_275194_, p_275207_, p_275634_, EconomySystem.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {

    }
}
