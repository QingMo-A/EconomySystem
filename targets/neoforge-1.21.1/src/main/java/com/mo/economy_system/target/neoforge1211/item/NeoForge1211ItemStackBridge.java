package com.mo.economy_system.target.neoforge1211.item;

import com.mo.economy_system.platform.item.EconomyItemStackBridge;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class NeoForge1211ItemStackBridge implements EconomyItemStackBridge {
    @Override
    public boolean hasCustomData(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    @Override
    public CompoundTag copyCustomData(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    @Override
    public void setCustomData(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag.copy()));
        }
    }

    @Override
    public boolean sameItemAndData(ItemStack first, ItemStack second) {
        return ItemStack.isSameItemSameComponents(first, second);
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
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString("id")));
        ItemStack stack = new ItemStack(item, Math.max(1, tag.getInt("count")));
        if (tag.contains("customData")) {
            setCustomData(stack, tag.getCompound("customData"));
        }
        return stack;
    }
}
