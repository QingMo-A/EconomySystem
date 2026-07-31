package com.mo.economy_system.core.territory_system;

import java.util.UUID;

public enum TerritoryPermissionLevel {
    OWNER_ONLY("仅领主"),
    MEMBERS("所有成员"),
    EVERYONE("所有人");

    private final String displayName;

    TerritoryPermissionLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TerritoryPermissionLevel next() {
        return switch (this) {
            case OWNER_ONLY -> MEMBERS;
            case MEMBERS -> EVERYONE;
            case EVERYONE -> OWNER_ONLY;
        };
    }

    public boolean allows(Territory territory, UUID playerUUID) {
        return switch (this) {
            case OWNER_ONLY -> territory.isOwner(playerUUID);
            case MEMBERS -> territory.isOwner(playerUUID) || territory.hasPermission(playerUUID);
            case EVERYONE -> true;
        };
    }
}
