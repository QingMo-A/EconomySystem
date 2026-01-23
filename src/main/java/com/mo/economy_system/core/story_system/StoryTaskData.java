package com.mo.economy_system.core.story_system;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 故事任务数据
 * 属于某个故事阶段的任务
 */
public class StoryTaskData {
    private int taskId;
    private String taskName;
    private String taskContent;
    private long startTime;
    private long endTime;
    private boolean taskState;     // 任务是否启用
    private boolean isCompleted;   // 任务是否已完成

    // 完成该任务的玩家列表
    private Set<FinishedPlayer> finishedPlayers = new HashSet<>();

    // 客户端专用：标记当前玩家是否完成
    private transient boolean isClientPlayerFinished;

    // ==================== FinishedPlayer 内部类 ====================
    public static class FinishedPlayer {
        String playerName;
        UUID playerUUID;

        public FinishedPlayer() {}

        public FinishedPlayer(String playerName, UUID playerUUID) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
        }

        public String getPlayerName() { return playerName; }
        public void setPlayerName(String playerName) { this.playerName = playerName; }

        public UUID getPlayerUUID() { return playerUUID; }
        public void setPlayerUUID(UUID playerUUID) { this.playerUUID = playerUUID; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FinishedPlayer that = (FinishedPlayer) o;
            return Objects.equals(playerUUID, that.playerUUID);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerUUID);
        }
    }

    // ==================== 构造函数 ====================
    public StoryTaskData() {}

    public StoryTaskData(int taskId, String taskName, String taskContent, long startTime, long endTime) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.taskContent = taskContent;
        this.startTime = startTime;
        this.endTime = endTime;
        this.taskState = true;
        this.isCompleted = false;
    }

    // ==================== Getters and Setters ====================
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }

    public String getTaskContent() { return taskContent; }
    public void setTaskContent(String taskContent) { this.taskContent = taskContent; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public boolean isTaskState() { return taskState; }
    public void setTaskState(boolean taskState) { this.taskState = taskState; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public Set<FinishedPlayer> getFinishedPlayers() { return finishedPlayers; }
    public void setFinishedPlayers(Set<FinishedPlayer> finishedPlayers) { this.finishedPlayers = finishedPlayers; }

    public boolean isClientPlayerFinished() { return isClientPlayerFinished; }
    public void setClientPlayerFinished(boolean clientPlayerFinished) { isClientPlayerFinished = clientPlayerFinished; }

    // ==================== 辅助方法 ====================
    /**
     * 添加完成任务的玩家
     */
    public void addFinishedPlayer(String playerName, UUID playerUUID) {
        if (finishedPlayers == null) {
            finishedPlayers = new HashSet<>();
        }
        finishedPlayers.add(new FinishedPlayer(playerName, playerUUID));
    }

    /**
     * 获取完成任务的玩家数量
     */
    public int getFinishedPlayerCount() {
        return finishedPlayers != null ? finishedPlayers.size() : 0;
    }

    /**
     * 判断玩家是否已完成该任务
     */
    public boolean isPlayerFinished(UUID playerUUID) {
        if (finishedPlayers == null) return false;
        return finishedPlayers.stream()
                .anyMatch(player -> player.getPlayerUUID() != null && player.getPlayerUUID().equals(playerUUID));
    }
}
