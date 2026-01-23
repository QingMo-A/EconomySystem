package com.mo.economy_system.network.packets.notice_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.notice.PlayerNoticeDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 标记公告为已请求数据包（客户端 -> 服务端）
 */
public class Packet_MarkNoticeReadRequest {

    private final int noticeId;

    public Packet_MarkNoticeReadRequest(int noticeId) {
        this.noticeId = noticeId;
    }

    public static void encode(Packet_MarkNoticeReadRequest msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.noticeId);
    }

    public static Packet_MarkNoticeReadRequest decode(FriendlyByteBuf buf) {
        int noticeId = buf.readInt();
        return new Packet_MarkNoticeReadRequest(noticeId);
    }

    public static void handle(Packet_MarkNoticeReadRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 只在服务端处理
            if (context.getDirection().getReceptionSide().isServer()) {
                handleServer(msg, context);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleServer(Packet_MarkNoticeReadRequest msg, NetworkEvent.Context context) {
        var serverPlayer = context.getSender();
        if (serverPlayer == null) {
            EconomySystem.LOGGER.warn("Packet_MarkNoticeReadRequest: serverPlayer is null");
            return;
        }

        // 标记公告为已读
        PlayerNoticeDataManager.markAsRead(serverPlayer.getUUID(), msg.noticeId);
        EconomySystem.LOGGER.debug("玩家 {} 标记公告 {} 为已读", serverPlayer.getName().getString(), msg.noticeId);
    }

    public int getNoticeId() {
        return noticeId;
    }
}
