// Packet_SyncCompleteTask.java
package com.mo.economy_system.network.packets.task_system;

import com.mo.economy_system.core.task_system.TaskDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

public class Packet_SyncCompleteTask implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_SyncCompleteTask> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "task_system/packet_sync_complete_task"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_SyncCompleteTask> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_SyncCompleteTask.encode(packet, buf), Packet_SyncCompleteTask::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final int taskId; //任务ID
    private final boolean isServerTask; // true=故事任务，false=个人任务

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

    public static void handle(Packet_SyncCompleteTask packet, IPayloadContext context) {
        // 在服务端主线程执行
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null; // 获取发送请求的玩家
            if (player == null) return;

            UUID playerUUID = player.getUUID();
            String playerName = player.getGameProfile().getName();

            // 根据任务类型调用对应方法更新数据
            if (packet.isServerTask) {
                TaskDataManager.playerCompleteStoryTask(packet.taskId, playerName, playerUUID);
            } else {
                TaskDataManager.playerCompleteOwnTask(packet.taskId, playerName, playerUUID);
            }
        });
    }
}
