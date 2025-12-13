package com.mo.economy_system.network.packets;

import com.mo.economy_system.EconomySystem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class Packet_JoinMessage {
    private final Component title;
    private final Component content;

    public Packet_JoinMessage(Component title, Component content) {
        this.title = title;
        this.content = content;
    }

    public static void encode(Packet_JoinMessage packet, FriendlyByteBuf buf) {
        buf.writeComponent(packet.title);
        buf.writeComponent(packet.content);
    }

    public static Packet_JoinMessage decode(FriendlyByteBuf buf) {
        Component title = buf.readComponent();
        Component content = buf.readComponent();
        return new Packet_JoinMessage(title, content);
    }

    public static void handle(Packet_JoinMessage packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final Component safeTitle = packet.title;
        final Component safeContent = packet.content;

        context.enqueueWork(() -> processOnMainThread(safeTitle, safeContent));
        context.setPacketHandled(true);
    }

    private static void processOnMainThread(Component title, Component content) {
        // 关键修复：确保传递的是SafeRunnable类型
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(title, content));
    }

    // 修复：实现SafeRunnable接口而非普通Runnable
    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
        private final Component title;
        private final Component content;

        public ClientRunnable(Component title, Component content) {
            this.title = title;
            this.content = content;
        }

        @Override
        public void run() {
            showToastOnClient(title, content);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void showToastOnClient(Component title, Component content) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.isSameThread() && minecraft.getToasts() != null && minecraft.player != null) {
                minecraft.getToasts().addToast(
                        new net.minecraft.client.gui.components.toasts.SystemToast(
                                net.minecraft.client.gui.components.toasts.SystemToast.SystemToastIds.TUTORIAL_HINT,
                                title,
                                content
                        )
                );
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("显示进服/离开弹窗失败", e);
        }
    }
}