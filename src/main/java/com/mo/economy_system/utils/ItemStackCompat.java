package com.mo.economy_system.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class ItemStackCompat {
    private ItemStackCompat() {
    }

    public static boolean hasTag(ItemStack stack) {
        return stack.has(DataComponents.CUSTOM_DATA);
    }

    public static CompoundTag getTag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? null : data.copyTag();
    }

    public static void setTag(ItemStack stack, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    public static CompoundTag saveSimple(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        tag.putString("id", itemId.toString());
        tag.putInt("count", stack.getCount());
        CompoundTag customData = getTag(stack);
        if (customData != null && !customData.isEmpty()) {
            tag.put("customData", customData);
        }
        return tag;
    }

    public static ItemStack loadSimple(CompoundTag tag) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(tag.getString("id")));
        ItemStack stack = new ItemStack(item, Math.max(1, tag.getInt("count")));
        if (tag.contains("customData")) {
            setTag(stack, tag.getCompound("customData"));
        }
        return stack;
    }
}
