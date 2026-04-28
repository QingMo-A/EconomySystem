package com.mo.economy_system.core.npc_system;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NpcData {
    private int npcId;
    private String npcName = "";
    private String npcIntroduction = "";
    private String npcGender = "";
    @Deprecated
    private String npcFeamale;
    private String npcProfession = "";
    private int storyStageId;
    private List<String> dialogues = new ArrayList<>();
    private Map<String, Integer> actionFavorabilityRequirements = new HashMap<>();
    private NpcThoughtData currentThought;
    private NpcWarningRuleData warningRules = new NpcWarningRuleData();

    public NpcData() {
    }

    public NpcData(int npcId, String npcName, String npcIntroduction, String npcGender, String npcProfession) {
        this.npcId = npcId;
        this.npcName = npcName;
        this.npcIntroduction = npcIntroduction;
        this.npcGender = npcGender;
        this.npcProfession = npcProfession;
    }

    public int getNpcId() {
        return npcId;
    }

    public void setNpcId(int npcId) {
        this.npcId = npcId;
    }

    public String getNpcName() {
        return npcName == null ? "" : npcName;
    }

    public void setNpcName(String npcName) {
        this.npcName = npcName;
    }

    public String getNpcIntroduction() {
        return npcIntroduction == null ? "" : npcIntroduction;
    }

    public void setNpcIntroduction(String npcIntroduction) {
        this.npcIntroduction = npcIntroduction;
    }

    public String getNpcGender() {
        if (npcGender != null && !npcGender.isEmpty()) {
            return npcGender;
        }
        return npcFeamale == null ? "" : npcFeamale;
    }

    public void setNpcGender(String npcGender) {
        this.npcGender = npcGender;
    }

    public String getNpcProfession() {
        return npcProfession == null ? "" : npcProfession;
    }

    public void setNpcProfession(String npcProfession) {
        this.npcProfession = npcProfession;
    }

    public int getStoryStageId() {
        return storyStageId;
    }

    public void setStoryStageId(int storyStageId) {
        this.storyStageId = storyStageId;
    }

    public List<String> getDialogues() {
        if (dialogues == null) {
            dialogues = new ArrayList<>();
        }
        return dialogues;
    }

    public void setDialogues(List<String> dialogues) {
        this.dialogues = dialogues;
    }

    public Map<String, Integer> getActionFavorabilityRequirements() {
        if (actionFavorabilityRequirements == null) {
            actionFavorabilityRequirements = new HashMap<>();
        }
        return actionFavorabilityRequirements;
    }

    public void setActionFavorabilityRequirements(Map<String, Integer> actionFavorabilityRequirements) {
        this.actionFavorabilityRequirements = actionFavorabilityRequirements;
    }

    public NpcThoughtData getCurrentThought() {
        return currentThought;
    }

    public void setCurrentThought(NpcThoughtData currentThought) {
        this.currentThought = currentThought;
    }

    public NpcWarningRuleData getWarningRules() {
        if (warningRules == null) {
            warningRules = new NpcWarningRuleData();
        }
        return warningRules;
    }

    public void setWarningRules(NpcWarningRuleData warningRules) {
        this.warningRules = warningRules;
    }
}
