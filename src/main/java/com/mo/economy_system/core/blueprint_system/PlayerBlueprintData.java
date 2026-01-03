package com.mo.economy_system.core.blueprint_system;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

public class PlayerBlueprintData extends SavedData {
    // 存储可制作物品的NBT键名
    private static final String UNLOCKED_ITEMS_KEY = "unlocked_items";

    private static final String BLUEPRINT_ITEMS_KEY = "blueprint_items";

    /**
     * 解锁一个物品的制作权限
     */
    public static void unlockItem(Player player, String itemId) {
        CompoundTag playerData = player.getPersistentData();
        ListTag unlockedList;

        // 获取现有的解锁列表，如果没有则创建新列表
        if (playerData.contains(UNLOCKED_ITEMS_KEY, 9)) { // 9 对应 TAG_List
            unlockedList = playerData.getList(UNLOCKED_ITEMS_KEY, 8); // 8 对应 TAG_String
        } else {
            unlockedList = new ListTag();
        }

        // 检查是否已解锁
        boolean alreadyUnlocked = false;
        for (int i = 0; i < unlockedList.size(); i++) {
            if (itemId.equals(unlockedList.getString(i))) {
                alreadyUnlocked = true;
                break;
            }
        }

        // 如果未解锁，则添加到列表
        if (!alreadyUnlocked) {
            unlockedList.add(StringTag.valueOf(itemId));
            playerData.put(UNLOCKED_ITEMS_KEY, unlockedList);
        }
    }

    /**
     * 检查玩家是否可以制作某个物品
     */
    public static boolean canCraftItem(Player player, String itemId) {
        // 先检查默认解锁的物品（基础物品）
        if (isDefaultUnlocked(itemId)) {
            return true;
        }

        CompoundTag playerData = player.getPersistentData();

        // 如果连数据都没有，检查默认列表
        if (!playerData.contains(UNLOCKED_ITEMS_KEY, 9)) {
            return false;
        }

        ListTag unlockedList = playerData.getList(UNLOCKED_ITEMS_KEY, 8);

        // 检查物品ID是否在列表中
        for (int i = 0; i < unlockedList.size(); i++) {
            if (itemId.equals(unlockedList.getString(i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查玩家是否可以制作某个物品（通过ItemStack）
     */
    public static boolean canCraftItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) return true; // 空物品堆总是允许

        String itemId = ForgeRegistries.ITEMS.getKey(stack.getItem()).toString();
        return canCraftItem(player, itemId);
    }

    /**
     * 为玩家添加蓝图物品（用于创造模式标签）
     */
    public static void addBlueprintItem(Player player, String blueprintItemId) {
        CompoundTag playerData = player.getPersistentData();
        ListTag blueprintList;

        if (playerData.contains(BLUEPRINT_ITEMS_KEY, 9)) {
            blueprintList = playerData.getList(BLUEPRINT_ITEMS_KEY, 8);
        } else {
            blueprintList = new ListTag();
        }

        boolean alreadyAdded = false;
        for (int i = 0; i < blueprintList.size(); i++) {
            if (blueprintItemId.equals(blueprintList.getString(i))) {
                alreadyAdded = true;
                break;
            }
        }

        if (!alreadyAdded) {
            blueprintList.add(StringTag.valueOf(blueprintItemId));
            playerData.put(BLUEPRINT_ITEMS_KEY, blueprintList);
        }
    }

    /**
     * 获取玩家所有蓝图物品ID
     */
    public static Set<String> getAllBlueprintItems(Player player) {
        Set<String> blueprints = new HashSet<>();
        CompoundTag playerData = player.getPersistentData();

        if (playerData.contains(BLUEPRINT_ITEMS_KEY, 9)) {
            ListTag blueprintList = playerData.getList(BLUEPRINT_ITEMS_KEY, 8);

            for (int i = 0; i < blueprintList.size(); i++) {
                blueprints.add(blueprintList.getString(i));
            }
        }

        return blueprints;
    }

    /**
     * 获取玩家所有可以制作的物品
     */
    public static Set<String> getAllUnlockedItems(Player player) {
        Set<String> items = new HashSet<>();
        CompoundTag playerData = player.getPersistentData();

        if (playerData.contains(UNLOCKED_ITEMS_KEY, 9)) {
            ListTag unlockedList = playerData.getList(UNLOCKED_ITEMS_KEY, 8);

            for (int i = 0; i < unlockedList.size(); i++) {
                items.add(unlockedList.getString(i));
            }
        }

        // 添加默认解锁的物品
        items.addAll(getDefaultUnlockedItems());

        return items;
    }

    /**
     * 默认解锁的物品列表（基础物品，不需要蓝图）
     */
    public static Set<String> getDefaultUnlockedItems() {
        Set<String> defaultItems = new HashSet<>();

        // 添加原版基础物品
        defaultItems.add("minecraft:stick");
        defaultItems.add("minecraft:wooden_planks");
        defaultItems.add("minecraft:torch");
        defaultItems.add("minecraft:crafting_table");
        defaultItems.add("minecraft:wooden_axe");
        defaultItems.add("minecraft:wooden_shovel");

        return defaultItems;
    }

    /**
     * 检查物品是否默认解锁
     */
    public static boolean isDefaultUnlocked(String itemId) {
        return getDefaultUnlockedItems().contains(itemId);
    }

    /**
     * 清除玩家的所有解锁物品（用于重置）
     */
    public static void clearAllUnlocks(Player player) {
        CompoundTag playerData = player.getPersistentData();
        playerData.remove(UNLOCKED_ITEMS_KEY);
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag) {
        return null;
    }
}
