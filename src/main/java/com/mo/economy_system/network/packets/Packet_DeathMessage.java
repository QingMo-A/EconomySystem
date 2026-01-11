package com.mo.economy_system.network.packets;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.server.serverui.customsystemui.SystemMessageDisplay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/**
 * 玩家死亡消息网络包 - 用于在右上角显示死亡消息
 */
public class Packet_DeathMessage {
    private final Component deathMessage;

    public Packet_DeathMessage(Component deathMessage) {
        this.deathMessage = deathMessage;
    }

    public static void encode(Packet_DeathMessage packet, FriendlyByteBuf buf) {
        buf.writeComponent(packet.deathMessage);
    }

    public static Packet_DeathMessage decode(FriendlyByteBuf buf) {
        Component deathMessage = buf.readComponent();
        return new Packet_DeathMessage(deathMessage);
    }

    public static void handle(Packet_DeathMessage packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        final Component safeDeathMessage = packet.deathMessage;

        context.enqueueWork(() -> processOnMainThread(safeDeathMessage));
        context.setPacketHandled(true);
    }

    private static void processOnMainThread(Component deathMessage) {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> new ClientRunnable(deathMessage));
    }

    @OnlyIn(Dist.CLIENT)
    private static class ClientRunnable implements DistExecutor.SafeRunnable {
        private final Component deathMessage;

        public ClientRunnable(Component deathMessage) {
            this.deathMessage = deathMessage;
        }

        @Override
        public void run() {
            showDeathMessageOnClient(deathMessage);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void showDeathMessageOnClient(Component deathMessage) {
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            if (minecraft != null && minecraft.isSameThread() && minecraft.player != null) {
                // 使用系统消息显示框
                SystemMessageDisplay.addMessage(deathMessage);
            }
        } catch (Exception e) {
            EconomySystem.LOGGER.error("显示死亡消息失败", e);
        }
    }
}
