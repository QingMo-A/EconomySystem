package com.mo.economy_system.network.packets.tip_system;

import com.mo.economy_system.screen.server_screen.tips.TipDisplayManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端向客户端发送 Tip 信息的数据包
 */
public class Packet_SendTipToClient {
    // Tip 文本内容
    private final String tipText;
    // Tip 显示时长（毫秒）
    private final int displayDuration;

    // 构造方法（用于服务端构建数据包）
    public Packet_SendTipToClient(String tipText, int displayDuration) {
        this.tipText = tipText;
        this.displayDuration = displayDuration;
    }

    // 反序列化（客户端解码数据包）
    public static Packet_SendTipToClient decode(FriendlyByteBuf buf) {
        String text = buf.readUtf();
        int duration = buf.readInt();
        return new Packet_SendTipToClient(text, duration);
    }

    // 序列化（服务端编码数据包）
    public static void encode(Packet_SendTipToClient packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.tipText);
        buf.writeInt(packet.displayDuration);
    }

    // 数据包处理逻辑（客户端执行）
    public static void handle(Packet_SendTipToClient packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        // 确保在客户端主线程执行 UI 渲染相关操作
        context.enqueueWork(() -> {
            // 调用 TipDisplayManager 添加 Tip 信息，自动渲染
            TipDisplayManager.addMessage(packet.tipText, packet.displayDuration);
        });
        context.setPacketHandled(true);
    }
}