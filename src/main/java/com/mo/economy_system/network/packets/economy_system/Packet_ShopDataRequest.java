package com.mo.economy_system.network.packets.economy_system;

import com.mo.economy_system.EconomySystem;
import com.mo.economy_system.core.economy_system.shop.ShopItem;
import com.mo.economy_system.events.EconomySystem_EventHandler;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.utils.Util_Message;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public class Packet_ShopDataRequest implements net.minecraft.network.protocol.common.custom.CustomPacketPayload {

    public static final net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<Packet_ShopDataRequest> TYPE = new net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.mo.economy_system.EconomySystem.MODID, "economy_system/packet_shop_data_request"));
    public static final net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Packet_ShopDataRequest> STREAM_CODEC = net.minecraft.network.codec.StreamCodec.of((buf, packet) -> Packet_ShopDataRequest.encode(packet, buf), Packet_ShopDataRequest::decode);

    @Override
    public net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type<? extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> type() {
        return TYPE;
    }

    public Packet_ShopDataRequest() {}

    public static void encode(Packet_ShopDataRequest msg, FriendlyByteBuf buf) {}

    public static Packet_ShopDataRequest decode(FriendlyByteBuf buf) {
        return new Packet_ShopDataRequest();
    }

    public static void handle(Packet_ShopDataRequest msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = context.player() instanceof ServerPlayer serverPlayer ? serverPlayer : null;
            if (player != null) {
                // Util_Message.sendDebugMessage("收到来自客户端的商店数据请求");
                // 从 ShopManager 获取商店商品
                List<ShopItem> shopItems = EconomySystem_EventHandler.shopManager.getItems();

                // Util_Message.sendDebugMessage("商店数据: " + shopItems.size() + " 个");

                // 将商品列表发送到客户端
                EconomySystem_NetworkManager.sendToClient(player, new Packet_ShopDataResponse(shopItems));
            }
        });
    }
}
