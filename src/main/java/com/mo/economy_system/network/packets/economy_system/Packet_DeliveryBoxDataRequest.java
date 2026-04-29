package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryBoxSavedData;
import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class Packet_DeliveryBoxDataRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_DeliveryBoxDataRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_delivery_box_data_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_DeliveryBoxDataRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_DeliveryBoxDataRequest.encode(packet, buf), Packet_DeliveryBoxDataRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    public Packet_DeliveryBoxDataRequest() {}

    public static void encode(Packet_DeliveryBoxDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_DeliveryBoxDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_DeliveryBoxDataRequest();
    }

    public static void handle(Packet_DeliveryBoxDataRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                DeliveryBoxSavedData data = DeliveryBoxSavedData.getInstance(player.serverLevel());
                // 获取收货箱数据
                List<DeliveryItem> deliveryItems = data.getItems(player.getUUID());
                // 发送响应数据包
                EconomySystem_NetworkManager.sendToClient(player, new Packet_DeliveryBoxDataResponse(deliveryItems));
            }
        });
    }
}
