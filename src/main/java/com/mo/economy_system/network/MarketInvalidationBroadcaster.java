package com.mo.economy_system.network;
import com.mo.economy_system.common.market.MarketOrderType;import com.mo.economy_system.common.network.MarketDataResponseMessage;import com.mo.economy_system.core.economy_system.market.MarketSavedData;import net.minecraft.server.level.ServerPlayer;
public final class MarketInvalidationBroadcaster {
 private MarketInvalidationBroadcaster(){}
 public static void broadcast(ServerPlayer source){var orders=MarketSavedData.getInstance(source.serverLevel()).getOrders();int sales=0,demand=0;for(var o:orders)if(o.type()==MarketOrderType.SALES)sales++;else demand++;var message=MarketDataResponseMessage.invalidated(sales,demand);for(ServerPlayer player:source.server.getPlayerList().getPlayers())EconomySystem_NetworkManager.sendToClient(player,message);}
}
