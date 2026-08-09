package com.mo.economy_system.datagen;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.item.EconomySystem_Items;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput, HolderLookup.Provider registries) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, EconomySystem_Items.CLAIM_WAND.get())
                .requires(Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(EconomySystem.MODID, "stick_to_claim_wand"));
    }
}
