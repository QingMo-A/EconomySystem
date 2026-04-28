package com.mo.economy_system.core.task_system;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class TaskPlayerData extends TaskBaseData {
    //存储完成任务玩家数据的集合
    private Set<FinishedPlayer> finishedPlayers = new HashSet<>();

    @OnlyIn(Dist.CLIENT)
    private boolean isClientPlayerFinished;
    @OnlyIn(Dist.CLIENT)
    public void setClientPlayerFinished(boolean finished) {
        this.isClientPlayerFinished = finished;
    }
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

        //重写equals，以UUID作为唯一标识
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FinishedPlayer that = (FinishedPlayer) o;
            return Objects.equals(playerUUID, that.playerUUID);
        }

        //重写hashCode,基于UUID计算哈希值
        @Override
        public int hashCode() {
            return Objects.hash(playerUUID);
        }
    }

    //构造方法里面不要写玩家的集合，集合用来存储完成任务的玩家
    public TaskPlayerData(int taskId, String taskName, String taskContent, long startTime, long endTime) {
        super(taskId, taskName, taskContent, startTime, endTime);
    }

    //添加完成任务的玩家
    public void addFinishedPlayer(String playerName, UUID playerUUID) {
        finishedPlayers.add(new FinishedPlayer(playerName, playerUUID));
    }

    //判断玩家是否已完成任务
    public boolean isPlayerFinished(UUID playerUUID) {
        return finishedPlayers.stream()
                .anyMatch(player -> player.getPlayerUUID().equals(playerUUID));
    }

    //获取所有完成任务的玩家
    public Set<FinishedPlayer> getFinishedPlayers() {
        return finishedPlayers;
    }
}
