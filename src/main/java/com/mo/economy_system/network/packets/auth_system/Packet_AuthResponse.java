//package com.mo.economy_system.network.packets.auth_system;
//
//import com.mo.economy_system.core.auth_system.network.ServerAuthHandler;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraftforge.network.NetworkEvent;
//
//import java.util.function.Supplier;
//
///**
// * 客户端发送给服务端的认证响应包
// * 包含登录或注册信息
// */
//public class Packet_AuthResponse {
//    private final boolean isLogin;
//    private final String password;
//
//    public Packet_AuthResponse(boolean isLogin, String password) {
//        this.isLogin = isLogin;
//        this.password = password;
//    }
//
//    public static Packet_AuthResponse decode(FriendlyByteBuf buffer) {
//        return new Packet_AuthResponse(buffer.readBoolean(), buffer.readUtf(32767));
//    }
//
//    public static void encode(Packet_AuthResponse packet, FriendlyByteBuf buffer) {
//        buffer.writeBoolean(packet.isLogin);
//        buffer.writeUtf(packet.password);
//    }
//
//    public static void handle(Packet_AuthResponse packet, Supplier<NetworkEvent.Context> contextSupplier) {
//        NetworkEvent.Context context = contextSupplier.get();
//        context.enqueueWork(() -> {
//            ServerPlayer player = context.getSender();
//            if (player != null) {
//                ServerAuthHandler.handleAuthResponse(player, packet.isLogin(), packet.password);
//            }
//        });
//        context.setPacketHandled(true);
//    }
//
//    public boolean isLogin() {
//        return isLogin;
//    }
//
//    public String getPassword() {
//        return password;
//    }
//}
