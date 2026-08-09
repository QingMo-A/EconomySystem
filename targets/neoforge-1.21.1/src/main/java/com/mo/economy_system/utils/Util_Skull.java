package com.mo.economy_system.utils;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

public class Util_Skull {

    /**
     * 创建一个带有玩家皮肤的头颅 ItemStack
     *
     * @param playerUUID  玩家 UUID
     * @param playerName  玩家名称（可选，用于显示头颅名称）
     * @return 玩家皮肤的头颅 ItemStack
     */
    public static ItemStack createPlayerHead(UUID playerUUID, String playerName) {
        // 创建一个玩家头颅的 ItemStack
        ItemStack playerHead = new ItemStack(Items.PLAYER_HEAD);

        // 获取头颅的 NBT 数据
        CompoundTag tag = new CompoundTag();
        CompoundTag skullOwnerTag = new CompoundTag();

        // 设置玩家的 UUID 和名称
        skullOwnerTag.putString("Name", playerName != null ? playerName : "");
        skullOwnerTag.putUUID("Id", playerUUID);

        // 将 "SkullOwner" 数据添加到 ItemStack 的 NBT
        tag.put("SkullOwner", skullOwnerTag);
        com.mo.economy_system.utils.ItemStackDataHelper.setTag(playerHead, tag);

        return playerHead;
    }
}
