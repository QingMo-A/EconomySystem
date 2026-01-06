package com.mo.economy_system.core.blueprint_system;

import com.mo.economy_system.item.items.BlueprintItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerBlueprintData {
    // 存储可制作物品的NBT键名
    private static final String UNLOCKED_ITEMS_KEY = "unlocked_items";

    private static final String BLUEPRINT_ITEMS_KEY = "blueprint_items";

    private static List<ItemStack> itemStackList = new ArrayList<>();

    private static List<String> itemsRequiringBlueprint = new ArrayList<>();

    // 存储所有工作台配方的静态Map
    private static final Map<ResourceLocation, Recipe<?>> WORKBENCH_RECIPE_MAP = new ConcurrentHashMap<>();

    // 存储需要蓝图的物品ID集合
    private static final Set<ResourceLocation> ITEMS_REQUIRING_BLUEPRINT = new HashSet<>();

    private static Set<String> defaultItems = new HashSet<>();

    // 定义一个需要排除的物品关键字集合
    public static final Set<String> EXCLUDED_ITEM_KEYWORDS = new HashSet<>(Arrays.asList(
            "*_wood",
            "*_planks",
            "*_log",
            "*_terracotta",
            "*_bed",
            "*_candle",
            "*_glass_pane",
            "*_glass",
            "*_sign",
            "*_carpet",
            "*_stairs",
            "*_slab",
            "raw_*",
            "*_wool",
            "*_copper",
            "*_block",
            "*_button",
            "*_fence_gate",
            "*_door",
            "*_boat",
            "*_banner",
            "*_fence",
            "*_ingot",
            "*_dye",
            "*_plate",
            "*_concrete_powder",
            "minecraft:snow",
            "*_trapdoor"
    ));



    /**
     * 清空工作台配方Map
     */
    public static void clearWorkbenchRecipes() {
        WORKBENCH_RECIPE_MAP.clear();
        ITEMS_REQUIRING_BLUEPRINT.clear();
    }

    /**
     * 添加工作台配方到Map
     */
    public static void addWorkbenchRecipe(ResourceLocation recipeId, Recipe<?> recipe) {
        WORKBENCH_RECIPE_MAP.put(recipeId, recipe);
        // 获取结果物品ID并添加到集合中
        ResourceLocation outputItemId = getRecipeOutputItemId(recipe);
        if (outputItemId != null) {
            ITEMS_REQUIRING_BLUEPRINT.add(outputItemId);

            // 调试输出
            System.out.println("[Blueprint] 添加配方: " + recipeId + " -> 物品: " + outputItemId);
        }
    }

    /**
     * 从Recipe中提取输出物品ID
     */
    private static ResourceLocation getRecipeOutputItemId(Recipe<?> recipe) {
        try {
            // 尝试获取配方的输出
            ItemStack output = recipe.getResultItem(null); // 注意：这里需要RegistryAccess
            if (output != null && !output.isEmpty()) {
                return ForgeRegistries.ITEMS.getKey(output.getItem());
            }
        } catch (Exception e) {
            // 如果上面的方法失败，尝试备用方案
            return getRecipeOutputFromJson(recipe);
        }
        return null;
    }

    /**
     * 备用方案：通过其他方式获取输出物品ID
     */
    private static ResourceLocation getRecipeOutputFromJson(Recipe<?> recipe) {
        try {
            // 方法1：尝试通过toString解析
            String recipeString = recipe.toString();
            if (recipeString.contains("result=")) {
                // 简化解析逻辑
                // 实际实现可能需要更复杂的解析
            }

            // 方法2：对于特定类型的配方，可以尝试获取
            if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shapedRecipe) {
                ItemStack output = shapedRecipe.getResultItem(null);
                if (output != null) {
                    return ForgeRegistries.ITEMS.getKey(output.getItem());
                }
            } else if (recipe instanceof net.minecraft.world.item.crafting.ShapelessRecipe shapelessRecipe) {
                ItemStack output = shapelessRecipe.getResultItem(null);
                if (output != null) {
                    return ForgeRegistries.ITEMS.getKey(output.getItem());
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 获取所有工作台配方的Map
     */
    public static Map<ResourceLocation, Recipe<?>> getWorkbenchRecipes() {
        return WORKBENCH_RECIPE_MAP;
    }

    /**
     * 根据物品ID获取其工作台配方
     */
    public static Recipe<?> getRecipeForItem(ResourceLocation itemId) {
        // 遍历所有配方，查找输出为指定物品的配方
        for (Recipe<?> recipe : WORKBENCH_RECIPE_MAP.values()) {
            try {
                // 获取配方的输出物品
                net.minecraft.world.item.ItemStack output = recipe.getResultItem(null); // 注意：需要RegistryAccess
                if (output != null && !output.isEmpty()) {
                    ResourceLocation outputItemId = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output.getItem());
                    if (itemId.equals(outputItemId)) {
                        return recipe;
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    private static final Set<String> SPECIAL_RECIPE_BLACKLIST = Set.of(
            // 盔甲染色相关
            "minecraft:crafting_special_armordye",
            "minecraft:crafting_special_bannerduplicate",
            "minecraft:crafting_special_shielddecoration",
            "minecraft:crafting_special_shulkerboxcoloring",

            // 工具修复
            "minecraft:crafting_special_repairitem",

            // 烟花相关
            "minecraft:crafting_special_firework_star",
            "minecraft:crafting_special_firework_rocket",

            // 药水箭
            "minecraft:crafting_special_tippedarrow",

            // 地图相关
            "minecraft:crafting_special_mapcloning",
            "minecraft:crafting_special_mapextending",

            // 其他特殊合成
            "minecraft:crafting_special_suspiciousstew",
            "minecraft:crafting_special_bookcloning",

            // 切石机配方（不是工作台合成）
            "minecraft:stonecutting"
    );



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

    public static void initAllBlueprintItems() {
        // 定义需要蓝图解锁的物品列表
        // 获取所有已注册的物品
        for (ResourceLocation resourceLocation : ITEMS_REQUIRING_BLUEPRINT) {
            if (resourceLocation == null) continue;

            String itemId = resourceLocation.toString();

            // 检查是否需要蓝图
            if (!(PlayerBlueprintData.isDefaultUnlocked(itemId))) {
                itemsRequiringBlueprint.add(itemId);
            }
        }

        for (String itemId : itemsRequiringBlueprint) {
            itemStackList.add(BlueprintItem.createBlueprint(itemId));
        }
    }

    public static List<ItemStack> getAllBlueprintItems() {
        return itemStackList;
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

    public static void addDefaultUnlockedItems(String itemId) {
        defaultItems.add(itemId);
    }

    /**
     * 默认解锁的物品列表（基础物品，不需要蓝图）
     */
    public static Set<String> getDefaultUnlockedItems() {
        // 添加原版基础物品
        defaultItems.add("minecraft:stick");
        defaultItems.add("minecraft:oak_planks");
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
}
