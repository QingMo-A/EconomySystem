package com.mo.economy_system.core.story_system;

import java.util.ArrayList;
import java.util.List;

/**
 * 故事阶段数据
 * 每个阶段包含：阶段信息 + 任务列表 + 怪物数值调整
 */
public class StoryStageData {
    private int stageId;
    private String stageName;
    private String stageDescription;

    // 该阶段包含的任务列表
    private List<StoryTaskData> tasks = new ArrayList<>();

    // 怪物数值调整（该阶段生效）
    private MonsterModifier monsterModifier;

    // ==================== MonsterModifier 内部类 ====================
    public static class MonsterModifier {
        private float healthMultiplier = 1.0f;     // 生命值倍率
        private float damageMultiplier = 1.0f;     // 伤害倍率
        private float speedMultiplier = 1.0f;      // 速度倍率
        private float knockbackResistance = 0.0f;  // 击退抗性

        public MonsterModifier() {}

        public MonsterModifier(float healthMult, float damageMult, float speedMult, float knockbackResist) {
            this.healthMultiplier = healthMult;
            this.damageMultiplier = damageMult;
            this.speedMultiplier = speedMult;
            this.knockbackResistance = knockbackResist;
        }

        // Getters
        public float getHealthMultiplier() { return healthMultiplier; }
        public void setHealthMultiplier(float healthMultiplier) { this.healthMultiplier = healthMultiplier; }

        public float getDamageMultiplier() { return damageMultiplier; }
        public void setDamageMultiplier(float damageMultiplier) { this.damageMultiplier = damageMultiplier; }

        public float getSpeedMultiplier() { return speedMultiplier; }
        public void setSpeedMultiplier(float speedMultiplier) { this.speedMultiplier = speedMultiplier; }

        public float getKnockbackResistance() { return knockbackResistance; }
        public void setKnockbackResistance(float knockbackResistance) { this.knockbackResistance = knockbackResistance; }
    }

    // ==================== 构造函数 ====================
    public StoryStageData() {}

    public StoryStageData(int stageId, String stageName, String stageDescription) {
        this.stageId = stageId;
        this.stageName = stageName;
        this.stageDescription = stageDescription;
    }

    // ==================== Getters and Setters ====================
    public int getStageId() { return stageId; }
    public void setStageId(int stageId) { this.stageId = stageId; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getStageDescription() { return stageDescription; }
    public void setStageDescription(String stageDescription) { this.stageDescription = stageDescription; }

    public List<StoryTaskData> getTasks() { return tasks; }
    public void setTasks(List<StoryTaskData> tasks) { this.tasks = tasks; }

    public void addTask(StoryTaskData task) {
        if (tasks == null) {
            tasks = new ArrayList<>();
        }
        tasks.add(task);
    }

    public MonsterModifier getMonsterModifier() { return monsterModifier; }
    public void setMonsterModifier(MonsterModifier monsterModifier) { this.monsterModifier = monsterModifier; }

    // ==================== 辅助方法 ====================
    /**
     * 获取该阶段总任务数
     */
    public int getTotalTaskCount() {
        return tasks != null ? tasks.size() : 0;
    }

    /**
     * 获取该阶段已完成任务数
     */
    public int getCompletedTaskCount() {
        if (tasks == null) return 0;
        return (int) tasks.stream().filter(StoryTaskData::isCompleted).count();
    }

    /**
     * 获取该阶段完成百分比
     */
    public float getProgressPercentage() {
        int total = getTotalTaskCount();
        if (total == 0) return 0.0f;
        return (float) getCompletedTaskCount() / total;
    }
}
