package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.core.playerattributes_system.death.Screen_RevivalCharm;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开复活护符 GUI 数据包
 * 服务端发送到客户端
 */
public class Packet_OpenRevivalCharmGUI {

    public Packet_OpenRevivalCharmGUI() {}

    public static void encode(Packet_OpenRevivalCharmGUI packet, FriendlyByteBuf buf) {}

    public static Packet_OpenRevivalCharmGUI decode(FriendlyByteBuf buf) {
        return new Packet_OpenRevivalCharmGUI();
    }

    public static void handle(Packet_OpenRevivalCharmGUI packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        // 只在客户端执行
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> {
                handleClient();
            });
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        Minecraft.getInstance().setScreen(new Screen_RevivalCharm());
    }
}
