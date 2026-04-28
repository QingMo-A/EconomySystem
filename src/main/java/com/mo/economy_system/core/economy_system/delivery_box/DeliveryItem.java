package com.mo.economy_system.core.economy_system.delivery_box;

import com.mo.economy_system.utils.ItemStackCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class DeliveryItem {
    protected final UUID dataID;
    protected final String itemID;
    protected final ItemStack itemStack;
    protected final String source;

    public DeliveryItem(UUID dataID, String itemID, ItemStack itemStack, String source) {
        this.dataID = dataID;
        this.itemID = itemID;
        this.itemStack = itemStack;
        this.source = source;
    }

    // 将BoxItem转换为NBT格式
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("dataID", dataID);
        tag.putString("itemID", itemID);

        // 保存物品
        tag.put("itemStack", ItemStackCompat.saveSimple(itemStack));

        tag.putString("source", source);

        return tag;
    }

    // 从NBT反序列化BoxItem
    public static DeliveryItem fromNBT(CompoundTag tag) {
        UUID dataID = tag.getUUID("dataID");
        String itemID = tag.getString("itemID");
        ItemStack itemStack = ItemStackCompat.loadSimple(tag.getCompound("itemStack"));
        String source = tag.getString("source");

        return new DeliveryItem(dataID, itemID, itemStack, source);
    }

    public UUID getDataID() {
        return dataID;
    }

    public String getSource() {
        return source;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getItemID() {
        return itemID;
    }
}
