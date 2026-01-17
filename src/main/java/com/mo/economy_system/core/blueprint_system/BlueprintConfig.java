package com.mo.economy_system.core.blueprint_system;

import com.google.gson.annotations.SerializedName;

/**
 * 蓝图配置类
 * 用于定义哪些物品需要蓝图才能制作
 */
public class BlueprintConfig {
    /**
     * 默认解锁的物品ID列表（不需要蓝图的物品）
     */
    @SerializedName("default_unlocked_items")
    private final java.util.List<String> defaultUnlockedItems;

    /**
     * 排除关键字（匹配这些关键字的物品不需要蓝图）
     */
    @SerializedName("excluded_keywords")
    private final java.util.List<String> excludedKeywords;

    public BlueprintConfig() {
        this.defaultUnlockedItems = new java.util.ArrayList<>();
        this.excludedKeywords = new java.util.ArrayList<>();
    }

    public java.util.List<String> getDefaultUnlockedItems() {
        return defaultUnlockedItems;
    }

    public java.util.List<String> getExcludedKeywords() {
        return excludedKeywords;
    }

    public void addDefaultUnlockedItem(String itemId) {
        defaultUnlockedItems.add(itemId);
    }

    public void addExcludedKeyword(String keyword) {
        excludedKeywords.add(keyword);
    }
}