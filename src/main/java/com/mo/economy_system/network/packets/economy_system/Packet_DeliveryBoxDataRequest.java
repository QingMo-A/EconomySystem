package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Supplier;

public class Packet_DeliveryBoxDataRequest {
    public Packet_DeliveryBoxDataRequest() {}

    public static void encode(Packet_DeliveryBoxDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_DeliveryBoxDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_DeliveryBoxDataRequest();
    }

    public static void handle(Packet_DeliveryBoxDataRequest msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                DeliveryBoxSavedData data = DeliveryBoxSavedData.getInstance(player.serverLevel());
                // 获取收货箱数据
                List<DeliveryItem> deliveryItems = data.getItems(player.getUUID());
                // 发送响应数据包
                EconomySystem_NetworkManager.INSTANCE.send(player, new Packet_DeliveryBoxDataResponse(deliveryItems));
            }
        });
        context.setPacketHandled(true);
    }
}
