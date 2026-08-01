package com.mo.economy_system.target.neoforge1211.protocol;

import com.mo.economy_system.common.client.ClientMarketState;
import com.mo.economy_system.common.market.MarketDataQueryService;
import com.mo.economy_system.common.market.MarketOrderType;
import com.mo.economy_system.common.network.*;
import com.mo.economy_system.core.economy_system.market.*;
import com.mo.economy_system.network.EconomySystem_NetworkManager;
import com.mo.economy_system.platform.EconomyServices;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import java.util.*;

public final class NeoForge1211MarketDataHandlers {
    public static void request(MarketDataRequestMessage message, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) {
            var data=MarketSavedData.getInstance(player.serverLevel());
            EconomySystem_NetworkManager.sendToClient(player,MarketDataQueryService.query(data.getOrders(),player.getUUID(),message));
        }});
    }
    public static void response(MarketDataResponseMessage message, IPayloadContext context) {
        context.enqueueWork(() -> { if (ClientMarketState.apply(message)) ClientOnly.apply(message); });
    }
    private static final class ClientOnly {
        static void apply(MarketDataResponseMessage message) {
            var minecraft=net.minecraft.client.Minecraft.getInstance(); var screen=minecraft.screen;
            if(screen instanceof com.mo.economy_system.screen.Screen_Home home)home.updateTradeInfo(message.totalSales(),message.totalDemand());
            if(message.kind()==MarketDataResponseKind.INVALIDATED&&screen instanceof com.mo.economy_system.screen.economy_system.market.Screen_Market market){market.refresh();return;}
            if(message.kind()!=MarketDataResponseKind.PAGE||!(screen instanceof com.mo.economy_system.screen.economy_system.market.Screen_Market market)||minecraft.level==null)return;
            try { List<MarketItem> items=new ArrayList<>(); for(MarketOrderSnapshot order:message.orders()){
                var stack=EconomyServices.platform().itemStacks().restoreSnapshot(order.item(),minecraft.level.registryAccess()).orElseThrow(); stack.setCount(order.quantity());
                items.add(order.type()==MarketOrderType.DEMAND?new DemandOrder(order.tradeId(),order.item().itemId(),stack,order.totalPrice(),order.ownerName(),order.ownerId(),order.listingTime(),order.expirationTime(),order.delivered()):new SalesOrder(order.tradeId(),order.item().itemId(),stack,order.totalPrice(),order.ownerName(),order.ownerId(),order.listingTime(),order.expirationTime()));
            } market.updateMarketItems(items); } catch(RuntimeException ignored) { /* keep the previous complete page */ }
        }
    }
}
