package com.mo.economy_system.network.packets.notice_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.notice.PlayerNoticeDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 标记公告为已请求数据包（客户端 -> 服务端）
 */
public class Packet_MarkNoticeReadRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_MarkNoticeReadRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "notice_system/packet_mark_notice_read_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_MarkNoticeReadRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_MarkNoticeReadRequest.encode(packet, buf), Packet_MarkNoticeReadRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(Packet_MarkNoticeReadRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            handleServer(msg, context);
        });
    }

    private static void handleServer(Packet_MarkNoticeReadRequest msg, IPayloadContext context) {
        var serverPlayer = context.player() instanceof net.minecraft.server.level.ServerPlayer player ? player : null;
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
