package com.mo.economy_system.network.packets.task_system;


import com.mo.economy_system.core.task_system.TaskPlayerData;
import com.mo.economy_system.core.task_system.TaskServerData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

//全量包，进服申请一次
public class Packet_SyncFullTaskData {
    private final UUID playerUUID;
    private Map<Integer, TaskPlayerData> taskPlayerData = new HashMap<>();
    private Map<Integer, TaskServerData> taskServerData = new HashMap<>();

    public Packet_SyncFullTaskData(UUID playerUUID, Map<Integer, TaskPlayerData> playerData, Map<Integer, TaskServerData> serverData) {
        this.playerUUID = playerUUID;
        this.taskPlayerData = new HashMap<>(playerData);
        this.taskServerData = new HashMap<>(serverData);
    }

    public static void encode(Packet_SyncFullTaskData packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUUID);
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

        buf.writeInt(packet.taskServerData.size()); //先写服务器任务的大小
        for (Map.Entry<Integer, TaskServerData> entry : packet.taskServerData.entrySet()) {
            int taskId = entry.getKey();
            TaskServerData task = entry.getValue();

            buf.writeInt(taskId);
            buf.writeUtf(task.getTaskName());
            buf.writeUtf(task.getTaskContent());
            buf.writeLong(task.getTaskStartTime());
            buf.writeLong(task.getTaskEndTime());
            buf.writeFloat(task.getTaskCompletePercentage());
            buf.writeBoolean(task.isPlayerFinished(packet.playerUUID));
        }
    }

    public static Packet_SyncFullTaskData decode(FriendlyByteBuf buf) {
        UUID playerUUID = buf.readUUID();

        int playerTaskSize = buf.readInt();
        Map<Integer, TaskPlayerData> playerTaskMap = new HashMap<>();

        for (int i = 0; i < playerTaskSize; i++) {
            int taskId = buf.readInt();
            String taskName = buf.readUtf();
            String taskContent = buf.readUtf();
            long startTime = buf.readLong();
            long endTime = buf.readLong();
            boolean isPlayerFinished = buf.readBoolean();

            //重建TaskPlayerData对象
            TaskPlayerData task = new TaskPlayerData(taskId, taskName, taskContent, startTime, endTime);
            task.setClientPlayerFinished(isPlayerFinished);
            playerTaskMap.put(taskId, task);
        }

        int serverTaskSize = buf.readInt();
        Map<Integer, TaskServerData> serverTaskMap = new HashMap<>();

        for (int i = 0; i < serverTaskSize; i++) {
            int taskId = buf.readInt();
            String taskName = buf.readUtf();
            String taskContent = buf.readUtf();
            long startTime = buf.readLong();
            long endTime = buf.readLong();
            float progress = buf.readFloat();
            boolean isPlayerFinished = buf.readBoolean();

            TaskServerData task = new TaskServerData(taskId, taskName, taskContent, startTime, endTime, progress);

            task.setClientPlayerFinished(isPlayerFinished);
            serverTaskMap.put(taskId, task);
        }

        return new Packet_SyncFullTaskData(playerUUID, playerTaskMap, serverTaskMap);
    }

    public static void handle(Packet_SyncFullTaskData packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true); //标记包已处理

        //主线程执行，避免线程安全问题
        ctx.enqueueWork(() -> {
            // 仅在客户端执行逻辑
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                // 客户端更新全量任务缓存
                ClientTaskCache.updateFullTaskData(
                        packet.getPlayerUUID(),
                        packet.getTaskPlayerData(),
                        packet.getTaskServerData()
                );
            });
        });
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Map<Integer, TaskPlayerData> getTaskPlayerData() {
        return taskPlayerData;
    }

    public Map<Integer, TaskServerData> getTaskServerData() {
        return taskServerData;
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientTaskCache {
        // 客户端全局任务缓存
        private static final Map<Integer, TaskPlayerData> CLIENT_PLAYER_TASK_CACHE = new HashMap<>();
        private static final Map<Integer, TaskServerData> CLIENT_SERVER_TASK_CACHE = new HashMap<>();

        //全量更新缓存
        public static void updateFullTaskData(UUID playerUUID, Map<Integer, TaskPlayerData> playerData, Map<Integer, TaskServerData> serverData) {
            //清空旧缓存
            CLIENT_PLAYER_TASK_CACHE.clear();
            CLIENT_SERVER_TASK_CACHE.clear();
            //覆盖为新的全量数据
            CLIENT_PLAYER_TASK_CACHE.putAll(playerData);
            CLIENT_SERVER_TASK_CACHE.putAll(serverData);
            // 可选：通知UI刷新（后续你实现UI时调用）
            // TaskUIManager.refreshTaskUI();
        }

        public static Map<Integer, TaskPlayerData> getClientPlayerTaskCache() {
            return CLIENT_PLAYER_TASK_CACHE;
        }

        public static Map<Integer, TaskServerData> getClientServerTaskCache() {
            return CLIENT_SERVER_TASK_CACHE;
        }
    }
}
