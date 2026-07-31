package com.mo.economy_system.target.forge1201.item;

import com.mo.economy_system.platform.item.EconomyItemStackBridge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class Forge1201ItemStackBridge implements EconomyItemStackBridge {
    @Override
    public boolean hasCustomData(ItemStack stack) {
        return stack.hasTag();
    }

    @Override
    public CompoundTag copyCustomData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? null : tag.copy();
    }

    @Override
    public void setCustomData(ItemStack stack, CompoundTag tag) {
        stack.setTag(tag == null || tag.isEmpty() ? null : tag.copy());
    }

    @Override
    public boolean sameItemAndData(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameTags(first, second);
    }

    @Override
    public CompoundTag saveSimple(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        tag.putInt("count", stack.getCount());
        CompoundTag customData = copyCustomData(stack);
        if (customData != null && !customData.isEmpty()) {
            tag.put("customData", customData);
        }
        return tag;
    }

    @Override
    public ItemStack loadSimple(CompoundTag tag) {
        Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(tag.getString("id")));
        ItemStack stack = new ItemStack(item, Math.max(1, tag.getInt("count")));
        if (tag.contains("customData")) {
            setCustomData(stack, tag.getCompound("customData"));
        }
        return stack;
    }
}
