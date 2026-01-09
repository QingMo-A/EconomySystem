//package com.mo.economy_system.network.packets.auth_system;
//
//import com.mo.economy_system.core.auth_system.ServerAuthPacketHandler;
//import net.minecraft.network.FriendlyByteBuf;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraftforge.network.NetworkEvent;
//
//import java.util.function.Supplier;
//
//public class Packet_LoginRequest {
//    private final String password;
//
//    public Packet_LoginRequest(String password) {
//        this.password = password;
//    }
//
//    public static Packet_LoginRequest decode(FriendlyByteBuf buffer) {
//        return new Packet_LoginRequest(buffer.readUtf());
//    }
//
//    public static void encode(Packet_LoginRequest packet, FriendlyByteBuf buffer) {
//        buffer.writeUtf(packet.password);
//    }
//
//    public static void handle(Packet_LoginRequest packet, Supplier<NetworkEvent.Context> contextSupplier) {
//        NetworkEvent.Context context = contextSupplier.get();
//        context.enqueueWork(() -> {
//            ServerPlayer player = context.getSender();
//            if (player != null) {
//                ServerAuthPacketHandler.handleLoginRequest(player, packet.password);
//            }
//        });
//        context.setPacketHandled(true);
//    }
//
//    public String getPassword() {
//        return password;
//    }
//}
