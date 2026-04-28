package com.mo.economy_system.core.npc_system;

import java.util.UUID;

public class NpcRelationData {
    private int npcId;
    private UUID targetPlayerUUID;
    private int favorability;
    private RelationType relationType = RelationType.STRANGER;
    private long lastInteractionTime;

    public NpcRelationData() {
    }

    public NpcRelationData(int npcId, UUID targetPlayerUUID) {
        this.npcId = npcId;
        this.targetPlayerUUID = targetPlayerUUID;
        refreshRelationType();
    }

    public int getNpcId() {
        return npcId;
    }

    public UUID getTargetPlayerUUID() {
        return targetPlayerUUID;
    }

    public int getFavorability() {
        return favorability;
    }

    public void setFavorability(int favorability) {
        this.favorability = Math.max(-1000, Math.min(1000, favorability));
        refreshRelationType();
    }

    public void addFavorability(int amount) {
        setFavorability(this.favorability + amount);
        this.lastInteractionTime = System.currentTimeMillis();
    }

    public RelationType getRelationType() {
        refreshRelationType();
        return relationType;
    }

    public long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public void refreshRelationType() {
        this.relationType = RelationType.fromFavorability(favorability);
    }

    public enum RelationType {
        HOSTILE(-1000, "敌对"),
        STRANGER(-99, "陌生"),
        FAMILIAR(100, "熟悉"),
        FRIEND(300, "朋友"),
        TRUSTED(600, "信任"),
        CLOSE(850, "亲近");

        private final int threshold;
        private final String displayName;

        RelationType(int threshold, String displayName) {
            this.threshold = threshold;
            this.displayName = displayName;
        }

        public int getThreshold() {
            return threshold;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static RelationType fromFavorability(int favorability) {
            if (favorability < STRANGER.threshold) {
                return HOSTILE;
            }
            if (favorability >= CLOSE.threshold) {
                return CLOSE;
            }
            if (favorability >= TRUSTED.threshold) {
                return TRUSTED;
            }
            if (favorability >= FRIEND.threshold) {
                return FRIEND;
            }
            if (favorability >= FAMILIAR.threshold) {
                return FAMILIAR;
            }
            return STRANGER;
        }
    }
}
