package com.mo.economy_system.core.territory_system;

public enum TerritoryPermissionAction {
    PLACE_BLOCK("放置方块"),
    BREAK_BLOCK("破坏方块"),
    USE_ITEM("使用物品"),
    INTERACT_BLOCK("交互方块"),
    OPEN_CONTAINER("打开容器");

    private final String displayName;

    TerritoryPermissionAction(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
