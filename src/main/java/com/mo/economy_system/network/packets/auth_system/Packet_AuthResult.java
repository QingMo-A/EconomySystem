//package com.mo.economy_system.network.packets.auth_system;
//
//import com.mo.economy_system.core.auth_system.network.ClientAuthHandler;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraftforge.network.NetworkEvent;
//
//import java.util.function.Supplier;
//
///**
// * 服务端发送给客户端的认证结果包
// * 通知客户端认证成功或失败
// */
//public class Packet_AuthResult {
//    private final boolean success;
//    private final String message;
//
//    public Packet_AuthResult(boolean success, String message) {
//        this.success = success;
//        this.message = message;
//    }
//
//    public static Packet_AuthResult decode(FriendlyByteBuf buffer) {
//        return new Packet_AuthResult(buffer.readBoolean(), buffer.readUtf(32767));
//    }
//
//    public static void encode(Packet_AuthResult packet, FriendlyByteBuf buffer) {
//        buffer.writeBoolean(packet.success);
//        buffer.writeUtf(packet.message);
//    }
//
//    public static void handle(Packet_AuthResult packet, Supplier<NetworkEvent.Context> contextSupplier) {
//        NetworkEvent.Context context = contextSupplier.get();
//        context.enqueueWork(() -> {
//            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
//                net.minecraftforge.api.distmarker.Dist.CLIENT,
//                () -> () -> ClientAuthHandler.handleAuthResult(packet.success, packet.message)
//            );
//        });
//        context.setPacketHandled(true);
//    }
//
//    public boolean isSuccess() {
//        return success;
//    }
//
//    public String getMessage() {
//        return message;
//    }
//}
