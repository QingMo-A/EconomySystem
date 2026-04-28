package com.mo.economy_system.network.packets.economy_system.demand_order;

import com.mo.economy_system.core.economy_system.EconomySavedData;
import com.mo.economy_system.core.economy_system.market.MarketItem;
import com.mo.economy_system.core.economy_system.market.MarketManager;
import com.mo.economy_system.utils.Util_MessageKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public class Packet_CreateDemandOrder {

    private static final String LIST_SUCCESSFULLY_MESSAGE_KEY = "message.list.list_successfully";

    private final MarketItem marketItem;

    public Packet_CreateDemandOrder(MarketItem marketItem) {
        this.marketItem = marketItem;
    }

    public static void encode(Packet_CreateDemandOrder msg, FriendlyByteBuf buf) {
        buf.writeNbt(msg.marketItem.toNBT());
    }

    public static Packet_CreateDemandOrder decode(FriendlyByteBuf buf) {
        return new Packet_CreateDemandOrder(MarketItem.fromNBT(buf.readNbt()));
    }

    public static void handle(Packet_CreateDemandOrder msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender(); // 获取发送上架请求的玩家
            if (player == null) return;

            ServerLevel serverLevel = player.serverLevel();
            EconomySavedData savedData = EconomySavedData.getInstance(serverLevel);

            // 验证买家是否有足够货币
            int price = msg.marketItem.getBasePrice();
            if (!savedData.hasEnoughBalance(player.getUUID(), price)) {
                player.sendSystemMessage(Component.translatable(Util_MessageKeys.MARKET_PURCHASE_FAILED_MESSAGE_KEY));
                return;
            }

            savedData.minBalance(player.getUUID(), price);
            // 将商品加入市场管理器
            MarketManager.addMarketItem(msg.marketItem);

            // 发送成功消息给玩家
            player.sendSystemMessage(Component.translatable(LIST_SUCCESSFULLY_MESSAGE_KEY));
        });
        context.setPacketHandled(true);
    }

}
