package com.mo.economy_system.network.packets.task_system;


import com.mo.economy_system.core.story_system.StoryStageData;
import com.mo.economy_system.core.story_system.StoryTaskData;
import com.mo.economy_system.core.task_system.TaskPlayerData;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

//全量包，进服申请一次
public class Packet_SyncFullTaskData {
    private final UUID playerUUID;
    private Map<Integer, TaskPlayerData> taskPlayerData = new HashMap<>();
    private Map<Integer, StoryStageData> storyStageData = new HashMap<>();

    public Packet_SyncFullTaskData(UUID playerUUID, Map<Integer, TaskPlayerData> playerData, Map<Integer, StoryStageData> stageData) {
        this.playerUUID = playerUUID;
        this.taskPlayerData = new HashMap<>(playerData);
        this.storyStageData = new HashMap<>(stageData);
    }

    public static void encode(Packet_SyncFullTaskData packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUUID);

        // ========== 编码玩家任务 ==========
        buf.writeInt(packet.taskPlayerData.size());
        for (Map.Entry<Integer, TaskPlayerData> entry : packet.taskPlayerData.entrySet()) {
            int taskId = entry.getKey();
            TaskPlayerData task = entry.getValue();

            buf.writeInt(taskId);
            buf.writeUtf(task.getTaskName());
            buf.writeUtf(task.getTaskContent());
            buf.writeLong(task.getTaskStartTime());
            buf.writeLong(task.getTaskEndTime());
            buf.writeBoolean(task.isPlayerFinished(packet.playerUUID));
        }

        // ========== 编码故事阶段数据 ==========
        buf.writeInt(packet.storyStageData.size()); // 阶段数量
        for (Map.Entry<Integer, StoryStageData> stageEntry : packet.storyStageData.entrySet()) {
            StoryStageData stage = stageEntry.getValue();

            // 编码阶段基本信息
            buf.writeInt(stage.getStageId());
            buf.writeUtf(stage.getStageName());
            buf.writeUtf(stage.getStageDescription());

            // 编码怪物数值调整
            StoryStageData.MonsterModifier modifier = stage.getMonsterModifier();
            if (modifier != null) {
                buf.writeBoolean(true); // 有 modifier
                buf.writeFloat(modifier.getHealthMultiplier());
                buf.writeFloat(modifier.getDamageMultiplier());
                buf.writeFloat(modifier.getSpeedMultiplier());
                buf.writeFloat(modifier.getKnockbackResistance());
            } else {
                buf.writeBoolean(false);
            }

            // 编码该阶段的任务列表
            List<StoryTaskData> tasks = stage.getTasks();
            if (tasks == null) {
                buf.writeInt(0);
            } else {
                buf.writeInt(tasks.size());
                for (StoryTaskData task : tasks) {
                    buf.writeInt(task.getTaskId());
                    buf.writeUtf(task.getTaskName());
                    buf.writeUtf(task.getTaskContent());
                    buf.writeLong(task.getStartTime());
                    buf.writeLong(task.getEndTime());
                    buf.writeBoolean(task.isTaskState());
                    buf.writeBoolean(task.isCompleted());
                    buf.writeBoolean(task.isPlayerFinished(packet.playerUUID));

                    // 编码完成玩家列表
                    buf.writeInt(task.getFinishedPlayerCount());
                    if (task.getFinishedPlayers() != null) {
                        for (StoryTaskData.FinishedPlayer fp : task.getFinishedPlayers()) {
                            buf.writeUtf(fp.getPlayerName());
                            buf.writeUUID(fp.getPlayerUUID());
                        }
                    }
                }
            }
        }
    }

    public static Packet_SyncFullTaskData decode(FriendlyByteBuf buf) {
        UUID playerUUID = buf.readUUID();

        // ========== 解码玩家任务 ==========
        int playerTaskSize = buf.readInt();
        Map<Integer, TaskPlayerData> playerTaskMap = new HashMap<>();

        for (int i = 0; i < playerTaskSize; i++) {
            int taskId = buf.readInt();
            String taskName = buf.readUtf();
            String taskContent = buf.readUtf();
            long startTime = buf.readLong();
            long endTime = buf.readLong();
            boolean isPlayerFinished = buf.readBoolean();

            TaskPlayerData task = new TaskPlayerData(taskId, taskName, taskContent, startTime, endTime);
            task.setClientPlayerFinished(isPlayerFinished);
            playerTaskMap.put(taskId, task);
        }

        // ========== 解码故事阶段数据 ==========
        int stageCount = buf.readInt();
        Map<Integer, StoryStageData> stageMap = new HashMap<>();

        for (int i = 0; i < stageCount; i++) {
            // 解码阶段基本信息
            int stageId = buf.readInt();
            String stageName = buf.readUtf();
            String stageDescription = buf.readUtf();

            StoryStageData stage = new StoryStageData(stageId, stageName, stageDescription);

            // 解码怪物数值调整
            boolean hasModifier = buf.readBoolean();
            if (hasModifier) {
                float healthMult = buf.readFloat();
                float damageMult = buf.readFloat();
                float speedMult = buf.readFloat();
                float knockbackResist = buf.readFloat();
                stage.setMonsterModifier(new StoryStageData.MonsterModifier(healthMult, damageMult, speedMult, knockbackResist));
            }

            // 解码任务列表
            int taskCount = buf.readInt();
            List<StoryTaskData> tasks = new ArrayList<>();
            for (int j = 0; j < taskCount; j++) {
                int taskId = buf.readInt();
                String taskName = buf.readUtf();
                String taskContent = buf.readUtf();
                long startTime = buf.readLong();
                long endTime = buf.readLong();
                boolean taskState = buf.readBoolean();
                boolean isCompleted = buf.readBoolean();
                boolean isPlayerFinished = buf.readBoolean();

                StoryTaskData task = new StoryTaskData(taskId, taskName, taskContent, startTime, endTime);
                task.setTaskState(taskState);
                task.setCompleted(isCompleted);
                task.setClientPlayerFinished(isPlayerFinished);

                // 解码完成玩家列表
                int finishedCount = buf.readInt();
                for (int k = 0; k < finishedCount; k++) {
                    String playerName = buf.readUtf();
                    UUID playerUuid = buf.readUUID();
                    task.addFinishedPlayer(playerName, playerUuid);
                }

                tasks.add(task);
            }
            stage.setTasks(tasks);
            stageMap.put(stageId, stage);
        }

        return new Packet_SyncFullTaskData(playerUUID, playerTaskMap, stageMap);
    }

    public static void handle(Packet_SyncFullTaskData packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);

        ctx.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // 客户端更新缓存
                ClientTaskCache.updateFullTaskData(
                        packet.getPlayerUUID(),
                        packet.getTaskPlayerData(),
                        packet.getStoryStageData()
                );
                // 更新 ClientCacheManager
                com.mo.economy_system.client.cache.ClientCacheManager.setPlayerTasks(packet.getTaskPlayerData());
                com.mo.economy_system.client.cache.ClientCacheManager.setStoryStages(packet.getStoryStageData());
            });
        });
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Map<Integer, TaskPlayerData> getTaskPlayerData() {
        return taskPlayerData;
    }

    public Map<Integer, StoryStageData> getStoryStageData() {
        return storyStageData;
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientTaskCache {
        private static final Map<Integer, TaskPlayerData> CLIENT_PLAYER_TASK_CACHE = new HashMap<>();
        private static final Map<Integer, StoryStageData> CLIENT_STORY_STAGE_CACHE = new HashMap<>();

        public static void updateFullTaskData(UUID playerUUID, Map<Integer, TaskPlayerData> playerData, Map<Integer, StoryStageData> stageData) {
            CLIENT_PLAYER_TASK_CACHE.clear();
            CLIENT_STORY_STAGE_CACHE.clear();
            CLIENT_PLAYER_TASK_CACHE.putAll(playerData);
            CLIENT_STORY_STAGE_CACHE.putAll(stageData);
        }

        public static Map<Integer, TaskPlayerData> getClientPlayerTaskCache() {
            return CLIENT_PLAYER_TASK_CACHE;
        }

        public static Map<Integer, StoryStageData> getClientStoryStageCache() {
            return CLIENT_STORY_STAGE_CACHE;
        }
    }
}
