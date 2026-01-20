package com.mo.economy_system.core.playerattributes_system.death;

import com.mo.economy_system.EconomySystem;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 死亡物品存储工具类
 * 用于在玩家死亡时暂存物品，等待玩家选择是否保留
 */
public class DeathItemStorage {

    /**
     * 存储待掉落的物品
     * Key: 玩家UUID, Value: 存储的物品列表
     */
    private static final Map<UUID, StoredItems> STORED_DROPS = new HashMap<>();

    /**
     * 存储玩家物品栏
     */
    public static void storePlayerInventory(Player player) {
        UUID uuid = player.getUUID();
        Inventory inventory = player.getInventory();

        // 存储主物品栏、盔甲栏和副手栏
        NonNullList<ItemStack> mainItems = NonNullList.create();
        NonNullList<ItemStack> armorItems = NonNullList.create();
        NonNullList<ItemStack> offhandItems = NonNullList.create();

        // 复制主物品栏
        for (int i = 0; i < inventory.items.size(); i++) {
            mainItems.add(inventory.items.get(i).copy());
        }

        // 复制盔甲栏
        for (int i = 0; i < inventory.armor.size(); i++) {
            armorItems.add(inventory.armor.get(i).copy());
        }

        // 复制副手栏
        for (int i = 0; i < inventory.offhand.size(); i++) {
            offhandItems.add(inventory.offhand.get(i).copy());
        }

        // 存储到 map
        STORED_DROPS.put(uuid, new StoredItems(mainItems, armorItems, offhandItems));

        EconomySystem.LOGGER.info("玩家 {} 的物品已被暂存，等待复活选择", player.getScoreboardName());
    }

    /**
     * 检查玩家是否有存储的物品
     */
    public static boolean hasStoredItems(UUID uuid) {
        return STORED_DROPS.containsKey(uuid);
    }

    /**
     * 掉落存储的物品
     */
    public static void dropStoredItems(Player player) {
        UUID uuid = player.getUUID();
        StoredItems stored = STORED_DROPS.remove(uuid);

        if (stored == null) {
            return;
        }

        EconomySystem.LOGGER.info("玩家 {} 选择正常复活，掉落存储的物品", player.getScoreboardName());

        // 清空当前物品栏
        Inventory inventory = player.getInventory();
        inventory.items.clear();
        inventory.armor.clear();
        inventory.offhand.clear();

        // 掉落物品
        dropItems(player, stored.mainItems());
        dropItems(player, stored.armorItems());
        dropItems(player, stored.offhandItems());
    }

    /**
     * 保留存储的物品
     */
    public static void keepStoredItems(Player player) {
        UUID uuid = player.getUUID();
        StoredItems stored = STORED_DROPS.remove(uuid);

        if (stored == null) {
            return;
        }

        EconomySystem.LOGGER.info("玩家 {} 选择保留物品复活", player.getScoreboardName());

        // 物品已经在玩家身上，不需要做任何事
    }

    /**
     * 清除存储的物品
     */
    public static void clearStoredItems(UUID uuid) {
        STORED_DROPS.remove(uuid);
    }

    /**
     * 掉落物品列表
     */
    private static void dropItems(Player player, NonNullList<ItemStack> items) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                player.drop(stack, true, false);
            }
        }
    }

    /**
     * 存储的物品数据
     */
    public record StoredItems(
            NonNullList<ItemStack> mainItems,
            NonNullList<ItemStack> armorItems,
            NonNullList<ItemStack> offhandItems
    ) {}
}
