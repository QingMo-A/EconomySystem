package com.mo.economy_system.core.playerattributes_system.death;

import com.mo.economy_system.EconomySystem;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 死亡物品存储工具类
 * 玩家默认保留物品，选择普通死亡时在死亡点掉落所有物品
 */
public class DeathItemStorage {

    /**
     * 存储待处理的物品
     * Key: 玩家UUID, Value: 存储的物品和死亡位置
     */
    private static final Map<UUID, StoredItems> STORED_DROPS = new HashMap<>();

    /**
     * 存储玩家物品栏和死亡位置
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

        // 存储死亡位置信息
        String dimension = player.level().dimension().location().toString();
        double deathX = player.getX();
        double deathY = player.getY();
        double deathZ = player.getZ();

        // 存储到 map
        STORED_DROPS.put(uuid, new StoredItems(mainItems, armorItems, offhandItems, dimension, deathX, deathY, deathZ));

        EconomySystem.LOGGER.info("玩家 {} 的物品已暂存，死亡位置: {} ({}, {}, {})",
                player.getScoreboardName(), dimension, (int)deathX, (int)deathY, (int)deathZ);
    }

    /**
     * 检查玩家是否有存储的物品
     */
    public static boolean hasStoredItems(UUID uuid) {
        return STORED_DROPS.containsKey(uuid);
    }

    /**
     * 掉落存储的物品（在死亡点）
     */
    public static void dropStoredItems(Player player) {
        UUID uuid = player.getUUID();
        StoredItems stored = STORED_DROPS.remove(uuid);

        if (stored == null) {
            return;
        }

        EconomySystem.LOGGER.info("玩家 {} 选择普通死亡，在死亡点掉落所有物品", player.getScoreboardName());

        // 清空当前物品栏
        Inventory inventory = player.getInventory();
        inventory.items.clear();
        inventory.armor.clear();
        inventory.offhand.clear();
        inventory.setChanged();

        // 在死亡位置掉落物品
        dropItemsAtDeathLocation(player, stored);
    }

    /**
     * 保留存储的物品（恢复到玩家物品栏）
     */
    public static void keepStoredItems(Player player) {
        UUID uuid = player.getUUID();
        StoredItems stored = STORED_DROPS.remove(uuid);

        if (stored == null) {
            return;
        }

        EconomySystem.LOGGER.info("玩家 {} 选择保留物品", player.getScoreboardName());

        // 恢复物品到玩家物品栏
        Inventory inventory = player.getInventory();

        // 恢复主物品栏
        for (int i = 0; i < stored.mainItems().size() && i < inventory.items.size(); i++) {
            inventory.items.set(i, stored.mainItems().get(i).copy());
        }

        // 恢复盔甲栏
        for (int i = 0; i < stored.armorItems().size() && i < inventory.armor.size(); i++) {
            inventory.armor.set(i, stored.armorItems().get(i).copy());
        }

        // 恢复副手栏
        for (int i = 0; i < stored.offhandItems().size() && i < inventory.offhand.size(); i++) {
            inventory.offhand.set(i, stored.offhandItems().get(i).copy());
        }

        // 同步到客户端
        inventory.setChanged();
        player.containerMenu.broadcastChanges();
    }

    /**
     * 清除存储的物品
     */
    public static void clearStoredItems(UUID uuid) {
        STORED_DROPS.remove(uuid);
    }

    /**
     * 在死亡位置掉落物品
     */
    private static void dropItemsAtDeathLocation(Player player, StoredItems stored) {
        Level level = player.level();

        // 检查死亡位置的维度是否与当前维度相同
        String currentDimension = level.dimension().location().toString();
        if (!currentDimension.equals(stored.dimension())) {
            EconomySystem.LOGGER.warn("死亡维度({})与当前维度({})不同，物品将在当前位置掉落",
                    stored.dimension(), currentDimension);
            // 维度不同，在当前位置掉落
            dropItemsAt(player.level(), player.getX(), player.getY(), player.getZ(), stored);
            return;
        }

        // 在死亡位置掉落
        dropItemsAt(level, stored.deathX(), stored.deathY(), stored.deathZ(), stored);
    }

    /**
     * 在指定位置掉落物品列表
     */
    private static void dropItemsAt(Level level, double x, double y, double z, StoredItems stored) {
        dropItemList(level, x, y, z, stored.mainItems());
        dropItemList(level, x, y, z, stored.armorItems());
        dropItemList(level, x, y, z, stored.offhandItems());
    }

    /**
     * 在指定位置掉落物品列表
     */
    private static void dropItemList(Level level, double x, double y, double z, NonNullList<ItemStack> items) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                spawnItemEntity(level, x, y, z, stack);
            }
        }
    }

    /**
     * 在指定位置生成物品实体
     */
    private static void spawnItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        if (level instanceof ServerLevel serverLevel) {
            // 添加一点随机偏移，防止物品堆叠在一起
            double offsetX = (level.random.nextDouble() - 0.5) * 0.5;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.5;

            ItemEntity itemEntity = new ItemEntity(EntityType.ITEM, level);
            itemEntity.setItem(stack.copy());
            itemEntity.setPos(x + offsetX, y, z + offsetZ);
            itemEntity.setDeltaMovement(
                    (level.random.nextDouble() - 0.5) * 0.1,
                    0.2,
                    (level.random.nextDouble() - 0.5) * 0.1
            );
            itemEntity.setPickUpDelay(10); // 10 tick 后才能捡起

            serverLevel.addFreshEntity(itemEntity);
        }
    }

    /**
     * 存储的物品数据（包含死亡位置）
     */
    public record StoredItems(
            NonNullList<ItemStack> mainItems,
            NonNullList<ItemStack> armorItems,
            NonNullList<ItemStack> offhandItems,
            String dimension,      // 死亡维度
            double deathX,         // 死亡 X 坐标
            double deathY,         // 死亡 Y 坐标
            double deathZ          // 死亡 Z 坐标
    ) {}
}