package com.mo.economy_system.network.packets;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.screen.server_screen.customsystemui.SystemMessageDisplay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.DistExecutor;
import net.neoforged.neoforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * 通用系统消息数据包 - 将所有系统消息显示在右上角
 */
public class Packet_SystemMessage {
    private final Component message;
    private final int borderColor; // 边框颜色

    public Packet_SystemMessage(Component message, int borderColor) {
        this.message = message;
        this.borderColor = borderColor;
    }

    // 向后兼容：只传消息时使用默认颜色（Rank 颜色）
    public Packet_SystemMessage(Component message) {
        this(message, -1); // -1 表示使用本地玩家 Rank 颜色
    }

    public static void encode(Packet_SystemMessage packet, FriendlyByteBuf buf) {
        ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.encode(buf, packet.message);
        buf.writeInt(packet.borderColor);
    }

    public static Packet_SystemMessage decode(FriendlyByteBuf buf) {
        Component message = ComponentSerialization.TRUSTED_CONTEXT_FREE_STREAM_CODEC.decode(buf);
        int borderColor = buf.readInt();
        return new Packet_SystemMessage(message, borderColor);
    }

    public static void handle(Packet_SystemMessage packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final Component safeMessage = packet.message;
        final int safeColor = packet.borderColor;

        context.enqueueWork(() -> processOnMainThread(safeMessage, safeColor));
        context.setPacketHandled(true);
    }

    private static void processOnMainThread(Component message, int borderColor) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(message, borderColor));
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
        private final Component message;
        private final int borderColor;

        public ClientRunnable(Component message, int borderColor) {
            this.message = message;
            this.borderColor = borderColor;
        }

        @Override
        public void run() {
            showMessageOnClient(message, borderColor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void showMessageOnClient(Component message, int borderColor) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.isSameThread() && minecraft.player != null) {
                SystemMessageDisplay.addMessage(message, borderColor);
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("显示系统消息失败", e);
        }
    }
}
