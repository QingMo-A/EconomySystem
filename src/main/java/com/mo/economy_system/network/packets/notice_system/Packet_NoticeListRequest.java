package com.mo.economy_system.network.packets.notice_system;

import com.mo.economy_system.server.notice.NoticeManager;
import com.mo.economy_system.server.notice.PlayerNoticeDataManager;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.mo.economy_system.compat.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;

/**
 * 公告列表请求数据包（客户端 -> 服务端）
 * 玩家点击"服务器公告"按钮时发送
 */
public class Packet_NoticeListRequest {

    public Packet_NoticeListRequest() {
    }

    public static void encode(Packet_NoticeListRequest msg, FriendlyByteBuf buf) {
        // 无需写入数据
    }

    public static Packet_NoticeListRequest decode(FriendlyByteBuf buf) {
        return new Packet_NoticeListRequest();
    }

    public static void handle(Packet_NoticeListRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 获取公告列表
                var notices = NoticeManager.getNotices();
                // 获取玩家已读公告ID
                var readNoticeIds = PlayerNoticeDataManager.getReadNoticeIds(player.getUUID());

                // 发送响应
                EconomySystem_NetworkManager.INSTANCE.send(
                    player,
                    new Packet_NoticeListResponse(notices, readNoticeIds)
                );
            }
        });
        context.setPacketHandled(true);
    }
}
