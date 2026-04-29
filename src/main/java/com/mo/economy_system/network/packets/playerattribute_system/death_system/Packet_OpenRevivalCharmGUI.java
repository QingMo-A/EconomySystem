package com.mo.economy_system.network.packets.playerattribute_system.death_system;

import com.mo.economy_system.core.playerattributes_system.death.Screen_RevivalCharm;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;


/**
 * 打开复活护符 GUI 数据包
 * 服务端发送到客户端
 */
public class Packet_OpenRevivalCharmGUI implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_OpenRevivalCharmGUI> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "playerattribute_system/death_system/packet_open_revival_charm_gui"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_OpenRevivalCharmGUI> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_OpenRevivalCharmGUI.encode(packet, buf), Packet_OpenRevivalCharmGUI::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public Packet_OpenRevivalCharmGUI() {}

    public static void encode(Packet_OpenRevivalCharmGUI packet, FriendlyByteBuf buf) {}

    public static Packet_OpenRevivalCharmGUI decode(FriendlyByteBuf buf) {
        return new Packet_OpenRevivalCharmGUI();
    }

    public static void handle(Packet_OpenRevivalCharmGUI packet, IPayloadContext context) {

        // 只在客户端执行
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> {
                handleClient();
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient() {
        Minecraft.getInstance().setScreen(new Screen_RevivalCharm());
    }
}
