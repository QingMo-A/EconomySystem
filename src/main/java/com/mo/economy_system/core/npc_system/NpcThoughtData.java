package com.mo.economy_system.core.npc_system;

public class NpcThoughtData {
    private String thoughtText = "";
    private String wantedItemId = "";
    private String wantedAction = "";
    private int favorabilityReward = 20;
    private double returnGiftChance = 0.0D;

    public NpcThoughtData() {
    }

    public NpcThoughtData(String thoughtText, String wantedItemId, String wantedAction, int favorabilityReward, double returnGiftChance) {
        this.thoughtText = thoughtText;
        this.wantedItemId = wantedItemId;
        this.wantedAction = wantedAction;
        this.favorabilityReward = favorabilityReward;
        this.returnGiftChance = returnGiftChance;
    }

    public String getThoughtText() {
        return thoughtText == null ? "" : thoughtText;
    }

    public String getWantedItemId() {
        return wantedItemId == null ? "" : wantedItemId;
    }

    public String getWantedAction() {
        return wantedAction == null ? "" : wantedAction;
    }

    public int getFavorabilityReward() {
        return favorabilityReward;
    }

    public double getReturnGiftChance() {
        return returnGiftChance;
    }
}
