package com.mo.economy_system.utils;

import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;

public final class ItemStackDataHelper {
    private ItemStackDataHelper() {
    }

    public static boolean hasTag(ItemStack stack) {
        return EconomyServices.platform().itemStacks().hasCustomData(stack);
    }

    public static CompoundTag getTag(ItemStack stack) {
        return EconomyServices.platform().itemStacks().copyCustomData(stack);
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        EconomyServices.platform().itemStacks().setCustomData(stack, tag);
    }

    public static CompoundTag saveSimple(ItemStack stack) {
        return EconomyServices.platform().itemStacks().saveSimple(stack);
    }

    public static ItemStack loadSimple(CompoundTag tag) {
        return EconomyServices.platform().itemStacks().loadSimple(tag);
    }

    public static String saveFull(ItemStack stack, RegistryAccess registryAccess) {
        return saveFullTag(stack, registryAccess).toString();
    }

    public static ItemStack loadFull(String itemData, RegistryAccess registryAccess) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        CompoundTag tag = TagParser.parseTag(itemData);
        return loadFullTag(tag, registryAccess);
    }

    public static CompoundTag saveFullTag(ItemStack stack, HolderLookup.Provider registries) {
        return (CompoundTag) stack.save(registries);
    }

    public static ItemStack loadFullTag(CompoundTag tag, HolderLookup.Provider registries) {
        return ItemStack.parseOptional(registries, tag);
    }
}
