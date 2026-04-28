package com.mo.economy_system.core.economy_system.delivery_box;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class DeliveryBoxSavedData extends SavedData {
    private static final String DATA_NAME = "delivery_box_data";
    private final Map<UUID, List<DeliveryItem>> playerBoxes = new HashMap<>();

    // 加载数据
    public static DeliveryBoxSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DeliveryBoxSavedData data = new DeliveryBoxSavedData();
        for (String key : tag.getAllKeys()) {
            UUID uuid = UUID.fromString(key);
            List<DeliveryItem> items = new ArrayList<>();
            ListTag listTag = tag.getList(key, Tag.TAG_COMPOUND);
            for (int i = 0; i < listTag.size(); i++) {
                items.add(DeliveryItem.fromNBT(listTag.getCompound(i)));
            }
            data.playerBoxes.put(uuid, items);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        for (Map.Entry<UUID, List<DeliveryItem>> entry : playerBoxes.entrySet()) {
            ListTag listTag = new ListTag();
            for (DeliveryItem boxItem : entry.getValue()) {
                listTag.add(boxItem.toNBT());
            }
            tag.put(entry.getKey().toString(), listTag);
        }
        return tag;
    }

    // 获取玩家数据
    public static DeliveryBoxSavedData getInstance(ServerLevel level) {
        // 获取主世界（Overworld）的保存数据
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) {
            throw new IllegalStateException("Overworld is not loaded!");
        }
        return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(DeliveryBoxSavedData::new, DeliveryBoxSavedData::load), DATA_NAME);
    }

    // 添加物品到玩家箱子
    public void addItem(UUID playerUUID, DeliveryItem boxItem) {
        playerBoxes.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(boxItem);
        setDirty();
    }

    // 获取玩家箱子物品
    public List<DeliveryItem> getItems(UUID playerUUID) {
        return playerBoxes.getOrDefault(playerUUID, new ArrayList<>());
    }

    // 重载 getItems 方法，获取指定玩家指定 ID 的物品
    public DeliveryItem getItem(UUID playerUUID, UUID dataUUID) {
        // 获取指定玩家的物品列表
        List<DeliveryItem> items = playerBoxes.get(playerUUID);

        if (items != null) {
            // 遍历物品列表，查找匹配的物品
            for (DeliveryItem item : items) {
                if (item.getDataID().equals(dataUUID)) { // 假设 DeliveryItem 有一个 getItemUUID 方法来获取物品的 UUID
                    return item;
                }
            }
        }

        // 如果没有找到对应的物品，返回 null 或抛出异常，视需求而定
        return null; // 或者 throw new ItemNotFoundException("Item not found!");
    }

    // 移除指定玩家的指定 ID 物品
    public boolean removeItem(UUID playerUUID, UUID dataUUID) {
        // 获取玩家的物品列表
        List<DeliveryItem> items = playerBoxes.get(playerUUID);

        if (items != null) {
            // 迭代移除符合条件的物品
            boolean removed = items.removeIf(item -> item.getDataID().equals(dataUUID));

            if (removed) {
                // 如果列表为空，则删除玩家的条目，防止存储空列表
                if (items.isEmpty()) {
                    playerBoxes.remove(playerUUID);
                }
                setDirty(); // 标记数据已变更，确保保存
            }
            return removed; // 返回是否成功移除
        }

        return false; // 玩家没有该物品
    }


    // 向玩家发送消息
    public void sendBoxUpdateMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message));
    }
}
