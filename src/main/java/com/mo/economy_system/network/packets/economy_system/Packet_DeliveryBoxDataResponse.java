package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.core.economy_system.delivery_box.DeliveryItem;
import com.mo.economy_system.screen.economy_system.deliver_box.Screen_DeliveryBox;
import com.mo.economy_system.screen.economy_system.market.Screen_Market;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public class Packet_DeliveryBoxDataResponse implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_DeliveryBoxDataResponse> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_delivery_box_data_response"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_DeliveryBoxDataResponse> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_DeliveryBoxDataResponse.encode(packet, buf), Packet_DeliveryBoxDataResponse::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }
    private final List<DeliveryItem> deliveryItems;

    // 构造函数
    public Packet_DeliveryBoxDataResponse(List<DeliveryItem> deliveryItems) {
        this.deliveryItems = deliveryItems;
    }

    // 编码数据包
    public static void encode(Packet_DeliveryBoxDataResponse msg, FriendlyByteBuf buf) {
        // 先写物品数量
        buf.writeInt(msg.deliveryItems.size());
        for (DeliveryItem item : msg.deliveryItems) {
            buf.writeNbt(item.toNBT());
        }
    }

    // 解码数据包
    public static Packet_DeliveryBoxDataResponse decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        List<DeliveryItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(DeliveryItem.fromNBT(buf.readNbt()));
        }
        return new Packet_DeliveryBoxDataResponse(items);
    }

    // 处理响应包
    public static void handle(Packet_DeliveryBoxDataResponse msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 获取当前屏幕实例并更新收货箱物品
            if (Minecraft.getInstance().screen instanceof Screen_DeliveryBox screenDeliveryBox) {
                screenDeliveryBox.updateDeliveryItems(msg.deliveryItems);
            }
        });
    }
}
