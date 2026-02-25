package com.mo.economy_system.network.packets.notice_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.screen.server_screen.serverscreen.ServerScreenUI_Screen;
import com.mo.economy_system.server.notice.NoticeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 公告列表响应数据包（服务端 -> 客户端）
 * 包含所有公告和玩家已读状态
 */
public class Packet_NoticeListResponse {

    private final List<NoticeData> notices;
    private final Set<Integer> readNoticeIds;

    public Packet_NoticeListResponse(List<NoticeData> notices, Set<Integer> readNoticeIds) {
        this.notices = notices;
        this.readNoticeIds = readNoticeIds;
    }

    public static void encode(Packet_NoticeListResponse msg, FriendlyByteBuf buf) {
        // 写入公告数量
        buf.writeInt(msg.notices.size());
        for (NoticeData notice : msg.notices) {
            buf.writeInt(notice.getNoticeId());
            buf.writeUtf(notice.getNoticeTitle());
            buf.writeUtf(notice.getNoticeContent());
            buf.writeLong(notice.getPublishTime());
        }

        // 写入已读公告ID数量
        buf.writeInt(msg.readNoticeIds.size());
        for (Integer readId : msg.readNoticeIds) {
            buf.writeInt(readId);
        }
    }

    public static Packet_NoticeListResponse decode(FriendlyByteBuf buf) {
        List<NoticeData> notices = new ArrayList<>();
        Set<Integer> readNoticeIds = new HashSet<>();

        // 读取公告数量
        int noticeCount = buf.readInt();
        for (int i = 0; i < noticeCount; i++) {
            int noticeId = buf.readInt();
            String title = buf.readUtf();
            String content = buf.readUtf();
            long publishTime = buf.readLong();
            notices.add(new NoticeData(noticeId, title, content, publishTime));
        }

        // 读取已读公告ID数量
        int readCount = buf.readInt();
        for (int i = 0; i < readCount; i++) {
            readNoticeIds.add(buf.readInt());
        }

        return new Packet_NoticeListResponse(notices, readNoticeIds);
    }

    public static void handle(Packet_NoticeListResponse msg, Supplier<NetworkEvent.Context> contextSupplier) {
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
    private static void handleClient(Packet_NoticeListResponse msg) {
        EconomySystem.LOGGER.info("收到 {} 条公告", msg.notices.size());
        // 将公告数据传递给UI
        ServerScreenUI_Screen.setNoticeData(msg.notices, msg.readNoticeIds);
    }

    public List<NoticeData> getNotices() {
        return notices;
    }

    public Set<Integer> getReadNoticeIds() {
        return readNoticeIds;
    }
}
