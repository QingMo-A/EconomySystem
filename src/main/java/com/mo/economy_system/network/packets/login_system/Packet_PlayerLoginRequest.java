package com.mo.economy_system.network.packets.login_system;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.network.handling.IPayloadContext;


//服务端发送登录请求，传一个布尔型，如果是true是注册，false是登录
public class Packet_PlayerLoginRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_PlayerLoginRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "login_system/packet_player_login_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_PlayerLoginRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_PlayerLoginRequest.encode(packet, buf), Packet_PlayerLoginRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    // true=注册, false=登录
    private final boolean loginOrRegister;

    public Packet_PlayerLoginRequest(boolean loginOrRegister) {
        this.loginOrRegister = loginOrRegister;
    }

    public boolean isLoginOrRegister() {
        return loginOrRegister;
    }

    public static void encode(Packet_PlayerLoginRequest playerLoginRequest, FriendlyByteBuf buffer) {
        buffer.writeBoolean(playerLoginRequest.loginOrRegister);
    }

    public static Packet_PlayerLoginRequest decode(FriendlyByteBuf buffer) {
        boolean loginOrRegister = buffer.readBoolean();
        return new Packet_PlayerLoginRequest(loginOrRegister);
    }

    public static void handle(Packet_PlayerLoginRequest playerLoginRequest, IPayloadContext context) {

        // 只在客户端执行UI逻辑
        if (FMLLoader.getDist().isClient()) {
            context.enqueueWork(() -> {
                handleClient(playerLoginRequest);
            });
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(Packet_PlayerLoginRequest msg) {
        Minecraft minecraft = Minecraft.getInstance();
        // loginOrRegister: true=注册, false=登录
        // Screen_LoginUI参数: true=需要注册, false=不需要注册
        boolean requireRegistration = msg.isLoginOrRegister();
        com.mo.economy_system.EconomySystem.LOGGER.info("客户端收到登录请求包，loginOrRegister={}, requireRegistration={}", msg.isLoginOrRegister(), requireRegistration);
        minecraft.setScreen(new com.mo.economy_system.core.login_system.Screen_LoginUI(requireRegistration));
    }
}
