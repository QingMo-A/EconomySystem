package com.mo.economy_system.core.npc_system;

import java.util.ArrayList;
import java.util.List;

public class NpcDialogueViewData {
    private final int npcId;
    private final int entityId;
    private final String npcName;
    private final String npcIntroduction;
    private final String npcGender;
    private final String npcProfession;
    private final int storyStageId;
    private final List<String> dialogues;
    private final String thoughtText;
    private final String wantedItemId;
    private final int favorability;
    private final String relationName;
    private final List<String> availableActions;

    public NpcDialogueViewData(
            int npcId,
            int entityId,
            String npcName,
            String npcIntroduction,
            String npcGender,
            String npcProfession,
            int storyStageId,
            List<String> dialogues,
            String thoughtText,
            String wantedItemId,
            int favorability,
            String relationName,
            List<String> availableActions
    ) {
        this.npcId = npcId;
        this.entityId = entityId;
        this.npcName = npcName == null ? "" : npcName;
        this.npcIntroduction = npcIntroduction == null ? "" : npcIntroduction;
        this.npcGender = npcGender == null ? "" : npcGender;
        this.npcProfession = npcProfession == null ? "" : npcProfession;
        this.storyStageId = storyStageId;
        this.dialogues = dialogues == null ? new ArrayList<>() : new ArrayList<>(dialogues);
        this.thoughtText = thoughtText == null ? "" : thoughtText;
        this.wantedItemId = wantedItemId == null ? "" : wantedItemId;
        this.favorability = favorability;
        this.relationName = relationName == null ? "" : relationName;
        this.availableActions = availableActions == null ? new ArrayList<>() : new ArrayList<>(availableActions);
    }

    public int getNpcId() {
        return npcId;
    }

    public int getEntityId() {
        return entityId;
    }

    public String getNpcName() {
        return npcName;
    }

    public String getNpcIntroduction() {
        return npcIntroduction;
    }

    public String getNpcGender() {
        return npcGender;
    }

    public String getNpcProfession() {
        return npcProfession;
    }

    public int getStoryStageId() {
        return storyStageId;
    }

    public List<String> getDialogues() {
        return dialogues;
    }

    public String getThoughtText() {
        return thoughtText;
    }

    public String getWantedItemId() {
        return wantedItemId;
    }

    public int getFavorability() {
        return favorability;
    }

    public String getRelationName() {
        return relationName;
    }

    public List<String> getAvailableActions() {
        return availableActions;
    }
}
