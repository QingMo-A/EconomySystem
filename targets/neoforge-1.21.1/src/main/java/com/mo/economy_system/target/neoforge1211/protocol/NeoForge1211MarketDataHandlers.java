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
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

public final class NeoForge1211MarketDataHandlers {
    private static final Logger LOGGER=LogUtils.getLogger();
    public static void request(MarketDataRequestMessage message, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) {
            try {
                var data=MarketSavedData.getInstance(player.serverLevel());
                var response=MarketDataQueryService.query(data.getView(),player.getUUID(),message);
                EconomySystem_NetworkManager.sendToClient(player,response);
            } catch (RuntimeException exception) {
                LOGGER.error("Failed to serve market data request player={} request={}",player.getUUID(),message.requestId(),exception);
            }
        }});
    }
    public static void response(MarketDataResponseMessage message, IPayloadContext context) {
        context.enqueueWork(() -> { if(message.kind()==MarketDataResponseKind.PAGE)ClientOnly.applyPage(message);else if(ClientMarketState.apply(message))ClientOnly.applyNonPage(message); });
    }
    private static final class ClientOnly {
        static void applyNonPage(MarketDataResponseMessage message) {
            var minecraft=net.minecraft.client.Minecraft.getInstance(); var screen=minecraft.screen;
            if(screen instanceof com.mo.economy_system.screen.Screen_Home home)home.updateTradeInfo(message.totalSales(),message.totalDemand());
            if(message.kind()==MarketDataResponseKind.INVALIDATED&&screen instanceof com.mo.economy_system.screen.economy_system.market.Screen_Market market){market.refresh();return;}
        }
        static void applyPage(MarketDataResponseMessage message) {
            if(!ClientMarketState.canAcceptPage(message))return;var minecraft=net.minecraft.client.Minecraft.getInstance();var screen=minecraft.screen;if(minecraft.level==null)return;
            if(message.orders().isEmpty()&&message.offset()>0&&message.offset()>=message.totalMatched()&&screen instanceof com.mo.economy_system.screen.economy_system.market.Screen_Market market){market.requestFallbackPage(message.totalMatched());return;}
            try { List<MarketItem> items=new ArrayList<>(); for(MarketOrderSnapshot order:message.orders()){
                var stack=EconomyServices.platform().itemStacks().restoreSnapshot(order.item(),minecraft.level.registryAccess()).orElseThrow(); stack.setCount(order.quantity());
                items.add(order.type()==MarketOrderType.DEMAND?new DemandOrder(order.tradeId(),order.item().itemId(),stack,order.totalPrice(),order.ownerName(),order.ownerId(),order.listingTime(),order.expirationTime(),order.delivered()):new SalesOrder(order.tradeId(),order.item().itemId(),stack,order.totalPrice(),order.ownerName(),order.ownerId(),order.listingTime(),order.expirationTime()));
            } if(!ClientMarketState.commitPage(message))return;if(screen instanceof com.mo.economy_system.screen.economy_system.market.Screen_Market market)market.updateMarketItems(items); }
            catch(RuntimeException exception){ClientMarketState.pageError(message.requestId(),message.marketRevision(),"market_sync_failed");LOGGER.error("Failed to materialize market page request={} revision={}",message.requestId(),message.marketRevision(),exception);if(minecraft.player!=null)minecraft.player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.market.sync_failed"),false);}
        }
    }
}
