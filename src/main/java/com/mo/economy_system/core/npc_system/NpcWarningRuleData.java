package com.mo.economy_system.core.npc_system;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NpcWarningRuleData {
    private Set<UUID> playerWhitelist = new HashSet<>();
    private Set<UUID> playerBlacklist = new HashSet<>();
    private Set<String> entityWhitelist = new HashSet<>();
    private Set<String> entityBlacklist = new HashSet<>();
    private Set<String> itemWhitelist = new HashSet<>();
    private Set<String> itemBlacklist = new HashSet<>();

    public Set<UUID> getPlayerWhitelist() {
        if (playerWhitelist == null) {
            playerWhitelist = new HashSet<>();
        }
        return playerWhitelist;
    }

    public Set<UUID> getPlayerBlacklist() {
        if (playerBlacklist == null) {
            playerBlacklist = new HashSet<>();
        }
        return playerBlacklist;
    }

    public Set<String> getEntityWhitelist() {
        if (entityWhitelist == null) {
            entityWhitelist = new HashSet<>();
        }
        return entityWhitelist;
    }

    public Set<String> getEntityBlacklist() {
        if (entityBlacklist == null) {
            entityBlacklist = new HashSet<>();
        }
        return entityBlacklist;
    }

    public Set<String> getItemWhitelist() {
        if (itemWhitelist == null) {
            itemWhitelist = new HashSet<>();
        }
        return itemWhitelist;
    }

    public Set<String> getItemBlacklist() {
        if (itemBlacklist == null) {
            itemBlacklist = new HashSet<>();
        }
        return itemBlacklist;
    }
}
