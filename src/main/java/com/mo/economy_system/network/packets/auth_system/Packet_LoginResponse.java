//package com.mo.economy_system.network.packets.auth_system;
//
//import com.mo.economy_system.core.auth_system.network.ClientAuthHandler;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraftforge.api.distmarker.Dist;
//import net.minecraftforge.fml.DistExecutor;
//import net.minecraftforge.network.NetworkEvent;
//
//import java.util.function.Supplier;
//
//public class Packet_LoginResponse {
//    private final boolean success;
//    private final String message;
//
//    public Packet_LoginResponse(boolean success, String message) {
//        this.success = success;
//        this.message = message;
//    }
//
//    public static Packet_LoginResponse decode(FriendlyByteBuf buffer) {
//        return new Packet_LoginResponse(buffer.readBoolean(), buffer.readUtf());
//    }
//
//    public static void encode(Packet_LoginResponse packet, FriendlyByteBuf buffer) {
//        buffer.writeBoolean(packet.success);
//        buffer.writeUtf(packet.message);
//    }
//
//    public static void handle(Packet_LoginResponse packet, Supplier<NetworkEvent.Context> contextSupplier) {
//        NetworkEvent.Context context = contextSupplier.get();
//        context.enqueueWork(() -> {
//            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
//                // 使用ClientAuthHandler处理响应
//                ClientAuthHandler.handleAuthResult(packet.success, packet.message);
//            });
//        });
//        context.setPacketHandled(true);
//    }
//}
