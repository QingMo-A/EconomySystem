// Packet_SyncCompleteTask.java
package com.mo.economy_system.network.packets.task_system;

import com.mo.economy_system.core.task_system.TaskDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class Packet_SyncCompleteTask {
    private final int taskId; //任务ID
    private final boolean isServerTask; // true=服务器任务，false=个人任务

    public Packet_SyncCompleteTask(int taskId, boolean isServerTask) {
        this.taskId = taskId;
        this.isServerTask = isServerTask;
    }

    public static void encode(Packet_SyncCompleteTask packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.taskId);
        buf.writeBoolean(packet.isServerTask);
    }

    public static Packet_SyncCompleteTask decode(FriendlyByteBuf buf) {
        int taskId = buf.readInt();
        boolean isServerTask = buf.readBoolean();
        return new Packet_SyncCompleteTask(taskId, isServerTask);
    }

    public static void handle(Packet_SyncCompleteTask packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);
        // 在服务端主线程执行
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender(); // 获取发送请求的玩家
            if (player == null) return;

            UUID playerUUID = player.getUUID();
            String playerName = player.getGameProfile().getName();

            // 根据任务类型调用对应方法更新数据
            if (packet.isServerTask) {
                TaskDataManager.playerCompleteServerTask(packet.taskId, playerName, playerUUID);
            } else {
                TaskDataManager.playerCompleteOwnTask(packet.taskId, playerName, playerUUID);
            }
        });
    }
}