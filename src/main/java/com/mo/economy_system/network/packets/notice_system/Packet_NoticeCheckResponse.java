package com.mo.economy_system.network.packets.notice_system;

import com.mo.economy_system.EconomySystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 新公告提醒数据包（服务端 -> 客户端）
 * 玩家登录时，如果有新公告则发送此包提醒玩家
 */
public class Packet_NoticeCheckResponse {

    private final boolean hasNewNotice;
    private final int latestNoticeId;
    private final String latestNoticeTitle;

    public Packet_NoticeCheckResponse(boolean hasNewNotice, int latestNoticeId, String latestNoticeTitle) {
        this.hasNewNotice = hasNewNotice;
        this.latestNoticeId = latestNoticeId;
        this.latestNoticeTitle = latestNoticeTitle;
    }

    public static void encode(Packet_NoticeCheckResponse msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.hasNewNotice);
        buf.writeInt(msg.latestNoticeId);
        buf.writeUtf(msg.latestNoticeTitle);
    }

    public static Packet_NoticeCheckResponse decode(FriendlyByteBuf buf) {
        boolean hasNewNotice = buf.readBoolean();
        int latestNoticeId = buf.readInt();
        String latestNoticeTitle = buf.readUtf();
        return new Packet_NoticeCheckResponse(hasNewNotice, latestNoticeId, latestNoticeTitle);
    }

    public static void handle(Packet_NoticeCheckResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 只在客户端处理
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClient(msg);
            }
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_NoticeCheckResponse msg) {
        if (msg.hasNewNotice) {
            Minecraft mc = Minecraft.getInstance();
            // 显示提示消息
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("§a§l[新公告] §e" + msg.latestNoticeTitle),
                false
            );
            EconomySystem.LOGGER.info("收到新公告提醒: {}", msg.latestNoticeTitle);
        }
    }

    public boolean isHasNewNotice() {
        return hasNewNotice;
    }

    public int getLatestNoticeId() {
        return latestNoticeId;
    }

    public String getLatestNoticeTitle() {
        return latestNoticeTitle;
    }
}
