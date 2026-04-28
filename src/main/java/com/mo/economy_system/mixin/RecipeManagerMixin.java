package com.mo.economy_system.mixin;

import com.google.gson.JsonElement;
import com.mo.economy_system.core.blueprint_system.PlayerBlueprintData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

    @Shadow
    private Map<ResourceLocation, RecipeHolder<?>> byName;

    /**
     * 在配方加载开始时清空工作台配方缓存
     */
    @Inject(
            method = "apply",
            at = @At("HEAD")
    )
    private void economy_system$clearWorkbenchRecipes(Map<ResourceLocation, JsonElement> recipeList, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        PlayerBlueprintData.clearWorkbenchRecipes();
    }

    /**
     * 在配方加载完成后收集所有工作台配方
     */
    @Inject(
            method = "apply",
            at = @At("RETURN")
    )
    private void economy_system$collectWorkbenchRecipes(Map<ResourceLocation, JsonElement> recipeList, ResourceManager resourceManager, ProfilerFiller profiler, CallbackInfo ci) {
        // 遍历所有配方，收集工作台配方
        for (Map.Entry<ResourceLocation, RecipeHolder<?>> entry : this.byName.entrySet()) {
            ResourceLocation recipeId = entry.getKey();
            Recipe<?> recipe = entry.getValue().value();

            // 检查是否为工作台合成配方
            if (recipe.getType() == RecipeType.CRAFTING) {
                // 检查是否为有效的合成配方（排除特殊配方）
                if (isValidCraftingRecipe(recipeId, recipe)) {
                    // 添加到PlayerBlueprintData的recipeMap
                    PlayerBlueprintData.addWorkbenchRecipe(recipeId, recipe);
                }
            }
        }
    }

    /**
     * 判断是否为有效的工作台配方
     */
    private boolean isValidCraftingRecipe(ResourceLocation recipeId, Recipe<?> recipe) {
        // 排除特殊配方
        if (recipeId.getPath().contains("crafting_special_")) {
            return false;
        }

        // 获取配方结果物品ID
        String resultItemId = getResultItemIdFromRecipe(recipe);

        if (resultItemId != null) {
            // 转换为小写以进行不区分大小写的比较
            String lowerResultId = resultItemId.toLowerCase();

            // 检查是否匹配排除关键词
            for (String keyword : PlayerBlueprintData.EXCLUDED_ITEM_KEYWORDS) {
                if (matchesKeyword(lowerResultId, keyword)) {
                    // 添加到默认解锁物品列表
                    PlayerBlueprintData.addDefaultUnlockedItems(resultItemId);
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    /**
     * 从配方中提取结果物品ID
     */
    private String getResultItemIdFromRecipe(Recipe<?> recipe) {
        try {
            // 尝试从配方结果获取物品ID
            // 使用空RegistryAccess（仅用于获取物品ID，不需要完整访问）
            RegistryAccess registryAccess = RegistryAccess.EMPTY;
            if (recipe.getResultItem(registryAccess) != null &&
                recipe.getResultItem(registryAccess).getItem() != null) {
                return recipe.getResultItem(registryAccess).getItem().toString();
            }
        } catch (Exception e) {
            // 忽略错误，返回null
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
            return false;
        }
    }
}
