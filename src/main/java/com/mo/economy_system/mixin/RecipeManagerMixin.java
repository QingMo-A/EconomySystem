package com.mo.economy_system.mixin;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.*;
import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Iterator;
import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes;
    private Map<ResourceLocation, Recipe<?>> byName;
    private boolean hasErrors;
    private final ICondition.IContext context;

    public RecipeManagerMixin(ICondition.IContext context) {
        super(GSON, "recipes");

        this.recipes = ImmutableMap.of();
        this.byName = ImmutableMap.of();
        this.context = context;
    }

    /**
     * @author
     * @reason
     */
    @Overwrite
    protected void apply(Map<ResourceLocation, JsonElement> p_44037_, ResourceManager p_44038_, ProfilerFiller p_44039_) {
        PlayerBlueprintData.clearWorkbenchRecipes();

        this.hasErrors = false;
        Map<RecipeType<?>, ImmutableMap.Builder<ResourceLocation, Recipe<?>>> map = Maps.newHashMap();
        ImmutableMap.Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();
        Iterator var6 = p_44037_.entrySet().iterator();

        while(true) {
            Map.Entry entry;
            ResourceLocation resourcelocation;
            do {
                if (!var6.hasNext()) {
                    this.recipes = (Map)map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, (p_44033_) -> {
                        return ((ImmutableMap.Builder)p_44033_.getValue()).build();
                    }));
                    this.byName = builder.build();
                    LOGGER.info("Loaded {} recipes", map.size());
                    return;
                }

                entry = (Map.Entry)var6.next();
                resourcelocation = (ResourceLocation)entry.getKey();
            } while(resourcelocation.getPath().startsWith("_"));

            try {
                if (((JsonElement)entry.getValue()).isJsonObject() && !CraftingHelper.processConditions(((JsonElement)entry.getValue()).getAsJsonObject(), "conditions", this.context)) {
                    LOGGER.debug("Skipping loading recipe {} as it's conditions were not met", resourcelocation);
                } else {
                    Recipe<?> recipe = RecipeManager.fromJson(resourcelocation, GsonHelper.convertToJsonObject((JsonElement)entry.getValue(), "top element"), this.context);
                    if (recipe == null) {
                        LOGGER.info("Skipping loading recipe {} as it's serializer returned null", resourcelocation);
                    } else {
                        // ========== 关键代码：收集工作台配方 ==========
                        // 检查是否为工作台合成配方
                        if (recipe.getType() == RecipeType.CRAFTING) {
                            // 检查是否为有效的合成配方（排除特殊配方）
                            if (isValidCraftingRecipe(resourcelocation, (JsonElement)entry.getValue(), recipe)) {
                                // 添加到PlayerBlueprintData的recipeMap
                                PlayerBlueprintData.addWorkbenchRecipe(resourcelocation, recipe);

                                // 可选：调试输出
                                if (LOGGER.isDebugEnabled()) {
                                    try {
                                        ItemStack output = recipe.getResultItem(null);
                                        if (output != null) {
                                            LOGGER.debug("Added workbench recipe: {} -> {}",
                                                    resourcelocation,
                                                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(output.getItem()));
                                        }
                                    } catch (Exception e) {
                                        // 忽略
                                    }
                                }
                            }
                        }

                        // 原版逻辑
                        ((ImmutableMap.Builder)map.computeIfAbsent(recipe.getType(), (p_44075_) -> {
                            return ImmutableMap.builder();
                        })).put(resourcelocation, recipe);
                        builder.put(resourcelocation, recipe);
                    }
                }
            } catch (JsonParseException | IllegalArgumentException var10) {
                RuntimeException jsonparseexception = var10;
                LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
            }
        }
    }

    // 添加一个辅助方法来判断是否为有效的工作台配方
    /*private boolean isValidCraftingRecipe(ResourceLocation recipeId, JsonElement element, Recipe<?> recipe) {
        // 排除特殊配方
        if (recipeId.getPath().contains("crafting_special_")) {
            return false;
        }

        // 检查配方类型（通过JSON）
        if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();
            if (json.has("type")) {
                String type = json.get("type").getAsString();
                // 只收集标准的合成配方，排除特殊合成
                if (type.equals("minecraft:crafting_shaped") ||
                        type.equals("minecraft:crafting_shapeless")) {
                    return true;
                }
            }
        }

        return false;
    }*/
    private boolean isValidCraftingRecipe(ResourceLocation recipeId, JsonElement element, Recipe<?> recipe) {
        // 排除特殊配方
        if (recipeId.getPath().contains("crafting_special_")) {
            return false;
        }

        // 检查配方类型（通过JSON）
        if (element.isJsonObject()) {
            JsonObject json = element.getAsJsonObject();

            // 1. 检查配方类型
            if (json.has("type")) {
                String type = json.get("type").getAsString();

                // 只收集标准的合成配方，排除特殊合成
                if (type.equals("minecraft:crafting_shaped") ||
                        type.equals("minecraft:crafting_shapeless")) {

                    // 2. 检查结果物品是否包含排除关键词
                    String resultItemId = getResultItemIdFromJson(json);
                    if (resultItemId != null) {
                        // 转换为小写以进行不区分大小写的比较
                        String lowerResultId = resultItemId.toLowerCase();

                        // 检查是否匹配排除关键词
                        for (String keyword : PlayerBlueprintData.EXCLUDED_ITEM_KEYWORDS) {
                            if (matchesKeyword(lowerResultId, keyword)) {
                                // 可选：记录排除日志
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug("Excluding recipe {} because result matches keyword '{}'",
                                            recipeId, keyword);
                                }
                                PlayerBlueprintData.addDefaultUnlockedItems(resultItemId);
                                return false;
                            }
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 从JSON中提取结果物品ID
     */
    private String getResultItemIdFromJson(JsonObject json) {
        try {
            // 检查是否有"result"字段
            if (json.has("result")) {
                JsonElement resultElement = json.get("result");

                if (resultElement.isJsonObject()) {
                    // 格式: {"item": "minecraft:stone", "count": 1}
                    return resultElement.getAsJsonObject().get("item").getAsString();
                } else if (resultElement.isJsonPrimitive()) {
                    // 格式: "minecraft:stone"
                    return resultElement.getAsString();
                }
            }

            // 某些配方可能使用"output"字段
            if (json.has("output")) {
                JsonElement outputElement = json.get("output");
                if (outputElement.isJsonObject()) {
                    return outputElement.getAsJsonObject().get("item").getAsString();
                } else if (outputElement.isJsonPrimitive()) {
                    return outputElement.getAsString();
                }
            }

        } catch (Exception e) {
            LOGGER.warn("Failed to extract result item ID from recipe JSON", e);
        }

        return null;
    }

    /**
     * 检查物品ID是否匹配关键词
     * 规则：
     * 1. 如果关键词包含 "*"，则作为通配符模式匹配
     * 2. 否则，进行完全匹配
     */
    private boolean matchesKeyword(String itemId, String keyword) {
        // 转换为小写确保不区分大小写
        String lowerKeyword = keyword.toLowerCase();

        if (lowerKeyword.contains("*")) {
            // 通配符模式
            return matchesWildcard(itemId, lowerKeyword);
        } else {
            // 完全匹配模式
            return itemId.equals(lowerKeyword);
        }
    }

    /**
     * 通配符匹配
     * 支持的通配符模式：
     * 1. "*_door" - 匹配所有以 "_door" 结尾的物品
     * 2. "wooden_*" - 匹配所有以 "wooden_" 开头的物品
     * 3. "*_ingot_*" - 匹配包含 "_ingot_" 的物品
     * 4. "*wood*" - 匹配包含 "wood" 的物品
     */
    private boolean matchesWildcard(String itemId, String pattern) {
        try {
            // 将通配符模式转换为正则表达式
            // 注意：需要转义正则特殊字符，但 * 除外
            String regex = pattern
                    .replace(".", "\\.")      // 转义点号
                    .replace("?", "\\?")      // 转义问号
                    .replace("+", "\\+")      // 转义加号
                    .replace("(", "\\(")      // 转义左括号
                    .replace(")", "\\)")      // 转义右括号
                    .replace("[", "\\[")      // 转义左方括号
                    .replace("]", "\\]")      // 转义右方括号
                    .replace("{", "\\{")      // 转义左花括号
                    .replace("}", "\\}")      // 转义右花括号
                    .replace("^", "\\^")      // 转义脱字符
                    .replace("$", "\\$")      // 转义美元符号
                    .replace("|", "\\|")      // 转义竖线
                    .replace("\\", "\\\\")    // 转义反斜杠
                    .replace("*", ".*");      // 将 * 转换为 .*

            // 添加锚定以确保完全匹配
            if (!pattern.startsWith("*")) {
                regex = "^" + regex; // 如果模式不以 * 开头，则从开头匹配
            }
            if (!pattern.endsWith("*")) {
                regex = regex + "$"; // 如果模式不以 * 结尾，则匹配到结尾
            }

            return itemId.matches(regex);
        } catch (Exception e) {
            LOGGER.warn("Failed to match wildcard pattern '{}' for item '{}'", pattern, itemId, e);
            return false;
        }
    }
}
