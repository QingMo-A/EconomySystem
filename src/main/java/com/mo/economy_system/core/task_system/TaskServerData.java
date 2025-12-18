package com.mo.economy_system.core.task_system;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TaskServerData extends TaskBaseData {
    private float taskCompletePercentage;
    private Set<FinishedPlayer> finishedPlayers = new HashSet<>();

    // 客户端专用字段：标记当前玩家是否完成
    @OnlyIn(Dist.CLIENT)
    private boolean isClientPlayerFinished;

    // 客户端专用setter
    @OnlyIn(Dist.CLIENT)
    public void setClientPlayerFinished(boolean finished) {
        this.isClientPlayerFinished = finished;
    }

    // 客户端专用getter（UI渲染时调用）
    @OnlyIn(Dist.CLIENT)
    public boolean isClientPlayerFinished() {
        return this.isClientPlayerFinished;
    }

    public static class FinishedPlayer {
        String playerName;
        UUID playerUUID;

        public FinishedPlayer(String playerName, UUID playerUUID) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
        }

        public String getPlayerName() {
            return playerName;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

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

    public TaskServerData(int taskId, String taskName, String taskContent, long startTime, long endTime, float taskCompletePercentage) {
        super(taskId, taskName, taskContent, startTime, endTime);
        this.taskCompletePercentage = taskCompletePercentage;
    }

    //获取完成百分比
    public float getTaskCompletePercentage() {
        return taskCompletePercentage;
    }
    //设置完成百分比
    public void setTaskCompletePercentage(float taskCompletePercentage) {
        this.taskCompletePercentage = taskCompletePercentage;
    }

    //添加完成任务的玩家
    public void addFinishedPlayer(String playerName, UUID playerUUID) {
        finishedPlayers.add(new FinishedPlayer(playerName, playerUUID));
    }
    //获取完成任务的玩家数量
    public int getFinishedPlayerCount() {
        return finishedPlayers.size();
    }
    //判断玩家是否已完成该服务器任务
    public boolean isPlayerFinished(UUID playerUUID) {
        return finishedPlayers.stream()
                .anyMatch(player -> player.getPlayerUUID().equals(playerUUID));
    }
}
